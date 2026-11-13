#!/usr/bin/env python3
"""Prepare/apply exact-secret read access for an existing Sanctuary API execution role.
No credentials are read. Default is review only; use --apply to attach the scoped policy.
"""
import argparse,json,subprocess
p=argparse.ArgumentParser();p.add_argument('stage',choices=['dev','prod']);p.add_argument('--apply',action='store_true');args=p.parse_args()
def aws(*command):
 result=subprocess.run(['aws',*command,'--region','us-east-1','--output','json'],capture_output=True,text=True,check=True)
 return json.loads(result.stdout) if result.stdout.strip() else {}
if aws('sts','get-caller-identity')['Account']!='160885294528':raise RuntimeError('Unexpected AWS account')
secret=aws('secretsmanager','describe-secret','--secret-id',f'sanctuary/{args.stage}/firebase-service-account')
role=f'sanctuary-api-{args.stage}-execution'
aws('iam','get-role','--role-name',role)
policy={'Version':'2012-10-17','Statement':[{'Effect':'Allow','Action':['secretsmanager:GetSecretValue'],'Resource':secret['ARN']}]}
print(json.dumps({'role':role,'policy':policy},indent=2))
if args.apply:
 aws('iam','put-role-policy','--role-name',role,'--policy-name','SanctuaryFirebaseMessagingSecret','--policy-document',json.dumps(policy))
 decision=aws('iam','simulate-principal-policy','--policy-source-arn',f'arn:aws:iam::160885294528:role/{role}','--action-names','secretsmanager:GetSecretValue','--resource-arns',secret['ARN'])
 if any(r['EvalDecision']!='allowed'for r in decision['EvaluationResults']):raise RuntimeError('Secret read permission did not validate')
 print('Exact Firebase secret read permission validated. No service deployment performed.')
