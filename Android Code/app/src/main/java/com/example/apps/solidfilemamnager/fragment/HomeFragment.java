/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.apps.solidfilemamnager.fragment;

import static android.content.Context.RECEIVER_EXPORTED;
import static com.example.apps.solidfilemamnager.BaseActivity.State.MODE_GRID;
import static com.example.apps.solidfilemamnager.adapter.HomeAdapter.TYPE_MAIN;
import static com.example.apps.solidfilemamnager.adapter.HomeAdapter.TYPE_RECENT;
import static com.example.apps.solidfilemamnager.adapter.HomeAdapter.TYPE_SHORTCUT;
import static com.example.apps.solidfilemamnager.provider.AppsProvider.getRunningAppProcessInfo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apps.solidfilemamnager.BaseActivity;
import com.example.apps.solidfilemamnager.DocumentsActivity;
import com.example.apps.solidfilemamnager.DocumentsApplication;
import com.example.apps.solidfilemamnager.R;
import com.example.apps.solidfilemamnager.adapter.CommonInfo;
import com.example.apps.solidfilemamnager.adapter.HomeAdapter;
import com.example.apps.solidfilemamnager.common.DialogBuilder;
import com.example.apps.solidfilemamnager.common.RecyclerFragment;
import com.example.apps.solidfilemamnager.cursor.LimitCursorWrapper;
import com.example.apps.solidfilemamnager.loader.RecentLoader;
import com.example.apps.solidfilemamnager.misc.AsyncTask;
import com.example.apps.solidfilemamnager.misc.IconHelper;
import com.example.apps.solidfilemamnager.misc.IconUtils;
import com.example.apps.solidfilemamnager.misc.RootsCache;
import com.example.apps.solidfilemamnager.misc.Utils;
import com.example.apps.solidfilemamnager.model.DirectoryResult;
import com.example.apps.solidfilemamnager.model.DocumentInfo;
import com.example.apps.solidfilemamnager.model.RootInfo;
import com.example.apps.solidfilemamnager.provider.AppsProvider;
import com.example.apps.solidfilemamnager.setting.SettingsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.example.apps.solidfilemamnager.provider.MediaDocumentsProvider;
import com.example.apps.solidfilemamnager.provider.ExtraDocumentsProvider;
import com.example.apps.solidfilemamnager.provider.ExternalStorageProvider;
import com.example.apps.solidfilemamnager.provider.NonMediaDocumentsProvider;



/**
 * Display home.
 */
public class HomeFragment extends RecyclerFragment implements HomeAdapter.OnItemClickListener {
    public static final String TAG = "HomeFragment";
    public static final String ROOTS_CHANGED = "android.intent.action.ROOTS_CHANGED";
    private static final int MAX_RECENT_COUNT = 10;

    private final int mLoaderId = 42;
    private RootsCache roots;
    private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;
    private RootInfo mHomeRoot;
    private BaseActivity mActivity;
    private IconHelper mIconHelper;
    private ArrayList<CommonInfo> mainData;
    private ArrayList<CommonInfo> shortcutsData;
    private HomeAdapter mAdapter;
    private RootInfo processRoot;

    // Storage Cards ke liye Views
    private CardView internalStorageCard, sdCardCard;
    private TextView internalStorageTitle, internalStorageInfo, internalStoragePercentage;
    private TextView sdCardTitle, sdCardInfo, sdCardPercentage;
    private ProgressBar internalStorageProgress, sdCardProgress;

    public static void show(FragmentManager fm) {
        final HomeFragment fragment = new HomeFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static HomeFragment get(FragmentManager fm) {
        return (HomeFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        RecyclerView recyclerView = getListView();
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        // Storage Cards ke Views ko initialize karein
        internalStorageCard = view.findViewById(R.id.internal_storage_card);
        internalStorageTitle = view.findViewById(R.id.internal_storage_title);
        internalStorageInfo = view.findViewById(R.id.internal_storage_info);
        internalStoragePercentage = view.findViewById(R.id.internal_storage_percentage);
        internalStorageProgress = view.findViewById(R.id.internal_storage_progress);

        sdCardCard = view.findViewById(R.id.sd_card_card);
        sdCardTitle = view.findViewById(R.id.sd_card_title);
        sdCardInfo = view.findViewById(R.id.sd_card_info);
        sdCardPercentage = view.findViewById(R.id.sd_card_percentage);
        sdCardProgress = view.findViewById(R.id.sd_card_progress);

        mActivity = ((BaseActivity) getActivity());
        mIconHelper = new IconHelper(mActivity, MODE_GRID);
        ArrayList<CommonInfo> data = new ArrayList<>();
        if (null == mAdapter) {
            mAdapter = new HomeAdapter(getActivity(), data, mIconHelper);
            mAdapter.setOnItemClickListener(this);
        }
        setListShown(true);

        // Click Listeners for Storage Cards
        internalStorageCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (roots == null) roots = DocumentsApplication.getRootsCache(getActivity());
                RootInfo primaryRoot = roots.getPrimaryRoot();
                if (primaryRoot != null && mActivity instanceof DocumentsActivity) {
                    ((DocumentsActivity) mActivity).onRootPicked(primaryRoot, mHomeRoot);
                }
            }
        });

        sdCardCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (roots == null) roots = DocumentsApplication.getRootsCache(getActivity());
                RootInfo secondaryRoot = roots.getSecondaryRoot();
                if (secondaryRoot != null && mActivity instanceof DocumentsActivity) {
                    ((DocumentsActivity) mActivity).onRootPicked(secondaryRoot, mHomeRoot);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showData();
        registerReceiver();
        if (getActivity() != null) {
            getActivity().setTitle("Home");
        }
    }

    @Override
    public void onPause() {
        unRegisterReceiver();
        super.onPause();
    }

    private void getMainData() {
        mHomeRoot = roots.getHomeRoot();
        mainData = new ArrayList<>();

        // Internal Storage
        final RootInfo primaryRoot = roots.getPrimaryRoot();
        if (primaryRoot != null) {
            internalStorageCard.setVisibility(View.VISIBLE);
            long totalBytes = primaryRoot.totalBytes;
            if (totalBytes > 0) {
                long availableBytes = primaryRoot.availableBytes;
                long usedBytes = totalBytes - availableBytes;
                int progress = (int) ((usedBytes * 100) / totalBytes);

                internalStorageTitle.setText("Internal Storage");
                internalStorageInfo.setText(
                        Formatter.formatFileSize(getActivity(), usedBytes) + " / " + 
                        Formatter.formatFileSize(getActivity(), totalBytes));
                internalStorageProgress.setProgress(progress);
                internalStoragePercentage.setText(progress + "%");
            }
        } else {
            internalStorageCard.setVisibility(View.GONE);
        }

        // SD Card
        final RootInfo secondaryRoot = roots.getSecondaryRoot();
        if (secondaryRoot != null) {
            sdCardCard.setVisibility(View.VISIBLE);
            long totalBytes = secondaryRoot.totalBytes;
            if (totalBytes > 0) {
                long availableBytes = secondaryRoot.availableBytes;
                long usedBytes = totalBytes - availableBytes;
                int progress = (int) ((usedBytes * 100) / totalBytes);

                sdCardTitle.setText(secondaryRoot.title);
                sdCardInfo.setText(
                        Formatter.formatFileSize(getActivity(), usedBytes) + " / " + 
                        Formatter.formatFileSize(getActivity(), totalBytes));
                sdCardProgress.setProgress(progress);
                sdCardPercentage.setText(progress + "%");
            }
        } else {
            sdCardCard.setVisibility(View.GONE);
        }
    }

    private void getRecentsData() {
        final BaseActivity.State state = getDisplayState(this);
        mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {

            @Override
            public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
                return new RecentLoader(getActivity(), roots, state);
            }

            @Override
            public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
                if (!isAdded())
                    return;
                if (null != result.cursor && result.cursor.getCount() != 0) {
                    mAdapter.setRecentData(new LimitCursorWrapper(result.cursor, MAX_RECENT_COUNT));
                }
                setListShown(true);
            }

