#!/usr/bin/env python3
"""Loopback-only synthetic API for admin visual QA. Never connects to AWS or real accounts."""
import argparse, json, re, urllib.parse
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from datetime import datetime, timedelta, timezone
p=argparse.ArgumentParser();p.add_argument('--signed-in',action='store_true');args=p.parse_args()
PUBLIC=Path(__file__).resolve().parents[1]/'public'
state={'signed':args.signed_in}
terms={'intentions':[{'id':'7','title':'Peace'},{'id':'8','title':'Family'},{'id':'9','title':'Teachers'}],'patronages':[{'id':'1','title':'Artists'},{'id':'2','title':'Students'}]}
fields={
'saints':[('id','text'),('slug','text'),('name','text'),('feast_month','integer'),('feast_day','integer'),('image_url','text')]+[(f'{key}_{lang}','text')for key in ['name','feast_label','summary','biography']for lang in ['en','es','pl']],
'prayers':[('id','text'),('slug','text'),('category','text'),('image_url','text'),('source_title','text'),('source_type','text')]+[(f'{key}_{lang}','text')for key in ['title','body','alternate_title','note']for lang in ['en','es','pl']],
'novenas':[('id','text'),('slug','text'),('duration_days','integer'),('image_url','text')]+[(f'{key}_{lang}','text')for key in ['title','description']for lang in ['en','es','pl']],
'novena-days':[('id','bigint'),('novena_id','text'),('day_number','integer')]+[(f'{key}_{lang}','text')for key in ['title','scripture','prayer','reflection','body']for lang in ['en','es','pl']],
'intentions':[('id','bigint'),('slug','text')]+[(f'label_{lang}','text')for lang in ['en','es','pl']],
'patronages':[('id','bigint'),('slug','text')]+[(f'label_{lang}','text')for lang in ['en','es','pl']]}
NOW='2026-11-10T12:00:00Z'
def session():return{'email':'owner@example.test','expiresAt':(datetime.now(timezone.utc)+timedelta(hours=24)).isoformat(),'idleHours':8,'maximumHours':24}
def schema(kind):return[{'name':name,'type':typ,'required':name!='image_url','generated':name=='id' and typ=='bigint'}for name,typ in fields[kind]]
def detail(kind,id):
 values={name:'' for name,_ in fields[kind]}; values.update(id=id,slug=id,image_url='http://127.0.0.1:8080/fixture/'+('saints' if kind=='saints' else 'novenas' if kind=='novenas' else 'prayers')+'.svg')
 for lang in ['en','es','pl']:
  values['name_'+lang]='Saint of Hope'; values['title_'+lang]='A prayer for today'; values['biography_'+lang]='A synthetic biography for local visual testing. No real content or user data is modified.';values['summary_'+lang]='A life of faith and quiet kindness.';values['body_'+lang]='A synthetic prayer for visual testing.';values['label_'+lang]='Peace'
 values={key:value for key,value in values.items() if key in dict(fields[kind])}
 if kind=='saints':values.update(name='Saint of Hope',feast_month=11,feast_day=10)
 if kind=='novenas':values.update(duration_days=30,title_en='TEST PREVIEW — 30-day novena')
 if kind=='prayers':values.update(category='prayer',source_title='Synthetic fixture',source_type='local')
 if kind=='novena-days':
  values.update(novena_id='novenas-0',day_number=int(id))
  for lang in ['en','es','pl']:values['title_'+lang]=''
 links={'tags':['hope','daily']}
 if kind in ['saints','novenas']:links['intentions']=['7']
 if kind in ['saints','novenas']:links['patronages']=['1']
 if kind in ['intentions','patronages','novena-days']:links={}
 related={key:[row for row in terms[key] if row['id'] in ids]for key,ids in links.items() if key in terms}
 return{'values':values,'version':'synthetic-v1','links':links,'related':related}
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def cors(self):
  origin=self.headers.get('Origin')
  if origin in ['http://127.0.0.1:4300','http://localhost:4300','http://127.0.0.1:4200','http://localhost:4200']:
   self.send_header('Access-Control-Allow-Origin',origin);self.send_header('Access-Control-Allow-Credentials','true');self.send_header('Access-Control-Allow-Headers','content-type,x-sanctuary-admin,x-sanctuary-admin-client');self.send_header('Access-Control-Allow-Methods','GET,POST,PUT,OPTIONS')
 def output(self,data,status=200):
  self.send_response(status);self.cors();self.send_header('Content-Type','application/json');self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(json.dumps(data).encode())
 def do_OPTIONS(self):self.output({})
 def do_GET(self):
  path=urllib.parse.urlparse(self.path).path;q=urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
  if path.startswith('/fixture/'):
   file=PUBLIC/'home-cards'/Path(path).name
   if not file.is_file():self.output({},404);return
   self.send_response(200);self.cors();self.send_header('Content-Type','image/svg+xml');self.end_headers();self.wfile.write(file.read_bytes());return
  if path=='/admin-api/session':self.output(session() if state['signed'] else {'message':'No synthetic session'},200 if state['signed'] else 401);return
  if path.startswith('/admin-api/schema/'):self.output(schema(path.split('/')[-1]));return
  if path=='/admin-api/directory':
   people=[{'id':'one','profile_id':'fixture-profile','display_name':'Anna Example','email':'anna@example.test','status':'CONFIRMED','email_verified':True,'enabled':True,'registered_at':NOW,'last_seen_at':NOW,'last_login_at':NOW,'login_count':12,'saved':5,'active_novenas':1,'completed_novenas':2},{'id':'two','profile_id':None,'display_name':'Daniel Example','email':'daniel@example.test','status':'UNCONFIRMED','email_verified':False,'enabled':True,'registered_at':NOW,'last_seen_at':None,'last_login_at':None,'login_count':0,'saved':0,'active_novenas':0,'completed_novenas':0}]
   query=q.get('q',[''])[0].lower();people=[row for row in people if query in (row['display_name']+row['email']).lower()]
   self.output({'items':people,'hasMore':False,'syncedAt':NOW,'summary':{'registrations':2,'verified':1,'active_7d':1,'active_30d':1,'active_novenas':1,'completed_novenas':2}});return
  if path.startswith('/admin-api/users/'):
   self.output({'profile':{'email':'anna@example.test','display_name':'Anna Example','first_name':'Anna','last_name':'Example','preferred_language':'en','created_at':NOW,'last_login_at':NOW,'last_seen_at':NOW,'login_count':12},'favorites':[{'title':'Prayer of Hope','item_type':'prayer','created_at':NOW}],'novenas':[{'title':'Nine Days of Hope','status':'active','current_day':3,'completed_days':'[1,2]','started_at':NOW,'updated_at':NOW}],'events':[{'activity_type':'login','resource_type':'account','occurred_at':NOW,'metadata':'{}'}],'hasMore':False});return
  if path=='/admin-api/audit':self.output({'items':[{'id':1,'entity':'saints','record_id':'saint-hope','reason':'Synthetic preview history','created_at':NOW}],'hasMore':False});return
  if re.match(r'/admin-api/novenas/[^/]+/days$',path):self.output({'items':[{'id':str(day),'day_number':day,'title':''}for day in range(1,31)]});return
  match=re.match(r'/admin-api/content/([^/]+)(?:/(.+))?$',path)
  if match:
   kind,id=match.groups()
   if id:self.output(detail(kind,id));return
   if kind in terms:
    query=q.get('q',[''])[0].lower();self.output({'items':[row for row in terms[kind] if query in row['title'].lower()],'hasMore':False});return
   names=['Saint of Hope','A Life of Kindness','A Quiet Courage','A Prayerful Heart'] if kind=='saints' else ['TEST PREVIEW — 30-day novena'] if kind=='novenas' else ['Prayer of Hope','Morning Offering','A Moment of Gratitude','Prayer for Peace']
   self.output({'items':[{'id':kind+'-'+str(i),'title':name,'image_url':'http://127.0.0.1:8080/fixture/'+('saints' if kind=='saints' else 'novenas' if kind=='novenas' else 'prayers')+'.svg'}for i,name in enumerate(names)],'hasMore':False});return
  self.output({},404)
 def do_POST(self):
  path=urllib.parse.urlparse(self.path).path
  raw=self.rfile.read(int(self.headers.get('Content-Length',0)))
  body=json.loads(raw or b'{}') if 'application/json' in self.headers.get('Content-Type','') else {}
  if path.endswith('/login'):self.output({'challenge':'synthetic-challenge','mode':'password','secret':''});return
  if path.endswith('/password'):self.output({'challenge':'synthetic-challenge','mode':'enroll','secret':'JBSWY3DPEHPK3PXP'});return
  if path.endswith('/verify'):state['signed']=True;self.output(session());return
  if path.endswith('/logout'):state['signed']=False;self.output({});return
  if path.endswith('/images'):self.output({'path':'/fixture/saints.svg'});return
  if '/terms/' in path:
   kind=path.split('/')[-1];row={'id':str(len(terms[kind])+20),'title':body['labels']['label_en']};terms[kind].append(row);self.output({'values':{'id':row['id'],**body['labels']},'version':'synthetic-new'});return
  if '/content/' in path:self.output({'values':{**body['values'],'id':body['values'].get('id','synthetic')},'links':body.get('links',{}),'related':{},'version':'synthetic-saved'});return
  self.output({},404)
 do_PUT=do_POST
print('Synthetic preview only: http://127.0.0.1:8080 — no real accounts or content',flush=True)
ThreadingHTTPServer(('127.0.0.1',8080),Handler).serve_forever()
