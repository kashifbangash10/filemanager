package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_GRID;

public class LoadingHolder extends BaseHolder {

    public LoadingHolder(DocumentsAdapter.Environment environment, Context context, ViewGroup parent) {
        super(context, parent, getLayoutId(environment));
    }

    public static int getLayoutId(DocumentsAdapter.Environment environment){
        int layoutId = R.layout.item_loading_list;
        if(environment.getDisplayState().derivedMode == MODE_GRID){
            layoutId = R.layout.item_loading_grid;
        }
        return layoutId;
    }
}
