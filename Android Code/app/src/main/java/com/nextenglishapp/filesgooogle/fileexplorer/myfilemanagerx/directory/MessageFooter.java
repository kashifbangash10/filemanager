package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory.DocumentsAdapter.Environment;

public class MessageFooter extends Footer {

    public MessageFooter(Environment environment, int itemViewType, int icon, String message) {
        super(itemViewType);
        mIcon = icon;
        mMessage = message;
        mEnv = environment;
    }
}