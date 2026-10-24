const https = require('node:https');
const http = require('node:http');

function jsonResponse(statusCode, payload) {
  return {
    statusCode,
    body: JSON.stringify(payload),
  };
}

function parseRequest(event) {
  if (typeof event?.body !== 'string') return event ?? {};

  try {
    return JSON.parse(event.body);
  } catch {
    return {};
  }
}

function readWanAndroidUser(cookie) {
  return new Promise((resolve, reject) => {
    const request = https.get(
      'https://www.wanandroid.com/user/lg/userinfo/json',
      { headers: { Cookie: cookie } },
      (response) => {
        let raw = '';
        response.setEncoding('utf8');
        response.on('data', (chunk) => { raw += chunk; });
        response.on('end', () => {
          if (response.statusCode !== 200) {
            reject(new Error('WanAndroid 用户信息请求失败'));
            return;
          }

          try {
            const payload = JSON.parse(raw);
            const user = payload?.data?.userInfo;
            if (payload?.errorCode !== 0 || !Number.isInteger(user?.id)) {
              reject(new Error('WanAndroid 登录态无效'));
              return;
            }
            resolve({ id: user.id });
          } catch {
            reject(new Error('WanAndroid 用户信息解析失败'));
          }
        });
      },
    );
    request.on('error', () => reject(new Error('WanAndroid 用户信息请求失败')));
  });
}

function createProductionTicketIssuer() {
  const cloudbase = require('@cloudbase/node-sdk');
  const credentials = require('./tcb_custom_login.json');
  const app = cloudbase.init({ env: credentials.env_id, credentials });

  return async (uid) => app.auth().createTicket(uid);
}

function createIssueTicketHandler({ fetchWanAndroidUser, createTicket }) {
  return async (event = {}) => {
    const request = parseRequest(event);
    const cookie = request.cookie;

    if (typeof cookie !== 'string' || cookie.trim().length === 0) {
      return jsonResponse(401, { error: '未检测到登录态' });
    }

    try {
      const user = await fetchWanAndroidUser(cookie);
      if (!Number.isInteger(user?.id)) throw new Error('WanAndroid 用户无效');

      const ticket = await createTicket(`wan_${user.id}`);
      return jsonResponse(200, { ticket });
    } catch {
      return jsonResponse(401, { error: 'Hash 登录态已失效，请重新登录' });
    }
  };
}

let productionHandler;

function getProductionHandler() {
  if (!productionHandler) {
    productionHandler = createIssueTicketHandler({
      fetchWanAndroidUser: readWanAndroidUser,
      createTicket: createProductionTicketIssuer(),
    });
  }
  return productionHandler;
}

function writeHttpResponse(response, statusCode, payload) {
  response.writeHead(statusCode, {
    'content-type': 'application/json; charset=utf-8',
    'access-control-allow-origin': '*',
  });
  response.end(JSON.stringify(payload));
}

function createHttpServer(handler) {
  return http.createServer((request, response) => {
    if (request.method === 'OPTIONS') {
      response.writeHead(204, {
        'access-control-allow-origin': '*',
        'access-control-allow-methods': 'POST, OPTIONS',
        'access-control-allow-headers': 'content-type',
      });
      response.end();
      return;
    }

    if (request.method !== 'POST') {
      writeHttpResponse(response, 405, { error: '仅支持 POST 请求' });
      return;
    }

    let rawBody = '';
    request.setEncoding('utf8');
    request.on('data', (chunk) => {
      rawBody += chunk;
      if (rawBody.length > 16 * 1024) {
        request.destroy();
        writeHttpResponse(response, 413, { error: '请求体过大' });
      }
    });
    request.on('end', async () => {
      if (response.writableEnded) return;
      const result = await handler({ body: rawBody });
      writeHttpResponse(response, result.statusCode, JSON.parse(result.body));
    });
    request.on('error', () => {
      if (!response.writableEnded) {
        writeHttpResponse(response, 400, { error: '请求无效' });
      }
    });
  });
}

function start() {
  const port = Number(process.env.PORT || 9000);
  const server = createHttpServer(getProductionHandler());
  server.listen(port);
  return server;
}

if (require.main === module) {
  start();
}

exports.createHttpServer = createHttpServer;
exports.createIssueTicketHandler = createIssueTicketHandler;
