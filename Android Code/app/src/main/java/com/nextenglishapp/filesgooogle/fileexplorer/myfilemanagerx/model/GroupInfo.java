package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model;

import java.util.List;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.fragment.RootsFragment.Item;

/**
 * Created by HaKr on 07/08/16.
 */

public class GroupInfo {
    public String label;
    public List<Item> itemList;

    public GroupInfo(String text, List<Item> list){
        label = text;
        itemList = list;
    }
}
