package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity
import com.nextguidance.filesexplorer.filemanager.smartfiles.R
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class AnalysisDetailFragment : Fragment() {

    companion object {
        const val TAG = "AnalysisDetailFragment"
        private const val ARG_TITLE = "title"
        private const val ARG_FOLDERS = "folders"
        private const val ARG_DUPLICATES = "duplicates"
        private const val ARG_LARGE_FILES = "large_files"
        private const val ARG_APPS = "apps"

        @JvmStatic
        fun show(fm: FragmentManager?, title: String,
                 folders: List<AnalysisFragment.FolderItem>?,
                 duplicates: List<AnalysisFragment.DuplicateGroup>?,
                 largeFiles: List<AnalysisFragment.FileItem>?,
                 apps: List<AnalysisFragment.AppItem>?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            folders?.let { args.putSerializable(ARG_FOLDERS, it as Serializable) }
            duplicates?.let { args.putSerializable(ARG_DUPLICATES, it as Serializable) }
            largeFiles?.let { args.putSerializable(ARG_LARGE_FILES, it as Serializable) }
            apps?.let { args.putSerializable(ARG_APPS, it as Serializable) }
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
    }

    private lateinit var recyclerView: RecyclerView
    private var title: String? = null
    private var adapter: DetailAdapter? = null

    private var bottomBar: View? = null
    private var cleanupButton: Button? = null
    private var summaryText: TextView? = null
    private var selectAllButton: ImageView? = null

    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<DetailAdapter.DetailItem>()
    private var mActionMode: android.view.ActionMode? = null

    private val folderStack = Stack<File>()
    private var isInFolderNavigation = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_analysis_detail, container, false)
        recyclerView = view.findViewById(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(context)

        try {
            bottomBar = view.findViewById(R.id.bottom_bar)
            cleanupButton = view.findViewById(R.id.btn_cleanup)
            summaryText = view.findViewById(R.id.summary_text)
            selectAllButton = view.findViewById(R.id.btn_select_all)

            cleanupButton?.setOnClickListener { performCleanup() }
            selectAllButton?.setOnClickListener { toggleSelectAll() }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bottom bar views not found")
        }

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                handleBackPressed()
            } else false
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = arguments?.getString(ARG_TITLE)
        updateToolbarTitle()
        loadData()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!this@AnalysisDetailFragment.handleBackPressed()) {
                    fragmentManager?.popBackStack()
                }
            }
        })
    }

    private fun updateToolbarTitle() {
        (activity as? DocumentsActivity)?.setAnalysisMode(true, title)
    }

    override fun onResume() {
        super.onResume()
        updateToolbarTitle()
    }

    override fun onDestroy() {
        if (isSelectionMode) {
            (activity as? DocumentsActivity)?.setActionMode(false)
        }
        (activity as? DocumentsActivity)?.setAnalysisMode(false)
        super.onDestroy()
    }

    private fun handleBackPressed(): Boolean {
        if (isSelectionMode) {
            exitSelectionMode()
            return true
        }
        if (isInFolderNavigation && folderStack.isNotEmpty()) {
            folderStack.pop()
            if (folderStack.isNotEmpty()) {
                openFolderInternally(folderStack.peek())
                return true
            } else {
                isInFolderNavigation = false
                loadData()
                return true
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData() {
        try {
            val args = arguments ?: return
            isInFolderNavigation = false
            folderStack.clear()

            val items = mutableListOf<DetailAdapter.DetailItem>()
            var isDuplicateView = false

            when {
                args.containsKey(ARG_FOLDERS) -> {
                    val folders = args.getSerializable(ARG_FOLDERS) as? List<AnalysisFragment.FolderItem>
                    folders?.forEach { folder ->
                        items.add(DetailAdapter.DetailItem().apply {
                            name = folder.name
                            path = getShortPath(folder.path)
                            subtitle = "${folder.itemCount} items"
                            size = try { Formatter.formatFileSize(context, folder.size) } catch(e: Exception) { "0 B" }
                            file = File(folder.path)
                            isFolder = true
                            icon = R.drawable.ic_root_folder
                            iconColor = getFolderIconColor(folder.name)
                        })
                    }
                }
                args.containsKey(ARG_DUPLICATES) -> {
                    isDuplicateView = true
                    val duplicates = args.getSerializable(ARG_DUPLICATES) as? List<AnalysisFragment.DuplicateGroup>
                    duplicates?.let {
                        var totalDuplicates = 0
                        var totalSize = 0L
                        it.forEach { group ->
                            totalDuplicates += group.filePaths.size
                            totalSize += group.size * group.filePaths.size
                            group.filePaths.forEach { filePath ->
                                try {
                                    val f = File(filePath)
                                    items.add(DetailAdapter.DetailItem().apply {
                                        name = group.fileName
                                        path = getShortPath(filePath)
                                        size = try { Formatter.formatFileSize(context, group.size) } catch(e: Exception) { "0 B" }
                                        subtitle = getFileDate(f)
                                        file = f
                                        isFolder = false
                                        isSelectable = true
                                        icon = getFileIcon(group.fileName)
                                        iconColor = -0x3ef9 // 0xFFFFC107
                                    })
                                } catch (e: Exception) {}
                            }
                        }
                        bottomBar?.visibility = View.VISIBLE
                        summaryText?.text = "Duplicate files: $totalDuplicates  Size: ${try { Formatter.formatFileSize(context, totalSize) } catch(e: Exception) { "" }}"
                    }
                }
                args.containsKey(ARG_LARGE_FILES) -> {
                    val largeFiles = args.getSerializable(ARG_LARGE_FILES) as? List<AnalysisFragment.FileItem>
                    largeFiles?.forEach { fileInfo ->
                        try {
                            items.add(DetailAdapter.DetailItem().apply {
                                name = fileInfo.name
                                val fileName = fileInfo.name.lowercase()
                                if (fileName.endsWith(".apk")) {
                                    path = null
                                    subtitle = getFileDate(File(fileInfo.fullPath))
                                } else {
                                    path = getShortPath(fileInfo.fullPath)
                                    subtitle = null
                                }
                                size = try { Formatter.formatFileSize(context, fileInfo.size) } catch(e: Exception) { "0 B" }
                                file = File(fileInfo.fullPath)
                                isFolder = false
                                icon = getFileIcon(fileInfo.name)
                                iconColor = -0x6800 // 0xFFFF9800
                            })
                        } catch (e: Exception) {}
                    }
                }
                args.containsKey(ARG_APPS) -> {
                    val apps = args.getSerializable(ARG_APPS) as? List<AnalysisFragment.AppItem>
                    apps?.forEach { app ->
                        items.add(DetailAdapter.DetailItem().apply {
                            name = app.name
                            path = app.packageName
                            subtitle = getInstallDate()
                            size = try { Formatter.formatFileSize(context, app.size) } catch(e: Exception) { "0 B" }
                            packageName = app.packageName
                            isApp = true
                            icon = R.drawable.ic_root_apps
                            iconColor = 0
                        })
                    }
                }
            }



            adapter = DetailAdapter(items, isSelectionMode, lifecycleScope,
                this::handleItemClick, this::handleSelectionChanged, this::handleItemLongClick)
            recyclerView.adapter = adapter
            
            // Restore selection if in mode
             if (isSelectionMode) {
                adapter?.setSelectedItems(selectedItems)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading data", e)
            Toast.makeText(context, "Error displaying items", Toast.LENGTH_SHORT).show()
        }
    }



    private fun handleItemClick(item: DetailAdapter.DetailItem) {
        item.packageName?.let { openAppInfo(it) } ?: run {
            if (item.isFolder) {
                openFolderInternally(item.file)
            } else if (item.file != null && !item.isSelectable) {
                openFileDirect(item.file!!)
            }
        }
    }

    private fun handleItemLongClick(item: DetailAdapter.DetailItem) {
        if (!isSelectionMode) {
            startSelectionMode(item)
        }
    }
    
    private fun startSelectionMode(initialItem: DetailAdapter.DetailItem) {
        isSelectionMode = true
        val act = activity as? DocumentsActivity ?: return
        
        // Get toolbar
        val toolbar = act.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        
        // Start ActionMode
        mActionMode = toolbar?.startActionMode(object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_analysis_selection, menu)
                act.setActionMode(true)
                setupSelectionBar()
                return true
            }
            
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                return false
            }
            
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_select_all -> toggleSelectAll()
                    R.id.action_refresh -> loadData()
                }
                return true
            }
            
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {
                exitSelectionMode()
            }
        })
        
        // Reload adapter
        loadData()
        
        // Select initial item
        adapter?.toggleSelection(initialItem)
    }

    private fun setupSelectionBar() {
        val act = activity as? DocumentsActivity ?: return
        val selectionBar = act.selectionBar ?: return
        
        // Make all buttons visible
        selectionBar.findViewById<View>(R.id.action_copy)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { performOperation("Copy", false) }
        }
        selectionBar.findViewById<View>(R.id.action_move)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { performOperation("Move", false) }
        }
        selectionBar.findViewById<View>(R.id.action_rename)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { performOperation("Rename", true) }
        }
        selectionBar.findViewById<View>(R.id.action_delete)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { performDelete() }
        }
        selectionBar.findViewById<View>(R.id.action_more)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { showMoreMenu(it) }
        }
        
        updateSelectionTitle(0)
    }
    
    private fun showMoreMenu(anchor: View) {
        val popup = android.widget.PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, "Select All")
        popup.menu.add(0, 2, 1, "Share")
        popup.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                1 -> toggleSelectAll()
                2 -> performOperation("Share", false)
            }
            true
        }
        popup.show()
    }
    
    private fun performOperation(op: String, singleItem: Boolean) {
        val selected = adapter?.getSelectedItemsList() ?: return
        if (selected.isEmpty()) return
        
        if (singleItem && selected.size > 1) {
            Toast.makeText(context, "Select only one item for $op", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Placeholder for operations
        Toast.makeText(context, "$op ${selected.size} items", Toast.LENGTH_SHORT).show()
        // Here we would implement real copy/move logic or delegate to DirectoryFragment logic
    }
    
    private fun performDelete() {
        val selected = adapter?.getSelectedItemsList() ?: return
        if (selected.isEmpty()) return
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Delete ${selected.size} items?")
            .setPositiveButton("Delete") { _, _ ->
                 lifecycleScope.launch(Dispatchers.IO) {
                     selected.forEach { it.file?.delete() }
                     withContext(Dispatchers.Main) {
                         exitSelectionMode()
                     }
                 }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        mActionMode?.finish()
        mActionMode = null
        (activity as? DocumentsActivity)?.setActionMode(false)
        loadData()
    }

    private fun handleSelectionChanged(selectedCount: Int, totalSize: Long, items: Set<DetailAdapter.DetailItem>) {
        // Update local tracking
        selectedItems.clear()
        selectedItems.addAll(items)
        
        // Update title to show count
        if (isSelectionMode) {
            updateSelectionTitle(selectedCount)
            
            // Validate Select All icon state
            val totalItems = adapter?.itemCount ?: 0
            val selectAllItem = mActionMode?.menu?.findItem(R.id.action_select_all)
            if (selectedCount > 0 && selectedCount == totalItems) {
                 selectAllItem?.icon?.alpha = 255
            } else {
                 selectAllItem?.icon?.alpha = 130
            }
        }
        
        if (!isSelectionMode) {
            // Logic for "Duplicate/Large File" cleanup mode (existing)
            summaryText?.let { summary ->
                cleanupButton?.let { cleanup ->
                    if (selectedCount > 0) {
                        summary.text = "$selectedCount selected"
                        cleanup.text = "Clean up ${Formatter.formatFileSize(context, totalSize)}"
                        cleanup.isEnabled = true
                    } else {
                        bottomBar?.visibility = View.VISIBLE
                        cleanup.isEnabled = false
                    }
                }
            }
        }
    }
    
    private fun updateSelectionTitle(count: Int) {
        val totalItems = adapter?.itemCount ?: 0
        mActionMode?.title = "$count / $totalItems"
    }

    private fun toggleSelectAll() {
        adapter?.toggleSelectAll()
    }

    private fun performCleanup() {
        val selectedFiles = adapter?.getSelectedFiles() ?: return
        if (selectedFiles.isEmpty()) {
            Toast.makeText(context, "No files selected", Toast.LENGTH_SHORT).show()
            return
        }

        val finalTotalSize = selectedFiles.sumOf { it.length() }

        android.app.AlertDialog.Builder(context)
            .setTitle("Delete Files")
            .setMessage("Delete ${selectedFiles.size} selected files?\n\nThis will free up ${Formatter.formatFileSize(context, finalTotalSize)}")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.Main) {
                    var deletedCount = 0
                    var freedSpace = 0L

                    withContext(Dispatchers.IO) {
                        selectedFiles.forEach { file ->
                            try {
                                val fileSize = file.length()
                                if (file.exists() && file.delete()) {
                                    deletedCount++
                                    freedSpace += fileSize
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    Toast.makeText(context, "Deleted $deletedCount files\n${Formatter.formatFileSize(context, freedSpace)} freed", Toast.LENGTH_LONG).show()
                    fragmentManager?.popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFolderInternally(folder: File?) {
        try {
            if (folder == null || !folder.exists() || !folder.isDirectory) {
                Toast.makeText(context, "Folder not found", Toast.LENGTH_SHORT).show()
                return
            }

            if (!isInFolderNavigation) {
                isInFolderNavigation = true
                folderStack.clear()
            }

            if (folderStack.isEmpty() || folderStack.peek() != folder) {
                folderStack.push(folder)
            }

            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val cache = AnalysisFragment.getCache()
                    val currentPath = folder.absolutePath
                    val items = withContext(Dispatchers.IO) {
                        val result = mutableListOf<DetailAdapter.DetailItem>()
                        val seenPaths = mutableSetOf<String>()

                        // 1. Prefer Cache (Consistent with Scan Results)
                        if (cache != null) {
                            // Sub-folders detected during scan
                            cache.allFoldersMap.forEach { (path, folderInfo) ->
                                if (path != currentPath) {
                                    val f = File(path)
                                    if (f.parent == currentPath && !seenPaths.contains(path)) {
                                        seenPaths.add(path)
                                        result.add(DetailAdapter.DetailItem().apply {
                                            name = folderInfo.name
                                            this.path = getShortPath(path)
                                            this.file = f
                                            isFolder = true
                                            icon = R.drawable.ic_root_folder
                                            iconColor = getFolderIconColor(name ?: "")
                                            size = Formatter.formatFileSize(context, folderInfo.size)
                                            subtitle = "${folderInfo.itemCount} items"
                                        })
                                    }
                                }
                            }
                            // Files detected during scan
                            cache.allFiles.filter { it.path == currentPath }.forEach { fileInfo ->
                                if (!seenPaths.contains(fileInfo.fullPath)) {
                                    seenPaths.add(fileInfo.fullPath)
                                    result.add(DetailAdapter.DetailItem().apply {
                                        name = fileInfo.name
                                        this.path = getShortPath(fileInfo.fullPath)
                                        this.file = File(fileInfo.fullPath)
                                        isFolder = false
                                        icon = getFileIcon(name ?: "")
                                        iconColor = -0xde690d // 0xFF2196F3
                                        size = Formatter.formatFileSize(context, fileInfo.size)
                                        subtitle = getFileDate(this.file!!)
                                    })
                                }
                            }
                        }

                        // 2. Supplement with real-time listFiles()
                        folder.listFiles()?.filter { !it.name.startsWith(".") }?.forEach { file ->
                            val absPath = file.absolutePath
                            if (!seenPaths.contains(absPath)) {
                                seenPaths.add(absPath)
                                try {
                                    result.add(DetailAdapter.DetailItem().apply {
                                        name = file.name
                                        path = getShortPath(absPath)
                                        this.file = file
                                        if (file.isDirectory) {
                                            isFolder = true
                                            icon = R.drawable.ic_root_folder
                                            iconColor = getFolderIconColor(name ?: "")
                                            val cachedFolder = cache?.allFoldersMap?.get(absPath)
                                            if (cachedFolder != null) {
                                                size = Formatter.formatFileSize(context, cachedFolder.size)
                                                subtitle = "${cachedFolder.itemCount} items"
                                            } else {
                                                size = Formatter.formatFileSize(context, getFolderSizeQuick(file))
                                                subtitle = "${countFilesQuick(file)} items"
                                            }
                                        } else {
                                            isFolder = false
                                            icon = getFileIcon(name ?: "")
                                            iconColor = -0xde690d // 0xFF2196F3
                                            size = Formatter.formatFileSize(context, file.length())
                                            subtitle = getFileDate(file)
                                        }
                                    })
                                } catch (e: Exception) {}
                            }
                        }
                        result.sortWith(compareBy({ !it.isFolder }, { it.name?.lowercase() }))
                        result
                    }

                    if (items.isEmpty()) {
                        Toast.makeText(context, "Empty folder", Toast.LENGTH_SHORT).show()
                    } else {
                        title = folder.name
                        updateToolbarTitle()
                        bottomBar?.visibility = View.GONE
                        title = folder.name
                        updateToolbarTitle()
                        bottomBar?.visibility = View.GONE
                        adapter = DetailAdapter(items, isSelectionMode, lifecycleScope, this@AnalysisDetailFragment::handleItemClick, this@AnalysisDetailFragment::handleSelectionChanged, this@AnalysisDetailFragment::handleItemLongClick)
                        recyclerView.adapter = adapter
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open folder", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot access folder", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileDirect(file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mimeType = getMimeType(file)
            val uri = getFileUri(file)
            val fileName = file.name.lowercase()

            if (isVideoFile(fileName)) {
                openVideoDirectly(uri, file)
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            val pm = requireContext().packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

            if (resolveInfo?.activityInfo != null) {
                intent.component = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
                startActivity(intent)
            } else {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (fallback.resolveActivity(pm) != null) {
                    startActivity(fallback)
                } else {
                    Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openVideoDirectly(uri: Uri, file: File) {
        val pm = requireContext().packageManager
        val videoPlayers = arrayOf(
            arrayOf("com.mxtech.videoplayer", "com.mxtech.videoplayer.ActivityScreen"),
            arrayOf("com.mxtech.videoplayer.pro", "com.mxtech.videoplayer.ActivityScreen"),
            arrayOf("com.mxtech.videoplayer.ad", "com.mxtech.videoplayer.ActivityScreen"),
            arrayOf("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"),
            arrayOf("com.brouken.player", "com.brouken.player.PlayerActivity"),
            arrayOf("is.xyz.mpv", "is.xyz.mpv.MPVActivity")
        )

        for (player in videoPlayers) {
            val packageName = player[0]
            val activityName = player[1]
            if (isAppInstalled(pm, packageName)) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        `package` = packageName
                        component = ComponentName(packageName, activityName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("title", file.name)
                    }
                    startActivity(intent)
                    return
                } catch (e: Exception) {}
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.activityInfo != null) {
                intent.component = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAppInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isVideoFile(fileName: String): Boolean {
        val n = fileName.lowercase()
        return n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".mkv") || n.endsWith(".mov") ||
                n.endsWith(".wmv") || n.endsWith(".flv") || n.endsWith(".webm") || n.endsWith(".3gp") ||
                n.endsWith(".m4v") || n.endsWith(".ts") || n.endsWith(".mpg") || n.endsWith(".mpeg")
    }

    private fun getFileUri(file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        } else {
            Uri.fromFile(file)
        }
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open app info", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getShortPath(fullPath: String?): String? {
        if (fullPath == null) return null
        return if (fullPath.contains("/storage/emulated/0/")) fullPath.replace("/storage/emulated/0/", "/") else fullPath
    }

    private fun getFileDate(file: File): String {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            sdf.format(Date(file.lastModified()))
        } catch (e: Exception) { "" }
    }

    private fun getInstallDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun getFileIcon(fileName: String): Int {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || name.endsWith(".tar") || name.endsWith(".gz") -> R.drawable.ic_root_archive
            name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv") || name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".3gp") -> R.drawable.ic_root_video
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".heic") -> R.drawable.ic_root_image
            name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".flac") || name.endsWith(".aac") -> R.drawable.ic_root_audio
            name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx") -> R.drawable.ic_root_document
            name.endsWith(".apk") -> R.drawable.ic_root_apps
            else -> R.drawable.ic_root_document
        }
    }

    private fun getFolderIconColor(folderName: String): Int {
        val name = folderName.lowercase()
        return when {
            name == "android" -> -0x3f5b00 // 0xFF4CAF50
            name == "dcim" || name == "pictures" || name == "camera" -> -0xde690d // 0xFF2196F3
            name == "downloads" || name == "download" -> -0x63d850 // 0xFF9C27B0
            name == "documents" -> -0x6800 // 0xFFFF9800
            name == "music" || name == "audio" -> -0xbba8ce // 0xFFF44336
            name == "movies" || name == "video" -> -0x16e19d // 0xFFE91E63
            else -> -0xde690d // 0xFF2196F3
        }
    }

    private fun getFolderSizeQuick(folder: File): Long {
        var size = 0L
        try {
            folder.listFiles()?.take(101)?.forEach { if (it.isFile) size += it.length() }
        } catch (e: Exception) {}
        return size
    }

    private fun countFilesQuick(folder: File): Int {
        var count = 0
        try {
            folder.listFiles()?.forEach { 
                if (it.isFile) count++ 
                if (count > 100) return 100
            }
        } catch (e: Exception) {}
        return count
    }

    private fun getMimeType(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".mp4") -> "video/mp4"
            name.endsWith(".avi") -> "video/x-msvideo"
            name.endsWith(".mkv") -> "video/x-matroska"
            name.endsWith(".mov") -> "video/quicktime"
            name.endsWith(".webm") -> "video/webm"
            name.endsWith(".3gp") -> "video/3gpp"
            name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
            name.endsWith(".png") -> "image/png"
            name.endsWith(".gif") -> "image/gif"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".mp3") -> "audio/mpeg"
            name.endsWith(".wav") -> "audio/wav"
            name.endsWith(".m4a") -> "audio/mp4"
            name.endsWith(".pdf") -> "application/pdf"
            name.endsWith(".zip") -> "application/zip"
            name.endsWith(".apk") -> "application/vnd.android.package-archive"
            name.endsWith(".txt") -> "text/plain"
            name.endsWith(".doc") || name.endsWith(".docx") -> "application/msword"
            name.endsWith(".xls") || name.endsWith(".xlsx") -> "application/vnd.ms-excel"
            else -> "*/*"
        }
    }

    private class DetailAdapter(
        private val items: List<DetailItem>,
        private val showCheckboxes: Boolean,
        private val scope: CoroutineScope,
        private val clickListener: (DetailItem) -> Unit,
        private val selectionListener: (Int, Long, Set<DetailItem>) -> Unit,
        private val longClickListener: (DetailItem) -> Unit
    ) : RecyclerView.Adapter<DetailAdapter.ViewHolder>() {

        private val selectedPositions = mutableSetOf<Int>() // Changed from Integer to Int for better Kotlin compat
        
        fun toggleSelection(item: DetailItem) {
             val index = items.indexOf(item)
             if (index != -1) {
                 if (selectedPositions.contains(index)) selectedPositions.remove(index)
                 else selectedPositions.add(index)
                 notifyItemChanged(index)
                 updateSelection()
             }
        }
        
        fun setSelectedItems(selected: Set<DetailItem>) {
             selectedPositions.clear()
             selected.forEach { 
                 val idx = items.indexOf(it)
                 if (idx != -1) selectedPositions.add(idx)
             }
             notifyDataSetChanged()
             updateSelection()
        }
        
        fun getSelectedItemsList(): List<DetailItem> {
             return items.filterIndexed { index, _ -> selectedPositions.contains(index) }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.icon)
            val name: TextView = view.findViewById(R.id.name)
            val path: TextView = view.findViewById(R.id.path)
            val size: TextView = view.findViewById(R.id.size)
            val subtitle: TextView? = try { view.findViewById(R.id.subtitle) } catch (e: Exception) { null }
            val checkbox: CheckBox? = try { view.findViewById(R.id.checkbox) } catch (e: Exception) { null }

            fun bind(item: DetailItem, showCheckbox: Boolean, isChecked: Boolean, scope: CoroutineScope) {
                name.text = item.name ?: ""
                path.text = ""
                path.visibility = View.GONE
                size.text = item.size ?: ""
                size.visibility = if (item.size != null) View.VISIBLE else View.GONE
                subtitle?.text = item.subtitle ?: ""
                subtitle?.visibility = if (item.subtitle != null) View.VISIBLE else View.GONE

                checkbox?.visibility = if (showCheckbox) View.VISIBLE else View.GONE
                if (showCheckbox) checkbox?.isChecked = isChecked

                if (item.icon != 0) {
                    icon.setImageResource(item.icon)
                    icon.scaleType = ImageView.ScaleType.CENTER
                    icon.visibility = View.VISIBLE
                    if (item.iconColor != 0 && !item.isApp) {
                        icon.setColorFilter(item.iconColor, PorterDuff.Mode.SRC_IN)
                    }
                }

                if (item.isApp && item.packageName != null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val pm = itemView.context.packageManager
                            val appIcon = pm.getApplicationIcon(item.packageName!!)
                            withContext(Dispatchers.Main) {
                                icon.setImageDrawable(appIcon)
                                icon.clearColorFilter()
                                icon.scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                icon.setImageResource(R.drawable.ic_root_apps)
                            }
                        }
                    }
                } else {
                    item.file?.let { file ->
                        val fileName = file.name.lowercase()
                        when {
                            isVideoFile(fileName) -> {
                                scope.launch(Dispatchers.IO) {
                                    val thumb = ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
                                    withContext(Dispatchers.Main) {
                                        thumb?.let {
                                            icon.setImageBitmap(it)
                                            icon.clearColorFilter()
                                            icon.scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                    }
                                }
                            }
                            isImageFile(fileName) -> {
                                scope.launch(Dispatchers.IO) {
                                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                                    bitmap?.let {
                                        val thumb = ThumbnailUtils.extractThumbnail(it, 120, 120)
                                        withContext(Dispatchers.Main) {
                                            icon.setImageBitmap(thumb)
                                            icon.clearColorFilter()
                                            icon.scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                        if (it != thumb) it.recycle()
                                    }
                                }
                            }
                            fileName.endsWith(".apk") -> {
                                scope.launch(Dispatchers.IO) {
                                    val pm = itemView.context.packageManager
                                    val pkgInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
                                    pkgInfo?.applicationInfo?.let { appInfo ->
                                        appInfo.sourceDir = file.absolutePath
                                        appInfo.publicSourceDir = file.absolutePath
                                        val apkIcon = appInfo.loadIcon(pm)
                                        withContext(Dispatchers.Main) {
                                            icon.setImageDrawable(apkIcon)
                                            icon.clearColorFilter()
                                            icon.scaleType = ImageView.ScaleType.CENTER
                                        }
                                    }
                                }
                            }
                            else -> {
                                // Already set default icon above
                            }
                        }
                    }
                }
            }

            private fun isVideoFile(fileName: String) = fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mkv") || fileName.endsWith(".mov") || fileName.endsWith(".wmv") || fileName.endsWith(".flv") || fileName.endsWith(".webm") || fileName.endsWith(".3gp") || fileName.endsWith(".m4v")
            private fun isImageFile(fileName: String) = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".gif") || fileName.endsWith(".bmp") || fileName.endsWith(".webp") || fileName.endsWith(".heic")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_analysis_details, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val isChecked = selectedPositions.contains(position)
            holder.bind(item, showCheckboxes, isChecked, scope)

            if (!showCheckboxes) {
                holder.itemView.setOnClickListener { clickListener(item) }
                holder.itemView.setOnLongClickListener { 
                    longClickListener(item)
                    true
                }
            } else {
                // Remove old listener first to prevent double firing
                holder.checkbox?.setOnCheckedChangeListener(null)
                
                holder.checkbox?.setOnCheckedChangeListener { _, checked ->
                    val currentPosition = holder.adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        if (checked) {
                            selectedPositions.add(currentPosition)
                        } else {
                            selectedPositions.remove(currentPosition)
                        }
                        updateSelection()
                    }
                }
                holder.itemView.setOnClickListener { 
                    holder.checkbox?.toggle()
                }
            }
        }

        override fun getItemCount() = items.size

        private fun updateSelection() {
            var totalSize = 0L
            val selectedBytes = mutableSetOf<DetailItem>()
            selectedPositions.forEach { pos ->
                if (pos < items.size) {
                    val itm = items[pos]
                    selectedBytes.add(itm)
                    itm.file?.let { totalSize += it.length() }
                }
            }
            selectionListener(selectedPositions.size, totalSize, selectedBytes)
        }

        fun toggleSelectAll() {
            if (selectedPositions.size == items.size) selectedPositions.clear()
            else {
                selectedPositions.clear()
                for (i in items.indices) selectedPositions.add(i)
            }
            notifyDataSetChanged()
            updateSelection()
        }

        fun getSelectedFiles(): List<File> {
            return selectedPositions.mapNotNull { pos -> if (pos.toInt() < items.size) items[pos.toInt()].file else null }
        }

        class DetailItem {
            var name: String? = null
            var path: String? = null
            var size: String? = null
            var subtitle: String? = null
            var packageName: String? = null
            var isFolder = false
            var isSelectable = false
            var isApp = false
            var file: File? = null
            var icon = 0
            var iconColor = 0
        }
    }
}
