# Smart Tourism

Android Java/XML application for Handheld Device Programming II. The current app has Firebase email/password authentication, profiles and wishlists in Firestore, sample tour/vehicle/guide catalogues, private on-device reviews, Google Maps, language resources, and a local demo booking and payment simulation.

See [the proposal gap analysis](docs/proposal-gap-analysis.md) for feature coverage and the remaining viva work.

## Run locally

1. Open this directory in Android Studio and let Gradle sync.
2. Use an Android device or emulator with Google Play services (min SDK 24).
3. Add `MAPS_API_KEY=your_restricted_android_maps_key` to the root `local.properties` file. Keep that file out of Git. Configure an Android app restriction for package `lk.janith.smart_tourism` and the signing certificate SHA-1 in Google Cloud. Without this key, map tiles cannot load.
4. The included `app/google-services.json` links to Firebase project `smart-tourism-abbb0` and contains registrations for the tourist and admin Android apps. This repository builds the tourist app with package `lk.janith.smart_tourism`; the Google services plugin selects that entry. In this Firebase project, enable email/password authentication and provide suitable Firestore rules for each signed-in user's `users/{uid}` document and `wishlist` subcollection. Accounts in the previous project `smart-tourism-be6a6` are not available in this project unless separately migrated; create a new test account here if needed.
5. Run the `app` configuration. For command-line verification, run `gradlew.bat :app:assembleDebug` on Windows.

## Viva demo path

Sign up or sign in → Explore → switch between Tours, Vehicles, and Guides → search and open a listing → choose people, pickup, mobile number, travel date and pickup time → Book Now → My Activities → Continue to demo payment → choose Card (simulation), Mobile wallet (simulation), or Cash on arrival → Confirm demo booking → Bookings. Open the listing again to save or edit a private 1–5 star review. Use the tour wishlist, profile, map, and language menu as other examples. Follow the [manual test checklist](docs/manual-test-checklist.md) when checking the build on a device.

The booking confirmation writes a **local demo record** scoped to the signed-in user on that device, with the selected payment method and a simulated payment status. It does not collect card details, charge money, reserve a vehicle or guide, contact a provider, or synchronize booking history to Firestore. All listings and prices are illustrative. Removing a booking removes only that local record. Reviews are private to the signed-in account on the device and are not public ratings. A local notification is shown after confirmation if the user allows notifications; this is not a push message from a provider.

## Next work

- Compare the app with the assignment brief and marking rubric.
- Test registration, Firestore rules, the map key, and the full booking path on a physical Android device.
- `MapFragment` also calls the Routes and Places web APIs directly with the map key. A key restricted for the Android Maps SDK may be rejected by those web APIs. Move those requests behind a backend with a separate restricted server key, or use the appropriate Android SDK before treating live routes and nearby places as reliable. The straight-line route fallback is only an estimate.
- Add any required notifications, sensor interactions, multimedia, remote booking backend, or admin features after checking the brief.

The Maps API key previously committed to the public repository should be rotated or disabled in Google Cloud. Removing it from current source does not remove it from Git history.

The Firebase configuration file and the Maps API key are separate. Switching Firebase projects does not supply the Maps key in `local.properties` or move old Authentication users and Firestore documents.
