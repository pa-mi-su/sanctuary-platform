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
        rendered = JSON.parse(output)
        assert_equal 'fixture-execution_role', rendered['executionRoleArn']
        assert_equal 'fixture-task_role', rendered['taskRoleArn']
        container = rendered.fetch('containerDefinitions').first
        environment = container.fetch('environment').to_h { |entry| [entry['name'], entry['value']] }
        assert_equal enabled, environment['SANCTUARY_ADMIN_ENABLED']
        assert_equal 'fixture-admin_sub', environment['SANCTUARY_ADMIN_OWNER_SUB']
        refute environment.key?('SANCTUARY_ADMIN_ENCRYPTION_KEY')
        secret = container.fetch('secrets').find { |entry| entry['name'] == 'SANCTUARY_ADMIN_ENCRYPTION_KEY' }
        enabled == 'true' ? assert_equal('fixture-admin_key_secret', secret['valueFrom']) : assert_nil(secret)
      end
    end
  end

  def test_push_credential_is_injected_only_when_enabled
    %w[api-dev-deploy.yml api-prod-deploy.yml].each do |name|
      run = workflow(name).fetch('jobs').fetch('deploy').fetch('steps').find { |step| step.fetch('run', '').include?('--arg admin_key_secret') }.fetch('run')
      program = run.match(/'del\((.*?)\)' task-definition.json/m)[0].sub(/' task-definition.json\z/, '').delete_prefix("'")
      %w[true false].each do |enabled|
        args = run.scan(/--arg (\w+) /).flatten.flat_map { |key| ['--arg', key, key == 'push_enabled' ? enabled : 'fixture-' + key] }
        input = { 'containerDefinitions' => [{ 'name' => 'Main', 'environment' => [], 'secrets' => [] }] }
        output, error, status = Open3.capture3('jq', *args, program, stdin_data: JSON.generate(input))
        assert status.success?, error
        container = JSON.parse(output).fetch('containerDefinitions').first
        env = container.fetch('environment').to_h { |entry| [entry['name'], entry['value']] }
        assert_equal enabled, env['SANCTUARY_PUSH_ENABLED']
        refute env.key?('SANCTUARY_FIREBASE_SERVICE_ACCOUNT_JSON')
        secret = container.fetch('secrets').find { |entry| entry['name'] == 'SANCTUARY_FIREBASE_SERVICE_ACCOUNT_JSON' }
        enabled == 'true' ? assert_equal('fixture-firebase_secret', secret['valueFrom']) : assert_nil(secret)
      end
    end
  end

  def test_api_tests_never_inherit_live_push_enablement
    %w[api-dev-deploy.yml api-prod-deploy.yml].each do |name|
      workflow(name).fetch('jobs').each_value do |job|
        job.fetch('steps').select { |step| step['name'] == 'Run API tests' }.each do |step|
          assert_equal 'false', step.fetch('env').fetch('SANCTUARY_PUSH_ENABLED'), name
        end
      end
    end
  end

  def test_ios_archives_do_not_force_app_profiles_onto_packages
    source = File.read(File.join(__dir__, '..', 'workflows', 'ios-pipeline.yml'))
    archives = source.split('          xcodebuild ').drop(1).select { |command| command.include?("\n            archive\n") }.map { |command| command.split("\n            archive").first }
    assert_equal 3, archives.size
    archives.each do |command|
      refute_match(/PROVISIONING_PROFILE_SPECIFIER=|CODE_SIGN_STYLE=|CODE_SIGN_IDENTITY=|DEVELOPMENT_TEAM=/, command)
    end
    project = File.read(File.join(__dir__, '..', '..', 'apps', 'ios', 'Sanctuary.xcodeproj', 'project.pbxproj'))
    %w[Dev UAT Prod].each { |stage| assert_includes project, 'PROVISIONING_PROFILE_SPECIFIER = "Sanctuary ' + stage + ' App Store";' }
  end

  def test_delayed_pr_checks_cannot_cancel_deployments
    %w[api-dev-deploy.yml api-prod-deploy.yml web-dev-deploy.yml web-prod-deploy.yml].each do |name|
      group = workflow(name).fetch('concurrency').fetch('group')
      # A merged PR may report refs/heads/main; its number must still distinguish it.
      assert_includes group, 'github.event.pull_request.number || github.ref', name
    end
  end

  def test_web_entry_page_publishes_after_assets_without_deleting_old_bundles
    %w[web-dev-deploy.yml web-prod-deploy.yml].each do |name|
      steps = workflow(name).fetch('jobs').fetch('deploy').fetch('steps')
      sync = steps.find { |step| step['name'] == 'Sync web build to S3' }.fetch('run')
      refute_includes sync, '--delete'
      assert_includes sync, '--exclude "index.html"'
      names = steps.map { |step| step['name'] }
      assert_operator names.index('Publish entry page after assets'), :>, names.index('Publish admin entry as HTML')
      assert_operator names.index('Publish entry page after assets'), :<, names.index('Invalidate CloudFront cache')
    end
  end

end
