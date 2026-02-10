package com.nextguidance.filesexplorer.filemanager.smartfiles.model;

import android.net.Uri;

public class AudioInfo {
    public String documentId;
    public String displayName;
    public String path;
    public long size;
    public long lastModified;
    public String mimeType;
    public Uri derivedUri;
    public long duration; // in milliseconds
    public boolean isChecked = false;

    // For folder grouping
    public String folderName;
}
