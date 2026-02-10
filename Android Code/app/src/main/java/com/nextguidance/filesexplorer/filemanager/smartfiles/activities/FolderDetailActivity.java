package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import android.content.Intent;
import com.nextguidance.filesexplorer.filemanager.smartfiles.AboutActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.SplashActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.VideoGridAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoGridAdapter adapter;
    private List<DocumentInfo> videoList = new ArrayList<>();
    private String folderPath;
    private boolean isGridView = true;
    private View toolbarContent;
    private SearchView searchView;
    private static final String PREFS_NAME = "VideoPrefs";
    private static final String KEY_IS_GRID = "isGridView";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_folder_detail);

        folderPath = getIntent().getStringExtra("folder_path");
        String folderName = getIntent().getStringExtra("folder_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        TextView title = findViewById(R.id.toolbar_title);
        if (title != null) {
            title.setText(folderName);
        }

        recyclerView = findViewById(R.id.recycler_view);
        
        loadPrefs();

        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        adapter = new VideoGridAdapter(this, videoList);
        adapter.setGridView(isGridView);
        recyclerView.setAdapter(adapter);

        toolbarContent = findViewById(R.id.toolbar_content);
        searchView = findViewById(R.id.search_view);
        findViewById(R.id.menu_search).setOnClickListener(v -> showSearch());
        findViewById(R.id.menu_grid_list).setOnClickListener(v -> toggleGridList());
        findViewById(R.id.menu_more).setOnClickListener(v -> showMoreMenu(v));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finishAffinity();
                startActivity(new Intent(this, SplashActivity.class)); // Go home
                return true;
            } else if (id == R.id.nav_files) {
                Intent intent = new Intent(this, DocumentsActivity.class);
                intent.putExtra("SHOW_FILES", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_clean) {
                Intent intent = new Intent(this, DocumentsActivity.class);
                intent.putExtra("SHOW_CLEAN", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            return true;
        });

        setupSearch();
        updateLayoutIcon();
        loadVideosInFolder();
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGridView = prefs.getBoolean(KEY_IS_GRID, true);
    }

    private void showSearch() {
        toolbarContent.setVisibility(View.GONE);
        searchView.setVisibility(View.VISIBLE);
        searchView.setIconified(false);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            toolbarContent.setVisibility(View.VISIBLE);
            searchView.setVisibility(View.GONE);
            return false;
        });
    }

    private void toggleGridList() {
        isGridView = !isGridView;
        updateLayoutIcon();
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_GRID, isGridView);
        editor.apply();

        // Re-setup layout manager
        if (isGridView) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        adapter.setGridView(isGridView);
        recyclerView.setAdapter(adapter); // Re-set adapter to refresh items
    }

    private void updateLayoutIcon() {
        ImageView gridListIcon = findViewById(R.id.menu_grid_list);
        if (gridListIcon != null) {
            gridListIcon.setImageResource(isGridView ? R.drawable.ic_menu_view_list : R.drawable.ic_menu_view_grid);
        }
    }

    private void showMoreMenu(View v) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.CustomPopupMenuDark);
        PopupMenu popup = new PopupMenu(wrapper, v);
        popup.getMenu().add("Sort by");
        popup.getMenu().add("Settings");
        popup.getMenu().add("About");
        popup.getMenu().add("Exit");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "Sort by":
                    showSortMenu(v);
                    break;
                case "Settings":
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
                case "About":
                    startActivity(new Intent(this, AboutActivity.class));
                    break;
                case "Exit":
                    finishAffinity();
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void showSortMenu(View v) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.CustomPopupMenuDark);
        PopupMenu sortPopup = new PopupMenu(wrapper, v);
        sortPopup.getMenu().add("Name (A-Z)");
        sortPopup.getMenu().add("Date (Newest)");
        sortPopup.getMenu().add("Size (Largest)");

        sortPopup.setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "Sorting by: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            // Implement sorting logic
            return true;
        });
        sortPopup.show();
    }

    private void loadVideosInFolder() {
        videoList.clear();
        if (folderPath == null) return;

        ContentResolver contentResolver = getContentResolver();
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

        String selection = MediaStore.Video.Media.DATA + " LIKE ?";
        String[] selectionArgs = new String[] { folderPath + "/%" };

        Cursor cursor = contentResolver.query(videoUri, projection, selection, selectionArgs, MediaStore.Video.Media.DATE_MODIFIED + " DESC");

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
