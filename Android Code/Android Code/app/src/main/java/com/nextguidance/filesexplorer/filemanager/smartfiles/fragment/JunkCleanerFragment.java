package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;

import java.io.File;

import java.util.Random;

public class JunkCleanerFragment extends Fragment {
    public static final String TAG = "JunkCleanerFragment";

    private ProgressBar mainProgress;
    private TextView textSize, textUnit, textPath, textBtnMain;
    private LinearLayout categoriesContainer;
    private View bottomActionContainer, btnCleanAction;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long totalJunkBytes = 0;
    private int currentCategoryIndex = 0;
    private boolean isScanning = false;
    
    private final String[] categories = {
        "App cache", "Residual junk", "Empty folders", "System junk", "AD junk", "Apks"
    };
    
    private final long[] categorySizes = new long[categories.length];
    private long lastPathUpdateTime = 0;

    public static void show(FragmentManager fm) {
        JunkCleanerFragment fragment = new JunkCleanerFragment();
        fm.beginTransaction()
            .replace(R.id.container_directory, fragment, TAG)
            .addToBackStack(null)
            .commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_junk_cleaner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupToolbar(view);
        mainProgress = view.findViewById(R.id.scanning_progress);
        textSize = view.findViewById(R.id.text_size);
        textUnit = view.findViewById(R.id.text_unit);
        textPath = view.findViewById(R.id.text_scanning_path);
        categoriesContainer = view.findViewById(R.id.categories_container);
        bottomActionContainer = view.findViewById(R.id.bottom_action_container);
        btnCleanAction = view.findViewById(R.id.btn_clean_now);
        textBtnMain = view.findViewById(R.id.text_btn_main);
        
        btnCleanAction.setOnClickListener(v -> {
             String formattedSize = Formatter.formatFileSize(getContext(), totalJunkBytes);
             CleanerResultFragment.show(getFragmentManager(), new java.util.ArrayList<>(), formattedSize, "Junk files");
        });

        initCategories();
        
        // Hide bottom nav
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        // Hide Activity toolbar
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) activityToolbar.setVisibility(View.GONE);

        startScanning();
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getFragmentManager().popBackStack());
    }

    private void initCategories() {
        categoriesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String cat : categories) {
            View itemView = inflater.inflate(R.layout.item_junk_category, categoriesContainer, false);
            TextView name = itemView.findViewById(R.id.text_category_name);
            name.setText(cat);
            categoriesContainer.addView(itemView);
        }
    }

    private void startScanning() {
        if (isScanning) return;
        isScanning = true;
        
        mainProgress.setProgress(0);
        totalJunkBytes = 0;
        currentCategoryIndex = 0;
        for (int i = 0; i < categorySizes.length; i++) categorySizes[i] = 0;

        new Thread(() -> {
            File root = android.os.Environment.getExternalStorageDirectory();
            recursiveScan(root);
            try { Thread.sleep(500); } catch (Exception e) {}
            handler.post(this::finishScanning);
        }).start();

        // UI Progress Update loop
        handler.post(new Runnable() {
            int progress = 0;
            @Override
            public void run() {
                if (!isAdded() || !isScanning) return;
                
                progress += 1;
                if (progress > 100) progress = 100;
                mainProgress.setProgress(progress);
                
                updateSizeDisplay();
                
                // Gradually complete categories
                if (progress % 16 == 0 && currentCategoryIndex < categories.length) {
                    completeCategory(currentCategoryIndex);
                    currentCategoryIndex++;
                }

                if (isScanning || progress < 100) {
                    handler.postDelayed(this, 40);
                }
            }
        });
    }

    private void recursiveScan(File dir) {
        if (dir == null || !dir.exists() || !isScanning) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isScanning) return;
            
            final String path = file.getAbsolutePath();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPathUpdateTime > 100) {
                lastPathUpdateTime = currentTime;
                handler.post(() -> {
                    if (isAdded()) textPath.setText("Scanning: " + path);
                });
            }

            if (file.isDirectory()) {
                String name = file.getName().toLowerCase();
                if (name.contains("cache") || name.equals(".thumbnails")) {
                    long size = getFolderSize(file);
                    totalJunkBytes += size;
                    categorySizes[0] += size; // App cache
                } else if (name.contains("ad") || name.contains("ads")) {
                    long size = getFolderSize(file);
                    totalJunkBytes += size;
                    categorySizes[4] += size; // AD Junk
                } else {
                    File[] sub = file.listFiles();
                    if (sub == null || sub.length == 0) {
                        categorySizes[2] += 4096; // Empty folders
                        totalJunkBytes += 4096;
                    } else {
                        recursiveScan(file);
                    }
                }
            } else {
                String name = file.getName().toLowerCase();
                long length = file.length();
                if (name.endsWith(".apk")) {
                    categorySizes[5] += length;
                    totalJunkBytes += length;
                } else if (name.endsWith(".log") || name.endsWith(".txt") && name.contains("log")) {
                    categorySizes[3] += length;
                    totalJunkBytes += length;
                } else if (name.endsWith(".tmp") || name.endsWith(".temp") || name.contains("residual")) {
                    categorySizes[1] += length;
                    totalJunkBytes += length;
                }
            }
        }
    }

    private long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File sub : files) size += getFolderSize(sub);
            }
        } else {
            size = f.length();
        }
        return size;
    }

    private void updateSizeDisplay() {
        String sizeStr = Formatter.formatFileSize(getContext(), totalJunkBytes);
        String[] parts = sizeStr.split(" ");
        if (parts.length >= 2) {
            textSize.setText(parts[0]);
            textUnit.setText(parts[1]);
        }
    }

    private void completeCategory(int index) {
        if (index >= categoriesContainer.getChildCount()) return;
        View view = categoriesContainer.getChildAt(index);
        view.findViewById(R.id.category_progress).setVisibility(View.GONE);
        
        TextView sizeView = view.findViewById(R.id.text_category_size);
        ImageView statusIcon = view.findViewById(R.id.category_status_icon);
        
        long size = categorySizes[index];
        if (size > 0) {
            sizeView.setVisibility(View.VISIBLE);
            sizeView.setText(Formatter.formatFileSize(getContext(), size));
            sizeView.setTextColor(0xFF333333);
            
            statusIcon.setVisibility(View.VISIBLE);
            if (index == 2) { // Empty folders
                statusIcon.setImageResource(R.drawable.ic_check_circle_outline_blue);
            } else {
                statusIcon.setImageResource(R.drawable.ic_check_circle_solid);
                statusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
            }
        } else {
            sizeView.setVisibility(View.VISIBLE);
            sizeView.setText("No junk");
            sizeView.setTextColor(0xFF9E9E9E);
            statusIcon.setVisibility(View.GONE);
        }
    }

    private void finishScanning() {
        isScanning = false;
        mainProgress.setProgress(100);
        textPath.setText("Scan completed");
        if (bottomActionContainer != null) bottomActionContainer.setVisibility(View.VISIBLE);
        
        updateSizeDisplay();
        
        String formattedSize = Formatter.formatFileSize(getContext(), totalJunkBytes);
        textBtnMain.setText("Clean up " + formattedSize);
        
        for (int i = 0; i < categories.length; i++) {
            completeCategory(i);
        }
    }

    @Override
    public void onDestroyView() {
        isScanning = false;
        // Restore bottom nav
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        // Restore Activity toolbar
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) activityToolbar.setVisibility(View.VISIBLE);

        super.onDestroyView();
    }
}
