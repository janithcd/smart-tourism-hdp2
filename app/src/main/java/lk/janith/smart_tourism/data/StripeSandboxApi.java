package lk.janith.smart_tourism.data;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.janith.smart_tourism.BuildConfig;

/** Test-only API. The Stripe secret and all pricing remain on the trusted server. */
public final class StripeSandboxApi {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(JSONObject result);
        void onError(String error);
    }

    private StripeSandboxApi() { }

    public static boolean available() {
        return BuildConfig.DEBUG && !BuildConfig.STRIPE_SANDBOX_URL.isEmpty();
    }

    public static void start(SharedPreferences draft, Callback callback) {
        if (!available()) { callback.onError("Stripe sandbox backend is not configured."); return; }
        try {
            JSONObject request = new JSONObject();
            request.put("type", draft.getString("package_service_type", "Tour"));
            request.put("listingId", draft.getString("package_catalog_id", ""));
            String people = draft.getString("package_pax", "1 Person");
            request.put("travelers", Integer.parseInt(people.split(" ")[0]));
            request.put("pickup", draft.getString("package_pickup_location", ""));
            request.put("mobile", draft.getString("package_mobile_number", ""));
            request.put("date", draft.getString("package_travel_date", ""));
            request.put("time", draft.getString("package_travel_time", ""));
            call("POST", "/checkout/start", request, callback);
        } catch (Exception error) {
            callback.onError("Choose a valid published listing and travelers.");
        }
    }

    public static void status(String orderId, Callback callback) {
        if (!available() || !orderId.matches("[A-Za-z0-9]{8,60}")) {
            callback.onError("No Stripe test checkout is pending."); return;
        }
        call("GET", "/checkout/status/" + orderId, null, callback);
    }

    private static void call(String method, String path, JSONObject data, Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onError("Sign in to continue."); return; }
        user.getIdToken(false).addOnSuccessListener(token -> IO.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL endpoint = new URL(BuildConfig.STRIPE_SANDBOX_URL + path);
                connection = (HttpURLConnection) endpoint.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Authorization", "Bearer " + token.getToken());
                connection.setRequestProperty("Accept", "application/json");
                if (data != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.getOutputStream().write(data.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (stream != null) {
                    try (InputStream input = stream) {
                        byte[] buffer = new byte[2048];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (bytes.size() + count > 16384) throw new IllegalStateException("Response too large");
                            bytes.write(buffer, 0, count);
                        }
                    }
                }
                JSONObject result = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException(result.optString("error", "Checkout unavailable"));
                }
                MAIN.post(() -> callback.onSuccess(result));
            } catch (Exception error) {
                String message = error.getMessage() == null ? "Checkout server unavailable." : error.getMessage();
                MAIN.post(() -> callback.onError(message));
            } finally {
                if (connection != null) connection.disconnect();
            }
        })).addOnFailureListener(error ->
                callback.onError("Could not verify your Firebase sign-in."));
    }
}
