package lk.janith.smart_tourism.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.TourPackage;

public class TourPackageAdapter extends RecyclerView.Adapter<TourPackageAdapter.ViewHolder> {

    public interface OnPackageClickListener {
        void onPackageClick(TourPackage tourPackage);
    }

    private final List<TourPackage> packageList;
    private final OnPackageClickListener listener;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firebaseFirestore;

    private final Set<String> wishlistIds = new HashSet<>();
    private boolean wishlistLoaded = false;
    private boolean wishlistLoading = false;

    public TourPackageAdapter(List<TourPackage> packageList, OnPackageClickListener listener) {
        this.packageList = packageList;
        this.listener = listener;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseFirestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tour_package, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TourPackage tourPackage = packageList.get(position);

        holder.imgPackage.setImageResource(tourPackage.getImageResId());
        holder.txtTitle.setText(tourPackage.getTitle());
        holder.txtDuration.setText(tourPackage.getDuration());
        holder.txtPrice.setText(tourPackage.getPrice());
        holder.txtDescription.setText(tourPackage.getDescription());

        if (!wishlistLoaded && !wishlistLoading) {
            loadWishlistIds();
        }

        String wishlistDocId = buildWishlistDocId(tourPackage);
        boolean isWishlisted = wishlistIds.contains(wishlistDocId);
        updateWishlistIcon(holder.btnWishlist, isWishlisted);

        holder.btnViewDetails.setOnClickListener(v -> listener.onPackageClick(tourPackage));
        holder.itemView.setOnClickListener(v -> listener.onPackageClick(tourPackage));

        holder.btnWishlist.setOnClickListener(v -> toggleWishlist(tourPackage, holder.btnWishlist));
    }

    @Override
    public int getItemCount() {
        return packageList.size();
    }

    private void loadWishlistIds() {
        if (firebaseAuth.getCurrentUser() == null) {
            wishlistLoaded = true;
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        wishlistLoading = true;

        firebaseFirestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    wishlistIds.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        wishlistIds.add(doc.getId());
                    }
                    wishlistLoaded = true;
                    wishlistLoading = false;
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    wishlistLoaded = true;
                    wishlistLoading = false;
                });
    }

    private void toggleWishlist(TourPackage tourPackage, ImageView btnWishlist) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(btnWishlist.getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();
        String wishlistDocId = buildWishlistDocId(tourPackage);

        if (wishlistIds.contains(wishlistDocId)) {
            firebaseFirestore.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(wishlistDocId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        wishlistIds.remove(wishlistDocId);
                        updateWishlistIcon(btnWishlist, false);
                        Toast.makeText(btnWishlist.getContext(), "Removed from wishlist", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(btnWishlist.getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        } else {
            Map<String, Object> wishlistItem = new HashMap<>();
            wishlistItem.put("title", tourPackage.getTitle());
            wishlistItem.put("duration", tourPackage.getDuration());
            wishlistItem.put("description", tourPackage.getDescription());
            wishlistItem.put("price", tourPackage.getPrice());
            wishlistItem.put("imageResId", tourPackage.getImageResId());

            firebaseFirestore.collection("users")
                    .document(uid)
                    .collection("wishlist")
                    .document(wishlistDocId)
                    .set(wishlistItem)
                    .addOnSuccessListener(unused -> {
                        wishlistIds.add(wishlistDocId);
                        updateWishlistIcon(btnWishlist, true);
                        Toast.makeText(btnWishlist.getContext(), "Added to wishlist", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(btnWishlist.getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private String buildWishlistDocId(TourPackage tourPackage) {
        String raw = tourPackage.getTitle() + "_" + tourPackage.getDuration();
        return raw.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase(Locale.ROOT);
    }

    private void updateWishlistIcon(ImageView imageView, boolean isWishlisted) {
        int tintColor;

        if (isWishlisted) {
            tintColor = ContextCompat.getColor(imageView.getContext(), R.color.md_theme_error);
        } else {
            tintColor = ContextCompat.getColor(imageView.getContext(), R.color.md_theme_onSurfaceVariant);
        }

        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPackage;
        ImageView btnWishlist;
        TextView txtTitle, txtDuration, txtPrice, txtDescription;
        MaterialButton btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPackage = itemView.findViewById(R.id.imgPackage);
            btnWishlist = itemView.findViewById(R.id.btnWishlist);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
