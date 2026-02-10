package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity
import com.nextguidance.filesexplorer.filemanager.smartfiles.R
import kotlinx.coroutines.*
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.*

class LargeFilesFragment : Fragment() {

    companion object {
        const val TAG = "LargeFilesFragment"

        @JvmStatic
        fun show(fm: FragmentManager) {
            val fragment = LargeFilesFragment()
            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
    }

    private lateinit var rvLargeFiles: RecyclerView
    private lateinit var adapter: LargeFilesAdapter
    private lateinit var bottomActionBar: View
    private lateinit var topBar: View
    private lateinit var tvTitle: TextView
    private lateinit var llSelectAll: View
    private lateinit var llSelectInterval: View
    private lateinit var ivSelectInterval: ImageView
    private lateinit var tvSelectInterval: TextView
    private lateinit var btnClose: ImageView

    private var isSelectionMode = false
    private var isIntervalMode = false
    private var intervalStartPos = -1
    private var largeFiles = mutableListOf<AnalysisFragment.FileItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_large_files, container, false)
        rvLargeFiles = view.findViewById(R.id.rv_large_files)
        bottomActionBar = view.findViewById(R.id.bottom_action_bar)
        topBar = view.findViewById(R.id.top_bar)
        tvTitle = view.findViewById(R.id.tv_title)
        llSelectAll = view.findViewById(R.id.ll_select_all)
        llSelectInterval = view.findViewById(R.id.ll_select_interval)
        ivSelectInterval = view.findViewById(R.id.iv_select_interval)
        tvSelectInterval = view.findViewById(R.id.tv_select_interval)
        btnClose = view.findViewById(R.id.btn_close)

