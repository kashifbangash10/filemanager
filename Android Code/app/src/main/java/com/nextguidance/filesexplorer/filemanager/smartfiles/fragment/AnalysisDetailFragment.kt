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
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
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
import java.text.DateFormat
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
        private const val ARG_AUTO_INTERNAL = "auto_internal"
        private const val ARG_AUTO_DUPLICATE = "auto_duplicate"
        private const val ARG_AUTO_LARGE = "auto_large_files"
        private const val ARG_AUTO_APPS = "auto_apps"
        private const val ARG_IS_CLEANER = "arg_is_cleaner"

        @JvmStatic
        fun showCleanerInternalStorage(fm: FragmentManager?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, "Storage manager")
            args.putBoolean(ARG_AUTO_INTERNAL, true)
            args.putBoolean(ARG_IS_CLEANER, true)
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        @JvmStatic
        fun showInternalStorage(fm: FragmentManager?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, "Internal Storage")
            args.putBoolean(ARG_AUTO_INTERNAL, true)
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        @JvmStatic
        fun showCleanerDuplicates(fm: FragmentManager?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, "Duplicate files")
            args.putBoolean(ARG_AUTO_DUPLICATE, true)
            args.putBoolean(ARG_IS_CLEANER, true)
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        @JvmStatic
        fun showCleanerLargeFiles(fm: FragmentManager?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, "Large files")
            args.putBoolean(ARG_AUTO_LARGE, true)
            args.putBoolean(ARG_IS_CLEANER, true)
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        @JvmStatic
        fun showCleanerAppManager(fm: FragmentManager?) {
            if (fm == null) return
            val fragment = AnalysisDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, "App manager")
            args.putBoolean(ARG_AUTO_APPS, true)
            args.putBoolean(ARG_IS_CLEANER, true)
            fragment.arguments = args

            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

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

    private var cleanerBottomBar: View? = null
    private var storageInfoText: TextView? = null
    private var btnCleanerAction: Button? = null
    private var isCleanerMode = false

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

            cleanerBottomBar = view.findViewById(R.id.cleaner_bottom_bar)
            storageInfoText = view.findViewById(R.id.storage_info_text)
            btnCleanerAction = view.findViewById(R.id.btn_cleaner_action)
            btnCleanerAction?.setOnClickListener { performDelete() }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bottom bar views not found")
        }

        view.isFocusableInTouchMode = true
        view.requestFocus()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = arguments?.getString(ARG_TITLE)
        isCleanerMode = arguments?.getBoolean(ARG_IS_CLEANER, false) == true
        
        if (isCleanerMode) {
             val path = Environment.getExternalStorageDirectory()
             try {
                val stat = android.os.StatFs(path.path)
                val total = Formatter.formatFileSize(context, stat.blockCountLong * stat.blockSizeLong)
                val available = Formatter.formatFileSize(context, stat.availableBlocksLong * stat.blockSizeLong)
                storageInfoText?.text = "Available: $available   Total: $total"
             } catch (e: Exception) {
                storageInfoText?.text = "Available: --   Total: --"
             }
             cleanerBottomBar?.visibility = View.VISIBLE
             bottomBar?.visibility = View.GONE
        } else {
             cleanerBottomBar?.visibility = View.GONE
             // bottomBar handled by existing logic
        }

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
            mActionMode?.finish()
            return true
        }
        if (isInFolderNavigation && folderStack.isNotEmpty()) {
            folderStack.pop()
            if (folderStack.isNotEmpty()) {
                val previousFolder = folderStack.peek()
                title = previousFolder.name
                updateToolbarTitle()
                openFolderInternally(previousFolder)
                return true
            } else {
                isInFolderNavigation = false
                // If we were at Internal Storage root, go back to Home tab
                if (title == "Internal Storage" || title == "Storage manager" || arguments?.getBoolean(ARG_AUTO_INTERNAL, false) == true) {
                    (activity as? DocumentsActivity)?.selectHomeTab()
                    return true
                }
                title = arguments?.getString(ARG_TITLE) ?: "Analysis"
                updateToolbarTitle()
                loadData()
                return true
            }
        }
        
        // If we are at the root of "Internal Storage" or "Storage manager", go back to Home tab
        if (title == "Internal Storage" || title == "Storage manager" || arguments?.getBoolean(ARG_AUTO_INTERNAL, false) == true) {
            (activity as? DocumentsActivity)?.selectHomeTab()
            return true
        }
        
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadData(resetNavigation: Boolean = true) {
        try {
            val args = arguments ?: return
            
            if (resetNavigation) {
                isInFolderNavigation = false
                folderStack.clear()
            }
            
            if (isInFolderNavigation && folderStack.isNotEmpty()) {
                openFolderInternally(folderStack.peek())
                return
            }

            val items = mutableListOf<DetailAdapter.DetailItem>()
            var isDuplicateView = false

            when {
                args.containsKey(ARG_FOLDERS) -> {
                    val folders = args.getSerializable(ARG_FOLDERS) as? List<AnalysisFragment.FolderItem>
                    folders?.forEach { folder ->
                        val f = File(folder.path)
                        if (f.exists()) {
                            items.add(DetailAdapter.DetailItem().apply {
                                name = folder.name
                                path = folder.path
                                sizeBytes = folder.size
                                size = Formatter.formatFileSize(context, folder.size)
                                subtitle = "${Formatter.formatFileSize(context, folder.size)} | ${folder.itemCount} items"
                                file = f
                                isFolder = true
                                 isSelectable = true
                                 icon = R.drawable.ic_root_folder
                                 iconColor = getFolderIconColor(folder.name ?: "")
                             })
                        }
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
                                    if (f.exists()) {
                                        items.add(DetailAdapter.DetailItem().apply {
                                            name = group.fileName
                                            path = getShortPath(filePath)
                                            sizeBytes = group.size
                                            size = try { Formatter.formatFileSize(context, group.size) } catch(e: Exception) { "0 B" }
                                            subtitle = getFileDate(f)
                                            file = f
                                            isFolder = false
                                            isSelectable = true
                                            icon = getFileIcon(group.fileName)
                                            iconColor = -0x3ef9
                                        })
                                    }
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
                            val f = File(fileInfo.fullPath)
                            if (f.exists()) {
                                items.add(DetailAdapter.DetailItem().apply {
                                    name = fileInfo.name
                                    sizeBytes = fileInfo.size
                                    val fileName = fileInfo.name.lowercase()
                                    if (fileName.endsWith(".apk")) {
                                        path = null
                                        subtitle = getFileDate(f)
                                    } else {
                                        path = getShortPath(fileInfo.fullPath)
                                        subtitle = null
                                    }
                                    size = try { Formatter.formatFileSize(context, fileInfo.size) } catch(e: Exception) { "0 B" }
                                    file = f
                                    isFolder = false
                                    isSelectable = true
                                    icon = getFileIcon(fileInfo.name)
                                    iconColor = -0x6800
                                })
                            }
                        } catch (e: Exception) {}
                    }
                }
                args.containsKey(ARG_APPS) -> {
                    val apps = args.getSerializable(ARG_APPS) as? List<AnalysisFragment.AppItem>
                    apps?.forEach { app ->
                        items.add(DetailAdapter.DetailItem().apply {
                            name = app.name
                            path = app.packageName
                            sizeBytes = app.size
                            subtitle = getInstallDate()
                            size = try { Formatter.formatFileSize(context, app.size) } catch(e: Exception) { "0 B" }
                            packageName = app.packageName
                            isApp = true
                            isSelectable = true
                            icon = R.drawable.ic_root_apps
                            iconColor = 0
                        })
                    }
                }
                args.getBoolean(ARG_AUTO_INTERNAL, false) -> {
                    val cache = AnalysisFragment.getCache()
                    if (cache != null && cache.isValid() && cache.storageFolders.isNotEmpty()) {
                        cache.storageFolders.forEach { folder ->
                            val f = File(folder.path)
                            if (f.exists()) {
                                items.add(DetailAdapter.DetailItem().apply {
                                    name = folder.name
                                    path = folder.path
                                    sizeBytes = folder.size
                                    size = try { Formatter.formatFileSize(context, folder.size) } catch(e: Exception) { "0 B" }
                                    subtitle = "${try { Formatter.formatFileSize(context, folder.size) } catch(e: Exception) { "" }} | ${folder.itemCount} items"
                                    file = f
                                    isFolder = true
                                    isSelectable = true
                                     icon = R.drawable.ic_root_folder
                                     iconColor = getFolderIconColor(folder.name ?: "")
                                })
                            }
                        }
                        bottomBar?.visibility = View.GONE
                    } else {
                        // Quick load to avoid blank screen (Image 1 behavior)
                        val root = Environment.getExternalStorageDirectory()
                        openFolderInternally(root)
                        
                        // Background refresh to proper view (Image 2 behavior)
                        val contextSafe = context
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (contextSafe != null) AnalysisFragment.startGlobalAnalysis(contextSafe)
                            
                            var attempts = 0
                            while (attempts < 40) { // Wait up to 20 seconds
                                delay(500)
                                val c = AnalysisFragment.getCache()
                                if (c != null && c.isValid()) {
                                    // Only refresh if user is still at the root of internal storage
                                    val rootPath = Environment.getExternalStorageDirectory().absolutePath
                                    if (isAdded && isInFolderNavigation && folderStack.size == 1 && 
                                        folderStack.peek().absolutePath == rootPath) {
                                        loadData(resetNavigation = true)
                                    }
                                    break
                                }
                                attempts++
                            }
                        }
                        return
                    }
                }
            args.getBoolean(ARG_AUTO_DUPLICATE, false) -> {
                isDuplicateView = true
                val cache = AnalysisFragment.getCache()
                if (cache != null && cache.isValid() && cache.duplicateGroups.isNotEmpty()) {
                    var totalDuplicates = 0
                    var totalSize = 0L
                    cache.duplicateGroups.forEach { group ->
                        totalDuplicates += group.filePaths.size
                        totalSize += group.size * group.filePaths.size
                        group.filePaths.forEach { filePath ->
                            try {
                                val f = File(filePath)
                                if (f.exists()) {
                                    items.add(DetailAdapter.DetailItem().apply {
                                        name = group.fileName
                                        path = getShortPath(filePath)
                                        sizeBytes = group.size
                                        size = try { Formatter.formatFileSize(context, group.size) } catch(e: Exception) { "0 B" }
                                        subtitle = getFileDate(f)
                                        file = f
                                        isFolder = false
                                        isSelectable = true
                                        icon = getFileIcon(group.fileName)
                                        iconColor = -0x3ef9
                                    })
                                }
                            } catch (e: Exception) {}
                        }
                    }
                    bottomBar?.visibility = View.VISIBLE
                    summaryText?.text = "Duplicate files: $totalDuplicates  Size: ${try { Formatter.formatFileSize(context, totalSize) } catch(e: Exception) { "" }}"
                } else {
                    startAnalysisScan()
                }
            }
            args.getBoolean(ARG_AUTO_LARGE, false) -> {
                val cache = AnalysisFragment.getCache()
                if (cache != null && cache.isValid() && cache.largeFiles.isNotEmpty()) {
                    cache.largeFiles.forEach { fileInfo ->
                        try {
                            val f = File(fileInfo.fullPath)
                            if (f.exists()) {
                                items.add(DetailAdapter.DetailItem().apply {
                                    name = fileInfo.name
                                    sizeBytes = fileInfo.size
                                    val fileName = fileInfo.name.lowercase()
                                    if (fileName.endsWith(".apk")) {
                                        path = null
                                        subtitle = getFileDate(f)
                                    } else {
                                        path = getShortPath(fileInfo.fullPath)
                                        subtitle = null
                                    }
                                    size = try { Formatter.formatFileSize(context, fileInfo.size) } catch(e: Exception) { "0 B" }
                                    file = f
                                    isFolder = false
                                    isSelectable = true
                                    icon = getFileIcon(fileInfo.name)
                                    iconColor = -0x6800
                                })
                            }
                        } catch (e: Exception) {}
                    }
                } else {
                    startAnalysisScan()
                }
            }
            args.getBoolean(ARG_AUTO_APPS, false) -> {
                val cache = AnalysisFragment.getCache()
                if (cache != null && cache.isValid() && cache.apps.isNotEmpty()) {
                    cache.apps.forEach { app ->
                        items.add(DetailAdapter.DetailItem().apply {
                            name = app.name
                            path = app.packageName
                            sizeBytes = app.size
                            subtitle = getInstallDate()
                            size = try { Formatter.formatFileSize(context, app.size) } catch(e: Exception) { "0 B" }
                            packageName = app.packageName
                            isApp = true
                            isSelectable = true
                            icon = R.drawable.ic_root_apps
                            iconColor = 0
                        })
                    }
                } else {
                    startAnalysisScan()
                }
            }
        }

        val currentAdapter = adapter
        if (currentAdapter != null) {
            currentAdapter.updateItems(items)
            currentAdapter.setSelectionMode(isSelectionMode || isCleanerMode)
            if (isSelectionMode) {
                currentAdapter.setSelectedItems(selectedItems)
            }
        } else {
            adapter = DetailAdapter(items, isSelectionMode || isCleanerMode, lifecycleScope,
                this::handleItemClick, this::handleSelectionChanged, this::handleItemLongClick, isCleanerMode)
            recyclerView.adapter = adapter
        }
        
        if (isCleanerMode && items.isNotEmpty()) {
             handleSelectionChanged(0, 0, emptySet())
        }
    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error loading data", e)
    }
}

