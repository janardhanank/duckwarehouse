import test from 'node:test';
import assert from 'node:assert/strict';
import { validateDuckForm } from '../src/lib/duckForm.js';

const valid = { color: 'Red', size: 'Large', price: '10.00', quantity: '5' };
test('add preserves exact decimal strings and emits integer quantity', () => {
  assert.deepEqual(validateDuckForm(valid).payload, { color: 'Red', size: 'Large', price: '10.00', quantity: 5 });
});
test('edit sends only price and quantity, and allows zero stock', () => {
  assert.deepEqual(validateDuckForm({ ...valid, quantity: '0' }, true).payload, { price: '10.00', quantity: 0 });
  assert.ok(validateDuckForm({ ...valid, quantity: '0' }).errors.quantity);
});
test('invalid precision, price limits, and zero prices are rejected', () => {
  for (const price of ['0', '0.00', '1.001', '-1', '1e3', '10000000000', 'Infinity', 'abc']) {
    assert.ok(validateDuckForm({ ...valid, price }).errors.price, price);
  }
  for (const price of ['0.01', '9999999999.99']) assert.equal(validateDuckForm({ ...valid, price }).payload.price, price);
});
test('fractional and out-of-range quantities are rejected', () => {
  for (const quantity of ['1.5', '-1', '2147483648', '1e3', '', 'Infinity']) {
    assert.ok(validateDuckForm({ ...valid, quantity }).errors.quantity, quantity);
  }
  assert.equal(validateDuckForm({ ...valid, quantity: '2147483647' }).payload.quantity, 2147483647);
});
test('invalid warehouse labels are rejected before add', () => {
  assert.ok(validateDuckForm({ ...valid, color: 'Blue' }).errors.color);
  assert.ok(validateDuckForm({ ...valid, size: 'Tiny' }).errors.size);
});
