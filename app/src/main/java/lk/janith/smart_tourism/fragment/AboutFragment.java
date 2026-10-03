package lk.janith.smart_tourism.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.janith.smart_tourism.R;

public class AboutFragment extends Fragment {
    private static final String SOURCE_URL = "https://github.com/janithcd/smart-tourism-hdp2";
    private static final String GOOGLE_TERMS = "https://maps.google.com/help/terms_maps/";
    private static final String GOOGLE_PRIVACY = "https://policies.google.com/privacy";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String version = getString(R.string.about_unknown_version);
        try {
            String installed = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            if (installed != null) version = installed;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        ((TextView) view.findViewById(R.id.aboutVersion))
                .setText(getString(R.string.about_version, version));
        view.findViewById(R.id.aboutSource).setOnClickListener(v -> openUrl(SOURCE_URL));
        view.findViewById(R.id.aboutGoogleTerms).setOnClickListener(v -> openUrl(GOOGLE_TERMS));
        view.findViewById(R.id.aboutGooglePrivacy).setOnClickListener(v -> openUrl(GOOGLE_PRIVACY));
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.about_no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
