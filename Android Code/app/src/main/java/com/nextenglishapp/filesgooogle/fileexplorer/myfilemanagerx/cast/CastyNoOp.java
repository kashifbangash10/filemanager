package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.cast;

import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.app.MediaRouteButton;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.MediaQueue;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

class CastyNoOp extends Casty {
    private CastyPlayer castyPlayer;

    CastyNoOp() {
        castyPlayer = new CastyPlayerNoOp();
    }

    @Override
    public CastyPlayer getPlayer() {
        return castyPlayer;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void addMediaRouteMenuItem(@NonNull Menu menu) {
        
    }

    @Override
    public void setUpMediaRouteButton(@NonNull MediaRouteButton mediaRouteButton) {
        
    }

    @Override
    public Casty withMiniController() {
        return this;
    }

    @Override
    public void addMiniController() {
        
    }

    @Override
    public void setOnConnectChangeListener(@Nullable Casty.OnConnectChangeListener onConnectChangeListener) {
        
    }

    @Override
    public void setOnCastSessionUpdatedListener(@Nullable Casty.OnCastSessionUpdatedListener onCastSessionUpdatedListener) {
        
    }

    @Override
    public CastSession getCastSession() {
        return null;
    }

    @Override
    public RemoteMediaClient getRemoteMediaClient() {
        return null;
    }

    @Override
    public MediaQueue getMediaQueue() {
        return null;
    }
}
