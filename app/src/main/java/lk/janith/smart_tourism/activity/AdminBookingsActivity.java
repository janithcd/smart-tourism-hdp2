package lk.janith.smart_tourism.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import lk.janith.smart_tourism.R;

/** Shows only Firestore demo submissions; older on-device bookings are not available to admins. */
public class AdminBookingsActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private TextView status;
    private LinearLayout content;
    private LinearLayout list;
    private boolean authorized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);
        MaterialToolbar toolbar = findViewById(R.id.adminBookingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        status = findViewById(R.id.adminBookingsStatus);
        content = findViewById(R.id.adminBookingsContent);
        list = findViewById(R.id.adminBookingsList);
        findViewById(R.id.adminBookingsRefresh).setOnClickListener(v -> loadBookings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        authorized = false;
        content.setVisibility(View.GONE);
        status.setText(R.string.admin_checking_access);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        String uid = user.getUid();
        firestore.collection("admins").document(uid).get(Source.SERVER)
                .addOnSuccessListener(this, doc -> {
                    if (!isCurrentUser(uid)) return;
                    authorized = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("active"))
                            && "admin".equals(doc.getString("role"));
                    if (!authorized) {
                        status.setText(R.string.admin_access_denied);
                        return;
                    }
                    content.setVisibility(View.VISIBLE);
                    loadBookings();
                })
                .addOnFailureListener(this, error -> {
                    if (isCurrentUser(uid)) status.setText(R.string.admin_access_unavailable);
                });
    }

    private boolean isCurrentUser(String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && user != null && uid.equals(user.getUid());
    }

    private void loadBookings() {
        if (!authorized) return;
        status.setText(R.string.admin_loading_bookings);
        list.removeAllViews();
        firestore.collection("demo_bookings")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get(Source.SERVER)
                .addOnSuccessListener(this, snapshot -> {
                    if (!authorized || isFinishing()) return;
                    list.removeAllViews();
                    status.setText(snapshot.isEmpty() ? R.string.admin_no_bookings
                            : R.string.admin_bookings_loaded);
                    for (DocumentSnapshot doc : snapshot.getDocuments()) addBooking(doc);
                })
                .addOnFailureListener(this, error -> {
                    if (authorized && !isFinishing())
                        status.setText(getString(R.string.admin_bookings_load_failed, error.getMessage()));
                });
    }

    private void addBooking(DocumentSnapshot doc) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        card.setLayoutParams(cardParams);
        LinearLayout body = new LinearLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        body.setPadding(padding, padding, padding, padding);
        body.setOrientation(LinearLayout.VERTICAL);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.admin_booking_heading,
                value(doc, "title"), value(doc, "reviewStatus")));
        heading.setTextSize(18);
        heading.setTextColor(getColor(R.color.md_theme_onSurface));
        body.addView(heading);

        TextView details = new TextView(this);
        details.setText(getString(R.string.admin_booking_details,
                value(doc, "email"), value(doc, "mobile"), value(doc, "date"),
                value(doc, "time"), value(doc, "pax"), value(doc, "pickup"),
                value(doc, "price"), value(doc, "paymentMethod"),
                doc.getId().length() >= 8 ? doc.getId().substring(0, 8) : doc.getId()));
        details.setTextColor(getColor(R.color.md_theme_onSurfaceVariant));
        body.addView(details);

        if ("New".equals(doc.getString("reviewStatus"))) {
            MaterialButton reviewed = new MaterialButton(this);
            reviewed.setText(R.string.admin_mark_reviewed);
            reviewed.setAllCaps(false);
            reviewed.setOnClickListener(v -> {
                if (!authorized) return;
                reviewed.setEnabled(false);
                doc.getReference().update("reviewStatus", "Reviewed")
                        .addOnSuccessListener(this, unused -> loadBookings())
                        .addOnFailureListener(this, error -> {
                            reviewed.setEnabled(true);
                            status.setText(getString(R.string.admin_booking_update_failed,
                                    error.getMessage()));
                        });
            });
            body.addView(reviewed);
        }
        card.addView(body);
        list.addView(card);
    }

    private static String value(DocumentSnapshot doc, String field) {
        String text = doc.getString(field);
        return text == null ? "" : text;
    }
}
