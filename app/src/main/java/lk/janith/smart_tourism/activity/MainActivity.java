package lk.janith.smart_tourism.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.fragment.BookingsFragment;
import lk.janith.smart_tourism.fragment.ExploreFragment;
import lk.janith.smart_tourism.fragment.HomeFragment;
import lk.janith.smart_tourism.fragment.MapFragment;
import lk.janith.smart_tourism.fragment.MyActivitiesFragment;
import lk.janith.smart_tourism.fragment.ProfileFragment;
import lk.janith.smart_tourism.fragment.WishlistFragment;
import lk.janith.smart_tourism.model.User;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DEFAULT_PROFILE_PIC_URL = "https://openclipart.org/image/800px/346569";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private ImageView headerAvatar;
    private TextView headerName;
    private TextView headerEmail;
    private TextView headerCountry;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    public void refreshUserHeader() {
        loadDrawerHeaderUser();
    }

    public void showBookingsScreen() {
        if (bottomNavigationView.getSelectedItemId() == R.id.bottom_bookings) {
            loadFragment(new BookingsFragment(), R.string.bottom_nav_booking_title, false);
        } else {
            bottomNavigationView.setSelectedItemId(R.id.bottom_bookings);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, SigninActivity.class));
            finish();
            return;
        }
        firebaseFirestore = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open_drawer,
                R.string.close_drawer
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        setupHeaderViews();
        loadDrawerHeaderUser();

        if (getIntent() != null && getIntent().getBooleanExtra("open_my_activities", false)) {
            openMyActivitiesScreen();
        } else if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), R.string.bottom_nav_home_title, false);
            bottomNavigationView.setSelectedItemId(R.id.bottom_home);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.bottom_home) {
                loadFragment(new HomeFragment(), R.string.bottom_nav_home_title, false);
                return true;

            } else if (id == R.id.bottom_map) {
                loadFragment(new MapFragment(), R.string.bottom_nav_map_title, false);
                return true;

            } else if (id == R.id.bottom_bookings) {
                loadFragment(new BookingsFragment(), R.string.bottom_nav_booking_title, false);
                return true;

            } else if (id == R.id.bottom_explore) {
                loadFragment(new ExploreFragment(), R.string.bottom_nav_explore_title, false);
                return true;

            } else if (id == R.id.bottom_profile) {
                loadFragment(new ProfileFragment(), R.string.bottom_nav_profile_title, false);
                return true;
            }

            return false;
        });

        if (getIntent() != null && getIntent().getBooleanExtra("open_bookings", false)) {
            showBookingsScreen();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getBooleanExtra("open_my_activities", false)) {
            openMyActivitiesScreen();
        } else if (intent != null && intent.getBooleanExtra("open_bookings", false)) {
            showBookingsScreen();
        }
    }

    private void openMyActivitiesScreen() {
        loadFragment(new MyActivitiesFragment(), R.string.side_nav_activities_title, true);
        navigationView.setCheckedItem(R.id.nav_my_activities);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrawerHeaderUser();
        refreshAdminMenu();
    }

    private void refreshAdminMenu() {
        MenuItem manageTours = navigationView.getMenu().findItem(R.id.nav_manage_tours);
        MenuItem manageCategories = navigationView.getMenu().findItem(R.id.nav_manage_categories);
        MenuItem adminBookings = navigationView.getMenu().findItem(R.id.nav_admin_bookings);
        manageTours.setVisible(false);
        manageCategories.setVisible(false);
        adminBookings.setVisible(false);
        if (firebaseAuth.getCurrentUser() == null) return;

        String uid = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("admins").document(uid).get(Source.SERVER)
                .addOnSuccessListener(this, document -> {
                    if (firebaseAuth.getCurrentUser() != null
                            && uid.equals(firebaseAuth.getCurrentUser().getUid())) {
                        boolean isAdmin = document.exists()
                                && Boolean.TRUE.equals(document.getBoolean("active"))
                                && "admin".equals(document.getString("role"));
                        manageTours.setVisible(isAdmin);
                        manageCategories.setVisible(isAdmin);
                        adminBookings.setVisible(isAdmin);
                    }
                });
    }

    private void setupHeaderViews() {
        View headerView = navigationView.getHeaderView(0);
        headerAvatar = headerView.findViewById(R.id.headerAvatar);
        headerName = headerView.findViewById(R.id.headerName);
        headerEmail = headerView.findViewById(R.id.headerEmail);
        headerCountry = headerView.findViewById(R.id.headerCountry);
    }

    private void loadDrawerHeaderUser() {
        if (firebaseAuth.getCurrentUser() == null) {
            headerName.setText(getString(R.string.guest_user));
            headerEmail.setText(getString(R.string.no_email));
            headerCountry.setText(getString(R.string.no_country));

            Glide.with(this)
                    .load(DEFAULT_PROFILE_PIC_URL)
                    .placeholder(R.drawable.account_circle_24px)
                    .error(R.drawable.account_circle_24px)
                    .circleCrop()
                    .into(headerAvatar);
            return;
        }

        String uid = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            String firstName = user.getFname() != null ? user.getFname() : "";
                            String lastName = user.getLname() != null ? user.getLname() : "";
                            String fullName = (firstName + " " + lastName).trim();

                            headerName.setText(fullName.isEmpty() ? getString(R.string.user_default_name) : fullName);
                            headerEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.no_email));
                            headerCountry.setText(user.getCountry() != null ? user.getCountry() : getString(R.string.no_country));

                            String profileUrl = user.getProfilePic();
                            if (profileUrl == null || profileUrl.trim().isEmpty()) {
                                profileUrl = DEFAULT_PROFILE_PIC_URL;
                            }

                            Glide.with(MainActivity.this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.account_circle_24px)
                                    .error(R.drawable.account_circle_24px)
                                    .circleCrop()
                                    .into(headerAvatar);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.user_document_not_found), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                MainActivity.this,
                                getString(R.string.firestore_read_failed, e.getMessage()),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadFragment(Fragment fragment, int titleResId, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFrame, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
        toolbar.setTitle(getString(titleResId));
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(MainActivity.this, SigninActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_sinhala),
                getString(R.string.language_hindi),
                getString(R.string.language_french),
                getString(R.string.language_german),
                getString(R.string.language_russian)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_language)
                .setItems(languages, (dialog, which) -> {
                    if (which == 0) {
                        setAppLanguage("en");
                    } else if (which == 1) {
                        setAppLanguage("si");
                    } else if (which == 2) {
                        setAppLanguage("hi");
                    } else if (which == 3) {
                        setAppLanguage("fr");
                    } else if (which == 4) {
                        setAppLanguage("de");
                    } else if (which == 5) {
                        setAppLanguage("ru");
                    }
                })
                .show();
    }

    private void setAppLanguage(String languageTag) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag)
        );
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_my_activities) {
            openMyActivitiesScreen();

        } else if (id == R.id.nav_manage_tours) {
            startActivity(new Intent(this, AdminToursActivity.class));

        } else if (id == R.id.nav_manage_categories) {
            startActivity(new Intent(this, AdminCategoriesActivity.class));

        } else if (id == R.id.nav_admin_bookings) {
            startActivity(new Intent(this, AdminBookingsActivity.class));

        } else if (id == R.id.nav_airport_shuttle) {
            Toast.makeText(this, getString(R.string.airport_shuttle_clicked), Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_near_places) {
            Toast.makeText(this, getString(R.string.nearby_places_clicked), Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, getString(R.string.settings_clicked), Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_about) {
            Toast.makeText(this, getString(R.string.about_clicked), Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_languages) {
            showLanguageDialog();

        } else if (id == R.id.nav_wishlist) {
            loadFragment(new WishlistFragment(), R.string.side_nav_wishlist_title, true);

        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
