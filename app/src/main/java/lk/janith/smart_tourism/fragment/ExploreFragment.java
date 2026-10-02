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
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.TourPackageDetailsActivity;
import lk.janith.smart_tourism.adapter.TourPackageAdapter;
import lk.janith.smart_tourism.adapter.TravelServiceAdapter;
import lk.janith.smart_tourism.data.ServiceCatalog;
import lk.janith.smart_tourism.data.TourCatalog;
import lk.janith.smart_tourism.model.TourPackage;
import lk.janith.smart_tourism.model.TravelService;

public class ExploreFragment extends Fragment {
    private final List<TourPackage> filteredPackages = new ArrayList<>();
    private final List<TourPackage> allPackages = TourCatalog.getPackages();
    private final List<TravelService> filteredServices = new ArrayList<>();
    private final List<TravelService> vehicles = ServiceCatalog.getServices(ServiceCatalog.VEHICLE);
    private final List<TravelService> guides = ServiceCatalog.getServices(ServiceCatalog.GUIDE);
    private TourPackageAdapter tourAdapter;
    private TravelServiceAdapter serviceAdapter;
    private RecyclerView list;
    private TextInputEditText search;
    private TextView emptyState;
    private int selectedTab;

    public ExploreFragment() {
        super(R.layout.fragment_explore);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        search = view.findViewById(R.id.editExploreSearch);
        list = view.findViewById(R.id.recyclerExplorePackages);
        emptyState = view.findViewById(R.id.exploreEmptyState);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        tourAdapter = new TourPackageAdapter(filteredPackages, tourPackage -> {
            Intent intent = new Intent(requireContext(), TourPackageDetailsActivity.class);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_TITLE, tourPackage.getTitle());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DURATION, tourPackage.getDuration());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_PRICE, tourPackage.getPrice());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DESCRIPTION, tourPackage.getDescription());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_IMAGE_RES_ID, tourPackage.getImageResId());
            startActivity(intent);
        });
        serviceAdapter = new TravelServiceAdapter(filteredServices, service -> {
            Intent intent = new Intent(requireContext(), TourPackageDetailsActivity.class);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_SERVICE_TYPE, service.type);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_TITLE, service.title);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DURATION, service.duration);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_PRICE, service.price);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DESCRIPTION, service.description);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_IMAGE_RES_ID, service.imageResId);
            startActivity(intent);
        });

        TabLayout tabs = view.findViewById(R.id.exploreTabs);
        tabs.addTab(tabs.newTab().setText(R.string.explore_tours_tab));
        tabs.addTab(tabs.newTab().setText(R.string.explore_vehicles_tab));
        tabs.addTab(tabs.newTab().setText(R.string.explore_guides_tab));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                list.setAdapter(selectedTab == 0 ? tourAdapter : serviceAdapter);
                filterItems();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt("selected_tab", 0);
        }
        tabs.selectTab(tabs.getTabAt(selectedTab));
        list.setAdapter(selectedTab == 0 ? tourAdapter : serviceAdapter);
        filterItems();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("selected_tab", selectedTab);
        super.onSaveInstanceState(outState);
    }

    private void filterItems() {
        String query = search.getText() == null ? "" : search.getText().toString();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (selectedTab == 0) {
            filteredPackages.clear();
            for (TourPackage tour : allPackages) {
                String searchable = tour.getTitle() + " " + tour.getDescription() + " " + tour.getDuration();
                if (searchable.toLowerCase(Locale.ROOT).contains(needle)) filteredPackages.add(tour);
            }
            tourAdapter.notifyDataSetChanged();
            emptyState.setVisibility(filteredPackages.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            filteredServices.clear();
            List<TravelService> source = selectedTab == 1 ? vehicles : guides;
            for (TravelService service : source) {
                String searchable = service.title + " " + service.description + " " + service.duration;
                if (searchable.toLowerCase(Locale.ROOT).contains(needle)) filteredServices.add(service);
            }
            serviceAdapter.notifyDataSetChanged();
            emptyState.setVisibility(filteredServices.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
