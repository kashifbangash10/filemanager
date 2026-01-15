package com.nextguidance.filesexplorer.filemanager.smartfiles.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.JunkItem;
import java.util.List;

public class JunkCleanAdapter extends RecyclerView.Adapter<JunkCleanAdapter.ViewHolder> {

    private List<JunkItem> items;
    private OnCheckedChangeListener onCheckedChangeListener;

    public interface OnCheckedChangeListener {
        void onCheckChanged();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    public JunkCleanAdapter(List<JunkItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_junk_clean, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JunkItem item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvSize.setText(item.getSize());
        
        // Remove listener before setting check state to avoid recursion or multiple triggers
        holder.cbChecked.setOnCheckedChangeListener(null);
        holder.cbChecked.setChecked(item.isChecked());
        
        if (item.isScanning()) {
            holder.pbScanning.setVisibility(View.VISIBLE);
            holder.cbChecked.setVisibility(View.GONE);
            holder.tvSize.setVisibility(View.GONE);
            holder.tvScanPath.setVisibility(View.VISIBLE);
            holder.ivChevron.setVisibility(View.GONE);
            holder.tvScanPath.setText("Scanning: " + item.getScanPath());
        } else {
            holder.pbScanning.setVisibility(View.GONE);
            holder.tvScanPath.setVisibility(View.GONE);
            holder.ivChevron.setVisibility(View.VISIBLE);
            
            // Handle "No junk" state
            boolean hasJunk = !item.getSize().equals("0 B") && !item.getSize().equalsIgnoreCase("No junk");
            if (hasJunk) {
                holder.cbChecked.setVisibility(View.VISIBLE);
                holder.tvSize.setVisibility(View.VISIBLE);
                holder.tvSize.setTextColor(0xFF212121); // Darker
            } else {
                holder.cbChecked.setVisibility(View.GONE);
                holder.tvSize.setVisibility(View.VISIBLE);
                holder.tvSize.setText("No junk");
                holder.tvSize.setTextColor(0xFF9E9E9E); // Gray
                holder.ivChevron.setVisibility(View.GONE); // No sub-items for No Junk
            }
        }

        // Handle Expansion - Reference app shows chevron up when expanded
        holder.ivChevron.setRotation(item.isExpanded() ? 180 : 0);
        holder.subItemsLayout.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        holder.subItemsLayout.setBackgroundColor(0x05000000); // Very light gray tint

        holder.headerLayout.setOnClickListener(v -> {
            if (!item.isScanning() && item.getSubItems() != null && !item.getSubItems().isEmpty()) {
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(position);
            }
        });

        // Populate Sub-Items (Files) with Checkbox Logic
        holder.subItemsLayout.removeAllViews();
        if (item.isExpanded() && item.getSubItems() != null) {
            for (JunkItem.SubJunkItem subItem : item.getSubItems()) {
                View subView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_sub_junk, holder.subItemsLayout, false);
                TextView tvPath = subView.findViewById(R.id.tvSubPath);
                TextView tvSize = subView.findViewById(R.id.tvSubSize);
                ImageView ivIcon = subView.findViewById(R.id.ivSubIcon);
                CheckBox cbSub = subView.findViewById(R.id.cbSub);

                tvPath.setText(subItem.getLabel());
                tvSize.setText(subItem.getSize());
                ivIcon.setImageResource(subItem.getIconResId());
                if (subItem.getTintColor() != 0) {
                    ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(subItem.getTintColor()));
                }
                cbSub.setChecked(subItem.isChecked());

                // Toggle alpha based on checked state for professional look
                subView.setAlpha(subItem.isChecked() ? 1.0f : 0.4f);

                cbSub.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    subItem.setChecked(isChecked);
                    subView.setAlpha(isChecked ? 1.0f : 0.5f);
                    if (onCheckedChangeListener != null) {
                        onCheckedChangeListener.onCheckChanged();
                    }
                });

                holder.subItemsLayout.addView(subView);
            }
        }

        holder.cbChecked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSize, tvScanPath;
        CheckBox cbChecked;
        ProgressBar pbScanning;
        ImageView ivChevron;
        LinearLayout subItemsLayout;
        View headerLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvJunkName);
            tvSize = itemView.findViewById(R.id.tvJunkSize);
            tvScanPath = itemView.findViewById(R.id.tvScanPath);
            cbChecked = itemView.findViewById(R.id.cbJunk);
            pbScanning = itemView.findViewById(R.id.pbJunk);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            subItemsLayout = itemView.findViewById(R.id.subItemsLayout);
            headerLayout = itemView.findViewById(R.id.headerLayout);
        }
    }
}
