package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx;

import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_BROWSE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_CREATE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_GET_CONTENT;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_MANAGE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_OPEN;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.ACTION_OPEN_TREE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.MODE_GRID;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BaseActivity.State.MODE_LIST;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.DirectoryFragment.ANIM_DOWN;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.DirectoryFragment.ANIM_NONE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.DirectoryFragment.ANIM_SIDE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.DirectoryFragment.ANIM_UP;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AnalyticsManager.FILE_COUNT;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AnalyticsManager.FILE_MOVE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AnalyticsManager.FILE_TYPE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.SAFManager.ADD_STORAGE_REQUEST_CODE;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.SecurityHelper.REQUEST_CONFIRM_CREDENTIALS;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils.EXTRA_ROOT;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.ExternalStorageProvider.isDownloadAuthority;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudrail.si.CloudRail;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.archive.DocumentArchiveHelper;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.cast.CastUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.cast.Casty;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.common.RootsCommonFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.ConnectionsFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.CreateDirectoryFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.CreateFileFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.DirectoryFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.HomeFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.MoveFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.PickFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.QueueFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.RecentsCreateFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.SaveFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.ServerFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.TransferFragment;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.libcore.io.IoUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AnalyticsManager;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AppRate;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.AsyncTask;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ConnectionUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ContentProviderClientCompat;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.CrashReportingManager;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.FileUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.IntentUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.MimePredicate;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.PermissionUtil;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.ProviderExecutor;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.RootsCache;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.SAFManager;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.SecurityHelper;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.SystemBarTintManager;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DocumentInfo;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DocumentStack;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DocumentsContract;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DocumentsContract.Root;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.DurableUtils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.RootInfo;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.network.NetworkConnection;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.MediaDocumentsProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.RecentsProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.RecentsProvider.RecentColumns;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.RecentsProvider.ResumeColumns;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.setting.SettingsActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.TransferHelper;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.DirectoryContainerView;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.DrawerLayoutHelper;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.FloatingActionsMenu;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.ui.fabs.SimpleMenuListenerAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;



public class DocumentsActivity extends BaseActivity implements MenuItem.OnMenuItemClickListener {
    private static final String EXTRA_STATE = "state";
    private static final String EXTRA_AUTHENTICATED = "authenticated";
    private static final String EXTRA_ACTIONMODE = "actionmode";
    private static final String EXTRA_SEARCH_STATE = "searchsate";
    public static final String BROWSABLE = "android.intent.category.BROWSABLE";
    private static final int UPLOAD_FILE = 99;

    private static final int CODE_FORWARD = 42;
    private static final int CODE_SETTINGS = 92;

    private boolean mInAnalysis = false;
    private String mAnalysisTitle = "Analysis";
    private static final boolean SHOW_NATIVE_ADS = false;

    private boolean mShowAsDialog;

    private SearchView mSearchView;

    private Toolbar mToolbar;
    private Spinner mToolbarStack;

    private DrawerLayoutHelper mDrawerLayoutHelper;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mRootsContainer;
    private View mInfoContainer;

    private DirectoryContainerView mDirectoryContainer;

    private boolean mIgnoreNextNavigation;
    private boolean mIgnoreNextClose;
    private boolean mIgnoreNextCollapse;

    private boolean mSearchExpanded;
    private boolean mSearchResultShown;

