package com.nextguidance.filesexplorer.filemanager.smartfiles.adapter;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.CrashReportingManager;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.IconHelper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.IconUtils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.Utils;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.RootInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.setting.SettingsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.ui.CircleImage;
import com.nextguidance.filesexplorer.filemanager.smartfiles.ui.NumberProgressBar;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;



public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {
    public static final int TYPE_MAIN = 1;
    public static final int TYPE_SHORTCUT = 2;
    public static final int TYPE_RECENT = 3;

    private final int mDefaultColor;
    private Activity mContext;
    private OnItemClickListener onItemClickListener;
    private ArrayList<CommonInfo> mData = new ArrayList<>();
    private Cursor recentCursor;
    private final IconHelper mIconHelper;

    public HomeAdapter(Activity context, ArrayList<CommonInfo> data, IconHelper iconHelper) {
        mContext = context;
        mData = data;
        mDefaultColor = SettingsActivity.getPrimaryColor();
        mIconHelper = iconHelper;
    }

    public void setData(ArrayList<CommonInfo> data) {
        if (data == mData) {
            return;
        }

        mData = data;
        notifyDataSetChanged();
    }

    public void setRecentData(Cursor cursor) {
        recentCursor = cursor;
        notifyDataSetChanged();
    }

    public int getRecentSize() {
        return recentCursor != null && recentCursor.getCount() != 0 ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(position);
    }

    @Override
    public int getItemCount() {
        return mData.size() + getRecentSize();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MAIN: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_home, parent, false);
                return new MainViewHolder(itemView);
            }
            case TYPE_SHORTCUT: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_shortcuts_grid, parent, false);
                return new ShortcutViewHolder(itemView);
            }
            case TYPE_RECENT: {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gallery, parent, false);
                return new GalleryViewHolder(itemView);
            }
        }
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcuts, parent, false);
        return new ShortcutViewHolder(itemView);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(ViewHolder item, View view, int position);

        void onItemLongClick(ViewHolder item, View view, int position);

        void onItemViewClick(ViewHolder item, View view, int position);
    }

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        protected final CircleImage iconBackground;
        protected final ImageView icon;
        protected final TextView title;
        protected TextView summary;
        protected NumberProgressBar progress;
        protected ImageButton action;
        protected View action_layout;
        protected View card_view;
        protected final ImageView iconMime;
        protected final ImageView iconThumb;
        protected final View iconMimeBackground;
        public CommonInfo commonInfo;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(ViewHolder.this, v, getLayoutPosition());
                    }
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemLongClick(ViewHolder.this, v, getLayoutPosition());
                    }
                    return false;
                }
            });
            icon = (ImageView) v.findViewById(android.R.id.icon);
            iconBackground = (CircleImage) v.findViewById(R.id.icon_background);
            title = (TextView) v.findViewById(android.R.id.title);

            card_view = v.findViewById(R.id.card_view);
            summary = (TextView) v.findViewById(android.R.id.summary);
            progress = (NumberProgressBar) v.findViewById(android.R.id.progress);
            action_layout = v.findViewById(R.id.action_layout);
            action = (ImageButton) v.findViewById(R.id.action);

            iconMime = (ImageView) v.findViewById(R.id.icon_mime);
            iconThumb = (ImageView) v.findViewById(R.id.icon_thumb);
            iconMimeBackground = v.findViewById(R.id.icon_mime_background);
        }

        public abstract void setData(int position);
    }

    public class MainViewHolder extends ViewHolder {
        private final int accentColor;
        private final int color;

        public MainViewHolder(View v) {
            super(v);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(MainViewHolder.this, action, getLayoutPosition());
                    }
                }
            });
            accentColor = SettingsActivity.getAccentColor();
            color = SettingsActivity.getPrimaryColor();
        }

        @Override
        public void setData(int position) {
            commonInfo = getItem(position);
            icon.setImageDrawable(commonInfo.rootInfo.loadDrawerIcon(mContext));
            title.setText(commonInfo.rootInfo.title);
            int drawableId = -1;
            if (commonInfo.rootInfo.isAppProcess()) {
                drawableId = R.drawable.ic_clean;
            } else if (commonInfo.rootInfo.isStorage() && !commonInfo.rootInfo.isSecondaryStorage()) {
                drawableId = R.drawable.ic_analyze;
            }
            int rootColor = ContextCompat.getColor(mContext, commonInfo.rootInfo.derivedColor);
            if (drawableId != -1) {
                action.setImageDrawable(IconUtils.applyTint(mContext, drawableId, rootColor));
                action_layout.setVisibility(View.VISIBLE);
            } else {
                action.setImageDrawable(null);
                action_layout.setVisibility(View.GONE);
            }

            if (commonInfo.rootInfo.availableBytes >= 0) {
                try {
                    Long current = 100 * commonInfo.rootInfo.availableBytes / commonInfo.rootInfo.totalBytes;
                    progress.setVisibility(View.VISIBLE);
                    progress.setMax(100);
                    progress.setProgress(100 - current.intValue());
                    progress.setColor(rootColor);
                    animateProgress(progress, commonInfo.rootInfo);
                } catch (Exception e) {
                    progress.setVisibility(View.GONE);
                }
            } else {
                progress.setVisibility(View.GONE);
            }
        }
    }

    public class ShortcutViewHolder extends ViewHolder {
        public ImageView icon;
        public View iconBackground;
        public TextView title;
        public TextView subtitle;

        public ShortcutViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            iconBackground = v.findViewById(R.id.icon_background);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
        }

        @Override
        public void setData(int position) {
            commonInfo = getItem(position);
            RootInfo root = commonInfo.rootInfo;
            if (root == null) return;

            title.setText(root.title);
            String rootTitle = root.title != null ? root.title.toLowerCase() : "";

            // Icons aur background color set karein
            if (iconBackground instanceof com.nextguidance.filesexplorer.filemanager.smartfiles.ui.CircleImage) {
                if (isPremiumCategory(rootTitle)) {
                    iconBackground.setVisibility(View.GONE);
                    icon.setPadding(0, 0, 0, 0);
                } else {
                    iconBackground.setVisibility(View.VISIBLE);
                    int p = (int) (5 * mContext.getResources().getDisplayMetrics().density);
                    icon.setPadding(p, p, p, p);
                    ((com.nextguidance.filesexplorer.filemanager.smartfiles.ui.CircleImage) iconBackground)
                            .setColor(ContextCompat.getColor(mContext, root.derivedColor));
                }
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (rootTitle.equals("analysis")) {
                    // Analysis Activity open karein
                    if (mContext instanceof androidx.fragment.app.FragmentActivity) {
                        com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.AnalysisFragment.show(
                                ((androidx.fragment.app.FragmentActivity) mContext).getSupportFragmentManager());
                    }
                } else if (rootTitle.equals("cleaner") || rootTitle.equals("clean")) {
                    if (mContext instanceof DocumentsActivity) {
                        androidx.fragment.app.Fragment f = ((DocumentsActivity) mContext).getSupportFragmentManager().findFragmentById(R.id.container_directory);
                        if (f instanceof com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.HomeFragment) {
                            ((com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.HomeFragment) f).cleanRAM();
                        }
                    }
                } else {
                    // Baaki items ke liye normal flow
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(ShortcutViewHolder.this, v, getLayoutPosition());
                    }
                }
            });

            // Icons aur data set karein
            switch (rootTitle) {
                case "analysis":
                    if (root.totalBytes > 0) {
                        long used = root.totalBytes - root.availableBytes;
                        int progress = (int) ((used * 100) / root.totalBytes);
                        subtitle.setText(progress + "%");
                    } else {
                        subtitle.setText("0%");
                    }
                    icon.setImageResource(R.drawable.ic_category_analysis);
                    break;

                case "downloads":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_downloads);
                    break;

                case "video":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_videos);
                    break;

                case "audio":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_audio);
                    break;

                case "images":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_images);
                    break;

                case "apps":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_apps);
                    break;

                case "documents":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_documents);
                    break;

                case "archives":
                    if (root.totalBytes > 0) {
                        subtitle.setText(Formatter.formatShortFileSize(mContext, root.totalBytes));
                    } else {
                        subtitle.setText("0 B");
                    }
                    icon.setImageResource(R.drawable.ic_category_archives);
                    break;

                case "more":
                    subtitle.setText("");
                    icon.setImageResource(R.drawable.ic_category_more);
                    break;

                case "cleaner":
                    subtitle.setText("Clean Now");
                    icon.setImageDrawable(IconUtils.applyTint(mContext, R.drawable.ic_root_process, ContextCompat.getColor(mContext, root.derivedColor)));
                    break;

                case "wifi share":
                    subtitle.setText("Transfer");
                    icon.setImageResource(R.drawable.ic_category_wifi_share);
                    break;

                case "transfer to pc":
                    subtitle.setText("FTP Server");
                    icon.setImageResource(R.drawable.ic_category_transfer_pc);
                    break;

                case "cast queue":
                    subtitle.setText("Chromecast");
                    icon.setImageResource(R.drawable.ic_category_cast);
                    break;

                case "connections":
                    subtitle.setText("Cloud & FTP");
                    icon.setImageResource(R.drawable.ic_category_connections);
                    break;

                default:
                    subtitle.setVisibility(View.VISIBLE);
                    subtitle.setText("0 B");
                    if (root.loadShortcutIcon(mContext) != null) {
                        icon.setImageDrawable(root.loadShortcutIcon(mContext));
                    }
                    break;
            }
        }
    }
