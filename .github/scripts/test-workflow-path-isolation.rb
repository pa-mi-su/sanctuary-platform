#!/usr/bin/env ruby
# Verify that content-admin changes cannot select native workflows.
require 'yaml'
require 'minitest/autorun'
require 'json'
require 'open3'

class WorkflowPathIsolationTest < Minitest::Test
  def workflow(name)
    YAML.load_file(File.join(__dir__, '..', 'workflows', name))
  end

  def triggers?(name, event, file)
    config = workflow(name)
    paths = (config['on'] || config[true]).fetch(event).fetch('paths')
    selected = false
    paths.each do |pattern|
      excluded = pattern.start_with?('!')
      pattern = pattern.delete_prefix('!')
      pattern += '/*' if pattern.end_with?('/**')
      selected = !excluded if File.fnmatch?(pattern, file, File::FNM_PATHNAME | File::FNM_DOTMATCH)
    end
    selected
  end

  def test_admin_changes_do_not_trigger_native_builds
    files = %w[apps/web/src/app/admin/admin-page.component.ts apps/api/src/main/java/app/sanctuary/api/admin/AdminController.java apps/api/src/main/resources/db/migration/V20__content_admin.sql package-lock.json .github/workflows/web-prod-deploy.yml .github/workflows/api-prod-deploy.yml]
    %w[ios-pipeline.yml android-pipeline.yml].each do |name|
      %w[push pull_request].each do |event|
        files.each { |file| refute triggers?(name, event, file), "#{name}: #{file} must not select #{event}" }
      end
    end
  end

  def test_native_source_still_selects_its_own_build
    %w[push pull_request].each do |event|
      assert triggers?('ios-pipeline.yml', event, 'apps/ios/Sanctuary/ContentView.swift')
      assert triggers?('android-pipeline.yml', event, 'apps/android/app/src/main/Test.kt')
      refute triggers?('ios-pipeline.yml', event, 'apps/ios/README.md')
      refute triggers?('android-pipeline.yml', event, 'apps/android/README.md')
    end
  end

  def test_admin_selects_web_and_api_validation
    assert triggers?('web-prod-deploy.yml', 'pull_request', 'apps/web/src/app/admin/admin-page.component.ts')
    assert triggers?('api-prod-deploy.yml', 'pull_request', 'apps/api/src/main/java/app/sanctuary/api/admin/AdminController.java')
  end
  def test_ecs_rendering_keeps_admin_secret_out_of_plain_environment
    %w[api-prod-deploy.yml api-dev-deploy.yml].each do |name|
      run = workflow(name).fetch('jobs').fetch('deploy').fetch('steps').map { |step| step['run'] }.compact.find { |script| script.include?("'del(") }
      program = run.match(/'(del\([\s\S]+?)' task-definition.json/)[1]
      %w[true false].each do |enabled|
        args = run.scan(/--arg (\w+) /).flatten.flat_map { |key| ['--arg', key, key == 'admin_enabled' ? enabled : "fixture-#{key}"] }
        input = { 'taskDefinitionArn' => 'old', 'containerDefinitions' => [{ 'name' => 'Main', 'environment' => [], 'secrets' => [] }] }
        output, error, status = Open3.capture3('jq', *args, program, stdin_data: JSON.generate(input))
        assert status.success?, error
        container = JSON.parse(output).fetch('containerDefinitions').first
        environment = container.fetch('environment').to_h { |entry| [entry['name'], entry['value']] }
        assert_equal enabled, environment['SANCTUARY_ADMIN_ENABLED']
        assert_equal 'fixture-admin_sub', environment['SANCTUARY_ADMIN_OWNER_SUB']
        refute environment.key?('SANCTUARY_ADMIN_ENCRYPTION_KEY')
        secret = container.fetch('secrets').find { |entry| entry['name'] == 'SANCTUARY_ADMIN_ENCRYPTION_KEY' }
        enabled == 'true' ? assert_equal('fixture-admin_key_secret', secret['valueFrom']) : assert_nil(secret)
      end
    end
  end

  def test_delayed_pr_checks_cannot_cancel_deployments
    %w[api-dev-deploy.yml api-prod-deploy.yml web-dev-deploy.yml web-prod-deploy.yml].each do |name|
      group = workflow(name).fetch('concurrency').fetch('group')
      # A merged PR may report refs/heads/main; its number must still distinguish it.
      assert_includes group, 'github.event.pull_request.number || github.ref', name
    end
  end

end
