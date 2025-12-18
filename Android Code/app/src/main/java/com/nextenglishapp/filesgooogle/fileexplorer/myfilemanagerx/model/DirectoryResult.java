package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.libcore.io.IoUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ContentProviderClientCompat;

import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.MODE_UNKNOWN;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.SORT_ORDER_UNKNOWN;

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