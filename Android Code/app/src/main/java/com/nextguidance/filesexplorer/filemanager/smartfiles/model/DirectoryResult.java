package com.nextguidance.filesexplorer.filemanager.smartfiles.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import com.nextguidance.filesexplorer.filemanager.smartfiles.libcore.io.IoUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ContentProviderClientCompat;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_UNKNOWN;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.SORT_ORDER_UNKNOWN;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        IoUtils.closeQuietly(cursor);
        ContentProviderClientCompat.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}