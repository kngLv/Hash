const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

test('HTTP 云函数代码包包含 CloudBase 所需的启动文件', () => {
  const bootstrapPath = path.resolve(__dirname, '..', 'scf_bootstrap');

  assert.equal(fs.existsSync(bootstrapPath), true);
  assert.equal(fs.readFileSync(bootstrapPath, 'utf8'), '#!/bin/bash\nnode index.js\n');
});
