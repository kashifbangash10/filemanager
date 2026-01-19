package com.nextguidance.filesexplorer.filemanager.smartfiles.directory;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;

import androidx.collection.ArrayMap;

import com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsApplication;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.common.RecyclerFragment.RecyclerItemClickListener.OnItemClickListener;
import com.nextguidance.filesexplorer.filemanager.smartfiles.cursor.RootCursorWrapper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.directory.DocumentsAdapter.Environment;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.IconUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.ProviderExecutor;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.RootsCache;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.Utils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.RootInfo;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.ACTION_BROWSE;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_GRID;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.DirectoryFragment.TYPE_RECENT_OPEN;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo.getCursorString;

public class ListDocumentHolder extends DocumentHolder {

    public ListDocumentHolder(Context context, ViewGroup parent, int layout, OnItemClickListener onItemClickListener,
                              Environment environment) {
        super(context, parent, layout, onItemClickListener, environment);
    }

    public ListDocumentHolder(Context context, ViewGroup parent, OnItemClickListener onItemClickListener,
                              Environment environment) {
        this(context, parent,
                getLayoutId(environment),
                onItemClickListener, environment);
    }

    public static int getLayoutId(Environment environment){
        int layoutId = R.layout.item_doc_list;
        if(environment.isApp()){
            layoutId = environment.getRoot().isAppProcess() ? R.layout.item_doc_process_list : R.layout.item_doc_app_list;
        }
        return layoutId;
    }

    @Override
    public void setData(Cursor cursor, int position) {
        super.setData(cursor, position);
        final Context context = mContext;
        final BaseActivity.State state = mEnv.getDisplayState();
        final RootsCache roots = DocumentsApplication.getRootsCache(context);

        mDoc.updateFromCursor(cursor, getCursorString(cursor, RootCursorWrapper.COLUMN_AUTHORITY));

        if (state.action == ACTION_BROWSE) {
            if (null != iconView) {
                iconView.setOnClickListener(this);
            }
        }

        final boolean enabled = mEnv.isDocumentEnabled(mDoc.mimeType, mDoc.flags);
        setEnabled(enabled);

        mIconHelper.stopLoading(iconThumb);

        iconMime.animate().cancel();
        iconMime.setAlpha(1f);
        iconThumb.animate().cancel();
        iconThumb.setAlpha(0f);

        mIconHelper.load(mDoc, iconThumb, iconMime, iconMimeBackground);

        boolean hasLine1 = false;
        boolean hasLine2 = false;

        final boolean hideTitle = (state.derivedMode == MODE_GRID) && mEnv.hideGridTiles();
        if (!hideTitle) {
            title.setText(mDoc.displayName);
            hasLine1 = true;
        }

        if (summary != null) {
            summary.setVisibility(View.GONE);
        }

        Drawable iconDrawable = null;
        if (mEnv.getType() == TYPE_RECENT_OPEN) {
            final String docRootId = getCursorString(cursor, RootCursorWrapper.COLUMN_ROOT_ID);


            final RootInfo root = roots.getRootBlocking(mDoc.authority, docRootId);
            if (state.derivedMode == MODE_GRID) {
                iconDrawable = root.loadGridIcon(context);
            } else {
                iconDrawable = root.loadIcon(context);
            }

        } else {


            if (Utils.isDir(mDoc.mimeType) && state.derivedMode == MODE_GRID) {
                iconDrawable = IconUtils.applyTintAttr(context, R.drawable.ic_root_folder,
                        android.R.attr.textColorPrimaryInverse);
            }
        }

        if (icon1 != null)
            icon1.setVisibility(View.GONE);
        if (icon2 != null)
            icon2.setVisibility(View.GONE);

        if (iconDrawable != null) {
            if (hasLine1 && icon1 != null) {
                icon1.setVisibility(View.VISIBLE);
                icon1.setImageDrawable(iconDrawable);
            } else if (icon2 != null) {
                icon2.setVisibility(View.VISIBLE);
                icon2.setImageDrawable(iconDrawable);
            }
        }

        if (Utils.isDir(mDoc.mimeType) && mDoc.summary != null) {
             date.setText(mDoc.summary);
             hasLine2 = true;
        } else {
            if (mDoc.lastModified == -1) {
                date.setText(null);
            } else {
                date.setText(Utils.formatTime(context, mDoc.lastModified));
                hasLine2 = true;
            }
        }

        final FolderSizeAsyncTask oldSizeTask = (FolderSizeAsyncTask) size.getTag();
        if (oldSizeTask != null) {
            oldSizeTask.preempt();
            size.setTag(null);
        }
        if (state.showSize) {
            size.setVisibility(View.VISIBLE);
            if (Utils.isDir(mDoc.mimeType) || mDoc.size == -1) {
                size.setText(null);
                ArrayMap<Integer, Long> sizes = DocumentsApplication.getFolderSizes();
                if (state.showFolderSize) {
                    long sizeInBytes = sizes.containsKey(position) ? sizes.get(position) : -1;
                    if (sizeInBytes != -1) {
                        size.setText(Formatter.formatFileSize(context, sizeInBytes));
                    } else {
                        final FolderSizeAsyncTask task = new FolderSizeAsyncTask(size, mDoc.path, position);
                        size.setTag(task);
                        ProviderExecutor.forAuthority(mDoc.authority).execute(task);
                    }
                }
            } else {
                size.setText(Formatter.formatFileSize(context, mDoc.size));
                // No need to set hasLine2 = true here as size is now in line 1
            }
        } else {
            size.setVisibility(View.GONE);
        }

        if (line1 != null) {
            line1.setVisibility(hasLine1 ? View.VISIBLE : View.GONE);
        }
        if (line2 != null) {
            line2.setVisibility(hasLine2 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);


        final float imgAlpha = enabled ? 1f : DISABLED_ALPHA;
        iconMime.setAlpha(imgAlpha);
        iconThumb.setAlpha(imgAlpha);
    }
}
