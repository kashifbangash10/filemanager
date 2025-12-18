package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisDetailFragment extends Fragment {
    public static final String TAG = "AnalysisDetailFragment";

    private static final String ARG_TITLE = "title";
    private static final String ARG_FOLDERS = "folders";
    private static final String ARG_DUPLICATES = "duplicates";
    private static final String ARG_LARGE_FILES = "large_files";
    private static final String ARG_APPS = "apps";

    private RecyclerView recyclerView;
    private String title;
    private DetailAdapter adapter;

    private View bottomBar;
    private Button cleanupButton;
    private TextView summaryText;
    private ImageView selectAllButton;

    private Stack<File> folderStack = new Stack<>();
    private boolean isInFolderNavigation = false;

    private ExecutorService executor;
    private Handler mainHandler;

    public static void show(FragmentManager fm, String title,
                            List<AnalysisFragment.FolderItem> folders,
                            List<AnalysisFragment.DuplicateGroup> duplicates,
                            List<AnalysisFragment.FileItem> largeFiles,
                            List<AnalysisFragment.AppItem> apps) {

        final AnalysisDetailFragment fragment = new AnalysisDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);

        if (folders != null) args.putSerializable(ARG_FOLDERS, (Serializable) folders);
        if (duplicates != null) args.putSerializable(ARG_DUPLICATES, (Serializable) duplicates);
        if (largeFiles != null) args.putSerializable(ARG_LARGE_FILES, (Serializable) largeFiles);
        if (apps != null) args.putSerializable(ARG_APPS, (Serializable) apps);

        fragment.setArguments(args);

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analysis_detail, container, false);

        recyclerView = view.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        executor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());

        try {
            bottomBar = view.findViewById(R.id.bottom_bar);
            cleanupButton = view.findViewById(R.id.btn_cleanup);
            summaryText = view.findViewById(R.id.summary_text);
            selectAllButton = view.findViewById(R.id.btn_select_all);

            if (cleanupButton != null) {
                cleanupButton.setOnClickListener(v -> performCleanup());
            }
            if (selectAllButton != null) {
                selectAllButton.setOnClickListener(v -> toggleSelectAll());
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Bottom bar views not found");
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.getAction() == android.view.KeyEvent.ACTION_UP) {
                return handleBackPressed();
            }
            return false;
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            updateToolbarTitle();
            loadData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void updateToolbarTitle() {
        try {
            if (getActivity() instanceof BaseActivity) {
                BaseActivity activity = (BaseActivity) getActivity();
                activity.setTitle(title);
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle(title);
                }
            }
        } catch (Exception e) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        updateToolbarTitle();
    }

    private boolean handleBackPressed() {
        if (isInFolderNavigation && !folderStack.isEmpty()) {
            folderStack.pop();
            if (!folderStack.isEmpty()) {
                openFolderInternally(folderStack.peek());
                return true;
            } else {
                isInFolderNavigation = false;
                loadData();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Bundle args = getArguments();
        if (args == null) return;

        isInFolderNavigation = false;
        folderStack.clear();

        List<DetailAdapter.DetailItem> items = new ArrayList<>();
        boolean isDuplicateView = false;

        if (args.containsKey(ARG_FOLDERS)) {
            List<AnalysisFragment.FolderItem> folders =
                    (List<AnalysisFragment.FolderItem>) args.getSerializable(ARG_FOLDERS);

            if (folders != null) {
                for (AnalysisFragment.FolderItem folder : folders) {
                    DetailAdapter.DetailItem item = new DetailAdapter.DetailItem();
                    item.name = folder.name;
                    item.path = getShortPath(folder.path);
                    item.subtitle = folder.itemCount + " items";
                    item.size = Formatter.formatFileSize(getContext(), folder.size);
                    item.file = new File(folder.path);
                    item.isFolder = true;
                    item.icon = R.drawable.ic_root_folder;
                    item.iconColor = getFolderIconColor(folder.name);
                    items.add(item);
                }
            }
        }
        else if (args.containsKey(ARG_DUPLICATES)) {
            isDuplicateView = true;
            List<AnalysisFragment.DuplicateGroup> duplicates =
                    (List<AnalysisFragment.DuplicateGroup>) args.getSerializable(ARG_DUPLICATES);

            if (duplicates != null) {
                int totalDuplicates = 0;
                long totalSize = 0;

                for (AnalysisFragment.DuplicateGroup group : duplicates) {
                    totalDuplicates += group.filePaths.size();
                    totalSize += group.size * group.filePaths.size();

                    for (String filePath : group.filePaths) {
                        File file = new File(filePath);
                        DetailAdapter.DetailItem item = new DetailAdapter.DetailItem();
                        item.name = group.fileName;
                        item.path = getShortPath(filePath);
                        item.size = Formatter.formatFileSize(getContext(), group.size);
                        item.subtitle = getFileDate(file);
                        item.file = file;
                        item.isFolder = false;
                        item.isSelectable = true;
                        item.icon = getFileIcon(group.fileName);
                        item.iconColor = 0xFFFFC107;
                        items.add(item);
                    }
                }

                if (bottomBar != null && summaryText != null) {
                    bottomBar.setVisibility(View.VISIBLE);
                    summaryText.setText("Duplicate files: " + totalDuplicates +
                            "  Size: " + Formatter.formatFileSize(getContext(), totalSize));
                }
            }
        }
        else if (args.containsKey(ARG_LARGE_FILES)) {
            List<AnalysisFragment.FileItem> largeFiles =
                    (List<AnalysisFragment.FileItem>) args.getSerializable(ARG_LARGE_FILES);

            if (largeFiles != null) {
                for (AnalysisFragment.FileItem fileInfo : largeFiles) {
                    DetailAdapter.DetailItem item = new DetailAdapter.DetailItem();
                    item.name = fileInfo.name;

                    String fileName = fileInfo.name.toLowerCase();
                    if (fileName.endsWith(".apk")) {
                        item.path = null;
                        item.subtitle = getFileDate(new File(fileInfo.fullPath));
                    } else {
                        item.path = getShortPath(fileInfo.fullPath);
                        item.subtitle = null;
                    }

                    item.size = Formatter.formatFileSize(getContext(), fileInfo.size);
                    item.file = new File(fileInfo.fullPath);
                    item.isFolder = false;
                    item.icon = getFileIcon(fileInfo.name);
                    item.iconColor = 0xFFFF9800;
                    items.add(item);
                }
            }
        }
        else if (args.containsKey(ARG_APPS)) {
            List<AnalysisFragment.AppItem> apps =
                    (List<AnalysisFragment.AppItem>) args.getSerializable(ARG_APPS);

            if (apps != null) {
                for (AnalysisFragment.AppItem app : apps) {
                    DetailAdapter.DetailItem item = new DetailAdapter.DetailItem();
                    item.name = app.name;
                    item.path = app.packageName;
                    item.subtitle = getInstallDate();
                    item.size = Formatter.formatFileSize(getContext(), app.size);
                    item.packageName = app.packageName;
                    item.isApp = true;
                    item.icon = R.drawable.ic_root_apps;
                    item.iconColor = 0;
                    items.add(item);
                }
            }
        }

        adapter = new DetailAdapter(items, isDuplicateView, executor, mainHandler,
                this::handleItemClick, this::handleSelectionChanged);
        recyclerView.setAdapter(adapter);
    }

    private void handleItemClick(DetailAdapter.DetailItem item) {
        if (item.packageName != null) {
            openAppInfo(item.packageName);
        } else if (item.isFolder) {
            openFolderInternally(item.file);
        } else if (item.file != null && !item.isSelectable) {
            openFileDirect(item.file);
        }
    }

    private void handleSelectionChanged(int selectedCount, long totalSize) {
        if (summaryText != null && cleanupButton != null) {
            if (selectedCount > 0) {
                summaryText.setText(selectedCount + " selected");
                cleanupButton.setText("Clean up " + Formatter.formatFileSize(getContext(), totalSize));
                cleanupButton.setEnabled(true);
            } else {
                if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
                cleanupButton.setEnabled(false);
            }
        }
    }

    private void toggleSelectAll() {
        if (adapter != null) adapter.toggleSelectAll();
    }

    private void performCleanup() {
        if (adapter == null) return;

        final List<File> selectedFiles = adapter.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Toast.makeText(getContext(), "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        long totalSize = 0;
        for (File file : selectedFiles) totalSize += file.length();
        final long finalTotalSize = totalSize;

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Files")
                .setMessage("Delete " + selectedFiles.size() + " selected files?\n\n" +
                        "This will free up " + Formatter.formatFileSize(getContext(), finalTotalSize))
                .setPositiveButton("Delete", (dialog, which) -> {
                    executor.execute(() -> {
                        int deletedCount = 0;
                        long freedSpace = 0;

                        for (File file : selectedFiles) {
                            try {
                                long fileSize = file.length();
                                if (file.exists() && file.delete()) {
                                    deletedCount++;
                                    freedSpace += fileSize;
                                }
                            } catch (Exception e) {}
                        }

                        final int finalDeleted = deletedCount;
                        final long finalFreed = freedSpace;

                        mainHandler.post(() -> {
                            Toast.makeText(getContext(),
                                    "Deleted " + finalDeleted + " files\n" +
                                            Formatter.formatFileSize(getContext(), finalFreed) + " freed",
                                    Toast.LENGTH_LONG).show();
                            if (getFragmentManager() != null) {
                                getFragmentManager().popBackStack();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFolderInternally(final File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            Toast.makeText(getContext(), "Folder not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isInFolderNavigation) {
            isInFolderNavigation = true;
            folderStack.clear();
        }

        if (folderStack.isEmpty() || !folderStack.peek().equals(folder)) {
            folderStack.push(folder);
        }

        executor.execute(() -> {
            final List<DetailAdapter.DetailItem> items = new ArrayList<>();
            File[] files = folder.listFiles();

            if (files == null || files.length == 0) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Empty folder", Toast.LENGTH_SHORT).show());
                return;
            }

            for (File file : files) {
                if (file.getName().startsWith(".")) continue;

                DetailAdapter.DetailItem item = new DetailAdapter.DetailItem();
                item.name = file.getName();
                item.path = getShortPath(file.getAbsolutePath());
                item.file = file;

                if (file.isDirectory()) {
                    item.isFolder = true;
                    item.icon = R.drawable.ic_root_folder;
                    item.iconColor = getFolderIconColor(file.getName());
                    item.size = Formatter.formatFileSize(getContext(), getFolderSizeQuick(file));
                    item.subtitle = countFilesQuick(file) + " items";
                } else {
                    item.isFolder = false;
                    item.icon = getFileIcon(file.getName());
                    item.iconColor = 0xFF2196F3;
                    item.size = Formatter.formatFileSize(getContext(), file.length());
                    item.subtitle = getFileDate(file);
                }
                items.add(item);
            }

            if (items.isEmpty()) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Empty folder", Toast.LENGTH_SHORT).show());
                return;
            }

            Collections.sort(items, (a, b) -> {
                if (a.isFolder != b.isFolder) return a.isFolder ? -1 : 1;
                return a.name.compareToIgnoreCase(b.name);
            });

            mainHandler.post(() -> {
                title = folder.getName();
                updateToolbarTitle();
                if (bottomBar != null) bottomBar.setVisibility(View.GONE);

                adapter = new DetailAdapter(items, false, executor, mainHandler,
                        this::handleItemClick, this::handleSelectionChanged);
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void openFileDirect(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(getContext(), "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String mimeType = getMimeType(file);
            Uri uri = getFileUri(file);
            String fileName = file.getName().toLowerCase();

            if (isVideoFile(fileName)) {
                openVideoDirectly(uri, file);
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            PackageManager pm = requireContext().getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String pkgName = resolveInfo.activityInfo.packageName;
                String className = resolveInfo.activityInfo.name;
                intent.setComponent(new ComponentName(pkgName, className));
                startActivity(intent);
            } else {
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(uri, mimeType);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (fallback.resolveActivity(pm) != null) {
                    startActivity(fallback);
                } else {
                    Toast.makeText(getContext(), "No app found to open this file", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to open: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openVideoDirectly(Uri uri, File file) {
        PackageManager pm = requireContext().getPackageManager();

        String[][] videoPlayers = {
                {"com.mxtech.videoplayer", "com.mxtech.videoplayer.ActivityScreen"},
                {"com.mxtech.videoplayer.pro", "com.mxtech.videoplayer.ActivityScreen"},
                {"com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.ActivityScreen"},
                {"org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"},
                {"com.brouken.player", "com.brouken.player.PlayerActivity"},
                {"is.xyz.mpv", "is.xyz.mpv.MPVActivity"},
        };

        for (String[] player : videoPlayers) {
            String packageName = player[0];
            String activityName = player[1];

            if (isAppInstalled(pm, packageName)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");
                    intent.setPackage(packageName);
                    intent.setComponent(new ComponentName(packageName, activityName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra("title", file.getName());

                    startActivity(intent);
                    android.util.Log.d(TAG, "✅ Video opened in: " + packageName);
                    return;
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Failed to open with " + packageName);
                }
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                intent.setComponent(new ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name));
                startActivity(intent);
            } else {
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "No video player found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isAppInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isVideoFile(String fileName) {
        String n = fileName.toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".mkv") ||
                n.endsWith(".mov") || n.endsWith(".wmv") || n.endsWith(".flv") ||
                n.endsWith(".webm") || n.endsWith(".3gp") || n.endsWith(".m4v") ||
                n.endsWith(".ts") || n.endsWith(".mpg") || n.endsWith(".mpeg");
    }

    private Uri getFileUri(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);
        }
        return Uri.fromFile(file);
    }

    private void openAppInfo(String packageName) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open app info", Toast.LENGTH_SHORT).show();
        }
    }

    private String getShortPath(String fullPath) {
        if (fullPath.contains("/storage/emulated/0/")) {
            return fullPath.replace("/storage/emulated/0/", "/");
        }
        return fullPath;
    }

    private String getFileDate(File file) {
        try {
            long lastModified = file.lastModified();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "dd/MM/yyyy hh:mm a", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(lastModified));
        } catch (Exception e) { return ""; }
    }

    private String getInstallDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private int getFileIcon(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                name.endsWith(".tar") || name.endsWith(".gz")) return R.drawable.ic_root_archive;
        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") ||
                name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".3gp"))
            return R.drawable.ic_root_video;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".heic"))
            return R.drawable.ic_root_image;
        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") ||
                name.endsWith(".flac") || name.endsWith(".aac")) return R.drawable.ic_root_audio;
        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") ||
                name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx"))
            return R.drawable.ic_root_document;
        if (name.endsWith(".apk")) return R.drawable.ic_root_apps;
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

    private long getFolderSizeQuick(File folder) {
        long size = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null) {
                int count = 0;
                for (File file : files) {
                    if (count++ > 100) break;
                    if (file.isFile()) size += file.length();
                }
            }
        } catch (Exception e) {}
        return size;
    }

    private int countFilesQuick(File folder) {
        int count = 0;
        try {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) count++;
                    if (count > 100) break;
                }
            }
        } catch (Exception e) {}
        return count;
    }

    private String getMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".avi")) return "video/x-msvideo";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        if (name.endsWith(".mov")) return "video/quicktime";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".3gp")) return "video/3gpp";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".zip")) return "application/zip";
        if (name.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "application/msword";
        if (name.endsWith(".xls") || name.endsWith(".xlsx")) return "application/vnd.ms-excel";
        return "*/*";
    }

    // ADAPTER
    private static class DetailAdapter extends RecyclerView.Adapter<DetailAdapter.ViewHolder> {
        private final List<DetailItem> items;
        private final boolean showCheckboxes;
        private final Set<Integer> selectedPositions = new HashSet<>();
        private final OnItemClickListener clickListener;
        private final OnSelectionChangedListener selectionListener;
        private final ExecutorService executor;
        private final Handler mainHandler;

        interface OnItemClickListener { void onItemClick(DetailItem item); }
        interface OnSelectionChangedListener { void onSelectionChanged(int count, long totalSize); }

        DetailAdapter(List<DetailItem> items, boolean showCheckboxes,
                      ExecutorService executor, Handler mainHandler,
                      OnItemClickListener clickListener,
                      OnSelectionChangedListener selectionListener) {
            this.items = items;
            this.showCheckboxes = showCheckboxes;
            this.executor = executor;
            this.mainHandler = mainHandler;
            this.clickListener = clickListener;
            this.selectionListener = selectionListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_analysis_details, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            final DetailItem item = items.get(position);
            holder.bind(item, showCheckboxes, selectedPositions.contains(position), executor, mainHandler);

            if (!showCheckboxes) {
                holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
            } else {
                if (holder.checkbox != null) {
                    holder.checkbox.setOnCheckedChangeListener(null);
                    holder.checkbox.setOnCheckedChangeListener((btn, isChecked) -> {
                        if (isChecked) selectedPositions.add(position);
                        else selectedPositions.remove(position);
                        updateSelection();
                    });
                }
                holder.itemView.setOnClickListener(v -> {
                    if (holder.checkbox != null) holder.checkbox.toggle();
                });
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        private void updateSelection() {
            long totalSize = 0;
            for (int pos : selectedPositions) {
                if (pos < items.size() && items.get(pos).file != null) {
                    totalSize += items.get(pos).file.length();
                }
            }
            selectionListener.onSelectionChanged(selectedPositions.size(), totalSize);
        }

        void toggleSelectAll() {
            if (selectedPositions.size() == items.size()) selectedPositions.clear();
            else {
                selectedPositions.clear();
                for (int i = 0; i < items.size(); i++) selectedPositions.add(i);
            }
            notifyDataSetChanged();
            updateSelection();
        }

        List<File> getSelectedFiles() {
            List<File> files = new ArrayList<>();
            for (int pos : selectedPositions) {
                if (pos < items.size() && items.get(pos).file != null) {
                    files.add(items.get(pos).file);
                }
            }
            return files;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name, path, size, subtitle;
            CheckBox checkbox;

            ViewHolder(View view) {
                super(view);
                icon = view.findViewById(R.id.icon);
                name = view.findViewById(R.id.name);
                path = view.findViewById(R.id.path);
                size = view.findViewById(R.id.size);
                try { subtitle = view.findViewById(R.id.subtitle); } catch (Exception e) {}
                try { checkbox = view.findViewById(R.id.checkbox); } catch (Exception e) {}
            }

            void bind(final DetailItem item, boolean showCheckbox, boolean isChecked,
                      ExecutorService executor, final Handler mainHandler) {

                if (name != null) name.setText(item.name != null ? item.name : "");
                if (path != null) {
                    path.setText(item.path != null ? item.path : "");
                    path.setVisibility(item.path != null ? View.VISIBLE : View.GONE);
                }
                if (size != null) {
                    size.setText(item.size != null ? item.size : "");
                    size.setVisibility(item.size != null ? View.VISIBLE : View.GONE);
                }
                if (subtitle != null) {
                    subtitle.setText(item.subtitle != null ? item.subtitle : "");
                    subtitle.setVisibility(item.subtitle != null ? View.VISIBLE : View.GONE);
                }

                if (checkbox != null) {
                    checkbox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
                    if (showCheckbox) checkbox.setChecked(isChecked);
                }

                if (icon != null) {
                    if (item.icon != 0) {
                        icon.setImageResource(item.icon);
                        icon.setScaleType(ImageView.ScaleType.CENTER);
                        icon.setVisibility(View.VISIBLE);
                        if (item.iconColor != 0 && !item.isApp) {
                            icon.setColorFilter(item.iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                        }
                    }

                    if (item.file != null && isVideoFile(item.file.getName())) {
                        final String filePath = item.file.getAbsolutePath();
                        executor.execute(() -> {
                            try {
                                final Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
                                        filePath, MediaStore.Video.Thumbnails.MINI_KIND);
                                if (thumbnail != null) {
                                    mainHandler.post(() -> {
                                        icon.setImageBitmap(thumbnail);
                                        icon.clearColorFilter();
                                        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    });
                                }
                            } catch (Exception e) {}
                        });
                    }
                    else if (item.file != null && isImageFile(item.file.getName())) {
                        final String filePath = item.file.getAbsolutePath();
                        executor.execute(() -> {
                            try {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inSampleSize = 4;
                                Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                                if (bitmap != null) {
                                    final Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 120, 120);
                                    mainHandler.post(() -> {
                                        icon.setImageBitmap(thumbnail);
                                        icon.clearColorFilter();
                                        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    });
                                    if (bitmap != thumbnail) bitmap.recycle();
                                }
                            } catch (Exception e) {}
                        });
                    }
                    else if (item.file != null && item.file.getName().toLowerCase().endsWith(".apk")) {
                        final String filePath = item.file.getAbsolutePath();
                        final PackageManager pm = itemView.getContext().getPackageManager();
                        executor.execute(() -> {
                            try {
                                android.content.pm.PackageInfo pkgInfo = pm.getPackageArchiveInfo(
                                        filePath, PackageManager.GET_ACTIVITIES);
                                if (pkgInfo != null) {
                                    pkgInfo.applicationInfo.sourceDir = filePath;
                                    pkgInfo.applicationInfo.publicSourceDir = filePath;
                                    final Drawable apkIcon = pkgInfo.applicationInfo.loadIcon(pm);
                                    mainHandler.post(() -> {
                                        icon.setImageDrawable(apkIcon);
                                        icon.clearColorFilter();
                                        icon.setScaleType(ImageView.ScaleType.CENTER);
                                    });
                                }
                            } catch (Exception e) {}
                        });
                    }
                    else if (item.isApp && item.packageName != null) {
                        final String packageName = item.packageName;
                        final PackageManager pm = itemView.getContext().getPackageManager();
                        executor.execute(() -> {
                            try {
                                final Drawable appIcon = pm.getApplicationIcon(packageName);
                                mainHandler.post(() -> {
                                    icon.setImageDrawable(appIcon);
                                    icon.setVisibility(View.VISIBLE);
                                    icon.clearColorFilter();
                                    icon.setScaleType(ImageView.ScaleType.CENTER);
                                });
                            } catch (Exception e) {}
                        });
                    }
                }
            }

            private boolean isVideoFile(String fileName) {
                String n = fileName.toLowerCase();
                return n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".mkv") ||
                        n.endsWith(".mov") || n.endsWith(".wmv") || n.endsWith(".flv") ||
                        n.endsWith(".webm") || n.endsWith(".3gp") || n.endsWith(".m4v");
            }

            private boolean isImageFile(String fileName) {
                String n = fileName.toLowerCase();
                return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
                        n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp") ||
                        n.endsWith(".heic");
            }
        }

        static class DetailItem {
            String name, path, size, subtitle, packageName;
            boolean isFolder, isSelectable, isApp;
            File file;
            int icon, iconColor;
        }
    }
}
