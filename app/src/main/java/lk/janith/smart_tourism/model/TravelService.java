package lk.janith.smart_tourism.model;

/** A sample vehicle or guide offering. There is no live provider or availability feed. */
public final class TravelService {
    public final String type;
    public final String title;
    public final String duration;
    public final String price;
    public final String description;
    public final int imageResId;

    public TravelService(String type, String title, String duration, String price,
                         String description, int imageResId) {
        this.type = type;
        this.title = title;
        this.duration = duration;
        this.price = price;
        this.description = description;
        this.imageResId = imageResId;
    }
}
