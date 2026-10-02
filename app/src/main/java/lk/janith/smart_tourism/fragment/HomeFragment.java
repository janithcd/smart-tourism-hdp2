package lk.janith.smart_tourism.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.activity.TourPackageDetailsActivity;
import lk.janith.smart_tourism.adapter.CategoryAdapter;
import lk.janith.smart_tourism.adapter.HomeSliderAdapter;
import lk.janith.smart_tourism.adapter.TourPackageAdapter;
import lk.janith.smart_tourism.data.FirebaseTourCatalog;
import lk.janith.smart_tourism.data.TourCatalog;
import lk.janith.smart_tourism.model.Category;
import lk.janith.smart_tourism.model.TourPackage;

public class HomeFragment extends Fragment {

    private ViewPager2 viewPagerSlider;
    private LinearLayout layoutDots;
    private RecyclerView recyclerCategories;
    private RecyclerView recyclerPackages;
    private TextView catalogSource;

    private final List<Integer> sliderImages = new ArrayList<>();
    private final List<Category> categoryList = new ArrayList<>();
    private final List<TourPackage> packageList = new ArrayList<>();
    private final List<TourPackage> availablePackages = new ArrayList<>();

    private CategoryAdapter categoryAdapter;
    private TourPackageAdapter packageAdapter;
    private boolean liveCatalog;
    private String selectedCategoryId;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        liveCatalog = false;
        selectedCategoryId = null;
        packageAdapter = null;

        viewPagerSlider = view.findViewById(R.id.viewPagerSlider);
        layoutDots = view.findViewById(R.id.layoutDots);
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        recyclerPackages = view.findViewById(R.id.recyclerPackages);
        catalogSource = view.findViewById(R.id.homeCatalogSource);

        setupSlider();
        setupCategories();
        setupPackages();
        loadLivePackages();
    }

    private void setupSlider() {
        sliderImages.clear();
        sliderImages.add(R.drawable.sigiriya);
        sliderImages.add(R.drawable.kandy);
        sliderImages.add(R.drawable.anuradhapura);
        sliderImages.add(R.drawable.fishing);
        sliderImages.add(R.drawable.wildliefe);
        sliderImages.add(R.drawable.train);
        sliderImages.add(R.drawable.yoga);

        HomeSliderAdapter sliderAdapter = new HomeSliderAdapter(sliderImages);
        viewPagerSlider.setAdapter(sliderAdapter);

        setupDots(sliderImages.size(), 0);

        viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setupDots(sliderImages.size(), position);
            }
        });
    }

    private void setupCategories() {
        recyclerCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        recyclerCategories.setNestedScrollingEnabled(false);

        categoryList.clear();
        categoryList.add(new Category("Culture", true));
        categoryList.add(new Category("Wildlife", false));
        categoryList.add(new Category("Beach", false));
        categoryList.add(new Category("Adventure", false));
        categoryList.add(new Category("Wellness", false));
        categoryList.add(new Category("Food", false));

        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            for (Category item : categoryList) {
                item.setSelected(false);
            }

            category.setSelected(true);
            categoryAdapter.notifyDataSetChanged();
            if (liveCatalog) {
                selectedCategoryId = category.getId();
                showSelectedPackages();
            }
        });

        recyclerCategories.setAdapter(categoryAdapter);
    }

    private void setupPackages() {
        recyclerPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPackages.setNestedScrollingEnabled(false);

        availablePackages.clear();
        availablePackages.addAll(TourCatalog.getPackages());
        showSelectedPackages();

        packageAdapter = new TourPackageAdapter(packageList, tourPackage -> {
            startActivity(TourPackageDetailsActivity.intentFor(requireContext(), tourPackage));
        });

        recyclerPackages.setAdapter(packageAdapter);
    }

    private void loadLivePackages() {
        FirebaseTourCatalog.loadPackages(new FirebaseTourCatalog.Listener<TourPackage>() {
            @Override public void onLoaded(List<TourPackage> packages) {
                if (!isAdded() || getView() == null || packages.isEmpty()) return;
                liveCatalog = true;
                selectedCategoryId = null;
                availablePackages.clear();
                availablePackages.addAll(packages);
                categoryList.clear();
                categoryList.add(new Category(getString(R.string.catalog_all_categories), true));
                categoryAdapter.notifyDataSetChanged();
                catalogSource.setText(R.string.catalog_live_tours_notice);
                showSelectedPackages();
                loadLiveCategories();
            }

            @Override public void onError(Exception error) {
                // Keep the bundled sample catalogue available for an offline viva.
            }
        });
    }

    private void loadLiveCategories() {
        FirebaseTourCatalog.loadCategories(new FirebaseTourCatalog.Listener<Category>() {
            @Override public void onLoaded(List<Category> categories) {
                if (!isAdded() || getView() == null || !liveCatalog) return;
                categoryList.addAll(categories);
                categoryAdapter.notifyDataSetChanged();
            }

            @Override public void onError(Exception error) {
                // "All" still displays live packages if categories are unavailable.
            }
        });
    }

    private void showSelectedPackages() {
        packageList.clear();
        for (TourPackage tour : availablePackages) {
            if (!liveCatalog || selectedCategoryId == null
                    || selectedCategoryId.equals(tour.getCategoryId())) {
                packageList.add(tour);
            }
        }
        if (packageAdapter != null) packageAdapter.notifyDataSetChanged();
    }

    private void setupDots(int count, int currentPosition) {
        layoutDots.removeAllViews();

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(18, 18);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);

            if (i == currentPosition) {
                dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_active));
            } else {
                dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.dot_inactive));
            }

            layoutDots.addView(dot);
        }
    }
}
