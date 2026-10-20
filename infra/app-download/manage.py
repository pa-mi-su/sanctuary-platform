#!/usr/bin/env python3
"""Explicit production activation or rollback of the isolated /app edge redirect.
No operation runs unless a mode is selected. Preserve unrelated distribution fields.
"""
import argparse, json, subprocess
from pathlib import Path
FUNCTION='sanctuary-app-download'
DISTRIBUTION='E34NUCAHSMCJFM'
ACCOUNT='160885294528'
def aws(*args):
    return json.loads(subprocess.check_output(['aws',*args,'--output','json'],text=True))
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode',choices=['plan','activate-production','rollback-production'])
    parser.add_argument('--backup-dir',type=Path,required=True)
    a=parser.parse_args()
    if aws('sts','get-caller-identity')['Account']!=ACCOUNT:raise SystemExit('Wrong AWS account')
    current=aws('cloudfront','get-distribution-config','--id',DISTRIBUTION)
    config=current['DistributionConfig']
    aliases=config['Aliases'].get('Items',[])
    if 'mydailysanctuary.com' not in aliases or 'dev.mydailysanctuary.com' in aliases:raise SystemExit('Wrong distribution aliases')
    if config.get('CacheBehaviors',{}).get('Quantity',0):raise SystemExit('Review new ordered behaviors before proceeding')
    behavior=config['DefaultCacheBehavior']
    if any(x.get('EventType')=='viewer-request' for x in behavior.get('LambdaFunctionAssociations',{}).get('Items',[])):raise SystemExit('Existing viewer-request Lambda must be reviewed')
    desc=aws('cloudfront','describe-function','--name',FUNCTION,'--stage','DEVELOPMENT')
    arn=desc['FunctionSummary']['FunctionMetadata']['FunctionARN']
    items=behavior.get('FunctionAssociations',{}).get('Items',[])
    active=[x for x in items if x['EventType']=='viewer-request']
    if any(x['FunctionARN']!=arn for x in active):raise SystemExit('Existing viewer-request function must not be replaced')
    print(json.dumps({'mode':a.mode,'domain':'mydailysanctuary.com','function':FUNCTION,'paths':['/app','/app/'],'otherRequests':'pass through unchanged','alreadyAttached':bool(active)},indent=2))
    if a.mode=='plan':return
    a.backup_dir.mkdir(parents=True,exist_ok=True)
    backup=a.backup_dir/'distribution-before.json'
    if backup.exists():raise SystemExit('Choose a fresh backup directory; never overwrite the previous activation backup')
    backup.write_text(json.dumps(current,indent=2))
    if a.mode=='activate-production':
        # Require a successful AWS-native test of the current development version.
        for agent,expected in [('iPhone','apps.apple.com'),('Android','play.google.com')]:
            event={'version':'1.0','context':{'eventType':'viewer-request'},'viewer':{'ip':'203.0.113.1'},'request':{'method':'GET','uri':'/app','headers':{'user-agent':{'value':agent}},'querystring':{},'cookies':{}}}
            event_path=a.backup_dir/(agent+'.json');event_path.write_text(json.dumps(event))
            test=aws('cloudfront','test-function','--name',FUNCTION,'--if-match',desc['ETag'],'--stage','DEVELOPMENT','--event-object','fileb://'+str(event_path.resolve()))['TestResult']
            if test.get('FunctionErrorMessage'):raise SystemExit(test['FunctionErrorMessage'])
            output=json.loads(test['FunctionOutput'])
            response=output.get('response',output)
            if response.get('statusCode')!=302 or expected not in response.get('headers',{}).get('location',{}).get('value',''):raise SystemExit('AWS routing test failed')
        aws('cloudfront','publish-function','--name',FUNCTION,'--if-match',desc['ETag'])
        items=[x for x in items if x['EventType']!='viewer-request']+[{'EventType':'viewer-request','FunctionARN':arn}]
    else:
        items=[x for x in items if not (x['EventType']=='viewer-request' and x['FunctionARN']==arn)]
    behavior['FunctionAssociations']={'Quantity':len(items),**({'Items':items} if items else {})}
    updated=a.backup_dir/'distribution-updated.json';updated.write_text(json.dumps(config))
    result=aws('cloudfront','update-distribution','--id',DISTRIBUTION,'--if-match',current['ETag'],'--distribution-config','file://'+str(updated.resolve()))
    print(json.dumps({'distributionId':result['Distribution']['Id'],'status':result['Distribution']['Status'],'operation':a.mode},indent=2))
    print('Wait for CloudFront Deployed, then test the live URL with iPhone, Android, and desktop user agents. The printed QR URL is unchanged.')
if __name__=='__main__':main()
