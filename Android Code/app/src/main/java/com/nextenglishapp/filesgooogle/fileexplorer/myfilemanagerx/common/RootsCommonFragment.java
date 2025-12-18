package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.RootsFragment;

public class RootsCommonFragment extends RootsFragment {

    public static void show(FragmentManager fm, Intent includeApps) {
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_INCLUDE_APPS, includeApps);

        final RootsCommonFragment fragment = new RootsCommonFragment();
        fragment.setArguments(args);

        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_roots, fragment);
        ft.commitAllowingStateLoss();
    }

    public static RootsCommonFragment get(FragmentManager fm) {
        return (RootsCommonFragment) fm.findFragmentById(R.id.container_roots);
    }
}