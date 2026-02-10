package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.chrisbanes.photoview.PhotoView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import java.io.File;
import java.io.IOException;

public class PdfPageFragment extends Fragment {

    private String path;
    private int pageIndex;

    public static PdfPageFragment newInstance(String path, int index) {
        PdfPageFragment fragment = new PdfPageFragment();
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putInt("index", index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            path = getArguments().getString("path");
            pageIndex = getArguments().getInt("index");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_page, container, false);
        PhotoView photoView = view.findViewById(R.id.photo_view);

        try {
            Bitmap bitmap = renderPage(path, pageIndex);
            if (bitmap != null) {
                photoView.setImageBitmap(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return view;
    }

    private Bitmap renderPage(String path, int index) throws IOException {
        File file = new File(path);
        if (!file.exists()) return null;
        
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(fd);
        if (index >= renderer.getPageCount()) {
            renderer.close();
            fd.close();
            return null;
        }
        
        PdfRenderer.Page page = renderer.openPage(index);

        // Increase size for better quality (e.g., 2x or 3x)
        int width = page.getWidth() * 2;
        int height = page.getHeight() * 2;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        page.close();
        renderer.close();
        fd.close();

        return bitmap;
    }
}
