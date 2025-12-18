package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
