package lk.janith.smart_tourism.model;

import com.google.android.gms.maps.model.LatLng;

public class AttractionPlace {
    private final String id;
    private final String name;
    private final String subtitle;
    private final String address;
    private final double rating;
    private final int ratingCount;
    private final LatLng latLng;

    public AttractionPlace(String id, String name, String subtitle, String address,
                           double rating, int ratingCount, LatLng latLng) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.address = address;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.latLng = latLng;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getAddress() {
        return address;
    }

    public double getRating() {
        return rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public LatLng getLatLng() {
        return latLng;
    }
}