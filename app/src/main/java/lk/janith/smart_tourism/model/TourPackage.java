package lk.janith.smart_tourism.model;

import java.util.Locale;
import java.util.Map;

public class TourPackage {

    private final String id;
    private final String title;
    private final String categoryId;
    private final String duration;
    private final String price;
    private final Double numericPrice;
    private final String description;
    private final String route;
    private final String overview;
    private final String imageUrl;
    private final boolean featured;
    private final boolean active;
    private final int imageResId;

    /** The bundled sample tours remain usable when Firestore is unavailable. */
    public TourPackage(String title, String duration, String price, String description, int imageResId) {
        this(null, title, null, duration, price, null, description, description, null, null,
                false, true, imageResId);
    }

    private TourPackage(String id, String title, String categoryId, String duration,
                        String price, Double numericPrice, String description, String route,
                        String overview, String imageUrl, boolean featured, boolean active, int imageResId) {
        this.id = id;
        this.title = title;
        this.categoryId = categoryId;
        this.duration = duration;
        this.price = price;
        this.numericPrice = numericPrice;
        this.description = description;
        this.route = route;
        this.overview = overview;
        this.imageUrl = imageUrl;
        this.featured = featured;
        this.active = active;
        this.imageResId = imageResId;
    }

    /** Accepts the numeric price and image URL used by the original Firestore catalogue,
     * as well as the string price and drawable ID written by the bundled demo wishlist. */
    public static TourPackage fromRecord(String documentId, Map<String, Object> record, int fallbackImageResId) {
        String packageId = string(record.get("packageId"));
        if (packageId.isEmpty()) packageId = string(record.get("id"));
        if (packageId.isEmpty()) packageId = documentId;

        Object rawPrice = record.get("price");
        String displayPrice = rawPrice instanceof Number
                ? String.format(Locale.US, "$%.2f", ((Number) rawPrice).doubleValue())
                : string(rawPrice);
        Double numericPrice = rawPrice instanceof Number ? ((Number) rawPrice).doubleValue() : null;
        Object rawImageResId = record.get("imageResId");
        int imageResId = rawImageResId instanceof Number && ((Number) rawImageResId).intValue() > 0
                ? ((Number) rawImageResId).intValue() : fallbackImageResId;

        String description = string(record.get("description"));
        String route = string(record.get("route"));
        return new TourPackage(packageId, string(record.get("title")),
                string(record.get("categoryId")), string(record.get("duration")), displayPrice,
                numericPrice, description, route.isEmpty() ? description : route, string(record.get("overview")),
                string(record.get("imageUrl")), Boolean.TRUE.equals(record.get("featured")),
                !Boolean.FALSE.equals(record.get("active")), imageResId);
    }

    private static String string(Object value) {
        return value instanceof String ? (String) value : "";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getDuration() {
        return duration;
    }

    public String getPrice() {
        return price;
    }

    /** Keep the original numeric Firestore price when saving a live tour to the wishlist. */
    public Object getWishlistPrice() {
        return numericPrice != null ? numericPrice : price;
    }

    public String getDescription() {
        return description;
    }

    public String getRoute() {
        return route;
    }

    public String getOverview() {
        return overview;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isFeatured() {
        return featured;
    }

    public boolean isActive() {
        return active;
    }

    public int getImageResId() {
        return imageResId;
    }
}
