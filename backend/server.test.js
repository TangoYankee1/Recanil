const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('./server');

async function fixture(t, overrides = {}) {
  const calls = [];
  const customers = [];
  const accounts = new Map();
  const auth = {
    async signUp(input) {
      calls.push(['signup', input]);
      if (accounts.has(input.email)) return { error: { code: 'user_already_exists', status: 422 } };
      accounts.set(input.email, input.password);
      return { data: { user: { id: 'user-1' }, session: {} }, error: null };
    },
    async signInWithPassword(input) {
      calls.push(['login', input]);
      return accounts.get(input.email) === input.password
        ? { data: { session: { access_token: 'user-token' } }, error: null }
        : { error: { code: 'invalid_credentials', status: 400 } };
    },
    ...overrides
  };
  const admin = {
    auth: { getUser: async token => token === 'user-token'
      ? { data: { user: { id: 'user-1' } }, error: null }
      : { data: { user: null }, error: { message: 'expired', status: 401 } } },
    from(table) {
      assert.equal(table, 'customer');
      return {
        select() { return { order() { return { async range(start, end) {
          return { data: customers.slice(start, end + 1), error: null };
        } }; } }; },
        insert(customer) { return { select() { return { async single() {
          const row = { ...customer, customerid: customers.length + 1 };
          customers.push(row);
          return { data: row, error: null };
        } }; } }; }
      };
    }
  };
  const server = createApp({ admin, authClient: () => ({ auth }) }).listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const url = `http://127.0.0.1:${server.address().port}`;
  async function request(path, method = 'GET', body, token) {
    const response = await fetch(url + path, {
      method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    });
    return { status: response.status, body: await response.json() };
  }
  return { request, calls, customers };
}

const account = { username: 'Student', email: 'student@example.com', password: ' Correct123! ' };
const customer = { lastname: 'Dela Cruz', firstname: 'Juan', mobileno: '09171234567', address: 'Manila' };

test('signup, login, add customer, and list use the same contracts as Android', async t => {
  const { request } = await fixture(t);
  assert.equal((await request('/signup', 'POST', account)).status, 201);
  const login = await request('/login', 'POST', account);
  assert.equal(login.status, 200);
  assert.equal(login.body.token, 'user-token');
  assert.equal('password' in login.body, false);
  const saved = await request('/customers', 'POST', customer, login.body.token);
  assert.equal(saved.status, 201);
  assert.equal(saved.body.customerid, 1);
  assert.equal(saved.body.mobileno, '09171234567');
  const list = await request('/customers?offset=0', 'GET', undefined, login.body.token);
  assert.deepEqual(list.body, [saved.body]);
});

test('normalizes email while preserving password spaces and rejects duplicate accounts', async t => {
  const { request, calls } = await fixture(t);
  await request('/signup', 'POST', { ...account, email: ' STUDENT@example.com ' });
  assert.equal(calls[0][1].email, account.email);
  assert.equal(calls[0][1].password, account.password);
  assert.equal((await request('/signup', 'POST', account)).status, 409);
  assert.equal((await request('/login', 'POST', { ...account, password: account.password.trim() })).status, 401);
});

test('missing and expired customer tokens are rejected before accessing the database', async t => {
  const { request, customers } = await fixture(t);
  assert.equal((await request('/customers')).status, 401);
  assert.equal((await request('/customers', 'POST', customer, 'expired')).status, 401);
  assert.equal(customers.length, 0);
});

test('invalid signup and customer fields do not write data', async t => {
  const { request, calls, customers } = await fixture(t);
  assert.equal((await request('/signup', 'POST', { ...account, password: '123' })).status, 400);
  assert.equal(calls.length, 0);
  assert.equal((await request('/customers', 'POST', { ...customer, mobileno: 9171234567 }, 'user-token')).status, 400);
  assert.equal((await request('/customers', 'POST', { ...customer, address: ' ' }, 'user-token')).status, 400);
  assert.equal(customers.length, 0);
});

test('customer ID is generated and additional client fields are ignored', async t => {
  const { request } = await fixture(t);
  const result = await request('/customers', 'POST', { ...customer, customerid: 999, owner: 'someone' }, 'user-token');
  assert.equal(result.body.customerid, 1);
  assert.equal('owner' in result.body, false);
});

test('lists are paginated and invalid offsets are rejected', async t => {
  const { request, customers } = await fixture(t);
  for (let i = 1; i <= 51; i++) customers.push({ ...customer, customerid: i });
  assert.equal((await request('/customers?offset=0', 'GET', undefined, 'user-token')).body.length, 50);
  assert.equal((await request('/customers?offset=50', 'GET', undefined, 'user-token')).body[0].customerid, 51);
  assert.equal((await request('/customers?offset=-1', 'GET', undefined, 'user-token')).status, 400);
});

test('upstream failures return a safe error rather than leaking credentials', async t => {
  const { request } = await fixture(t, { signUp: async () => { throw new Error('private upstream detail'); } });
  const result = await request('/signup', 'POST', account);
  assert.equal(result.status, 503);
  assert.equal(JSON.stringify(result.body).includes('private upstream detail'), false);
});

test('old plaintext user endpoint is removed', async t => {
  const { request } = await fixture(t);
  assert.equal((await request('/users')).status, 404);
});
