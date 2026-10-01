package lk.janith.smart_tourism.data;

import java.util.ArrayList;
import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.TourPackage;

/** The small, offline catalogue shared by Home and Explore. */
public final class TourCatalog {
    private TourCatalog() { }

    public static List<TourPackage> getPackages() {
        List<TourPackage> packages = new ArrayList<>();
        packages.add(new TourPackage("5 Days Highlights of Sri Lanka", "5 Days", "$390",
                "Colombo • Sigiriya • Kandy • Ella • Galle", R.drawable.sigiriya));
        packages.add(new TourPackage("7 Days Classic Sri Lanka", "7 Days", "$520",
                "Negombo • Sigiriya • Kandy • Ella • Yala • Galle", R.drawable.kandy));
        packages.add(new TourPackage("10 Days Heritage & Nature", "10 Days", "$780",
                "Negombo • Anuradhapura • Sigiriya • Kandy • Ella • Yala • Mirissa",
                R.drawable.anuradhapura));
        packages.add(new TourPackage("14 Days Complete Island Tour", "14 Days", "$1100",
                "Negombo • Anuradhapura • Sigiriya • Polonnaruwa • Kandy • Ella • Arugam Bay • Yala • Galle",
                R.drawable.train));
        return packages;
    }
}
