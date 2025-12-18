package com.example.apps.solidfilemamnager.directory;

import static com.example.apps.solidfilemamnager.BaseActivity.State.MODE_GRID;
import static com.example.apps.solidfilemamnager.model.DocumentInfo.getCursorInt;
import static com.example.apps.solidfilemamnager.model.DocumentInfo.getCursorLong;
import static com.example.apps.solidfilemamnager.model.DocumentInfo.getCursorString;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.example.apps.solidfilemamnager.R;
import com.example.apps.solidfilemamnager.common.RecyclerFragment;
import com.example.apps.solidfilemamnager.cursor.MatrixCursor;
import com.example.apps.solidfilemamnager.cursor.RootCursorWrapper;
import com.example.apps.solidfilemamnager.model.DirectoryResult;
import com.example.apps.solidfilemamnager.model.DocumentsContract;

public abstract class DirectoryFragmentFlavour extends RecyclerFragment {
    public static final int AD_POSITION = 5;

    public void loadNativeAds(final DirectoryResult result) {
        int cursorCount = result.cursor != null ? result.cursor.getCount() : 0;
        if (cursorCount <= 5) {
            showData(result);
            return;
        }

        Log.e("result.mode", "" + result.mode);
        String appUnitId = result.mode == MODE_GRID ? getString(R.string.admob_native) : getString(R.string.admob_native);
        Log.e("appUnitId", "" + appUnitId);

        insertNativeAds(result);

        showData(result);
    }

    private void insertNativeAds(DirectoryResult result) {
        int cursorCount = result.cursor != null ? result.cursor.getCount() : 0;


        int index = 0;
        MatrixCursor matrixCursor = new MatrixCursor(result.cursor.getColumnNames());
        Bundle bundle = new Bundle();

        result.cursor.moveToPosition(-1);
        for (int i = 0; i < (cursorCount); i++) {
            result.cursor.moveToNext();
            String authority = getCursorString(result.cursor, RootCursorWrapper.COLUMN_AUTHORITY);
            if (TextUtils.isEmpty(authority)) {
                continue;
            }
            final MatrixCursor.RowBuilder row = matrixCursor.newRow();
            row.add(RootCursorWrapper.COLUMN_AUTHORITY, authority);
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_DOCUMENT_ID));
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_MIME_TYPE));
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_DISPLAY_NAME));
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, getCursorLong(result.cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED));
            row.add(DocumentsContract.Document.COLUMN_FLAGS, getCursorInt(result.cursor, DocumentsContract.Document.COLUMN_FLAGS));
            row.add(DocumentsContract.Document.COLUMN_SUMMARY, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_SUMMARY));
            row.add(DocumentsContract.Document.COLUMN_SIZE, getCursorLong(result.cursor, DocumentsContract.Document.COLUMN_SIZE));
            row.add(DocumentsContract.Document.COLUMN_ICON, getCursorInt(result.cursor, DocumentsContract.Document.COLUMN_ICON));
            row.add(DocumentsContract.Document.COLUMN_PATH, getCursorString(result.cursor, DocumentsContract.Document.COLUMN_PATH));

        }
        result.cursor = matrixCursor;
        showData(result);
    }

    public abstract void showData(DirectoryResult result);
}
