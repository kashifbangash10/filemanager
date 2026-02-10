package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.VideoGridAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideosTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private VideoGridAdapter adapter;
    private List<DocumentInfo> videoList = new ArrayList<>();
    private boolean isGridView = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        
        SharedPreferences prefs = getContext().getSharedPreferences("VideoPrefs", android.content.Context.MODE_PRIVATE);
        isGridView = prefs.getBoolean("isGridView", true);

        if (isGridView) {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 3));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        
        adapter = new VideoGridAdapter(getContext(), videoList);
        adapter.setGridView(isGridView);
        recyclerView.setAdapter(adapter);

        loadVideos();
        
        return view;
    }

    public void onSearch(String query) {
        if (adapter != null) {
            adapter.getFilter().filter(query);
        }
    }

    private void loadVideos() {
        videoList.clear();
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE
        };

        Cursor cursor = contentResolver.query(videoUri, projection, null, null, MediaStore.Video.Media.DATE_MODIFIED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                DocumentInfo doc = new DocumentInfo();
                doc.documentId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                doc.displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                doc.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                doc.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                doc.lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)) * 1000;
                doc.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
                doc.summary = formatDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                doc.derivedUri = Uri.withAppendedPath(videoUri, doc.documentId);
                videoList.add(doc);
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.updateList(videoList);
    }

    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
