"""Exercise retirement policy without AWS, Firebase, or database access."""
import contextlib, io, json, pathlib, runpy, subprocess, sys, tempfile, unittest
from unittest.mock import patch
SCRIPT=pathlib.Path(__file__).with_name('audit-push-registrations.py')
IDENTIFIER='11111111-2222-3333-4444-555555555555'
class AuditPolicyTest(unittest.TestCase):
    def run_audit(self,status,apply=False,changed='1'):
        mutations=[]
        device={'id':IDENTIFIER,'platform':'android','token':'private-test-token','fingerprint':'a'*32,'updated_at':'2026-11-13T20:00:00+00:00'}
        def execute(command,**kwargs):
            if command[0]=='aws':
                operation=command[1:3]
                if operation==['sts','get-caller-identity']:value={'Account':'160885294528'}
                elif operation==['rds','describe-db-instances']:value={'DBInstances':[{'DBInstanceStatus':'available','Endpoint':{'Address':'test.invalid','Port':5432},'MasterUserSecret':{'SecretArn':'db-secret'}}]}
                elif command[command.index('--secret-id')+1]=='db-secret':value={'SecretString':json.dumps({'username':'test','password':'private-test-password'})}
                else:value={'SecretString':json.dumps({'project_id':'sanctuary-7f082'})}
                return subprocess.CompletedProcess(command,0,json.dumps(value),'')
            if command[0].endswith('psql'):
                sql=command[-1]
                if 'json_agg' in sql:output=json.dumps([device])
                elif 'UPDATE push_devices' in sql:mutations.append(sql);output=changed
                else:output='1'
                return subprocess.CompletedProcess(command,0,output,'')
            return subprocess.CompletedProcess(command,0,json.dumps([{'id':IDENTIFIER,'platform':'android','status':status}]),'')
        with tempfile.TemporaryDirectory() as directory:
            cp=pathlib.Path(directory)/'classpath';cp.write_text('test-classpath')
            args=[str(SCRIPT),'dev','--classpath',str(cp)]+(['--apply'] if apply else [])
            output=io.StringIO()
            with patch.object(sys,'argv',args),patch('subprocess.run',side_effect=execute),patch('urllib.request.urlretrieve'),contextlib.redirect_stdout(output):
                runpy.run_path(str(SCRIPT),run_name='__main__')
        self.assertNotIn('private-test-token',output.getvalue())
        self.assertNotIn('private-test-password',output.getvalue())
        return json.loads(output.getvalue()),mutations
    def test_validated_or_uncertain_tokens_are_never_retired(self):
        for status in ['FCM_VALIDATED','INVALID_ARGUMENT','UNAVAILABLE','PERMISSION_DENIED','APNS_NOT_VERIFIED']:
            with self.subTest(status=status):
                report,mutations=self.run_audit(status,True)
                self.assertFalse(mutations);self.assertFalse(report['results'][0]['retired'])
    def test_read_only_mode_does_not_mutate_confirmed_deleted_token(self):
        report,mutations=self.run_audit('UNREGISTERED')
        self.assertFalse(mutations);self.assertFalse(report['results'][0]['retired'])
    def test_apply_only_retires_confirmed_deleted_token_with_race_guards(self):
        report,mutations=self.run_audit('UNREGISTERED',True)
        self.assertTrue(report['results'][0]['retired']);self.assertEqual(len(mutations),1)
        self.assertIn("md5(token)='"+'a'*32+"'",mutations[0])
        self.assertIn("updated_at='2026-11-13T20:00:00+00:00'",mutations[0])
        self.assertIn('SET valid=false',mutations[0]);self.assertNotIn('DELETE',mutations[0])
    def test_concurrent_registration_refresh_is_not_reported_retired(self):
        report,_=self.run_audit('UNREGISTERED',True,changed='0')
        self.assertFalse(report['results'][0]['retired'])
if __name__=='__main__':unittest.main()
