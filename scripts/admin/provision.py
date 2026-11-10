#!/usr/bin/env python3
"""Provision isolated single-owner admin auth; never print passwords or encryption keys."""
import argparse, base64, json, os, subprocess

parser = argparse.ArgumentParser()
parser.add_argument('stage', choices=['dev', 'prod'])
args = parser.parse_args()
stage = args.stage
OWNER = 'pmsulikowski@icloud.com'
REGION = 'us-east-1'
ACCOUNT = '160885294528'
PUBLIC_POOLS = {'dev': 'us-east-1_oCrXzjKbV', 'prod': 'us-east-1_syYJKg0NY'}

def aws(service, operation, **values):
    result = subprocess.run(['aws', service, operation, '--region', REGION, '--output', 'json', '--cli-input-json', json.dumps(values)], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f'{service} {operation} failed: {result.stderr.strip()}')
    return json.loads(result.stdout) if result.stdout.strip() else {}

identity = aws('sts', 'get-caller-identity')
if identity['Account'] != ACCOUNT:
    raise RuntimeError('Refusing to provision in an unexpected AWS account')
name = f'sanctuary-{stage}-content-admin'
pools = aws('cognito-idp', 'list-user-pools', MaxResults=60)['UserPools']
matching = [pool for pool in pools if pool['Name'] == name]
if len(matching) > 1:
    raise RuntimeError('Ambiguous admin pools')
if matching:
    pool = matching[0]['Id']
else:
    result = aws('cognito-idp', 'create-user-pool', PoolName=name, UsernameAttributes=['email'],
        AutoVerifiedAttributes=['email'], UsernameConfiguration={'CaseSensitive': False},
        AdminCreateUserConfig={'AllowAdminCreateUserOnly': True, 'InviteMessageTemplate': {
            'EmailSubject': f'Sanctuary {stage.upper()} admin access',
            'EmailMessage': 'Your Sanctuary '+stage.upper()+' admin account is ready. Username: {username}. Temporary password: {####}. Open https://'+('dev.' if stage == 'dev' else '')+'mydailysanctuary.com/admin, sign in, choose your password, and enroll your authenticator. Do not send your password or authenticator codes to anyone.'}},
        Policies={'PasswordPolicy': {'MinimumLength': 14, 'RequireUppercase': True, 'RequireLowercase': True,
            'RequireNumbers': True, 'RequireSymbols': True, 'TemporaryPasswordValidityDays': 7}},
        AccountRecoverySetting={'RecoveryMechanisms': [{'Priority': 1, 'Name': 'verified_email'}]},
        DeletionProtection='ACTIVE', UserPoolTier='LITE', UserPoolTags={'Application': 'Sanctuary', 'Stage': stage, 'Purpose': 'ContentAdmin'})
    pool = result['UserPool']['Id']
aws('cognito-idp', 'set-user-pool-mfa-config', UserPoolId=pool, MfaConfiguration='OPTIONAL', SoftwareTokenMfaConfiguration={'Enabled': True})
clients = aws('cognito-idp', 'list-user-pool-clients', UserPoolId=pool, MaxResults=60)['UserPoolClients']
matching = [client for client in clients if client['ClientName'] == name+'-web']
if matching:
    client = matching[0]['ClientId']
else:
    client = aws('cognito-idp', 'create-user-pool-client', UserPoolId=pool, ClientName=name+'-web', GenerateSecret=False,
        ExplicitAuthFlows=['ALLOW_USER_PASSWORD_AUTH', 'ALLOW_REFRESH_TOKEN_AUTH'], PreventUserExistenceErrors='ENABLED',
        EnableTokenRevocation=True, AuthSessionValidity=5, AccessTokenValidity=1, IdTokenValidity=1,
        TokenValidityUnits={'AccessToken': 'hours', 'IdToken': 'hours'})['UserPoolClient']['ClientId']
secret_name = f'/sanctuary/{stage}/content-admin/challenge-key'
secrets = aws('secretsmanager', 'list-secrets', Filters=[{'Key': 'name', 'Values': [secret_name]}])['SecretList']
matching = [secret for secret in secrets if secret['Name'] == secret_name]
if matching:
    secret_arn = matching[0]['ARN']
else:
    secret_arn = aws('secretsmanager', 'create-secret', Name=secret_name,
        Description='AES-256-GCM key for short-lived Sanctuary content-admin challenges',
        SecretString=base64.b64encode(os.urandom(32)).decode(),
        Tags=[{'Key': 'Application', 'Value': 'Sanctuary'}, {'Key': 'Stage', 'Value': stage}])['ARN']
aws('iam', 'put-role-policy', RoleName=f'sanctuary-api-{stage}-execution', PolicyName='SanctuaryContentAdminChallengeKey',
    PolicyDocument=json.dumps({'Version': '2012-10-17', 'Statement': [{'Effect': 'Allow', 'Action': 'secretsmanager:GetSecretValue', 'Resource': secret_arn}]}))
aws('iam', 'put-role-policy', RoleName=f'sanctuary-api-{stage}-task', PolicyName='SanctuaryContentAdminDirectoryRead',
    PolicyDocument=json.dumps({'Version': '2012-10-17', 'Statement': [{'Effect': 'Allow', 'Action': 'cognito-idp:ListUsers', 'Resource': f'arn:aws:cognito-idp:{REGION}:{ACCOUNT}:userpool/{PUBLIC_POOLS[stage]}'}]}))
users = aws('cognito-idp', 'list-users', UserPoolId=pool, Limit=60)['Users']
if any(next((a['Value'] for a in u['Attributes'] if a['Name']=='email'), '') != OWNER for u in users):
    raise RuntimeError('Unexpected account in the admin-only pool')
if not users:
    user = aws('cognito-idp', 'admin-create-user', UserPoolId=pool, Username=OWNER,
        UserAttributes=[{'Name': 'email', 'Value': OWNER}, {'Name': 'email_verified', 'Value': 'true'}], DesiredDeliveryMediums=['EMAIL'])['User']
else:
    user = users[0]
sub = next(a['Value'] for a in user['Attributes'] if a['Name'] == 'sub')
metadata = {'stage': stage, 'poolId': pool, 'clientId': client, 'ownerSub': sub, 'keySecretArn': secret_arn, 'ownerStatus': user['UserStatus']}
for key, value in {'ENABLED': 'true', 'OWNER_SUB': sub, 'CLIENT_ID': client, 'KEY_SECRET_ARN': secret_arn}.items():
    subprocess.run(['gh', 'variable', 'set', f'API_{stage.upper()}_ADMIN_{key}', '--repo', 'pa-mi-su/sanctuary-platform', '--env', stage, '--body', value], check=True, capture_output=True)
print(json.dumps(metadata, indent=2))
