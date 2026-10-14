#!/usr/bin/env python3
"""Exercise deployment guards with a fake AWS CLI; never contacts AWS."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('apply-web-security-headers.sh')
MANAGED = '67f7725c-6f97-4210-82d7-5512b31e9d03'


class SecurityHeadersDeploymentTest(unittest.TestCase):
    def run_script(self, *, environment='dev', policy=MANAGED, alias=None, current_policy=None, policy_name=None, aliases=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            config = {'Aliases': {'Quantity': 1, 'Items': aliases or [alias or f'{environment}.mydailysanctuary.com']},
                      'Comment': 'preserve this', 'Enabled': True,
                      'DefaultCacheBehavior': {'TargetOriginId': 'existing-origin'},
                      'CacheBehaviors': {'Quantity': 1, 'Items': [{'PathPattern': '/assets/*', 'TargetOriginId': 'existing-origin'}]}}
            if current_policy:
                config['DefaultCacheBehavior']['ResponseHeadersPolicyId'] = current_policy
                config['CacheBehaviors']['Items'][0]['ResponseHeadersPolicyId'] = current_policy
            (root / 'config.json').write_text(json.dumps({'ETag': 'existing-etag', 'DistributionConfig': config}))
            (root / 'policy.json').write_text(json.dumps({'ResponseHeadersPolicy': {'ResponseHeadersPolicyConfig': {
                'Name': policy_name or f'sanctuary-{environment}-web-security'}}}))
            fake = root / 'aws'
            fake.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
root=pathlib.Path(os.environ['FAKE_AWS_ROOT'])
args=sys.argv[1:]
with (root/'calls.jsonl').open('a') as output: output.write(json.dumps(args)+'\\n')
if args[:2]==['cloudfront','get-distribution-config']: print((root/'config.json').read_text())
elif args[:2]==['cloudfront','get-response-headers-policy']: print((root/'policy.json').read_text())
elif args[:2]==['cloudfront','update-distribution']:
    assert args[args.index('--if-match')+1]=='existing-etag'
    source=pathlib.Path(args[args.index('--distribution-config')+1].removeprefix('file://'))
    (root/'updated.json').write_text(source.read_text())
elif args[:3]!=['cloudfront','wait','distribution-deployed']: sys.exit(99)
''')
            fake.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'], FAKE_AWS_ROOT=str(root),
                       WEB_DEPLOY_ENVIRONMENT=environment, WEB_SECURITY_HEADERS_POLICY_ID=policy,
                       WEB_CLOUDFRONT_DISTRIBUTION_ID='test-distribution')
            result = subprocess.run(['bash', str(SCRIPT)], env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in (root/'calls.jsonl').read_text().splitlines()] if (root/'calls.jsonl').exists() else []
            updated = json.loads((root/'updated.json').read_text()) if (root/'updated.json').exists() else None
            return result, calls, updated, config

    def test_managed_dev_policy_preserves_distribution_and_uses_etag(self):
        result, calls, updated, original = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(updated['Comment'], original['Comment'])
        self.assertEqual(updated['DefaultCacheBehavior']['TargetOriginId'], 'existing-origin')
        self.assertEqual(updated['DefaultCacheBehavior']['ResponseHeadersPolicyId'], MANAGED)
        self.assertEqual(updated['CacheBehaviors']['Items'][0]['ResponseHeadersPolicyId'], MANAGED)
        self.assertFalse(any(call[1] == 'get-response-headers-policy' for call in calls))
        self.assertTrue(any(call[1] == 'update-distribution' for call in calls))

    def test_managed_production_policy_is_supported_without_plan_changes(self):
        result, calls, updated, original = self.run_script(environment='prod', alias='mydailysanctuary.com')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(updated['DefaultCacheBehavior']['ResponseHeadersPolicyId'], MANAGED)
        self.assertEqual(updated['CacheBehaviors']['Items'][0]['ResponseHeadersPolicyId'], MANAGED)
        # Only response policy references change; no pricing or origin settings are added/removed.
        updated['DefaultCacheBehavior'].pop('ResponseHeadersPolicyId')
        updated['CacheBehaviors']['Items'][0].pop('ResponseHeadersPolicyId')
        self.assertEqual(updated, original)
        self.assertFalse(any(call[1] == 'get-response-headers-policy' for call in calls))

    def test_production_cannot_target_dev_distribution(self):
        result, _, updated, _ = self.run_script(environment='prod', alias='dev.mydailysanctuary.com')
        self.assertNotEqual(result.returncode, 0)
        self.assertIsNone(updated)

    def test_mixed_environment_aliases_fail_before_mutation(self):
        for environment in ('dev', 'prod'):
            with self.subTest(environment=environment):
                result, _, updated, _ = self.run_script(environment=environment,
                    aliases=['mydailysanctuary.com', 'dev.mydailysanctuary.com'])
                self.assertNotEqual(result.returncode, 0)
                self.assertIsNone(updated)

    def test_unknown_environment_is_rejected(self):
        result, calls, updated, _ = self.run_script(environment='main')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])
        self.assertIsNone(updated)

    def test_wrong_distribution_alias_fails_before_mutation(self):
        result, _, updated, _ = self.run_script(alias='mydailysanctuary.com')
        self.assertNotEqual(result.returncode, 0)
        self.assertIsNone(updated)

    def test_wrong_custom_policy_environment_fails_before_mutation(self):
        result, _, updated, _ = self.run_script(policy='custom-policy', policy_name='sanctuary-prod-web-security')
        self.assertNotEqual(result.returncode, 0)
        self.assertIsNone(updated)

    def test_already_configured_policy_does_not_update(self):
        result, calls, updated, _ = self.run_script(current_policy=MANAGED)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIsNone(updated)
        self.assertFalse(any(call[1] == 'update-distribution' for call in calls))

    def test_existing_production_custom_policy_path_is_preserved(self):
        result, calls, updated, _ = self.run_script(environment='prod', policy='custom-policy', alias='mydailysanctuary.com')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(updated['DefaultCacheBehavior']['ResponseHeadersPolicyId'], 'custom-policy')
        self.assertTrue(any(call[1] == 'get-response-headers-policy' for call in calls))


if __name__ == '__main__':
    unittest.main()
