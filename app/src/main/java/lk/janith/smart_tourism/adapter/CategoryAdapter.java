package lk.janith.smart_tourism.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.janith.smart_tourism.R;
import lk.janith.smart_tourism.model.Category;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categoryList;
    private final OnCategoryClickListener listener;

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.txtCategoryName.setText(category.getTitle());

        if (category.isSelected()) {
            holder.categoryCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primary)
            );
            holder.txtCategoryName.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_onPrimary)
            );
        } else {
            holder.categoryCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_surface)
            );
            holder.txtCategoryName.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_onSurface)
            );
        }

        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CardView categoryCard;
        TextView txtCategoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryCard = itemView.findViewById(R.id.categoryCard);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
        }
    }
}