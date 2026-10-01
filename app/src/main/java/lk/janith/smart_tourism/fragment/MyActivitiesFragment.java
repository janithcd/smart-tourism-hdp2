package lk.janith.smart_tourism.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import lk.janith.smart_tourism.R;

public class MyActivitiesFragment extends Fragment {

    private ImageView imgPackage;
    private TextView txtTitle;
    private TextView txtDuration;
    private TextView txtPrice;
    private TextView txtRoute;
    private TextView txtPax;
    private TextView txtPickup;
    private TextView txtMobile;

    private View emptyStateCard;
    private View activityCard;

    private MaterialButton btnRemoveTour;
    private MaterialButton btnCheckout;

    public MyActivitiesFragment() {
        super(R.layout.fragment_my_activities);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgPackage = view.findViewById(R.id.imgPackage);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtDuration = view.findViewById(R.id.txtDuration);
        txtPrice = view.findViewById(R.id.txtPrice);
        txtRoute = view.findViewById(R.id.txtRoute);
        txtPax = view.findViewById(R.id.txtPax);
        txtPickup = view.findViewById(R.id.txtPickup);
        txtMobile = view.findViewById(R.id.txtMobile);

        emptyStateCard = view.findViewById(R.id.emptyStateCard);
        activityCard = view.findViewById(R.id.activityCard);

        btnRemoveTour = view.findViewById(R.id.btnRemoveTour);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        loadDraftBooking();

        btnRemoveTour.setOnClickListener(v -> {
            clearDraftBooking();
            loadDraftBooking();
            Toast.makeText(requireContext(), getString(R.string.tour_removed_success), Toast.LENGTH_SHORT).show();
        });

        btnCheckout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), getString(R.string.checkout_coming_soon), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDraftBooking();
    }

    private void loadDraftBooking() {
        SharedPreferences preferences = requireContext()
                .getSharedPreferences("smart_tourism_booking_draft", Context.MODE_PRIVATE);

        String title = preferences.getString("package_title", "");
        String duration = preferences.getString("package_duration", "");
        String price = preferences.getString("package_price", "");
        String description = preferences.getString("package_description", "");
        String pax = preferences.getString("package_pax", "");
        String pickup = preferences.getString("package_pickup_location", "");
        String mobile = preferences.getString("package_mobile_number", "");
        int imageResId = preferences.getInt("package_image_res_id", R.drawable.location_on_24px);

        boolean hasBooking = title != null && !title.trim().isEmpty();

        if (!hasBooking) {
            activityCard.setVisibility(View.GONE);
            emptyStateCard.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(false);
            btnCheckout.setAlpha(0.5f);
            return;
        }

        activityCard.setVisibility(View.VISIBLE);
        emptyStateCard.setVisibility(View.GONE);
        btnCheckout.setEnabled(true);
        btnCheckout.setAlpha(1f);

        imgPackage.setImageResource(imageResId);
        txtTitle.setText(title);
        txtDuration.setText(duration);
        txtPrice.setText(price);
        txtRoute.setText(description);
        txtPax.setText(pax);
        txtPickup.setText(pickup);
        txtMobile.setText(mobile);
    }

    private void clearDraftBooking() {
        SharedPreferences preferences = requireContext()
                .getSharedPreferences("smart_tourism_booking_draft", Context.MODE_PRIVATE);

        preferences.edit().clear().apply();
    }
}