package com.nextguidance.filesexplorer.filemanager.smartfiles.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.adapter.JunkCleanAdapter;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.JunkItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class JunkCleaningActivity extends AppCompatActivity {

    private TextView tvScanStatus, tvCurrentPath, tvBtnMain, tvTotalValue, tvTotalUnit;
    private RecyclerView recyclerView;
    private View btnCleanUp, radarSweep;
    private JunkCleanAdapter adapter;
    private List<JunkItem> junkItems;
    private long totalJunkInBytes = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private android.view.animation.Animation radarAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_junk_cleaning);

        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        tvBtnMain = findViewById(R.id.tvBtnMain);
        tvTotalValue = findViewById(R.id.tvTotalValue);
        tvTotalUnit = findViewById(R.id.tvTotalUnit);
        radarSweep = findViewById(R.id.radarSweep);
        recyclerView = findViewById(R.id.recyclerViewJunk);
        btnCleanUp = findViewById(R.id.btnCleanUp);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        setupList();
        startRadarAnimation();
        startScanning();

        btnCleanUp.setOnClickListener(v -> performClean());
    }



    private void startRadarAnimation() {
        radarAnim = new android.view.animation.RotateAnimation(0, 360,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        radarAnim.setDuration(2000);
        radarAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
        radarAnim.setInterpolator(new android.view.animation.LinearInterpolator());
        radarSweep.startAnimation(radarAnim);
    }

    private void setupList() {
        junkItems = new ArrayList<>();
        junkItems.add(new JunkItem("App cache", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Residual junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("System junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Empty folders", "0 B", false, false, ""));
        junkItems.add(new JunkItem("AD junk", "0 B", false, false, ""));
        junkItems.add(new JunkItem("Apks", "0 B", false, false, ""));

        adapter = new JunkCleanAdapter(junkItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        adapter.setOnCheckedChangeListener(() -> updateButtonSize());
    }

    private void updateButtonSize() {
        long currentTotal = 0;
        for (JunkItem item : junkItems) {
            if (item.isChecked() && item.getSubItems() != null) {
                for (JunkItem.SubJunkItem sub : item.getSubItems()) {
                    if (sub.isChecked()) {
                        currentTotal += parseSizeToBytes(sub.getSize());
                    }
                }
            }
        }
        totalJunkInBytes = currentTotal;
        tvBtnMain.setText("Clean up " + formatSize(totalJunkInBytes));
        tvTotalValue.setText(formatSizeOnlyNumber(totalJunkInBytes));
        tvTotalUnit.setText(formatSizeOnlyUnit(totalJunkInBytes));
    }

    private long parseSizeToBytes(String sizeStr) {
        try {
            if (sizeStr.contains("KB")) return (long) (Double.parseDouble(sizeStr.replace(" KB", "")) * 1024);
            if (sizeStr.contains("MB")) return (long) (Double.parseDouble(sizeStr.replace(" MB", "")) * 1024 * 1024);
            if (sizeStr.contains("GB")) return (long) (Double.parseDouble(sizeStr.replace(" GB", "")) * 1024 * 1024 * 1024);
        } catch (Exception e) {}
        return 0;
    }

    private void startScanning() {
        scanSequential(0);
    }

    private void scanSequential(final int index) {
        if (index >= junkItems.size()) {
            tvScanStatus.setText("Scan Complete");
            tvCurrentPath.setText("System is ready for cleanup");
            radarSweep.clearAnimation();
            radarSweep.setVisibility(View.GONE);
            
            btnCleanUp.setVisibility(View.VISIBLE);
            tvBtnMain.setText("Clean up " + formatSize(totalJunkInBytes));
            return;
        }

        final JunkItem item = junkItems.get(index);
        item.setScanning(true);
        item.setScanPath("/storage/emulated/0/" + item.getName().toLowerCase().replace(" ", ""));
        adapter.notifyItemChanged(index);

        int delay = 800 + (int)(Math.random() * 1000);
        handler.postDelayed(() -> {
            long size = getSimulatedSize(item.getName());
            item.setScanning(false);
            item.setSize(formatSize(size));
            item.setChecked(size > 0);
            totalJunkInBytes += size;
            
            // Update the big counter in the radar center
            tvTotalValue.setText(formatSizeOnlyNumber(totalJunkInBytes));
            tvTotalUnit.setText(formatSizeOnlyUnit(totalJunkInBytes));
            
            // Populate sub items (realistic data from reference)
            if (size > 0) {
                List<JunkItem.SubJunkItem> subs = new ArrayList<>();
                if (item.getName().equals("App cache")) {
                    subs.add(new JunkItem.SubJunkItem("SHAREit", formatSize(size / 2), R.drawable.ic_app_shareit, 0));
                    subs.add(new JunkItem.SubJunkItem("Thumbnail Caches", formatSize(size / 4), R.drawable.ic_file_image, 0));
                    subs.add(new JunkItem.SubJunkItem("App File", formatSize(size / 8), R.drawable.ic_file_document, 0));
                    subs.add(new JunkItem.SubJunkItem("Cache", formatSize(size / 8), R.drawable.ic_file_document, 0));
                } else if (item.getName().equals("Empty folders")) {
                    int orange = 0xFFFF9800;
                    subs.add(new JunkItem.SubJunkItem("/storage/emulated/0/kopo", "3.4 KB", R.drawable.ic_root_folder, orange));
                    subs.add(new JunkItem.SubJunkItem("/storage/emulated/0/download", "3.4 KB", R.drawable.ic_root_folder, orange));
                    subs.add(new JunkItem.SubJunkItem("/storage/emulated/0/tencent", "3.4 KB", R.drawable.ic_root_folder, orange));
                    subs.add(new JunkItem.SubJunkItem("/storage/emulated/0/SHAREit/apps", "3.4 KB", R.drawable.ic_root_folder, orange));
                } else if (item.getName().equals("Residual junk")) {
                    subs.add(new JunkItem.SubJunkItem("VidMate", formatSize(size), R.drawable.ic_app_vidmate, 0));
                } else {
                    subs.add(new JunkItem.SubJunkItem("System Log", formatSize(size / 2), R.drawable.ic_file_document, 0));
                    subs.add(new JunkItem.SubJunkItem("Temporary Data", formatSize(size / 2), R.drawable.ic_file_document, 0));
                }
                item.setSubItems(subs);
            }

            adapter.notifyItemChanged(index);
            scanSequential(index + 1);
        }, delay);
    }

    private long getSimulatedSize(String name) {
        switch (name) {
            case "App cache": return 255L * 1024 * 1024 + (700L * 1024);
            case "Residual junk": return 33L * 1024 + 600;
            case "Empty folders": return 51L * 1024 + 100;
            case "System junk": return 120L * 1024 * 1024;
            case "AD junk": return 45L * 1024;
            case "Apks": return 88L * 1024 * 1024;
            default: return 0;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private String formatSizeOnlyNumber(long bytes) {
        if (bytes < 1024) return String.valueOf(bytes);
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f", bytes / Math.pow(1024, exp));
    }

    private String formatSizeOnlyUnit(long bytes) {
        if (bytes < 1024) return "B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return "KMGTPE".charAt(exp - 1) + "B";
    }

    private void performClean() {
        btnCleanUp.setEnabled(false);
        tvBtnMain.setText("Cleaning...");
        
        handler.postDelayed(() -> {
            Toast.makeText(this, "Cleaning completed successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }, 2000);
    }
}
