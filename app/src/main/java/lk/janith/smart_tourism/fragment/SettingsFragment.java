package lk.janith.smart_tourism.fragment;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import lk.janith.smart_tourism.AppAppearance;
import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.MainActivity;

public class SettingsFragment extends Fragment {

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.settingsThemeRow).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.settingsLanguageRow).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showLanguageDialog());
        view.findViewById(R.id.settingsNotificationsRow).setOnClickListener(v -> openNotifications());
        view.findViewById(R.id.settingsLocationRow).setOnClickListener(v -> openAppDetails());
        view.findViewById(R.id.settingsOpenProfile).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openProfileScreen());
        view.findViewById(R.id.settingsSignOut).setOnClickListener(v ->
                ((MainActivity) requireActivity()).confirmSignOut());
        refreshValues(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) refreshValues(requireView());
    }

    private void refreshValues(View view) {
        int mode = AppAppearance.getThemeMode(requireContext());
        int themeLabel = mode == AppCompatDelegate.MODE_NIGHT_YES ? R.string.settings_dark
                : mode == AppCompatDelegate.MODE_NIGHT_NO ? R.string.settings_light
                : R.string.settings_system_default;
        ((TextView) view.findViewById(R.id.settingsThemeValue)).setText(themeLabel);

        Locale selected = AppCompatDelegate.getApplicationLocales().get(0);
        int languageLabel = R.string.settings_system_default;
        if (selected != null) {
            switch (selected.getLanguage()) {
                case "en": languageLabel = R.string.language_english; break;
                case "si": languageLabel = R.string.language_sinhala; break;
                case "hi": languageLabel = R.string.language_hindi; break;
                case "fr": languageLabel = R.string.language_french; break;
                case "de": languageLabel = R.string.language_german; break;
                case "ru": languageLabel = R.string.language_russian; break;
                default: break;
            }
        }
        ((TextView) view.findViewById(R.id.settingsLanguageValue)).setText(languageLabel);

        boolean notifications = NotificationManagerCompat.from(requireContext())
                .areNotificationsEnabled();
        ((TextView) view.findViewById(R.id.settingsNotificationsValue)).setText(
                notifications ? R.string.settings_allowed : R.string.settings_not_allowed);

        int locationLabel = R.string.settings_not_allowed;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationLabel = R.string.settings_precise;
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationLabel = R.string.settings_approximate;
        }
        ((TextView) view.findViewById(R.id.settingsLocationValue)).setText(locationLabel);
    }

    private void showThemeDialog() {
        int current = AppAppearance.getThemeMode(requireContext());
        int selectedIndex = current == AppCompatDelegate.MODE_NIGHT_NO ? 1
                : current == AppCompatDelegate.MODE_NIGHT_YES ? 2 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(R.array.settings_theme_options, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    AppAppearance.setThemeMode(requireContext(), THEME_MODES[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            openAppDetails();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            openAppDetails();
        }
    }

    private void openAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.settings_cannot_open, Toast.LENGTH_SHORT).show();
        }
    }
}
