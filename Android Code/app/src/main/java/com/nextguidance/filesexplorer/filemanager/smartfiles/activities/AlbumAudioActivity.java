package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.dialogs.AudioPlayerBottomSheet;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.AudioInfo;
import java.util.ArrayList;
import java.util.List;

public class AlbumAudioActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private List<AudioInfo> audioList = new ArrayList<>();
    private String folderPath;
    private String folderName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_album_images);

        folderPath = getIntent().getStringExtra("folder_path");
        folderName = getIntent().getStringExtra("folder_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle(folderName);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AudioAdapter(this, audioList);
        recyclerView.setAdapter(adapter);

        loadAudioFromFolder();
    }

    private void loadAudioFromFolder() {
        audioList.clear();
        ContentResolver cr = getContentResolver();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.DATA + " LIKE ?";
        String[] selectionArgs = new String[] { folderPath + "/%" };

        Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, MediaStore.Audio.Media.DATE_MODIFIED + " DESC");

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
                
                // Extra safety: make sure parent matches
                if (new java.io.File(audio.path).getParent().equals(folderPath)) {
                    audioList.add(audio);
                }
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

    private static class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {
        private Context context;
        private List<AudioInfo> items;

        public AudioAdapter(Context context, List<AudioInfo> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_audio_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AudioInfo audio = items.get(position);
            holder.tvName.setText(audio.displayName);
            holder.tvDate.setText(DateFormat.format("dd/MM/yyyy", audio.lastModified).toString());
            holder.tvSize.setText(Formatter.formatFileSize(context, audio.size));

            holder.itemView.setOnClickListener(v -> {
                AudioPlayerBottomSheet playerSheet = new AudioPlayerBottomSheet(audio);
                playerSheet.show(((AppCompatActivity)context).getSupportFragmentManager(), "AudioPlayer");
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvSize;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_filename);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvSize = itemView.findViewById(R.id.tv_size);
            }
        }
    }
}
