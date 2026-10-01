package lk.janith.smart_tourism.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
import lk.janith.smart_tourism.data.TourCatalog;
import lk.janith.smart_tourism.model.Category;
import lk.janith.smart_tourism.model.TourPackage;

public class HomeFragment extends Fragment {

    private ViewPager2 viewPagerSlider;
    private LinearLayout layoutDots;
    private RecyclerView recyclerCategories;
    private RecyclerView recyclerPackages;

    private final List<Integer> sliderImages = new ArrayList<>();
    private final List<Category> categoryList = new ArrayList<>();
    private final List<TourPackage> packageList = new ArrayList<>();

    private CategoryAdapter categoryAdapter;
    private TourPackageAdapter packageAdapter;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPagerSlider = view.findViewById(R.id.viewPagerSlider);
        layoutDots = view.findViewById(R.id.layoutDots);
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        recyclerPackages = view.findViewById(R.id.recyclerPackages);

        setupSlider();
        setupCategories();
        setupPackages();
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
        });

        recyclerCategories.setAdapter(categoryAdapter);
    }

    private void setupPackages() {
        recyclerPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPackages.setNestedScrollingEnabled(false);

        packageList.clear();
        packageList.addAll(TourCatalog.getPackages());

        packageAdapter = new TourPackageAdapter(packageList, tourPackage -> {
            Intent intent = new Intent(requireContext(), TourPackageDetailsActivity.class);
            intent.putExtra(TourPackageDetailsActivity.EXTRA_TITLE, tourPackage.getTitle());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DURATION, tourPackage.getDuration());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_PRICE, tourPackage.getPrice());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_DESCRIPTION, tourPackage.getDescription());
            intent.putExtra(TourPackageDetailsActivity.EXTRA_IMAGE_RES_ID, tourPackage.getImageResId());
            startActivity(intent);
        });

        recyclerPackages.setAdapter(packageAdapter);
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
