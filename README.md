# Smart Tourism

Android Java/XML application for Handheld Device Programming II. The current app has Firebase email/password authentication, profiles and wishlists in Firestore, a Firestore tour catalogue with a bundled sample fallback, an admin-only tour editor, sample vehicles/guides, private on-device reviews, Google Maps with a magnetic compass, language resources, and a local demo booking and payment simulation.

See [the proposal gap analysis](docs/proposal-gap-analysis.md) for feature coverage and the remaining viva work.

## Run locally

1. Open this directory in Android Studio and let Gradle sync.
2. Use an Android device or emulator with Google Play services (min SDK 24).
3. Add `MAPS_API_KEY=your_restricted_android_maps_key` to the root `local.properties` file. Keep that file out of Git. Configure an Android app restriction for package `lk.janith.smart_tourism` and the signing certificate SHA-1 in Google Cloud. Without this key, map tiles cannot load.
4. The included `app/google-services.json` links to Firebase project `smart-tourism-abbb0` and contains registrations for the tourist and admin Android apps. This repository builds the tourist app with package `lk.janith.smart_tourism`; the Google services plugin selects that entry. In this Firebase project, enable email/password authentication and [review and publish the Firestore rules](docs/admin-tour-setup.md) for owner-only profiles/wishlists, published catalogues, and admin-only writes. Accounts and documents in the previous project `smart-tourism-be6a6` are not available here unless separately migrated; create a new test account and catalogue documents here if needed.
5. Run the `app` configuration. For command-line verification, run `gradlew.bat :app:assembleDebug` on Windows.

## Viva demo path

Sign up or sign in → Explore → switch between Tours, Vehicles, and Guides → search and open a listing → choose people, pickup, mobile number, travel date and pickup time → Book Now → My Activities → Continue to demo payment → choose Card (simulation), Mobile wallet (simulation), or Cash on arrival → Confirm demo booking → Bookings. Open the listing again to save or edit a private 1–5 star review. Use the tour wishlist, profile, map compass, and language menu as other examples. After the [admin setup](docs/admin-tour-setup.md), an admin account can add and publish a tour for Home and Explore. Follow the [manual test checklist](docs/manual-test-checklist.md) when checking the build on a device.

## Original Firestore catalogue

The original local Android Studio copy had a Firestore catalogue that was absent from the GitHub upload. Home and Explore now query active documents in `packages`; Home also loads active documents in `categories` and filters tours by `categoryId`. A package has `title`, `duration`, numeric `price`, `description`, `route`, `overview`, `imageUrl`, `categoryId`, and `active: true`. A category has `title` and `active: true`; its document ID is used as the package's `categoryId`. Only nonempty active packages replace the bundled sample tours. If reads fail or return no active packages, the app keeps the bundled tours. The screen labels indicate which source is showing; Logcat tag `FirebaseTourCatalog` reports failed reads. Vehicles and guides stay illustrative.

The original Firestore wishlist used a numeric price and package ID, whereas newer demo items use a string price and a local drawable. The app can display both. The old Firestore `my_activities` drafts are not copied into the new on-device booking flow, and no checkout or payment is performed on Firestore. The old project's data must be migrated or recreated in `smart-tourism-abbb0` before it appears in this app.

The booking confirmation writes a **local demo record** scoped to the signed-in user on that device, with the selected payment method and a simulated payment status. It does not collect card details, charge money, reserve a vehicle or guide, contact a provider, or synchronize booking history to Firestore. All listings and prices are illustrative. Removing a booking removes only that local record. Reviews are private to the signed-in account on the device and are not public ratings. A local notification is shown after confirmation if the user allows notifications; this is not a push message from a provider.

## Next work

- Compare the app with the assignment brief and marking rubric.
- Test registration, Firestore rules, catalogue loading and fallback, the map key, and the full booking path on a physical Android device.
- Check the admin editor against the actual Firebase project with an administrator and a regular tourist; the repository's rules tests run in the Firestore emulator.
- `MapFragment` also calls the Routes and Places web APIs directly with the map key. A key restricted for the Android Maps SDK may be rejected by those web APIs. Move those requests behind a backend with a separate restricted server key, or use the appropriate Android SDK before treating live routes and nearby places as reliable. The straight-line route fallback is only an estimate.
- Plan provider registration, shared booking status, and any required push notifications or multimedia after checking the brief. The in-app admin editor currently manages tours only.

## Credential handling

An early public commit contained a Google Maps API key in `AndroidManifest.xml`. Current source uses `${MAPS_API_KEY}` from the ignored root `local.properties`, but the original key is still visible in Git history. Treat it as exposed: in its Google Cloud project, restrict it immediately, replace it with a new Android-restricted key for `lk.janith.smart_tourism` and the correct signing SHA-1, then disable or delete the old key after confirming the replacement works. Do not paste a key into an issue, screenshot, commit, or pull request. Rewriting Git history alone does not revoke a key or remove it from other clones and cached references.

`app/google-services.json` is intentionally tracked because the Android Firebase SDK needs this client configuration. Its Firebase API key identifies a project; it does not authorize database access. In Google Cloud Credentials, keep that key limited to the Firebase APIs the app uses, and enforce Firestore Security Rules. Use a separate restricted key for Maps. Switching Firebase projects does not supply a Maps key or move old Authentication users and Firestore documents.

The CI credential check scans tracked files for common key and private-key formats and rejects local credential files. It excludes the expected Firebase API key in `google-services.json`; GitHub secret scanning and push protection should also be enabled in the repository settings. This check cannot undo a previous disclosure or detect every possible credential format.
