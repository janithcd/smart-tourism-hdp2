'use strict';

const http = require('node:http');

class HttpError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

function quote(listing, type, input) {
  if (!listing || listing.active !== true || typeof listing.title !== 'string'
      || !listing.title.trim() || typeof listing.price !== 'number'
      || !Number.isFinite(listing.price) || listing.price < 0.50
      || listing.price > 1000000 || (type !== 'Tour' && listing.type !== type)) {
    throw new HttpError(400, 'This published listing is unavailable for Stripe test checkout.');
  }
  const capacity = type === 'Tour' ? 8 : listing.capacity;
  if (!Number.isInteger(capacity) || capacity < 1 || capacity > 30
      || !Number.isInteger(input.travelers) || input.travelers < 1
      || input.travelers > capacity) {
    throw new HttpError(400, 'Traveler count exceeds this listing’s capacity.');
  }
  const dateTime = new Date(`${input.date}T${input.time}:00+05:30`);
  const dateParts = /^\d{4}-(\d{2})-(\d{2})$/.exec(input.date || '');
  const validDate = dateParts
    && new Date(Date.UTC(Number(input.date.slice(0, 4)), Number(dateParts[1]) - 1,
      Number(dateParts[2]))).toISOString().slice(0, 10) === input.date;
  if (typeof input.pickup !== 'string' || !input.pickup.trim() || input.pickup.length > 250
      || typeof input.mobile !== 'string' || !/^\+?[0-9 -]{8,40}$/.test(input.mobile)
      || typeof input.date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(input.date)
      || typeof input.time !== 'string' || !/^([01]\d|2[0-3]):[0-5]\d$/.test(input.time)
      || !validDate || !Number.isFinite(dateTime.getTime())
      || dateTime.getTime() <= Date.now()) {
    throw new HttpError(400, 'Enter a future travel time and valid contact details.');
  }
  return {
    title: listing.title.trim(),
    type,
    duration: listing.duration || '',
    route: type === 'Tour' ? (listing.route || '') : (listing.description || ''),
    amountCents: Math.round(listing.price * 100),
    travelers: input.travelers,
    pickup: input.pickup.trim(),
    mobile: input.mobile.trim(),
    date: input.date,
    time: input.time
  };
}

async function readBody(request, maxBytes = 16384) {
  const chunks = [];
  let length = 0;
  for await (const chunk of request) {
    length += chunk.length;
    if (length > maxBytes) throw new HttpError(413, 'Request too large.');
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}

function respond(response, status, value) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store' });
  response.end(JSON.stringify(value));
}

