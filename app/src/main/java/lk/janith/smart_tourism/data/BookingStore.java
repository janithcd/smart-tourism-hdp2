package lk.janith.smart_tourism.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lk.janith.smart_tourism.R;

/** Local demo records, scoped to the signed-in Firebase user on this device. */
public final class BookingStore {
    private static final String DRAFT_PREFIX = "smart_tourism_booking_draft_";
    private static final String HISTORY_PREFIX = "smart_tourism_booking_history_";
    private static final String HISTORY_KEY = "bookings";

    private BookingStore() { }

    private static String userId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Sign in before booking a tour");
        }
        return user.getUid();
    }

    public static SharedPreferences draft(Context context) {
        return context.getSharedPreferences(DRAFT_PREFIX + userId(), Context.MODE_PRIVATE);
    }

    private static SharedPreferences history(Context context) {
        return context.getSharedPreferences(HISTORY_PREFIX + userId(), Context.MODE_PRIVATE);
    }

    public static final class Booking {
        public final String id, title, duration, price, route, pax, pickup, mobile, date;
        public final int imageResId;
        public final long createdAt;

        private Booking(String id, String title, String duration, String price, String route,
                        String pax, String pickup, String mobile, String date, int imageResId,
                        long createdAt) {
            this.id = id;
            this.title = title;
            this.duration = duration;
            this.price = price;
            this.route = route;
            this.pax = pax;
            this.pickup = pickup;
            this.mobile = mobile;
            this.date = date;
            this.imageResId = imageResId;
            this.createdAt = createdAt;
        }

        private JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("title", title);
            json.put("duration", duration);
            json.put("price", price);
            json.put("route", route);
            json.put("pax", pax);
            json.put("pickup", pickup);
            json.put("mobile", mobile);
            json.put("date", date);
            json.put("imageResId", imageResId);
            json.put("createdAt", createdAt);
            return json;
        }

        private static Booking fromJson(JSONObject json) {
            return new Booking(json.optString("id"), json.optString("title"),
                    json.optString("duration"), json.optString("price"),
                    json.optString("route"), json.optString("pax"),
                    json.optString("pickup"), json.optString("mobile"),
                    json.optString("date"), json.optInt("imageResId", R.drawable.location_on_24px),
                    json.optLong("createdAt"));
        }
    }

    public static List<Booking> getAll(Context context) {
        List<Booking> bookings = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(history(context).getString(HISTORY_KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null && item.optString("id").length() >= 8
                        && !item.optString("title").isEmpty()) {
                    bookings.add(Booking.fromJson(item));
                }
            }
        } catch (JSONException ignored) {
            // A damaged local record should not crash the Bookings screen.
        }
        return bookings;
    }

    /** Persist the selected tour as a local demo booking. No payment or server request occurs. */
    public static Booking confirmDraft(Context context) {
        SharedPreferences draft = draft(context);
        String title = draft.getString("package_title", "");
        String date = draft.getString("package_travel_date", "");
        if (title == null || title.trim().isEmpty() || date == null || date.isEmpty()) {
            return null;
        }

        Booking booking = new Booking(UUID.randomUUID().toString(), title,
                draft.getString("package_duration", ""), draft.getString("package_price", ""),
                draft.getString("package_description", ""), draft.getString("package_pax", ""),
                draft.getString("package_pickup_location", ""),
                draft.getString("package_mobile_number", ""), date,
                draft.getInt("package_image_res_id", R.drawable.location_on_24px),
                System.currentTimeMillis());

        try {
            SharedPreferences history = history(context);
            JSONArray existing = new JSONArray(history.getString(HISTORY_KEY, "[]"));
            JSONArray updated = new JSONArray();
            updated.put(booking.toJson());
            for (int i = 0; i < existing.length(); i++) updated.put(existing.get(i));
            if (!history.edit().putString(HISTORY_KEY, updated.toString()).commit()) return null;
            draft.edit().clear().apply();
            return booking;
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static boolean remove(Context context, String bookingId) {
        try {
            SharedPreferences history = history(context);
            JSONArray existing = new JSONArray(history.getString(HISTORY_KEY, "[]"));
            JSONArray updated = new JSONArray();
            for (int i = 0; i < existing.length(); i++) {
                JSONObject item = existing.optJSONObject(i);
                if (item != null && !bookingId.equals(item.optString("id"))) updated.put(item);
            }
            return history.edit().putString(HISTORY_KEY, updated.toString()).commit();
        } catch (JSONException ignored) {
            return false;
        }
    }
}
