# Proposal coverage — Handheld Device Programming II

Source: *Smart Tourism and Travel Booking Mobile Application*, project proposal by Janith Chamuka Dasanayaka (19 February 2026). This is a proposal, not the marking rubric. Status below describes the code in this branch and must still be checked on a device.

| Proposed requirement | Current evidence | Status / next step |
| --- | --- | --- |
| Tourist registration and login | Firebase email/password sign-up, sign-in, profile | Implemented; verify Firebase configuration and user flow on device. |
| Tour browsing and service details | Four static packages, Home/Explore list, category search, details | Demo implemented; catalogue is hardcoded. |
| Vehicle and guide browsing | Three illustrative vehicles and three guide services in Explore, each with details and the same local booking flow | Demo implemented; no real providers, availability, or rates. |
| Provider/business registration and service management | Only tourist-style registration | Missing. Add role-aware registration and provider listing management with Firestore rules. |
| Booking scheduling | Date, time, travelers, pickup, phone, draft | Demo implemented; validate the full flow on device. |
| Payment simulation | Card, mobile wallet and cash choices; local simulated status | Demo implemented; no actual payment service or card collection. |
| Booking history and tracking | Per-user local history on one device | Partial; no Firestore sync, provider acceptance or live status changes. |
| Reviews and ratings | Save, edit, and remove a 1–5 star review with comment per listing and signed-in account on one device | Partial; private demo feedback only, with no shared review feed or moderation. |
| Notifications and broadcast receiver | Explicit booking-saved receiver posts local notification when permitted | Partial; no Firebase Cloud Messaging or provider-driven updates. |
| Google Maps and location discovery | Maps SDK, route stops, direct Routes/Places requests | Partial; test API permissions/key restrictions and device rendering. |
| Local storage and network communication | SharedPreferences drafts/history, Firebase Auth/Firestore, Maps requests | Demonstrated; local history is not synchronized. |
| Multimedia and sensors | Tour photographs and image carousel; no sensor interaction | Partial; add a purposeful sensor feature and verify multimedia requirement with lecturer. |
| UI animations/transitions | Splash, page slider and Material components | Partial; inspect against the assessment criteria. |
| Administrator panel | No admin interface or protected role | Missing. |
| Testing and security | Debug build workflow, basic template tests, Firebase authentication | Partial; add functional tests, review Firestore rules and protect the Maps key. |

## Viva preparation order

1. Run the draft PR on a device using the [manual test checklist](manual-test-checklist.md): registration, login, each Explore category, reviews, date/time validation, payment simulation, notification, booking history, wishlist, and map. Record actual failures and fix them first.
2. Implement provider/admin roles and booking status updates only with enforceable Firestore rules. A screen that merely looks like an admin panel would not protect data.
3. Add a useful sensor feature, assess push notifications and multimedia, and collect functional test evidence.
4. Prepare a short presentation that states which features are working demos and which proposal items remain incomplete. Do not describe local demo bookings or simulated payments as live transactions.

The original Maps key appeared in public Git history and should be rotated or disabled. The Android Maps SDK and the direct Routes/Places web requests may need different restricted credentials or a backend proxy.
