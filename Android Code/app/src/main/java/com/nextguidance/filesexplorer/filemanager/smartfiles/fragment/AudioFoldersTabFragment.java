package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioFoldersTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private List<AudioFolderInfo> folderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FolderAdapter(getContext(), folderList);
        recyclerView.setAdapter(adapter);

        loadFolders();
        return view;
    }

    private void loadFolders() {
        folderList.clear();
        Map<String, AudioFolderInfo> folderMap = new HashMap<>();
        ContentResolver cr = getContext().getContentResolver();

        String[] projection = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
        };

        Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                File file = new File(path);
                String parentPath = file.getParent();
                if (parentPath == null) continue;
                String folderName = file.getParentFile().getName();

                AudioFolderInfo folder = folderMap.get(parentPath);
                if (folder == null) {
                    folder = new AudioFolderInfo();
                    folder.name = folderName;
                    folder.path = parentPath;
                    folder.count = 1;
                    folderMap.put(parentPath, folder);
                } else {
                    folder.count++;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        folderList.addAll(folderMap.values());
        adapter.notifyDataSetChanged();
    }

    private static class AudioFolderInfo {
        String name;
        String path;
        int count;
    }

    private static class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
        private Context context;
        private List<AudioFolderInfo> folders;

        public FolderAdapter(Context context, List<AudioFolderInfo> folders) {
            this.context = context;
            this.folders = folders;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_audio_folder, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AudioFolderInfo folder = folders.get(position);
            holder.tvName.setText(folder.name);
            holder.tvCount.setText(folder.count + (folder.count == 1 ? " song" : " songs"));

            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(context, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.AlbumAudioActivity.class);
                intent.putExtra("folder_path", folder.path);
                intent.putExtra("folder_name", folder.name);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return folders.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCount;
            CheckBox cbSelect;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_folder_name);
                tvCount = itemView.findViewById(R.id.tv_song_count);
                cbSelect = itemView.findViewById(R.id.cb_select);
            }
        }
    }
}
