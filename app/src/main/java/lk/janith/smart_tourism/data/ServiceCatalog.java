package lk.janith.smart_tourism.data;

import java.util.ArrayList;
import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.TravelService;

/** Illustrative offline services for demonstrating the booking flow, not real providers. */
public final class ServiceCatalog {
    public static final String VEHICLE = "Vehicle";
    public static final String GUIDE = "Guide";

    private ServiceCatalog() { }

    public static List<TravelService> getServices(String type) {
        List<TravelService> services = new ArrayList<>();
        if (VEHICLE.equals(type)) {
            services.add(new TravelService(VEHICLE, "Airport transfer car", "One way", "$35 demo rate",
                    "Colombo airport • Hotel transfer • Up to 3 travelers", R.drawable.airport_shuttle_24px));
            services.add(new TravelService(VEHICLE, "Private day van", "1 Day", "$85 demo rate",
                    "Flexible itinerary • Up to 7 travelers • Hotel pickup", R.drawable.airport_shuttle_24px));
            services.add(new TravelService(VEHICLE, "City travel car", "1 Day", "$55 demo rate",
                    "Colombo city • Up to 3 travelers • Hotel pickup", R.drawable.airport_shuttle_24px));
        } else if (GUIDE.equals(type)) {
            services.add(new TravelService(GUIDE, "Colombo city guide", "Half day", "$30 demo rate",
                    "City landmarks • Walking route • Meeting point pickup", R.drawable.explore_24px));
            services.add(new TravelService(GUIDE, "Heritage site guide", "1 Day", "$45 demo rate",
                    "Sigiriya area • Cultural highlights • Meeting point pickup", R.drawable.explore_24px));
            services.add(new TravelService(GUIDE, "Kandy culture guide", "Half day", "$30 demo rate",
                    "Kandy city • Cultural sights • Meeting point pickup", R.drawable.explore_24px));
        }
        return services;
    }
}
