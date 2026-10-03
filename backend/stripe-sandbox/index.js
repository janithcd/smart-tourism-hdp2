'use strict';

const Stripe = require('stripe');
const admin = require('firebase-admin');
const { createServer } = require('./server');

const key = process.env.STRIPE_SECRET_KEY || '';
const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET || '';
const projectId = process.env.FIREBASE_PROJECT_ID || '';
const baseUrl = process.env.PUBLIC_BASE_URL || '';
if (!key.startsWith('sk_test_') || !webhookSecret.startsWith('whsec_') || !projectId
    || !(baseUrl.startsWith('https://') || /^http:\/\/localhost(:\d+)?$/.test(baseUrl))) {
  throw new Error('Configure only Stripe test keys, the Firebase project and an HTTPS or localhost return URL.');
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId
});
const stripe = new Stripe(key);
const app = createServer({
  stripe,
  auth: admin.auth(),
  db: admin.firestore(),
  serverTimestamp: () => admin.firestore.FieldValue.serverTimestamp(),
  webhookSecret,
  baseUrl
});
const port = Number(process.env.PORT || 4242);
app.listen(port, '127.0.0.1', () => {
  console.log(`Stripe sandbox listening on http://localhost:${port}`);
});
