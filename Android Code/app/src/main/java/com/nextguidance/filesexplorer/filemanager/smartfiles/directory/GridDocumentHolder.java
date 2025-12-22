package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.nextguidance.filesexplorer.filemanager.smartfiles.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
