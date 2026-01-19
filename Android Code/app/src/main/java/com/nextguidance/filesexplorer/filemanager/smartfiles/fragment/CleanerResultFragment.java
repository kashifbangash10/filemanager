package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CleanerResultFragment extends Fragment {

    private static final String ARG_PATHS = "paths";
    private static final String ARG_SIZE = "size";
    private static final String ARG_TITLE = "title";

    private List<String> pathsToDelete;
    private String sizeDisplay;
    private String title;

    private ImageView iconStatus;
    private ProgressBar progressLoading;
    private TextView textStatus;
    private TextView textSubStatus;
    private ImageView btnBack;
    private View ivSparkles;
    private View rippleContainer;
    private ObjectAnimator pulseAnimator;
    private InterstitialAd mInterstitialAd;
    private int adRetryCount = 0;

    public static void show(FragmentManager fm, ArrayList<String> paths, String sizeDisplay, String title) {
        CleanerResultFragment fragment = new CleanerResultFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PATHS, paths);
        args.putString(ARG_SIZE, sizeDisplay);
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        
        fm.beginTransaction()
            .replace(R.id.container_directory, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cleaner_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            pathsToDelete = getArguments().getStringArrayList(ARG_PATHS);
            sizeDisplay = getArguments().getString(ARG_SIZE);
            title = getArguments().getString(ARG_TITLE, "Storage manager");
        }

        iconStatus = view.findViewById(R.id.icon_status);
        progressLoading = view.findViewById(R.id.progress_loading);
        textStatus = view.findViewById(R.id.text_status);
        textSubStatus = view.findViewById(R.id.text_sub_status);
        btnBack = view.findViewById(R.id.iv_back);
        ivSparkles = view.findViewById(R.id.iv_sparkles);
        rippleContainer = view.findViewById(R.id.ripple_container);

        TextView titleView = view.findViewById(R.id.text_title);
        if (titleView != null && title != null) titleView.setText(title);

        btnBack.setOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        // Hide bottom nav
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        // Hide Activity toolbar
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) activityToolbar.setVisibility(View.GONE);

        startPulseAnimation();
        loadInterstitialAd();
        startCleaning();
    }

    private void startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                rippleContainer,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.15f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.15f),
                PropertyValuesHolder.ofFloat("alpha", 0.7f, 1.0f)
        );
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        // Restore bottom nav
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        // Restore Activity toolbar
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) activityToolbar.setVisibility(View.VISIBLE);
    }

    private void startCleaning() {
        textStatus.setText("Cleaning...");
        textSubStatus.setText("Optimizing storage space");
        progressLoading.setVisibility(View.VISIBLE);
        iconStatus.setVisibility(View.VISIBLE);
        iconStatus.setImageResource(R.drawable.ic_cleaner_broom);
        ivSparkles.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (pathsToDelete != null) {
                for (String path : pathsToDelete) {
                    try {
                        File file = new File(path);
                        deleteRecursive(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                showInterstitialThenSuccess();
            });
        });
    }

    private void loadInterstitialAd() {
        if (getContext() == null) {
            Log.e("CleanerResult", "Context is null, cannot load ad");
            return;
        }
        Log.d("CleanerResult", "Starting to load interstitial ad");
        // Toast.makeText(getContext(), "Loading Ad...", Toast.LENGTH_SHORT).show(); // Removed for production feel but kept in mind
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(getContext(), getString(R.string.admob_interstitial_junk), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d("CleanerResult", "Ad loaded successfully");
                        mInterstitialAd = interstitialAd;
                        // Toast.makeText(getContext(), "Ad Loaded", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e("CleanerResult", "Ad failed to load: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                        // Toast.makeText(getContext(), "Ad Failed: " + loadAdError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showInterstitialThenSuccess() {
        if (!isAdded()) return;
        
        InterstitialAd activityAd = null;
        if (getActivity() instanceof DocumentsActivity) {
            activityAd = ((DocumentsActivity) getActivity()).getHomeInterstitialAd();
        }

        final InterstitialAd adToShow = (activityAd != null) ? activityAd : mInterstitialAd;

        if (adToShow != null) {
            Log.d("CleanerResult", "Showing interstitial ad");
            adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d("CleanerResult", "Ad dismissed");
                    if (getActivity() instanceof DocumentsActivity) {
                        ((DocumentsActivity) getActivity()).setHomeInterstitialAd(null);
                        ((DocumentsActivity) getActivity()).loadHomeInterstitial(false);
                    }
                    showSuccess();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    Log.e("CleanerResult", "Ad failed to show: " + adError.getMessage());
                    if (getActivity() instanceof DocumentsActivity) {
                        ((DocumentsActivity) getActivity()).setHomeInterstitialAd(null);
                    }
                    showSuccess();
                }
            });
            adToShow.show(getActivity());
        } else {
            if (adRetryCount < 3) { // Reduced retries but kept it fast
                adRetryCount++;
                Log.d("CleanerResult", "Ad not ready yet, retrying... (" + adRetryCount + ")");
                // Toast.makeText(getContext(), "Fetching ad...", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(this::showInterstitialThenSuccess, 1000);
            } else {
                Log.d("CleanerResult", "Ad null after retries, skipping to success");
                Toast.makeText(getContext(), "Ad not available, showing results", Toast.LENGTH_SHORT).show();
                showSuccess();
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    private void showSuccess() {
        progressLoading.setVisibility(View.GONE);
        iconStatus.setVisibility(View.VISIBLE);
        iconStatus.setImageResource(R.drawable.ic_check_circle_white);
        ivSparkles.setVisibility(View.VISIBLE);
        
        textStatus.setText(sizeDisplay + " cleaned");
        textSubStatus.setText("Your device is now optimized");
        
        btnBack.setVisibility(View.VISIBLE);
        
        // Final success pop animation
        rippleContainer.setScaleX(1.15f);
        rippleContainer.setScaleY(1.15f);
        rippleContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
                
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }

        // Return to Cleaner Tab after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                returnToCleanerTab();
            }
        }, 3000);
    }

    private void returnToCleanerTab() {
        if (getFragmentManager() != null) {
            // Pop everything from backstack to return to the root fragment (Cleaner tab)
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }
}
