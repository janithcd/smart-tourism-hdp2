package lk.janith.smart_tourism.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.AttractionPlace;

public class AttractionAdapter extends RecyclerView.Adapter<AttractionAdapter.ViewHolder> {

    public interface OnAttractionClickListener {
        void onAttractionClick(AttractionPlace place);
    }

    private final List<AttractionPlace> attractionList;
    private final OnAttractionClickListener listener;

    public AttractionAdapter(List<AttractionPlace> attractionList, OnAttractionClickListener listener) {
        this.attractionList = attractionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attraction_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttractionPlace place = attractionList.get(position);

        holder.txtAttractionName.setText(place.getName());

        if (place.getRating() > 0) {
            holder.txtAttractionMeta.setText(String.format(Locale.getDefault(),
                    "%.1f ★ (%d)", place.getRating(), place.getRatingCount()));
        } else {
            holder.txtAttractionMeta.setText("No rating yet");
        }

        String desc = place.getSubtitle();
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            desc = desc + " • " + place.getAddress();
        }
        holder.txtAttractionDesc.setText(desc);

        holder.itemView.setOnClickListener(v -> listener.onAttractionClick(place));
    }

    @Override
    public int getItemCount() {
        return attractionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAttractionName, txtAttractionMeta, txtAttractionDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAttractionName = itemView.findViewById(R.id.txtAttractionName);
            txtAttractionMeta = itemView.findViewById(R.id.txtAttractionMeta);
            txtAttractionDesc = itemView.findViewById(R.id.txtAttractionDesc);
        }
    }
}