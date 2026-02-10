package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.ImagesTabFragment;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.bumptech.glide.Glide;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;

public class AlbumImagesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageGridAdapter adapter;
    private List<DocumentInfo> imageList = new ArrayList<>();
    private String albumName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_album_images);

        albumName = getIntent().getStringExtra("album_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle(albumName);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        
        adapter = new ImageGridAdapter(this, imageList);
        recyclerView.setAdapter(adapter);

        loadImagesFromAlbum();
    }

    private void loadImagesFromAlbum() {
        imageList.clear();
        ContentResolver cr = getContentResolver();
        String[] projection = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        };

        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[] { albumName };

        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, MediaStore.Images.Media.DATE_MODIFIED + " DESC");

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.ViewHolder> {
        private Context context;
        private List<DocumentInfo> images;

        public ImageGridAdapter(Context context, List<DocumentInfo> images) {
            this.context = context;
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_video_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentInfo img = images.get(position);
            holder.tvName.setText(img.displayName);
            String size = Formatter.formatFileSize(context, img.size);
            String date = DateFormat.format("dd MMM yyyy", img.lastModified).toString();
            holder.tvInfo.setText(date + " • " + size);
            holder.tvDuration.setVisibility(View.GONE);
            if (holder.ivPlay != null) holder.ivPlay.setVisibility(View.GONE);

            Glide.with(context)
                .load(img.derivedUri)
                .centerCrop()
                .placeholder(R.drawable.ic_doc_image)
                .into(holder.ivThumb);
                
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ImageViewerActivity.class);
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
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }
    }
}
