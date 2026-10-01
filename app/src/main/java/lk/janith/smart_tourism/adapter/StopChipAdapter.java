package lk.janith.smart_tourism.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.RouteStop;

public class StopChipAdapter extends RecyclerView.Adapter<StopChipAdapter.ViewHolder> {

    public interface OnStopClickListener {
        void onStopClick(int position);
    }

    private final List<RouteStop> stops;
    private final OnStopClickListener listener;

    public StopChipAdapter(List<RouteStop> stops, OnStopClickListener listener) {
        this.stops = stops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteStop stop = stops.get(position);
        holder.txtStopName.setText(stop.getName());

        if (stop.isSelected()) {
            holder.stopCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary));
            holder.stopCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary));
            holder.txtStopName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_onPrimary));
        } else {
            holder.stopCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_surface));
            holder.stopCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_outline));
            holder.txtStopName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_onSurface));
        }

        holder.itemView.setOnClickListener(v -> listener.onStopClick(position));
    }

    @Override
    public int getItemCount() {
        return stops.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView stopCard;
        TextView txtStopName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stopCard = itemView.findViewById(R.id.stopCard);
            txtStopName = itemView.findViewById(R.id.txtStopName);
        }
    }
}