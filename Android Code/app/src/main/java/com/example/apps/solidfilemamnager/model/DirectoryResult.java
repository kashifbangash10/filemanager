package com.example.apps.solidfilemamnager.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import com.example.apps.solidfilemamnager.libcore.io.IoUtils;
import com.example.apps.solidfilemamnager.misc.ContentProviderClientCompat;

import static com.example.apps.solidfilemamnager.BaseActivity.State.MODE_UNKNOWN;
import static com.example.apps.solidfilemamnager.BaseActivity.State.SORT_ORDER_UNKNOWN;

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