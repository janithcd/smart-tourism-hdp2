package lk.janith.smart_tourism.activity;

import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
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

/** Manages the Firestore categories used by the tour editor and Home filters. */
public class AdminCategoriesActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private CollectionReference categories;
    private TextView status;
    private TextView editorTitle;
    private LinearLayout content;
    private LinearLayout categoryList;
    private TextInputEditText title;
    private TextInputEditText description;
    private TextInputEditText imageUrl;
    private SwitchMaterial active;
    private MaterialButton save;
    private String editingId;
    private boolean authorized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories);
        categories = firestore.collection("categories");
        com.google.android.material.appbar.MaterialToolbar toolbar =
                findViewById(R.id.adminCategoriesToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        status = findViewById(R.id.adminCategoriesStatus);
        content = findViewById(R.id.adminCategoriesContent);
        categoryList = findViewById(R.id.adminCategoryList);
        editorTitle = findViewById(R.id.adminCategoryEditorTitle);
        title = findViewById(R.id.adminCategoryTitle);
        description = findViewById(R.id.adminCategoryDescription);
        imageUrl = findViewById(R.id.adminCategoryImage);
        active = findViewById(R.id.adminCategoryActive);
        save = findViewById(R.id.adminCategorySave);
        findViewById(R.id.adminCategoryNew).setOnClickListener(v -> clearEditor());
        save.setOnClickListener(v -> saveCategory());
        if (savedInstanceState != null) {
            editingId = savedInstanceState.getString("editing_category_id");
            if (editingId != null) editorTitle.setText(R.string.admin_edit_category);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("editing_category_id", editingId);
        super.onSaveInstanceState(outState);
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
                    status.setText(R.string.admin_access_granted);
                    loadCategories();
                })
                .addOnFailureListener(this, error -> {
                    if (isCurrentUser(uid)) status.setText(R.string.admin_access_unavailable);
                });
    }

    private boolean isCurrentUser(String uid) {
        FirebaseUser user = auth.getCurrentUser();
        return !isFinishing() && user != null && uid.equals(user.getUid());
    }

    private void loadCategories() {
        if (!authorized) return;
        categoryList.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText(R.string.admin_loading_categories);
        categoryList.addView(loading);
        categories.get(Source.SERVER)
                .addOnSuccessListener(this, snapshot -> {
                    if (!authorized || isFinishing()) return;
                    categoryList.removeAllViews();
                    List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
                    documents.sort(Comparator.comparing(doc ->
                            value(doc.getString("title")).toLowerCase(Locale.ROOT)));
                    if (documents.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText(R.string.admin_no_categories);
                        categoryList.addView(empty);
                    }
                    for (DocumentSnapshot doc : documents) {
                        MaterialButton row = new MaterialButton(this);
                        row.setText(getString(R.string.admin_category_row,
                                value(doc.getString("title")).isEmpty()
                                        ? doc.getId() : value(doc.getString("title")),
                                Boolean.TRUE.equals(doc.getBoolean("active"))
                                        ? getString(R.string.admin_active) : getString(R.string.admin_hidden)));
                        row.setAllCaps(false);
                        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        row.setOnClickListener(v -> editCategory(doc));
                        categoryList.addView(row);
                    }
                })
                .addOnFailureListener(this, error -> {
                    if (authorized && !isFinishing())
                        status.setText(getString(R.string.admin_categories_load_failed, error.getMessage()));
                });
    }

    private void editCategory(DocumentSnapshot doc) {
        editingId = doc.getId();
        editorTitle.setText(R.string.admin_edit_category);
        title.setText(value(doc.getString("title")));
        description.setText(value(doc.getString("description")));
        imageUrl.setText(value(doc.getString("imageUrl")));
        active.setChecked(Boolean.TRUE.equals(doc.getBoolean("active")));
        status.setText(getString(R.string.admin_editing, title.getText()));
    }

    private void clearEditor() {
        editingId = null;
        editorTitle.setText(R.string.admin_new_category);
        title.setText("");
        description.setText("");
        imageUrl.setText("");
        active.setChecked(false);
        status.setText(R.string.admin_category_draft_notice);
    }

    private void saveCategory() {
        if (!authorized || auth.getCurrentUser() == null) return;
        String name = value(title.getText() == null ? null : title.getText().toString());
        String details = value(description.getText() == null ? null : description.getText().toString());
        String image = value(imageUrl.getText() == null ? null : imageUrl.getText().toString());
        if (name.isEmpty() || name.length() > 100) {
            title.setError(getString(R.string.admin_title_error));
            title.requestFocus();
            return;
        }
        if (details.length() > 2000) {
            description.setError(getString(R.string.admin_category_description_error));
            return;
        }
        if (!image.isEmpty() && (image.length() > 500 || !image.startsWith("https://")
                || !Patterns.WEB_URL.matcher(image).matches())) {
            imageUrl.setError(getString(R.string.admin_image_error));
            imageUrl.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", name);
        data.put("description", details);
        data.put("imageUrl", image);
        data.put("active", active.isChecked());
        DocumentReference ref = editingId == null
                ? categories.document() : categories.document(editingId);
        save.setEnabled(false);
        (editingId == null ? ref.set(data) : ref.update(data))
                .addOnSuccessListener(this, unused -> {
                    save.setEnabled(true);
                    editingId = ref.getId();
                    editorTitle.setText(R.string.admin_edit_category);
                    status.setText(R.string.admin_category_saved);
                    loadCategories();
                })
                .addOnFailureListener(this, error -> {
                    save.setEnabled(true);
                    status.setText(getString(R.string.admin_category_save_failed, error.getMessage()));
                });
    }

    private static String value(String text) {
        return text == null ? "" : text.trim();
    }
}
