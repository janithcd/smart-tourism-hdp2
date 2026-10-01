package lk.janith.smart_tourism.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.adapter.TourPackageAdapter;
import lk.janith.smart_tourism.model.TourPackage;

public class WishlistFragment extends Fragment {

    private RecyclerView recyclerWishlist;
    private View emptyWishlistCard;
    private ImageView btnBackWishlist;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private final List<TourPackage> wishlist = new ArrayList<>();
    private TourPackageAdapter adapter;

    public WishlistFragment() {
        super(R.layout.fragment_wishlist);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerWishlist = view.findViewById(R.id.recyclerWishlist);
        emptyWishlistCard = view.findViewById(R.id.emptyWishlistCard);
        btnBackWishlist = view.findViewById(R.id.btnBackWishlist);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        recyclerWishlist.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TourPackageAdapter(wishlist, tourPackage -> {
            // Later you can open details screen here
        });

        recyclerWishlist.setAdapter(adapter);

        btnBackWishlist.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        loadWishlist();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        if (firebaseAuth.getCurrentUser() == null) {
            wishlist.clear();
            adapter.notifyDataSetChanged();
            recyclerWishlist.setVisibility(View.GONE);
            emptyWishlistCard.setVisibility(View.VISIBLE);
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("users")
                .document(uid)
                .collection("wishlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    wishlist.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String title = doc.getString("title");
                        String duration = doc.getString("duration");
                        String price = doc.getString("price");
                        String description = doc.getString("description");
                        Long imageResIdLong = doc.getLong("imageResId");

                        int imageResId = imageResIdLong != null
                                ? imageResIdLong.intValue()
                                : R.drawable.location_on_24px;

                        TourPackage tourPackage = new TourPackage(
                                title != null ? title : "",
                                duration != null ? duration : "",
                                price != null ? price : "",
                                description != null ? description : "",
                                imageResId
                        );

                        wishlist.add(tourPackage);
                    }

                    adapter.notifyDataSetChanged();

                    if (wishlist.isEmpty()) {
                        recyclerWishlist.setVisibility(View.GONE);
                        emptyWishlistCard.setVisibility(View.VISIBLE);
                    } else {
                        recyclerWishlist.setVisibility(View.VISIBLE);
                        emptyWishlistCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load wishlist: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}