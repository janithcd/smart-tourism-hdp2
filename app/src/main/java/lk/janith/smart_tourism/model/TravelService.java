package lk.janith.smart_tourism.model;

/** A vehicle or guide listing. Bookings still use the demo flow. */
public final class TravelService {
    public final String id;
    public final String type;
    public final String title;
    public final String duration;
    public final String price;
    public final String description;
    public final int imageResId;
    public final String imageUrl;
    public final int capacity;
    public final String pickup;
    public final boolean sample;

    public TravelService(String type, String title, String duration, String price,
                         String description, int imageResId) {
        this.type = type;
        this.id = "";
        this.title = title;
        this.duration = duration;
        this.price = price;
        this.description = description;
        this.imageResId = imageResId;
        this.imageUrl = "";
        this.capacity = type.equals("Vehicle") && title.toLowerCase(java.util.Locale.ROOT).contains("van") ? 7
                : type.equals("Vehicle") ? 3 : 8;
        this.pickup = "";
        this.sample = true;
    }

    public TravelService(String id, String type, String title, String duration, String price,
                         String description, String imageUrl, int capacity, String pickup,
                         int imageResId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.duration = duration;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.capacity = capacity;
        this.pickup = pickup;
        this.imageResId = imageResId;
        this.sample = false;
    }
}
