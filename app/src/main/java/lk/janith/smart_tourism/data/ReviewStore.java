package lk.janith.smart_tourism.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

/** Private demo feedback stored on this device for the current Firebase account. */
public final class ReviewStore {
    private static final String PREFIX = "smart_tourism_reviews_";

    private ReviewStore() { }

    public static final class Review {
        public final int stars;
        public final String comment;

        private Review(int stars, String comment) {
            this.stars = stars;
            this.comment = comment;
        }
    }

    private static SharedPreferences preferences(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("Sign in before reviewing a service");
        return context.getSharedPreferences(PREFIX + user.getUid(), Context.MODE_PRIVATE);
    }

    private static String key(String type, String title) {
        return type + ":" + title;
    }

    public static Review get(Context context, String type, String title) {
        String raw = preferences(context).getString(key(type, title), null);
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            int stars = json.optInt("stars");
            if (stars < 1 || stars > 5) return null;
            return new Review(stars, json.optString("comment"));
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static boolean save(Context context, String type, String title, int stars, String comment) {
        String trimmed = comment.trim();
        if (stars < 1 || stars > 5 || trimmed.isEmpty() || trimmed.length() > 500) return false;
        try {
            JSONObject json = new JSONObject();
            json.put("stars", stars);
            json.put("comment", trimmed);
            return preferences(context).edit().putString(key(type, title), json.toString()).commit();
        } catch (JSONException ignored) {
            return false;
        }
    }

    public static boolean remove(Context context, String type, String title) {
        return preferences(context).edit().remove(key(type, title)).commit();
    }
}
