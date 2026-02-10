package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.AudioActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.AudioInfo;
import java.util.ArrayList;
import java.util.List;

public class SongsTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private List<AudioInfo> audioList = new ArrayList<>();
    private List<AudioInfo> filteredList = new ArrayList<>();
    private boolean isGridView = false;
    private String iconSize = "large";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);

        SharedPreferences prefs = getContext().getSharedPreferences(AudioActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isGridView = prefs.getBoolean(AudioActivity.KEY_IS_GRID, false);
        iconSize = prefs.getString(AudioActivity.KEY_ICON_SIZE, "large");

        if (isGridView) {
            int spanCount = iconSize.equals("small") ? 3 : 2;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        adapter = new AudioAdapter(getContext(), filteredList, isGridView);
        recyclerView.setAdapter(adapter);

        loadSongs();
        return view;
    }

    public void onSearch(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(audioList);
        } else {
            for (AudioInfo audio : audioList) {
                if (audio.displayName.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(audio);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadSongs() {
        audioList.clear();
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DURATION
        };

        Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Audio.Media.DATE_MODIFIED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                AudioInfo audio = new AudioInfo();
                audio.documentId = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                audio.displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                audio.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                audio.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                audio.lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)) * 1000;
                audio.duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                audio.derivedUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audio.documentId);
                audioList.add(audio);
            } while (cursor.moveToNext());
            cursor.close();
        }
        filteredList.clear();
        filteredList.addAll(audioList);
        adapter.notifyDataSetChanged();
    }

    private static class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {
        private Context context;
        private List<AudioInfo> items;
        private boolean isGrid;

        public AudioAdapter(Context context, List<AudioInfo> items, boolean isGrid) {
            this.context = context;
            this.items = items;
            this.isGrid = isGrid;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = isGrid ? R.layout.item_audio_grid : R.layout.item_audio_list;
            View view = LayoutInflater.from(context).inflate(layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AudioInfo audio = items.get(position);
            holder.tvName.setText(audio.displayName);
            
            String sizeStr = Formatter.formatFileSize(context, audio.size);
            String dateStr = isGrid ? DateFormat.format("dd MMM", audio.lastModified).toString() 
                                   : DateFormat.format("dd/MM/yyyy", audio.lastModified).toString();
            
            if (isGrid) {
                holder.tvDate.setText(dateStr);
                holder.tvSize.setText(sizeStr);
            } else {
                holder.tvDate.setText(dateStr);
                holder.tvSize.setText(sizeStr);
            }

            holder.itemView.setOnClickListener(v -> {
                com.nextguidance.filesexplorer.filemanager.smartfiles.dialogs.AudioPlayerBottomSheet playerSheet = 
                    new com.nextguidance.filesexplorer.filemanager.smartfiles.dialogs.AudioPlayerBottomSheet(audio);
                playerSheet.show(((androidx.appcompat.app.AppCompatActivity)context).getSupportFragmentManager(), "AudioPlayer");
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvSize;
            ImageView ivMore;
            CheckBox cbSelect;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_filename);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvSize = itemView.findViewById(R.id.tv_size);
                ivMore = itemView.findViewById(R.id.iv_more);
                cbSelect = itemView.findViewById(R.id.cb_select);
            }
        }
    }
}
