package lk.janith.smart_tourism.model;

import com.google.android.gms.maps.model.LatLng;

public class RouteStop {
    private final String name;
    private final LatLng latLng;
    private boolean selected;

    public RouteStop(String name, LatLng latLng, boolean selected) {
        this.name = name;
        this.latLng = latLng;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}