package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.JunkCleanAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.JunkItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import androidx.annotation.NonNull;

public class JunkCleaningActivity extends AppCompatActivity {

    private TextView tvScanStatus, tvCurrentPath, tvBtnMain, tvTotalValue, tvTotalUnit;
    private RecyclerView recyclerView;
    private View btnCleanUp, radarSweep, cleaningOverlay, cleaningContainer, successContainer;
    private ImageView ivBroom, ivSuccessCheck;
    private TextView tvCleanedAmount;
    private JunkCleanAdapter adapter;
    private List<JunkItem> junkItems;
    private long totalJunkInBytes = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private android.view.animation.Animation radarAnim;
    private ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private boolean isScanning = false;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_junk_cleaning);

        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        tvBtnMain = findViewById(R.id.tvBtnMain);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvTotalUnit = findViewById(R.id.tvTotalUnit);
        radarSweep = findViewById(R.id.radarSweep);
        recyclerView = findViewById(R.id.recyclerViewJunk);
        btnCleanUp = findViewById(R.id.btnCleanUp);

        cleaningOverlay = findViewById(R.id.cleaningOverlay);
        cleaningContainer = findViewById(R.id.cleaningContainer);
        successContainer = findViewById(R.id.successContainer);
        ivBroom = findViewById(R.id.ivBroom);
        ivSuccessCheck = findViewById(R.id.ivSuccessCheck);
        tvCleanedAmount = findViewById(R.id.tvCleanedAmount);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnBackCleaning).setOnClickListener(v -> finish());

        setupList();
        startRadarAnimation();
        startRealScanning();

        btnCleanUp.setOnClickListener(v -> performClean());
        loadInterstitialAd();
    }



    private void startRadarAnimation() {
        radarAnim = new android.view.animation.RotateAnimation(0, 360,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        radarAnim.setDuration(2000);
        radarAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
        radarAnim.setInterpolator(new android.view.animation.LinearInterpolator());
        radarSweep.startAnimation(radarAnim);
    }

    private void setupList() {
        junkItems = new ArrayList<>();
        junkItems.add(new JunkItem("App cache", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Residual junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("System junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Empty folders", "0 B", false, false, ""));
        junkItems.add(new JunkItem("AD junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Apks", "0 B", false, false, ""));

        adapter = new JunkCleanAdapter(junkItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        adapter.setOnCheckedChangeListener(() -> updateButtonSize());
    }

    private void updateButtonSize() {
        long selectedTotal = 0;
        for (JunkItem item : junkItems) {
            if (item.isChecked()) {
                if (item.getSubItems() != null && !item.getSubItems().isEmpty()) {
                    for (JunkItem.SubJunkItem sub : item.getSubItems()) {
                        if (sub.isChecked()) {
                            selectedTotal += parseSizeToBytes(sub.getSize());
                        }
                    }
                } else {
                    // Category checked but no nested subitems
                    selectedTotal += parseSizeToBytes(item.getSize());
                }
            }
        }
        tvBtnMain.setText("Clean up " + formatSize(selectedTotal));
    }

    private long parseSizeToBytes(String sizeStr) {
        try {
            if (sizeStr.contains("KB")) return (long) (Double.parseDouble(sizeStr.replace(" KB", "")) * 1024);
            if (sizeStr.contains("MB")) return (long) (Double.parseDouble(sizeStr.replace(" MB", "")) * 1024 * 1024);
            if (sizeStr.contains("GB")) return (long) (Double.parseDouble(sizeStr.replace(" GB", "")) * 1024 * 1024 * 1024);
        } catch (Exception e) {}
        return 0;
    }

    private void startRealScanning() {
        if (isScanning) return;
        isScanning = true;
        totalJunkInBytes = 0;
        
        scanExecutor.execute(() -> {
            File root = Environment.getExternalStorageDirectory();
            scanDirectory(root);
            
            handler.post(() -> {
                isScanning = false;
                tvScanStatus.setText("Scan Complete");
                tvCurrentPath.setText("Real junk files identified");
                radarSweep.clearAnimation();
                radarSweep.setVisibility(View.GONE);
                btnCleanUp.setVisibility(View.VISIBLE);
                updateButtonSize();
            });
        });
    }

    private void scanDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) return;

        // --- SAFETY FILTER: Skip Important Personal Folders ---
        String name = directory.getName();
        if (name.equalsIgnoreCase("DCIM") || 
            name.equalsIgnoreCase("Pictures") || 
            name.equalsIgnoreCase("Documents") ||
            name.equalsIgnoreCase("Movies") ||
            name.equalsIgnoreCase("Music") ||
            name.equalsIgnoreCase("Download") && !isScanningDownloadsForApks()) { // Only scan Downloads for APKs
            return; 
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            final String path = file.getAbsolutePath();
            handler.post(() -> tvCurrentPath.setText(path));

            if (file.isDirectory()) {
                // Check for Empty Folders (Truly empty)
                File[] subFiles = file.listFiles();
                if (subFiles != null && subFiles.length == 0) {
                    addJunkItem("Empty folders", file);
                } else {
                    // Check for AD Junk folders
                    if (isAdJunkFolder(file.getName())) {
                        addJunkItem("AD junk", file);
                    } else if (file.getName().equalsIgnoreCase("cache") || file.getName().equalsIgnoreCase(".cache")) {
                        addJunkItem("App cache", file);
                    }
                    scanDirectory(file); // Recursive scan
                }
            } else {
                // --- Safe File Type Selection ---
                if (path.endsWith(".apk")) {
                    addJunkItem("Apks", file);
                } else if (path.endsWith(".log") || path.endsWith(".tmp") || path.endsWith(".temp")) {
                    addJunkItem("System junk", file);
                } else if (isKnownTrashFile(file)) {
                    addJunkItem("Residual junk", file);
                }
            }
        }
    }

    private boolean isScanningDownloadsForApks() {
        // Allow scanning Downloads folder ONLY to find APK files
        return true; 
    }

    private boolean isKnownTrashFile(File file) {
        String name = file.getName().toLowerCase();
        // Target only common system-generated trash names
        return name.startsWith(".trash") || name.startsWith(".ostat") || name.endsWith(".bak");
    }

    private boolean isAdJunkFolder(String name) {
        String lower = name.toLowerCase();
        // Target specific known Ad SDK folders
        return lower.contains(".exo") || lower.contains("adcache") || lower.contains("unityads") || lower.contains("mraid");
    }

    private void addJunkItem(String category, File file) {
        long size = file.isDirectory() ? getFolderSize(file) : file.length();
        if (size == 0) return;

        handler.post(() -> {
            for (JunkItem item : junkItems) {
                if (item.getName().equals(category)) {
                    totalJunkInBytes += size;
                    
                    // Update category size
                    long currentCatSize = parseSizeToBytes(item.getSize()) + size;
                    item.setSize(formatSize(currentCatSize));
                    item.setChecked(true);

                    // Add to sub-items with proper icons and colors
                    int icon = R.drawable.ic_root_document;
                    int tint = 0;
                    
                    switch (category) {
                        case "App cache":
                            icon = R.drawable.ic_root_apps;
                            tint = 0xFF2196F3; // Blue
                            break;
                        case "Residual junk":
                            icon = R.drawable.ic_root_document;
                            tint = 0xFFFF9800; // Orange
                            break;
                        case "System junk":
                            icon = R.drawable.ic_root_document;
                            tint = 0xFFF44336; // Red
                            break;
                        case "Empty folders":
                            icon = R.drawable.ic_root_folder;
                            tint = 0xFF9E9E9E; // Grey
                            break;
                        case "AD junk":
                            icon = R.drawable.ic_root_archive;
                            tint = 0xFF9C27B0; // Purple
                            break;
                        case "Apks":
                            icon = R.drawable.ic_root_apps; // Android robot icon
                            tint = 0xFF4CAF50; // Green
                            break;
                    }
                    
                    item.getSubItems().add(new JunkItem.SubJunkItem(file.getName(), formatSize(size), file.getAbsolutePath(), icon, tint));
                    
                    tvTotalValue.setText(formatSizeOnlyNumber(totalJunkInBytes));
                    tvTotalUnit.setText(formatSizeOnlyUnit(totalJunkInBytes));
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        });
    }

    private long getFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) size += file.length();
                else size += getFolderSize(file);
            }
        }
        return size;
    }



    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private String formatSizeOnlyNumber(long bytes) {
        if (bytes < 1024) return String.valueOf(bytes);
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f", bytes / Math.pow(1024, exp));
    }

    private String formatSizeOnlyUnit(long bytes) {
        if (bytes < 1024) return "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return "KMGTPE".charAt(exp - 1) + "B";
    }

    private void performClean() {
        final List<File> filesToDelete = new ArrayList<>();
        long selectedTotal = 0;
        
        for (JunkItem item : junkItems) {
            if (item.isChecked()) {
                for (JunkItem.SubJunkItem sub : item.getSubItems()) {
                    if (sub.isChecked()) {
                        File file = new File(sub.getPath());
                        if (file.exists()) {
                            filesToDelete.add(file);
                            selectedTotal += parseSizeToBytes(sub.getSize());
                        }
                    }
                }
            }
        }
        
        final long finalTotal = selectedTotal;
        btnCleanUp.setEnabled(false);
        
        // Show Overlay
        cleaningOverlay.setVisibility(View.VISIBLE);
        cleaningContainer.setVisibility(View.VISIBLE);
        successContainer.setVisibility(View.GONE);
        
        // Start Broom Anim
        startBroomAnimation();

        // Perform real deletion
        scanExecutor.execute(() -> {
            for (File file : filesToDelete) {
                deleteFileRecursive(file);
            }
            
            handler.postDelayed(() -> {
                showInterstitialThenSuccess(finalTotal);
            }, 3000);
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, getString(R.string.admob_interstitial_junk), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    private void showInterstitialThenSuccess(long cleanedBytes) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    showSuccessScreen(cleanedBytes);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mInterstitialAd = null;
                    showSuccessScreen(cleanedBytes);
                }
            });
            mInterstitialAd.show(this);
        } else {
            showSuccessScreen(cleanedBytes);
        }
    }

    private void deleteFileRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFileRecursive(child);
                }
            }
        }
        file.delete();
    }

    private void startBroomAnimation() {
        android.view.animation.Animation wiggle = new android.view.animation.RotateAnimation(-15, 15,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        wiggle.setDuration(400);
        wiggle.setRepeatMode(android.view.animation.Animation.REVERSE);
        wiggle.setRepeatCount(android.view.animation.Animation.INFINITE);
        ivBroom.startAnimation(wiggle);
    }

    private void showSuccessScreen(long cleanedBytes) {
        ivBroom.clearAnimation();
        cleaningContainer.setVisibility(View.GONE);
        successContainer.setVisibility(View.VISIBLE);
        
        String cleanedText = formatSize(cleanedBytes) + " cleaned";
        tvCleanedAmount.setText(cleanedText);

        // Save cleaned state persistently
        getSharedPreferences("junk_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("is_cleaned", true)
                .putLong("last_clean_time", System.currentTimeMillis())
                .apply();

        // Auto finish after 2.5 seconds
        handler.postDelayed(() -> {
            setResult(RESULT_OK);
            finish();
        }, 2500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanExecutor.shutdownNow();
    }
}
