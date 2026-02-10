package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.ImagesActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import java.util.ArrayList;
import java.util.List;

public class ImagesTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<DocumentInfo> imageList = new ArrayList<>();
    private boolean isGridView = true;
    private String iconSize = "large";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        
        SharedPreferences prefs = getContext().getSharedPreferences(ImagesActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isGridView = prefs.getBoolean(ImagesActivity.KEY_IS_GRID, true);
        iconSize = prefs.getString(ImagesActivity.KEY_ICON_SIZE, "large");

        if (isGridView) {
            int spanCount = iconSize.equals("small") ? 4 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        
        adapter = new ImageAdapter(getContext(), imageList, isGridView);
        recyclerView.setAdapter(adapter);

        loadImages();
        return view;
    }

    private void loadImages() {
        imageList.clear();
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        };

        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                DocumentInfo doc = new DocumentInfo();
                doc.documentId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                doc.displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                doc.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                doc.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                doc.lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)) * 1000;
                doc.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
                doc.derivedUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, doc.documentId);
                imageList.add(doc);
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private Context context;
        private List<DocumentInfo> images;
        private boolean isGrid;

        public ImageAdapter(Context context, List<DocumentInfo> images, boolean isGrid) {
            this.context = context;
            this.images = images;
            this.isGrid = isGrid;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reuse video item layouts for consistency
            int layout = isGrid ? R.layout.item_video_grid : R.layout.item_video_list;
            View view = LayoutInflater.from(context).inflate(layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentInfo img = images.get(position);
            holder.tvName.setText(img.displayName);
            String size = Formatter.formatFileSize(context, img.size);
            String date = DateFormat.format("dd MMM yyyy", img.lastModified).toString();
            holder.tvInfo.setText(date + " • " + size);
            
            if (holder.tvDuration != null) holder.tvDuration.setVisibility(View.GONE);
            if (holder.ivPlay != null) holder.ivPlay.setVisibility(View.GONE);

            Glide.with(context)
                .load(img.derivedUri)
                .centerCrop()
                .placeholder(R.drawable.ic_doc_image)
                .into(holder.ivThumb);
                
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.ImageViewerActivity.class);
                intent.setData(img.derivedUri);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb, ivPlay;
            TextView tvName, tvInfo, tvDuration;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumb = itemView.findViewById(R.id.iv_thumbnail);
                ivPlay = itemView.findViewById(R.id.iv_play);
                tvName = itemView.findViewById(R.id.tv_filename);
                tvInfo = itemView.findViewById(R.id.tv_info);
                tvDuration = itemView.findViewById(R.id.tv_duration); // Might be null in list view
            }
        }
    }
}
