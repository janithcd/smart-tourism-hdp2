package lk.janith.smart_tourism.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.data.BookingStore;

public class TourPackageDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DURATION = "extra_duration";
    public static final String EXTRA_PRICE = "extra_price";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_IMAGE_RES_ID = "extra_image_res_id";

    private ImageView imgPackage;
    private ImageView btnBack;
    private TextView txtTitle;
    private TextView txtDuration;
    private TextView txtPrice;
    private TextView txtRoute;
    private TextView txtOverview;
    private TextView txtHighlights;
    private MaterialButton btnBookNow;

    private TextInputLayout paxLayout;
    private TextInputLayout pickupLayout;
    private TextInputLayout mobileLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout timeLayout;

    private AutoCompleteTextView autoPax;
    private AutoCompleteTextView autoPickupLocation;
    private TextInputEditText editMobileNumber;
    private TextInputEditText editTravelDate;
    private TextInputEditText editTravelTime;

    private String title;
    private String duration;
    private String price;
    private String description;
    private int imageResId;

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

        paxLayout = findViewById(R.id.paxLayout);
        pickupLayout = findViewById(R.id.pickupLayout);
        mobileLayout = findViewById(R.id.mobileLayout);
        dateLayout = findViewById(R.id.dateLayout);
        timeLayout = findViewById(R.id.timeLayout);

        autoPax = findViewById(R.id.autoPax);
        autoPickupLocation = findViewById(R.id.autoPickupLocation);
        editMobileNumber = findViewById(R.id.editMobileNumber);
        editTravelDate = findViewById(R.id.editTravelDate);
        editTravelTime = findViewById(R.id.editTravelTime);

        title = getIntent().getStringExtra(EXTRA_TITLE);
        duration = getIntent().getStringExtra(EXTRA_DURATION);
        price = getIntent().getStringExtra(EXTRA_PRICE);
        description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        imageResId = getIntent().getIntExtra(EXTRA_IMAGE_RES_ID, R.drawable.location_on_24px);

        txtTitle.setText(title != null ? title : "");
        txtDuration.setText(duration != null ? duration : "");
        txtPrice.setText(price != null ? price : "");
        txtRoute.setText(description != null ? description : "");
        imgPackage.setImageResource(imageResId);

        txtOverview.setText(buildOverview(title, duration));
        txtHighlights.setText(buildHighlights(description));

        setupPaxDropdown();
        setupPickupDropdown(duration);
        editTravelDate.setOnClickListener(v -> showDatePicker());
        editTravelTime.setOnClickListener(v -> showTimePicker());

        btnBack.setOnClickListener(v -> finish());

        btnBookNow.setOnClickListener(v -> {
            paxLayout.setError(null);
            pickupLayout.setError(null);
            mobileLayout.setError(null);
            dateLayout.setError(null);
            timeLayout.setError(null);

            String paxValue = autoPax.getText() != null ? autoPax.getText().toString().trim() : "";
            String pickupValue = autoPickupLocation.getText() != null ? autoPickupLocation.getText().toString().trim() : "";
            String mobileValue = editMobileNumber.getText() != null ? editMobileNumber.getText().toString().trim() : "";
            String dateValue = editTravelDate.getText() != null ? editTravelDate.getText().toString().trim() : "";
            String timeValue = editTravelTime.getText() != null ? editTravelTime.getText().toString().trim() : "";

            boolean hasError = false;

            if (paxValue.isEmpty()) {
                paxLayout.setError(getString(R.string.package_select_pax_error));
                hasError = true;
            }

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

    private void setupPaxDropdown() {
        List<String> paxOptions = new ArrayList<>();
        paxOptions.add("1 Person");
        for (int i = 2; i <= 8; i++) {
            paxOptions.add(i + " People");
        }

        ArrayAdapter<String> paxAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                paxOptions
        );

        autoPax.setAdapter(paxAdapter);
        autoPax.setOnClickListener(v -> autoPax.showDropDown());
    }

    private void setupPickupDropdown(String durationValue) {
        List<String> pickupOptions = new ArrayList<>();

        if (durationValue != null && durationValue.startsWith("5")) {
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
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) ->
                editTravelDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(today.getTimeInMillis());
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
            String[] dateParts = dateValue.split("-");
            String[] timeParts = timeValue.split(":");
            Calendar selected = Calendar.getInstance();
            selected.set(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2]), Integer.parseInt(timeParts[0]),
                    Integer.parseInt(timeParts[1]));
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            return selected.after(Calendar.getInstance());
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException error) {
            return false;
        }
    }

    private void saveBookingDraft(String paxValue, String pickupValue, String mobileValue,
                                  String dateValue, String timeValue) {
        SharedPreferences preferences = BookingStore.draft(this);

        preferences.edit()
                .putString("package_title", title)
                .putString("package_duration", duration)
                .putString("package_price", price)
                .putString("package_description", description)
                .putInt("package_image_res_id", imageResId)
                .putString("package_pax", paxValue)
                .putString("package_pickup_location", pickupValue)
                .putString("package_mobile_number", mobileValue)
                .putString("package_travel_date", dateValue)
                .putString("package_travel_time", timeValue)
                .apply();
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
