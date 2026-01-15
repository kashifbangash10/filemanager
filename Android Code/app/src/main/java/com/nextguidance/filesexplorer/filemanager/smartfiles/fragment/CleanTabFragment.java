package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.JunkCleaningActivity;

public class CleanTabFragment extends Fragment {
    public static final String TAG = "CleanTabFragment";
    private static final int REQUEST_CODE_CLEAN = 1001;

    public static void show(FragmentManager fm) {
        CleanTabFragment fragment = new CleanTabFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_empty_clean, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof DocumentsActivity) {
            DocumentsActivity activity = (DocumentsActivity) getActivity();
            activity.setAnalysisMode(true, "");
            if (activity.getToolbar() != null) {
                activity.getToolbar().setVisibility(View.GONE);
            }
        }

        startPremiumAnimations(view);

        view.findViewById(R.id.cleanButton).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), JunkCleaningActivity.class);
            startActivityForResult(intent, REQUEST_CODE_CLEAN);
        });

        // Optional: show state if cleaned, but keep button working
        boolean isCleaned = getContext().getSharedPreferences("junk_prefs", Activity.MODE_PRIVATE)
                .getBoolean("is_cleaned", false);
        long lastCleanTime = getContext().getSharedPreferences("junk_prefs", Activity.MODE_PRIVATE)
                .getLong("last_clean_time", 0);

        if (isCleaned && (System.currentTimeMillis() - lastCleanTime) < 2 * 60 * 60 * 1000) {
            showCleanedState(view);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CLEAN && resultCode == Activity.RESULT_OK) {
            if (getView() != null) {
                showCleanedState(getView());
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof DocumentsActivity) {
            DocumentsActivity activity = (DocumentsActivity) getActivity();
            if (activity.getToolbar() != null) {
                activity.getToolbar().setVisibility(View.VISIBLE);
            }
        }
    }

    private void startPremiumAnimations(View view) {
        // 1. Floating Orbs Animation
        animateOrb(view.findViewById(R.id.orb1), 100f, 150f, 6000);
        animateOrb(view.findViewById(R.id.orb2), -120f, -100f, 8000);
        animateOrb(view.findViewById(R.id.orb3), 80f, -200f, 7000);

        // 2. Pulse Glows
        animatePulse(view.findViewById(R.id.pulseGlow1), 1.0f, 1.25f, 2000);
        animatePulse(view.findViewById(R.id.pulseGlow2), 1.0f, 1.35f, 2500);
        animatePulse(view.findViewById(R.id.pulseGlow3), 1.0f, 1.45f, 3000);

        // 3. Shimmer Effect on Button
        View shimmer = view.findViewById(R.id.shimmerView);
        Animation shimmerAnim = new TranslateAnimation(
            Animation.ABSOLUTE, -100f,
            Animation.ABSOLUTE, 400f,
            Animation.ABSOLUTE, 0f,
            Animation.ABSOLUTE, 0f
        );
        shimmerAnim.setDuration(2000);
        shimmerAnim.setRepeatCount(Animation.INFINITE);
        shimmer.startAnimation(shimmerAnim);
        
        // 4. Title Glow Pulse
        TextView titleText = view.findViewById(R.id.titleText);
        android.animation.ObjectAnimator titleGlow = android.animation.ObjectAnimator.ofFloat(titleText, "alpha", 0.7f, 1.0f);
        titleGlow.setDuration(1500);
        titleGlow.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        titleGlow.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        titleGlow.start();
    }

    private void animateOrb(View orb, float tx, float ty, int duration) {
        android.animation.ObjectAnimator x = android.animation.ObjectAnimator.ofFloat(orb, "translationX", 0f, tx);
        android.animation.ObjectAnimator y = android.animation.ObjectAnimator.ofFloat(orb, "translationY", 0f, ty);
        x.setDuration(duration);
        y.setDuration(duration);
        x.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        x.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        y.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        y.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        x.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        y.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        x.start();
        y.start();
    }

    private void animatePulse(View glow, float start, float end, int duration) {
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(glow, "scaleX", start, end);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(glow, "scaleY", start, end);
        android.animation.ObjectAnimator alpha = android.animation.ObjectAnimator.ofFloat(glow, "alpha", glow.getAlpha(), glow.getAlpha() * 0.5f);
        
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);
        
        scaleX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        alpha.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        alpha.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        
        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void showCleanedState(View view) {
        TextView scanDetail = view.findViewById(R.id.scanDetailText);
        scanDetail.setText("All junk files removed successfully.");
    }
}
