package lk.janith.smart_tourism.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.StorageMetadata;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.MainActivity;
import lk.janith.smart_tourism.activity.SigninActivity;
import lk.janith.smart_tourism.data.ProfilePhoto;
import lk.janith.smart_tourism.model.User;

public class ProfileFragment extends Fragment {

    private ImageView profileAvatar;
    private TextView txtFullName;
    private TextView txtEmailTop;
    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editEmail;
    private TextInputEditText editCountry;
    private TextInputEditText editBirthday;
    private MaterialButton btnChangeProfilePic;
    private MaterialButton btnUpdateProfile;
    private MaterialButton btnDeleteProfile;
    private ProgressBar profileProgressBar;
    private View profileContentLayout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private String currentUid = "";
    private boolean uploadingPhoto;
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) uploadProfilePicture(uri);
            });

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileAvatar = view.findViewById(R.id.profileAvatar);
        txtFullName = view.findViewById(R.id.txtFullName);
        txtEmailTop = view.findViewById(R.id.txtEmailTop);
        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editEmail = view.findViewById(R.id.editEmail);
        editCountry = view.findViewById(R.id.editCountry);
        editBirthday = view.findViewById(R.id.editBirthday);
        btnChangeProfilePic = view.findViewById(R.id.btnChangeProfilePic);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        btnDeleteProfile = view.findViewById(R.id.btnDeleteProfile);
        profileProgressBar = view.findViewById(R.id.profileProgressBar);
        profileContentLayout = view.findViewById(R.id.profileContentLayout);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        editBirthday.setOnClickListener(v -> showDatePicker());

        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnChangeProfilePic.setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        btnDeleteProfile.setOnClickListener(v -> showDeleteProfileDialog());

    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No signed-in user", Toast.LENGTH_LONG).show();
            return;
        }

        currentUid = firebaseAuth.getCurrentUser().getUid();

        showLoading(true);
        setTemporaryValues();

        DocumentReference userRef = firebaseFirestore.collection("users").document(currentUid);

        userRef.get(Source.CACHE)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User cachedUser = documentSnapshot.toObject(User.class);
                        if (cachedUser != null) {
                            bindUser(cachedUser);
                        }
                    }
                });

        userRef.get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            bindUser(user);
                        }
                    } else {
                        Toast.makeText(requireContext(), "User document not found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Firestore read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void bindUser(User user) {
        String firstName = user.getFname() != null ? user.getFname() : "";
        String lastName = user.getLname() != null ? user.getLname() : "";
        String fullName = (firstName + " " + lastName).trim();

        txtFullName.setText(TextUtils.isEmpty(fullName) ? "User" : fullName);
        txtEmailTop.setText(user.getEmail() != null ? user.getEmail() : "No email");

        editFirstName.setText(firstName);
        editLastName.setText(lastName);
        editEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        editCountry.setText(user.getCountry() != null ? user.getCountry() : "");
        editBirthday.setText(user.getBirthday() != null ? user.getBirthday() : "");

        if (!uploadingPhoto) ProfilePhoto.load(profileAvatar, user.getProfilePic(), currentUid);
    }

    private void updateProfile() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String firstName = getText(editFirstName);
        String lastName = getText(editLastName);
        String country = getText(editCountry);
        String birthday = getText(editBirthday);

        if (firstName.isEmpty()) {
            editFirstName.setError("Enter first name");
            editFirstName.requestFocus();
            return;
        }

        if (firstName.length() < 2) {
            editFirstName.setError("First name is too short");
            editFirstName.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            editLastName.setError("Enter last name");
            editLastName.requestFocus();
            return;
        }

        if (lastName.length() < 2) {
            editLastName.setError("Last name is too short");
            editLastName.requestFocus();
            return;
        }

        if (country.isEmpty()) {
            editCountry.setError("Enter country");
            editCountry.requestFocus();
            return;
        }

        if (birthday.isEmpty()) {
            editBirthday.setError("Select birthday");
            editBirthday.requestFocus();
            return;
        }

        btnUpdateProfile.setEnabled(false);
        btnUpdateProfile.setText("Updating...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("fname", firstName);
        updates.put("lname", lastName);
        updates.put("country", country);
        updates.put("birthday", birthday);

        firebaseFirestore.collection("users")
                .document(currentUid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    btnUpdateProfile.setEnabled(true);
                    btnUpdateProfile.setText("Update Profile");

                    String fullName = (firstName + " " + lastName).trim();
                    txtFullName.setText(fullName);

                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    refreshMainHeader();
                })
                .addOnFailureListener(e -> {
                    btnUpdateProfile.setEnabled(true);
                    btnUpdateProfile.setText("Update Profile");
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadProfilePicture(Uri uri) {
        if (uploadingPhoto || firebaseAuth.getCurrentUser() == null) return;
        String uid = firebaseAuth.getCurrentUser().getUid();
        String type = requireContext().getContentResolver().getType(uri);
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)
                && !"image/webp".equals(type)) {
            Toast.makeText(requireContext(), R.string.profile_photo_type_error, Toast.LENGTH_LONG).show();
            return;
        }

        try (Cursor cursor = requireContext().getContentResolver().query(
                uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (column >= 0 && !cursor.isNull(column)
                        && cursor.getLong(column) > ProfilePhoto.MAX_BYTES) {
                    Toast.makeText(requireContext(), R.string.profile_photo_size_error, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (RuntimeException error) {
            Toast.makeText(requireContext(), R.string.profile_photo_read_error, Toast.LENGTH_LONG).show();
            return;
        }

        uploadingPhoto = true;
        btnChangeProfilePic.setEnabled(false);
        btnChangeProfilePic.setText(R.string.profile_photo_uploading);
        profileProgressBar.setVisibility(View.VISIBLE);

        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(type).build();
        ProfilePhoto.referenceFor(uid).putFile(uri, metadata)
                .addOnSuccessListener(task -> firebaseFirestore.collection("users").document(uid)
                        .update("profilePic", ProfilePhoto.pathFor(uid))
                        .addOnSuccessListener(unused -> {
                            finishPhotoUpload();
                            if (isAdded() && getView() != null && uid.equals(currentUid)) {
                                ProfilePhoto.load(profileAvatar, ProfilePhoto.pathFor(uid), uid);
                                refreshMainHeader();
                                Toast.makeText(requireContext(), R.string.profile_photo_saved, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(error -> {
                            finishPhotoUpload();
                            if (isAdded()) Toast.makeText(requireContext(),
                                    R.string.profile_photo_profile_error, Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(error -> {
                    finishPhotoUpload();
                    if (isAdded()) Toast.makeText(requireContext(),
                            R.string.profile_photo_upload_error, Toast.LENGTH_LONG).show();
                });
    }

    private void finishPhotoUpload() {
        uploadingPhoto = false;
        if (isAdded() && getView() != null) {
            btnChangeProfilePic.setEnabled(true);
            btnChangeProfilePic.setText(R.string.change_profile_pic);
            profileProgressBar.setVisibility(View.GONE);
        }
    }

    private void showDeleteProfileDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfile() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        btnDeleteProfile.setEnabled(false);
        String uid = firebaseAuth.getCurrentUser().getUid();

        firebaseAuth.getCurrentUser().delete()
                .addOnSuccessListener(unused ->
                        firebaseFirestore.collection("users")
                                .document(uid)
                                .delete()
                                .addOnSuccessListener(unused1 -> {
                                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(requireContext(), SigninActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    requireActivity().finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnDeleteProfile.setEnabled(true);
                                    Toast.makeText(requireContext(), "Firestore delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                })
                )
                .addOnFailureListener(e -> {
                    btnDeleteProfile.setEnabled(true);
                    Toast.makeText(requireContext(), "Delete failed: " + e.getMessage() + ". Please sign in again and retry.", Toast.LENGTH_LONG).show();
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );
                    editBirthday.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void setTemporaryValues() {
        txtFullName.setText("Loading...");
        txtEmailTop.setText("Loading...");
    }

    private void showLoading(boolean show) {
        profileProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        profileContentLayout.setAlpha(show ? 0.65f : 1f);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void refreshMainHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshUserHeader();
        }
    }
}
