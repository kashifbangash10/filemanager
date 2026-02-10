package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_GRID;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo.getCursorInt;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo.getCursorLong;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo.getCursorString;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RecyclerFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.MatrixCursor;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.RootCursorWrapper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DirectoryResult;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentsContract;

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
        if (cursorCount == 0) {
             showData(result);
             return;
        }

        MatrixCursor matrixCursor = new MatrixCursor(result.cursor.getColumnNames());
        
        result.cursor.moveToPosition(-1);
        int displayedItems = 0;
        
        while(result.cursor.moveToNext()) {
            String authority = getCursorString(result.cursor, RootCursorWrapper.COLUMN_AUTHORITY);
            if (TextUtils.isEmpty(authority)) {
                continue;
            }
            
            // Insert ad
            if (displayedItems == AD_POSITION) {
                 final MatrixCursor.RowBuilder row = matrixCursor.newRow();
                 row.add(RootCursorWrapper.COLUMN_AUTHORITY, ""); 
                 row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, "AD");
                 row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, "application/ad");
                 row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Ad");
                 row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0L);
                 row.add(DocumentsContract.Document.COLUMN_FLAGS, 0);
                 row.add(DocumentsContract.Document.COLUMN_SUMMARY, "");
                 row.add(DocumentsContract.Document.COLUMN_SIZE, 0L);
                 row.add(DocumentsContract.Document.COLUMN_ICON, 0);
                 row.add(DocumentsContract.Document.COLUMN_PATH, "");
                 displayedItems++;
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
            
            displayedItems++;
        }
        result.cursor = matrixCursor;
        showData(result);
    }

    public abstract void showData(DirectoryResult result);
}
