package lk.janith.smart_tourism.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.data.BookingStore;
import lk.janith.smart_tourism.data.FirebaseBookingStore;
import lk.janith.smart_tourism.data.StripeSandboxApi;
import lk.janith.smart_tourism.receiver.BookingConfirmationReceiver;

/** Demo choices plus an optional server-verified, debug-only Stripe test checkout. */
public class PaymentActivity extends AppCompatActivity {
    private static final String PENDING_METHOD = "pending_payment_method";
    private static final String TEST_ORDER = "order_id";
    private String pendingPaymentMethod;
    private RadioGroup paymentMethods;
    private boolean saving;
    private BookingStore.Booking pendingConfirmedBooking;
    private boolean checkingStripe;
    private TextView stripeStatus;

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

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        SharedPreferences draft = BookingStore.draft(this);
        if (draft.getString("package_title", "").isEmpty() && pendingStripeOrder().isEmpty()) {
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
        stripeStatus = findViewById(R.id.stripeStatus);
        findViewById(R.id.paymentStripeTest).setVisibility(StripeSandboxApi.available()
                && !draft.getString("package_catalog_id", "").isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnCheckStripe).setOnClickListener(v -> checkStripeOrder());
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
        if (!pendingStripeOrder().isEmpty()) checkStripeOrder();
    }

    private void confirmSelection(SharedPreferences draft) {
        int selected = paymentMethods.getCheckedRadioButtonId();
        if (selected == R.id.paymentStripeTest) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.payment_stripe_test)
                    .setMessage(R.string.payment_stripe_start)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.payment_stripe_test, (dialog, which) -> startStripe(draft))
                    .show();
            return;
        }
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

    private SharedPreferences stripePending() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("Sign in to check the test payment");
        return getSharedPreferences("stripe_sandbox_pending_" + user.getUid(), MODE_PRIVATE);
    }

    private String pendingStripeOrder() {
        try { return stripePending().getString(TEST_ORDER, ""); }
        catch (IllegalStateException ignored) { return ""; }
    }

    private boolean currentUser(String uid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return !isFinishing() && !isDestroyed() && user != null && uid.equals(user.getUid());
    }

    private void showStripeStatus(int message) {
        stripeStatus.setVisibility(View.VISIBLE);
        stripeStatus.setText(message);
    }

    private void setStripeBusy(boolean busy) {
        findViewById(R.id.btnConfirmPayment).setEnabled(!busy && pendingStripeOrder().isEmpty());
        findViewById(R.id.btnCheckStripe).setEnabled(!busy);
    }

    private void startStripe(SharedPreferences draft) {
        if (saving || checkingStripe) return;
        if (!pendingStripeOrder().isEmpty()) { checkStripeOrder(); return; }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        saving = true;
        setStripeBusy(true);
        showStripeStatus(R.string.payment_stripe_opening);
        StripeSandboxApi.start(draft, new StripeSandboxApi.Callback() {
            @Override public void onSuccess(JSONObject result) {
                if (!currentUser(uid)) return;
                saving = false;
                setStripeBusy(false);
                String id = result.optString("orderId");
                String url = result.optString("url");
                if (!id.matches("[A-Za-z0-9]{8,60}") || !safeCheckoutUrl(url)
                        || !stripePending().edit().putString(TEST_ORDER, id).commit()) {
                    stripeStatus.setText(R.string.booking_save_failed);
                    return;
                }
                showStripeStatus(R.string.payment_stripe_pending);
                setStripeBusy(false);
                findViewById(R.id.btnCheckStripe).setVisibility(View.VISIBLE);
                openCheckout(url);
            }

            @Override public void onError(String error) {
                if (!currentUser(uid)) return;
                saving = false;
                setStripeBusy(false);
                stripeStatus.setText(getString(R.string.payment_stripe_failed, error));
            }
        });
    }

    private static boolean safeCheckoutUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            return "https".equals(uri.getScheme()) && "checkout.stripe.com".equals(uri.getHost());
        } catch (RuntimeException ignored) { return false; }
    }

    private void openCheckout(String url) {
        if (!safeCheckoutUrl(url)) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (RuntimeException error) {
            stripeStatus.setText(getString(R.string.payment_stripe_failed,
                    "Install a browser and check the status again."));
        }
    }

    private void checkStripeOrder() {
        String order = pendingStripeOrder();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (order.isEmpty() || user == null || checkingStripe || saving) return;
        String uid = user.getUid();
        checkingStripe = true;
        setStripeBusy(true);
        showStripeStatus(R.string.payment_stripe_checking);
        findViewById(R.id.btnCheckStripe).setVisibility(View.VISIBLE);
        StripeSandboxApi.status(order, new StripeSandboxApi.Callback() {
            @Override public void onSuccess(JSONObject result) {
                if (!currentUser(uid) || !order.equals(pendingStripeOrder())) return;
                checkingStripe = false;
                setStripeBusy(false);
                String state = result.optString("status");
                if ("paid".equals(state)) {
                    BookingStore.Booking booking = BookingStore.prepareStripeTest(result);
                    if (booking == null || !BookingStore.saveConfirmed(getApplicationContext(), booking, uid)) {
                        stripeStatus.setText(R.string.booking_save_failed);
                        return;
                    }
                    BookingStore.clearDraftIfListing(getApplicationContext(), uid,
                            result.optString("listingId"));
                    stripePending().edit().remove(TEST_ORDER).apply();
                    findViewById(R.id.btnCheckStripe).setVisibility(View.GONE);
                    showSaved(booking);
                } else if ("expired".equals(state) || "failed".equals(state)) {
                    stripePending().edit().remove(TEST_ORDER).apply();
                    stripeStatus.setText(R.string.payment_stripe_expired);
                    findViewById(R.id.btnCheckStripe).setVisibility(View.GONE);
                } else {
                    stripeStatus.setText(R.string.payment_stripe_pending);
                    String url = result.optString("checkoutUrl");
                    if (safeCheckoutUrl(url)) {
                        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnCheckStripe))
                                .setText(R.string.payment_stripe_resume);
                        findViewById(R.id.btnCheckStripe).setOnClickListener(v -> openCheckout(url));
                    } else {
                        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnCheckStripe))
                                .setText(R.string.payment_stripe_check);
                        findViewById(R.id.btnCheckStripe).setOnClickListener(v -> checkStripeOrder());
                    }
                }
            }

            @Override public void onError(String error) {
                if (!currentUser(uid)) return;
                checkingStripe = false;
                setStripeBusy(false);
                stripeStatus.setText(getString(R.string.payment_stripe_failed, error));
            }
        });
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
                .setTitle("stripe_test".equals(booking.source)
                        ? R.string.payment_stripe_saved_title : R.string.booking_saved_title)
                .setMessage("stripe_test".equals(booking.source)
                        ? R.string.payment_stripe_saved_message : R.string.booking_saved_message)
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
