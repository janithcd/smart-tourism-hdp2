package lk.janith.smart_tourism.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.TourPackageDetailsActivity;
import lk.janith.smart_tourism.adapter.TourPackageAdapter;
import lk.janith.smart_tourism.data.TourCatalog;
import lk.janith.smart_tourism.model.TourPackage;

public class ExploreFragment extends Fragment {
    private final List<TourPackage> filteredPackages = new ArrayList<>();
    private final List<TourPackage> allPackages = TourCatalog.getPackages();
    private TourPackageAdapter adapter;
    private TextView emptyState;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextInputEditText search = view.findViewById(R.id.editExploreSearch);
        RecyclerView list = view.findViewById(R.id.recyclerExplorePackages);
        emptyState = view.findViewById(R.id.exploreEmptyState);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TourPackageAdapter(filteredPackages, tourPackage -> {
            Intent intent = new Intent(requireContext(), TourPackageDetailsActivity.class);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_TITLE, tourPackage.getTitle());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DURATION, tourPackage.getDuration());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_PRICE, tourPackage.getPrice());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DESCRIPTION, tourPackage.getDescription());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_IMAGE_RES_ID, tourPackage.getImageResId());
            startActivity(intent);
        });
        list.setAdapter(adapter);
        filterPackages("");

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPackages(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void filterPackages(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        filteredPackages.clear();
        for (TourPackage tour : allPackages) {
            String searchable = tour.getTitle() + " " + tour.getDescription() + " " + tour.getDuration();
            if (searchable.toLowerCase(Locale.ROOT).contains(needle)) filteredPackages.add(tour);
        }
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(filteredPackages.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
