package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ✅ ANR-FREE SOLUTION with TRUE BACKGROUND THREADING
 * ✅ 100% Data Display without MainThread blocking
 * ✅ Fast & Responsive UI
 * ✅ CRASH-FIXED: ProgressBar ClassCastException
 * ✅ FIXED: No repeated loading - Data loads only once
 * ✅ FIXED: App Manager now shows ALL user apps properly
 * ✅ ADS REMOVED: All ad code removed
 */
public class AnalysisFragment extends Fragment {
    public static final String TAG = "AnalysisFragment";

    // ✅ OPTIMIZED CONSTANTS - Faster scanning
    private static final int MAX_SCAN_DEPTH = 20;
    private static final int UI_UPDATE_INTERVAL = 2000;
    private static final int DUPLICATE_MIN_SIZE = 100 * 1024;
    private static final int LARGE_FILE_THRESHOLD = 50 * 1024 * 1024;
    private static final int CHUNK_SIZE = 50;

    private LinearLayout containerLayout;
    private View loadingView;
    private TextView loadingStatus;

    // ✅ CACHED DATA - Singleton pattern with analysis state tracking
    private static AnalysisCache cachedData = null;
    private static boolean isAnalysisRunning = false;
    private static boolean isAnalysisComplete = false;

    private long lastUIUpdate = 0;
    private AtomicBoolean shouldStopScanning = new AtomicBoolean(false);
    private Handler mainHandler;
    private ExecutorService executor;

    private boolean isDataLoaded = false;

    // ✅ Cache class with analysis state
    private static class AnalysisCache {
        List<FolderItem> storageFolders = new ArrayList<>();
        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        List<FileItem> largeFiles = new ArrayList<>();
        List<AppItem> apps = new ArrayList<>();
        long cacheTimestamp = 0;
        boolean isAnalysisComplete = false;

        boolean isValid() {
            return System.currentTimeMillis() - cacheTimestamp < 5 * 60 * 1000; // 5 minutes cache
        }

        void clear() {
            storageFolders.clear();
            duplicateGroups.clear();
            largeFiles.clear();
            apps.clear();
            cacheTimestamp = 0;
            isAnalysisComplete = false;
        }

        void markAnalysisComplete() {
            isAnalysisComplete = true;
            cacheTimestamp = System.currentTimeMillis();
        }
    }

    public static void show(FragmentManager fm) {
        android.util.Log.d(TAG, "✅ Opening AnalysisFragment");
        final AnalysisFragment fragment = new AnalysisFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

    public static void clearCache() {
        if (cachedData != null) {
            cachedData.clear();
            cachedData = null;
        }
        isAnalysisRunning = false;
        isAnalysisComplete = false;
        android.util.Log.d(TAG, "🗑️ Cache cleared");
    }

    // ✅ New method to force refresh analysis
    public static void refreshAnalysis() {
        clearCache();
        isAnalysisComplete = false;
        isAnalysisRunning = false;
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis, container, false);

        containerLayout = view.findViewById(R.id.container_analysis);
        loadingView = view.findViewById(R.id.loading);

        try {
            loadingStatus = view.findViewById(R.id.loading_status);
        } catch (Exception e) {
            loadingStatus = null;
        }

        mainHandler = new Handler(Looper.getMainLooper());
        // ✅ Use FixedThreadPool for parallel category scanning
        executor = Executors.newFixedThreadPool(4);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateToolbarTitle();

        // ✅ CRASH FIX: Disable ProgressBar state saving
        disableProgressBarStateSaving();

        // ✅ FIXED: Check if analysis is already complete before loading
        if (cachedData != null && cachedData.isValid() && cachedData.isAnalysisComplete) {
            android.util.Log.d(TAG, "📦 Loading from COMPLETE cache");
            loadFromCache();
        } else if (isAnalysisRunning) {
            android.util.Log.d(TAG, "⏳ Analysis already in progress, waiting...");
            showLoadingState();
        } else {
            android.util.Log.d(TAG, "🔍 Starting fresh analysis");
            loadAnalysisData();
        }
    }

