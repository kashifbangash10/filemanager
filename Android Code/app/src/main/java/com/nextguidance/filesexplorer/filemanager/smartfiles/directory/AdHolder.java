package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import vocsy.ads.GoogleAds;

public class AdHolder extends BaseHolder {

    public AdHolder(Context context, ViewGroup parent) {
        super(createContainer(context));
    }

    private static FrameLayout createContainer(Context context) {
        FrameLayout layout = new FrameLayout(context);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    public void loadAd(Context context) {
        GoogleAds.getInstance().addNativeView(context, itemView);
    }
}
