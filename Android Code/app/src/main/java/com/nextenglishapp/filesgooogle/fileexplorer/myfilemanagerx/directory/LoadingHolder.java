package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory;

import android.content.Context;
import android.view.ViewGroup;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;

import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.MODE_GRID;

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