function createServer({ stripe, auth, db, serverTimestamp, webhookSecret, baseUrl }) {
  async function authenticated(request) {
    const match = /^Bearer ([A-Za-z0-9._-]+)$/.exec(request.headers.authorization || '');
    if (!match) throw new HttpError(401, 'Sign in to use Stripe test checkout.');
    let user;
    try { user = await auth.verifyIdToken(match[1]); }
    catch (_) { throw new HttpError(401, 'Your Firebase sign-in expired. Sign in again.'); }
    if (!user.uid || !user.email) throw new HttpError(401, 'An email account is required.');
    return user;
  }

  async function markPaid(session) {
    const id = session.metadata && session.metadata.orderId;
    if (!/^[A-Za-z0-9]{8,60}$/.test(id || '') || session.livemode !== false
        || session.mode !== 'payment' || session.payment_status !== 'paid') return false;
    const orderRef = db.collection('stripe_sandbox_orders').doc(id);
    const bookingRef = db.collection('demo_bookings').doc(id);
    await db.runTransaction(async transaction => {
      const snapshot = await transaction.get(orderRef);
      if (!snapshot.exists) throw new HttpError(404, 'Unknown checkout.');
      const order = snapshot.data();
      if (order.sessionId !== session.id || session.client_reference_id !== id
          || session.amount_total !== order.amountCents || session.currency !== 'usd') {
        throw new HttpError(400, 'Stripe test checkout does not match the server quote.');
      }
      if (order.status === 'paid') return;
      transaction.update(orderRef, { status: 'paid', paidAt: serverTimestamp(),
        paymentIntentId: typeof session.payment_intent === 'string'
          ? session.payment_intent : '' });
      transaction.set(bookingRef, {
        id, uid: order.uid, email: order.email, source: 'stripe_test',
        title: order.title, type: order.type, duration: order.duration,
        price: `$${(order.amountCents / 100).toFixed(2)} USD (test)`,
        route: order.route, pax: order.travelers === 1 ? '1 Person'
          : `${order.travelers} People`,
        pickup: order.pickup, mobile: order.mobile, date: order.date, time: order.time,
        paymentMethod: 'Stripe Checkout (test mode)',
        paymentStatus: 'Paid in test mode (no real money)',
        reviewStatus: 'New', createdAt: serverTimestamp()
      });
    });
    return true;
  }

  return http.createServer(async (request, response) => {
    try {
      const path = new URL(request.url, baseUrl).pathname;
      if (request.method === 'GET' && path === '/health') {
        return respond(response, 200, { mode: 'stripe_test', ready: true });
      }
      if (request.method === 'GET' && path === '/checkout/return') {
        response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8',
          'Cache-Control': 'no-store', 'Content-Security-Policy': "default-src 'none'; style-src 'unsafe-inline'" });
        response.end('<!doctype html><meta name="viewport" content="width=device-width, initial-scale=1">'
          + '<style>body{font:18px system-ui;margin:2rem;max-width:28rem}a{display:block;background:#2D628B;color:white;padding:1rem;border-radius:1rem;text-align:center}</style>'
          + '<h1>Stripe test checkout</h1><p>Return to Smart Tourism to verify the result. This test does not move real money or confirm a travel reservation.</p>'
          + '<a href="smarttourism://checkout/return">Return to app</a>');
        return;
      }
      if (request.method === 'POST' && path === '/webhook') {
        if (!webhookSecret) throw new HttpError(503, 'Webhook not configured.');
        const body = await readBody(request, 1024 * 1024);
        let event;
        try {
          event = stripe.webhooks.constructEvent(body, request.headers['stripe-signature'],
            webhookSecret);
        } catch (_) {
          throw new HttpError(400, 'Invalid Stripe signature.');
        }
        if (event.type === 'checkout.session.completed'
            || event.type === 'checkout.session.async_payment_succeeded') {
          if (event.data.object.payment_status === 'paid') await markPaid(event.data.object);
        }
        return respond(response, 200, { received: true });
      }
      if (request.method === 'POST' && path === '/checkout/start') {
        const user = await authenticated(request);
        let input;
        try { input = JSON.parse((await readBody(request)).toString('utf8')); }
        catch (error) {
          if (error instanceof HttpError) throw error;
          throw new HttpError(400, 'Invalid checkout request.');
        }
        if (!input || typeof input !== 'object' || Array.isArray(input))
          throw new HttpError(400, 'Invalid checkout request.');
        const type = input.type;
        const listingId = input.listingId;
        if (!['Tour', 'Vehicle', 'Guide'].includes(type)
            || typeof listingId !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(listingId)) {
          throw new HttpError(400, 'Choose a published Firestore listing.');
        }
        const collection = type === 'Tour' ? 'packages' : 'services';
        const listing = await db.collection(collection).doc(listingId).get();
        const priced = quote(listing.exists ? listing.data() : null, type, input);
        const orderRef = db.collection('stripe_sandbox_orders').doc();
        const id = orderRef.id;
        await orderRef.set({ ...priced, uid: user.uid, email: user.email,
          listingId, status: 'pending', createdAt: serverTimestamp() });
        let session;
        try {
          session = await stripe.checkout.sessions.create({
            mode: 'payment', payment_method_types: ['card'], client_reference_id: id,
            metadata: { orderId: id }, customer_email: user.email,
            line_items: [{ price_data: { currency: 'usd', unit_amount: priced.amountCents,
              product_data: { name: priced.title } }, quantity: 1 }],
            success_url: `${baseUrl}/checkout/return`,
            cancel_url: `${baseUrl}/checkout/return`
          });
          if (session.livemode !== false || !session.id.startsWith('cs_test_')
              || !session.url.startsWith('https://checkout.stripe.com/')) {
            throw new Error('Only hosted test Checkout sessions are accepted.');
          }
          await orderRef.update({ sessionId: session.id });
        } catch (error) {
          await orderRef.update({ status: 'failed' });
          if (session && session.id) {
            try { await stripe.checkout.sessions.expire(session.id); } catch (_) { /* Already closed. */ }
          }
          throw error;
        }
        return respond(response, 200, { orderId: id, url: session.url,
          amountCents: priced.amountCents, currency: 'usd' });
      }
      const statusMatch = /^\/checkout\/status\/([A-Za-z0-9]{8,60})$/.exec(path);
      if (request.method === 'GET' && statusMatch) {
        const user = await authenticated(request);
        const ref = db.collection('stripe_sandbox_orders').doc(statusMatch[1]);
        let snapshot = await ref.get();
        if (!snapshot.exists || snapshot.data().uid !== user.uid) {
          throw new HttpError(404, 'Checkout not found.');
        }
        let order = snapshot.data();
        let checkoutUrl;
        if (order.status === 'pending' && order.sessionId) {
          const session = await stripe.checkout.sessions.retrieve(order.sessionId);
          if (session.payment_status === 'paid') {
            await markPaid(session);
            snapshot = await ref.get();
            order = snapshot.data();
          } else if (session.status === 'expired') {
            await ref.update({ status: 'expired' });
            order = { ...order, status: 'expired' };
          } else if (typeof session.url === 'string'
              && session.url.startsWith('https://checkout.stripe.com/')) {
            checkoutUrl = session.url;
          }
        }
        return respond(response, 200, { orderId: snapshot.id, status: order.status,
          amountCents: order.amountCents, currency: 'usd', title: order.title,
          listingId: order.listingId,
          type: order.type, duration: order.duration, route: order.route,
          travelers: order.travelers, pickup: order.pickup, mobile: order.mobile,
          date: order.date, time: order.time, checkoutUrl });
      }
      throw new HttpError(404, 'Not found.');
    } catch (error) {
      if (!(error instanceof HttpError)) console.error('Stripe sandbox request failed:', error);
      if (!response.headersSent) respond(response, error.code || 500,
        { error: error instanceof HttpError ? error.message : 'Sandbox server unavailable.' });
    }
  });
}

module.exports = { createServer, quote, HttpError };
