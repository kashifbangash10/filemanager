package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.cast;


import androidx.annotation.NonNull;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

class CastyPlayerNoOp extends CastyPlayer {
    @Override
    public void play() {
        
    }

    @Override
    public void pause() {
        
    }

    @Override
    public void seek(long time) {
        
    }

    @Override
    public void togglePlayPause() {
        
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isBuffering() {
        return false;
    }

    @Override
    public boolean loadMediaAndPlay(@NonNull MediaData mediaData) {
        return false;
    }

    @Override
    public boolean loadMediaAndPlay(@NonNull MediaInfo mediaInfo) {
        return false;
    }

    @Override
    public boolean loadMediaAndPlay(@NonNull MediaInfo mediaInfo, boolean autoPlay, long position) {
        return false;
    }

    @Override
    public boolean loadMediaAndPlayInBackground(@NonNull MediaData mediaData) {
        return false;
    }

    @Override
    public boolean loadMediaAndPlayInBackground(@NonNull MediaInfo mediaInfo) {
        return false;
    }

    @Override
    public boolean loadMediaAndPlayInBackground(@NonNull MediaInfo mediaInfo, boolean autoPlay, long position) {
        return false;
    }

    @Override
    public boolean loadMediaInQueueAndPlay(MediaQueueItem queueItem) {
        return false;
    }

    @Override
    public boolean loadMediaInQueueAndPlayInBackground(MediaQueueItem queueItem) {
        return false;
    }

    @Override
    public RemoteMediaClient getRemoteMediaClient() {
        return null;
    }
}
