const express = require('express');
const { createClient } = require('@supabase/supabase-js');

const COLUMNS = 'customerid,lastname,firstname,mobileno,address';
const PAGE_SIZE = 50;
const emailOf = value => typeof value === 'string' ? value.trim().toLowerCase() : '';
const validEmail = email => email.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const fail = (res, status, message) => res.status(status).json({ success: false, message });

// Inject clients in tests. A new auth client per request prevents one user's
// sign-in from replacing the shared administrator client's database credentials.
function createApp({ admin, authClient }) {
  const app = express();
  app.disable('x-powered-by');
  app.use(express.json({ limit: '16kb' }));
  app.get('/', (req, res) => res.json({ message: 'Recanil API is running', version: 2 }));

  app.post('/signup', async (req, res) => {
    const email = emailOf(req.body?.email);
    const password = req.body?.password;
    const username = typeof req.body?.username === 'string' ? req.body.username.trim() : '';
    if (!validEmail(email) || !username || username.length > 100
        || typeof password !== 'string' || password.length < 6 || password.length > 128) {
      return fail(res, 400, 'Enter a username, valid email, and password of 6 to 128 characters.');
    }
    const { data, error } = await authClient().auth.signUp({
      email, password, options: { data: { username } }
    });
    if (error) {
      if (!error.status || error.status >= 500) {
        return fail(res, 503, 'Account service is unavailable. Please try again.');
      }
      const duplicate = ['user_already_exists', 'email_exists'].includes(error.code);
      return fail(res, duplicate ? 409 : error.status === 429 ? 429 : 400,
        duplicate ? 'An account with this email already exists.'
          : error.status === 429 ? 'Too many attempts. Please try again later.'
          : 'Unable to create account. Check your email and password requirements.');
    }
    if (!data.user) return fail(res, 502, 'Account service returned an incomplete response.');
    // With Confirm email enabled, Supabase can intentionally conceal duplicates.
    res.status(201).json({ success: true, message: data.session
      ? 'Account created. You can now log in.'
      : 'Check your email to confirm your account before logging in.' });
  });

  app.post('/login', async (req, res) => {
    const email = emailOf(req.body?.email);
    const password = req.body?.password;
    if (!validEmail(email) || typeof password !== 'string' || !password || password.length > 128) {
      return fail(res, 400, 'Enter a valid email and password.');
    }
    const { data, error } = await authClient().auth.signInWithPassword({ email, password });
    if (error && (!error.status || error.status >= 500)) {
      return fail(res, 503, 'Account service is unavailable. Please try again.');
    }
    if (error) return fail(res, error.status === 429 ? 429 : 401,
      error.status === 429 ? 'Too many attempts. Please try again later.'
        : error.code === 'email_not_confirmed' ? 'Confirm your email before logging in.'
        : 'Invalid email or password');
    if (!data.session?.access_token) return fail(res, 502, 'Account service returned an incomplete response.');
    res.json({ success: true, message: 'Login successful!', token: data.session.access_token });
  });

  app.use('/customers', async (req, res, next) => {
    const match = /^Bearer (\S+)$/i.exec(req.get('Authorization') || '');
    if (!match) return fail(res, 401, 'Please log in again.');
    const { data, error } = await admin.auth.getUser(match[1]);
    if (error && (!error.status || error.status >= 500)) {
      return fail(res, 503, 'Account service is unavailable. Please try again.');
    }
    if (error || !data.user) return fail(res, 401, 'Session expired. Please log in again.');
    next();
  });

  app.get('/customers', async (req, res) => {
    const offset = Number(req.query.offset ?? 0);
    if (!Number.isSafeInteger(offset) || offset < 0 || offset > 1000000) {
      return fail(res, 400, 'Invalid customer list offset.');
    }
    const { data, error } = await admin.from('customer').select(COLUMNS)
      .order('customerid', { ascending: true }).range(offset, offset + PAGE_SIZE - 1);
    if (error) return fail(res, 502, 'Unable to load customers. Check the database setup.');
    res.json(data);
  });

  app.post('/customers', async (req, res) => {
    const customer = {};
    for (const [key, max] of Object.entries({ lastname: 100, firstname: 100, mobileno: 30, address: 500 })) {
      const value = req.body?.[key];
      if (typeof value !== 'string' || !value.trim() || value.trim().length > max) {
        return fail(res, 400, `${key} is required and must contain at most ${max} characters.`);
      }
      customer[key] = value.trim();
    }
    const { data, error } = await admin.from('customer').insert(customer).select(COLUMNS).single();
    if (error) return fail(res, 502, 'Unable to save customer. Check the database setup.');
    res.status(201).json(data);
  });

  // The old /users route exposed plaintext passwords; it is intentionally removed.
  app.use((req, res) => fail(res, 404, 'Endpoint not found.'));
  app.use((error, req, res, next) => {
    if (error.type === 'entity.parse.failed') return fail(res, 400, 'Request body must be valid JSON.');
    if (error.type === 'entity.too.large') return fail(res, 413, 'Request body is too large.');
    fail(res, 503, 'Service temporarily unavailable. Please try again.');
  });
  return app;
}

if (require.main === module) {
  const url = process.env.SUPABASE_URL;
  const key = process.env.SUPABASE_SECRET_KEY;
  if (!url || !key) throw new Error('Set SUPABASE_URL and SUPABASE_SECRET_KEY in Render.');
  const options = { auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false } };
  const admin = createClient(url, key, options);
  const app = createApp({ admin, authClient: () => createClient(url, key, options) });
  app.listen(process.env.PORT || 3000, () => console.log('Recanil API is running'));
}

module.exports = { createApp };
