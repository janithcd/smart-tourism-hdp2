package lk.janith.smart_tourism.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.User;

public class SignUpActivity extends AppCompatActivity {

    private static final String DEFAULT_PROFILE_PIC_URL = "https://openclipart.org/image/800px/346569";

    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private TextInputEditText editEmail;
    private TextInputEditText editBirthday;
    private TextInputEditText editPassword;
    private TextInputEditText editRetypePassword;
    private AutoCompleteTextView autoCountry;
    private MaterialButton btnSignUp;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editEmail = findViewById(R.id.editEmail);
        editBirthday = findViewById(R.id.editBirthday);
        editPassword = findViewById(R.id.editPassword);
        editRetypePassword = findViewById(R.id.editRetypePassword);
        autoCountry = findViewById(R.id.autoCountry);
        btnSignUp = findViewById(R.id.btnSignUp);

        setupCountryDropdown();
        setupBirthdayPicker();

        findViewById(R.id.txtSignIn).setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SigninActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> signUpUser());
    }

    private void signUpUser() {
        String firstName = getText(editFirstName);
        String lastName = getText(editLastName);
        String email = getText(editEmail).toLowerCase(Locale.ROOT);
        String country = autoCountry.getText().toString().trim();
        String birthday = getText(editBirthday);
        String password = getText(editPassword);
        String retypePassword = getText(editRetypePassword);

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

        if (email.isEmpty()) {
            editEmail.setError("Enter email");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Enter a valid email");
            editEmail.requestFocus();
            return;
        }

        if (country.isEmpty()) {
            autoCountry.setError("Select country");
            autoCountry.requestFocus();
            return;
        }

        if (birthday.isEmpty()) {
            editBirthday.setError("Select birthday");
            editBirthday.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("Enter password");
            editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editPassword.setError("Password must be at least 6 characters");
            editPassword.requestFocus();
            return;
        }

        if (retypePassword.isEmpty()) {
            editRetypePassword.setError("Retype your password");
            editRetypePassword.requestFocus();
            return;
        }

        if (!password.equals(retypePassword)) {
            editRetypePassword.setError("Passwords do not match");
            editRetypePassword.requestFocus();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = firebaseAuth.getCurrentUser() != null
                                ? firebaseAuth.getCurrentUser().getUid()
                                : null;

                        if (uid == null) {
                            resetButton();
                            Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_LONG).show();
                            return;
                        }

                        User user = new User(
                                uid,
                                firstName,
                                lastName,
                                email,
                                country,
                                birthday,
                                DEFAULT_PROFILE_PIC_URL
                        );

                        firebaseFirestore.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                    firebaseAuth.signOut();

                                    Intent intent = new Intent(SignUpActivity.this, SigninActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    if (firebaseAuth.getCurrentUser() != null) {
                                        firebaseAuth.getCurrentUser().delete();
                                    }

                                    resetButton();
                                    Toast.makeText(this, "Failed to save user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        resetButton();
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resetButton() {
        btnSignUp.setEnabled(true);
        btnSignUp.setText("Create Account");
    }

    private void setupCountryDropdown() {
        String[] countries = getResources().getStringArray(R.array.country_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                countries
        );

        autoCountry.setAdapter(adapter);
    }

    private void setupBirthdayPicker() {
        editBirthday.setOnClickListener(v -> showDatePicker());
        editBirthday.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
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
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private String getText(TextInputEditText editText) {
        if (editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }
}