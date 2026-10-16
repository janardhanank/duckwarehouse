import test from 'node:test';
import assert from 'node:assert/strict';
import { ducksApi } from '../src/api/ducks.js';

test('edit uses PUT and does not change exact money strings', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/ducks/7');
      assert.equal(options.method, 'PUT');
      assert.deepEqual(JSON.parse(options.body), { price: '9999999999.99', quantity: 4 });
      return new Response('{"id":7}', { status: 200 });
    };
    assert.equal((await ducksApi.edit(7, { price: '9999999999.99', quantity: 4 })).id, 7);
  } finally { globalThis.fetch = original; }
});
test('backend field errors remain available to the form', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Invalid request', fields: { quantity: 'Invalid quantity' } }), { status: 400 });
    await assert.rejects(ducksApi.add({}), error => error.status === 400 && error.fields.quantity === 'Invalid quantity');
  } finally { globalThis.fetch = original; }
});
test('an interrupted write is never retried automatically', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  try {
    globalThis.fetch = async () => { calls++; throw new TypeError('Network failure'); };
    await assert.rejects(ducksApi.add({}), /check whether the change completed/);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});
test('logical deletion handles an empty 204 response', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async () => new Response(null, { status: 204 });
    assert.equal(await ducksApi.remove(1), null);
  } finally { globalThis.fetch = original; }
});
