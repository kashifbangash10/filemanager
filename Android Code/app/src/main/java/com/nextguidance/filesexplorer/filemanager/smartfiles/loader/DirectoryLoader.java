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

package com.nextguidance.filesexplorer.filemanager.smartfiles.loader;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.OperationCanceledException;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;

import com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsApplication;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.FilteringCursorWrapper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.RootCursorWrapper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.SortingCursorWrapper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.libcore.io.IoUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.AsyncTaskLoader;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ContentProviderClientCompat;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.CrashReportingManager;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ProviderExecutor;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DirectoryResult;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentsContract;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentsContract.Document;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.RootInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.RecentsProvider;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.RecentsProvider.StateColumns;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.SORT_ORDER_DISPLAY_NAME;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.SORT_ORDER_LAST_MODIFIED;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.SORT_ORDER_SIZE;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.TAG;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo.getCursorInt;

public class DirectoryLoader extends AsyncTaskLoader<DirectoryResult> {

    private static final String[] SEARCH_REJECT_MIMES = new String[] { };

    private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();

    private final int mType;
    private final RootInfo mRoot;
    private DocumentInfo mDoc;
    private final Uri mUri;
    private final int mUserSortOrder;

    private CancellationSignal mSignal;
    private DirectoryResult mResult;

    public DirectoryLoader(Context context, int type, RootInfo root, DocumentInfo doc, Uri uri,
            int userSortOrder) {
        super(context, ProviderExecutor.forAuthority(root.authority));
        mType = type;
        mRoot = root;
        mDoc = doc;
        mUri = uri;
        mUserSortOrder = userSortOrder;
    }

    @Override
    public final DirectoryResult loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mSignal = new CancellationSignal();
        }

        final ContentResolver resolver = getContext().getContentResolver();
        final String authority = mUri.getAuthority();

        final DirectoryResult result = new DirectoryResult();

        int userMode = State.MODE_UNKNOWN;
        int userSortOrder = State.SORT_ORDER_UNKNOWN;


        if (mType == DirectoryFragment.TYPE_SEARCH) {
            final Uri docUri = DocumentsContract.buildDocumentUri(
                    mRoot.authority, mRoot.documentId);
            try {
                mDoc = DocumentInfo.fromUri(resolver, docUri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Failed to query", e);
                result.exception = e;
                CrashReportingManager.logException(e);
                return result;
            }
        }


        Cursor cursor = null;
        try {
            final Uri stateUri = RecentsProvider.buildState(
                    mRoot.authority, mRoot.rootId, mDoc.documentId);
            cursor = resolver.query(stateUri, null, null, null, null);
            if (null != cursor && cursor.moveToFirst()) {
                userMode = getCursorInt(cursor, StateColumns.MODE);
                userSortOrder = getCursorInt(cursor, StateColumns.SORT_ORDER);
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        if (userMode != State.MODE_UNKNOWN) {
            result.mode = userMode;
        } else {
            if ((mDoc.flags & Document.FLAG_DIR_PREFERS_GRID) != 0) {
                result.mode = State.MODE_GRID;
            } else {
                result.mode = State.MODE_LIST;
            }
        }

        if (userSortOrder != State.SORT_ORDER_UNKNOWN) {
            result.sortOrder = userSortOrder;
        } else {
            if ((mDoc.flags & Document.FLAG_DIR_PREFERS_LAST_MODIFIED) != 0) {
                result.sortOrder = State.SORT_ORDER_LAST_MODIFIED;
            } else {
                result.sortOrder = State.SORT_ORDER_DISPLAY_NAME;
            }
        }


        if (mType == DirectoryFragment.TYPE_SEARCH) {

        }

        Log.d(TAG, "userMode=" + userMode + ", userSortOrder=" + userSortOrder + " --> mode="
                + result.mode + ", sortOrder=" + result.sortOrder);

        ContentProviderClient client = null;
        try {
            client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, authority);

            cursor = client.query(
                    mUri, null, null, null, getQuerySortOrder(result.sortOrder));
            cursor.registerContentObserver(mObserver);

            cursor = new RootCursorWrapper(mUri.getAuthority(), mRoot.rootId, cursor, -1);

            if (mType == DirectoryFragment.TYPE_SEARCH) {
                cursor = new SortingCursorWrapper(cursor, result.sortOrder);

                cursor = new FilteringCursorWrapper(cursor, null, SEARCH_REJECT_MIMES);
            } else {

                cursor = new SortingCursorWrapper(cursor, result.sortOrder);
            }

            result.client = client;
            result.cursor = cursor;
        } catch (Exception e) {
            Log.w(TAG, "Failed to query", e);
            CrashReportingManager.logException(e);
            result.exception = e;
        } finally {
            synchronized (this) {
                mSignal = null;
            }

            ContentProviderClientCompat.releaseQuietly(client);
        }

        return result;
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mSignal != null) {
                mSignal.cancel();
            }
        }
    }

    @Override
    public void deliverResult(DirectoryResult result) {
        if (isReset()) {
            IoUtils.closeQuietly(result);
            return;
        }
        DirectoryResult oldResult = mResult;
        mResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }

        if (oldResult != null && oldResult != result) {
            IoUtils.closeQuietly(oldResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mResult != null) {
            deliverResult(mResult);
        }
        if (takeContentChanged() || mResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(DirectoryResult result) {
        IoUtils.closeQuietly(result);
    }

    @Override
    protected void onReset() {
        super.onReset();


        onStopLoading();

        IoUtils.closeQuietly(mResult);
        mResult = null;

        getContext().getContentResolver().unregisterContentObserver(mObserver);
    }

    public static String getQuerySortOrder(int sortOrder) {
        switch (sortOrder) {
            case SORT_ORDER_DISPLAY_NAME:
                return Document.COLUMN_DISPLAY_NAME + " ASC";
            case SORT_ORDER_LAST_MODIFIED:
                return Document.COLUMN_LAST_MODIFIED + " DESC";
            case SORT_ORDER_SIZE:
                return Document.COLUMN_SIZE + " DESC";
            default:
                return null;
        }
    }

    public final class ForceLoadContentObserver extends ContentObserver {
        public ForceLoadContentObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            final String path  = null != uri ? uri.getPath() : "";
            if(!TextUtils.isEmpty(path)){
                return;
            }
            super.onChange(selfChange, uri);
        }
    }

}