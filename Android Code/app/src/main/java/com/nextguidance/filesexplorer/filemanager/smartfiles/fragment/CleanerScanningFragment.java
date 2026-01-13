package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.graphics.drawable.DrawableCompat;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.AnalysisDetailFragment;

public class CleanerScanningFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    public static final int TYPE_STORAGE = 0;
    public static final int TYPE_DUPLICATE = 1;
    public static final int TYPE_LARGE = 2;
    public static final int TYPE_APPS = 3;

    private int type;
    private TextView pathView;
    private final Handler handler = new Handler();
    private final String[] fakePaths = {
        "/storage/emulated/0/DCIM/Camera/IMG_20250112.jpg",
        "/storage/emulated/0/Android/data/com.whatsapp/files",
        "/storage/emulated/0/Music/Unknown/song.mp3",
        "/storage/emulated/0/Download/document.pdf",
        "/storage/emulated/0/Pictures/Telegram/image_05.jpg",
        "/storage/emulated/0/Movies/Instagram/video_123.mp4"
    };

    public static void show(FragmentManager fm, int type) {
        CleanerScanningFragment fragment = new CleanerScanningFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        fm.beginTransaction()
            .replace(R.id.container_directory, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cleaner_scanning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) type = getArguments().getInt(ARG_TYPE);
        setupUI(view);
        startScan();
    }

    private void setupUI(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            if (getFragmentManager() != null) getFragmentManager().popBackStack();
        });
        
        if(toolbar.getNavigationIcon() != null) {
            DrawableCompat.setTint(toolbar.getNavigationIcon(), 0xFFFFFFFF);
        }

        TextView titleView = view.findViewById(R.id.text_title);
        TextView statusView = view.findViewById(R.id.text_status);
        ImageView iconView = view.findViewById(R.id.icon_scan_type);
        pathView = view.findViewById(R.id.text_path);

        View ringOuter = view.findViewById(R.id.ring_outer);
        View ringInner = view.findViewById(R.id.ring_inner);

        ScaleAnimation pulse = new ScaleAnimation(1f, 1.2f, 1f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pulse.setDuration(1000);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        ringOuter.startAnimation(pulse);

        switch (type) {
            case TYPE_STORAGE:
                titleView.setText("Storage manager");
                statusView.setText("Scanning storage files");
                iconView.setImageResource(R.drawable.ic_cleaner_storage);
                break;
            case TYPE_DUPLICATE:
                titleView.setText("Duplicate files");
                statusView.setText("Scanning duplicate files");
                iconView.setImageResource(R.drawable.ic_cleaner_duplicate);
                break;
            case TYPE_LARGE:
                titleView.setText("Large files");
                statusView.setText("Scanning large files");
                iconView.setImageResource(R.drawable.ic_cleaner_large);
                break;
            case TYPE_APPS:
                titleView.setText("App manager");
                statusView.setText("Scanning apps");
                iconView.setImageResource(R.drawable.ic_root_apps);
                break;
        }
        iconView.setColorFilter(0xFFFFFFFF);
    }

    private void startScan() {
        Runnable updatePath = new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (!isAdded()) return;
                pathView.setText(fakePaths[i % fakePaths.length]);
                i++;
                handler.postDelayed(this, 150);
            }
        };
        handler.post(updatePath);

        handler.postDelayed(() -> {
            if (!isAdded()) return;
            openResult();
        }, 2200);
    }

    private void openResult() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
            
            if (type == TYPE_STORAGE) {
                AnalysisDetailFragment.showInternalStorage(getFragmentManager());
            } else {
                 // Reuse AnalysisDetail or other fragment
                 AnalysisDetailFragment.showInternalStorage(getFragmentManager()); 
            }
        }
    }
}
