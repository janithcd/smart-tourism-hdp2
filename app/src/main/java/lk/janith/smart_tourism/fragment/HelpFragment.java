package lk.janith.smart_tourism.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.janith.smart_tourism.R;

public class HelpFragment extends Fragment {
    private static final String GMAIL_PACKAGE = "com.google.android.gm";
    private static final String SOURCE_URL = "https://sltda.gov.lk/en/developing-planning";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.helpTourismHotline).setOnClickListener(v -> dial(getString(R.string.help_hotline_number)));
        view.findViewById(R.id.helpTourismOffice).setOnClickListener(v -> dial(getString(R.string.help_office_number)));
        view.findViewById(R.id.helpTourismEmail).setOnClickListener(v -> email(getString(R.string.help_email_address)));
        view.findViewById(R.id.helpContactSource).setOnClickListener(v -> openSource());
    }

    private void dial(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.help_no_dialer, Toast.LENGTH_SHORT).show();
        }
    }

    private void email(String address) {
        Uri uri = Uri.fromParts("mailto", address, null);
        Intent compose = new Intent(Intent.ACTION_SENDTO, uri).setPackage(GMAIL_PACKAGE);
        try {
            startActivity(compose);
        } catch (ActivityNotFoundException noGmail) {
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, uri));
            } catch (ActivityNotFoundException noEmailApp) {
                Toast.makeText(requireContext(), R.string.help_no_email_app, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openSource() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.about_no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
