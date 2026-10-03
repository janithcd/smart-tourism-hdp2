# Proposal coverage — Handheld Device Programming II

Source: *Smart Tourism and Travel Booking Mobile Application*, project proposal by Janith Chamuka Dasanayaka (19 February 2026). This is a proposal, not the marking rubric. Status below describes the code in this branch and must still be checked on a device.

| Proposed requirement | Current evidence | Status / next step |
| --- | --- | --- |
| Tourist registration and login | Firebase email/password sign-up, sign-in, profile | Implemented; verify Firebase configuration and user flow on device. |
| Tour browsing and service details | Active Firestore packages and categories on Home/Explore, image/route/overview details, bundled tour fallback and Explore search | Implemented with offline fallback; verify catalogue reads, category filtering and details on a device. |
| Vehicle and guide browsing | Three illustrative vehicles and three guide services in Explore, each with details and the same local booking flow | Demo implemented; no real providers, availability, or rates. |
| Provider/business registration and service management | Only tourist-style registration | Missing. Add role-aware registration and provider listing management with Firestore rules. |
| Booking scheduling | Date, time, travelers, pickup, phone, draft | Demo implemented; validate the full flow on device. |
| Payment simulation | Card, mobile wallet and cash choices; simulated status copied into a Firestore demo submission | Demo implemented; no actual payment service or card collection. |
| Booking history and tracking | Per-user local history; new demo submissions shared with an admin who can mark Reviewed | Partial; no cross-device history download, provider acceptance or real status updates. Older local records stay on the original device. |
| Reviews and ratings | Save, edit, and remove a 1–5 star review with comment per listing and signed-in account on one device | Partial; private demo feedback only, with no shared review feed or moderation. |
| Notifications and broadcast receiver | Explicit booking-saved receiver posts local notification when permitted | Partial; no Firebase Cloud Messaging or provider-driven updates. |
| Google Maps and location discovery | Maps SDK, route stops, direct Routes/Places requests | Partial; test API permissions/key restrictions and device rendering. |
| Local storage and network communication | SharedPreferences drafts/history, Firebase Auth/Firestore catalogue and demo booking submissions, Maps requests | Demonstrated; local history is not downloaded to another device. |
| Multimedia and sensors | Tour photographs and image carousel; Map screen magnetic compass from the rotation-vector sensor | Partial; verify heading and sensor fallback on a device, and check multimedia requirement with lecturer. |
| UI animations/transitions | Splash, page slider and Material components | Partial; inspect against the assessment criteria. |
| Administrator panel | Role-checked tour and category editors, dropdown category selection, latest 50 demo submissions, Reviewed flag under Firestore rules | Partial; publish updated rules and test with the actual admin and tourist accounts. The older separate admin app is not in this repo. |
| Testing and security | Debug build, tour record tests, Firestore rules with emulator tests, Firebase authentication | Partial; run device checks, publish the reviewed rules, and protect the Maps key. |

## Viva preparation order

1. Run the draft PR on a device using the [manual test checklist](manual-test-checklist.md): registration, login, each Explore category, reviews, date/time validation, payment simulation, notification, booking history, wishlist, and map. Record actual failures and fix them first.
2. Set up and test the [admin catalogue and demo booking manager](admin-tour-setup.md) with the reviewed Firestore rules. Provider registration and real booking status updates still need a backend design.
3. Verify the compass on a sensor-equipped phone, assess push notifications and multimedia, and collect functional test evidence.
4. Prepare a short presentation that states which features are working demos and which proposal items remain incomplete. Do not describe Firestore demo submissions or simulated payments as live transactions.

The original Maps key appeared in public Git history and should be rotated or disabled. The Android Maps SDK and the direct Routes/Places web requests may need different restricted credentials or a backend proxy.
