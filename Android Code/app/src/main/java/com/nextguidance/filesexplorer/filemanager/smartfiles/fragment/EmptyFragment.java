package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;

public class EmptyFragment extends Fragment {
    public static final String TAG = "EmptyFragment";

    public static void show(FragmentManager fm) {
        EmptyFragment fragment = new EmptyFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We use a simple empty view
        return new View(getContext());
    }
}
