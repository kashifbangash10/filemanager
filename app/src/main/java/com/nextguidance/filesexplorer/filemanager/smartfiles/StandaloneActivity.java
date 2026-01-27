package com.nextguidance.filesexplorer.filemanager.smartfiles;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RootsCommonFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.CreateDirectoryFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.SaveFragment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.libcore.io.IoUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.AsyncTask;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ContentProviderClientCompat;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.MimePredicate;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ProviderExecutor;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.RootsCache;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.Utils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentStack;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentsContract;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DurableUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.RootInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.RecentsProvider;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.RecentsProvider.ResumeColumns;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment.ANIM_DOWN;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment.ANIM_NONE;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment.ANIM_SIDE;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment.ANIM_UP;

public class StandaloneActivity extends BaseActivity {
    public static final String TAG = "StandaloneActivity";
    private static final String EXTRA_STATE = "state";
    private static final int CODE_FORWARD = 42;
    private SearchView mSearchView;
    private Toolbar mToolbar;
    private Spinner mToolbarStack;
    private Toolbar mRootsToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIgnoreNextNavigation;
    private boolean mIgnoreNextClose;
    private boolean mIgnoreNextCollapse;
    private boolean mSearchExpanded;
    private RootsCache mRoots;
    private State mState;
    @Override
    public void onCreate(Bundle icicle) {
        if(Utils.hasLollipop()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        else if(Utils.hasKitKat()){
            setTheme(R.style.DocumentsTheme_Translucent);
        }
        setUpStatusBar();

        super.onCreate(icicle);
        mRoots = DocumentsApplication.getRootsCache(this);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity);
        final Context context = this;
        final Resources res = getResources();

        final WindowManager.LayoutParams a = getWindow().getAttributes();
        final Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);

        getWindow().setAttributes(a);
        if (icicle != null) {
            mState = icicle.getParcelable(EXTRA_STATE);
        } else {
            buildDefaultState();
        }
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextAppearance(context,
                android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
        mToolbarStack = (Spinner) findViewById(R.id.stack);
        mToolbarStack.setOnItemSelectedListener(mStackListener);
        mRootsToolbar = (Toolbar) findViewById(R.id.roots_toolbar);
        if (mRootsToolbar != null) {
            mRootsToolbar.setTitleTextAppearance(context,
                    android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
        }
        setSupportActionBar(mToolbar);
        
        // Handle incoming file from external apps
        final Intent intent = getIntent();
        if (intent != null && (Intent.ACTION_VIEW.equals(intent.getAction()) || 
                               Intent.ACTION_SEND.equals(intent.getAction()) ||
                               Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))) {
            handleExternalFileIntent(intent);
            return;
        }
        
        RootsCommonFragment.show(getSupportFragmentManager(), null);
        if (!mState.restored) {
            new RestoreStackTask().execute();
        } else {
            onCurrentDirectoryChanged(ANIM_NONE);
        }
    }

    @Override
    public String getTag() {
        return null;
    }

