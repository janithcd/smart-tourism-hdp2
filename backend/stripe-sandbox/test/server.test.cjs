'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { Readable } = require('node:stream');
const { createServer, quote, HttpError } = require('../server');

const booking = {
  type: 'Tour', listingId: 'tour1', travelers: 2, pickup: 'Colombo',
  mobile: '+94770000000', date: '2099-01-01', time: '10:00', price: 0.01
};

function fixture() {
  const records = new Map([['packages/tour1', {
    active: true, title: 'Kandy tour', price: 120, duration: '2 days', route: 'Colombo to Kandy'
  }]]);
  let session;
  const ref = (collection, id) => {
    const key = `${collection}/${id}`;
    return {
      id, get: async () => ({ id, exists: records.has(key), data: () => records.get(key) }),
      set: async value => records.set(key, value),
      update: async value => records.set(key, { ...records.get(key), ...value })
    };
  };
  const db = {
    collection: name => ({ doc: id => ref(name, id || 'OrderABC123') }),
    runTransaction: async fn => fn({
      get: target => target.get(),
      update: (target, value) => target.update(value),
      set: (target, value) => target.set(value)
    })
  };
  const stripe = {
    checkout: { sessions: {
      create: async input => {
        assert.equal(input.line_items[0].price_data.unit_amount, 12000);
        session = {
          id: 'cs_test_123', url: 'https://checkout.stripe.com/c/pay/cs_test_123',
          livemode: false, mode: 'payment', payment_status: 'unpaid', status: 'open',
          metadata: { orderId: input.client_reference_id },
          client_reference_id: input.client_reference_id, amount_total: 12000,
          currency: 'usd', payment_intent: 'pi_test_123'
        };
        return session;
      },
      retrieve: async () => session,
      expire: async () => {}
    } },
    webhooks: { constructEvent: (body, signature, secret) => {
      if (signature !== 'valid' || secret !== 'whsec_test') throw Error('Invalid signature');
      return JSON.parse(body.toString());
    } }
  };
  const server = createServer({ stripe, db,
    auth: { verifyIdToken: async token => {
      if (token !== 'alice' && token !== 'bob') throw Error('Invalid token');
      return { uid: token, email: `${token}@example.com` };
    } },
    serverTimestamp: () => 'timestamp', webhookSecret: 'whsec_test',
    baseUrl: 'http://localhost:4242' });
  return { records, server, getSession: () => session };
}

async function withServer(run) {
  const f = fixture();
  const request = (path, options = {}) => new Promise((resolve, reject) => {
    const input = Readable.from([Buffer.from(options.body || '')]);
    input.url = path;
    input.method = options.method || 'GET';
    input.headers = Object.fromEntries(Object.entries(options.headers || {})
      .map(([name, value]) => [name.toLowerCase(), value]));
    const response = {
      headersSent: false,
      writeHead(status) { this.status = status; this.headersSent = true; },
      end(body) { resolve({ status: this.status, json: async () => JSON.parse(body) }); }
    };
    f.server.listeners('request')[0](input, response).catch(reject);
  });
  await run(f, request);
}

function start(request, data = booking, token = 'alice') {
  return request('/checkout/start', { method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(data) });
}

test('a server quote ignores the client price and rejects hidden listings or excess travelers', () => {
  const listing = { active: true, title: 'Kandy tour', price: 120 };
  assert.equal(quote(listing, 'Tour', booking).amountCents, 12000);
  assert.throws(() => quote({ ...listing, active: false }, 'Tour', booking), HttpError);
  assert.throws(() => quote(listing, 'Tour', { ...booking, travelers: 9 }), HttpError);
  assert.throws(() => quote(listing, 'Tour', { ...booking, date: '2099-02-31' }), HttpError);
});

test('Firebase authentication, ownership, and published listing are enforced', async () => {
  await withServer(async ({ records }, request) => {
    assert.equal((await start(request, booking, 'invalid')).status, 401);
    assert.equal((await start(request, { ...booking, listingId: 'missing' })).status, 400);
    const created = await start(request);
    assert.equal(created.status, 200);
    const { orderId, amountCents } = await created.json();
    assert.equal(amountCents, 12000);
    assert.equal(records.get(`stripe_sandbox_orders/${orderId}`).uid, 'alice');
    const status = await request(`/checkout/status/${orderId}`, {
      headers: { Authorization: 'Bearer bob' }
    });
    assert.equal(status.status, 404);
    assert.equal((await request(`/checkout/status/${orderId}`, {
      headers: { Authorization: 'Bearer alice' }
    })).status, 200);
  });
});

test('only a verified paid matching Stripe test session creates one server booking', async () => {
  await withServer(async ({ records, getSession }, request) => {
    const { orderId } = await (await start(request)).json();
    const session = getSession();
    const event = object => ({ type: 'checkout.session.completed', data: { object } });
    async function webhook(body, signature = 'valid') {
      return request('/webhook', { method: 'POST',
        headers: { 'stripe-signature': signature }, body: JSON.stringify(body) });
    }
    assert.equal((await webhook(event({ ...session, payment_status: 'paid' }), 'bad')).status, 400);
    assert.equal(records.has(`demo_bookings/${orderId}`), false);
    assert.equal((await webhook(event({ ...session, payment_status: 'paid',
      amount_total: 1 }))).status, 400);
    assert.equal((await webhook(event({ ...session, payment_status: 'paid',
      livemode: true }))).status, 200);
    assert.equal(records.has(`demo_bookings/${orderId}`), false);
    session.payment_status = 'paid';
    assert.equal((await webhook(event(session))).status, 200);
    assert.equal((await webhook(event(session))).status, 200);
    assert.equal(records.get(`demo_bookings/${orderId}`).source, 'stripe_test');
    assert.equal(records.get(`stripe_sandbox_orders/${orderId}`).status, 'paid');
    const status = await request(`/checkout/status/${orderId}`, {
      headers: { Authorization: 'Bearer alice' }
    });
    const result = await status.json();
    assert.equal(result.status, 'paid');
    assert.equal(result.title, 'Kandy tour');
    assert.equal(result.amountCents, 12000);
  });
});

test('status polling verifies paid Stripe session even before a webhook arrives', async () => {
  await withServer(async ({ records, getSession }, request) => {
    const { orderId } = await (await start(request)).json();
    getSession().payment_status = 'paid';
    const response = await request(`/checkout/status/${orderId}`, {
      headers: { Authorization: 'Bearer alice' }
    });
    assert.equal((await response.json()).status, 'paid');
    assert.equal(records.get(`demo_bookings/${orderId}`).source, 'stripe_test');
  });
});
