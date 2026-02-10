package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.ImagesActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

public class AlbumsTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlbumAdapter adapter;
    private List<AlbumInfo> albumList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false); // Reuse fragment layout
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        adapter = new AlbumAdapter(getContext(), albumList);
        recyclerView.setAdapter(adapter);

        loadAlbums();
        return view;
    }

    private void loadAlbums() {
        albumList.clear();
        Map<String, AlbumInfo> albumMap = new HashMap<>();
        ContentResolver cr = getContext().getContentResolver();
        
        String[] projection = {
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID
        };

        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            int bucketIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

            do {
                String bucketName = cursor.getString(bucketIdx);
                String data = cursor.getString(dataIdx);
                long id = cursor.getLong(idIdx);
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));

                AlbumInfo album = albumMap.get(bucketName);
                if (album == null) {
                    album = new AlbumInfo();
                    album.name = bucketName;
                    album.previewUri = uri;
                    album.count = 1;
                    albumMap.put(bucketName, album);
                } else {
                    album.count++;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        albumList.addAll(albumMap.values());
        adapter.notifyDataSetChanged();
    }

    private static class AlbumInfo {
        String name;
        int count;
        Uri previewUri;
    }

    private static class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
        private Context context;
        private List<AlbumInfo> albums;

        public AlbumAdapter(Context context, List<AlbumInfo> albums) {
            this.context = context;
            this.albums = albums;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_album_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AlbumInfo album = albums.get(position);
            holder.tvName.setText(album.name);
            holder.tvCount.setText(album.count + (album.count == 1 ? " photo" : " photos"));
            Glide.with(context).load(album.previewUri).centerCrop().into(holder.ivPreview);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.AlbumImagesActivity.class);
                intent.putExtra("album_name", album.name);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return albums.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPreview;
            TextView tvName, tvCount;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPreview = itemView.findViewById(R.id.iv_album_preview);
                tvName = itemView.findViewById(R.id.tv_album_name);
                tvCount = itemView.findViewById(R.id.tv_photo_count);
            }
        }
    }
}
