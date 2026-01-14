package com.nextguidance.filesexplorer.filemanager.smartfiles.model;

import java.util.ArrayList;
import java.util.List;

public class JunkItem {
    private String name;
    private String size;
    private boolean isChecked;
    private boolean isScanning;
    private String scanPath;
    private boolean isExpanded = false;
    private List<SubJunkItem> subItems = new ArrayList<>();

    public JunkItem(String name, String size, boolean isChecked, boolean isScanning, String scanPath) {
        this.name = name;
        this.size = size;
        this.isChecked = isChecked;
        this.isScanning = isScanning;
        this.scanPath = scanPath;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    public boolean isScanning() { return isScanning; }
    public void setScanning(boolean scanning) { isScanning = scanning; }

    public String getScanPath() { return scanPath; }
    public void setScanPath(String scanPath) { this.scanPath = scanPath; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public List<SubJunkItem> getSubItems() { return subItems; }
    public void setSubItems(List<SubJunkItem> subItems) { this.subItems = subItems; }

    public static class SubJunkItem {
        private String label;
        private String size;
        private int iconResId;
        private boolean isChecked = true;
        private int tintColor = 0; // 0 means no tint

        public SubJunkItem(String label, String size, int iconResId) {
            this.label = label;
            this.size = size;
            this.iconResId = iconResId;
        }

        public SubJunkItem(String label, String size, int iconResId, int tintColor) {
            this.label = label;
            this.size = size;
            this.iconResId = iconResId;
            this.tintColor = tintColor;
        }

        public String getLabel() { return label; }
        public String getSize() { return size; }
        public int getIconResId() { return iconResId; }
        public boolean isChecked() { return isChecked; }
        public void setChecked(boolean checked) { isChecked = checked; }
        public int getTintColor() { return tintColor; }
    }
}
