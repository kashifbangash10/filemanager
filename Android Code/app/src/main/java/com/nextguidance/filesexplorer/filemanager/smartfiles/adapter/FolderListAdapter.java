package com.nextguidance.filesexplorer.filemanager.smartfiles.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.fragment.FoldersTabFragment;
import java.util.List;

public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.ViewHolder> {

    private Context context;
    private List<FoldersTabFragment.FolderModel> folderList;

    public FolderListAdapter(Context context, List<FoldersTabFragment.FolderModel> folderList) {
        this.context = context;
        this.folderList = folderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoldersTabFragment.FolderModel folder = folderList.get(position);
        holder.tvFolderName.setText(folder.name);
        
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Opening " + folder.name, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.FolderDetailActivity.class);
            intent.putExtra("folder_path", folder.path);
            intent.putExtra("folder_name", folder.name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFolder;
        TextView tvFolderName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFolder = itemView.findViewById(R.id.iv_folder);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
        }
    }
}
