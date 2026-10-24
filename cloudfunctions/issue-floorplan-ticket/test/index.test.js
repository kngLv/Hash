const test = require('node:test');
const assert = require('node:assert/strict');
const { createHttpServer, createIssueTicketHandler } = require('../index.js');

test('有效 Cookie 会为 WanAndroid 用户签发稳定 Ticket', async () => {
  const handler = createIssueTicketHandler({
    fetchWanAndroidUser: async (cookie) => {
      assert.equal(cookie, 'loginUserName=test; token_pass=test');
      return { id: 12345 };
    },
    createTicket: async (uid) => {
      assert.equal(uid, 'wan_12345');
      return 'signed-ticket';
    },
  });

  assert.deepEqual(
    await handler({ body: JSON.stringify({ cookie: 'loginUserName=test; token_pass=test' }) }),
    { statusCode: 200, body: JSON.stringify({ ticket: 'signed-ticket' }) },
  );
});

test('未携带 Cookie 返回 401 且不签发 Ticket', async () => {
  const handler = createIssueTicketHandler({
    fetchWanAndroidUser: async () => { throw new Error('不应调用'); },
    createTicket: async () => { throw new Error('不应调用'); },
  });

  const response = await handler({ body: '{}' });
  assert.equal(response.statusCode, 401);
  assert.deepEqual(JSON.parse(response.body), { error: '未检测到登录态' });
});

test('HTTP 服务拒绝非 POST 请求', async () => {
  const server = createHttpServer(async () => ({
    statusCode: 200,
    body: JSON.stringify({ ticket: 'signed-ticket' }),
  }));
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));

  try {
    const { port } = server.address();
    const response = await fetch(`http://127.0.0.1:${port}`);
    assert.equal(response.status, 405);
    assert.deepEqual(await response.json(), { error: '仅支持 POST 请求' });
  } finally {
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  }
});
