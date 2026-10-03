# Admin tour manager setup

The tourist app now contains a **Manage tours** screen. It reads and edits the existing `packages` collection in Firebase project `smart-tourism-abbb0`. It is available only to a signed-in user whose `admins/{uid}` document has `active: true` and `role: "admin"`. This matches the administrator record visible in the existing project. A menu check improves the UI; **Firestore Security Rules enforce the actual access**.

## One-time Firebase setup

1. In the Firebase Console, select **smart-tourism-abbb0** → Firestore Database → **Rules**. Compare the deployed rules with [`firestore.rules`](../firestore.rules). The file includes the existing tourist `users/{uid}` and wishlist permissions, active `packages`/`categories` reads, and new administrator writes. If your separate admin app uses other collections, preserve its required paths with equally narrow rules before publishing. The repository cannot publish rules to your Firebase project automatically.
2. Publish the reviewed rules to this project. Do not restore a time-limited `match /{document=**} { allow read, write: if ... }` rule; overlapping broad rules would defeat the admin restrictions.
3. In Authentication → Users, copy the **UID** of the account that will manage tours. In Firestore Data, create or verify `admins/{that-UID}` with fields `active` (boolean) `true` and `role` (string) `admin`. If the existing administrator document already matches that UID and fields, no new record is needed. Only a trusted Firebase Console operator or Admin SDK process should create or change this document. The Android client is forbidden from doing so.
4. Sign in to the tourist app using that account. Open the drawer → **Manage tours**. A new tour starts hidden. Enter title, duration, numeric USD price, and any route/description details, then Save. Select the tour in **Existing tours**, turn on **Publish tour**, and Save again. Reopen Home or Explore to see the published tour. Hiding a tour removes it from the public catalogue without deleting the document.
5. Sign in with a regular tourist account. **Manage tours** should be absent, and Firestore should reject direct attempts to change `packages` or `admins`.

If the screen says it cannot verify access, check the project ID, the signed-in UID, the `admins/{uid}` fields, the published rules and the device connection. The Firestore rules can be tested locally with Node.js 22 and Java 21 by running `npm install` and `npm run test:rules:emulator`; this uses a `demo-` emulator project and does not write to production.

This is a minimal tour-catalogue editor. The separate admin app source is not in this repository, and provider registration, availability, shared bookings, and provider status updates remain future work. Existing local demo bookings and payments are unchanged.
