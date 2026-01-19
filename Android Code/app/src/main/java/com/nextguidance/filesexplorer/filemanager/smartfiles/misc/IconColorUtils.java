/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nextguidance.filesexplorer.filemanager.smartfiles.misc;

import android.content.Context;

import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;

import com.nextguidance.filesexplorer.filemanager.smartfiles.R;
import com.nextguidance.filesexplorer.filemanager.smartfiles.model.DocumentsContract.Document;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.ExtraDocumentsProvider;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.MediaDocumentsProvider;
import com.nextguidance.filesexplorer.filemanager.smartfiles.provider.NonMediaDocumentsProvider;

import static com.nextguidance.filesexplorer.filemanager.smartfiles.network.NetworkConnection.CLIENT;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.network.NetworkConnection.SERVER;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.provider.CloudStorageProvider.TYPE_BOX;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.provider.CloudStorageProvider.TYPE_DROPBOX;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.provider.CloudStorageProvider.TYPE_GDRIVE;
import static com.nextguidance.filesexplorer.filemanager.smartfiles.provider.CloudStorageProvider.TYPE_ONEDRIVE;

public class IconColorUtils {

    private static ArrayMap<String, Integer> sMimeColors = new ArrayMap<>();

    private static void add(String mimeType, int resId) {
        if (sMimeColors.put(mimeType, resId) != null) {
            throw new RuntimeException(mimeType + " already registered!");
        }
    }

