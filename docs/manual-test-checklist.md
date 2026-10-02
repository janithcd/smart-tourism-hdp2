# Android device test checklist

Use a device or emulator with Google Play services. Build the current branch, sign in to a test account in Firebase project `smart-tourism-abbb0`, and record the device/Android version and each result. Accounts from the old project do not automatically work in this project. These checks need an installed app; a CI build alone cannot verify them.

| Step | Action | Expected result |
| --- | --- | --- |
| 1 | Sign up or sign in, open Explore. | Tours, Vehicles, and Guides tabs are visible, with a sample data notice. |
| 2 | Search `Kandy` in Tours, then switch to Guides with the same query. Clear the query. | Tour and guide results match their own category; clearing restores the guide list. |
| 3 | Open a vehicle and a guide in turn. | Each shows its correct category, illustrative rate, details, and booking setup. Car passenger choices stop at 3; van choices stop at 7. |
| 4 | In a listing, tap Save review without stars or text, then enter 4 stars and a comment. Reopen the same listing, edit it, then remove it. | Validation appears; saved rating/comment return for the same account; edit and removal persist. Other listings have no copied review. |
| 5 | Try booking without fields, with an invalid phone, and with a past date/time. | Required, phone, and future date/time errors prevent the draft. |
| 6 | Select a future date/time, valid phone, people, and pickup, then Book Now. | My Activities shows the selected service type, price, date, and time. |
| 7 | Continue to demo payment, select a method, and confirm. | No card fields are requested; a local demo record appears in Bookings, with category and payment simulation status. A notification appears if permitted. |
| 8 | Sign out, sign in as a second account, check Bookings and the first listing's review, then return to the first account. | The second account cannot see the first account's local booking/review; the first account's records remain. |
| 9 | Remove the booking, and open the map and tour wishlist. | Only the selected local booking is removed; map and wishlist still open. Record any key/API or Firestore errors separately. |
| 10 | With no active Firestore `packages`, open Home and Explore; then add an active category and numeric-price package in `smart-tourism-abbb0` and reopen both screens. | Bundled tours appear first with a sample notice. The Firestore tour replaces them once readable; Home shows the category filter, price, image and route/overview from the document. Vehicles and guides remain samples. |
| 11 | Save a live tour to the wishlist, open it from the wishlist, and also open a previously saved numeric-price wishlist item if available. | Both string and numeric prices display without a crash, and the tour details open. The booking and payment path still indicates a local demo. |

Record failures with a screenshot, the action taken, and the visible error. For a viva demonstration, say explicitly that services, rates, payments, bookings, and reviews in this flow are local or illustrative, rather than live provider transactions.
