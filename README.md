# Smart Tourism

Android Java/XML application for Handheld Device Programming II. The current app has Firebase email/password authentication, profiles and wishlists in Firestore, a local tour catalogue, Google Maps, language resources, and a local demo booking and payment simulation.

See [the proposal gap analysis](docs/proposal-gap-analysis.md) for feature coverage and the remaining viva work.

## Run locally

1. Open this directory in Android Studio and let Gradle sync.
2. Use an Android device or emulator with Google Play services (min SDK 24).
3. Add `MAPS_API_KEY=your_restricted_android_maps_key` to the root `local.properties` file. Keep that file out of Git. Configure an Android app restriction for package `lk.janith.smart_tourism` and the signing certificate SHA-1 in Google Cloud. Without this key, map tiles cannot load.
4. The included `app/google-services.json` links to the existing Firebase project. Enable email/password authentication and provide suitable Firestore rules for each signed-in user's `users/{uid}` document and `wishlist` subcollection.
5. Run the `app` configuration. For command-line verification, run `gradlew.bat :app:assembleDebug` on Windows.

## Viva demo path

Sign up or sign in → Home or Explore → open a tour → choose people, pickup, mobile number, travel date and pickup time → Book Now → My Activities → Continue to demo payment → choose Card (simulation), Mobile wallet (simulation), or Cash on arrival → Confirm demo booking → Bookings. Search for a destination on Explore and use the wishlist, profile, map, and language menu as other examples.

The booking confirmation writes a **local demo record** scoped to the signed-in user on that device, with the selected payment method and a simulated payment status. It does not collect card details, charge money, reserve a vehicle, contact a provider, or synchronize booking history to Firestore. The price shown comes from the static tour catalogue. Removing a booking removes only that local record. A local notification is shown after confirmation if the user allows notifications; this is not a push message from a provider.

## Next work

- Compare the app with the assignment brief and marking rubric.
- Test registration, Firestore rules, the map key, and the full booking path on a physical Android device.
- `MapFragment` also calls the Routes and Places web APIs directly with the map key. A key restricted for the Android Maps SDK may be rejected by those web APIs. Move those requests behind a backend with a separate restricted server key, or use the appropriate Android SDK before treating live routes and nearby places as reliable. The straight-line route fallback is only an estimate.
- Add any required notifications, sensor interactions, multimedia, remote booking backend, or admin features after checking the brief.

The Maps API key previously committed to the public repository should be rotated or disabled in Google Cloud. Removing it from current source does not remove it from Git history.