        rvLargeFiles.layoutManager = LinearLayoutManager(context)
        
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as? DocumentsActivity)?.let { act ->
            act.supportActionBar?.hide()
            act.findViewById<View>(R.id.banner_container)?.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as? DocumentsActivity)?.let { act ->
            act.supportActionBar?.show()
            act.findViewById<View>(R.id.banner_container)?.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSelectionMode) {
                    exitSelectionMode()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        })

        btnClose.setOnClickListener {
            if (isSelectionMode) {
                exitSelectionMode()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        llSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        llSelectInterval.setOnClickListener {
            toggleIntervalMode()
        }

        view.findViewById<View>(R.id.btn_move_to).setOnClickListener {
            showPasteToBottomSheet()
        }

        view.findViewById<View>(R.id.btn_delete).setOnClickListener {
            showDeleteConfirmation()
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cache = AnalysisFragment.getCache()
            if (cache != null && cache.largeFiles.isNotEmpty()) {
                largeFiles.clear()
                largeFiles.addAll(cache.largeFiles)
            } else {
                // Scan if cache is empty or invalid
                val scanned = scanLargeFiles()
                largeFiles.clear()
                largeFiles.addAll(scanned)
            }

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    adapter = LargeFilesAdapter(largeFiles, { pos -> onItemClicked(pos) }, { pos -> onItemLongClicked(pos) })
                    rvLargeFiles.adapter = adapter
                }
            }
        }
    }

    private suspend fun scanLargeFiles(): List<AnalysisFragment.FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AnalysisFragment.FileItem>()
        val context = context ?: return@withContext results
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        
        val threshold = 50 * 1024 * 1024L
        
        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                "${MediaStore.Files.FileColumns.SIZE} >= ?",
                arrayOf(threshold.toString()),
                "${MediaStore.Files.FileColumns.SIZE} DESC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataIdx) ?: continue
                    val file = File(path)
                    if (file.exists() && !path.contains("/Android/data/") && !path.contains("/Android/obb/")) {
                        val item = AnalysisFragment.FileItem().apply {
                            name = cursor.getString(nameIdx) ?: file.name
                            this.path = file.parent ?: ""
                            size = cursor.getLong(sizeIdx)
                            fullPath = path
                        }
                        results.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    private fun onItemClicked(position: Int) {
        val item = largeFiles[position]
        openFileDirect(File(item.fullPath))
    }

    private fun onItemLongClicked(position: Int) {
        if (!isSelectionMode) {
            enterSelectionMode()
        }
        adapter.toggleSelection(position)
        updateUI()
    }

    private fun openFileDirect(file: File) {
        val contextSafe = context ?: return
        if (!file.exists()) {
            Toast.makeText(contextSafe, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val extension = file.extension.lowercase()
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            val uri = androidx.core.content.FileProvider.getUriForFile(contextSafe, "${contextSafe.packageName}.provider", file)
            val fileName = file.name.lowercase()

            // Internal Video/Audio Player
            if (isVideoFile(fileName) || fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a")) {
                val intent = Intent(contextSafe, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.VideoPlayerActivity::class.java)
                intent.data = uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
                return
            }

            // Internal Image Viewer
            if (mimeType.startsWith("image/")) {
                val intent = Intent(contextSafe, com.nextguidance.filesexplorer.filemanager.smartfiles.activities.ImageViewerActivity::class.java)
                intent.data = uri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            val pm = contextSafe.packageManager
            val resolveInfo = pm.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)

            if (resolveInfo?.activityInfo != null) {
                val pkgName = resolveInfo.activityInfo.packageName
                val clsName = resolveInfo.activityInfo.name

                // Avoid recursive call to self if it's our own generic activity that might fail
                if (pkgName == contextSafe.packageName && (clsName.contains("StandaloneActivity") || clsName.contains("NoteActivity"))) {
                    Toast.makeText(contextSafe, "Cannot open this file internally", Toast.LENGTH_SHORT).show()
                } else {
                    intent.component = android.content.ComponentName(pkgName, clsName)
                    startActivity(intent)
                }
            } else {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (fallback.resolveActivity(pm) != null) {
                    startActivity(fallback)
                } else {
                    Toast.makeText(contextSafe, "No app found to open this file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(contextSafe, "Cannot open: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isVideoFile(fileName: String) = fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mkv") || fileName.endsWith(".mov") || fileName.endsWith(".wmv") || fileName.endsWith(".flv") || fileName.endsWith(".webm") || fileName.endsWith(".3gp") || fileName.endsWith(".m4v")

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.setSelectionMode(true)
        llSelectAll.visibility = View.VISIBLE
        llSelectInterval.visibility = View.VISIBLE
        bottomActionBar.visibility = View.VISIBLE
        tvTitle.text = "0 Selected"
        (activity as? DocumentsActivity)?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        isIntervalMode = false
        intervalStartPos = -1
        adapter.setSelectionMode(false)
        adapter.clearSelection()
        llSelectAll.visibility = View.GONE
        llSelectInterval.visibility = View.GONE
        bottomActionBar.visibility = View.GONE
        tvTitle.text = "Large Files"
        updateIntervalUI()
        (activity as? DocumentsActivity)?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    private fun toggleIntervalMode() {
        isIntervalMode = !isIntervalMode
        intervalStartPos = -1
        updateIntervalUI()
    }

    private fun updateIntervalUI() {
        if (isIntervalMode) {
            ivSelectInterval.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue))
            tvSelectInterval.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
        } else {
            ivSelectInterval.setColorFilter(null)
            ivSelectInterval.imageTintList = android.content.res.ColorStateList.valueOf(0xFF757575.toInt())
            tvSelectInterval.setTextColor(0xFF757575.toInt())
        }
    }

    private fun updateUI() {
        val count = adapter.getSelectedCount()
        tvTitle.text = if (count > 0) "$count Selected" else "0 Selected"
        bottomActionBar.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun showPasteToBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_paste_to, null)
        
        val tvStorageInfo = view.findViewById<TextView>(R.id.tv_storage_info)
        
        // Storage info
        try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            tvStorageInfo.text = "${Formatter.formatFileSize(context, free)} free/${Formatter.formatFileSize(context, total)} total"
        } catch (e: Exception) {
            tvStorageInfo.text = "Internal storage"
        }

        view.findViewById<View>(R.id.ll_internal_storage).setOnClickListener {
            dialog.dismiss()
            moveSelectedFiles()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun moveSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return

        val filesToMove = selectedItems.map { File(it.fullPath) }.filter { it.exists() }
        if (filesToMove.isEmpty()) return

        // Enter Paste Mode and navigate to Internal Storage
        (activity as? DocumentsActivity)?.let { act ->
            exitSelectionMode()
            act.setPasteMode(true, filesToMove, null, true)
            AnalysisDetailFragment.showCleanerInternalStorage(parentFragmentManager)
        }
    }

    private fun showDeleteConfirmation() {
        val count = adapter.getSelectedCount()
        AlertDialog.Builder(context)
            .setMessage("Delete files permanently?")
            .setPositiveButton("Delete") { _, _ -> deleteSelectedFiles() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isEmpty()) return

        val progressDialog = AlertDialog.Builder(context)
            .setTitle("Deleting...")
            .setView(ProgressBar(context).apply { isIndeterminate = true; setPadding(40, 40, 40, 40) })
            .setCancelable(false)
            .show()

        lifecycleScope.launch(Dispatchers.IO) {
            var deletedCount = 0
            selectedItems.forEach { item ->
                try {
                    val file = File(item.fullPath)
                    if (file.exists() && file.delete()) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(context, "$deletedCount files deleted successfully", Toast.LENGTH_SHORT).show()
                refreshListAfterAction()
            }
        }
    }

    private fun refreshListAfterAction() {
        exitSelectionMode()
        // Force refresh cache in AnalysisFragment
        AnalysisFragment.setCache(null)
        loadData()
    }

    inner class LargeFilesAdapter(
        private val items: List<AnalysisFragment.FileItem>,
        private val itemClickListener: (Int) -> Unit,
        private val itemLongClickListener: (Int) -> Unit
    ) : RecyclerView.Adapter<LargeFilesAdapter.ViewHolder>() {

        private val selectedPositions = mutableSetOf<Int>()
        private var isSelectionMode = false

        fun setSelectionMode(enabled: Boolean) {
            isSelectionMode = enabled
            notifyDataSetChanged()
        }

        fun toggleSelection(position: Int) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
            } else {
                selectedPositions.add(position)
            }
            notifyItemChanged(position)
        }

        fun selectItem(position: Int) {
            selectedPositions.add(position)
            notifyItemChanged(position)
        }

        fun selectAll() {
            if (selectedPositions.size == items.size) {
                selectedPositions.clear()
            } else {
                for (i in items.indices) {
                    selectedPositions.add(i)
                }
            }
            notifyDataSetChanged()
            updateUI()
        }

        fun clearSelection() {
            selectedPositions.clear()
            notifyDataSetChanged()
        }

        fun getSelectedCount() = selectedPositions.size
        fun getSelectedItems() = selectedPositions.map { items[it] }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
            val tvFilename: TextView = view.findViewById(R.id.tv_filename)
            val tvPath: TextView = view.findViewById(R.id.tv_path)
            val tvSize: TextView = view.findViewById(R.id.tv_size)
            val ivCheckbox: ImageView = view.findViewById(R.id.iv_checkbox)

            fun bind(item: AnalysisFragment.FileItem, position: Int) {
                tvFilename.text = item.name
                tvPath.text = item.path.replace(Environment.getExternalStorageDirectory().absolutePath, "Internal storage")
                tvSize.text = Formatter.formatFileSize(context, item.size)
                
                ivCheckbox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                if (selectedPositions.contains(position)) {
                    ivCheckbox.setImageResource(R.drawable.ic_check_circle_blue)
                } else {
                    ivCheckbox.setImageResource(R.drawable.ic_checkbox_unselected)
                }

                // Load Thumbnail
                lifecycleScope.launch(Dispatchers.IO) {
                    val file = File(item.fullPath)
                    val ext = file.extension.lowercase()
                    val bitmap = when {
                        ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> {
                            val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                            BitmapFactory.decodeFile(item.fullPath, options)
                        }
                        ext in listOf("mp4", "mkv", "avi", "3gp") -> {
                            ThumbnailUtils.createVideoThumbnail(item.fullPath, MediaStore.Video.Thumbnails.MICRO_KIND)
                        }
                        else -> null
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            ivThumbnail.setImageBitmap(bitmap)
                        } else {
                            ivThumbnail.setImageResource(getFileIcon(item.name))
                        }
                    }
                }

                itemView.setOnClickListener { itemClickListener(position) }
                itemView.setOnLongClickListener {
                    itemLongClickListener(position)
                    true
                }
                
                ivCheckbox.setOnClickListener {
                     itemLongClickListener(position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_large_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount() = items.size

        private fun getFileIcon(name: String): Int {
            val n = name.lowercase()
            return when {
                n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z") -> R.drawable.ic_root_archive
                n.endsWith(".apk") -> R.drawable.ic_root_apps
                n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a") -> R.drawable.ic_root_audio
                else -> R.drawable.ic_root_document
            }
        }
    }
}
