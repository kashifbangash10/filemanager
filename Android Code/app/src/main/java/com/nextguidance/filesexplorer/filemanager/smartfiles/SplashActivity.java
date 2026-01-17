package com.nextguidance.filesexplorer.filemanager.smartfiles;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long MIN_SPLASH_TIME = 2000; // 2 seconds minimum to show branding
    private static final long MAX_AD_WAIT_TIME = 5000; // 5 seconds maximum wait for ad

    private InterstitialAd mInterstitialAd;
    private boolean isNavigating = false;
    private long startTime;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());

    private final Runnable timeoutRunnable = this::navigateToHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.DocumentsTheme_Splash); // Ensuring correct theme is used
        setContentView(R.layout.activity_splash_temp);

        startTime = System.currentTimeMillis();
        
        // Safety timeout: If nothing happens in 5 seconds, move to home
        timeoutHandler.postDelayed(timeoutRunnable, MAX_AD_WAIT_TIME);

        loadInterstitialAd();
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, getString(R.string.admob_interstitial), adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    checkAndShowAd();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e(TAG, "Ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                    // If ad fails, we check if minimum splash time has passed
                    long timeSpent = System.currentTimeMillis() - startTime;
                    if (timeSpent >= MIN_SPLASH_TIME) {
                        navigateToHome();
                    } else {
                        // Wait for the remaining splash time
                        timeoutHandler.postDelayed(() -> navigateToHome(), MIN_SPLASH_TIME - timeSpent);
                    }
                }
            });
    }

    private void checkAndShowAd() {
        if (isFinishing() || isDestroyed()) return;

        long timeSpent = System.currentTimeMillis() - startTime;
        if (timeSpent >= MIN_SPLASH_TIME) {
            showInterstitialAd();
        } else {
            // Wait for the remaining minimum splash time before showing
            timeoutHandler.postDelayed(this::showInterstitialAd, MIN_SPLASH_TIME - timeSpent);
        }
    }

    private void showInterstitialAd() {
        // Double check lifecycle to avoid "Ads showing over other apps" violation
        if (isFinishing() || isDestroyed() || mInterstitialAd == null) {
            navigateToHome();
            return;
        }

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                navigateToHome();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                mInterstitialAd = null;
                navigateToHome();
            }
        });

        mInterstitialAd.show(this);
        timeoutHandler.removeCallbacks(timeoutRunnable); // Stop the safety timeout
    }

    private synchronized void navigateToHome() {
        if (isNavigating) return;
        isNavigating = true;

        timeoutHandler.removeCallbacksAndMessages(null);
        
        Intent intent = new Intent(SplashActivity.this, DocumentsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
