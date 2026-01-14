package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.AnalysisDetailFragment; // Added import

public class EmptyFragment extends Fragment {
    public static final String TAG = "EmptyFragment";

    public static void show(FragmentManager fm) {
        EmptyFragment fragment = new EmptyFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cleaner_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getActivity() instanceof DocumentsActivity) {
            ((DocumentsActivity) getActivity()).setTitle("Home");
        }
        
        setupUI(view);
    }

    private void setupUI(View view) {
        // Setup Circle Button
        View cleanBtn = view.findViewById(R.id.btn_clean_action);
        cleanBtn.setOnClickListener(v -> {
            // Rotate animation for effect
            RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(1000);
            v.startAnimation(rotate);
            
            // Launch Junk Cleaner
            JunkCleanerFragment.show(getFragmentManager());
        });

        // Setup Option Cards
        setupOption(view.findViewById(R.id.option_storage), "Storage manager", "Free up Internal Storage", 
                R.drawable.ic_cleaner_storage, 0xFFF3E5F5, 0xFF9C27B0); // Purple
                
        setupOption(view.findViewById(R.id.option_duplicate), "Duplicate files", "Free up duplicate files", 
                R.drawable.ic_cleaner_duplicate, 0xFFFFF3E0, 0xFFFF9800); // Orange
                
        setupOption(view.findViewById(R.id.option_large), "Large files", "Free up files larger than 50MB", 
                R.drawable.ic_cleaner_large, 0xFFE3F2FD, 0xFF2196F3); // Blue
                
        setupOption(view.findViewById(R.id.option_apps), "App manager", "Uninstall unnecessary apps", 
                R.drawable.ic_root_apps, 0xFFE8F5E9, 0xFF4CAF50); // Green
    }

    private void setupOption(View optionView, String title, String subtitle, int iconRes, int bgTint, int iconTint) {
        TextView titleView = optionView.findViewById(R.id.text_title);
        TextView subtitleView = optionView.findViewById(R.id.text_subtitle);
        ImageView iconView = optionView.findViewById(R.id.icon_cleaner);

        titleView.setText(title);
        subtitleView.setText(subtitle);
        
        iconView.setImageResource(iconRes);
        iconView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgTint));
        iconView.setImageTintList(android.content.res.ColorStateList.valueOf(iconTint));
        
        optionView.setOnClickListener(v -> {
            // Handle click
            if (title.equals("Storage manager")) {
               CleanerScanningFragment.show(getFragmentManager(), CleanerScanningFragment.TYPE_STORAGE);
            } else if (title.equals("Duplicate files")) {
               CleanerScanningFragment.show(getFragmentManager(), CleanerScanningFragment.TYPE_DUPLICATE);
            } else if (title.equals("Large files")) {
               CleanerScanningFragment.show(getFragmentManager(), CleanerScanningFragment.TYPE_LARGE);
            } else if (title.equals("App manager")) {
               CleanerScanningFragment.show(getFragmentManager(), CleanerScanningFragment.TYPE_APPS);
            }
        });
    }
}
