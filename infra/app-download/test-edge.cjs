const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const context = {};
vm.runInNewContext(fs.readFileSync(__dirname + '/edge.js', 'utf8'), context);
function run(ua, uri = '/app', method = 'GET', querystring = {}) {
  const request = {uri,method,headers:ua === null ? {} : {'user-agent':{value:ua}},querystring};
  return {request,response:context.handler({version:'1.0',context:{eventType:'viewer-request'},viewer:{ip:'203.0.113.1'},request})};
}
for(const ua of ['Mozilla iPhone','Mozilla iPad','Mozilla iPod']) {
  for(const uri of ['/app','/app/']) {
    const {response:r} = run(ua,uri);
    assert.equal(r.statusCode,302);
    assert.equal(r.headers.location.value,'https://apps.apple.com/us/app/sanctuary-daily-companion/id6759986068');
    assert.match(r.headers['cache-control'].value,/no-store/);
    assert.equal(r.body,undefined);
  }
}
let r=run('Mozilla Android').response;
assert.equal(r.statusCode,302);assert.equal(r.headers.location.value,'https://play.google.com/store/apps/details?id=com.pamisu.sanctuary&hl=en_US');
for(const ua of [null,'','Mozilla Windows','Mozilla Macintosh']) {
  r=run(ua).response;assert.equal(r.statusCode,200);assert.match(r.body.data,/apps.apple.com/);assert.match(r.body.data,/play.google.com/);assert.equal(r.headers.location,undefined);
}
for(const uri of ['/','/news','/admin','/application','/app/download.js','/saints/1']) {
  const {request,response} = run('iPhone',uri);assert.equal(response,request);
}
r=run('Android','/app','GET',{url:{value:'https://example.invalid'}}).response;
assert.match(r.headers.location.value,/^https:\/\/play.google.com\//);
for(const ua of ['iPhone','Android','unknown']) assert.equal(run(ua,'/app','HEAD').response.body,undefined);
for(const method of ['POST','PUT','DELETE','OPTIONS']) {
  r=run('iPhone','/app',method).response;assert.equal(r.statusCode,405);assert.equal(r.headers.allow.value,'GET, HEAD');
}
assert.ok(fs.statSync(__dirname+'/edge.js').size<10000);
console.log('Edge redirect tests passed: store selection, fallback, exact paths, pass-through, HEAD, methods, and fixed destinations.');
