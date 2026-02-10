package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nextguidance.filesexplorer.filemanager.smartfiles.AboutActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.SplashActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.AudioFoldersTabFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.SongsTabFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;

public class AudioActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "AudioPrefs";
    public static final String KEY_IS_GRID = "isGridView";
    public static final String KEY_ICON_SIZE = "iconSize";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private View toolbarContent;
    private SearchView searchView;
    private boolean isGridView = true;
    private String iconSize = "large";
    private String[] tabs = {"Songs", "Folders"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_audio);

        loadPrefs();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle("Audio");
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        viewPager.setAdapter(new AudioPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabs[position])).attach();

        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    finishAffinity();
                    startActivity(new Intent(AudioActivity.this, SplashActivity.class));
                    return true;
                } else if (id == R.id.nav_files) {
                    Intent intent = new Intent(AudioActivity.this, DocumentsActivity.class);
                    intent.putExtra("SHOW_FILES", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_clean) {
                    Intent intent = new Intent(AudioActivity.this, DocumentsActivity.class);
                    intent.putExtra("SHOW_CLEAN", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return true;
            }
        });

        findViewById(R.id.menu_view_toggle).setOnClickListener(v -> showViewToggleDialog());
        findViewById(R.id.menu_more).setOnClickListener(v -> showMoreMenu(v));
        
        toolbarContent = findViewById(R.id.toolbar_content);
        searchView = findViewById(R.id.search_view);
        findViewById(R.id.menu_search).setOnClickListener(v -> showSearch());
        setupSearch();

        loadBannerAd();
        updateLayoutIcon();
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
                if (currentFragment instanceof SongsTabFragment) {
                    ((SongsTabFragment) currentFragment).onSearch(newText);
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
                    Toast.makeText(this, "Sort coming soon", Toast.LENGTH_SHORT).show();
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
            
            // Refresh fragments
            viewPager.setAdapter(new AudioPagerAdapter(this));
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
                
                DisplayMetrics outMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
                float widthPixels = outMetrics.widthPixels;
                float density = outMetrics.density;
                int adWidth = (int) (widthPixels / density);
                AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
                
                mAdView.setAdSize(adSize);
                mAdView.loadAd(new AdRequest.Builder().build());
            }
        } catch (Exception e) {
            Log.e("AudioActivity", "Error loading banner", e);
        }
    }

    private static class AudioPagerAdapter extends FragmentStateAdapter {

        public AudioPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new SongsTabFragment();
            return new AudioFoldersTabFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