// hello 
    public class GalleryViewHolder extends ViewHolder {
        private final RecyclerView recyclerview;
        private TextView recents;
        private RecentsAdapter adapter;

        public GalleryViewHolder(View v) {
            super(v);

            recyclerview = (RecyclerView) v.findViewById(R.id.recyclerview);
            recents = v.findViewById(R.id.recents);
            recents.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemViewClick(GalleryViewHolder.this, recents, getLayoutPosition());
                    }
                }
            });
        }

        @Override
        public void setData(int position) {
            commonInfo = CommonInfo.from(recentCursor);
            adapter = new RecentsAdapter(mContext, recentCursor, mIconHelper);
            adapter.setOnItemClickListener(new RecentsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(RecentsAdapter.ViewHolder item, int position) {
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(GalleryViewHolder.this, recyclerview, position);
                    }
                }
            });
            recyclerview.setAdapter(adapter);
        }

        public DocumentInfo getItem(int position) {
            return DocumentInfo.fromDirectoryCursor(adapter.getItem(position));
        }
    }

    public CommonInfo getItem(int position) {
        if (position < mData.size()) {
            return mData.get(position);
        } else {
            return CommonInfo.from(recentCursor);
        }
    }

    private boolean isPremiumCategory(String rootTitle) {
        if (rootTitle == null) return false;
        return rootTitle.equals("downloads") ||
                rootTitle.equals("video") ||
                rootTitle.equals("audio") ||
                rootTitle.equals("images") ||
                rootTitle.equals("apps") ||
                rootTitle.equals("documents") ||
                rootTitle.equals("archives") ||
                rootTitle.equals("wifi share") ||
                rootTitle.equals("transfer to pc") ||
                rootTitle.equals("cast queue") ||
                rootTitle.equals("connections") ||
                rootTitle.equals("more");
    }

    private void animateProgress(final NumberProgressBar item, RootInfo root) {
        try {
            final double percent = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
            final Timer timer = new Timer();
            item.setProgress(0);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (Utils.isActivityAlive(mContext)) {
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (item.getProgress() >= (int) percent) {
                                    timer.cancel();
                                } else {
                                    item.setProgress(item.getProgress() + 1);
                                }
                            }
                        });
                    }
                }
            }, 50, 20);
        } catch (Exception e) {
            item.setVisibility(View.GONE);
            CrashReportingManager.logException(e);
        }
    }
}