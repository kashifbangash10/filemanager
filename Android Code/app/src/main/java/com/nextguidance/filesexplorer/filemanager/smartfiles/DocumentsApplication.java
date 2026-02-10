/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nextguidance.filesexplorer.filemanager.smartfiles;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.DateUtils;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.collection.ArrayMap;

import com.cloudrail.si.CloudRail;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cast.Casty;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.AnalyticsManager;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ContentProviderClientCompat;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.CrashReportingManager;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.NotificationUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.RootsCache;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.SAFManager;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ThumbnailCache;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.Utils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.server.SimpleWebServer;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdError;
import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import java.util.Date;

public class DocumentsApplication extends Application {
    private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
    private static DocumentsApplication sInstance;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private RootsCache mRoots;
    private ArrayMap<Integer, Long> mSizes = new ArrayMap<Integer, Long>();
    private SAFManager mSAFManager;
    private Point mThumbnailsSize;
    private ThumbnailCache mThumbnailCache;

    private SimpleWebServer simpleWebServer;
    private boolean isStarted;
    private AppOpenAd appOpenAd = null;
    private Activity currentActivity;
    private static boolean isShowingAd = false;
    private long loadTime = 0;
    private Casty mCasty;

    public static RootsCache getRootsCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mRoots;
    }

    public static RootsCache getRootsCache() {
        return ((DocumentsApplication) DocumentsApplication.getInstance().getApplicationContext()).mRoots;
    }

    public static ArrayMap<Integer, Long> getFolderSizes() {
        return getInstance().mSizes;
    }

    public static SAFManager getSAFManager(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mSAFManager;
    }

    public static ThumbnailCache getThumbnailCache(Context context) {
        final DocumentsApplication app = (DocumentsApplication) context.getApplicationContext();
        return app.mThumbnailCache;
    }

    public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        return getThumbnailCache(context);
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
        if (authority == null) {
            throw new RemoteException("Authority is null");
        }
        final ContentProviderClient client = ContentProviderClientCompat.acquireUnstableContentProviderClient(resolver, authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        ContentProviderClientCompat.setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    @Override
    public void onCreate() {
        Utils.setAppThemeStyle(getBaseContext());
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        if (!BuildConfig.DEBUG) {
            AnalyticsManager.intialize(getApplicationContext());
        }
        
        MobileAds.initialize(this, initializationStatus -> {
            // Initialize App Open Ads after MobileAds is ready
            initializeAppOpenAds();
        });
        
        sInstance = this;
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        CloudRail.setAppKey(BuildConfig.LICENSE_KEY);
        CrashReportingManager.enable(getApplicationContext(), !BuildConfig.DEBUG);

        mRoots = new RootsCache(this);
        mRoots.updateAsync();

        mSAFManager = new SAFManager(this);

        mThumbnailCache = new ThumbnailCache(memoryClassBytes / 4);

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        packageFilter.addDataScheme("package");
        registerReceiver(mCacheReceiver, packageFilter);

        final IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mCacheReceiver, localeFilter);


        if (Utils.hasOreo()) {
            NotificationUtils.createNotificationChannels(this);
        }
    }

    public static synchronized DocumentsApplication getInstance() {
        return sInstance;
    }

    public void initCasty(Activity activity) {
        mCasty = Casty.create(activity);
    }

    public Casty getCasty() {
        return mCasty;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mThumbnailCache.onTrimMemory(level);
    }

    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri data = intent.getData();
            if (data != null) {
                final String authority = data.getAuthority();
                mRoots.updateAuthorityAsync(authority);
            } else {
                mRoots.updateAsync();
            }
        }
    };
    
    private void initializeAppOpenAds() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
            
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivity = activity;
            }
            
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                currentActivity = activity;
            }
            
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (currentActivity == activity) {
                    currentActivity = null;
                }
            }
        });
        
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                showAppOpenAd();
            }
        });
        
        fetchAppOpenAd();
    }
    
    public void fetchAppOpenAd() {
        if (isAdAvailable()) {
            return;
        }
        
        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                appOpenAd = ad;
                loadTime = (new Date()).getTime();
                Log.d("AppOpenAd", "App Open Ad loaded successfully");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AppOpenAd", "Failed to load App Open Ad: " + loadAdError.getMessage());
            }
        };
        
        AdRequest request = new AdRequest.Builder().build();
        String adUnitId = getString(R.string.admob_app_open);
        Log.d("AppOpenAd", "Fetching App Open Ad with ID: " + adUnitId);
        
        AppOpenAd.load(
            this,
            adUnitId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            loadCallback
        );
    }
    
    private boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }
    
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }
    
    public void showAppOpenAd() {
        if (isShowingAd) {
            return;
        }

        if (!isAdAvailable()) {
            fetchAppOpenAd();
            return;
        }
        
        if (currentActivity == null || currentActivity instanceof SplashActivity) {
            return;
        }
        
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                isShowingAd = false;
                fetchAppOpenAd();
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                appOpenAd = null;
                isShowingAd = false;
                Log.e("AppOpenAd", "Failed to show: " + adError.getMessage());
                fetchAppOpenAd();
            }
            
            @Override
            public void onAdShowedFullScreenContent() {
                isShowingAd = true;
                Log.d("AppOpenAd", "App Open Ad shown");
            }
        });
        
        appOpenAd.show(currentActivity);
    }
}
