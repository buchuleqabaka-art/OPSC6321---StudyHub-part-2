const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readConfig } = require('../../server/config.cjs');
test('supports application default credentials and valid host port', () => {
  assert.deepEqual(readConfig({}), {port:8080,serviceAccount:null});
  assert.equal(readConfig({PORT:'9000'}).port,9000);
});
test('rejects invalid ports without logging secrets', () => {
  for (const PORT of ['-1','0','65536','9000oops']) assert.throws(() => readConfig({PORT}), /PORT must/);
});
test('validates service-account configuration without exposing values', () => {
  const account={project_id:'demo',client_email:'test@example.com',private_key:'test-only'};
  assert.deepEqual(readConfig({FIREBASE_SERVICE_ACCOUNT:JSON.stringify(account)}).serviceAccount,account);
  assert.throws(()=>readConfig({FIREBASE_SERVICE_ACCOUNT:'secret-invalid-json'}),/^Error: FIREBASE_SERVICE_ACCOUNT must contain valid JSON$/);
  for (const value of ['null','{}','[]','{"project_id":"demo"}']) assert.throws(()=>readConfig({FIREBASE_SERVICE_ACCOUNT:value}),/needs project_id/);
});
