package lk.janith.smart_tourism.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** Firestore copies of demo bookings, readable only by administrators or their owner. */
public final class FirebaseBookingStore {
    private FirebaseBookingStore() { }

    public static Task<Void> submit(BookingStore.Booking booking) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            throw new IllegalStateException("Sign in with an email account to submit a demo booking");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", booking.id);
        data.put("uid", user.getUid());
        data.put("email", user.getEmail());
        data.put("source", "demo");
        data.put("title", booking.title);
        data.put("type", booking.type);
        data.put("duration", booking.duration);
        data.put("price", booking.price);
        data.put("route", booking.route);
        data.put("pax", booking.pax);
        data.put("pickup", booking.pickup);
        data.put("mobile", booking.mobile);
        data.put("date", booking.date);
        data.put("time", booking.time);
        data.put("paymentMethod", booking.paymentMethod);
        data.put("paymentStatus", booking.paymentStatus);
        data.put("reviewStatus", "New");
        data.put("createdAt", FieldValue.serverTimestamp());
        return FirebaseFirestore.getInstance().collection("demo_bookings")
                .document(booking.id).set(data);
    }

    public static Task<Void> remove(String bookingId) {
        return FirebaseFirestore.getInstance().collection("demo_bookings")
                .document(bookingId).delete();
    }
}
