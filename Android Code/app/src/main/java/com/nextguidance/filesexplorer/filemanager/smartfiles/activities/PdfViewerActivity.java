package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import java.io.File;
import java.io.IOException;

public class PdfViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvPageCounter;
    private String pdfPath;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        pdfPath = getIntent().getStringExtra("path");
        String name = getIntent().getStringExtra("name");
        if (name != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
        }

        viewPager = findViewById(R.id.view_pager);
        tvPageCounter = findViewById(R.id.tv_page_counter);

        try {
            openPdf(pdfPath);
            int pageCount = pdfRenderer.getPageCount();
            
            viewPager.setAdapter(new PdfPagerAdapter(this, pageCount, pdfPath));
            
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    tvPageCounter.setText((position + 1) + " / " + pageCount);
                }
            });

            findViewById(R.id.btn_prev).setOnClickListener(v -> {
                if (viewPager.getCurrentItem() > 0) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                }
            });

            findViewById(R.id.btn_next).setOnClickListener(v -> {
                if (viewPager.getCurrentItem() < pageCount - 1) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            });

            tvPageCounter.setText("1 / " + pageCount);

        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void openPdf(String path) throws IOException {
        File file = new File(path);
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(fileDescriptor);
    }

    @Override
    protected void onDestroy() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class PdfPagerAdapter extends FragmentStateAdapter {
        private int count;
        private String path;

        public PdfPagerAdapter(@NonNull AppCompatActivity activity, int count, String path) {
            super(activity);
            this.count = count;
            this.path = path;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return PdfPageFragment.newInstance(path, position);
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }
}
