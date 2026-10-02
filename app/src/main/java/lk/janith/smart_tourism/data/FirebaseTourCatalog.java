package lk.janith.smart_tourism.data;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.Category;
import lk.janith.smart_tourism.model.TourPackage;

/** Reads the catalogue created for the original Firestore-backed app. */
public final class FirebaseTourCatalog {
    private static final String TAG = "FirebaseTourCatalog";

    public interface Listener<T> {
        void onLoaded(List<T> items);
        void onError(Exception error);
    }

    private FirebaseTourCatalog() { }

    public static void loadPackages(Listener<TourPackage> listener) {
        FirebaseFirestore.getInstance().collection("packages")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<TourPackage> packages = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Map<String, Object> data = document.getData();
                        if (data == null) continue;
                        TourPackage tour = TourPackage.fromRecord(document.getId(), data,
                                R.drawable.sigiriya);
                        if (!tour.getTitle().trim().isEmpty()) packages.add(tour);
                    }
                    listener.onLoaded(packages);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Could not load active packages; showing sample tours", error);
                    listener.onError(error);
                });
    }

    public static void loadCategories(Listener<Category> listener) {
        FirebaseFirestore.getInstance().collection("categories")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Category> categories = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Map<String, Object> data = document.getData();
                        if (data == null) continue;
                        String title = text(data.get("title"));
                        if (title.trim().isEmpty()) continue;
                        categories.add(new Category(document.getId(), title,
                                text(data.get("imageUrl")), text(data.get("description")),
                                true, false));
                    }
                    listener.onLoaded(categories);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Could not load active categories", error);
                    listener.onError(error);
                });
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }
}