    static {
        int icon;


        icon = R.color.item_apps;
        add("application/vnd.android.package-archive", icon);


        icon = R.color.item_audio;
        add("application/ogg", icon);
        add("application/x-flac", icon);


        icon = R.color.item_wifi_share;
        add("application/pgp-keys", icon);
        add("application/pgp-signature", icon);
        add("application/x-pkcs12", icon);
        add("application/x-pkcs7-certreqresp", icon);
        add("application/x-pkcs7-crl", icon);
        add("application/x-x509-ca-cert", icon);
        add("application/x-x509-user-cert", icon);
        add("application/x-pkcs7-certificates", icon);
        add("application/x-pkcs7-mime", icon);
        add("application/x-pkcs7-signature", icon);


        icon = R.color.item_wifi_share;
        add("application/rdf+xml", icon);
        add("application/rss+xml", icon);
        add("application/x-object", icon);
        add("application/xhtml+xml", icon);
        add("text/css", icon);
        add("text/html", icon);
        add("text/xml", icon);
        add("text/x-c++hdr", icon);
        add("text/x-c++src", icon);
        add("text/x-chdr", icon);
        add("text/x-csrc", icon);
        add("text/x-dsrc", icon);
        add("text/x-csh", icon);
        add("text/x-haskell", icon);
        add("text/x-java", icon);
        add("text/x-literate-haskell", icon);
        add("text/x-pascal", icon);
        add("text/x-tcl", icon);
        add("text/x-tex", icon);
        add("application/x-latex", icon);
        add("application/x-texinfo", icon);
        add("application/atom+xml", icon);
        add("application/ecmascript", icon);
        add("application/json", icon);
        add("application/javascript", icon);
        add("application/xml", icon);
        add("text/javascript", icon);
        add("application/x-javascript", icon);


        icon = R.color.item_audio;
        add("application/mac-binhex40", icon);
        add("application/rar", icon);
        add("application/zip", icon);
        add("application/x-apple-diskimage", icon);
        add("application/x-debian-package", icon);
        add("application/x-gtar", icon);
        add("application/x-iso9660-image", icon);
        add("application/x-lha", icon);
        add("application/x-lzh", icon);
        add("application/x-lzx", icon);
        add("application/x-stuffit", icon);
        add("application/x-tar", icon);
        add("application/x-webarchive", icon);
        add("application/x-webarchive-xml", icon);
        add("application/gzip", icon);
        add("application/x-7z-compressed", icon);
        add("application/x-deb", icon);
        add("application/x-rar-compressed", icon);


        icon = R.color.item_analysis;
        add("text/x-vcard", icon);
        add("text/vcard", icon);


        icon = R.color.item_cast_queue;
        add("text/calendar", icon);
        add("text/x-vcalendar", icon);


        icon = R.color.item_wifi_share;
        add("application/x-font", icon);
        add("application/font-woff", icon);
        add("application/x-font-woff", icon);
        add("application/x-font-ttf", icon);


        icon = R.color.item_images;
        add("application/vnd.oasis.opendocument.graphics", icon);
        add("application/vnd.oasis.opendocument.graphics-template", icon);
        add("application/vnd.oasis.opendocument.image", icon);
        add("application/vnd.stardivision.draw", icon);
        add("application/vnd.sun.xml.draw", icon);
        add("application/vnd.sun.xml.draw.template", icon);


        icon = R.color.item_documents;
        add("application/pdf", icon);


        icon = R.color.item_connections;
        add("application/vnd.stardivision.impress", icon);
        add("application/vnd.sun.xml.impress", icon);
        add("application/vnd.sun.xml.impress.template", icon);
        add("application/x-kpresenter", icon);
        add("application/vnd.oasis.opendocument.presentation", icon);


        icon = R.color.item_cleaner;
        add("application/vnd.oasis.opendocument.spreadsheet", icon);
        add("application/vnd.oasis.opendocument.spreadsheet-template", icon);
        add("application/vnd.stardivision.calc", icon);
        add("application/vnd.sun.xml.calc", icon);
        add("application/vnd.sun.xml.calc.template", icon);
        add("application/x-kspread", icon);


        icon = R.color.item_transfer_pc;
        add("application/vnd.oasis.opendocument.text", icon);
        add("application/vnd.oasis.opendocument.text-master", icon);
        add("application/vnd.oasis.opendocument.text-template", icon);
        add("application/vnd.oasis.opendocument.text-web", icon);
        add("application/vnd.stardivision.writer", icon);
        add("application/vnd.stardivision.writer-global", icon);
        add("application/vnd.sun.xml.writer", icon);
        add("application/vnd.sun.xml.writer.global", icon);
        add("application/vnd.sun.xml.writer.template", icon);
        add("application/x-abiword", icon);
        add("application/x-kword", icon);


        icon = R.color.item_video;
        add("application/x-quicktimeplayer", icon);
        add("application/x-shockwave-flash", icon);


        icon = R.color.item_transfer_pc;
        add("application/msword", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", icon);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", icon);


        icon = R.color.item_cleaner;
        add("application/vnd.ms-excel", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", icon);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", icon);


        icon = R.color.item_connections;
        add("application/vnd.ms-powerpoint", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.template", icon);
        add("application/vnd.openxmlformats-officedocument.presentationml.slideshow", icon);


        icon = R.color.item_doc_file;
        add(Document.MIME_TYPE_HIDDEN, icon);
    }

    public static int loadMimeColor(Context context, String mimeType,
                                    String authority, String docId, int defaultColor) {

        if (Utils.isDir(mimeType)) {
            if (MediaDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(MediaDocumentsProvider.TYPE_ALBUM)){
                    return ContextCompat.getColor(context, R.color.item_audio);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_IMAGES_BUCKET)){
                    return ContextCompat.getColor(context, R.color.item_images);
                }
                else if(docId.startsWith(MediaDocumentsProvider.TYPE_VIDEOS_BUCKET)){
                    return ContextCompat.getColor(context, R.color.item_video);
                }
            } else if (NonMediaDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(NonMediaDocumentsProvider.TYPE_APK_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_apps);
                }
                else if(docId.startsWith(NonMediaDocumentsProvider.TYPE_ARCHIVE_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_audio);
                }
                else if(docId.startsWith(NonMediaDocumentsProvider.TYPE_DOCUMENT_ROOT)){
                    return ContextCompat.getColor(context, R.color.item_documents);
                }
            } else if (ExtraDocumentsProvider.AUTHORITY.equals(authority)){
                if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_WHATSAPP)){
                    return ContextCompat.getColor(context, R.color.item_whatsapp);
                }
                else if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_TELEGRAMX)){
                    return ContextCompat.getColor(context, R.color.item_telegramx);
                }
                else if(docId.startsWith(ExtraDocumentsProvider.ROOT_ID_TELEGRAM)){
                    return ContextCompat.getColor(context, R.color.item_telegram);
                }
            }
            
            // Common folder names colorful styling
            String idLower = docId.toLowerCase();
            if (idLower.contains("download")) {
                return ContextCompat.getColor(context, R.color.item_downloads);
            } else if (idLower.contains("dcim") || idLower.contains("camera") || idLower.contains("picture")) {
                return ContextCompat.getColor(context, R.color.item_images);
            } else if (idLower.contains("movie") || idLower.contains("video")) {
                return ContextCompat.getColor(context, R.color.item_video);
            } else if (idLower.contains("music") || idLower.contains("audio") || idLower.contains("notification") || idLower.contains("ringtone")) {
                return ContextCompat.getColor(context, R.color.item_audio);
            } else if (idLower.contains("document") || idLower.contains("pdf")) {
                return ContextCompat.getColor(context, R.color.item_documents);
            } else if (idLower.contains("whatsapp") || idLower.contains("telegram") || idLower.contains("facebook") || idLower.contains("instagram")) {
                return ContextCompat.getColor(context, R.color.item_cleaner);
            }
            
            return ContextCompat.getColor(context, R.color.item_connections); // Default vibrant folder color
        }


        Integer resId = sMimeColors.get(mimeType);
        if (resId != null) {
            return ContextCompat.getColor(context, resId);
        }

        if (mimeType == null) {

            return ContextCompat.getColor(context, R.color.item_doc_generic);
        }


        final String typeOnly = mimeType.split("/")[0];

        if ("audio".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_audio);
        } else if ("image".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_images);
        } else if ("text".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_transfer_pc);
        } else if ("video".equals(typeOnly)) {
            return ContextCompat.getColor(context, R.color.item_video);
        } else {
            return ContextCompat.getColor(context, R.color.item_wifi_share);
        }
    }

    public static int loadSchmeColor(Context context, String type) {

        if (SERVER.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_server);
        } else if (CLIENT.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_client);
        } else {
            return ContextCompat.getColor(context, R.color.item_connection_server);
        }
    }

    public static int loadCloudColor(Context context, String type) {

        if (TYPE_GDRIVE.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_gdrive);
        } else if (TYPE_DROPBOX.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_dropbox);
        } else if (TYPE_ONEDRIVE.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_onedrive);
        } else if (TYPE_BOX.equals(type)) {
            return ContextCompat.getColor(context, R.color.item_connection_box);
        } else {
            return ContextCompat.getColor(context, R.color.item_connection_cloud);
        }
    }
}