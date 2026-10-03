package lk.janith.smart_tourism.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
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
import lk.janith.smart_tourism.data.ServiceCatalog;

/** Admin editor for published vehicle and guide listings; no provider or availability claims. */
public final class AdminServicesActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private CollectionReference services;
    private TextView status;
    private TextView editorTitle;
    private LinearLayout content;
    private LinearLayout serviceList;
    private MaterialAutoCompleteTextView typePicker;
    private TextInputEditText title;
    private TextInputEditText duration;
    private TextInputEditText price;
    private TextInputEditText capacity;
    private TextInputEditText pickup;
    private TextInputEditText description;
    private TextInputEditText imageUrl;
    private SwitchMaterial active;
    private MaterialButton save;
    private String selectedType = ServiceCatalog.VEHICLE;
    private String editingId;
    private boolean authorized;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_services);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        services = firestore.collection("services");

        MaterialToolbar toolbar = findViewById(R.id.servicesToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        status = findViewById(R.id.servicesStatus);
        editorTitle = findViewById(R.id.servicesEditorTitle);
        content = findViewById(R.id.servicesContent);
        serviceList = findViewById(R.id.servicesList);
        typePicker = findViewById(R.id.servicesType);
        title = findViewById(R.id.servicesTitle);
        duration = findViewById(R.id.servicesDuration);
        price = findViewById(R.id.servicesPrice);
        capacity = findViewById(R.id.servicesCapacity);
        pickup = findViewById(R.id.servicesPickup);
        description = findViewById(R.id.servicesDescription);
        imageUrl = findViewById(R.id.servicesImageUrl);
        active = findViewById(R.id.servicesActive);
        save = findViewById(R.id.servicesSave);

        typePicker.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{ServiceCatalog.VEHICLE, ServiceCatalog.GUIDE}));
        typePicker.setText(selectedType, false);
        typePicker.setOnClickListener(v -> typePicker.showDropDown());
        typePicker.setOnItemClickListener((parent, view, position, id) ->
                selectedType = (String) parent.getItemAtPosition(position));
        findViewById(R.id.servicesNew).setOnClickListener(v -> clearEditor());
        save.setOnClickListener(v -> saveService());
        if (state != null) {
            editingId = state.getString("editing_service_id");
            selectedType = state.getString("service_type", ServiceCatalog.VEHICLE);
            typePicker.setText(selectedType, false);
            if (editingId != null) editorTitle.setText(R.string.admin_edit_service);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyAccess();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString("editing_service_id", editingId);
        state.putString("service_type", selectedType);
        super.onSaveInstanceState(state);
    }

    private void verifyAccess() {
        authorized = false;
        content.setVisibility(View.GONE);
        status.setText(R.string.admin_checking_access);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { finish(); return; }
        String uid = user.getUid();
        firestore.collection("admins").document(uid).get(Source.SERVER)
                .addOnSuccessListener(this, doc -> {
                    if (!isCurrentUser(uid)) return;
                    authorized = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("active"))
                            && "admin".equals(doc.getString("role"));
                    if (!authorized) { status.setText(R.string.admin_access_denied); return; }
                    content.setVisibility(View.VISIBLE);
                    status.setText(R.string.admin_access_granted);
                    loadServices();
                })
                .addOnFailureListener(this, error -> {
                    if (isCurrentUser(uid)) status.setText(R.string.admin_access_unavailable);
                });
    }

    private boolean isCurrentUser(String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && user != null && uid.equals(user.getUid());
    }

    private void loadServices() {
        if (!authorized) return;
        serviceList.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText(R.string.admin_loading_services);
        serviceList.addView(loading);
        services.get(Source.SERVER).addOnSuccessListener(this, snapshot -> {
            if (!authorized || isFinishing()) return;
            serviceList.removeAllViews();
            List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
            documents.sort(Comparator.comparing(doc ->
                    text(doc.getString("title")).toLowerCase(Locale.ROOT)));
            if (documents.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText(R.string.admin_no_services);
                serviceList.addView(empty);
            }
            for (DocumentSnapshot doc : documents) {
                MaterialButton item = new MaterialButton(this);
                item.setAllCaps(false);
                item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                item.setText(getString(R.string.admin_service_row,
                        text(doc.getString("type")), text(doc.getString("title")),
                        Boolean.TRUE.equals(doc.getBoolean("active"))
                                ? getString(R.string.admin_active) : getString(R.string.admin_hidden)));
                item.setOnClickListener(v -> editService(doc));
                serviceList.addView(item);
            }
        }).addOnFailureListener(this, error -> {
            if (authorized && !isFinishing())
                status.setText(getString(R.string.admin_services_load_failed, error.getMessage()));
        });
    }

    private void editService(DocumentSnapshot doc) {
        editingId = doc.getId();
        editorTitle.setText(R.string.admin_edit_service);
        String type = doc.getString("type");
        selectedType = ServiceCatalog.GUIDE.equals(type) ? ServiceCatalog.GUIDE : ServiceCatalog.VEHICLE;
        typePicker.setText(selectedType, false);
        title.setText(text(doc.getString("title")));
        duration.setText(text(doc.getString("duration")));
        Object amount = doc.get("price");
        price.setText(amount instanceof Number
                ? String.format(Locale.US, "%.2f", ((Number) amount).doubleValue()) : "");
        Object seats = doc.get("capacity");
        capacity.setText(seats instanceof Number ? String.valueOf(((Number) seats).intValue()) : "");
        pickup.setText(text(doc.getString("pickup")));
        description.setText(text(doc.getString("description")));
        imageUrl.setText(text(doc.getString("imageUrl")));
        active.setChecked(Boolean.TRUE.equals(doc.getBoolean("active")));
        status.setText(getString(R.string.admin_editing, title.getText()));
    }

    private void clearEditor() {
        editingId = null;
        editorTitle.setText(R.string.admin_new_service);
        selectedType = ServiceCatalog.VEHICLE;
        typePicker.setText(selectedType, false);
        title.setText("");
        duration.setText("");
        price.setText("");
        capacity.setText("");
        pickup.setText("");
        description.setText("");
        imageUrl.setText("");
        active.setChecked(false);
        status.setText(R.string.admin_service_draft_notice);
    }

    private void saveService() {
        if (!authorized || auth.getCurrentUser() == null) return;
        String name = value(title);
        String length = value(duration);
        String location = value(pickup);
        String details = value(description);
        String image = value(imageUrl);
        if (name.isEmpty() || name.length() > 100) {
            title.setError(getString(R.string.admin_title_error)); title.requestFocus(); return;
        }
        if (length.isEmpty() || length.length() > 40) {
            duration.setError(getString(R.string.admin_duration_error)); duration.requestFocus(); return;
        }
        double amount;
        try { amount = Double.parseDouble(value(price).replace(',', '.')); }
        catch (NumberFormatException error) { amount = -1; }
        if (!Double.isFinite(amount) || amount <= 0 || amount > 1000000) {
            price.setError(getString(R.string.admin_price_error)); price.requestFocus(); return;
        }
        int people;
        try { people = Integer.parseInt(value(capacity)); }
        catch (NumberFormatException error) { people = -1; }
        if (people < 1 || people > 30) {
            capacity.setError(getString(R.string.admin_capacity_error)); capacity.requestFocus(); return;
        }
        if (location.isEmpty() || location.length() > 200) {
            pickup.setError(getString(R.string.admin_pickup_error)); pickup.requestFocus(); return;
        }
        if (details.isEmpty() || details.length() > 2000) {
            description.setError(getString(R.string.admin_description_error)); description.requestFocus(); return;
        }
        if (!image.isEmpty() && (image.length() > 500 || !image.startsWith("https://")
                || !Patterns.WEB_URL.matcher(image).matches())) {
            imageUrl.setError(getString(R.string.admin_image_error)); imageUrl.requestFocus(); return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", selectedType);
        data.put("title", name);
        data.put("duration", length);
        data.put("price", amount);
        data.put("capacity", people);
        data.put("pickup", location);
        data.put("description", details);
        data.put("imageUrl", image);
        data.put("active", active.isChecked());
        DocumentReference ref = editingId == null ? services.document() : services.document(editingId);
        Task<Void> write = editingId == null ? ref.set(data) : ref.update(data);
        save.setEnabled(false);
        write.addOnSuccessListener(this, unused -> {
            save.setEnabled(true);
            editingId = ref.getId();
            editorTitle.setText(R.string.admin_edit_service);
            status.setText(R.string.admin_service_saved);
            loadServices();
        }).addOnFailureListener(this, error -> {
            save.setEnabled(true);
            status.setText(getString(R.string.admin_service_save_failed, error.getMessage()));
        });
    }

    private static String value(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static String text(String value) {
        return TextUtils.isEmpty(value) ? "" : value;
    }
}
