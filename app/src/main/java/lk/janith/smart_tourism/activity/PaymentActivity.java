package lk.janith.smart_tourism.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.data.BookingStore;
import lk.janith.smart_tourism.data.FirebaseBookingStore;
import lk.janith.smart_tourism.receiver.BookingConfirmationReceiver;

/** Simulates a payment choice without collecting card details or contacting a payment service. */
public class PaymentActivity extends AppCompatActivity {
    private static final String PENDING_METHOD = "pending_payment_method";
    private String pendingPaymentMethod;
    private RadioGroup paymentMethods;
    private boolean saving;
    private BookingStore.Booking pendingConfirmedBooking;

    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (pendingPaymentMethod != null) {
                    String method = pendingPaymentMethod;
                    pendingPaymentMethod = null;
                    saveBooking(method, granted);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        if (savedInstanceState != null) {
            pendingPaymentMethod = savedInstanceState.getString(PENDING_METHOD);
        }

        SharedPreferences draft = BookingStore.draft(this);
        if (draft.getString("package_title", "").isEmpty()) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.paymentTourTitle)).setText(
                draft.getString("package_title", ""));
        ((TextView) findViewById(R.id.paymentPrice)).setText(
                draft.getString("package_price", ""));
        ((TextView) findViewById(R.id.paymentDateTime)).setText(getString(
                R.string.booking_date_time_value,
                draft.getString("package_travel_date", ""),
                draft.getString("package_travel_time", "")));

        paymentMethods = findViewById(R.id.paymentMethods);
        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> confirmSelection(draft));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(PENDING_METHOD, pendingPaymentMethod);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingConfirmedBooking != null) {
            BookingStore.Booking booking = pendingConfirmedBooking;
            pendingConfirmedBooking = null;
            showSaved(booking);
        }
    }

    private void confirmSelection(SharedPreferences draft) {
        int selected = paymentMethods.getCheckedRadioButtonId();
        final String method;
        if (selected == R.id.paymentCard) {
            method = "Card (simulation)";
        } else if (selected == R.id.paymentWallet) {
            method = "Mobile wallet (simulation)";
        } else if (selected == R.id.paymentCash) {
            method = "Cash on arrival";
        } else {
            Toast.makeText(this, R.string.payment_select_method, Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.booking_confirm_title)
                .setMessage(getString(R.string.booking_confirm_message,
                        draft.getString("package_title", ""),
                        draft.getString("package_travel_date", ""),
                        draft.getString("package_travel_time", "")))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.booking_confirm_action, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= 33
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        pendingPaymentMethod = method;
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        saveBooking(method, true);
                    }
                })
                .show();
    }

    private void saveBooking(String method, boolean canNotify) {
        if (saving) return;
        BookingStore.Booking booking = BookingStore.prepareDraft(this, method);
        if (booking == null) {
            Toast.makeText(this, R.string.booking_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.booking_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        String ownerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        saving = true;
        findViewById(R.id.btnConfirmPayment).setEnabled(false);
        Toast.makeText(this, R.string.booking_saving_cloud, Toast.LENGTH_SHORT).show();
        try {
            FirebaseBookingStore.submit(booking)
                    .addOnSuccessListener(unused -> {
                        saving = false;
                        if (!BookingStore.saveConfirmed(getApplicationContext(), booking, ownerUid)) {
                            FirebaseBookingStore.remove(booking.id);
                            if (!isFinishing() && !isDestroyed()) {
                                findViewById(R.id.btnConfirmPayment).setEnabled(true);
                                Toast.makeText(this, R.string.booking_save_failed, Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        if (canNotify) postConfirmationNotification(booking);
                        if (isFinishing() || isDestroyed()) return;
                        if (FirebaseAuth.getInstance().getCurrentUser() == null
                                || !ownerUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            finish();
                            return;
                        }
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            showSaved(booking);
                        } else {
                            pendingConfirmedBooking = booking;
                        }
                    })
                    .addOnFailureListener(error -> {
                        saving = false;
                        if (isFinishing() || isDestroyed()) return;
                        findViewById(R.id.btnConfirmPayment).setEnabled(true);
                        Toast.makeText(this,
                                getString(R.string.booking_cloud_failed, error.getMessage()),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (IllegalStateException error) {
            saving = false;
            findViewById(R.id.btnConfirmPayment).setEnabled(true);
            Toast.makeText(this, R.string.booking_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void postConfirmationNotification(BookingStore.Booking booking) {
        Intent notification = new Intent(this, BookingConfirmationReceiver.class);
        notification.setAction(BookingConfirmationReceiver.ACTION_BOOKING_SAVED);
        notification.putExtra(BookingConfirmationReceiver.EXTRA_TITLE, booking.title);
        notification.putExtra(BookingConfirmationReceiver.EXTRA_REFERENCE, booking.id);
        getApplicationContext().sendBroadcast(notification);
    }

    private void showSaved(BookingStore.Booking booking) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.booking_saved_title)
                .setMessage(R.string.booking_saved_message)
                .setCancelable(false)
                .setPositiveButton(R.string.booking_view_history, (dialog, which) -> openBookings())
                .show();
    }

    private void openBookings() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_bookings", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
