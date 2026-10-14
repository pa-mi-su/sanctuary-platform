"""Verify exact S3 route keys and deployment isolation without AWS access."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('publish-app-routes.sh')


class AppRoutePublishingTest(unittest.TestCase):
    def publish(self, environment='dev', bucket='dev.mydailysanctuary.com', shell=True):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            if shell:
                (root / 'index.html').write_text('<html><head><meta http-equiv="Content-Security-Policy" content="script-src self"></head><body><app-root></app-root></body></html>')
            fake = root / 'aws'
            fake.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
args=sys.argv[1:]
assert args[:2]==['s3api','put-object']
assert 'Content-Security-Policy' in pathlib.Path(args[args.index('--body')+1]).read_text()
with open(os.environ['ROUTE_TEST_CALLS'],'a') as output: output.write(json.dumps(args)+'\\n')
''')
            fake.chmod(0o755)
            calls_path = root / 'calls.jsonl'
            env = dict(os.environ, PATH=str(root)+os.pathsep+os.environ['PATH'], WEB_BUILD_DIR=str(root),
                WEB_DEPLOY_ENVIRONMENT=environment, WEB_S3_BUCKET=bucket, ROUTE_TEST_CALLS=str(calls_path))
            result = subprocess.run(['bash', str(SCRIPT)], env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in calls_path.read_text().splitlines()] if calls_path.exists() else []
            return result, calls

    def test_exact_routes_and_trailing_slashes_serve_html_for_both_environments(self):
        for environment, bucket in [('dev','dev.mydailysanctuary.com'),('prod','mydailysanctuary.com')]:
            with self.subTest(environment=environment):
                result, calls = self.publish(environment, bucket)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual([args[args.index('--key')+1] for args in calls],
                    ['news','news/','admin','admin/','admin/news','admin/news/'])
                for args in calls:
                    self.assertEqual(args[args.index('--bucket')+1], bucket)
                    self.assertEqual(args[args.index('--content-type')+1], 'text/html; charset=utf-8')
                    self.assertEqual(args[args.index('--cache-control')+1], 'no-cache')

    def test_cross_environment_and_unknown_targets_cannot_write(self):
        for environment,bucket in [('dev','mydailysanctuary.com'),('prod','dev.mydailysanctuary.com'),('main','mydailysanctuary.com')]:
            with self.subTest(environment=environment):
                result, calls = self.publish(environment,bucket)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(calls, [])

    def test_missing_app_shell_cannot_write(self):
        result, calls = self.publish(shell=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls, [])


if __name__ == '__main__':
    unittest.main()
