package com.example.apps.solidfilemamnager.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.example.apps.solidfilemamnager.R;
import com.example.apps.solidfilemamnager.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.example.apps.solidfilemamnager.directory.DocumentsAdapter.Environment;

public class GridDocumentHolder extends ListDocumentHolder {

    public GridDocumentHolder(Context context, ViewGroup parent,
                              OnItemClickListener onItemClickListener, Environment environment) {
        super(context, parent, R.layout.item_doc_grid, onItemClickListener, environment);
    }

}
