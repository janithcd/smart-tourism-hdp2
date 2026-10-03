# Stripe test checkout on a local backend

This optional debug build flow uses Stripe hosted Checkout **in test mode**. It does not move real money, reserve a tour, confirm a provider, or enable live payments in Sri Lanka. The app's existing simulation and cash choices remain available. Stripe is currently not listed as supporting live merchant accounts in Sri Lanka; do not present the test path as a production gateway. The free Firebase Spark plan is enough for Firestore/Auth here because the trusted Node server runs on your computer, not Cloud Functions.

## One-time preparation

1. Use a Stripe test account you are permitted to use. Open its test-mode developer API keys and copy only the **test** secret key into your local backend environment. Never put a Stripe secret in Android, `local.properties`, GitHub, or a screenshot. Do not switch to a live key.
2. In Firebase Console for `smart-tourism-abbb0`, create a dedicated service account for this local exercise using Project settings → Service accounts → Generate new private key. Store the JSON **outside the repository** and protect or delete it when finished. This credential has administrator access to Firestore. Do not commit it or send it to anyone. An existing trusted application-default credential is also suitable; set `GOOGLE_APPLICATION_CREDENTIALS` to its absolute file path.
3. Publish the repository's updated `firestore.rules` in Firebase Console. Client writes to `stripe_sandbox_orders` are denied, and test payment records cannot be created or deleted by tourist clients.
4. Create at least one **active Firestore** package or service with a numeric USD price. Bundled sample listings cannot be purchased through this test flow.

## Start the test server and app

1. Install Node.js 22 or newer and Stripe CLI. In `backend/stripe-sandbox`, run `npm install`, then copy `.env.example` to `.env` and replace placeholders with your own local test key, Firebase project ID, and absolute credential path. Keep `PUBLIC_BASE_URL=http://localhost:4242` and `PORT=4242` for the USB test. `.env` is ignored by Git. Run `npm start` in that folder and check `http://localhost:4242/health` on the computer.
2. In a second terminal, run `stripe login` and `stripe listen --forward-to localhost:4242/webhook`. Copy the CLI's `whsec_…` signing secret into your local `.env` as `STRIPE_WEBHOOK_SECRET`, then restart `npm start`. The CLI secret is specific to that listener. Keep the listener running during checkout.
3. In root `local.properties`, add `STRIPE_SANDBOX_URL=http://localhost:4242`. The URL is a debug-only public endpoint address, **not a key**. Rebuild the debug app. Connect an Android device with USB debugging (or an emulator) and run `adb reverse tcp:4242 tcp:4242`. The phone's `localhost` now reaches the Node process. Repeat `adb reverse` after reconnecting. The server binds to your computer's loopback address; do not expose it publicly.
4. Sign in, open a published tour, vehicle, or guide, choose future travel details, then My Activities → Payment options → **Stripe Checkout · TEST MODE (USD)**. The server checks your Firebase ID token, reads the current active listing and sets its USD amount. Complete hosted Checkout using Stripe's standard test card `4242 4242 4242 4242`, a future expiry and any CVC. The return page links back to the app. On returning, the app requests the server's payment status; the server also accepts signed webhooks. A paid test order is recorded once in Firestore and shown in the administrator's demo bookings screen. Canceling checkout or merely opening the return link does not record it as paid.

Only the Node process holds the `sk_test_` secret, the Firebase service account, and the webhook secret. It refuses a live Stripe key, verifies Stripe's signature over the raw webhook body, checks test mode/session ID/amount/currency, and writes the record after Stripe reports `paid`. The app never trusts a return URL as proof of payment. The admin record is retained when the tourist hides the local history copy.

## Verification and limitations

- Run `npm test` in `backend/stripe-sandbox` for pricing, authentication, owner, signature, and duplicate webhook checks. Run `npm run test:rules:emulator` at the repository root for Firestore client access tests.
- The test server stops when your terminal/computer stops. The checkout button appears only in a debug build with `STRIPE_SANDBOX_URL` set and a published listing selected. A release build has the URL empty.
- If checkout says unavailable, check the Node terminal, Stripe CLI, `adb reverse`, Firebase project/credential, and active listing. If a browser was closed, return to the app to check status or resume the pending test checkout. The price on Checkout comes from Firestore, which can differ from an older on-device draft.
- This is a teaching sandbox. It has no deployed backend, provider reservation, refunds, tax or currency conversion, customer support/payment policy, or production readiness. Do not collect real card numbers; keep demo claims explicitly in test mode.

Stripe references: [test-mode Checkout and fulfillment](https://docs.stripe.com/checkout/fulfillment), [webhook signatures](https://docs.stripe.com/webhooks/signature), [availability](https://stripe.com/global), and [test cards](https://docs.stripe.com/testing).
