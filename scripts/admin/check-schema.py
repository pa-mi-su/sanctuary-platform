#!/usr/bin/env python3
"""Read only Flyway history from a named Sanctuary database, using verified TLS."""
import argparse, json, os, subprocess, tempfile, urllib.request
p=argparse.ArgumentParser(); p.add_argument('stage',choices=['dev','prod']); stage=p.parse_args().stage

def aws(*args):
 r=subprocess.run(['aws',*args,'--region','us-east-1','--output','json'],capture_output=True,text=True,check=True)
 return json.loads(r.stdout)
if aws('sts','get-caller-identity')['Account']!='160885294528': raise RuntimeError('Unexpected AWS account')
db=aws('rds','describe-db-instances','--db-instance-identifier',f'sanctuary-{stage}-db')['DBInstances'][0]
if db['DBInstanceStatus']!='available': raise RuntimeError('Database is not available yet')
secret=json.loads(aws('secretsmanager','get-secret-value','--secret-id',db['MasterUserSecret']['SecretArn'])['SecretString'])
with tempfile.TemporaryDirectory(prefix='sanctuary-schema-') as directory:
 ca=os.path.join(directory,'rds-ca.pem'); urllib.request.urlretrieve('https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem',ca)
 env=os.environ.copy();env.update(PGPASSWORD=secret['password'],PGSSLMODE='verify-full',PGSSLROOTCERT=ca,PGCONNECT_TIMEOUT='10')
 result=subprocess.run(['/opt/homebrew/opt/postgresql@15/bin/psql','-h',db['Endpoint']['Address'],'-p',str(db['Endpoint']['Port']),'-U',secret['username'],'-d','sanctuary','-X','-v','ON_ERROR_STOP=1','-c','SELECT version, description, script, success FROM flyway_schema_history ORDER BY installed_rank;'],env=env,capture_output=True,text=True)
 if result.returncode: raise RuntimeError(result.stderr.strip())
 print(result.stdout)
