#!/usr/bin/env ruby
require 'yaml'
require 'minitest/autorun'
require 'open3'
require 'tempfile'

class AndroidTokenMaskingTest < Minitest::Test
  def workflow
    YAML.load_file(File.join(__dir__, '..', 'workflows', 'android-pipeline.yml'))
  end

  def selected?(event, file)
    events = workflow['on'] || workflow[true]
    selected = false
    events.fetch(event).fetch('paths').each do |pattern|
      excluded = pattern.start_with?('!')
      pattern = pattern.delete_prefix('!')
      pattern += '/*' if pattern.end_with?('/**')
      selected = !excluded if File.fnmatch?(pattern, file, File::FNM_PATHNAME | File::FNM_DOTMATCH)
    end
    selected
  end

  %w[build-dev build-uat build-prod].each do |lane|
    define_method("test_#{lane.tr('-', '_')}_masks_before_export") do
      steps = workflow.fetch('jobs').fetch(lane).fetch('steps')
      mint = steps.find { |step| step['id'] == 'play-token' }.fetch('run')
      mask = 'echo "::add-mask::${access_token}"'
      export = 'echo "access_token=${access_token}" >> "$GITHUB_OUTPUT"'
      assert_includes mint, mask
      assert_includes mint, export
      assert_operator mint.index(mask), :<, mint.index(export)
      assert_operator mint.index('if [[ -z "${access_token}" ]]'), :<, mint.index(mask)
      refute_match(/set\s+-[a-z]*x/, mint, 'Shell tracing would disclose the credential before masking')

      # Execute the real export fragment with an inert fixture, never a live credential.
      fragment = mint[mint.index(mask)..]
      Tempfile.create('android-token-output') do |output|
        stdout, stderr, result = Open3.capture3(
          { 'access_token' => 'synthetic-play-token', 'GITHUB_OUTPUT' => output.path },
          'bash', '-euo', 'pipefail', '-c', fragment
        )
        assert result.success?, stderr
        assert_equal "::add-mask::synthetic-play-token\n", stdout
        assert_equal "access_token=synthetic-play-token\n", File.read(output.path)
      end
      resolver = steps.find { |step| step['name'] == 'Resolve next Android version code' }
      assert_equal '${{ steps.play-token.outputs.access_token }}', resolver.fetch('env').fetch('PLAY_ACCESS_TOKEN')
    end
  end

  def test_validation_runs_guard_and_workflow_only_pushes_do_not_upload
    guard = '.github/scripts/test-android-token-masking.rb'
    steps = workflow.fetch('jobs').fetch('validate').fetch('steps')
    assert steps.any? { |step| step['run'] == "ruby #{guard}" }
    [guard, '.github/workflows/android-pipeline.yml'].each do |path|
      assert selected?('pull_request', path)
      refute selected?('push', path)
    end
  end
end
