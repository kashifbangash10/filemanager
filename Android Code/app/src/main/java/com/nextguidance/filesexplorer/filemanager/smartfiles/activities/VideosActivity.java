package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.VideosTabFragment;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.SearchView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.FoldersTabFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.AboutActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.SplashActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import android.util.DisplayMetrics;
import android.util.Log;

public class VideosActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private boolean isGridView = true;
    private static final String PREFS_NAME = "VideoPrefs";
    private static final String KEY_IS_GRID = "isGridView";
    private View toolbarContent;
    private SearchView searchView;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0288D1"));
        setContentView(R.layout.activity_videos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            getSupportActionBar().setTitle("Video");
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        viewPager.setAdapter(new VideosPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Videos");
            } else {
                tab.setText("Folders");
            }
        }).attach();

        bottomNavigation.setSelectedItemId(R.id.nav_home); // Match home screen image selection
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    finishAffinity();
                    startActivity(new Intent(VideosActivity.this, SplashActivity.class));
                    return true;
                } else if (id == R.id.nav_files) {
                    Intent intent = new Intent(VideosActivity.this, DocumentsActivity.class);
                    intent.putExtra("SHOW_FILES", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_clean) {
                    Intent intent = new Intent(VideosActivity.this, DocumentsActivity.class);
                    intent.putExtra("SHOW_CLEAN", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return true;
            }
        });

        findViewById(R.id.menu_grid_list).setOnClickListener(v -> toggleGridList());
        findViewById(R.id.menu_more).setOnClickListener(v -> showMoreMenu(v));
        
        toolbarContent = findViewById(R.id.toolbar_content);
        searchView = findViewById(R.id.search_view);
        findViewById(R.id.menu_search).setOnClickListener(v -> showSearch());
        setupSearch();

        loadPrefs();
        loadBannerAd();
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
                // Pass search to currently visible fragment
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                if (currentFragment instanceof VideosTabFragment) {
                    ((VideosTabFragment) currentFragment).onSearch(newText);
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
            // Implement sorting logic in fragment
            return true;
        });
        sortPopup.show();
    }

    private void toggleGridList() {
        isGridView = !isGridView;
        updateLayoutIcon();
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_GRID, isGridView);
        editor.apply();

        // Notify fragments to update their layout
        // In a real app, you'd use a ViewModel or EventBus
        // For now, we'll recreate the adapter or refresh current fragment
        viewPager.setAdapter(new VideosPagerAdapter(this));
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isGridView = prefs.getBoolean(KEY_IS_GRID, true);
        updateLayoutIcon();
    }

    private void updateLayoutIcon() {
        ImageView gridListIcon = findViewById(R.id.menu_grid_list);
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
            mAdView = new AdView(this);
            mAdView.setAdUnitId(getString(R.string.admob_banner));
            
            FrameLayout container = findViewById(R.id.banner_container);
            if (container != null) {
                container.removeAllViews();
                container.addView(mAdView);
                
                AdSize adSize = getAdSize();
                mAdView.setAdSize(adSize);
                
                AdRequest adRequest = new AdRequest.Builder().build();
                Log.d("AdMob", "Loading Banner Ad in VideosActivity with ID: " + getString(R.string.admob_banner));
                mAdView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.e("AdMob", "Banner Ad failed to load: " + loadAdError.getMessage() + " (Code: " + loadAdError.getCode() + ")");
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        Log.d("AdMob", "Banner Ad loaded successfully in VideosActivity!");
                    }
                });
                mAdView.loadAd(adRequest);
            } else {
                Log.e("AdMob", "banner_container not found in VideosActivity!");
            }
        } catch (Exception e) {
            Log.e("AdMob", "Error loading banner ad in VideosActivity: " + e.getMessage());
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

    private static class VideosPagerAdapter extends FragmentStateAdapter {

        public VideosPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new VideosTabFragment();
            } else {
                return new FoldersTabFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
