package com.nextguidance.filesexplorer.filemanager.smartfiles.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.nextguidance.filesexplorer.filemanager.smartfiles.R
import java.io.File

class DocumentViewerActivity : AppCompatActivity() {

    private var fileName: String = "Document"
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Document Viewer"

        val ivDocType = findViewById<ImageView>(R.id.iv_doc_type)
        val tvFileName = findViewById<TextView>(R.id.tv_file_name)
        val tvFileSize = findViewById<TextView>(R.id.tv_file_size)
        val btnOpenExternal = findViewById<View>(R.id.btn_open_external)

        fileUri = intent.data
        if (fileUri == null && intent.hasExtra(Intent.EXTRA_STREAM)) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (fileUri != null) {
            Toast.makeText(this, "Opening Document...", Toast.LENGTH_SHORT).show()
            fileName = getFileName(fileUri!!)
            tvFileName.text = fileName
            
            // Set icon based on extension
            setDocIcon(fileName, ivDocType)
            
            // Try to get size
            getFileSize(fileUri!!, tvFileSize)
        } else {
            tvFileName.text = "Cannot load file"
            Toast.makeText(this, "File not found or invalid URI", Toast.LENGTH_LONG).show()
        }

        btnOpenExternal.setOnClickListener {
            openInExternalApp()
        }
        
        loadBannerAd()
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        try {
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = it.getString(index)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentViewer", "Error querying filename", e)
        }
        
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "Document"
    }

    private fun setDocIcon(name: String, iv: ImageView) {
        val n = name.lowercase()
        iv.setImageResource(R.drawable.ic_root_document) // Default
        try {
            val color = when {
                n.endsWith(".pdf") -> androidx.core.content.ContextCompat.getColor(this, R.color.item_doc_pdf)
                n.endsWith(".doc") || n.endsWith(".docx") -> androidx.core.content.ContextCompat.getColor(this, R.color.item_doc_word)
                n.endsWith(".xls") || n.endsWith(".xlsx") -> androidx.core.content.ContextCompat.getColor(this, R.color.item_doc_excel)
                n.endsWith(".ppt") || n.endsWith(".pptx") -> androidx.core.content.ContextCompat.getColor(this, R.color.item_doc_powerpoint)
                else -> androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)
            }
            iv.setColorFilter(color)
        } catch (e: Exception) {
            iv.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor))
        }
    }

    private fun loadBannerAd() {
        try {
            val adView = com.google.android.gms.ads.AdView(this)
            adView.adUnitId = getString(R.string.admob_banner)
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            
            val container = findViewById<android.widget.FrameLayout>(R.id.banner_container)
            if (container != null) {
                container.removeAllViews()
                container.addView(adView)
                
                val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }
        } catch (e: Exception) {
            Log.e("DocumentViewer", "Error loading banner ad", e)
        }
    }

    private fun getFileSize(uri: Uri, tv: TextView) {
        try {
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            val size = cursor.getLong(sizeIndex)
                            tv.text = formatSize(size)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    tv.text = formatSize(file.length())
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentViewer", "Error getting file size", e)
            tv.text = ""
        }
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun openInExternalApp() {
        val uri = fileUri ?: return
        
        val mimeType = getMimeType(fileName)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        val myPackage = packageName
        val resInfo = packageManager.queryIntentActivities(intent, 0)
        
        if (resInfo.isNotEmpty()) {
            val targetedIntents = mutableListOf<Intent>()
            for (info in resInfo) {
                if (info.activityInfo.packageName != myPackage) {
                    val i = Intent(intent)
                    i.setPackage(info.activityInfo.packageName)
                    targetedIntents.add(i)
                }
            }
            
            if (targetedIntents.isNotEmpty()) {
                val chooser = Intent.createChooser(targetedIntents.removeAt(0), "Open with...")
                if (targetedIntents.isNotEmpty()) {
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
                }
                startActivity(chooser)
            } else {
                Toast.makeText(this, "No other app found to open this file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(name: String): String {
        val n = name.lowercase()
        return when {
            n.endsWith(".pdf") -> "application/pdf"
            n.endsWith(".doc") || n.endsWith(".docx") -> "application/msword"
            n.endsWith(".xls") || n.endsWith(".xlsx") -> "application/vnd.ms-excel"
            n.endsWith(".ppt") || n.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
            n.endsWith(".txt") -> "text/plain"
            else -> "*/*"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
