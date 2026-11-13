#!/usr/bin/env python3
"""Validate Android registrations without sending pushes. --apply retires only UNREGISTERED.
Secrets/tokens travel only through captured process input, never command arguments or reports.
"""
import argparse, json, os, pathlib, subprocess, tempfile, urllib.request, uuid
p=argparse.ArgumentParser()
p.add_argument('stage', choices=['dev','prod'])
p.add_argument('--classpath', required=True, help='File from Maven dependency:build-classpath for apps/api')
p.add_argument('--java', default='java')
p.add_argument('--apply', action='store_true')
a=p.parse_args()
def aws(*args):
    r=subprocess.run(['aws',*args,'--region','us-east-1','--output','json'],capture_output=True,text=True)
    if r.returncode: raise RuntimeError('AWS lookup failed: '+args[0]+' '+args[1])
    return json.loads(r.stdout)
if aws('sts','get-caller-identity')['Account'] != '160885294528': raise RuntimeError('Unexpected AWS account')
db=aws('rds','describe-db-instances','--db-instance-identifier',f'sanctuary-{a.stage}-db')['DBInstances'][0]
if db['DBInstanceStatus']!='available': raise RuntimeError('Database is not available')
secret=json.loads(aws('secretsmanager','get-secret-value','--secret-id',db['MasterUserSecret']['SecretArn'])['SecretString'])
credentials=json.loads(aws('secretsmanager','get-secret-value','--secret-id',f'sanctuary/{a.stage}/firebase-service-account')['SecretString'])
if credentials.get('project_id')!='sanctuary-7f082': raise RuntimeError('Unexpected Firebase project')
with tempfile.TemporaryDirectory(prefix='sanctuary-push-audit-') as directory:
    ca=os.path.join(directory,'rds-ca.pem')
    urllib.request.urlretrieve('https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem',ca)
    env=os.environ.copy();env.update(PGPASSWORD=secret['password'],PGSSLMODE='verify-full',PGSSLROOTCERT=ca,PGCONNECT_TIMEOUT='10')
    def query(sql):
        r=subprocess.run(['/opt/homebrew/opt/postgresql@15/bin/psql','-h',db['Endpoint']['Address'],'-p',str(db['Endpoint']['Port']),'-U',secret['username'],'-d','sanctuary','-X','-A','-t','-v','ON_ERROR_STOP=1','-c',sql],env=env,capture_output=True,text=True)
        if r.returncode: raise RuntimeError('Database audit operation failed')
        return r.stdout.strip()
    devices=json.loads(query("SELECT COALESCE(json_agg(d),'[]'::json) FROM (SELECT id,platform,token,md5(token) AS fingerprint,updated_at FROM push_devices WHERE enabled AND valid ORDER BY id) d"))
    helper=pathlib.Path(__file__).with_name('ValidatePushRegistrations.java')
    result=subprocess.run([a.java,'--class-path',pathlib.Path(a.classpath).read_text().strip(),str(helper)],input=json.dumps({'credentials':credentials,'devices':devices}),capture_output=True,text=True)
    if result.returncode: raise RuntimeError('Firebase validation helper failed; no records changed')
    results=json.loads(result.stdout)
    if len(results)!=len(devices) or {r['id'] for r in results}!={d['id'] for d in devices}: raise RuntimeError('Incomplete validation; no records changed')
    for row in results:
        row['retired']=False
        if row['status']=='UNREGISTERED' and a.apply:
            device=next(d for d in devices if d['id']==row['id'])
            identifier=str(uuid.UUID(row['id']))
            fingerprint=device['fingerprint']
            if len(fingerprint)!=32 or any(c not in '0123456789abcdef' for c in fingerprint): raise RuntimeError('Invalid fingerprint')
            # Match the validated token and its original update time: never retire a refreshed registration.
            instant=device['updated_at'].replace("'", "''")
            changed=query(f"WITH retired AS (UPDATE push_devices SET valid=false WHERE id='{identifier}'::uuid AND md5(token)='{fingerprint}' AND updated_at='{instant}'::timestamptz AND enabled AND valid RETURNING id) SELECT count(*) FROM retired")
            row['retired']=changed=='1'
    print(json.dumps({'stage':a.stage,'validationOnly':True,'applied':a.apply,'results':results,'remainingEligible':int(query("SELECT count(*) FROM push_devices WHERE enabled AND valid AND updated_at>now()-interval '60 days'"))},indent=2))