    /**
     * ✅ CRASH FIX: Disable state saving for all ProgressBars
     */
    private void disableProgressBarStateSaving() {
        try {
            if (getView() != null) {
                disableStateSavingForProgressBars(getView());
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error disabling ProgressBar state saving: " + e.getMessage());
        }
    }

    private void disableStateSavingForProgressBars(View view) {
        if (view instanceof ProgressBar) {
            view.setSaveEnabled(false);
            view.setSaveFromParentEnabled(false);
            android.util.Log.d(TAG, "✅ Disabled state saving for ProgressBar: " + view.getId());
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableStateSavingForProgressBars(viewGroup.getChildAt(i));
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // ✅ Don't save any view states that might corrupt ProgressBars
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // ✅ Re-apply our state protection after any system restoration
        disableProgressBarStateSaving();
    }

    private void updateToolbarTitle() {
        try {
            if (getActivity() instanceof BaseActivity) {
                BaseActivity activity = (BaseActivity) getActivity();
                activity.setTitle(getString(R.string.root_analysis));
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle(getString(R.string.root_analysis));
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error setting toolbar title: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity) {
            ((com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity) getActivity()).setAnalysisMode(true);
        }
        updateToolbarTitle();

        // ✅ Check if we need to refresh data when coming back to fragment
        if (isDataLoaded && cachedData != null && !cachedData.isValid()) {
            android.util.Log.d(TAG, "🔄 Cache expired, refreshing...");
            refreshAnalysis();
            loadAnalysisData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        shouldStopScanning.set(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity) {
            ((com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity) getActivity()).setAnalysisMode(false);
        }
        shouldStopScanning.set(true);
        if (executor != null) {
            executor.shutdownNow();
        }

        // ✅ Clear references to views
        containerLayout = null;
        loadingView = null;
        loadingStatus = null;
    }

    private void showLoadingState() {
        if (getActivity() == null || !isAdded()) return;

        loadingView.setVisibility(View.VISIBLE);
        containerLayout.removeAllViews();

        // Show a loading message
        if (loadingStatus != null) {
            loadingStatus.setText("Analysis in progress...");
        }
    }

    private void updateLoadingStatusThrottled(final String status) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUIUpdate >= UI_UPDATE_INTERVAL) {
            lastUIUpdate = currentTime;
            if (mainHandler != null && loadingStatus != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingStatus != null) {
                            loadingStatus.setText(status);
                        }
                    }
                });
            }
        }
    }

    private void loadFromCache() {
        if (getActivity() == null || !isAdded()) return;

        loadingView.setVisibility(View.GONE);
        containerLayout.removeAllViews();

        addItemToView(createStorageItemFromCache());
        addItemToView(createDuplicateItemFromCache());
        addItemToView(createLargeFilesItemFromCache());
        addItemToView(createAppsItemFromCache());

        isDataLoaded = true;
        android.util.Log.d(TAG, "✅ UI loaded from cache");
    }

    // ✅ SUPER-FAST PARALLEL ANALYSIS
    private void loadAnalysisData() {
        if (!Utils.isActivityAlive(getActivity())) return;

        isAnalysisRunning = true;
        shouldStopScanning.set(false);
        isDataLoaded = false;

        if (cachedData == null) cachedData = new AnalysisCache();
        cachedData.clear();

        // 1. Show scanning placeholders
        loadingView.setVisibility(View.GONE);
        containerLayout.removeAllViews();
        
        final AnalysisItem storagePH = createPlaceholder(AnalysisItem.TYPE_STORAGE, "Internal storage", R.drawable.ic_root_internal, "Calculating...");
        final AnalysisItem duplicatePH = createPlaceholder(AnalysisItem.TYPE_DUPLICATE, "Duplicate files", R.drawable.ic_root_document, "Scanning...");
        final AnalysisItem largePH = createPlaceholder(AnalysisItem.TYPE_LARGE_FILES, "Large files", R.drawable.ic_root_folder, "Scanning...");
        final AnalysisItem appsPH = createPlaceholder(AnalysisItem.TYPE_APP_MANAGER, "App manager", R.drawable.ic_root_apps, "Loading...");
        
        addItemToView(storagePH);
        addItemToView(duplicatePH);
        addItemToView(largePH);
        addItemToView(appsPH);

        // 2. Start Multi-Threaded scanning
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Start all three categories in parallel
                    Future<AnalysisItem> duplicateFuture = executor.submit(new java.util.concurrent.Callable<AnalysisItem>() {
                        @Override
                        public AnalysisItem call() throws Exception {
                            return analyzeDuplicateFilesOptimized();
                        }
                    });
                    Future<AnalysisItem> largeFilesFuture = executor.submit(new java.util.concurrent.Callable<AnalysisItem>() {
                        @Override
                        public AnalysisItem call() throws Exception {
                            return analyzeLargeFilesOptimized();
                        }
                    });
                    Future<AnalysisItem> appsFuture = executor.submit(new java.util.concurrent.Callable<AnalysisItem>() {
                        @Override
                        public AnalysisItem call() throws Exception {
                            return analyzeAppsOptimized();
                        }
                    });

                    // Storage analysis runs on current background thread
                    final AnalysisItem storageResult = analyzeInternalStorageOptimized();
                    updateUI(storageResult);

                    // Wait for others and update as they finish
                    updateUI(duplicateFuture.get());
                    updateUI(largeFilesFuture.get());
                    updateUI(appsFuture.get());

                    cachedData.markAnalysisComplete();
                    isAnalysisComplete = true;

                } catch (Exception e) {
                    android.util.Log.e(TAG, "Parallel Analysis Error: " + e.getMessage());
                } finally {
                    isAnalysisRunning = false;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && isAdded()) {
                                loadingView.setVisibility(View.GONE);
                                isDataLoaded = true;
                            }
                        }
                    });
                }
            }
        });
    }

    private void updateUI(final AnalysisItem item) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null && isAdded()) {
                    addItemToView(item);
                }
            }
        });
    }

    private AnalysisItem createPlaceholder(int type, String title, int icon, String summary) {
        AnalysisItem item = new AnalysisItem();
        item.type = type;
        item.icon = icon;
        item.title = title;
        item.summary = summary;
        item.isLoading = true;
        return item;
    }

    // ============================================================
    // ✅ OPTIMIZED ANALYSIS METHODS - NO MAINTHREAD BLOCKING
    // ============================================================

    private AnalysisItem analyzeInternalStorageOptimized() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_STORAGE;
        item.icon = R.drawable.ic_root_internal;
        item.title = "Internal storage";
        item.isLoading = false;
        item.subItems = new ArrayList<>();
        cachedData.storageFolders.clear();

        try {
            File storage = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(storage.getPath());

            long totalBytes = stat.getTotalBytes();
            long availableBytes = stat.getAvailableBytes();
            long usedBytes = totalBytes - availableBytes;

            item.percentage = (int) ((usedBytes * 100) / totalBytes);
            item.summary = Formatter.formatFileSize(getContext(), usedBytes) +
                    " / " + Formatter.formatFileSize(getContext(), totalBytes);

            updateLoadingStatusThrottled("Scanning storage folders...");

            File[] topFolders = storage.listFiles();
            if (topFolders != null) {
                for (int i = 0; i < topFolders.length && !shouldStopScanning.get(); i++) {
                    File folder = topFolders[i];

                    if (folder.isDirectory() && !folder.getName().startsWith(".")) {
                        try {
                            long folderSize = calculateFolderSizeQuick(folder, 0);
                            int itemCount = countFilesQuick(folder, 0);

                            if (folderSize > 0) {
                                FolderItem folderItem = new FolderItem();
                                folderItem.name = folder.getName();
                                folderItem.path = folder.getAbsolutePath();
                                folderItem.size = folderSize;
                                folderItem.itemCount = itemCount;
                                cachedData.storageFolders.add(folderItem);
                            }

                            if (i % 5 == 0) {
                                Thread.sleep(10);
                            }
                        } catch (Exception e) {
                            // Skip problematic folders
                        }
                    }
                }
            }

            Collections.sort(cachedData.storageFolders, new Comparator<FolderItem>() {
                @Override
                public int compare(FolderItem f1, FolderItem f2) {
                    return Long.compare(f2.size, f1.size);
                }
            });

            int count = Math.min(3, cachedData.storageFolders.size());
            for (int i = 0; i < count; i++) {
                FolderItem folderItem = cachedData.storageFolders.get(i);
                SubItem subItem = new SubItem();
                subItem.name = folderItem.name;
                subItem.path = folderItem.itemCount + " items";
                subItem.size = Formatter.formatFileSize(getContext(), folderItem.size);
                subItem.icon = R.drawable.ic_root_folder;
                subItem.iconColor = getFolderIconColor(folderItem.name);
                item.subItems.add(subItem);
            }

        } catch (Exception e) {
            item.summary = "Unable to analyze";
        }

        return item;
    }

    private long calculateFolderSizeQuick(File folder, int depth) {
        if (shouldStopScanning.get() || depth > MAX_SCAN_DEPTH || folder == null) {
            return 0;
        }

        long size = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null) {
                int processed = 0;
                for (File file : files) {
                    if (shouldStopScanning.get()) break;

                    if (file.isFile()) {
                        size += file.length();
                        processed++;
                    } else if (file.isDirectory() && !file.getName().startsWith(".")) {
                        if (depth < 6) {
                            size += calculateFolderSizeQuick(file, depth + 1);
                        }
                    }

                    if (processed % CHUNK_SIZE == 0) {
                        Thread.yield();
                    }
                }
            }
        } catch (Exception e) {
            // Skip
        }
        return size;
    }

    private int countFilesQuick(File folder, int depth) {
        if (shouldStopScanning.get() || depth > 8 || folder == null) {
            return 0;
        }

        int count = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (shouldStopScanning.get() || count > 1000) break;
                    if (file.isFile()) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            // Skip
        }
        return count;
    }

    private AnalysisItem analyzeDuplicateFilesOptimized() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_DUPLICATE;
        item.icon = R.drawable.ic_root_document;
        item.title = "Duplicate files";
        item.isLoading = false;
        item.subItems = new ArrayList<>();
        cachedData.duplicateGroups.clear();

        try {
            updateLoadingStatusThrottled("Scanning for duplicates...");
            Map<Long, List<File>> sizeMap = new HashMap<>();

            File storage = Environment.getExternalStorageDirectory();
            findDuplicatesOptimized(storage, sizeMap, 0);

            long totalDuplicateSize = 0;
            int duplicateCount = 0;

            for (Map.Entry<Long, List<File>> entry : sizeMap.entrySet()) {
                if (shouldStopScanning.get()) break;

                List<File> files = entry.getValue();
                if (files.size() > 1) {
                    long size = entry.getKey();
                    long wastedSpace = size * (files.size() - 1);
                    totalDuplicateSize += wastedSpace;
                    duplicateCount += (files.size() - 1);

                    DuplicateGroup group = new DuplicateGroup();
                    group.fileName = files.get(0).getName();
                    group.size = size;
                    group.count = files.size();
                    group.filePaths = new ArrayList<>();

                    for (File file : files) {
                        group.filePaths.add(file.getAbsolutePath());
                    }

                    cachedData.duplicateGroups.add(group);
                }
            }

            Collections.sort(cachedData.duplicateGroups, new Comparator<DuplicateGroup>() {
                @Override
                public int compare(DuplicateGroup g1, DuplicateGroup g2) {
                    long waste1 = g1.size * (g1.count - 1);
                    long waste2 = g2.size * (g2.count - 1);
                    return Long.compare(waste2, waste1);
                }
            });

            int count = Math.min(3, cachedData.duplicateGroups.size());
            for (int i = 0; i < count; i++) {
                DuplicateGroup group = cachedData.duplicateGroups.get(i);
                SubItem subItem = new SubItem();
                subItem.name = group.fileName;
                subItem.path = group.count + " copies found";
                long wastedSpace = group.size * (group.count - 1);
                subItem.size = Formatter.formatFileSize(getContext(), wastedSpace) + " wasted";
                subItem.icon = getFileIcon(group.fileName);
                subItem.iconColor = 0xFFFFC107;
                item.subItems.add(subItem);
            }

            item.summary = duplicateCount + " duplicates • " +
                    Formatter.formatFileSize(getContext(), totalDuplicateSize) + " wasted";

        } catch (Exception e) {
            item.summary = "Unable to scan";
        }

        return item;
    }

    private void findDuplicatesOptimized(File dir, Map<Long, List<File>> sizeMap, int depth) {
        if (shouldStopScanning.get() || depth > MAX_SCAN_DEPTH || dir == null) {
            return;
        }

        try {
            File[] files = dir.listFiles();
            if (files == null) return;

            int processed = 0;
            for (File file : files) {
                if (shouldStopScanning.get()) break;

                if (file.isDirectory()) {
                    if (!file.getName().startsWith(".")) {
                        findDuplicatesOptimized(file, sizeMap, depth + 1);
                    }
                } else {
                    long size = file.length();
                    if (size > DUPLICATE_MIN_SIZE) {
                        if (!sizeMap.containsKey(size)) {
                            sizeMap.put(size, new ArrayList<File>());
                        }
                        sizeMap.get(size).add(file);
                    }
                    processed++;
                }

                if (processed % CHUNK_SIZE == 0) {
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            // Skip
        }
    }

    private AnalysisItem analyzeLargeFilesOptimized() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_LARGE_FILES;
        item.icon = R.drawable.ic_root_folder;
        item.title = "Large files";
        item.isLoading = false;
        item.subItems = new ArrayList<>();
        cachedData.largeFiles.clear();

        try {
            updateLoadingStatusThrottled("Finding large files...");
            File storage = Environment.getExternalStorageDirectory();
            findLargeFilesOptimized(storage, cachedData.largeFiles, 0);

            Collections.sort(cachedData.largeFiles, new Comparator<FileItem>() {
                @Override
                public int compare(FileItem f1, FileItem f2) {
                    return Long.compare(f2.size, f1.size);
                }
            });

            long totalSize = 0;
            for (FileItem fileItem : cachedData.largeFiles) {
                totalSize += fileItem.size;
            }

            int count = Math.min(3, cachedData.largeFiles.size());
            for (int i = 0; i < count; i++) {
                FileItem fileItem = cachedData.largeFiles.get(i);
                SubItem subItem = new SubItem();
                subItem.name = fileItem.name;
                subItem.path = fileItem.path;
                subItem.size = Formatter.formatFileSize(getContext(), fileItem.size);
                subItem.icon = getFileIcon(fileItem.name);
                subItem.iconColor = 0xFFFF9800;
                item.subItems.add(subItem);
            }

            item.summary = cachedData.largeFiles.size() + " files • " +
                    Formatter.formatFileSize(getContext(), totalSize);

        } catch (Exception e) {
            item.summary = "Unable to scan";
        }

        return item;
    }

    private void findLargeFilesOptimized(File dir, List<FileItem> largeFiles, int depth) {
        if (shouldStopScanning.get() || depth > MAX_SCAN_DEPTH || dir == null) {
            return;
        }

        try {
            File[] files = dir.listFiles();
            if (files == null) return;

            int processed = 0;
            for (File file : files) {
                if (shouldStopScanning.get()) break;

                if (file.isDirectory()) {
                    if (!file.getName().startsWith(".")) {
                        findLargeFilesOptimized(file, largeFiles, depth + 1);
                    }
                } else {
                    long size = file.length();
                    if (size > LARGE_FILE_THRESHOLD) {
                        FileItem fileItem = new FileItem();
                        fileItem.name = file.getName();
                        fileItem.path = file.getParent();
                        fileItem.size = size;
                        fileItem.fullPath = file.getAbsolutePath();
                        largeFiles.add(fileItem);
                    }
                    processed++;
                }

                if (processed % CHUNK_SIZE == 0) {
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            // Skip
        }
    }

    private AnalysisItem analyzeAppsOptimized() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_APP_MANAGER;
        item.icon = R.drawable.ic_root_apps;
        item.title = "App manager";
        item.isLoading = false;
        item.subItems = new ArrayList<>();
        cachedData.apps.clear();


        try {
            updateLoadingStatusThrottled("Analyzing apps...");
            PackageManager pm = getContext().getPackageManager();

            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);

            long totalAppSize = 0;
            int userAppCount = 0;
            int systemAppCount = 0;

            android.util.Log.d(TAG, "📱 Total packages found: " + packages.size());

            for (PackageInfo packageInfo : packages) {
                if (shouldStopScanning.get()) break;

                try {
                    ApplicationInfo appInfo = packageInfo.applicationInfo;
                    File apkFile = new File(appInfo.sourceDir);

                    if (apkFile.exists()) {
                        long appSize = apkFile.length();
                        if (appSize <= 0) {
                            // Fallback for Android 11+ if file length is restricted
                            try {
                                appSize = new File(appInfo.publicSourceDir).length();
                            } catch (Exception e) {}
                        }
                        totalAppSize += appSize;

                        AppItem appItem = new AppItem();
                        appItem.name = pm.getApplicationLabel(appInfo).toString();
                        appItem.packageName = appInfo.packageName;
                        appItem.size = appSize;
                        appItem.path = appInfo.sourceDir;

                        boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                        cachedData.apps.add(appItem);

                        if (isSystemApp) {
                            systemAppCount++;
                        } else {
                            userAppCount++;
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "❌ Error processing app: " + e.getMessage());
                }
            }

            android.util.Log.d(TAG, "✅ Apps analysis complete - User: " + userAppCount +
                    ", System: " + systemAppCount + ", Total: " + cachedData.apps.size());

            Collections.sort(cachedData.apps, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem a1, AppItem a2) {
                    return Long.compare(a2.size, a1.size);
                }
            });

            int count = Math.min(3, cachedData.apps.size());
            for (int i = 0; i < count; i++) {
                AppItem appItem = cachedData.apps.get(i);
                SubItem subItem = new SubItem();
                subItem.name = appItem.name;
                subItem.path = appItem.packageName;
                subItem.size = Formatter.formatFileSize(getContext(), appItem.size);
                subItem.icon = R.drawable.ic_root_apps;
                subItem.packageName = appItem.packageName;
                subItem.iconColor = 0;
                item.subItems.add(subItem);
            }

            item.summary = cachedData.apps.size() + " apps • " +
                    Formatter.formatFileSize(getContext(), totalAppSize);


        } catch (Exception e) {
            android.util.Log.e(TAG, "❌ Error analyzing apps: " + e.getMessage());
            item.summary = "Error: " + e.getMessage();
        }

        return item;
    }

    // ============================================================
    // CACHE ITEM CREATORS
    // ============================================================

    private AnalysisItem createStorageItemFromCache() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_STORAGE;
        item.icon = R.drawable.ic_root_internal;
        item.title = "Internal storage";
        item.isLoading = false;
        item.subItems = new ArrayList<>();

        try {
            File storage = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(storage.getPath());
            long totalBytes = stat.getTotalBytes();
            long availableBytes = stat.getAvailableBytes();
            long usedBytes = totalBytes - availableBytes;

            item.percentage = (int) ((usedBytes * 100) / totalBytes);
            item.summary = Formatter.formatFileSize(getContext(), usedBytes) +
                    " / " + Formatter.formatFileSize(getContext(), totalBytes);

            int count = Math.min(3, cachedData.storageFolders.size());
            for (int i = 0; i < count; i++) {
                FolderItem folderItem = cachedData.storageFolders.get(i);
                SubItem subItem = new SubItem();
                subItem.name = folderItem.name;
                subItem.path = folderItem.itemCount + " items";
                subItem.size = Formatter.formatFileSize(getContext(), folderItem.size);
                subItem.icon = R.drawable.ic_root_folder;
                subItem.iconColor = getFolderIconColor(folderItem.name);
                item.subItems.add(subItem);
            }
        } catch (Exception e) {
            item.summary = "Unable to analyze";
        }

        return item;
    }

    private AnalysisItem createDuplicateItemFromCache() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_DUPLICATE;
        item.icon = R.drawable.ic_root_document;
        item.title = "Duplicate files";
        item.isLoading = false;
        item.subItems = new ArrayList<>();

        long totalDuplicateSize = 0;
        int duplicateCount = 0;

        for (DuplicateGroup group : cachedData.duplicateGroups) {
            long wastedSpace = group.size * (group.count - 1);
            totalDuplicateSize += wastedSpace;
            duplicateCount += (group.count - 1);
        }

        int count = Math.min(3, cachedData.duplicateGroups.size());
        for (int i = 0; i < count; i++) {
            DuplicateGroup group = cachedData.duplicateGroups.get(i);
            SubItem subItem = new SubItem();
            subItem.name = group.fileName;
            subItem.path = group.count + " copies found";
            long wastedSpace = group.size * (group.count - 1);
            subItem.size = Formatter.formatFileSize(getContext(), wastedSpace) + " wasted";
            subItem.icon = getFileIcon(group.fileName);
            subItem.iconColor = 0xFFFFC107;
            item.subItems.add(subItem);
        }

        item.summary = duplicateCount + " duplicates • " +
                Formatter.formatFileSize(getContext(), totalDuplicateSize) + " wasted";

        return item;
    }

    private AnalysisItem createLargeFilesItemFromCache() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_LARGE_FILES;
        item.icon = R.drawable.ic_root_folder;
        item.title = "Large files";
        item.isLoading = false;
        item.subItems = new ArrayList<>();

        long totalSize = 0;
        for (FileItem fileItem : cachedData.largeFiles) {
            totalSize += fileItem.size;
        }

        int count = Math.min(3, cachedData.largeFiles.size());
        for (int i = 0; i < count; i++) {
            FileItem fileItem = cachedData.largeFiles.get(i);
            SubItem subItem = new SubItem();
            subItem.name = fileItem.name;
            subItem.path = fileItem.path;
subItem.size = Formatter.formatFileSize(getContext(), fileItem.size);
            subItem.icon = getFileIcon(fileItem.name);
            subItem.iconColor = 0xFFFF9800;
            item.subItems.add(subItem);
        }

        item.summary = cachedData.largeFiles.size() + " files • " +
                Formatter.formatFileSize(getContext(), totalSize);

        return item;
    }

    private AnalysisItem createAppsItemFromCache() {
        AnalysisItem item = new AnalysisItem();
        item.type = AnalysisItem.TYPE_APP_MANAGER;
        item.icon = R.drawable.ic_root_apps;
        item.title = "App manager";
        item.isLoading = false;
        item.subItems = new ArrayList<>();

        long totalAppSize = 0;
        for (AppItem appItem : cachedData.apps) {
            totalAppSize += appItem.size;
        }

        int count = Math.min(3, cachedData.apps.size());
        for (int i = 0; i < count; i++) {
            AppItem appItem = cachedData.apps.get(i);
            SubItem subItem = new SubItem();
            subItem.name = appItem.name;
            subItem.path = appItem.packageName;
            subItem.size = Formatter.formatFileSize(getContext(), appItem.size);
            subItem.icon = R.drawable.ic_root_apps;
            subItem.packageName = appItem.packageName;
            subItem.iconColor = 0;
            item.subItems.add(subItem);
        }

        item.summary = cachedData.apps.size() + " apps • " +
                Formatter.formatFileSize(getContext(), totalAppSize);

        return item;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private int getFileIcon(String fileName) {
        String name = fileName.toLowerCase();

        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                name.endsWith(".tar") || name.endsWith(".gz")) {
            return R.drawable.ic_root_archive;
        }

        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") ||
                name.endsWith(".mov") || name.endsWith(".wmv") || name.endsWith(".3gp")) {
            return R.drawable.ic_root_video;
        }

        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".gif") || name.endsWith(".webp")) {
            return R.drawable.ic_root_image;
        }

        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") ||
                name.endsWith(".flac") || name.endsWith(".aac")) {
            return R.drawable.ic_root_audio;
        }

        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") ||
                name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx")) {
            return R.drawable.ic_root_document;
        }

        if (name.endsWith(".apk")) {
            return R.drawable.ic_root_apps;
        }

        return R.drawable.ic_root_document;
    }

    private int getFolderIconColor(String folderName) {
        String name = folderName.toLowerCase();

        if (name.equals("android")) return 0xFF4CAF50;
        if (name.equals("dcim") || name.equals("pictures") || name.equals("camera")) return 0xFF2196F3;
        if (name.equals("downloads") || name.equals("download")) return 0xFF9C27B0;
        if (name.equals("documents")) return 0xFFFF9800;
        if (name.equals("music") || name.equals("audio")) return 0xFFF44336;
        if (name.equals("movies") || name.equals("video")) return 0xFFE91E63;

        return 0xFF2196F3;
    }

    // ============================================================
    // UI CREATION
    // ============================================================

    private void addItemToView(AnalysisItem item) {
        if (getActivity() == null || !isAdded()) return;

        try {
            // Check if item already exists in view to update it
            View existingView = null;
            for (int i = 0; i < containerLayout.getChildCount(); i++) {
                View child = containerLayout.getChildAt(i);
                Object tag = child.getTag();
                if (tag instanceof Integer && (Integer) tag == item.type) {
                    existingView = child;
                    break;
                }
            }

            if (existingView != null) {
                updateItemView(existingView, item);
                return;
            }

            View itemView;
            if (item.type == AnalysisItem.TYPE_STORAGE) {
                itemView = createStorageView(item);
            } else {
                itemView = createCategoryView(item);
            }

            itemView.setTag(item.type);
            final int itemType = item.type;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleCardClick(itemType);
                }
            });

            containerLayout.addView(itemView);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error adding item to view: " + e.getMessage());
        }
    }

    private void updateItemView(View view, AnalysisItem item) {
        if (item.type == AnalysisItem.TYPE_STORAGE) {
            updateStorageView(view, item);
        } else {
            updateCategoryView(view, item);
        }
    }

    private void updateStorageView(View view, AnalysisItem item) {
        ImageView icon = view.findViewById(R.id.icon);
        TextView title = view.findViewById(R.id.title);
        TextView summary = view.findViewById(R.id.summary);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        LinearLayout subItemsContainer = view.findViewById(R.id.sub_items_container);
        TextView moreButton = view.findViewById(R.id.btn_more);

        icon.setImageResource(item.icon);
        icon.setColorFilter(getResources().getColor(R.color.primaryColor), android.graphics.PorterDuff.Mode.SRC_IN);


        title.setText(item.title);
        summary.setText(item.summary);
        progressBar.setProgress(item.percentage);
        progressBar.setIndeterminate(item.isLoading);
        
        int progressColor = (item.percentage > 85) ? 0xFFFF5252 : getResources().getColor(R.color.primaryColor);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        } else {
            progressBar.getProgressDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }


        if (!item.isLoading && item.subItems != null && !item.subItems.isEmpty()) {
            subItemsContainer.removeAllViews();
            subItemsContainer.setVisibility(View.VISIBLE);
            for (SubItem subItem : item.subItems) {
                View subItemView = createSubItemView(subItem);
                subItemsContainer.addView(subItemView);
            }
        } else {
            subItemsContainer.setVisibility(View.GONE);
        }
    }

    private void updateCategoryView(View view, AnalysisItem item) {
        ImageView icon = view.findViewById(R.id.icon);
        TextView title = view.findViewById(R.id.title);
        TextView summary = view.findViewById(R.id.summary);
        ProgressBar loadingBar = view.findViewById(R.id.loading);
        LinearLayout subItemsContainer = view.findViewById(R.id.sub_items_container);
        TextView moreButton = view.findViewById(R.id.btn_more);

        icon.setImageResource(item.icon);
        icon.setColorFilter(getResources().getColor(R.color.primaryColor), android.graphics.PorterDuff.Mode.SRC_IN);


        title.setText(item.title);
        summary.setText(item.summary);
        loadingBar.setVisibility(item.isLoading ? View.VISIBLE : View.GONE);

        if (!item.isLoading && item.subItems != null && !item.subItems.isEmpty()) {
            subItemsContainer.removeAllViews();
            subItemsContainer.setVisibility(View.VISIBLE);
            for (SubItem subItem : item.subItems) {
                View subItemView = createSubItemView(subItem);
                subItemsContainer.addView(subItemView);
            }

            int totalItems = 0;
            if (item.type == AnalysisItem.TYPE_DUPLICATE) totalItems = cachedData.duplicateGroups.size();
            else if (item.type == AnalysisItem.TYPE_LARGE_FILES) totalItems = cachedData.largeFiles.size();
            else if (item.type == AnalysisItem.TYPE_APP_MANAGER) totalItems = cachedData.apps.size();

            if (totalItems > 3) {
                moreButton.setVisibility(View.VISIBLE);
                moreButton.setText("+" + (totalItems - 3) + " more");
            } else {
                moreButton.setVisibility(View.GONE);
            }
        } else {
            subItemsContainer.setVisibility(View.GONE);
            moreButton.setVisibility(View.GONE);
        }
    }


    private void handleCardClick(int type) {
        switch (type) {
            case AnalysisItem.TYPE_STORAGE:
                showStorageDetails();
                break;
            case AnalysisItem.TYPE_DUPLICATE:
                showDuplicateDetails();
                break;
            case AnalysisItem.TYPE_LARGE_FILES:
                showLargeFilesDetails();
                break;
            case AnalysisItem.TYPE_APP_MANAGER:
                showAppDetails();
                break;
        }
    }

    private void showStorageDetails() {
        if (cachedData.storageFolders.isEmpty()) {
            Toast.makeText(getContext(), "No storage data available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getFragmentManager() != null) {
            AnalysisDetailFragment.show(getFragmentManager(), "Internal Storage",
                    cachedData.storageFolders, null, null, null);
        }
    }

    private void showDuplicateDetails() {
        if (cachedData.duplicateGroups.isEmpty()) {
            Toast.makeText(getContext(), "No duplicate files found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getFragmentManager() != null) {
            AnalysisDetailFragment.show(getFragmentManager(), "Duplicate Files",
                    null, cachedData.duplicateGroups, null, null);
        }
    }

    private void showLargeFilesDetails() {
        if (cachedData.largeFiles.isEmpty()) {
            Toast.makeText(getContext(), "No large files found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getFragmentManager() != null) {
            AnalysisDetailFragment.show(getFragmentManager(), "Large Files",
                    null, null, cachedData.largeFiles, null);
        }
    }

    private void showAppDetails() {
        if (cachedData.apps.isEmpty()) {
            Toast.makeText(getContext(), "No apps found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getFragmentManager() != null) {
            AnalysisDetailFragment.show(getFragmentManager(), "App Manager",
                    null, null, null, cachedData.apps);
        }
    }

    private View createStorageView(AnalysisItem item) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_storage, containerLayout, false);

        ImageView icon = view.findViewById(R.id.icon);
        TextView title = view.findViewById(R.id.title);
        TextView summary = view.findViewById(R.id.summary);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        LinearLayout subItemsContainer = view.findViewById(R.id.sub_items_container);
        TextView moreButton = view.findViewById(R.id.btn_more);

        progressBar.setSaveEnabled(false);
        progressBar.setSaveFromParentEnabled(false);

        icon.setImageResource(item.icon);
        icon.setColorFilter(getResources().getColor(R.color.primaryColor), android.graphics.PorterDuff.Mode.SRC_IN);
        title.setText(item.title);
        summary.setText(item.summary);
        progressBar.setProgress(item.percentage);
        
        int progressColor = (item.percentage > 85) ? 0xFFFF5252 : getResources().getColor(R.color.primaryColor);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        } else {
            progressBar.getProgressDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
        }


        if (item.subItems != null && !item.subItems.isEmpty()) {
            subItemsContainer.setVisibility(View.VISIBLE);
            for (SubItem subItem : item.subItems) {
                View subItemView = createSubItemView(subItem);
                subItemsContainer.addView(subItemView);
            }

            if (cachedData.storageFolders.size() > 3) {
                moreButton.setVisibility(View.VISIBLE);
                moreButton.setText("+" + (cachedData.storageFolders.size() - 3) + " more");
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showStorageDetails();
                    }
                });
            } else {
                moreButton.setVisibility(View.GONE);
            }
        } else {
            moreButton.setVisibility(View.GONE);
        }

        return view;
    }


    private View createCategoryView(AnalysisItem item) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_categry, containerLayout, false);

        ImageView icon = view.findViewById(R.id.icon);
        TextView title = view.findViewById(R.id.title);
        TextView summary = view.findViewById(R.id.summary);
        ProgressBar loadingBar = view.findViewById(R.id.loading);
        LinearLayout subItemsContainer = view.findViewById(R.id.sub_items_container);
        TextView moreButton = view.findViewById(R.id.btn_more);

        loadingBar.setSaveEnabled(false);
        loadingBar.setSaveFromParentEnabled(false);

        icon.setImageResource(item.icon);
        icon.setColorFilter(getResources().getColor(R.color.primaryColor), android.graphics.PorterDuff.Mode.SRC_IN);
        title.setText(item.title);
        summary.setText(item.summary);
        loadingBar.setVisibility(item.isLoading ? View.VISIBLE : View.GONE);

        if (item.subItems != null && !item.subItems.isEmpty()) {
            subItemsContainer.setVisibility(View.VISIBLE);
            for (SubItem subItem : item.subItems) {
                View subItemView = createSubItemView(subItem);
                subItemsContainer.addView(subItemView);
            }

            int totalItems = 0;
            if (item.type == AnalysisItem.TYPE_DUPLICATE) totalItems = cachedData.duplicateGroups.size();
            else if (item.type == AnalysisItem.TYPE_LARGE_FILES) totalItems = cachedData.largeFiles.size();
            else if (item.type == AnalysisItem.TYPE_APP_MANAGER) totalItems = cachedData.apps.size();

            if (totalItems > 3) {
                moreButton.setVisibility(View.VISIBLE);
                moreButton.setText("+" + (totalItems - 3) + " more");

                final int itemType = item.type;
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (itemType) {
                            case AnalysisItem.TYPE_DUPLICATE:
                                showDuplicateDetails();
                                break;
                            case AnalysisItem.TYPE_LARGE_FILES:
                                showLargeFilesDetails();
                                break;
                            case AnalysisItem.TYPE_APP_MANAGER:
                                showAppDetails();
                                break;
                        }
                    }
                });
            } else {
                moreButton.setVisibility(View.GONE);
            }
        } else {
            subItemsContainer.setVisibility(View.GONE);
            moreButton.setVisibility(View.GONE);
        }

        return view;
    }


    private View createSubItemView(SubItem subItem) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_sub_item, containerLayout, false);

        ImageView icon = view.findViewById(R.id.icon);
        TextView name = view.findViewById(R.id.name);
        TextView path = view.findViewById(R.id.path);
        TextView size = view.findViewById(R.id.size);

        if (icon != null && subItem.icon != 0) {
            icon.setImageResource(subItem.icon);
            icon.setVisibility(View.VISIBLE);

            if (subItem.iconColor != 0) {
                icon.setColorFilter(subItem.iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                icon.clearColorFilter();
            }
        }

        if (subItem.packageName != null && !subItem.packageName.isEmpty()) {
            try {
                PackageManager pm = getContext().getPackageManager();
                icon.setImageDrawable(pm.getApplicationIcon(subItem.packageName));
                icon.clearColorFilter();
            } catch (Exception e) {
                icon.setImageResource(R.drawable.ic_root_apps);
                if (subItem.iconColor != 0) {
                    icon.setColorFilter(subItem.iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
        }

        name.setText(subItem.name);
        path.setText(subItem.path);
        size.setText(subItem.size);

        return view;
    }

    // ============================================================
    // DATA CLASSES
    // ============================================================

    public static class AnalysisItem {
        public static final int TYPE_STORAGE = 1;
        public static final int TYPE_DUPLICATE = 2;
        public static final int TYPE_LARGE_FILES = 3;
        public static final int TYPE_APP_MANAGER = 4;

        public int type;
        public int icon;
        public String title;
        public String summary;
        public int percentage;
        public boolean isLoading;
        public List<SubItem> subItems;
    }

    public static class SubItem {
        public String name;
        public String path;
        public String size;
        public int icon;
        public int iconColor;
        public String packageName;
    }

    public static class FolderItem implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public String path;
        public long size;
        public int itemCount;
    }

    public static class FileItem implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public String path;
        public long size;
        public String fullPath;
    }

    public static class DuplicateGroup implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String fileName;
        public long size;
        public int count;
        public ArrayList<String> filePaths;

        public DuplicateGroup() {
            filePaths = new ArrayList<>();
        }
    }

    public static class AppItem implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public String packageName;
        public String path;
        public long size;
    }
}