    private RootsCache mRoots;
    private State mState;
    private boolean mAuthenticated;
    private FrameLayout mRateContainer;
    private boolean mActionMode;
    private FloatingActionsMenu mActionMenu;
    private RootInfo mParentRoot;
    private boolean SAFPermissionRequested;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle icicle) {
        setTheme(R.style.DocumentsTheme_Document);
        if (Utils.hasLollipop()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        } else if (Utils.hasKitKat()) {
            setTheme(R.style.DocumentsTheme_Translucent);
        }
        setUpStatusBar();


        super.onCreate(icicle);

        mRoots = DocumentsApplication.getRootsCache(this);

        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity);



        final Context context = this;
        final Resources res = getResources();
        mShowAsDialog = res.getBoolean(R.bool.show_as_dialog);

        mDirectoryContainer = (DirectoryContainerView) findViewById(R.id.container_directory);
        mRateContainer = (FrameLayout) findViewById(R.id.container_rate);

        initControls();

        if (icicle != null) {
            mState = icicle.getParcelable(EXTRA_STATE);
            mAuthenticated = icicle.getBoolean(EXTRA_AUTHENTICATED);
            mActionMode = icicle.getBoolean(EXTRA_ACTIONMODE);
        } else {
            buildDefaultState();
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextAppearance(context, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        if (Utils.hasKitKat() && !Utils.hasLollipop()) {
            ((LinearLayout.LayoutParams) mToolbar.getLayoutParams()).setMargins(0, getStatusBarHeight(this), 0, 0);
            mToolbar.setPadding(0, getStatusBarHeight(this), 0, 0);
        }


        mToolbarStack = (Spinner) findViewById(R.id.stack);
        mToolbarStack.setOnItemSelectedListener(mStackListener);

        setSupportActionBar(mToolbar);

        mRootsContainer = findViewById(R.id.drawer_roots);
        mInfoContainer = findViewById(R.id.container_info);

        if (!mShowAsDialog) {

            mDrawerLayoutHelper = new DrawerLayoutHelper(findViewById(R.id.drawer_layout));
            View view = findViewById(R.id.drawer_layout);
            if (view instanceof DrawerLayout) {
                DrawerLayout mDrawerLayout = (DrawerLayout) view;

                mDrawerToggle = new ActionBarDrawerToggle(
                        this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close);
                mDrawerLayout.addDrawerListener(mDrawerToggle);
                mDrawerToggle.syncState();
                lockInfoContainter();
            }
        }

        changeActionBarColor();


        if (mState.action == ACTION_MANAGE) {
            if (mShowAsDialog) {
                findViewById(R.id.container_roots).setVisibility(View.GONE);
            } else {
                mDrawerLayoutHelper.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }

        if (mState.action == ACTION_CREATE) {
            final String mimeType = getIntent().getType();
            final String title = getIntent().getStringExtra(IntentUtils.EXTRA_TITLE);
            SaveFragment.show(getSupportFragmentManager(), mimeType, title);
        } else if (mState.action == ACTION_OPEN_TREE) {
            PickFragment.show(getSupportFragmentManager());
        }

        if (mState.action == ACTION_BROWSE) {
            final Intent moreApps = new Intent(getIntent());
            moreApps.setComponent(null);
            moreApps.setPackage(null);
            RootsCommonFragment.show(getSupportFragmentManager(), moreApps);

            Log.e(" moreApps", "moreApps");
        } else if (mState.action == ACTION_OPEN || mState.action == ACTION_CREATE
                || mState.action == ACTION_GET_CONTENT || mState.action == ACTION_OPEN_TREE) {
            RootsCommonFragment.show(getSupportFragmentManager(), new Intent());

            Log.e(" RootsCommon", "RootsCommonFragment");
        }

        if (!mState.restored) {
            if (mState.action == ACTION_MANAGE) {
                final Uri rootUri = getIntent().getData();
                new RestoreRootTask(rootUri).executeOnExecutor(getCurrentExecutor());
            } else {
                if (isDownloadAuthority(getIntent())) {
                    onRootPicked(getDownloadRoot(), true);
                } else if (ConnectionUtils.isServerAuthority(getIntent())
                        || TransferHelper.isTransferAuthority(getIntent())) {
                    RootInfo root = getIntent().getExtras().getParcelable(EXTRA_ROOT);
                    onRootPicked(root, true);
                } else if (Utils.isQSTile(getIntent())) {
                    NetworkConnection networkConnection = NetworkConnection.getDefaultServer(this);
                    RootInfo root = mRoots.getRootInfo(networkConnection);
                    onRootPicked(root, true);
                } else {
                    try {
                        new RestoreStackTask().execute();
                    } catch (SQLiteFullException e) {
                        CrashReportingManager.logException(e);
                    }
                }
            }
        } else {
            onCurrentDirectoryChanged(ANIM_NONE);
        }

        if (!PermissionUtil.isStorageAccess(this)) {
            requestStoragePermissions();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (null != categories && categories.contains(BROWSABLE)) {
            try {


                CloudRail.setAuthenticationResponse(intent);
            } catch (Exception ignore) {
            }
        }
        super.onNewIntent(intent);
    }

    @Override
    public String getTag() {
        return null;
    }

    @Override
    public void again() {
        if (Utils.hasMarshmallow()) {
            RootsCache.updateRoots(this);
            mRoots = DocumentsApplication.getRootsCache(this);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRoots.updateAsync();
                    final RootInfo root = getCurrentRoot();
                    if (root.isHome()) {
                        HomeFragment homeFragment = HomeFragment.get(getSupportFragmentManager());
                        if (null != homeFragment) {
                            homeFragment.reloadData();
                        }
                    }
                }
            }, 500);
        }
    }

    private void lockInfoContainter() {
        if (mDrawerLayoutHelper.isDrawerOpen(mInfoContainer)) {
            mDrawerLayoutHelper.closeDrawer(mInfoContainer);
        }

        mDrawerLayoutHelper.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mInfoContainer);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public int getGravity() {
        if (Utils.hasJellyBeanMR1()) {
            Configuration config = getResources().getConfiguration();
            if (config.getLayoutDirection() != View.LAYOUT_DIRECTION_LTR) {
                return Gravity.LEFT;
            }
        }
        return Gravity.RIGHT;
    }

    public static boolean isRTL(Locale locale) {
        final int directionality = Character.getDirectionality(locale.getDisplayName().charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    private void initProtection() {

        if (mAuthenticated || !SettingsActivity.isSecurityEnabled(this)) {
            return;
        }

        if (Utils.hasMarshmallow()) {
            SecurityHelper securityHelper = new SecurityHelper(this);
            securityHelper.authenticate("AnExplorer", "Use device pattern to continue");
        }
    }

    private void buildDefaultState() {
        mState = new State();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (IntentUtils.ACTION_OPEN_DOCUMENT.equals(action)) {
            mState.action = ACTION_OPEN;
        } else if (IntentUtils.ACTION_CREATE_DOCUMENT.equals(action)) {
            mState.action = ACTION_CREATE;
        } else if (IntentUtils.ACTION_GET_CONTENT.equals(action)) {
            mState.action = ACTION_GET_CONTENT;
        } else if (IntentUtils.ACTION_OPEN_DOCUMENT_TREE.equals(action)) {
            mState.action = ACTION_OPEN_TREE;
        } else if (DocumentsContract.ACTION_MANAGE_ROOT.equals(action)) {

            mState.action = ACTION_BROWSE;
            Log.e(" mState.1", "ACTION_BROWSE");

        } else {

            mState.action = ACTION_BROWSE;
            Log.e(" mState.2", "ACTION_BROWSE");
        }

        if (mState.action == ACTION_OPEN || mState.action == ACTION_GET_CONTENT) {
            mState.allowMultiple = intent.getBooleanExtra(IntentUtils.EXTRA_ALLOW_MULTIPLE, false);
        }

        if (mState.action == ACTION_GET_CONTENT || mState.action == ACTION_BROWSE) {
            mState.acceptMimes = new String[]{"*/*"};
            mState.allowMultiple = true;
        } else if (intent.hasExtra(IntentUtils.EXTRA_MIME_TYPES)) {
            mState.acceptMimes = intent.getStringArrayExtra(IntentUtils.EXTRA_MIME_TYPES);
        } else {
            mState.acceptMimes = new String[]{intent.getType()};
        }

        mState.localOnly = intent.getBooleanExtra(IntentUtils.EXTRA_LOCAL_ONLY, true);
        mState.forceAdvanced = intent.getBooleanExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, false);
        mState.showAdvanced = mState.forceAdvanced
                | SettingsActivity.getDisplayAdvancedDevices(this);

        mState.rootMode = SettingsActivity.getRootMode(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (menuAction(item)) {
            closeDrawer();
            return true;
        }
        return false;
    }

    public void closeDrawer() {
        mDrawerLayoutHelper.closeDrawer(Utils.getActionDrawer(this));
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
            if (!Utils.isActivityAlive(DocumentsActivity.this)) {
                return;
            }
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
                CrashReportingManager.logException(e);
            } finally {
                IoUtils.closeQuietly(cursor);
            }

            if (mRestoredStack) {

                final Collection<RootInfo> matchingRoots = mRoots.getMatchingRootsBlocking(mState);
                try {
                    mState.stack.updateRoot(matchingRoots);
                    mState.stack.updateDocuments(getContentResolver());
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Failed to restore stack: " + e);
                    CrashReportingManager.logException(e);
                    mState.stack.reset();
                    mRestoredStack = false;
                }
            } else {
                RootInfo root = getCurrentRoot();
                if (null == root) {
                    return null;
                }
                final Uri uri = DocumentsContract.buildDocumentUri(root.authority, root.documentId);
                DocumentInfo result;
                try {
                    result = DocumentInfo.fromUri(getContentResolver(), uri);
                    if (result != null) {
                        mState.stack.push(result);
                        mState.stackTouched = true;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    CrashReportingManager.logException(e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!Utils.isActivityAlive(DocumentsActivity.this)) {
                return;
            }
            mState.restored = true;





            boolean showDrawer = false;
            if (!mRestoredStack) {
                showDrawer = false;
            }
            if (MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mState.acceptMimes)) {
                showDrawer = false;
            }
            if (mExternal && (mState.action == ACTION_GET_CONTENT || mState.action == ACTION_BROWSE)) {
                showDrawer = false;
            }

            if (showDrawer) {
                setRootsDrawerOpen(true);
            }

            onCurrentDirectoryChanged(ANIM_NONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeActionBarColor();
        if (mState.action == ACTION_MANAGE) {
            mState.showSize = true;
            mState.showFolderSize = false;
            mState.showThumbnail = true;
        } else {
            mState.showSize = SettingsActivity.getDisplayFileSize(this);
            mState.showFolderSize = SettingsActivity.getDisplayFolderSize(this);
            mState.showThumbnail = SettingsActivity.getDisplayFileThumbnail(this);
            mState.showHiddenFiles = SettingsActivity.getDisplayFileHidden(this);
            invalidateMenu();
        }
        initProtection();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        updateActionBar();
    }

    public void setRootsDrawerOpen(boolean open) {
        if (!mShowAsDialog) {
            if (open) {
                mDrawerLayoutHelper.openDrawer(mRootsContainer);
            } else {
                mDrawerLayoutHelper.closeDrawer(mRootsContainer);
            }
        }
    }

    public void setInfoDrawerOpen(boolean open) {
        if (!mShowAsDialog) {
            setRootsDrawerOpen(false);
            if (open) {
                mDrawerLayoutHelper.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mInfoContainer);
                mDrawerLayoutHelper.openDrawer(mInfoContainer);
            } else {
                lockInfoContainter();
            }
        }
    }

    private boolean isRootsDrawerOpen() {
        if (mShowAsDialog) {
            return false;
        } else {
            return mDrawerLayoutHelper.isDrawerOpen(mRootsContainer);
        }
    }

    public void setAnalysisMode(boolean active) {
        setAnalysisMode(active, "Analysis");
    }

    public void setAnalysisMode(boolean active, String title) {
        mInAnalysis = active;
        if (active && title != null) {
            mAnalysisTitle = title;
        }
        updateActionBar();
    }

    public void updateActionBar() {
        if (mInAnalysis) {
            setTitle(mAnalysisTitle);
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
                mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            }
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            mToolbarStack.setVisibility(View.GONE);
            return;
        }

        final RootInfo root = getCurrentRoot();

        final boolean showIndicator = !mShowAsDialog && (mState.action != ACTION_MANAGE);
        if (mShowAsDialog) {


            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbar.setNavigationContentDescription(R.string.drawer_open);

        if (mSearchExpanded) {
            setTitle(null);
            mToolbarStack.setVisibility(View.GONE);
            mToolbarStack.setAdapter(null);
        } else {
            if (mState.stack.size() <= 1) {
                if (null != root) {
                    if (root.isHome()) {
                        setTitle("Home");
                        if (mDrawerToggle != null) {
                            mDrawerToggle.setDrawerIndicatorEnabled(true);
                        }
                        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setRootsDrawerOpen(!isRootsDrawerOpen());
                            }
                        });
                    } else {
                        setTitle(root.title);
                        if (mDrawerToggle != null) {
                            mDrawerToggle.setDrawerIndicatorEnabled(false);
                            mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
                        }
                        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onBackPressed();
                            }
                        });
                    }
                    AnalyticsManager.setCurrentScreen(this, root.derivedTag);
                }
                mToolbarStack.setVisibility(View.GONE);
                mToolbarStack.setAdapter(null);
            } else {
                setTitle(null);
                if (mDrawerToggle != null) {
                    mDrawerToggle.setDrawerIndicatorEnabled(false);
                    mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
                }
                mToolbarStack.setVisibility(View.VISIBLE);
                mToolbarStack.setAdapter(mStackAdapter);
                mIgnoreNextNavigation = true;
                mToolbarStack.setSelection(mStackAdapter.getCount() - 1);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
            }
        }
    }

    public void setTitle(String title) {
        mToolbar.setTitle(title);
        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity, menu);

        final MenuItem searchMenu = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchMenu.getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchExpanded = mSearchResultShown = true;
                mState.currentSearch = query;
                mSearchView.clearFocus();
                onCurrentDirectoryChanged(ANIM_NONE);
                Bundle params = new Bundle();
                params.putString("query", query);
                AnalyticsManager.logEvent("search", getCurrentRoot(), params);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchExpanded = true;
                updateActionBar();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchExpanded = mSearchResultShown = false;
                if (mIgnoreNextCollapse) {
                    mIgnoreNextCollapse = false;
                    updateActionBar();
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
                mSearchExpanded = mSearchResultShown = false;
                if (mIgnoreNextClose) {
                    mIgnoreNextClose = false;
                    updateActionBar();
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
        updateMenuItems(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    public void updateMenuItems(Menu menu) {
        final FragmentManager fm = getSupportFragmentManager();
        final RootInfo root = getCurrentRoot();
        final DocumentInfo cwd = getCurrentDirectory();


        final MenuItem search = menu.findItem(R.id.menu_search);
        final MenuItem sort = menu.findItem(R.id.menu_sort);
        final MenuItem sortSize = menu.findItem(R.id.menu_sort_size);
        final MenuItem grid = menu.findItem(R.id.menu_grid);
        final MenuItem list = menu.findItem(R.id.menu_list);
        final MenuItem settings = menu.findItem(R.id.menu_settings);
        final MenuItem support = menu.findItem(R.id.menu_support);





        if (isRootsDrawerOpen()) {
            search.setVisible(false);
            sort.setVisible(false);
            grid.setVisible(false);
            list.setVisible(false);
            mIgnoreNextCollapse = true;
            search.collapseActionView();
            return;
        }

        sort.setVisible(cwd != null);
        if (root != null && root.isHome()) {
        grid.setVisible(false);
        list.setVisible(false);
    } else {
        grid.setVisible(!RootInfo.isOtherRoot(getCurrentRoot()) && mState.derivedMode != MODE_GRID);
        list.setVisible(mState.derivedMode != MODE_LIST);
    }

        if (mState.currentSearch != null) {



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

        final boolean searchVisible;
        if (mState.action == ACTION_CREATE || mState.action == ACTION_OPEN_TREE) {
            searchVisible = false;


            if (cwd == null) {
                grid.setVisible(false);
                list.setVisible(false);
            }
            if (mState.action == State.ACTION_CREATE) {
                if (null != SaveFragment.get(fm))
                    SaveFragment.get(fm).setSaveEnabled(cwd != null && cwd.isCreateSupported());
            }
        } else {
            searchVisible = root != null
                    && ((root.flags & Root.FLAG_SUPPORTS_SEARCH) != 0);

            if (null != SaveFragment.get(fm))
                SaveFragment.get(fm).setSaveEnabled(cwd != null && cwd.isCreateSupported());

            if (null != MoveFragment.get(fm))
                MoveFragment.get(fm).setSaveEnabled(cwd != null && cwd.isMoveSupported());
        }


        search.setVisible(searchVisible);

        settings.setVisible(mState.action != ACTION_MANAGE);

        Utils.inflateActionMenu(this, this, false, root, cwd);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null) {
            if (mDrawerLayoutHelper.isDrawerOpen(mInfoContainer)) {
                mDrawerLayoutHelper.closeDrawer(mInfoContainer);
            }
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        if (menuAction(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean menuAction(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_create_dir) {
            createFolder();
            return true;
        } else if (id == R.id.menu_create_file) {
            onStateChanged();
            createFile();
            return true;
        } else if (id == R.id.menu_search) {
            return false;
        } else if (id == R.id.menu_sort_name) {
            setUserSortOrder(State.SORT_ORDER_DISPLAY_NAME);
            Bundle params = new Bundle();
            params.putString("type", "name");
            AnalyticsManager.logEvent("sort_name", params);
            return true;
        } else if (id == R.id.menu_sort_date) {
            setUserSortOrder(State.SORT_ORDER_LAST_MODIFIED);
            Bundle params = new Bundle();
            params.putString("type", "modified");
            AnalyticsManager.logEvent("sort_modified", params);
            return true;
        } else if (id == R.id.menu_sort_size) {
            setUserSortOrder(State.SORT_ORDER_SIZE);
            Bundle params = new Bundle();
            params.putString("type", "size");
            AnalyticsManager.logEvent("sort_size", params);
            return true;
        } else if (id == R.id.menu_grid) {
            setUserMode(State.MODE_GRID);
            Bundle params = new Bundle();
            params.putString("type", "grid");
            AnalyticsManager.logEvent("display_grid", params);
            return true;
        } else if (id == R.id.menu_list) {
            setUserMode(State.MODE_LIST);
            Bundle params = new Bundle();
            params.putString("type", "list");
            AnalyticsManager.logEvent("display_list", params);
            return true;
        } else if (id == R.id.menu_settings) {
            startActivityForResult(new Intent(DocumentsActivity.this, SettingsActivity.class), CODE_SETTINGS);
            AnalyticsManager.logEvent("setting_open");
            return true;
        } else if (id == R.id.menu_about) {
            startActivity(new Intent(DocumentsActivity.this, AboutActivity.class));
            AnalyticsManager.logEvent("about_open");
            return true;
        } else if (id == R.id.menu_exit) {
            Bundle params = new Bundle();
            AnalyticsManager.logEvent("app_exit");
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }
        return false;
    }

    private void createFolder() {
        CreateDirectoryFragment.show(getSupportFragmentManager());
        Bundle params = new Bundle();
        params.putString(FILE_TYPE, "folder");
        AnalyticsManager.logEvent("create_folder", params);
    }

    private void createFile() {
        CreateFileFragment.show(getSupportFragmentManager(), "text/plain", "File");
        Bundle params = new Bundle();
        params.putString(FILE_TYPE, "file");
        AnalyticsManager.logEvent("create_file", params);
    }

    private void uploadFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            this.startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), UPLOAD_FILE);
        } catch (ActivityNotFoundException e) {
            Utils.showError(this, R.string.upload_error);
        }
        Bundle params = new Bundle();
        params.putString(FILE_TYPE, "file");
        AnalyticsManager.logEvent("upload_file", params);
    }

    /**
     * Update UI to reflect internal state changes not from user.
     */
    public void onStateChanged() {
        invalidateMenu();
    }

    /**
     * Set state sort order based on explicit user action.
     */
    private void setUserSortOrder(int sortOrder) {
        mState.userSortOrder = sortOrder;
        Fragment fragment = DirectoryFragment.get(getSupportFragmentManager());
        if (fragment instanceof DirectoryFragment) {
            final DirectoryFragment directory = (DirectoryFragment) fragment;
            if (directory != null) {
                directory.onUserSortOrderChanged();
            }
        }
    }

    /**
     * Set state mode based on explicit user action.
     */
    private void setUserMode(int mode) {
        mState.userMode = mode;
        Fragment fragment = DirectoryFragment.get(getSupportFragmentManager());
        if (fragment instanceof DirectoryFragment) {
            final DirectoryFragment directory = (DirectoryFragment) fragment;
            if (directory != null) {
                directory.onUserModeChanged();
            }
        }
    }

    /**
     * refresh Data currently shown
     */
    private void refreshData() {
        Fragment fragment = DirectoryFragment.get(getSupportFragmentManager());
        if (fragment instanceof DirectoryFragment) {
            final DirectoryFragment directory = (DirectoryFragment) fragment;
            if (directory != null) {
                directory.onUserSortOrderChanged();
            }
        }
    }


    public void setPending(boolean pending) {
        final SaveFragment save = SaveFragment.get(getSupportFragmentManager());
        if (save != null) {
            save.setPending(pending);
        }

        final RootInfo root = getCurrentRoot();
        if (root != null && (root.isRootedStorage() || root.isUsbStorage())) {
            refreshData();
        }
    }

    @Override
    public void onBackPressed() {
        if (mInAnalysis) {
            super.onBackPressed();
            return;
        }
        if (isRootsDrawerOpen() && !mShowAsDialog) {
            mDrawerLayoutHelper.closeDrawer(mRootsContainer);
            return;
        }
        if (mSearchExpanded) {

        }
        if (!mState.stackTouched) {
            super.onBackPressed();
            return;
        }

        final int size = mState.stack.size();
        if (size > 1) {
            mState.stack.pop();
            onCurrentDirectoryChanged(ANIM_UP);
        } else if (size == 1 && !isRootsDrawerOpen()) {

            if (null != mParentRoot) {
                onRootPicked(mParentRoot, true);
                mParentRoot = null;
                return;
            }

            final RootInfo current = getCurrentRoot();
            if (current != null && !current.isHome()) {
                onRootPicked(mRoots.getHomeRoot(), true);
                return;
            }

            super.onBackPressed();
        } else {
            if (null != mParentRoot) {
                onRootPicked(mParentRoot, true);
                mParentRoot = null;
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelable(EXTRA_STATE, mState);
        state.putBoolean(EXTRA_AUTHENTICATED, mAuthenticated);
        state.putBoolean(EXTRA_ACTIONMODE, mActionMode);
        state.putBoolean(EXTRA_SEARCH_STATE, mSearchResultShown);
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
            if (mState.stack.size() == 0) {
                return new DocumentInfo();
            }
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
                if (null != root) {
                    title.setText(root.title);
                }
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
                if (null != root) {
                    title.setText(root.title);
                    subdir.setVisibility(View.GONE);
                }
            } else {
                title.setText(doc.displayName);
                subdir.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    };



    private AdapterView.OnItemSelectedListener mStackListener = new AdapterView.OnItemSelectedListener() {
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

    public RootInfo getCurrentRoot() {
        if (mState.stack.root != null) {
            return mState.stack.root;
        } else {
            return mState.action == ACTION_BROWSE ? mRoots.getDefaultRoot() : mRoots.getStorageRoot();
        }
    }

    public RootInfo getDownloadRoot() {
        return mRoots.getDownloadRoot();
    }

    public RootInfo getAppsBackupRoot() {
        return mRoots.getAppsBackupRoot();
    }

    public RootsCache getRoots() {
        return mRoots;
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

    public State getDisplayState() {
        return mState;
    }

    public boolean isShowAsDialog() {
        return mShowAsDialog;
    }

    public boolean isCreateSupported() {
        final DocumentInfo cwd = getCurrentDirectory();
        if (mState.action == ACTION_OPEN_TREE) {
            return cwd != null && cwd.isCreateSupported();
        } else if (mState.action == ACTION_CREATE || mState.action == ACTION_GET_CONTENT) {
            return false;
        } else {
            return cwd != null && cwd.isCreateSupported();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onCurrentDirectoryChanged(int anim) {

        if (!Utils.isActivityAlive(DocumentsActivity.this)) {
            return;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final RootInfo root = getCurrentRoot();
        DocumentInfo cwd = getCurrentDirectory();


        boolean isExtra = (null != root && !root.isServerStorage() && !root.isTransfer());
        if (cwd == null && isExtra) {
            final Uri uri = DocumentsContract.buildDocumentUri(
                    root.authority, root.documentId);
            DocumentInfo result;
            try {
                result = DocumentInfo.fromUri(getContentResolver(), uri);
                if (result != null) {
                    mState.stack.push(result);
                    mState.stackTouched = true;
                    cwd = result;
                }
            } catch (FileNotFoundException e) {
                CrashReportingManager.logException(e);
            }
        }
        if (!SettingsActivity.getFolderAnimation(this)) {
            anim = 0;
        }

        if (cwd == null) {

            if (mState.action == ACTION_CREATE || mState.action == ACTION_OPEN_TREE) {
                RecentsCreateFragment.show(fm);
            } else {
                if (null != root && root.isHome()) {
                    HomeFragment.show(fm);
                } else if (null != root && root.isConnections()) {
                    ConnectionsFragment.show(fm);
                } else if (null != root && root.isTransfer()) {
                    TransferFragment.show(fm);
                } else if (null != root && root.isCast()) {
                    QueueFragment.show(fm);
                } else if (null != root && root.isServerStorage()) {
                    ServerFragment.show(fm, root);
                } else {
                    DirectoryFragment.showRecentsOpen(fm, anim, root);

                }
            }
        } else {
            if (mState.currentSearch != null && mSearchResultShown) {

                DirectoryFragment.showSearch(fm, root, cwd, mState.currentSearch, anim);
                mSearchResultShown = false;
            } else {

                DirectoryFragment.showNormal(fm, root, cwd, anim);
            }
        }


        if (mState.action == ACTION_CREATE) {
            final SaveFragment save = SaveFragment.get(fm);
            if (save != null) {
                save.setReplaceTarget(null);
            }
        }

        if (mState.action == ACTION_OPEN_TREE) {
            final PickFragment pick = PickFragment.get(fm);
            if (pick != null && null != cwd) {
                final CharSequence displayName = (mState.stack.size() <= 1) && null != root
                        ? root.title : cwd.displayName;
                pick.setPickTarget(cwd, displayName);
            }
        }

        final MoveFragment move = MoveFragment.get(fm);
        if (move != null) {
            move.setReplaceTarget(cwd);
        }

        final RootsCommonFragment roots = RootsCommonFragment.get(fm);
        if (roots != null) {
            roots.onCurrentRootChanged();
        }

        updateActionBar();
        invalidateMenu();
        dumpStack();

    }

    private AppRate.OnShowListener mOnShowListener = new AppRate.OnShowListener() {
        @Override
        public void onRateAppShowing() {

        }

        @Override
        public void onRateAppDismissed() {

        }

        @Override
        public void onRateAppClicked() {
            AnalyticsManager.logEvent("app_rate");
        }
    };

    public void onStackPicked(DocumentStack stack) {
        try {

            stack.updateDocuments(getContentResolver());

            mState.stack = stack;
            mState.stackTouched = true;
            onCurrentDirectoryChanged(ANIM_SIDE);

        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to restore stack: " + e);
            CrashReportingManager.logException(e);
        }
    }

    public void onRootPicked(RootInfo root, RootInfo parentRoot) {
        mParentRoot = parentRoot;
        onRootPicked(root, true);
    }

    public void onRootPicked(RootInfo root, boolean closeDrawer) {

        if (null == root) {
            return;
        }

        mState.stack.root = root;
        mState.stack.clear();
        mState.stackTouched = true;

        if (RootInfo.isOtherRoot(root) || mRoots.isRecentsRoot(root)) {
            onCurrentDirectoryChanged(ANIM_SIDE);
        } else {
            new PickRootTask(root).executeOnExecutor(getCurrentExecutor());
        }

        if (closeDrawer) {
            setRootsDrawerOpen(false);
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
                CrashReportingManager.logException(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (!Utils.isActivityAlive(DocumentsActivity.this)) {
                return;
            }
            if (result != null) {
                mState.stack.push(result);
                mState.stackTouched = true;
                onCurrentDirectoryChanged(ANIM_SIDE);
            }
        }
    }

    private class UploadFileTask extends AsyncTask<Void, Void, Boolean> {
        private final DocumentInfo mCwd;
        private final String mMimeType;
        private final String mDisplayName;
        private final Uri mUri;

        public UploadFileTask(Uri uri, String name, String mimeType) {
            mCwd = getCurrentDirectory();
            mDisplayName = name;
            mMimeType = mimeType;
            mUri = uri;
        }

        @Override
        protected void onPreExecute() {
            setPending(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final ContentResolver resolver = getContentResolver();
            ContentProviderClient client = null;
            Boolean hadTrouble = false;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(
                        resolver, mCwd.derivedUri.getAuthority());
                hadTrouble = !DocumentsContract.uploadDocument(
                        resolver, mCwd.derivedUri, mUri, mMimeType, mDisplayName);
            } catch (Exception e) {
                Log.w(DocumentsActivity.TAG, "Failed to upload document", e);
                CrashReportingManager.logException(e);
            } finally {
                ContentProviderClientCompat.releaseQuietly(client);
            }

            return hadTrouble;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Utils.showError(DocumentsActivity.this, R.string.upload_error);
            }
            setPending(false);
        }
    }

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
        } else if (requestCode == CODE_SETTINGS) {
            if (resultCode == RESULT_FIRST_USER) {
                recreate();
            }
        } else if (requestCode == ADD_STORAGE_REQUEST_CODE) {
            SAFManager.onActivityResult(this, requestCode, resultCode, data);
        } else if (requestCode == UPLOAD_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                final Uri uri = data.getData();
                final String name = FileUtils.getFilenameFromContentUri(this, uri);
                new UploadFileTask(uri, name,
                        FileUtils.getTypeForName(name)).executeOnExecutor(getCurrentExecutor());
            }
        } else if (requestCode == REQUEST_CONFIRM_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                mAuthenticated = true;
            } else {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onDocumentPicked(DocumentInfo doc) {
        final FragmentManager fm = getSupportFragmentManager();
        if (doc.isDirectory() || DocumentArchiveHelper.isSupportedArchiveType(doc.mimeType)) {
            mState.stack.push(doc);
            mState.stackTouched = true;
            onCurrentDirectoryChanged(ANIM_DOWN);
            final MoveFragment move = MoveFragment.get(fm);
            if (move != null) {
                move.setReplaceTarget(doc);
            }
        } else if (mState.action == ACTION_OPEN || mState.action == ACTION_GET_CONTENT) {

            new ExistingFinishTask(doc.derivedUri).executeOnExecutor(getCurrentExecutor());
        } else if (mState.action == ACTION_BROWSE) {


            final RootInfo rootInfo = getCurrentRoot();
            final Intent view = new Intent(Intent.ACTION_VIEW);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            view.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (RootInfo.isMedia(rootInfo)) {
                view.setDataAndType(MediaDocumentsProvider.getMediaUriForDocumentId(doc.documentId), doc.mimeType);
            } else {
                Uri contentUri = null;
                if ((rootInfo.isStorage() || doc.isMedia()) && !TextUtils.isEmpty(doc.path)) {
                    contentUri = FileUtils.getContentUriFromFilePath(this, new File(doc.path).getAbsolutePath());
                }
                if (null == contentUri) {
                    contentUri = doc.derivedUri;
                }
                view.setDataAndType(contentUri, doc.mimeType);
            }
            if ((MimePredicate.mimeMatches(MimePredicate.SPECIAL_MIMES, doc.mimeType)
                    || !Utils.isIntentAvailable(this, view)) && !Utils.hasNougat()) {
                try {
                    File file = new File(doc.path);
                    view.setDataAndType(Uri.fromFile(file), doc.mimeType);
                } catch (Exception e) {
                    view.setDataAndType(doc.derivedUri, doc.mimeType);
                    CrashReportingManager.logException(e);
                }
            }

            if (Utils.isIntentAvailable(this, view)) {


                try {
                    Casty casty = DocumentsApplication.getInstance().getCasty();
                    if (casty.isConnected() && doc.isMedia()) {
                        CastUtils.addToQueue(casty,
                                CastUtils.buildMediaInfo(doc, getRoots().getPrimaryRoot()));
                        invalidateMenu();
                    } else {
                        startActivity(view);
                    }
                } catch (Exception e) {
                    CrashReportingManager.logException(e);
                }
            } else {
                Utils.showError(this, R.string.toast_no_application);
            }
        } else if (mState.action == ACTION_CREATE) {


            SaveFragment.get(fm).setReplaceTarget(doc);
        } else if (mState.action == ACTION_MANAGE) {


            final Intent manage = new Intent(DocumentsContract.ACTION_MANAGE_DOCUMENT);
            manage.setData(doc.derivedUri);

            if (Utils.isIntentAvailable(this, manage)) {
                try {
                    startActivity(manage);
                } catch (ActivityNotFoundException ex) {

                    CrashReportingManager.logException(ex);
                    final Intent view = new Intent(Intent.ACTION_VIEW);
                    view.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    view.setData(doc.derivedUri);

                    try {
                        startActivity(view);
                    } catch (ActivityNotFoundException ex2) {
                        Utils.showError(this, R.string.toast_no_application);
                        CrashReportingManager.logException(ex2);
                    }
                }
            } else {
                Utils.showError(this, R.string.toast_no_application);
            }
        }
    }

    public void onDocumentsPicked(List<DocumentInfo> docs) {
        if (mState.action == ACTION_OPEN || mState.action == ACTION_GET_CONTENT || mState.action == ACTION_BROWSE) {
            final int size = docs.size();
            final Uri[] uris = new Uri[size];
            for (int i = 0; i < size; i++) {
                uris[i] = docs.get(i).derivedUri;
            }
            new ExistingFinishTask(uris).executeOnExecutor(getCurrentExecutor());
        }
    }

    public void onSaveRequested(DocumentInfo replaceTarget) {
        new ExistingFinishTask(replaceTarget.derivedUri).executeOnExecutor(getCurrentExecutor());
    }

    public void onSaveRequested(String mimeType, String displayName) {
        new CreateFinishTask(mimeType, displayName).executeOnExecutor(getCurrentExecutor());
    }

    public void onPickRequested(DocumentInfo pickTarget) {
        final Uri viaUri = DocumentsContract.buildTreeDocumentUri(pickTarget.authority,
                pickTarget.documentId);
        new PickFinishTask(viaUri).executeOnExecutor(getCurrentExecutor());
    }

    public void onMoveRequested(ArrayList<DocumentInfo> docs, DocumentInfo toDoc, boolean deleteAfter) {
        new MoveTask(docs, toDoc, deleteAfter).executeOnExecutor(getCurrentExecutor());
    }

    private void saveStackBlocking() {
        final ContentResolver resolver = getContentResolver();
        final ContentValues values = new ContentValues();

        final byte[] rawStack = DurableUtils.writeToArrayOrNull(mState.stack);
        if (mState.action == ACTION_CREATE || mState.action == ACTION_OPEN_TREE) {

            values.clear();
            values.put(RecentColumns.KEY, mState.stack.buildKey());
            values.put(RecentColumns.STACK, rawStack);
            resolver.insert(RecentsProvider.buildRecent(), values);
        }


        final String packageName = getCallingPackageMaybeExtra();
        values.clear();
        values.put(ResumeColumns.STACK, rawStack);
        values.put(ResumeColumns.EXTERNAL, 0);
        resolver.insert(RecentsProvider.buildResume(packageName), values);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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
            if (Utils.hasJellyBean()) {
                intent.setClipData(clipData);
            } else {
                intent.setData(uris[0]);
            }
        }

        if (mState.action == DocumentsActivity.State.ACTION_GET_CONTENT) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (mState.action == ACTION_OPEN_TREE) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        } else {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }

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
                CrashReportingManager.logException(e);
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
            if (!Utils.isActivityAlive(DocumentsActivity.this)) {
                return;
            }
            if (result != null) {
                onFinished(result);
            } else {
                final DocumentInfo cwd = getCurrentDirectory();
                if (!isSAFIssue(cwd.documentId)) {
                    Utils.showError(DocumentsActivity.this, R.string.save_error);
                }
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

    private class PickFinishTask extends android.os.AsyncTask<Void, Void, Void> {
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

    private class MoveTask extends AsyncTask<Void, Void, Boolean> {
        private final DocumentInfo toDoc;
        private final ArrayList<DocumentInfo> docs;
        private boolean deleteAfter;

        public MoveTask(ArrayList<DocumentInfo> docs, DocumentInfo toDoc, boolean deleteAfter) {
            this.docs = docs;
            this.toDoc = toDoc;
            this.deleteAfter = deleteAfter;
        }

        @Override
        protected void onPreExecute() {
            setMovePending(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final ContentResolver resolver = getContentResolver();
            final DocumentInfo cwd = null == toDoc ? getCurrentDirectory() : toDoc;

            boolean hadTrouble = false;
            for (DocumentInfo doc : docs) {

                if (!doc.isMoveSupported()) {
                    Log.w(TAG, "Skipping " + doc);
                    hadTrouble = true;
                    continue;
                }

                try {
                    if (deleteAfter) {
                        hadTrouble = DocumentsContract.moveDocument(resolver, doc.derivedUri, null,
                                cwd.derivedUri) == null;
                    } else {
                        hadTrouble = DocumentsContract.copyDocument(resolver, doc.derivedUri,
                                cwd.derivedUri) == null;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to move " + doc);
                    hadTrouble = true;
                    CrashReportingManager.logException(e);
                }
            }

            Bundle params2 = new Bundle();
            params2.putBoolean(FILE_MOVE, deleteAfter);
            params2.putInt(FILE_COUNT, docs.size());
            AnalyticsManager.logEvent("files_moved", params2);

            return hadTrouble;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!Utils.isActivityAlive(DocumentsActivity.this)) {
                return;
            }
            if (result) {

                Utils.showError(DocumentsActivity.this, R.string.save_error);

            }
            MoveFragment.hide(getSupportFragmentManager());
            setMovePending(false);
            refreshData();
        }
    }

    public void setMovePending(boolean pending) {
        final MoveFragment move = MoveFragment.get(getSupportFragmentManager());
        if (move != null) {
            move.setPending(pending);
        }
    }

    private void dumpStack() {
        Log.d(TAG, "Current stack: ");
        Log.d(TAG, " * " + mState.stack.root);
        for (DocumentInfo doc : mState.stack) {
            Log.d(TAG, " +-- " + doc);
        }
    }

    public static DocumentsActivity get(Fragment fragment) {
        return (DocumentsActivity) fragment.getActivity();
    }

    private final Handler handler = new Handler();
    private Drawable oldBackground;

    private void changeActionBarColor() {

        int color = SettingsActivity.getPrimaryColor(this);
        Drawable colorDrawable = new ColorDrawable(color);

        if (oldBackground == null) {
            getSupportActionBar().setBackgroundDrawable(colorDrawable);
        } else {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, colorDrawable});
            getSupportActionBar().setBackgroundDrawable(td);
            td.startTransition(200);
        }

        oldBackground = colorDrawable;

        setUpStatusBar();
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getSupportActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
        }
    };

    public boolean getActionMode() {
        return mActionMode;
    }

    public void setActionMode(boolean actionMode) {
        mActionMode = actionMode;
        mToolbar.setVisibility(actionMode ? View.INVISIBLE : View.VISIBLE);
    }

    public void invalidateMenu() {
        supportInvalidateOptionsMenu();
        mActionMenu.setVisibility(showActionMenu() ? View.VISIBLE : View.GONE);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor(this));
        if (Utils.hasLollipop()) {
            getWindow().setStatusBarColor(color);
        } else if (Utils.hasKitKat()) {
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(color);
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpDefaultStatusBar() {
        int color = ContextCompat.getColor(this, R.color.alertColor);
        if (Utils.hasLollipop()) {
            getWindow().setStatusBarColor(color);
        } else if (Utils.hasKitKat()) {
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(Utils.getStatusBarColor(color));
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void initControls() {
        mActionMenu = (FloatingActionsMenu) findViewById(R.id.fabs);
        mActionMenu.setMenuListener(mMenuListener);
    }

    public void upadateActionItems(RecyclerView currentView) {

        mActionMenu.attachToListView(currentView);
        RootInfo root = getCurrentRoot();
        if (null != root && root.isCloudStorage()) {
            mActionMenu.newNavigationMenu(R.menu.menu_fab_cloud);
        }
        int defaultColor = SettingsActivity.getPrimaryColor(this);
        ViewCompat.setNestedScrollingEnabled(currentView, true);
        mActionMenu.show();
        mActionMenu.setVisibility(showActionMenu() ? View.VISIBLE : View.GONE);
        mActionMenu.setBackgroundTintList(SettingsActivity.getAccentColor());
        mActionMenu.setSecondaryBackgroundTintList(Utils.getActionButtonColor(defaultColor));
    }


    private boolean showActionMenu() {
        final RootInfo root = getCurrentRoot();
        return !RootInfo.isOtherRoot(root) &&
                isCreateSupported() &&
                (null != root && (!root.isRootedStorage() || Utils.isRooted()))
                && mState.currentSearch == null;

    }

    private SimpleMenuListenerAdapter mMenuListener = new SimpleMenuListenerAdapter() {

        @Override
        public boolean onMenuItemSelected(MenuItem menuItem) {
            Bundle params = new Bundle();
            switch (menuItem.getItemId()) {
                case R.id.fab_create_file:
                    onStateChanged();
                    createFile();
                    mActionMenu.closeMenu();
                    break;

                case R.id.fab_upload_file:
                    onStateChanged();
                    uploadFile();
                    mActionMenu.closeMenu();
                    break;

                case R.id.fab_create_folder:
                    createFolder();
                    mActionMenu.closeMenu();
                    break;

            }
            return false;
        }
    };


    public void setSAFPermissionRequested(boolean SAFPermissionRequested) {
        this.SAFPermissionRequested = SAFPermissionRequested;
    }

    public boolean getSAFPermissionRequested() {
        return SAFPermissionRequested;
    }
}
