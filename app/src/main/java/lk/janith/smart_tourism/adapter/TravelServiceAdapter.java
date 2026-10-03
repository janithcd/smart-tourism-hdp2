package lk.janith.smart_tourism.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.TravelService;

public final class TravelServiceAdapter extends RecyclerView.Adapter<TravelServiceAdapter.Holder> {
    public interface OnServiceClickListener {
        void onServiceClick(TravelService service);
    }

    private final List<TravelService> services;
    private final OnServiceClickListener listener;

    public TravelServiceAdapter(List<TravelService> services, OnServiceClickListener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_travel_service, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TravelService service = services.get(position);
        if (service.imageUrl != null && !service.imageUrl.isEmpty()) {
            holder.image.setPadding(0, 0, 0, 0);
            holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(holder.itemView).load(service.imageUrl)
                    .placeholder(service.imageResId).error(service.imageResId).into(holder.image);
        } else {
            Glide.with(holder.itemView).clear(holder.image);
            int padding = (int) (holder.itemView.getResources().getDisplayMetrics().density * 16);
            holder.image.setPadding(padding, padding, padding, padding);
            holder.image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.image.setImageResource(service.imageResId);
        }
        holder.type.setText(service.type);
        holder.title.setText(service.title);
        holder.description.setText(service.description);
        holder.duration.setText(service.duration);
        holder.price.setText(service.price);
        holder.button.setOnClickListener(v -> listener.onServiceClick(service));
        holder.itemView.setOnClickListener(v -> listener.onServiceClick(service));
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView type, title, description, duration, price;
        final MaterialButton button;

        Holder(View view) {
            super(view);
            image = view.findViewById(R.id.serviceImage);
            type = view.findViewById(R.id.serviceType);
            title = view.findViewById(R.id.serviceTitle);
            description = view.findViewById(R.id.serviceDescription);
            duration = view.findViewById(R.id.serviceDuration);
            price = view.findViewById(R.id.servicePrice);
            button = view.findViewById(R.id.btnServiceDetails);
        }
    }
}
