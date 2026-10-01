package lk.janith.smart_tourism.model;

public class TourPackage {

    private String title;
    private String duration;
    private String price;
    private String description;
    private int imageResId;

    public TourPackage(String title, String duration, String price, String description, int imageResId) {
        this.title = title;
        this.duration = duration;
        this.price = price;
        this.description = description;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }
}