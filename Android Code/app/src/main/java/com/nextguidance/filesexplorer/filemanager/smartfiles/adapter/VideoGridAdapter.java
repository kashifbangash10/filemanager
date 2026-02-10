package com.nextguidance.filesexplorer.filemanager.smartfiles.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.VideoPlayerActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import java.util.ArrayList;
import java.util.List;

public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.ViewHolder> implements Filterable {

    private Context context;
    private List<DocumentInfo> videoList;
    private List<DocumentInfo> videoListFull;
    private boolean isGridView = true;

    public VideoGridAdapter(Context context, List<DocumentInfo> videoList) {
        this.context = context;
        this.videoList = videoList;
        this.videoListFull = new ArrayList<>(videoList);
    }

    public void setGridView(boolean gridView) {
        isGridView = gridView;
    }

    public void updateList(List<DocumentInfo> newList) {
        this.videoList = newList;
        this.videoListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isGridView ? R.layout.item_video_grid : R.layout.item_video_list;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentInfo video = videoList.get(position);
        
        holder.tvFilename.setText(video.displayName);
        String size = Formatter.formatFileSize(context, video.size);
        String date = DateFormat.format("dd MMM yyyy", video.lastModified).toString();
        holder.tvInfo.setText(date + " • " + size);
        holder.tvDuration.setText(video.summary);

        Glide.with(context)
                .load(video.derivedUri)
                .centerCrop()
                .placeholder(R.drawable.ic_doc_video)
                .into(holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.setData(video.derivedUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    @Override
    public Filter getFilter() {
        return videoFilter;
    }

    private Filter videoFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<DocumentInfo> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(videoListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (DocumentInfo item : videoListFull) {
                    if (item.displayName.toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            videoList.clear();
            videoList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, ivMore;
        TextView tvDuration, tvFilename, tvInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            ivMore = itemView.findViewById(R.id.iv_more);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvFilename = itemView.findViewById(R.id.tv_filename);
            tvInfo = itemView.findViewById(R.id.tv_info);
        }
    }
}
