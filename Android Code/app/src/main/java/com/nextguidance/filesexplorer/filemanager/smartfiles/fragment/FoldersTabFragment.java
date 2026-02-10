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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.FolderListAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoldersTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderListAdapter adapter;
    private List<FolderModel> folderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        adapter = new FolderListAdapter(getContext(), folderList);
        recyclerView.setAdapter(adapter);

        loadFolders();
        return view;
    }

    private void loadFolders() {
        folderList.clear();
        Map<String, FolderModel> folderMap = new HashMap<>();
        ContentResolver cr = getContext().getContentResolver();

        String[] projection = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };

        Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                File file = new File(path);
                String parentPath = file.getParent();
                if (parentPath == null) continue;
                String folderName = file.getParentFile().getName();

                FolderModel folder = folderMap.get(parentPath);
                if (folder == null) {
                    folder = new FolderModel();
                    folder.name = folderName;
                    folder.path = parentPath;
                    folderMap.put(parentPath, folder);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        folderList.addAll(folderMap.values());
        adapter.notifyDataSetChanged();
    }

    public static class FolderModel {
        public String name;
        public String path;
    }
}