            @Override
            public void onLoaderReset(Loader<DirectoryResult> loader) {
                mAdapter.setRecentData(null);
                setListShown(true);
            }
        };
        LoaderManager.getInstance(getActivity()).restartLoader(mLoaderId, null, mCallbacks);
    }

    public void reloadData() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showData();
            }
        }, 500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(HomeAdapter.ViewHolder item, View view, int position) {
        switch (item.commonInfo.type) {
            case TYPE_MAIN:
            case TYPE_SHORTCUT:
                if (null == item.commonInfo.rootInfo) {
                    return;
                }
                
                if (item.commonInfo.rootInfo.rootId.equals("clean")) {
                    cleanRAM();
                } else {
                    // Proper root ko open karein
                    DocumentsActivity activity = ((DocumentsActivity) getActivity());
                    if (activity != null) {
                        RootInfo rootToOpen = item.commonInfo.rootInfo;
                        activity.onRootPicked(rootToOpen, mHomeRoot);
                        
                        // Analytics (optional)
                        Bundle params = new Bundle();
                        params.putString("root_id", rootToOpen.rootId);
                        params.putString("root_title", rootToOpen.title);
                    }
                }
                break;
                
            case TYPE_RECENT:
                try {
                    final DocumentInfo documentInfo = ((HomeAdapter.GalleryViewHolder) item).getItem(position);
                    openDocument(documentInfo);
                } catch (Exception ignore) {
                }
                break;
        }
    }

    @Override
    public void onItemLongClick(HomeAdapter.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemViewClick(HomeAdapter.ViewHolder item, View view, int position) {
        switch (view.getId()) {
            case R.id.recents:
                DocumentsActivity activity = ((DocumentsActivity) getActivity());
                if (activity != null) {
                    activity.onRootPicked(roots.getRecentsRoot(), mHomeRoot);
                }
                break;

            case R.id.action:
                Bundle params = new Bundle();
                if (item.commonInfo.rootInfo.isAppProcess()) {
                    cleanRAM();
                } else {
                    Intent intent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
                    if (Utils.isIntentAvailable(getActivity(), intent)) {
                        getActivity().startActivity(intent);
                    } else {
                        Utils.showSnackBar(getActivity(), "Coming Soon!");
                    }
                }
                break;
        }
    }

    private void cleanRAM() {
        Bundle params = new Bundle();
        new OperationTask(processRoot).execute();
    }

    private class OperationTask extends AsyncTask<Void, Void, Boolean> {

        private Dialog progressDialog;
        private RootInfo root;
        private long currentAvailableBytes;

        public OperationTask(RootInfo root) {
            DialogBuilder builder = new DialogBuilder(getActivity());
            builder.setMessage("Cleaning up RAM...");
            builder.setIndeterminate(true);
            progressDialog = builder.create();
            this.root = root;
            currentAvailableBytes = root.availableBytes;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            cleanupMemory(getActivity());
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Utils.isActivityAlive(getActivity())) {
                return;
            }
            AppsProvider.notifyDocumentsChanged(getActivity(), root.rootId);
            AppsProvider.notifyRootsChanged(getActivity());
            RootsCache.updateRoots(getActivity(), AppsProvider.AUTHORITY);
            roots = DocumentsApplication.getRootsCache(getActivity());
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentAvailableBytes != 0) {
                        long availableBytes = processRoot.availableBytes - currentAvailableBytes;
                        String summaryText = availableBytes <= 0 ? "Already cleaned up!" :
                                getActivity().getString(R.string.root_available_bytes,
                                        Formatter.formatFileSize(getActivity(), availableBytes));
                        Utils.showSnackBar(getActivity(), summaryText);
                    }
                    progressDialog.dismiss();
                }
            }, 500);
        }
    }

    private static BaseActivity.State getDisplayState(Fragment fragment) {
        return ((BaseActivity) fragment.getActivity()).getDisplayState();
    }

    public void cleanupMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcessesList = getRunningAppProcessInfo(context);
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcessesList) {
            try {
                activityManager.killBackgroundProcesses(processInfo.processName);
            } catch (Exception e) {
            }
        }
    }

    private void openDocument(DocumentInfo doc) {
        ((BaseActivity) getActivity()).onDocumentPicked(doc);
        Bundle params = new Bundle();
        String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mAdapter);

        final GridLayoutManager layoutManager = (GridLayoutManager) getListView().getLayoutManager();
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case HomeAdapter.TYPE_SHORTCUT:
                        return 1; // Har shortcut item 1 column lega

                    case HomeAdapter.TYPE_MAIN:
                    case HomeAdapter.TYPE_RECENT:
                    default:
                        return 3; // Baaki sab poori width lenge
                }
            }
        });
    }

    @SuppressLint("NewApi")
    private void registerReceiver() {
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(ROOTS_CHANGED), RECEIVER_EXPORTED);
    }

    private void unRegisterReceiver() {
        if (null != broadcastReceiver) {
            getActivity().unregisterReceiver(broadcastReceiver);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showData();
        }
    };

    /**
     * Background task jo har category ka size calculate karega
     */
    @SuppressLint("StaticFieldLeak")
    private class LoadCategorySizesTask extends AsyncTask<Void, Void, ArrayList<CommonInfo>> {

        private final Context mContext;

        LoadCategorySizesTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setListShown(false);
        }

        @Override
        protected ArrayList<CommonInfo> doInBackground(Void... voids) {
            ArrayList<CommonInfo> calculatedShortcuts = new ArrayList<>();
            if (mContext == null || roots == null) return calculatedShortcuts;

            try {
                // 1. Analysis (Internal Storage)
                // 1. Analysis (Internal Storage)
                RootInfo internalRoot = roots.getPrimaryRoot();
                if (internalRoot != null) {
                    RootInfo analysisRoot = copyRootInfo(internalRoot);
                    analysisRoot.title = "Analysis";
                    calculatedShortcuts.add(CommonInfo.from(analysisRoot, TYPE_SHORTCUT));
                }

                // 2. Downloads
                RootInfo downloadsRoot = roots.getDownloadRoot();
                if (downloadsRoot != null) {
                    downloadsRoot = copyRootInfo(downloadsRoot);
                    downloadsRoot.title = "Downloads";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        long size = getCategorySize(MediaStore.Downloads.EXTERNAL_CONTENT_URI);
                        downloadsRoot.totalBytes = size;
                    } else {
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        if (downloadsDir.exists()) {
                            downloadsRoot.totalBytes = getFolderSize(downloadsDir);
                        }
                    }
                    calculatedShortcuts.add(CommonInfo.from(downloadsRoot, TYPE_SHORTCUT));
                }

                // 3. Videos
                RootInfo videosRoot = roots.getRootInfo("videos_root", MediaDocumentsProvider.AUTHORITY);
                if (videosRoot != null) {
                    videosRoot = copyRootInfo(videosRoot);
                    videosRoot.title = "Video"; // Force Singular
                    long size = getCategorySize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    videosRoot.totalBytes = size;
                    calculatedShortcuts.add(CommonInfo.from(videosRoot, TYPE_SHORTCUT));
                }

                // 4. Audio
                RootInfo audioRoot = roots.getRootInfo("audio_root", MediaDocumentsProvider.AUTHORITY);
                if (audioRoot != null) {
                    audioRoot = copyRootInfo(audioRoot);
                    audioRoot.title = "Audio";
                    long size = getCategorySize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    audioRoot.totalBytes = size;
                    calculatedShortcuts.add(CommonInfo.from(audioRoot, TYPE_SHORTCUT));
                }

                // 5. Images
                RootInfo imagesRoot = roots.getRootInfo("images_root", MediaDocumentsProvider.AUTHORITY);
                if (imagesRoot != null) {
                    imagesRoot = copyRootInfo(imagesRoot);
                    imagesRoot.title = "Images";
                    long size = getCategorySize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    imagesRoot.totalBytes = size;
                    calculatedShortcuts.add(CommonInfo.from(imagesRoot, TYPE_SHORTCUT));
                }

                // 6. Apps
                RootInfo appsRoot = roots.getAppRoot();
                if (appsRoot != null) {
                    appsRoot = copyRootInfo(appsRoot);
                    appsRoot.title = "Apps";
                    long size = getAppsSize();
                    appsRoot.totalBytes = size;
                    calculatedShortcuts.add(CommonInfo.from(appsRoot, TYPE_SHORTCUT));
                }

                // 7. Documents
                RootInfo documentsRoot = roots.getRootInfo("document_root", NonMediaDocumentsProvider.AUTHORITY);
                if (documentsRoot != null) {
                    documentsRoot = copyRootInfo(documentsRoot);
                    documentsRoot.title = "Documents";
                    long size = getDocumentsSize();
                    documentsRoot.totalBytes = size;
                    calculatedShortcuts.add(CommonInfo.from(documentsRoot, TYPE_SHORTCUT));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return calculatedShortcuts;
        }

        @Override
        protected void onPostExecute(ArrayList<CommonInfo> result) {
            super.onPostExecute(result);
            if (!Utils.isActivityAlive(getActivity())) return;

            shortcutsData = result;

            // Adapter ko data dein
            ArrayList<CommonInfo> data = new ArrayList<>();
            // data.addAll(mainData);
            data.addAll(shortcutsData);
            
            mAdapter.setData(data);

            // Recent media load karein
            if (SettingsActivity.getDisplayRecentMedia()) {
                getRecentsData();
            } else {
                mAdapter.setRecentData(null);
                setListShown(true);
            }
        }

        /**
         * MediaStore se category ka size calculate karta hai
         */
        private long getCategorySize(Uri uri) {
            long totalSize = 0;
            String[] projection = {MediaStore.MediaColumns.SIZE};
            Cursor cursor = null;
            
            try {
                cursor = mContext.getContentResolver().query(
                    uri, 
                    projection, 
                    null, 
                    null, 
                    null
                );
                
                if (cursor != null) {
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                    while (cursor.moveToNext()) {
                        long size = cursor.getLong(sizeColumn);
                        if (size > 0) {
                            totalSize += size;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            
            return totalSize;
        }

        /**
         * Documents ka size calculate karta hai
         */
        /**
         * Documents ka size calculate karta hai
         */
        private long getDocumentsSize() {
            long totalSize = 0;
            
            // Comprehensive list matching NonMediaDocumentsProvider + common types
            String[] documentMimeTypes = {
                "application/pdf",
                "application/epub+zip",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",      // xlsx
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", // pptx
                "application/vnd.openxmlformats-officedocument.presentationml.template",
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.text-master",
                "application/vnd.oasis.opendocument.text-template",
                "application/vnd.oasis.opendocument.text-web",
                "application/vnd.stardivision.writer",
                "application/vnd.stardivision.writer-global",
                "application/vnd.sun.xml.writer",
                "application/vnd.sun.xml.writer.global",
                "application/vnd.sun.xml.writer.template",
                "application/x-abiword",
                "application/x-kword",
                "text/plain",
                "text/html",
                "text/xml",
                "application/rtf"
            };
            
            try {
                Uri uri = MediaStore.Files.getContentUri("external");
                String[] projection = {MediaStore.Files.FileColumns.SIZE};
                
                // Build selection string for IN clause
                StringBuilder selection = new StringBuilder();
                selection.append(MediaStore.Files.FileColumns.MIME_TYPE + " IN (");
                for (int i = 0; i < documentMimeTypes.length; i++) {
                    selection.append("?");
                    if (i < documentMimeTypes.length - 1) {
                        selection.append(",");
                    }
                }
                selection.append(")");

                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(
                        uri,
                        projection,
                        selection.toString(),
                        documentMimeTypes,
                        null
                    );
                    
                    if (cursor != null) {
                        int sizeColumn = cursor.getColumnIndexOrThrow(
                                MediaStore.Files.FileColumns.SIZE);
                        while (cursor.moveToNext()) {
                            long size = cursor.getLong(sizeColumn);
                            if (size > 0) {
                                totalSize += size;
                            }
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return totalSize;
        }

        private long getAppsSize() {
             long totalSize = 0;
             android.content.pm.PackageManager pm = mContext.getPackageManager();
             java.util.List<android.content.pm.ApplicationInfo> installedApplications = pm.getInstalledApplications(0);
             
             for (android.content.pm.ApplicationInfo appInfo : installedApplications) {
                 // Include only User Apps and Updated System Apps
                 if ((appInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 ||
                     (appInfo.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                     File file = new File(appInfo.sourceDir);
                     totalSize += file.length();
                     
                     // Also check for split APKs if available (Android 5.0+)
                     if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                         if (appInfo.splitSourceDirs != null) {
                             for (String splitPath : appInfo.splitSourceDirs) {
                                  File splitFile = new File(splitPath);
                                  totalSize += splitFile.length();
                             }
                         }
                     }
                 }
             }
             return totalSize;
        }

        /**
         * Folder ka size recursively calculate karta hai
         */
        private long getFolderSize(File directory) {
            long size = 0;
            
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            size += file.length();
                        } else if (file.isDirectory()) {
                            size += getFolderSize(file);
                        }
                    }
                }
            }
            
            return size;
        }
    }

    public void showData() {
        if (!Utils.isActivityAlive(getActivity())) {
            return;
        }
        roots = DocumentsApplication.getRootsCache(getActivity());
        if (null == roots) {
            return;
        }
        mIconHelper.setThumbnailsEnabled(mActivity.getDisplayState().showThumbnail);

        // Storage Cards ka data set karein
        getMainData();

        // Background task shuru karein jo sizes calculate karega
        new LoadCategorySizesTask(getActivity()).execute();
    }
    private static RootInfo copyRootInfo(RootInfo root) {
        android.os.Parcel parcel = android.os.Parcel.obtain();
        root.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        RootInfo copy = RootInfo.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return copy;
    }
}