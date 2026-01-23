package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsApplication;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.CommonInfo;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.HomeAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.IconHelper;
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.RootsCache;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.RootInfo;

import java.util.ArrayList;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.HomeAdapter.TYPE_SHORTCUT;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity.State.MODE_GRID;

public class CategoryMoreFragment extends Fragment implements HomeAdapter.OnItemClickListener {

    public static final String TAG = "CategoryMoreFragment";
    
    private RecyclerView recyclerView;
    private HomeAdapter mAdapter;
    private RootsCache roots;
    private RootInfo mHomeRoot;
    private Toolbar toolbar;

    public static void show(FragmentManager fm) {
        if (fm == null) return;
        
        CategoryMoreFragment fragment = new CategoryMoreFragment();
        fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.more_recycler_view);
        TextView manageButton = view.findViewById(R.id.manage_button);
        
        // Setup toolbar back button
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        // Setup manage button (optional - can be implemented later)
        manageButton.setOnClickListener(v -> {
            // TODO: Implement manage functionality
        });

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        roots = DocumentsApplication.getRootsCache(getContext());
        mHomeRoot = roots.getHomeRoot();
        
        ArrayList<CommonInfo> moreShortcuts = new ArrayList<>();
        
        // 1. WiFi Share
        RootInfo transferRoot = roots.getTransferRoot();
        if (transferRoot != null) {
            transferRoot = copyRootInfo(transferRoot);
            transferRoot.title = "WiFi Share";
            transferRoot.derivedColor = android.R.color.holo_blue_dark;
            moreShortcuts.add(CommonInfo.from(transferRoot, TYPE_SHORTCUT));
        }

        // 2. Transfer to PC
        RootInfo serverRoot = roots.getServerRoot();
        if (serverRoot != null) {
            serverRoot = copyRootInfo(serverRoot);
            serverRoot.title = "Transfer to PC";
            serverRoot.derivedColor = android.R.color.holo_green_dark;
            moreShortcuts.add(CommonInfo.from(serverRoot, TYPE_SHORTCUT));
        }

        // 3. Cast Queue
        RootInfo castRoot = roots.getCastRoot();
        if (castRoot != null) {
            castRoot = copyRootInfo(castRoot);
            castRoot.title = "Cast Queue";
            castRoot.derivedColor = android.R.color.holo_orange_dark;
            moreShortcuts.add(CommonInfo.from(castRoot, TYPE_SHORTCUT));
        }

        // 4. Connections
        RootInfo connectionsRoot = roots.getConnectionsRoot();
        if (connectionsRoot != null) {
            connectionsRoot = copyRootInfo(connectionsRoot);
            connectionsRoot.title = "Connections";
            connectionsRoot.derivedColor = android.R.color.holo_purple;
            moreShortcuts.add(CommonInfo.from(connectionsRoot, TYPE_SHORTCUT));
        }

        IconHelper iconHelper = new IconHelper(getActivity(), MODE_GRID);
        mAdapter = new HomeAdapter(getActivity(), moreShortcuts, iconHelper);
        mAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mAdapter);

        // Hide Activity toolbar to avoid duplicate
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) {
            activityToolbar.setVisibility(View.GONE);
        }
    }

    private RootInfo copyRootInfo(RootInfo rootInfo) {
        RootInfo copy = new RootInfo();
        copy.authority = rootInfo.authority;
        copy.rootId = rootInfo.rootId;
        copy.flags = rootInfo.flags;
        copy.icon = rootInfo.icon;
        copy.title = rootInfo.title;
        copy.summary = rootInfo.summary;
        copy.documentId = rootInfo.documentId;
        copy.availableBytes = rootInfo.availableBytes;
        copy.totalBytes = rootInfo.totalBytes;
        return copy;
    }

    @Override
    public void onItemClick(HomeAdapter.ViewHolder item, View view, int position) {
        if (getActivity() instanceof DocumentsActivity) {
            DocumentsActivity activity = (DocumentsActivity) getActivity();
            activity.onRootPicked(item.commonInfo.rootInfo, mHomeRoot);
        }
    }

    @Override
    public void onItemLongClick(HomeAdapter.ViewHolder item, View view, int position) {
        // Not used
    }

    @Override
    public void onItemViewClick(HomeAdapter.ViewHolder item, View view, int position) {
        // Not used
    }

    @Override
    public void onDestroyView() {
        // Restore Activity toolbar
        View activityToolbar = getActivity().findViewById(R.id.toolbar);
        if (activityToolbar != null) {
            activityToolbar.setVisibility(View.VISIBLE);
        }
        super.onDestroyView();
    }
}
