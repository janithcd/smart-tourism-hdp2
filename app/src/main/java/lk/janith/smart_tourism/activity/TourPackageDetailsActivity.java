package lk.janith.smart_tourism.activity;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.data.BookingSelection;
import lk.janith.smart_tourism.data.BookingStore;
import lk.janith.smart_tourism.data.ReviewStore;
import lk.janith.smart_tourism.data.ServiceCatalog;
import lk.janith.smart_tourism.model.TourPackage;

public class TourPackageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_ID = "packageId";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DURATION = "extra_duration";
    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_IMAGE_RES_ID = "extra_image_res_id";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_ROUTE = "extra_route";
    public static final String EXTRA_OVERVIEW = "extra_overview";
    public static final String EXTRA_SERVICE_TYPE = "extra_service_type";
    public static final String EXTRA_CAPACITY = "extra_capacity";
    public static final String EXTRA_PICKUP = "extra_pickup";
    public static final String EXTRA_SERVICE_LIVE = "extra_service_live";
    public static final String TYPE_TOUR = "Tour";

    private ImageView imgPackage;
    private ImageView btnBack;
    private TextView txtTitle;
    private TextView txtDuration;
    private TextView txtPrice;
    private TextView txtRoute;
    private TextView txtOverview;
    private TextView txtHighlights;
    private MaterialButton btnBookNow;

    private TextInputLayout pickupLayout;
    private TextInputLayout mobileLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout timeLayout;

    private AutoCompleteTextView autoPickupLocation;
    private TextInputEditText editMobileNumber;
    private TextInputEditText editTravelDate;
    private TextInputEditText editTravelTime;
    private TextView passengerValue;
    private MaterialButton passengerMinus;
    private MaterialButton passengerPlus;
    private MaterialButton dateMinus;
    private int passengerCount = 1;
    private int maxPeople;

    private String title;
    private String duration;
    private String price;
    private String description;
    private String serviceType;
    private int imageResId;
    private String imageUrl;
    private int capacity;
    private String servicePickup;

    public static Intent intentFor(Context context, TourPackage tour) {
        return new Intent(context, TourPackageDetailsActivity.class)
                .putExtra(EXTRA_PACKAGE_ID, tour.getId())
                .putExtra(EXTRA_TITLE, tour.getTitle())
                .putExtra(EXTRA_DURATION, tour.getDuration())
                .putExtra(EXTRA_PRICE, tour.getPrice())
                .putExtra(EXTRA_DESCRIPTION, tour.getDescription())
                .putExtra(EXTRA_ROUTE, tour.getRoute())
                .putExtra(EXTRA_OVERVIEW, tour.getOverview())
                .putExtra(EXTRA_IMAGE_URL, tour.getImageUrl())
                .putExtra(EXTRA_IMAGE_RES_ID, tour.getImageResId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour_package_details);

        imgPackage = findViewById(R.id.imgPackage);
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        txtDuration = findViewById(R.id.txtDuration);
        txtPrice = findViewById(R.id.txtPrice);
        txtRoute = findViewById(R.id.txtRoute);
        txtOverview = findViewById(R.id.txtOverview);
        txtHighlights = findViewById(R.id.txtHighlights);
        btnBookNow = findViewById(R.id.btnBookNow);

        pickupLayout = findViewById(R.id.pickupLayout);
        mobileLayout = findViewById(R.id.mobileLayout);
        dateLayout = findViewById(R.id.dateLayout);
        timeLayout = findViewById(R.id.timeLayout);

        autoPickupLocation = findViewById(R.id.autoPickupLocation);
        editMobileNumber = findViewById(R.id.editMobileNumber);
        editTravelDate = findViewById(R.id.editTravelDate);
        editTravelTime = findViewById(R.id.editTravelTime);
        passengerValue = findViewById(R.id.passengerValue);
        passengerMinus = findViewById(R.id.passengerMinus);
        passengerPlus = findViewById(R.id.passengerPlus);
        dateMinus = findViewById(R.id.dateMinus);

        title = getIntent().getStringExtra(EXTRA_TITLE);
        duration = getIntent().getStringExtra(EXTRA_DURATION);
        price = getIntent().getStringExtra(EXTRA_PRICE);
        description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String route = getIntent().getStringExtra(EXTRA_ROUTE);
        String overview = getIntent().getStringExtra(EXTRA_OVERVIEW);
        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String requestedType = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);
        serviceType = ServiceCatalog.VEHICLE.equals(requestedType) || ServiceCatalog.GUIDE.equals(requestedType)
                ? requestedType : TYPE_TOUR;
        capacity = getIntent().getIntExtra(EXTRA_CAPACITY, 8);
        servicePickup = getIntent().getStringExtra(EXTRA_PICKUP);
        boolean liveService = getIntent().getBooleanExtra(EXTRA_SERVICE_LIVE, false);
        imageResId = getIntent().getIntExtra(EXTRA_IMAGE_RES_ID, R.drawable.location_on_24px);

        txtTitle.setText(title != null ? title : "");
        txtDuration.setText(duration != null ? duration : "");
        txtPrice.setText(price != null ? price : "");
        txtRoute.setText(route != null && !route.trim().isEmpty() ? route
                : description != null ? description : "");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(imageResId).error(imageResId).into(imgPackage);
        } else {
            imgPackage.setImageResource(imageResId);
        }

        ((TextView) findViewById(R.id.detailType)).setText(getString(
                liveService ? R.string.service_detail_live : R.string.service_detail_type, serviceType));
        if (!TYPE_TOUR.equals(serviceType)) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                imgPackage.setBackgroundResource(R.color.md_theme_primaryContainer);
                imgPackage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                int padding = (int) (getResources().getDisplayMetrics().density * 72);
                imgPackage.setPadding(padding, padding, padding, padding);
            }
            ((TextView) findViewById(R.id.detailDescriptionLabel)).setText(R.string.service_description_label);
            ((TextView) findViewById(R.id.detailHighlightsLabel)).setText(R.string.service_highlights_label);
        }

        txtOverview.setText(overview != null && !overview.trim().isEmpty() ? overview
                : TYPE_TOUR.equals(serviceType) ? buildOverview(title, duration)
                : getString(liveService ? R.string.service_live_overview : R.string.service_overview_text,
                        title, serviceType.toLowerCase(Locale.ROOT)));
        txtHighlights.setText(buildHighlights(description));

        setupPassengerStepper(savedInstanceState);
        setupPickupDropdown(duration);
        setupReview();
        if (savedInstanceState != null) {
            editTravelDate.setText(savedInstanceState.getString("selected_travel_date", ""));
        }
        editTravelDate.setOnClickListener(v -> showDatePicker());
        dateLayout.setEndIconOnClickListener(v -> showDatePicker());
        dateMinus.setOnClickListener(v -> moveDate(-1));
        findViewById(R.id.datePlus).setOnClickListener(v -> moveDate(1));
        updateDateControls();
        editTravelTime.setOnClickListener(v -> showTimePicker());

        btnBack.setOnClickListener(v -> finish());

        btnBookNow.setOnClickListener(v -> {
            pickupLayout.setError(null);
            mobileLayout.setError(null);
            dateLayout.setError(null);
            timeLayout.setError(null);

            String paxValue = passengerCount == 1 ? getString(R.string.booking_one_person)
                    : getString(R.string.booking_people_count, passengerCount);
            String pickupValue = autoPickupLocation.getText() != null ? autoPickupLocation.getText().toString().trim() : "";
            String mobileValue = editMobileNumber.getText() != null ? editMobileNumber.getText().toString().trim() : "";
            String dateValue = editTravelDate.getText() != null ? editTravelDate.getText().toString().trim() : "";
            String timeValue = editTravelTime.getText() != null ? editTravelTime.getText().toString().trim() : "";

            boolean hasError = false;

            if (pickupValue.isEmpty()) {
                pickupLayout.setError(getString(R.string.package_select_pickup_error));
                hasError = true;
            }

            if (mobileValue.isEmpty()) {
                mobileLayout.setError(getString(R.string.package_mobile_error_empty));
                hasError = true;
            } else if (!isValidMobileNumber(mobileValue)) {
                mobileLayout.setError(getString(R.string.package_mobile_error_invalid));
                hasError = true;
            }

            if (dateValue.isEmpty()) {
                dateLayout.setError(getString(R.string.booking_date_required));
                hasError = true;
            }

            if (timeValue.isEmpty()) {
                timeLayout.setError(getString(R.string.booking_time_required));
                hasError = true;
            }

            if (!dateValue.isEmpty() && !timeValue.isEmpty() && !isFutureBooking(dateValue, timeValue)) {
                timeLayout.setError(getString(R.string.booking_future_required));
                hasError = true;
            }

            if (hasError) return;

            saveBookingDraft(paxValue, pickupValue, mobileValue, dateValue, timeValue);

            Toast.makeText(
                    this,
                    getString(R.string.saved_to_my_activities),
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_my_activities", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("selected_passengers", passengerCount);
        state.putString("selected_travel_date", editTravelDate.getText() == null
                ? "" : editTravelDate.getText().toString());
        super.onSaveInstanceState(state);
    }

    private void setupPassengerStepper(Bundle state) {
        maxPeople = BookingSelection.maxTravelers(TYPE_TOUR.equals(serviceType) ? 8 : capacity);
        passengerCount = BookingSelection.travelers(
                state == null ? 1 : state.getInt("selected_passengers", 1), maxPeople);
        ((TextView) findViewById(R.id.passengerCapacity)).setText(
                getString(R.string.booking_capacity_hint, maxPeople));
        passengerMinus.setOnClickListener(v -> {
            passengerCount = BookingSelection.travelers(passengerCount - 1, maxPeople);
            updatePassengerControls();
        });
        passengerPlus.setOnClickListener(v -> {
            passengerCount = BookingSelection.travelers(passengerCount + 1, maxPeople);
            updatePassengerControls();
        });
        updatePassengerControls();
    }

    private void updatePassengerControls() {
        passengerValue.setText(String.valueOf(passengerCount));
        passengerValue.setContentDescription(getString(
                R.string.booking_traveler_count_accessibility, passengerCount, maxPeople));
        passengerMinus.setEnabled(passengerCount > 1);
        passengerPlus.setEnabled(passengerCount < maxPeople);
    }

    private void moveDate(int offset) {
        String current = editTravelDate.getText() == null
                ? "" : editTravelDate.getText().toString();
        editTravelDate.setText(BookingSelection.shiftDay(current, offset, Calendar.getInstance()));
        dateLayout.setError(null);
        updateDateControls();
    }

    private void updateDateControls() {
        String current = editTravelDate.getText() == null
                ? "" : editTravelDate.getText().toString();
        dateMinus.setEnabled(BookingSelection.canPreviousDay(current, Calendar.getInstance()));
    }

    private void setupPickupDropdown(String durationValue) {
        List<String> pickupOptions = new ArrayList<>();

        if (!TYPE_TOUR.equals(serviceType) && servicePickup != null && !servicePickup.trim().isEmpty()) {
            pickupOptions.add(servicePickup);
        } else if (ServiceCatalog.VEHICLE.equals(serviceType) && title != null && title.contains("Airport")) {
            pickupOptions.add("Bandaranaike International Airport");
            pickupOptions.add("Colombo Hotel");
        } else if (ServiceCatalog.GUIDE.equals(serviceType) && title != null && title.contains("Heritage")) {
            pickupOptions.add("Sigiriya Meeting Point");
        } else if (ServiceCatalog.GUIDE.equals(serviceType) && title != null && title.contains("Kandy")) {
            pickupOptions.add("Kandy City Meeting Point");
        } else if (ServiceCatalog.VEHICLE.equals(serviceType) || ServiceCatalog.GUIDE.equals(serviceType)) {
            pickupOptions.add("Colombo Hotel");
            pickupOptions.add("Colombo City Pickup");
        } else if (durationValue != null && durationValue.startsWith("5")) {
            pickupOptions.add("Colombo Hotel");
            pickupOptions.add("Colombo City Pickup");
            pickupOptions.add("Colombo Railway Station");
            pickupOptions.add("Pettah / Fort Area");
        } else {
            pickupOptions.add("Negombo Hotel");
            pickupOptions.add("Negombo City Pickup");
            pickupOptions.add("Bandaranaike International Airport");
        }

        ArrayAdapter<String> pickupAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                pickupOptions
        );

        autoPickupLocation.setAdapter(pickupAdapter);
        autoPickupLocation.setOnClickListener(v -> autoPickupLocation.showDropDown());
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        String cleaned = mobileNumber.replace(" ", "").replace("-", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.matches("\\d{8,15}");
    }

    private void showDatePicker() {
        Calendar today = Calendar.getInstance();
        Calendar selected = BookingSelection.parseDate(editTravelDate.getText() == null
                ? "" : editTravelDate.getText().toString());
        if (selected == null || selected.before(BookingSelection.startOfDay(today))) selected = today;
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            editTravelDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day));
            dateLayout.setError(null);
            updateDateControls();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(BookingSelection.startOfDay(today).getTimeInMillis());
        picker.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) ->
                editTravelTime.setText(String.format(Locale.US, "%02d:%02d", hour, minute)),
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private boolean isFutureBooking(String dateValue, String timeValue) {
        try {
            String[] timeParts = timeValue.split(":");
            Calendar selected = BookingSelection.parseDate(dateValue);
            if (selected == null || timeParts.length != 2) return false;
            selected.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            selected.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            selected.getTimeInMillis();
            return selected.after(Calendar.getInstance());
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private void saveBookingDraft(String paxValue, String pickupValue, String mobileValue,
                                  String dateValue, String timeValue) {
        SharedPreferences preferences = BookingStore.draft(this);

        preferences.edit()
                .putString("package_title", title)
                .putString("package_catalog_id",
                        getIntent().getStringExtra(EXTRA_PACKAGE_ID) == null ? ""
                                : getIntent().getStringExtra(EXTRA_PACKAGE_ID))
                .putString("package_service_type", serviceType)
                .putString("package_duration", duration)
                .putString("package_price", price)
                .putString("package_description", txtRoute.getText().toString())
                .putInt("package_image_res_id", imageResId)
                .putString("package_image_url", imageUrl != null ? imageUrl : "")
                .putString("package_pax", paxValue)
                .putString("package_pickup_location", pickupValue)
                .putString("package_mobile_number", mobileValue)
                .putString("package_travel_date", dateValue)
                .putString("package_travel_time", timeValue)
                .apply();
    }

    private void setupReview() {
        RatingBar rating = findViewById(R.id.reviewRating);
        TextInputLayout commentLayout = findViewById(R.id.reviewCommentLayout);
        TextInputEditText comment = findViewById(R.id.reviewComment);
        TextView status = findViewById(R.id.reviewStatus);
        MaterialButton remove = findViewById(R.id.btnRemoveReview);
        ReviewStore.Review saved = ReviewStore.get(this, serviceType, title);
        if (saved != null) {
            rating.setRating(saved.stars);
            comment.setText(saved.comment);
            status.setText(getString(R.string.review_saved_status, saved.stars));
            remove.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnSaveReview).setOnClickListener(v -> {
            commentLayout.setError(null);
            String value = comment.getText() == null ? "" : comment.getText().toString().trim();
            int stars = (int) rating.getRating();
            if (stars < 1) {
                Toast.makeText(this, R.string.review_rating_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (value.isEmpty()) {
                commentLayout.setError(getString(R.string.review_comment_required));
                return;
            }
            if (ReviewStore.save(this, serviceType, title, stars, value)) {
                status.setText(getString(R.string.review_saved_status, stars));
                remove.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.review_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.review_save_failed, Toast.LENGTH_LONG).show();
            }
        });
        remove.setOnClickListener(v -> {
            if (ReviewStore.remove(this, serviceType, title)) {
                rating.setRating(0);
                comment.setText("");
                commentLayout.setError(null);
                status.setText(R.string.review_none);
                remove.setVisibility(View.GONE);
                Toast.makeText(this, R.string.review_removed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.review_save_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String buildOverview(String titleValue, String durationValue) {
        String safeTitle = titleValue != null ? titleValue : getString(R.string.package_default_title);
        String safeDuration = durationValue != null ? durationValue : getString(R.string.package_default_duration);

        return getString(R.string.package_overview_text, safeTitle, safeDuration);
    }

    private String buildHighlights(String descriptionValue) {
        if (descriptionValue == null || descriptionValue.trim().isEmpty()) {
            return getString(R.string.package_highlights_empty);
        }

        String[] parts = descriptionValue.split("•");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("• ").append(trimmed);
            }
        }

        return builder.toString();
    }
}
