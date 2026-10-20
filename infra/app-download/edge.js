// Permanent printed entry point. Store targets stay server-controlled.
// CloudFront Functions runtime cloudfront-js-2.0; viewer-request event.
function handler(event) {
    var request = event.request;
    if (request.uri !== '/app' && request.uri !== '/app/') return request;
    var method = request.method || 'GET';
    var headers = {
        'cache-control': { value: 'private, no-store, max-age=0' },
        'vary': { value: 'User-Agent' },
        'x-content-type-options': { value: 'nosniff' },
        'referrer-policy': { value: 'no-referrer' }
    };
    if (method !== 'GET' && method !== 'HEAD') {
        headers.allow = { value: 'GET, HEAD' };
        return { statusCode: 405, statusDescription: 'Method Not Allowed', headers: headers };
    }
    var apple = 'https://apps.apple.com/us/app/sanctuary-daily-companion/id6759986068';
    var android = 'https://play.google.com/store/apps/details?id=com.pamisu.sanctuary&hl=en_US';
    var ua = request.headers && request.headers['user-agent'] ? request.headers['user-agent'].value : '';
    var target = /android/i.test(ua) ? android : /iPhone|iPad|iPod/i.test(ua) ? apple : null;
    if (target) {
        headers.location = { value: target };
        return { statusCode: 302, statusDescription: 'Found', headers: headers };
    }
    // A desktop-style iPad UA cannot reliably be distinguished from a Mac at the edge.
    // Unknown devices receive working buttons rather than a guessed destination.
    headers['content-type'] = { value: 'text/html; charset=utf-8' };
    headers['content-security-policy'] = { value: "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'" };
    var response = { statusCode: 200, statusDescription: 'OK', headers: headers };
    if (method !== 'HEAD') response.body = {
        encoding: 'text',
        data: '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><title>Get Sanctuary</title><style>body{margin:0;min-height:100vh;display:grid;place-items:center;background:#002650;color:#fff;font:18px/1.6 system-ui,sans-serif}main{max-width:460px;padding:32px;text-align:center}h1{font:36px/1.2 Georgia,serif;color:#F8D89A}a{color:#F8D89A}.button{display:block;margin:16px 0;padding:14px;border:1px solid #F0B058;border-radius:6px;text-decoration:none}a:focus-visible{outline:3px solid white;outline-offset:4px}</style><main><p>S A N C T U A R Y</p><h1>Make room for prayer.</h1><p>Choose your store to download Sanctuary: Daily Companion.</p><a class="button" href="'+apple+'">iPhone or iPad · App Store</a><a class="button" href="'+android.replace(/&/g,'&amp;')+'">Android · Google Play</a><p><a href="https://mydailysanctuary.com">Visit Sanctuary</a></p></main></html>'
    };
    return response;
}
