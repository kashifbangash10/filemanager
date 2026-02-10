package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.util.DisplayMetrics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nextguidance.filesexplorer.filemanager.smartfiles.AboutActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.SplashActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DocumentTabFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;

import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.PermissionUtil;

public class FileDocumentsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "DocumentPrefs";
    public static final String KEY_IS_GRID = "isGridView";
    public static final String KEY_ICON_SIZE = "iconSize";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private View toolbarContent;
    private SearchView searchView;
    private boolean isGridView = true;
    private String iconSize = "large";
    private String[] tabs = {"All", "WORD", "EXCEL", "PPT", "PDF", "TXT", "Others"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_file_documents);

        if (!PermissionUtil.isStorageAccess(this)) {
            // If we don't have permission, we should ideally go back to DocumentsActivity
            // which handles permission requests robustly.
            Toast.makeText(this, "Please grant storage permissions first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadPrefs();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        viewPager.setAdapter(new DocumentPagerAdapter(this));
        
        // Disable swiping if search is active (optional)
        // viewPager.setUserInputEnabled(true);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabs[position])).attach();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        setupBottomNav();
        setupMenuListeners();
        
        toolbarContent = findViewById(R.id.toolbar_content);
        searchView = findViewById(R.id.search_view);
        setupSearch();

        updateLayoutIcon();
        loadBannerAd();
    }

    private void setupBottomNav() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finishAffinity();
                startActivity(new Intent(FileDocumentsActivity.this, SplashActivity.class));
                return true;
            } else if (id == R.id.nav_files) {
                return true;
            } else if (id == R.id.nav_clean) {
                Intent intent = new Intent(FileDocumentsActivity.this, DocumentsActivity.class);
                intent.putExtra("SHOW_CLEAN", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupMenuListeners() {
        findViewById(R.id.menu_search).setOnClickListener(v -> showSearch());
        findViewById(R.id.menu_view_toggle).setOnClickListener(v -> showViewToggleDialog());
        findViewById(R.id.menu_sort).setOnClickListener(v -> showSortPopup(v));
        findViewById(R.id.menu_more).setOnClickListener(v -> showMoreMenu(v));
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
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                if (currentFragment instanceof DocumentTabFragment) {
                    ((DocumentTabFragment) currentFragment).onSearch(newText);
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

    private void showSortPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Name (A-Z)");
        popup.getMenu().add("Name (Z-A)");
        popup.getMenu().add("Date (New-Old)");
        popup.getMenu().add("Date (Old-New)");
        popup.getMenu().add("Size (Big-Small)");
        popup.getMenu().add("Size (Small-Big)");

        popup.setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "Sorting by: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            // Implement sorting logic if needed
            return true;
        });
        popup.show();
    }

    private void showMoreMenu(View v) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.CustomPopupMenuDark);
        PopupMenu popup = new PopupMenu(wrapper, v);
        popup.getMenu().add("About");
        popup.getMenu().add("Settings");
        popup.getMenu().add("Exit");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("About")) startActivity(new Intent(this, AboutActivity.class));
            else if (title.equals("Settings")) startActivity(new Intent(this, SettingsActivity.class));
            else if (title.equals("Exit")) finishAffinity();
            return true;
        });
        popup.show();
    }

    private void showViewToggleDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_view_toggle, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        RadioGroup rgViewMode = dialogView.findViewById(R.id.rg_view_mode);
        RadioGroup rgIconSize = dialogView.findViewById(R.id.rg_icon_size);

        rgViewMode.check(isGridView ? R.id.rb_grid : R.id.rb_list);
        rgIconSize.check(iconSize.equals("small") ? R.id.rb_small : R.id.rb_large);

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            boolean grid = rgViewMode.getCheckedRadioButtonId() == R.id.rb_grid;
            String size = rgIconSize.getCheckedRadioButtonId() == R.id.rb_small ? "small" : "large";

            savePrefs(grid, size);
            dialog.dismiss();
            updateLayoutIcon();
            
            // Refresh ViewPager to apply new layout
            viewPager.setAdapter(new DocumentPagerAdapter(this));
        });

        dialog.show();
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGridView = prefs.getBoolean(KEY_IS_GRID, true);
        iconSize = prefs.getString(KEY_ICON_SIZE, "large");
    }

    private void savePrefs(boolean grid, String size) {
        isGridView = grid;
        iconSize = size;
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_GRID, grid);
        editor.putString(KEY_ICON_SIZE, size);
        editor.apply();
    }

    private void updateLayoutIcon() {
        ImageView gridListIcon = findViewById(R.id.menu_view_toggle);
        if (gridListIcon != null) {
            gridListIcon.setImageResource(isGridView ? R.drawable.ic_menu_view_list : R.drawable.ic_menu_view_grid);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBannerAd() {
        try {
            AdView mAdView = new AdView(this);
            mAdView.setAdUnitId(getString(R.string.admob_banner));
            FrameLayout container = findViewById(R.id.banner_container);
            if (container != null) {
                container.removeAllViews();
                container.addView(mAdView);
                mAdView.setAdSize(getAdSize());
                mAdView.loadAd(new AdRequest.Builder().build());
            }
        } catch (Exception e) {
            Log.e("FileDocumentsActivity", "Error loading banner", e);
        }
    }

    private AdSize getAdSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private class DocumentPagerAdapter extends FragmentStateAdapter {
        public DocumentPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return DocumentTabFragment.newInstance(tabs[position]);
        }

        @Override
        public int getItemCount() {
            return tabs.length;
        }
    }
}
