package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import com.nextguidance.filesexplorer.filemanager.smartfiles.directory.DocumentsAdapter.Environment;

public class LoadingFooter extends Footer {

    public LoadingFooter(Environment environment, int type) {
        super(type);
        mEnv = environment;
        mIcon = 0;
        mMessage = "";
    }
}