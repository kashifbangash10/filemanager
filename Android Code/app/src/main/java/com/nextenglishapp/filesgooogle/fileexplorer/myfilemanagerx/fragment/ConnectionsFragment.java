package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.internal.NavigationMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsApplication;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.adapter.ConnectionsAdapter;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.cloud.CloudConnection;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common.DialogBuilder;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common.RecyclerFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.directory.DividerItemDecoration;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AnalyticsManager;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ProviderExecutor;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.RootsCache;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.RootInfo;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.network.NetworkConnection;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.ExplorerProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.NetworkStorageProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.setting.SettingsActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.FloatingActionsMenu;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.fabs.FabSpeedDial;

import static android.widget.LinearLayout.VERTICAL;


import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ConnectionUtils.addConnection;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ConnectionUtils.editConnection;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DocumentInfo.getCursorInt;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.network.NetworkConnection.SERVER;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider.TYPE_BOX;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider.TYPE_CLOUD;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider.TYPE_DROPBOX;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider.TYPE_GDRIVE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.CloudStorageProvider.TYPE_ONEDRIVE;

public class ConnectionsFragment extends RecyclerFragment
        implements View.OnClickListener, FabSpeedDial.MenuListener, ConnectionsAdapter.OnItemClickListener {

    public static final String TAG = "ConnectionsFragment";

    private ConnectionsAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private final int mLoaderId = 42;
    private FloatingActionsMenu mActionMenu;
    private RootInfo mConnectionsRoot;
    private int mLastShowAccentColor;

    public static void show(FragmentManager fm) {
        final ConnectionsFragment fragment = new ConnectionsFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static ConnectionsFragment get(FragmentManager fm) {
        return (ConnectionsFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnectionsRoot = DocumentsApplication.getRootsCache(getActivity()).getConnectionsRoot();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_connections,container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final Resources res = getActivity().getResources();

        mActionMenu = (FloatingActionsMenu) view.findViewById(R.id.fabs);
        mActionMenu.setMenuListener(this);
        mActionMenu.setVisibility( View.VISIBLE);
        mActionMenu.attachToListView(getListView());


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
        int accentColor = SettingsActivity.getAccentColor();
        if ((mLastShowAccentColor != 0 && mLastShowAccentColor == accentColor))
            return;
        int defaultColor = SettingsActivity.getPrimaryColor(getActivity());
        mActionMenu.setBackgroundTintList(SettingsActivity.getAccentColor());
        mActionMenu.setSecondaryBackgroundTintList(Utils.getActionButtonColor(defaultColor));
    }


    @Override
    public void onItemClick(ConnectionsAdapter.ViewHolder item, View view, int position) {
        final Cursor cursor = mAdapter.getItem(position);
        if (cursor != null) {
            openConnectionRoot(cursor);
        }
    }

    @Override
    public void onItemLongClick(ConnectionsAdapter.ViewHolder item, View view, int position) {

    }

    @Override
    public void onItemViewClick(ConnectionsAdapter.ViewHolder item, View view, int position) {
        showPopupMenu(view, position);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();

        mAdapter = new ConnectionsAdapter(context, null);
        mAdapter.setOnItemClickListener(this);
        mCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                Uri contentsUri = ExplorerProvider.buildConnection();

                String selection = null;
                String[] selectionArgs = null;
                if(!Utils.hasWiFi(getActivity())){
                    selection = ExplorerProvider.ConnectionColumns.TYPE + "!=? " ;
                    selectionArgs = new String[]{SERVER};
                }

                return new CursorLoader(context, contentsUri, null, selection, selectionArgs, null);
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
                if (!isAdded())
                    return;

                mAdapter.swapCursor(result);
                if (isResumed()) {
                    setListShown(true);
                } else {
                    setListShownNoAnimation(true);
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.swapCursor(null);
            }
        };
        setListAdapter(mAdapter);
        setListShown(false);

        LoaderManager.getInstance(getActivity()).restartLoader(mLoaderId, null, mCallbacks);

    }

    public void reload(){
        LoaderManager.getInstance(getActivity()).restartLoader(mLoaderId, null, mCallbacks);
        RootsCache.updateRoots(getActivity(), NetworkStorageProvider.AUTHORITY);
        RootsCache.updateRoots(getActivity(), CloudStorageProvider.AUTHORITY);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.connections_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        menuItemAction(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()){
            case R.id.fab:
                addConnection(getAppCompatActivity());
                break;
        }
    }

    private void showPopupMenu(View view, final int position) {
        PopupMenu popup = new PopupMenu(getActivity(), view);

        popup.getMenuInflater().inflate(R.menu.popup_connections, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return onPopupMenuItemClick(menuItem, position);
            }
        });
        popup.show();
    }

    public boolean onPopupMenuItemClick(MenuItem item, int position) {
        final Cursor cursor = mAdapter.getItem(position);
        int connection_id = getCursorInt(cursor, BaseColumns._ID);
        NetworkConnection networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_edit:
                if(!networkConnection.type.startsWith(TYPE_CLOUD)) {
                    editConnection(getAppCompatActivity(), connection_id);
                } else {
                    Utils.showSnackBar(getActivity(), "Cloud storage connection can't be edited");
                }
                return true;
            case R.id.menu_delete:
                if(!networkConnection.type.equals(SERVER)) {
                    deleteConnection(connection_id);
                } else {
                    Utils.showSnackBar(getActivity(), "Default server connection can't be deleted");
                }
                return true;
            default:
                return false;
        }
    }

    private void deleteConnection(final int connection_id) {
        DialogBuilder builder = new DialogBuilder(getActivity());
        builder.setMessage("Delete connection?")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int did) {
                        boolean success = NetworkConnection.deleteConnection(getActivity(), connection_id);
                        if(success){
                            reload();
                        }
                    }
                    }).setNegativeButton(android.R.string.cancel,  null);
        builder.showDialog();
        AnalyticsManager.logEvent("connection_delete");
    }

    public void openConnectionRoot(Cursor cursor) {
        NetworkConnection connection = NetworkConnection.fromConnectionsCursor(cursor);
        DocumentsActivity activity = ((DocumentsActivity)getActivity());
        if (connection.type.startsWith(TYPE_CLOUD)){
            activity.onRootPicked(activity.getRoots().getRootInfo(CloudConnection.fromCursor(getActivity(), cursor)), mConnectionsRoot);
        } else {
            activity.onRootPicked(activity.getRoots().getRootInfo(connection), mConnectionsRoot);
        }
    }

    public void openConnectionRoot(NetworkConnection connection) {
        DocumentsActivity activity = ((DocumentsActivity)getActivity());
        activity.onRootPicked(activity.getRoots().getRootInfo(connection), mConnectionsRoot);
    }

    public void openConnectionRoot(CloudConnection connection) {
        DocumentsActivity activity = ((DocumentsActivity)getActivity());
        activity.onRootPicked(activity.getRoots().getRootInfo(connection), mConnectionsRoot);
    }

    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    public boolean onMenuItemSelected(MenuItem menuItem) {
        menuItemAction(menuItem);
        mActionMenu.closeMenu();
        return false;
    }

    @Override
    public void onMenuClosed() {

    }

    public void addCloudConnection(String cloudType){
        final BaseActivity activity = (BaseActivity) getActivity();
        CloudConnection cloudStorage = CloudConnection.createCloudConnections(getActivity(), cloudType);
        new CloudConnection.CreateConnectionTask(activity, cloudStorage).executeOnExecutor(
                ProviderExecutor.forAuthority(CloudStorageProvider.AUTHORITY+cloudType));
        AnalyticsManager.logEvent("add_cloud");
    }

    public void menuItemAction(MenuItem menuItem) {
        final BaseActivity activity = (BaseActivity) getActivity();




        switch (menuItem.getItemId()){
            case R.id.cloud_gridve:
                addCloudConnection(TYPE_GDRIVE);
                break;

            case R.id.cloud_dropbox:
                addCloudConnection(TYPE_DROPBOX);
                break;









            case R.id.network_ftp:
                addConnection(getAppCompatActivity());
                AnalyticsManager.logEvent("add_ftp");
                break;
        }
    }
}