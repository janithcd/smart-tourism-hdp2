package lk.janith.smart_tourism.adapter;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import lk.janith.smart_tourism.R;

public class NearbyPlaceAdapter extends RecyclerView.Adapter<NearbyPlaceAdapter.PlaceHolder> {

    private final List<Place> places = new ArrayList<>();
    private final Consumer<Place> onSelect;
    private final Consumer<Place> onDirections;
    private Location origin;

    public NearbyPlaceAdapter(Consumer<Place> onSelect, Consumer<Place> onDirections) {
        this.onSelect = onSelect;
        this.onDirections = onDirections;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void submit(List<Place> results, Location location) {
        places.clear();
        places.addAll(results);
        origin = location;
        notifyDataSetChanged();
    }

    public void clear() {
        places.clear();
        origin = null;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_place, parent, false);
        return new PlaceHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceHolder holder, int position) {
        Place place = places.get(position);
        holder.name.setText(place.getDisplayName() == null
                ? holder.itemView.getContext().getString(R.string.nearby_unnamed_place)
                : place.getDisplayName());
        String address = place.getFormattedAddress();
        holder.address.setText(address == null ? "" : address);
        holder.address.setVisibility(address == null || address.isEmpty() ? View.GONE : View.VISIBLE);
        if (origin != null && place.getLocation() != null) {
            float[] distance = new float[1];
            Location.distanceBetween(origin.getLatitude(), origin.getLongitude(),
                    place.getLocation().latitude, place.getLocation().longitude, distance);
            holder.distance.setText(String.format(Locale.getDefault(), "%.1f km", distance[0] / 1000));
            holder.distance.setVisibility(View.VISIBLE);
        } else {
            holder.distance.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> onSelect.accept(place));
        holder.directions.setEnabled(place.getLocation() != null);
        holder.directions.setOnClickListener(v -> onDirections.accept(place));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView address;
        final TextView distance;
        final MaterialButton directions;

        PlaceHolder(@NonNull View view) {
            super(view);
            name = view.findViewById(R.id.nearbyPlaceName);
            address = view.findViewById(R.id.nearbyPlaceAddress);
            distance = view.findViewById(R.id.nearbyPlaceDistance);
            directions = view.findViewById(R.id.nearbyDirections);
        }
    }
}
