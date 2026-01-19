package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.media.MediaQueue;

import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsApplication;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.QueueAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cast.CastUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cast.Casty;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.ActionBarActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RecyclerFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RecyclerFragment.OnItemClickListener;
import com.nextguidance.filesexplorer.filemanager.smartfiles.directory.DividerItemDecoration;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.IconHelper;

import static android.widget.LinearLayout.VERTICAL;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_GRID;


public class QueueFragment extends RecyclerFragment implements OnItemClickListener {

    private static final String TAG = "QueueFragment";

    private QueueAdapter mAdapter;
    private Casty casty;

    public static void show(FragmentManager fm) {
        final QueueFragment fragment = new QueueFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static QueueFragment get(FragmentManager fm) {
        return (QueueFragment) fm.findFragmentByTag(TAG);
    }

    public QueueFragment() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        casty = DocumentsApplication.getInstance().getCasty();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Resources res = getActivity().getResources();

        final boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        final int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        DividerItemDecoration decoration = new DividerItemDecoration(getActivity(), VERTICAL);
        if (insetLeft) {
            decoration.setInset(insetSize, 0);
        } else {
            decoration.setInset(0, insetSize);
        }
             getListView().addItemDecoration(decoration);

    }

    @Override
    public void onResume() {
        super.onResume();
        registerDataObserver();
    }

    @Override
    public void onPause() {
        unregisterDataObserver();
        super.onPause();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateMediaQueue();
        setListShown(true);
        if(null == casty || !casty.isConnected()) {
            return;
        }
        IconHelper iconHelper = new IconHelper(getActivity(), MODE_GRID);
        mAdapter = new QueueAdapter(casty.getMediaQueue(), iconHelper);
        mAdapter.setOnItemClickListener(this);
        setListAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.queue_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_queue:
                CastUtils.clearQueue(casty);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(RecyclerView.ViewHolder item, View view, int position) {
        CastUtils.playFromQueue(casty, mAdapter.getItem(position).getItemId());
    }

    @Override
    public void onItemLongClick(RecyclerView.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemViewClick(RecyclerView.ViewHolder item, View view, int position) {
        onPopupMenuClick(view, position);
    }

    private void updateMediaQueue() {
        MediaQueue queue = casty.getMediaQueue();
        int queueCount = null != queue ? queue.getItemCount() : 0;
        String count = "";
        if (queueCount == 0) {
            setEmptyText(getString(R.string.queue_empty));
        } else {
            count = getResources().getQuantityString(R.plurals.queue_count, queueCount, queueCount);
            setEmptyText("");
        }
        ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(count);
    }

    private void registerDataObserver() {
        try {
            casty.getMediaQueue().registerCallback(callback);
        } catch (Exception ignored){}
    }

    private void unregisterDataObserver() {
        try {
            casty.getMediaQueue().unregisterCallback(callback);
        } catch (Exception ignored){}
    }

    private MediaQueue.Callback callback = new MediaQueue.Callback() {

        @Override
        public void itemsReloaded() {
            super.itemsReloaded();
            updateMediaQueue();
        }
    };

    void onPopupMenuClick(final View view, final int position) {
        final PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.queue_context);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onPopupMenuItemClick(item, position);
            }
        });
        popup.show();
    }

    public boolean onPopupMenuItemClick(final MenuItem menuItem, int position) {
        MediaQueueItem mediaQueueItem = mAdapter.getItem(position);
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                if (mediaQueueItem != null) {
                    CastUtils.removeQueueItem(casty, mediaQueueItem.getItemId());
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyView() {
        ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(null);
        super.onDestroyView();
    }
}