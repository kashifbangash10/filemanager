package com.nextguidance.filesexplorer.filemanager.smartfiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;

public class SplashActivity extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;
    private boolean isDelayOver = false;
    private boolean adLoadFailed = false; // To track if ad failed to load
    private boolean isAdDismissed = false; // To prevent double navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_temp);

        // Load Ad
        loadInterstitialAd();

        // 2 Seconds Delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                isDelayOver = true;
                if (mInterstitialAd != null) {
                    showInterstitialAd();
                } else if (adLoadFailed) {
                    navigateToHome();
                }
            }
        }, 2000);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, getString(R.string.admob_interadsid), adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    if (isDelayOver) {
                         showInterstitialAd();
                    }
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
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
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    adLoadFailed = true;
                    if (isDelayOver) {
                        navigateToHome();
                    }
                }
            });
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            navigateToHome();
        }
    }

    private void navigateToHome() {
        if (isAdDismissed) return;
        isAdDismissed = true;
        
        Intent intent = new Intent(SplashActivity.this, DocumentsActivity.class);
        startActivity(intent);
        finish();
    }
}