private fun startAnalysisScan() {
    lifecycleScope.launch(Dispatchers.Main) {
        val context = context ?: return@launch
        
        withContext(Dispatchers.IO) {
            // Check if global scan is running, if so, wait for it
            AnalysisFragment.startGlobalAnalysis(context)
            
            var attempts = 0
            while (attempts < 20) { // Wait up to 10 seconds (20 * 500ms)
                val cache = AnalysisFragment.getCache()
                if (cache != null && cache.isValid()) break
                delay(500)
                attempts++
            }
        }
        
        if (isAdded) {
            loadData(resetNavigation = false)
        }
    }
}
    private fun handleItemClick(item: DetailAdapter.DetailItem) {
        if (isCleanerMode) {
            adapter?.toggleSelection(item)
            return
        }
        item.packageName?.let { openAppInfo(it) } ?: run {
            if (item.isFolder) {
                openFolderInternally(item.file)
            } else if (item.file != null && !item.isSelectable) {
                openFileDirect(item.file!!)
            }
        }
    }

    private fun handleItemLongClick(item: DetailAdapter.DetailItem) {
        if (isCleanerMode) {
            adapter?.toggleSelection(item)
        } else if (!isSelectionMode) {
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
        
        // Update adapter mode without full reset if possible
        adapter?.setSelectionMode(true)
        if (adapter == null) {
            loadData(resetNavigation = false)
        }
        
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
            setOnClickListener { performRename() }
        }
        selectionBar.findViewById<View>(R.id.action_delete)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { performDelete() }
        }
        selectionBar.findViewById<View>(R.id.action_more)?.apply {
            visibility = View.VISIBLE
            setOnClickListener { showMoreMenu(it) }
        }
    }

    private fun performOperation(op: String, singleItem: Boolean) {
        val selected = adapter?.getSelectedItemsList() ?: return
        if (selected.isEmpty()) return
        
        when(op) {
            "Copy", "Move" -> {
                val files = selected.mapNotNull { it.file }
                if (files.isNotEmpty()) {
                    (activity as? DocumentsActivity)?.setPasteMode(true, files, null, op == "Move")
                    exitSelectionMode()
                    Toast.makeText(context, "${files.size} items ready to $op", Toast.LENGTH_SHORT).show()
                }
            }
            "Share" -> {
                val files = selected.mapNotNull { it.file }
                if (files.isNotEmpty()) {
                    shareFiles(files)
                }
            }
            else -> {
                Toast.makeText(context, "$op ${selected.size} items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onPasteRequested(files: List<File>, isMove: Boolean) {
        val targetFolder = if (isInFolderNavigation) folderStack.peek() else null
        
        if (targetFolder == null || !targetFolder.exists() || !targetFolder.isDirectory) {
             Toast.makeText(context, "Please navigate to a folder to paste", Toast.LENGTH_SHORT).show()
             return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            files.forEach { file ->
                try {
                    val dest = File(targetFolder, file.name)
                    if (isMove) {
                        if (file.renameTo(dest)) successCount++
                    } else {
                        file.copyRecursively(dest, overwrite = true)
                        successCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Paste failed for ${file.name}", e)
                }
            }

            withContext(Dispatchers.Main) {
                (activity as? DocumentsActivity)?.setPasteMode(false, null, null, false)
                loadData(resetNavigation = false)
                Toast.makeText(context, "Pasted $successCount items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFiles(files: List<File>) {
        try {
            val uris = ArrayList<Uri>()
            files.forEach { file ->
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                uris.add(uri)
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share files"))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Share failed", e)
            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectionTitle(count: Int) {
        val totalItems = adapter?.itemCount ?: 0
        mActionMode?.title = "$count / $totalItems"
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
    
    private fun performRename() {
        val selected = adapter?.getSelectedItemsList() ?: return
        if (selected.size != 1) {
            Toast.makeText(context, "Select only one item to rename", Toast.LENGTH_SHORT).show()
            return
        }
        val item = selected[0]
        val file = item.file ?: return

        val input = EditText(context)
        input.setText(item.name)
        input.setSelection(input.text.length)

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp

        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        container.setPadding(padding, padding / 2, padding, 0)
        container.addView(input)

        android.app.AlertDialog.Builder(context)
            .setTitle("Rename")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty() && newName != item.name) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val newFile = File(file.parent, newName)
                            if (file.renameTo(newFile)) {
                                withContext(Dispatchers.Main) {
                                    item.name = newName
                                    item.file = newFile
                                    item.path = getShortPath(newFile.absolutePath)
                                    exitSelectionMode()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete() {
        val selected = adapter?.getSelectedItemsList() ?: return
        if (selected.isEmpty()) return

        if (isCleanerMode) {
             android.app.AlertDialog.Builder(context)
                .setTitle("Clean up")
                .setMessage("Clean up files from device?")
                .setPositiveButton("Clean up") { _, _ ->
                     val paths = ArrayList<String>()
                     var totalSelectedSize = 0L
                     selected.forEach { 
                         it.file?.absolutePath?.let { p -> paths.add(p) }
                         totalSelectedSize += it.sizeBytes
                     }
                     
                     val sizeDisplay = Formatter.formatFileSize(context, totalSelectedSize)
                     
                     // Remove from adapter immediately to update UI underneath
                     val itemsToDelete = selected.toSet()
                     adapter?.removeItems(itemsToDelete)
                     exitSelectionMode()

                     CleanerResultFragment.show(fragmentManager, paths, sizeDisplay, title)
                }
                .setNegativeButton("Cancel", null)
                .show()
             return
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Delete ${selected.size} items?")
            .setPositiveButton("Delete") { _, _ ->
                 val itemsToDelete = selected.toSet()
                 adapter?.removeItems(itemsToDelete)
                 lifecycleScope.launch(Dispatchers.IO) {
                     itemsToDelete.forEach { it.file?.deleteRecursively() }
                     withContext(Dispatchers.Main) {
                         exitSelectionMode()
                     }
                 }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exitSelectionMode() {
        if (!isSelectionMode) return
        isSelectionMode = false
        selectedItems.clear()
        mActionMode?.finish()
        mActionMode = null
        (activity as? DocumentsActivity)?.setActionMode(false)
        adapter?.setSelectionMode(false)
        // Refresh data to show changes
        loadData(resetNavigation = false)
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
        } else {
            if (isCleanerMode) {
                 btnCleanerAction?.let { btn ->
                      if (selectedCount > 0) {
                           btn.text = "Clean up ${Formatter.formatFileSize(context, totalSize)}"
                           btn.isEnabled = true
                            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
                            btn.setTextColor(0xFFFFFFFF.toInt())
                      } else {
                           btn.text = "Clean up 0 B"
                           btn.isEnabled = false
                           btn.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
                           btn.setTextColor(0xFF757575.toInt())
                      }
                 }
            } else {
                summaryText?.let { summary ->
                    cleanupButton?.let { cleanup ->
                        if (selectedCount > 0) {
                            summary.text = "$selectedCount selected"
                            cleanup.text = "Clean up ${Formatter.formatFileSize(context, totalSize)}"
                            cleanup.isEnabled = true
                            cleanup.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
                            cleanup.setTextColor(0xFFFFFFFF.toInt())
                        } else {
                            cleanup.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
                            cleanup.setTextColor(0xFF757575.toInt())
                            cleanup.isEnabled = false
                        }
                    }
                }
            }
        }
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
                title = folder.name
                updateToolbarTitle()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val cache = AnalysisFragment.getCache()
                    val currentPath = folder.absolutePath
                    val items = withContext(Dispatchers.IO) {
                        val result = mutableListOf<DetailAdapter.DetailItem>()
                        
                        // Direct file system access - FAST!
                        folder.listFiles()?.filter { !it.name.startsWith(".") }?.forEach { file ->
                            try {
                                val absPath = file.absolutePath
                                result.add(DetailAdapter.DetailItem().apply {
                                    name = file.name
                                    path = getShortPath(absPath)
                                    this.file = file
                                    if (file.isDirectory) {
                                        isFolder = true
                                        icon = R.drawable.ic_root_folder
                                        iconColor = getFolderIconColor(name ?: "")
                                        // Try to get cached size, otherwise quick estimate
                                        val cachedFolder = cache?.allFoldersMap?.get(absPath)
                                        if (cachedFolder != null) {
                                            size = Formatter.formatFileSize(context, cachedFolder.size)
                                            subtitle = "${cachedFolder.itemCount} items"
                                        } else {
                                            // Quick count without deep scan
                                            val fileCount = file.listFiles()?.size ?: 0
                                            subtitle = "$fileCount items"
                                            size = "" // Don't calculate size for speed
                                        }
                                    } else {
                                        isFolder = false
                                        icon = getFileIcon(name ?: "")
                                        iconColor = -0xde690d
                                        size = Formatter.formatFileSize(context, file.length())
                                        subtitle = null
                                    }
                                })
                            } catch (e: Exception) {}
                        }
                        
                        // Sort: folders first, then alphabetically
                        result.sortWith(compareBy({ !it.isFolder }, { it.name?.lowercase() }))
                        result
                    }

                    if (items.isEmpty()) {
                        Toast.makeText(context, "Empty folder", Toast.LENGTH_SHORT).show()
                    } else {
                        val rootPath = Environment.getExternalStorageDirectory().absolutePath
                        title = if (folder.absolutePath == rootPath) "Internal Storage" else folder.name
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
            name == "dcim" || name == "dci" || name == "pictures" || name == "camera" -> -0xde690d // 0xFF2196F3
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
        private var items: List<DetailItem>,
        private var showCheckboxes: Boolean,
        private val scope: CoroutineScope,
        private val clickListener: (DetailItem) -> Unit,
        private val selectionListener: (Int, Long, Set<DetailItem>) -> Unit,
        private val longClickListener: (DetailItem) -> Unit,
        private val isCleanerMode: Boolean = false
    ) : RecyclerView.Adapter<DetailAdapter.ViewHolder>() {
        private var itemsList = items.toMutableList()
        private val selectedPositions = mutableSetOf<Int>()
        private val totalStorage: Long = File(Environment.getExternalStorageDirectory().absolutePath).totalSpace.coerceAtLeast(1L)
        
        fun updateItems(newItems: List<DetailItem>) {
            this.itemsList = newItems.toMutableList()
            this.items = newItems
            notifyDataSetChanged()
        }

        fun removeItems(itemsToRemove: Set<DetailItem>) {
            itemsList.removeAll(itemsToRemove)
            items = itemsList.toList()
            selectedPositions.clear()
            notifyDataSetChanged()
        }
        
        fun setSelectionMode(show: Boolean) {
            showCheckboxes = show
            if (!show) selectedPositions.clear()
            notifyDataSetChanged()
        }
        
        fun toggleSelection(item: DetailItem) {
             val index = itemsList.indexOf(item)
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
                 val idx = itemsList.indexOf(it)
                 if (idx != -1) selectedPositions.add(idx)
             }
             notifyDataSetChanged()
             updateSelection()
        }
        
        fun getSelectedItemsList(): List<DetailItem> {
             return itemsList.filterIndexed { index, _ -> selectedPositions.contains(index) }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.icon)
            val name: TextView = view.findViewById(R.id.name)
            val path: TextView = view.findViewById(R.id.path)
            val size: TextView = view.findViewById(R.id.size)
            val subtitle: TextView? = try { view.findViewById(R.id.subtitle) } catch (e: Exception) { null }
            val checkbox: CompoundButton? = try { view.findViewById(R.id.checkbox) } catch (e: Exception) { null } // Changed to CompoundButton for CheckBox/RadioButton
            val percentage: TextView? = try { view.findViewById(R.id.percentage) } catch (e: Exception) { null }
            val usageBar: View? = try { view.findViewById(R.id.usage_bar) } catch (e: Exception) { null }

            fun bind(item: DetailItem, showCheckbox: Boolean, isChecked: Boolean, scope: CoroutineScope) {
                name.text = item.name ?: ""
                path.text = ""
                path.visibility = View.GONE
                size.text = item.size ?: ""
                size.visibility = if (item.size != null) View.VISIBLE else View.GONE
                subtitle?.text = item.subtitle ?: ""
                subtitle?.visibility = if (item.subtitle != null) View.VISIBLE else View.GONE
                
                if (item.sizeBytes > 0 && this@DetailAdapter.isCleanerMode) {
                     val percent = (item.sizeBytes.toDouble() / this@DetailAdapter.totalStorage.toDouble()) * 100
                     percentage?.text = String.format("%.2f%%", percent)
                     if (usageBar != null) {
                          val params = usageBar.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                          if (params != null) {
                              params.matchConstraintPercentWidth = (percent / 100f).toFloat().coerceIn(0f, 1f)
                              usageBar.layoutParams = params
                              usageBar.visibility = View.VISIBLE
                          }
                     }
                     percentage?.visibility = View.VISIBLE
                } else {
                     percentage?.visibility = View.GONE
                     usageBar?.visibility = View.INVISIBLE
                }

                checkbox?.visibility = if (showCheckbox) View.VISIBLE else View.GONE
                if (showCheckbox) {
                    checkbox?.isChecked = isChecked
                    checkbox?.buttonTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
                }

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
            val item = itemsList[position]
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
                    holder.checkbox?.let { cb -> cb.isChecked = !cb.isChecked }
                }
            }
        }

        override fun getItemCount() = itemsList.size

        private fun updateSelection() {
            var totalSize = 0L
            val selectedBytes = mutableSetOf<DetailItem>()
            selectedPositions.forEach { pos ->
                if (pos < itemsList.size) {
                    val itm = itemsList[pos]
                    selectedBytes.add(itm)
                    totalSize += itm.sizeBytes
                }
            }
            selectionListener(selectedPositions.size, totalSize, selectedBytes)
        }

        fun toggleSelectAll() {
            if (selectedPositions.size == itemsList.size) selectedPositions.clear()
            else {
                selectedPositions.clear()
                for (i in itemsList.indices) selectedPositions.add(i)
            }
            notifyDataSetChanged()
            updateSelection()
        }

        fun getSelectedFiles(): List<File> {
            return selectedPositions.mapNotNull { pos -> if (pos.toInt() < itemsList.size) itemsList[pos.toInt()].file else null }
        }

        class DetailItem {
            var name: String? = null
            var path: String? = null
            var size: String? = null
            var sizeBytes: Long = 0
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
