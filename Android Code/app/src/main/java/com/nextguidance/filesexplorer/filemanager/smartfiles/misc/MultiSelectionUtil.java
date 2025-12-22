/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nextguidance.filesexplorer.filemanager.smartfiles.misc;

import android.os.Bundle;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;

import java.util.HashSet;

/**
 * Utilities for handling multiple selection in list views. Contains functionality similar to {@link
 * AbsListView#CHOICE_MODE_MULTIPLE_MODAL} which works with {@link AppCompatActivity} and
 * backward-compatible action bars.
 */
public class MultiSelectionUtil {
    /**
     * Attach a Controller to the given <code>listView</code>, <code>activity</code>
     * and <code>listener</code>.
     *
     * @param listView ListView which displays {@link android.widget.Checkable} items.
     * @param activity Activity which contains the ListView.
     * @param listener Listener that will manage the selection mode.
     * @return the attached Controller instance.
     */
    public static Controller attachMultiSelectionController(final AbsListView listView,
                                                            final AppCompatActivity activity, final MultiChoiceModeListener listener) {
        return new Controller(listView, activity, listener);
    }
    /**
     * Class which provides functionality similar to {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}
     * for the {@link AbsListView} provided to it. A
     * {@link AdapterView.OnItemLongClickListener} is set on the ListView so that
     * when an item is long-clicked an ActionBarCompat Action Mode is started. Once started, a
     * {@link AdapterView.OnItemClickListener} is set so that an item click toggles
     * that item's checked state.
     */
    public static class Controller {
        private final AbsListView mListView;
        private final AppCompatActivity mActivity;
        private final MultiChoiceModeListener mListener;
        private final Callbacks mCallbacks;

        private ActionMode mActionMode;

        private HashSet<Pair<Integer, Long>> mItemsToCheck;

        private AdapterView.OnItemClickListener mOldItemClickListener;
        private final Runnable mSetChoiceModeNoneRunnable = new Runnable() {
            @Override
            public void run() {
                mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            }
        };
        private Controller(AbsListView listView, AppCompatActivity activity,
                MultiChoiceModeListener listener) {
            mListView = listView;
            mActivity = activity;
            mListener = listener;
            mCallbacks = new Callbacks();


            listView.setOnItemLongClickListener(mCallbacks);
        }
        /**
         * Finish the current Action Mode (if there is one).
         */
        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
        /**
         * This method should be called from your {@link AppCompatActivity} or
         * {@link Fragment Fragment} to allow the controller to restore any
         * instance state.
         *
         * @param savedInstanceState - The state passed to your Activity or Fragment.
         */
        public void restoreInstanceState(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                long[] checkedIds = savedInstanceState.getLongArray(getStateKey());
                if (checkedIds != null && checkedIds.length > 0) {
                    HashSet<Long> idsToCheckOnRestore = new HashSet<Long>();
                    for (long id : checkedIds) {
                        idsToCheckOnRestore.add(id);
                    }
                    tryRestoreInstanceState(idsToCheckOnRestore);
                }
            }
        }

        public void saveInstanceState(Bundle outState) {
            if (mActionMode != null && mListView.getAdapter().hasStableIds()) {
                outState.putLongArray(getStateKey(), mListView.getCheckedItemIds());
            }
        }

        private String getStateKey() {
            return MultiSelectionUtil.class.getSimpleName() + "_" + mListView.getId();
        }
        private void tryRestoreInstanceState(HashSet<Long> idsToCheckOnRestore) {
            if (idsToCheckOnRestore == null || mListView.getAdapter() == null) {
                return;
            }
            boolean idsFound = false;
            Adapter adapter = mListView.getAdapter();
            for (int pos = adapter.getCount() - 1; pos >= 0; pos--) {
                if (idsToCheckOnRestore.contains(adapter.getItemId(pos))) {
                    idsFound = true;
                    if (mItemsToCheck == null) {
                        mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                    }
                    mItemsToCheck.add(new Pair<Integer, Long>(pos, adapter.getItemId(pos)));
                }
            }
            if (idsFound) {


                mActionMode = mActivity.startSupportActionMode(mCallbacks);
            }
        }
        /**
         * This class encapsulates all of the callbacks necessary for the controller class.
         */
        final class Callbacks implements ActionMode.Callback, AdapterView.OnItemClickListener,
                AdapterView.OnItemLongClickListener {
            @Override
            public final boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                if (mListener.onCreateActionMode(actionMode, menu)) {
                    mActionMode = actionMode;

                    mOldItemClickListener = mListView.getOnItemClickListener();

                    mListView.setOnItemClickListener(this);
                    mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                    mListView.removeCallbacks(mSetChoiceModeNoneRunnable);

                    if (mItemsToCheck != null) {
                        for (Pair<Integer, Long> posAndId : mItemsToCheck) {
                            mListView.setItemChecked(posAndId.first, true);

                            mListener.onItemCheckedStateChanged(mActionMode, posAndId.first,
                                    posAndId.second, true);
                        }
                    }
                    return true;
                }
                return false;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {

                return mListener.onPrepareActionMode(actionMode, menu);
            }
            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

                return mListener.onActionItemClicked(actionMode, menuItem);
            }
            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                mListener.onDestroyActionMode(actionMode);

                SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
                if (checkedPositions != null) {
                    for (int i = 0; i < checkedPositions.size(); i++) {
                        mListView.setItemChecked(checkedPositions.keyAt(i), false);
                    }
                }

                mListView.setOnItemClickListener(mOldItemClickListener);

                mActionMode = null;

                mListView.post(mSetChoiceModeNoneRunnable);
            }
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final boolean checked = mListView.isItemChecked(position);
                mListener.onItemCheckedStateChanged(mActionMode, position, id, checked);
                boolean hasCheckedItem = checked;

                if (!hasCheckedItem) {
                    SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
                    if (checkedItemPositions != null) {

                        int i = 0;
                        while (!hasCheckedItem && i < checkedItemPositions.size()) {
                            hasCheckedItem = checkedItemPositions.valueAt(i++);
                        }
                    }
                }

                if (!hasCheckedItem) {
                    mActionMode.finish();
                }
            }
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position,
                    long id) {


                if (mActionMode != null) {
                    return false;
                }
                mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                mItemsToCheck.add(new Pair<Integer, Long>(position, id));
                mActionMode = mActivity.startSupportActionMode(this);
                return true;
            }
        }
    }
    /**
     * @see AbsListView.MultiChoiceModeListener
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * @see AbsListView.MultiChoiceModeListener#onItemCheckedStateChanged(
         *android.view.ActionMode, int, long, boolean)
         */
        void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                       boolean checked);
    }
}