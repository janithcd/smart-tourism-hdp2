package lk.janith.smart_tourism.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.janith.smart_tourism.R;

/** Small admin-only editor for the Firestore tour catalogue shown on Home and Explore. */
public class AdminToursActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private CollectionReference packages;
    private TextView status;
    private TextView editorTitle;
    private LinearLayout content;
    private LinearLayout tourList;
    private TextInputEditText title;
    private TextInputEditText duration;
    private TextInputEditText price;
    private TextInputEditText categoryId;
    private TextInputEditText description;
    private TextInputEditText route;
    private TextInputEditText overview;
    private TextInputEditText imageUrl;
    private SwitchMaterial active;
    private SwitchMaterial featured;
    private MaterialButton save;
    private String editingDocumentId;
    private boolean authorized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tours);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        packages = firestore.collection("packages");

        MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        status = findViewById(R.id.adminStatus);
        content = findViewById(R.id.adminContent);
        tourList = findViewById(R.id.adminTourList);
        editorTitle = findViewById(R.id.adminEditorTitle);
        title = findViewById(R.id.adminTitle);
        duration = findViewById(R.id.adminDuration);
        price = findViewById(R.id.adminPrice);
        categoryId = findViewById(R.id.adminCategoryId);
        description = findViewById(R.id.adminDescription);
        route = findViewById(R.id.adminRoute);
        overview = findViewById(R.id.adminOverview);
        imageUrl = findViewById(R.id.adminImageUrl);
        active = findViewById(R.id.adminActive);
        featured = findViewById(R.id.adminFeatured);
        save = findViewById(R.id.adminSave);

        findViewById(R.id.adminNew).setOnClickListener(v -> clearEditor());
        save.setOnClickListener(v -> saveTour());
        if (savedInstanceState != null) {
            editingDocumentId = savedInstanceState.getString("editing_document_id");
            if (editingDocumentId != null) editorTitle.setText(R.string.admin_edit_tour);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyAccess();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("editing_document_id", editingDocumentId);
        super.onSaveInstanceState(outState);
    }

    private void verifyAccess() {
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
                .addOnSuccessListener(this, document -> {
                    if (!isCurrentUser(uid)) return;
                    authorized = document.exists()
                            && Boolean.TRUE.equals(document.getBoolean("active"))
                            && "admin".equals(document.getString("role"));
                    if (!authorized) {
                        status.setText(R.string.admin_access_denied);
                        return;
                    }
                    content.setVisibility(View.VISIBLE);
                    status.setText(R.string.admin_access_granted);
                    loadTours();
                })
                .addOnFailureListener(this, error -> {
                    if (isCurrentUser(uid)) status.setText(R.string.admin_access_unavailable);
                });
    }

    private boolean isCurrentUser(String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && user != null && uid.equals(user.getUid());
    }

    private void loadTours() {
        if (!authorized) return;
        tourList.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText(R.string.admin_loading_tours);
        tourList.addView(loading);

        packages.get(Source.SERVER)
                .addOnSuccessListener(this, snapshot -> {
                    if (!authorized || isFinishing()) return;
                    tourList.removeAllViews();
                    List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
                    documents.sort(Comparator.comparing(
                            doc -> text(doc.getString("title")).toLowerCase(Locale.ROOT)));
                    if (documents.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(R.string.admin_no_tours);
                        tourList.addView(empty);
                    }
                    for (DocumentSnapshot doc : documents) {
                        MaterialButton item = new MaterialButton(this);
                        String label = text(doc.getString("title"));
                        item.setText(getString(R.string.admin_tour_row,
                                label.isEmpty() ? doc.getId() : label,
                                Boolean.FALSE.equals(doc.getBoolean("active"))
                                        ? getString(R.string.admin_hidden)
                                        : getString(R.string.admin_active)));
                        item.setTextAllCaps(false);
                        item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        item.setOnClickListener(v -> editTour(doc));
                        tourList.addView(item);
                    }
                })
                .addOnFailureListener(this, error -> {
                    if (authorized && !isFinishing()) {
                        status.setText(getString(R.string.admin_load_failed, error.getMessage()));
                    }
                });
    }

    private void editTour(DocumentSnapshot doc) {
        editingDocumentId = doc.getId();
        editorTitle.setText(R.string.admin_edit_tour);
        title.setText(text(doc.getString("title")));
        duration.setText(text(doc.getString("duration")));
        Object rawPrice = doc.get("price");
        price.setText(rawPrice instanceof Number
                ? String.format(Locale.US, "%.2f", ((Number) rawPrice).doubleValue())
                : rawPrice instanceof String ? (String) rawPrice : "");
        categoryId.setText(text(doc.getString("categoryId")));
        description.setText(text(doc.getString("description")));
        route.setText(text(doc.getString("route")));
        overview.setText(text(doc.getString("overview")));
        imageUrl.setText(text(doc.getString("imageUrl")));
        active.setChecked(!Boolean.FALSE.equals(doc.getBoolean("active")));
        featured.setChecked(Boolean.TRUE.equals(doc.getBoolean("featured")));
        status.setText(getString(R.string.admin_editing, title.getText()));
    }

    private void clearEditor() {
        editingDocumentId = null;
        editorTitle.setText(R.string.admin_new_tour);
        title.setText("");
        duration.setText("");
        price.setText("");
        categoryId.setText("");
        description.setText("");
        route.setText("");
        overview.setText("");
        imageUrl.setText("");
        active.setChecked(false);
        featured.setChecked(false);
        status.setText(R.string.admin_draft_notice);
    }

    private void saveTour() {
        if (!authorized || auth.getCurrentUser() == null) return;

        String tourTitle = value(title);
        String tourDuration = value(duration);
        String tourPrice = value(price);
        String tourCategory = value(categoryId);
        String tourDescription = value(description);
        String tourRoute = value(route);
        String tourOverview = value(overview);
        String tourImage = value(imageUrl);

        if (tourTitle.isEmpty() || tourTitle.length() > 100) {
            title.setError(getString(R.string.admin_title_error));
            title.requestFocus();
            return;
        }
        if (tourDuration.isEmpty() || tourDuration.length() > 40) {
            duration.setError(getString(R.string.admin_duration_error));
            duration.requestFocus();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(tourPrice.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            amount = -1;
        }
        if (!Double.isFinite(amount) || amount <= 0 || amount > 1000000) {
            price.setError(getString(R.string.admin_price_error));
            price.requestFocus();
            return;
        }
        if (tourCategory.length() > 100 || tourDescription.length() > 2000
                || tourRoute.length() > 2000 || tourOverview.length() > 2000) {
            status.setText(R.string.admin_text_too_long);
            return;
        }
        if (!tourImage.isEmpty() && (tourImage.length() > 500
                || !tourImage.startsWith("https://")
                || !Patterns.WEB_URL.matcher(tourImage).matches())) {
            imageUrl.setError(getString(R.string.admin_image_error));
            imageUrl.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", tourTitle);
        data.put("duration", tourDuration);
        data.put("price", amount);
        data.put("categoryId", tourCategory);
        data.put("description", tourDescription);
        data.put("route", tourRoute);
        data.put("overview", tourOverview);
        data.put("imageUrl", tourImage);
        data.put("active", active.isChecked());
        data.put("featured", featured.isChecked());

        DocumentReference ref = editingDocumentId == null
                ? packages.document() : packages.document(editingDocumentId);
        Task<Void> write = editingDocumentId == null ? ref.set(data) : ref.update(data);
        save.setEnabled(false);
        write.addOnSuccessListener(this, unused -> {
            save.setEnabled(true);
            editingDocumentId = ref.getId();
            editorTitle.setText(R.string.admin_edit_tour);
            status.setText(R.string.admin_saved);
            loadTours();
        }).addOnFailureListener(this, error -> {
            save.setEnabled(true);
            status.setText(getString(R.string.admin_save_failed, error.getMessage()));
            Toast.makeText(this, R.string.admin_save_failed_short, Toast.LENGTH_LONG).show();
        });
    }

    private static String value(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static String text(String value) {
        return TextUtils.isEmpty(value) ? "" : value;
    }
}