    private void buildDefaultState() {
        mState = new State();
        final Intent intent = getIntent();
        mState.action = State.ACTION_MANAGE_ALL;
        mState.acceptMimes = new String[] { "*/*" };
        mState.allowMultiple = true;
        mState.acceptMimes = new String[] { intent.getType() != null ? intent.getType() : "*/*" };
        mState.localOnly = intent.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false);
        mState.forceAdvanced = intent.getBooleanExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, false);
        mState.showAdvanced = mState.forceAdvanced
                | SettingsActivity.getDisplayAdvancedDevices(this);
        mState.showSize = true;
    }
    private class RestoreRootTask extends AsyncTask<Void, Void, RootInfo> {
        private Uri mRootUri;
        public RestoreRootTask(Uri rootUri) {
            mRootUri = rootUri;
        }
        @Override
        protected RootInfo doInBackground(Void... params) {
            final String rootId = DocumentsContract.getRootId(mRootUri);
            return mRoots.getRootOneshot(mRootUri.getAuthority(), rootId);
        }
        @Override
        protected void onPostExecute(RootInfo root) {
            if (isDestroyed()) return;
            mState.restored = true;
            if (root != null) {
                onRootPicked(root, true);
            } else {
                Log.w(TAG, "Failed to find root: " + mRootUri);
                finish();
            }
        }
    }
    private class RestoreStackTask extends AsyncTask<Void, Void, Void> {
        private volatile boolean mRestoredStack;
        private volatile boolean mExternal;
        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... params) {
            final String packageName = getCallingPackageMaybeExtra();
            if (packageName != null) {
                final Cursor cursor = getContentResolver()
                        .query(RecentsProvider.buildResume(packageName), null, null, null, null);
                try {
                    if (null != cursor && cursor.moveToFirst()) {
                        mExternal = cursor.getInt(cursor.getColumnIndex(ResumeColumns.EXTERNAL)) != 0;
                        final byte[] rawStack = cursor.getBlob(
                                cursor.getColumnIndex(ResumeColumns.STACK));
                        DurableUtils.readFromArray(rawStack, mState.stack);
                        mRestoredStack = true;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to resume: " + e);
                } finally {
                    IoUtils.closeQuietly(cursor);
                }
            }
            if (mRestoredStack) {

                final Collection<RootInfo> matchingRoots = mRoots.getMatchingRootsBlocking(mState);
                try {
                    mState.stack.updateRoot(matchingRoots);
                    mState.stack.updateDocuments(getContentResolver());
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Failed to restore stack: " + e);
                    mState.stack.reset();
                    mRestoredStack = false;
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            if (isDestroyed()) return;
            mState.restored = true;
            onCurrentDirectoryChanged(ANIM_NONE);
        }
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
        updateActionBar();
    }
    @Override
    public void setRootsDrawerOpen(boolean open) {
        Log.w(TAG, "Trying to change state of roots drawer to > " + (open ? "open" : "closed"));

    }
    public void updateActionBar() {
        final RootInfo root = getCurrentRoot();
        mToolbar.setNavigationIcon(
                root != null ? root.loadToolbarIcon(mToolbar.getContext()) : null);
        mToolbar.setNavigationContentDescription(R.string.drawer_open);
        mToolbar.setNavigationOnClickListener(null);
        if (mSearchExpanded) {
            mToolbar.setTitle(null);
            mToolbarStack.setVisibility(View.GONE);
            mToolbarStack.setAdapter(null);
        } else {
            if (mState.stack.size() <= 1) {
                mToolbar.setTitle(root.title);
                mToolbarStack.setVisibility(View.GONE);
                mToolbarStack.setAdapter(null);
            } else {
                mToolbar.setTitle(null);
                mToolbarStack.setVisibility(View.VISIBLE);
                mToolbarStack.setAdapter(mStackAdapter);
                mIgnoreNextNavigation = true;
                mToolbarStack.setSelection(mStackAdapter.getCount() - 1);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity, menu);
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                default:
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
        final MenuItem searchMenu = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchMenu.getActionView();
        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchExpanded = true;
                mState.currentSearch = query;
                mSearchView.clearFocus();
                onCurrentDirectoryChanged(ANIM_NONE);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchMenu.setOnActionExpandListener(new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchExpanded = true;
                updateActionBar();
                return true;
            }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchExpanded = false;
                if (mIgnoreNextCollapse) {
                    mIgnoreNextCollapse = false;
                    return true;
                }
                mState.currentSearch = null;
                onCurrentDirectoryChanged(ANIM_NONE);
                return true;
            }
        });
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mSearchExpanded = false;
                if (mIgnoreNextClose) {
                    mIgnoreNextClose = false;
                    return false;
                }
                mState.currentSearch = null;
                onCurrentDirectoryChanged(ANIM_NONE);
                return false;
            }
        });
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final FragmentManager fm = getSupportFragmentManager();
        final RootInfo root = getCurrentRoot();
        final DocumentInfo cwd = getCurrentDirectory();
        final MenuItem createDir = menu.findItem(R.id.menu_create_dir);
        final MenuItem search = menu.findItem(R.id.menu_search);
        final MenuItem sort = menu.findItem(R.id.menu_sort);
        final MenuItem sortSize = menu.findItem(R.id.menu_sort_size);
        final MenuItem grid = menu.findItem(R.id.menu_grid);
        final MenuItem list = menu.findItem(R.id.menu_list);
        sort.setVisible(cwd != null);
        grid.setVisible(mState.derivedMode != State.MODE_GRID);
        list.setVisible(mState.derivedMode != State.MODE_LIST);
        if (mState.currentSearch != null) {

            sort.setVisible(false);
            search.expandActionView();
            mSearchView.setIconified(false);
            mSearchView.clearFocus();
            mSearchView.setQuery(mState.currentSearch, false);
        } else {
            mIgnoreNextClose = true;
            mSearchView.setIconified(true);
            mSearchView.clearFocus();
            mIgnoreNextCollapse = true;
            search.collapseActionView();
        }

        sortSize.setVisible(mState.showSize);
        search.setVisible(true);
        createDir.setVisible(true);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_create_dir) {
            CreateDirectoryFragment.show(getSupportFragmentManager());
            return true;
        } else if (id == R.id.menu_search) {
            return false;
        } else if (id == R.id.menu_sort_name) {
            setUserSortOrder(State.SORT_ORDER_DISPLAY_NAME);
            return true;
        } else if (id == R.id.menu_sort_date) {
            setUserSortOrder(State.SORT_ORDER_LAST_MODIFIED);
            return true;
        } else if (id == R.id.menu_sort_size) {
            setUserSortOrder(State.SORT_ORDER_SIZE);
            return true;
        } else if (id == R.id.menu_grid) {
            setUserMode(State.MODE_GRID);
            return true;
        } else if (id == R.id.menu_list) {
            setUserMode(State.MODE_LIST);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStateChanged() {
        supportInvalidateOptionsMenu();
    }
    /**
     * Set state sort order based on explicit user action.
     */
    private void setUserSortOrder(int sortOrder) {
        mState.userSortOrder = sortOrder;
        Fragment fragment = DirectoryFragment.get(getSupportFragmentManager());
        if(fragment instanceof DirectoryFragment) {
            final DirectoryFragment directory = (DirectoryFragment) fragment;
            directory.onUserSortOrderChanged();
        }
    }
    /**
     * Set state mode based on explicit user action.
     */
    private void setUserMode(int mode) {
        mState.userMode = mode;
        Fragment fragment = DirectoryFragment.get(getSupportFragmentManager());
        if(fragment instanceof DirectoryFragment) {
            final DirectoryFragment directory = (DirectoryFragment) fragment;
            directory.onUserModeChanged();
        }
    }
    @Override
    public void setPending(boolean pending) {
        final SaveFragment save = SaveFragment.get(getSupportFragmentManager());
        if (save != null) {
            save.setPending(pending);
        }
    }
    @Override
    public void onBackPressed() {
        if (!mState.stackTouched) {
            super.onBackPressed();
            return;
        }
        final int size = mState.stack.size();
        if (size > 1) {
            mState.stack.pop();
            onCurrentDirectoryChanged(ANIM_UP);
        } else {
            super.onBackPressed();
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable(EXTRA_STATE, mState);
    }
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }
    private BaseAdapter mStackAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mState.stack.size();
        }
        @Override
        public DocumentInfo getItem(int position) {
            return mState.stack.get(mState.stack.size() - position - 1);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_subdir_title, parent, false);
            }
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final DocumentInfo doc = getItem(position);
            if (position == 0) {
                final RootInfo root = getCurrentRoot();
                title.setText(root.title);
            } else {
                title.setText(doc.displayName);
            }
            return convertView;
        }
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_subdir, parent, false);
            }
            final ImageView subdir = (ImageView) convertView.findViewById(R.id.subdir);
            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final DocumentInfo doc = getItem(position);
            if (position == 0) {
                final RootInfo root = getCurrentRoot();
                title.setText(root.title);
                subdir.setVisibility(View.GONE);
            } else {
                title.setText(doc.displayName);
                subdir.setVisibility(View.VISIBLE);
            }
            return convertView;
        }
    };
    private OnItemSelectedListener mStackListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mIgnoreNextNavigation) {
                mIgnoreNextNavigation = false;
                return;
            }
            while (mState.stack.size() > position + 1) {
                mState.stackTouched = true;
                mState.stack.pop();
            }
            onCurrentDirectoryChanged(ANIM_UP);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
    @Override
    public RootInfo getCurrentRoot() {
        if (mState.stack.root != null) {
            return mState.stack.root;
        } else {
            return mRoots.getHomeRoot();
        }
    }

    public RootInfo getDownloadRoot() {
        return mRoots.getDownloadRoot();
    }

    @Override
    public boolean getActionMode() {
        return false;
    }

    @Override
    public void setActionMode(boolean actionMode) {
        if (actionMode) {
            lockNavigation();
        }
    }

    @Override
    public void setUpStatusBar() {

    }

    @Override
    public void setUpDefaultStatusBar() {

    }

    @Override
    public boolean isShowAsDialog() {
        return false;
    }

    @Override
    public void upadateActionItems(RecyclerView mCurrentView) {

    }

    @Override
    public void setInfoDrawerOpen(boolean open) {

    }

    @Override
    public void again() {

    }

    public DocumentInfo getCurrentDirectory() {
        return mState.stack.peek();
    }

    private String getCallingPackageMaybeExtra() {
        final String extra = getIntent().getStringExtra(DocumentsContract.EXTRA_PACKAGE_NAME);
        return (extra != null) ? extra : getCallingPackage();
    }
    public Executor getCurrentExecutor() {
        final DocumentInfo cwd = getCurrentDirectory();
        if (cwd != null && cwd.authority != null) {
            return ProviderExecutor.forAuthority(cwd.authority);
        } else {
            return AsyncTask.THREAD_POOL_EXECUTOR;
        }
    }
    @Override
    public State getDisplayState() {
        return mState;
    }

    private void onCurrentDirectoryChanged(int anim) {
        if (isNavigationLocked()) {
            return;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final RootInfo root = getCurrentRoot();
        final DocumentInfo cwd = getCurrentDirectory();
        if (cwd == null) {
            DirectoryFragment.showRecentsOpen(fm, anim, root);

            final boolean visualMimes = MimePredicate.mimeMatches(
                    MimePredicate.VISUAL_MIMES, mState.acceptMimes);
            mState.userMode = visualMimes ? State.MODE_GRID : State.MODE_LIST;
            mState.derivedMode = mState.userMode;
        } else {
            if (mState.currentSearch != null) {

                DirectoryFragment.showSearch(fm, root, cwd, mState.currentSearch, anim);
            } else {

                DirectoryFragment.showNormal(fm, root, cwd, anim);
            }
        }
        final RootsCommonFragment roots = RootsCommonFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }
        updateActionBar();
        supportInvalidateOptionsMenu();
        dumpStack();
    }
    @Override
    public void onStackPicked(DocumentStack stack) {
        if (isNavigationLocked()) {
            return;
        }
        try {

            stack.updateDocuments(getContentResolver());
            mState.stack = stack;
            mState.stackTouched = true;
            onCurrentDirectoryChanged(ANIM_SIDE);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to restore stack: " + e);
        }
    }
    @Override
    public void onRootPicked(RootInfo root, boolean closeDrawer) {
        if (isNavigationLocked()) {
            return;
        }

        mState.stack.root = root;
        mState.stack.clear();
        mState.stackTouched = true;
        if (!mRoots.isRecentsRoot(root)) {
            new PickRootTask(root).executeOnExecutor(getCurrentExecutor());
        } else {
            onCurrentDirectoryChanged(ANIM_SIDE);
        }
    }
    private class PickRootTask extends AsyncTask<Void, Void, DocumentInfo> {
        private RootInfo mRoot;
        public PickRootTask(RootInfo root) {
            mRoot = root;
        }
        @Override
        protected DocumentInfo doInBackground(Void... params) {
            try {
                final Uri uri = DocumentsContract.buildDocumentUri(
                        mRoot.authority, mRoot.documentId);
                return DocumentInfo.fromUri(getContentResolver(), uri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Failed to find root", e);
                return null;
            }
        }
        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (result != null) {
                mState.stack.push(result);
                mState.stackTouched = true;
                onCurrentDirectoryChanged(ANIM_SIDE);
            }
        }
    }
    @Override
    public void onAppPicked(ResolveInfo info) {
        final Intent intent = new Intent(getIntent());
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.setComponent(new ComponentName(
                info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
        startActivityForResult(intent, CODE_FORWARD);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult() code=" + resultCode);


        if (requestCode == CODE_FORWARD && resultCode != RESULT_CANCELED) {

            final String packageName = getCallingPackageMaybeExtra();
            final ContentValues values = new ContentValues();
            values.put(ResumeColumns.EXTERNAL, 1);
            getContentResolver().insert(RecentsProvider.buildResume(packageName), values);

            setResult(resultCode, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    @Override
    public void onDocumentPicked(DocumentInfo doc) {
        if (isNavigationLocked()) {
            return;
        }
        final FragmentManager fm = getSupportFragmentManager();
        if (doc.isDirectory()) {
            mState.stack.push(doc);
            mState.stackTouched = true;
            onCurrentDirectoryChanged(ANIM_DOWN);
        } else {

            final Intent view = new Intent(Intent.ACTION_VIEW);
            view.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            view.setData(doc.derivedUri);
            try {
                startActivity(view);
            } catch (ActivityNotFoundException ex2) {
                Toast.makeText(this, R.string.toast_no_application, Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void onDocumentsPicked(List<DocumentInfo> docs) {

    }
    @Override
    public void onSaveRequested(DocumentInfo replaceTarget) {
        new ExistingFinishTask(replaceTarget.derivedUri).executeOnExecutor(getCurrentExecutor());
    }
    @Override
    public void onSaveRequested(String mimeType, String displayName) {
        new CreateFinishTask(mimeType, displayName).executeOnExecutor(getCurrentExecutor());
    }

    @Override
    public boolean isCreateSupported() {
        return false;
    }

    @Override
    public void onPickRequested(DocumentInfo pickTarget) {
        final Uri viaUri = DocumentsContract.buildTreeDocumentUri(pickTarget.authority,
                pickTarget.documentId);
        new PickFinishTask(viaUri).executeOnExecutor(getCurrentExecutor());
    }
    private void saveStackBlocking() {
        final ContentResolver resolver = getContentResolver();
        final ContentValues values = new ContentValues();
        final byte[] rawStack = DurableUtils.writeToArrayOrNull(mState.stack);

        final String packageName = getCallingPackageMaybeExtra();
        values.clear();
        values.put(ResumeColumns.STACK, rawStack);
        values.put(ResumeColumns.EXTERNAL, 0);
        resolver.insert(RecentsProvider.buildResume(packageName), values);
    }
    private void onFinished(Uri... uris) {
        Log.d(TAG, "onFinished() " + Arrays.toString(uris));
        final Intent intent = new Intent();
        if (uris.length == 1) {
            intent.setData(uris[0]);
        } else if (uris.length > 1) {
            final ClipData clipData = new ClipData(
                    null, mState.acceptMimes, new ClipData.Item(uris[0]));
            for (int i = 1; i < uris.length; i++) {
                clipData.addItem(new ClipData.Item(uris[i]));
            }
            if(Utils.hasJellyBean()){
                intent.setClipData(clipData);
            }
            else{
                intent.setData(uris[0]);
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
    private class CreateFinishTask extends AsyncTask<Void, Void, Uri> {
        private final String mMimeType;
        private final String mDisplayName;
        public CreateFinishTask(String mimeType, String displayName) {
            mMimeType = mimeType;
            mDisplayName = displayName;
        }
        @Override
        protected void onPreExecute() {
            setPending(true);
        }
        @Override
        protected Uri doInBackground(Void... params) {
            final ContentResolver resolver = getContentResolver();
            final DocumentInfo cwd = getCurrentDirectory();
            ContentProviderClient client = null;
            Uri childUri = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(
                        resolver, cwd.derivedUri.getAuthority());

                childUri = DocumentsContract.createDocument(
                        resolver, cwd.derivedUri, mMimeType, mDisplayName);
            } catch (Exception e) {
                Log.w(TAG, "Failed to create document", e);
            } finally {
                ContentProviderClientCompat.releaseQuietly(client);
            }
            if (childUri != null) {
                saveStackBlocking();
            }
            return childUri;
        }
        @Override
        protected void onPostExecute(Uri result) {
            if (result != null) {
                onFinished(result);
            } else {
                Toast.makeText(StandaloneActivity.this, R.string.save_error, Toast.LENGTH_SHORT)
                        .show();
            }
            setPending(false);
        }
    }
    private class ExistingFinishTask extends AsyncTask<Void, Void, Void> {
        private final Uri[] mUris;
        public ExistingFinishTask(Uri... uris) {
            mUris = uris;
        }
        @Override
        protected Void doInBackground(Void... params) {
            saveStackBlocking();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            onFinished(mUris);
        }
    }
    private class PickFinishTask extends AsyncTask<Void, Void, Void> {
        private final Uri mUri;
        public PickFinishTask(Uri uri) {
            mUri = uri;
        }
        @Override
        protected Void doInBackground(Void... params) {
            saveStackBlocking();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            onFinished(mUri);
        }
    }
    private void dumpStack() {
        Log.d(TAG, "Current stack: ");
        Log.d(TAG, " * " + mState.stack.root);
        for (DocumentInfo doc : mState.stack) {
            Log.d(TAG, " +-- " + doc);
        }
    }
    
    private void handleExternalFileIntent(Intent intent) {
        try {
            Uri fileUri = null;
            
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                fileUri = intent.getData();
            } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
                fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
            
            if (fileUri != null) {
                // Get the file path from URI
                String filePath = getFilePathFromUri(fileUri);
                if (filePath != null) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        openFileDirectly(file);
                        return;
                    }
                }
                
                // If we can't get file path, try to open URI directly
                openUriDirectly(fileUri);
            } else {
                Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling external file intent", e);
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private String getFilePathFromUri(Uri uri) {
        if (uri == null) return null;
        
        // Handle file:// URIs
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        
        // Handle content:// URIs
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try {
                String[] projection = {android.provider.MediaStore.MediaColumns.DATA};
                android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA);
                            return cursor.getString(columnIndex);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file path from content URI", e);
            }
        }
        
        return null;
    }
    
    private void openFileDirectly(File file) {
        try {
            String fileName = file.getName().toLowerCase();
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getPackageName() + ".provider", 
                file
            );
            
            // Determine mime type
            String mimeType = getMimeTypeFromFile(fileName);
            
            // Internal Player Routing (Video/Audio)
            if (isVideoFile(fileName) || mimeType.startsWith("video/") || mimeType.startsWith("audio/") || fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                 Intent intent = new Intent(this, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.VideoPlayerActivity.class);
                 intent.setData(fileUri);
                 intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 startActivity(intent);
                 finish();
                 return;
            }
            
            // Internal Image Viewer
            if (mimeType.startsWith("image/")) {
                 Intent intent = new Intent(this, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.ImageViewerActivity.class);
                 intent.setData(fileUri);
                 intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 startActivity(intent);
                 finish();
                 return;
            }
            
            // Open with appropriate app, excluding ourselves
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(fileUri, mimeType);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            String myPackage = getPackageName();
            List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(viewIntent, 0);
            
            if (!resInfo.isEmpty()) {
                    java.util.List<Intent> targetedIntents = new java.util.ArrayList<>();
                    for (ResolveInfo info : resInfo) {
                        if (!info.activityInfo.packageName.equals(myPackage)) {
                            Intent i = new Intent(viewIntent);
                            i.setPackage(info.activityInfo.packageName);
                            targetedIntents.add(i);
                        }
                    }

                    if (!targetedIntents.isEmpty()) {
                        Intent chooserIntent = Intent.createChooser(targetedIntents.remove(0), "Open with");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toArray(new Intent[0]));
                        startActivity(chooserIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "No external app found to open this file", Toast.LENGTH_SHORT).show();
                        finish();
                    }
            } else {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file directly", e);
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void openUriDirectly(Uri uri) {
        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setData(uri);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (viewIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(viewIntent);
            } else {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error opening URI directly", e);
            Toast.makeText(this, "Error opening file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void openVideoWithExternalPlayer(Uri uri, File file) {
        // Try popular video players first
        String[] videoPlayers = {
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro", 
            "org.videolan.vlc",
            "com.brouken.player"
        };
        
        for (String packageName : videoPlayers) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/*");
                intent.setPackage(packageName);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return;
            } catch (Exception e) {
                // Player not installed, try next
            }
        }
        
        // No specific player found, exclude ourselves and show chooser
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Exclude our own package to prevent loop if we are the default
            String myPackage = getPackageName();
            List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(intent, 0);
            
            if (!resInfo.isEmpty()) {
                java.util.List<Intent> targetedIntents = new java.util.ArrayList<>();
                for (ResolveInfo info : resInfo) {
                    if (!info.activityInfo.packageName.equals(myPackage)) {
                        Intent i = new Intent(intent);
                        i.setPackage(info.activityInfo.packageName);
                        targetedIntents.add(i);
                    }
                }
                
                if (!targetedIntents.isEmpty()) {
                    Intent chooserIntent = Intent.createChooser(targetedIntents.remove(0), "Open Video");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toArray(new Intent[0]));
                    startActivity(chooserIntent);
                    finish();
                } else {
                    Toast.makeText(this, "No external video player found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "No video player found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "No video player found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private boolean isVideoFile(String fileName) {
        return fileName.endsWith(".mp4") || fileName.endsWith(".avi") || 
               fileName.endsWith(".mkv") || fileName.endsWith(".mov") ||
               fileName.endsWith(".wmv") || fileName.endsWith(".flv") ||
               fileName.endsWith(".webm") || fileName.endsWith(".3gp") ||
               fileName.endsWith(".m4v") || fileName.endsWith(".ts");
    }
    
    private String getMimeTypeFromFile(String fileName) {
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".avi")) return "video/x-msvideo";
        if (fileName.endsWith(".mkv")) return "video/x-matroska";
        if (fileName.endsWith(".mov")) return "video/quicktime";
        if (fileName.endsWith(".webm")) return "video/webm";
        if (fileName.endsWith(".3gp")) return "video/3gpp";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".m4a")) return "audio/mp4";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".zip")) return "application/zip";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) return "application/msword";
        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) return "application/vnd.ms-excel";
        return "*/*";
    }
    
    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }
}