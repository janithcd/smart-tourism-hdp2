package lk.janith.smart_tourism.data;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.TravelService;

/** Reads published admin-managed services; an empty result leaves bundled samples visible. */
public final class FirebaseServiceCatalog {
    private static final String TAG = "FirebaseServiceCatalog";

    public interface Listener {
        void onLoaded(List<TravelService> services);
        void onError(Exception error);
    }

    private FirebaseServiceCatalog() { }

    public static void load(Listener listener) {
        FirebaseFirestore.getInstance().collection("services")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<TravelService> services = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String type = document.getString("type");
                        String title = document.getString("title");
                        String duration = document.getString("duration");
                        String description = document.getString("description");
                        String pickup = document.getString("pickup");
                        String imageUrl = document.getString("imageUrl");
                        Object rawPrice = document.get("price");
                        Object rawCapacity = document.get("capacity");
                        if ((!ServiceCatalog.VEHICLE.equals(type) && !ServiceCatalog.GUIDE.equals(type))
                                || title == null || title.trim().isEmpty() || duration == null
                                || description == null || pickup == null || pickup.trim().isEmpty()
                                || !(rawPrice instanceof Number) || !(rawCapacity instanceof Number)) continue;
                        Number price = (Number) rawPrice;
                        Number capacity = (Number) rawCapacity;
                        if (!Double.isFinite(price.doubleValue()) || price.doubleValue() <= 0
                                || capacity.intValue() < 1 || capacity.intValue() > 30
                                || capacity.doubleValue() != capacity.intValue()) continue;
                        int icon = ServiceCatalog.VEHICLE.equals(type)
                                ? R.drawable.airport_shuttle_24px : R.drawable.explore_24px;
                        services.add(new TravelService(document.getId(), type, title, duration,
                                String.format(Locale.US, "$%.2f listed rate", price.doubleValue()),
                                description, imageUrl == null ? "" : imageUrl,
                                capacity.intValue(), pickup, icon));
                    }
                    listener.onLoaded(services);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Could not read published services", error);
                    listener.onError(error);
                });
    }
}
