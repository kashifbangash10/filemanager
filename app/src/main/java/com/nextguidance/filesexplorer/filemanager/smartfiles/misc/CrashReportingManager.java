package com.nextguidance.filesexplorer.filemanager.smartfiles.misc;

import android.content.Context;
import com.nextguidance.filesexplorer.filemanager.smartfiles.BuildConfig;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Created by HaKr on 23/05/16.
 * Updated to use Firebase Crashlytics.
 */

public class CrashReportingManager {

    public static void enable(Context context, boolean enable) {
        try {
            // Check if Firebase is initialized before accessing Crashlytics
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logException(Exception e) {
        logException(e, false);
    }

    public static void logException(Exception e, boolean log) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace();
        } else if (log) {
            try {
                FirebaseCrashlytics.getInstance().recordException(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void log(String s) {
        try {
            FirebaseCrashlytics.getInstance().log(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String tag, String s) {
        try {
            FirebaseCrashlytics.getInstance().log(tag + ":" + s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}