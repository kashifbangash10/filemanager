package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.activities.FileDocumentsActivity;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private List<DocumentInfo> documentList = new ArrayList<>();
    private List<DocumentInfo> filteredList = new ArrayList<>();
    private boolean isGridView = true;
    private String iconSize = "large";
    private String category = "All";

    public static DocumentTabFragment newInstance(String category) {
        DocumentTabFragment fragment = new DocumentTabFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString("category", "All");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos_tab, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);

        SharedPreferences prefs = getContext().getSharedPreferences(FileDocumentsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        isGridView = prefs.getBoolean(FileDocumentsActivity.KEY_IS_GRID, true);
        iconSize = prefs.getString(FileDocumentsActivity.KEY_ICON_SIZE, "large");

        updateLayoutManager();

        adapter = new DocumentAdapter(getContext(), filteredList, isGridView, iconSize);
        recyclerView.setAdapter(adapter);

        loadDocuments();
        return view;
    }

    private void updateLayoutManager() {
        if (isGridView) {
            int spanCount = iconSize.equals("small") ? 3 : 2;
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    public void onSearch(String query) {
        if (adapter == null) return;
        List<DocumentInfo> searchResults = new ArrayList<>();
        if (query.isEmpty()) {
            searchResults.addAll(documentList);
        } else {
            for (DocumentInfo doc : documentList) {
                if (doc.displayName.toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(doc);
                }
            }
        }
        filteredList.clear();
        filteredList.addAll(searchResults);
        adapter.notifyDataSetChanged();
    }

    private void loadDocuments() {
        documentList.clear();
        Context context = getContext();
        if (context == null) return;
        ContentResolver cr = context.getContentResolver();
        
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MIME_TYPE
        };

        // Broad selection to include common document types and extensions
        String selection = MediaStore.Files.FileColumns.SIZE + " > 0";
        
        Cursor cursor = cr.query(uri, projection, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            int mimeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
            int sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
            int dateIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int idIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);

            while (cursor.moveToNext()) {
                String name = nameIndex != -1 ? cursor.getString(nameIndex) : null;
                String path = dataIndex != -1 ? cursor.getString(dataIndex) : null;
                String mime = mimeIndex != -1 ? cursor.getString(mimeIndex) : null;
                long size = sizeIndex != -1 ? cursor.getLong(sizeIndex) : 0;
                long date = dateIndex != -1 ? cursor.getLong(dateIndex) : 0;
                String id = idIndex != -1 ? cursor.getString(idIndex) : "0";

                // If name is null, try to get it from path
                if (name == null && path != null) {
                    name = new File(path).getName();
                }

                if (isDocumentFile(name, mime)) {
                    DocumentInfo doc = new DocumentInfo();
                    doc.documentId = id;
                    doc.displayName = name != null ? name : "Unknown";
                    doc.path = path;
                    doc.size = size;
                    doc.lastModified = date * 1000;
                    doc.mimeType = mime;
                    doc.derivedUri = Uri.withAppendedPath(uri, id);
                    
                    if (matchesCategory(doc)) {
                        documentList.add(doc);
                    }
                }
            }
            cursor.close();
        }
        
        filteredList.clear();
        filteredList.addAll(documentList);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private boolean isDocumentFile(String name, String mime) {
        if (mime != null) {
            String lowerMime = mime.toLowerCase();
            if (lowerMime.startsWith("text/") || 
                lowerMime.contains("pdf") || 
                lowerMime.contains("msword") || 
                lowerMime.contains("vnd.ms-excel") || 
                lowerMime.contains("vnd.ms-powerpoint") ||
                lowerMime.contains("officedocument") ||
                lowerMime.contains("epub") ||
                lowerMime.contains("opendocument") ||
                lowerMime.contains("rtf") ||
                lowerMime.contains("abiword") ||
                lowerMime.contains("kword")) {
                return true;
            }
        }
        if (name != null) {
            String lower = name.toLowerCase();
            return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") ||
                   lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".ppt") ||
                   lower.endsWith(".pptx") || lower.endsWith(".txt") || lower.endsWith(".xml") ||
                   lower.endsWith(".json") || lower.endsWith(".conf") || lower.endsWith(".cfg") ||
                   lower.endsWith(".csv") || lower.endsWith(".rtf") || lower.endsWith(".odt") ||
                   lower.endsWith(".ods") || lower.endsWith(".odp") || lower.endsWith(".html") ||
                   lower.endsWith(".htm") || lower.endsWith(".log") || lower.endsWith(".epub") ||
                   lower.endsWith(".dot") || lower.endsWith(".dotx") || lower.endsWith(".xlsb") ||
                   lower.endsWith(".xlsm") || lower.endsWith(".xlt") || lower.endsWith(".xltx") ||
                   lower.endsWith(".pps") || lower.endsWith(".ppsx") || lower.endsWith(".pot") ||
                   lower.endsWith(".potx") || lower.endsWith(".wps") || lower.endsWith(".wpt");
        }
        return false;
    }

    private boolean matchesCategory(DocumentInfo doc) {
        if (category.equals("All")) return true;
        String name = doc.displayName.toLowerCase();
        String mime = doc.mimeType != null ? doc.mimeType.toLowerCase() : "";

        switch (category) {
            case "WORD":
                return name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".dot") || name.endsWith(".dotx") ||
                       mime.contains("msword") || mime.contains("wordprocessingml") || mime.contains("abiword");
            case "EXCEL":
                return name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".xlsb") || name.endsWith(".xlsm") || 
                       name.endsWith(".xlt") || name.endsWith(".xltx") || name.endsWith(".csv") ||
                       mime.contains("ms-excel") || mime.contains("spreadsheetml") || mime.contains("spreadsheet");
            case "PPT":
                return name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".pps") || name.endsWith(".ppsx") || 
                       name.endsWith(".pot") || name.endsWith(".potx") ||
                       mime.contains("ms-powerpoint") || mime.contains("presentationml") || mime.contains("presentation");
            case "PDF":
                return name.endsWith(".pdf") || mime.contains("pdf");
            case "TXT":
                return name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".conf") || name.endsWith(".cfg") || 
                       mime.contains("text/plain") || mime.startsWith("text/");
            case "Others":
                return !matchesCategory(doc, "WORD") && !matchesCategory(doc, "EXCEL") && 
                       !matchesCategory(doc, "PPT") && !matchesCategory(doc, "PDF") && !matchesCategory(doc, "TXT");
            default:
                return true;
        }
    }

    private boolean matchesCategory(DocumentInfo doc, String cat) {
        String name = doc.displayName.toLowerCase();
        String mime = doc.mimeType != null ? doc.mimeType.toLowerCase() : "";
        switch (cat) {
            case "WORD":
                return name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".dot") || name.endsWith(".dotx") ||
                       mime.contains("msword") || mime.contains("wordprocessingml") || mime.contains("abiword");
            case "EXCEL":
                return name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".xlsb") || name.endsWith(".xlsm") || 
                       name.endsWith(".xlt") || name.endsWith(".xltx") || name.endsWith(".csv") ||
                       mime.contains("ms-excel") || mime.contains("spreadsheetml") || mime.contains("spreadsheet");
            case "PPT":
                return name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".pps") || name.endsWith(".ppsx") || 
                       name.endsWith(".pot") || name.endsWith(".potx") ||
                       mime.contains("ms-powerpoint") || mime.contains("presentationml") || mime.contains("presentation");
            case "PDF":
                return name.endsWith(".pdf") || mime.contains("pdf");
            case "TXT":
                return name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".conf") || name.endsWith(".cfg") || 
                       mime.contains("text/plain") || mime.startsWith("text/");
            default: return false;
        }
    }

    private static class DocumentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Context context;
        private List<DocumentInfo> items;
        private boolean isGrid;
        private String iconSize;

        public DocumentAdapter(Context context, List<DocumentInfo> items, boolean isGrid, String iconSize) {
            this.context = context;
            this.items = items;
            this.isGrid = isGrid;
            this.iconSize = iconSize;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = isGrid ? R.layout.item_document_grid : R.layout.item_document_list;
            View view = LayoutInflater.from(context).inflate(layout, parent, false);
            return isGrid ? new GridViewHolder(view) : new ListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DocumentInfo doc = items.get(position);
            String filename = doc.displayName;
            String dateStr = isGrid ? DateFormat.format("d MMM", doc.lastModified).toString() : DateFormat.format("dd/MM/yyyy", doc.lastModified).toString();
            String sizeStr = Formatter.formatFileSize(context, doc.size);

            if (holder instanceof GridViewHolder) {
                GridViewHolder h = (GridViewHolder) holder;
                h.tvName.setText(filename);
                h.tvDate.setText(dateStr);
                h.tvSize.setText(sizeStr);
                
                // Set the specific colored icon as thumbnail
                h.ivThumbnail.setImageResource(getTypeIcon(doc));
                
                // Optional: If we had a real thumbnail loader, we'd use it here
            } else {
                ListViewHolder h = (ListViewHolder) holder;
                h.tvName.setText(filename);
                h.tvDate.setText(dateStr);
                h.tvSize.setText(sizeStr);
                h.ivTypeIcon.setImageResource(getTypeIcon(doc));
            }

            holder.itemView.setOnClickListener(v -> {
                openDocument(doc);
            });
        }

        private void openDocument(DocumentInfo doc) {
             String name = doc.displayName != null ? doc.displayName.toLowerCase() : "";
             if (name.endsWith(".pdf")) {
                 android.content.Intent intent = new android.content.Intent(context, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.PdfViewerActivity.class);
                 intent.putExtra("path", doc.path);
                 intent.putExtra("name", doc.displayName);
                 context.startActivity(intent);
             } else {
                 try {
                     android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                     intent.setDataAndType(doc.derivedUri, doc.mimeType);
                     intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                     context.startActivity(intent);
                 } catch (Exception e) {
                     android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show();
                 }
             }
        }

        private int getTypeIcon(DocumentInfo doc) {
            String name = doc.displayName != null ? doc.displayName.toLowerCase() : "";
            String mime = doc.mimeType != null ? doc.mimeType.toLowerCase() : "";

            if (name.endsWith(".pdf") || mime.contains("pdf")) return R.drawable.ic_file_pdf;
            if (name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".dot") || mime.contains("msword") || mime.contains("wordprocessingml")) return R.drawable.ic_file_word;
            if (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".xlsb") || name.endsWith(".xlsm") || name.endsWith(".csv") || mime.contains("ms-excel") || mime.contains("spreadsheet")) return R.drawable.ic_file_excel;
            if (name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".pps") || name.endsWith(".ppsx") || mime.contains("ms-powerpoint") || mime.contains("presentation")) return R.drawable.ic_file_ppt;
            if (name.endsWith(".txt") || mime.contains("text/plain")) return R.drawable.ic_file_txt;
            if (name.endsWith(".xml") || name.endsWith(".cfg") || name.endsWith(".config") || name.endsWith(".log") || mime.contains("xml") || mime.startsWith("text/")) return R.drawable.ic_file_xml;
            if (name.endsWith(".apk") || mime.contains("android.package-archive")) return R.drawable.ic_doc_apk;
            if (name.contains(".zip") || name.contains(".rar") || name.contains(".7z") || mime.contains("zip") || mime.contains("compressed")) return R.drawable.ic_doc_archive;
            
            return R.drawable.ic_file_generic;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class GridViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvSize;
            ImageView ivThumbnail;

            public GridViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_filename);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvSize = itemView.findViewById(R.id.tv_size);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            }
        }

        static class ListViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvSize;
            ImageView ivTypeIcon;
            CheckBox cbSelect;

            public ListViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_filename);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvSize = itemView.findViewById(R.id.tv_size);
                ivTypeIcon = itemView.findViewById(R.id.iv_type_icon);
                cbSelect = itemView.findViewById(R.id.cb_select);
            }
        }
    }
}
