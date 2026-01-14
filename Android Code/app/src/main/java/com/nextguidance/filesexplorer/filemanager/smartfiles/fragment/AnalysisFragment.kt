package com.nextguidance.filesexplorer.filemanager.smartfiles.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.nextguidance.filesexplorer.filemanager.smartfiles.BaseActivity
import com.nextguidance.filesexplorer.filemanager.smartfiles.DocumentsActivity
import com.nextguidance.filesexplorer.filemanager.smartfiles.R
import com.nextguidance.filesexplorer.filemanager.smartfiles.misc.Utils
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import java.util.ArrayList

class AnalysisFragment : Fragment() {



    companion object {
        const val TAG = "AnalysisFragment"
        private const val LARGE_FILE_THRESHOLD = 50 * 1024 * 1024L // 50MB
        
        private var cachedData: AnalysisCache? = null
        private var isAnalysisRunning = false
        private var isAnalysisComplete = false

        @JvmStatic
        fun getCache(): AnalysisCache? = cachedData

        @JvmStatic
        fun setCache(cache: AnalysisCache?) {
            cachedData = cache
        }

        @JvmStatic
        fun show(fm: FragmentManager) {
            val fragment = AnalysisFragment()
            fm.beginTransaction()
                .replace(R.id.container_directory, fragment, TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }

        @JvmStatic
        fun startGlobalAnalysis(context: Context) {
            if (isAnalysisRunning || (cachedData != null && cachedData!!.isValid())) return
            
            GlobalScope.launch(Dispatchers.IO) {
                isAnalysisRunning = true
                try {
                    val cache = cachedData ?: AnalysisCache()
                    cachedData = cache
                    
                    // Simple logic to perform background scan
                    val rootDir = Environment.getExternalStorageDirectory()
                    if (rootDir.exists()) {
                        performFullScan(context, rootDir.absolutePath, cache)
                        isAnalysisComplete = true
                        cache.markAnalysisComplete()
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Global Analysis Error", e)
                } finally {
                    isAnalysisRunning = false
                }
            }
        }

        private suspend fun performFullScan(ctx: Context, rootDir: String, cache: AnalysisCache) = withContext(Dispatchers.IO) {
            cache.clear()
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA
            )
            
            val folderSizeMap = mutableMapOf<String, Long>()
            val folderCountMap = mutableMapOf<String, Int>()
            val sizeMap = mutableMapOf<Long, MutableList<String>>()

            try {
                ctx.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection, null, null, null
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    
                    val videoExts = setOf("mp4", "avi", "mkv", "mov", "webm", "3gp")
                    val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp")
                    val audioExts = setOf("mp3", "wav", "m4a", "flac")
                    val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "txt", "ppt", "pptx")

                    val stat = StatFs(rootDir)
                    cache.totalStorageBytes = stat.totalBytes

                    while (cursor.moveToNext()) {
                        val size = cursor.getLong(sizeIdx)
                        val path = cursor.getString(dataIdx) ?: continue
                        val name = cursor.getString(nameIdx) ?: "Unknown"
                        val ext = name.substringAfterLast('.', "").lowercase()

                        if (videoExts.contains(ext)) cache.totalVideoSize += size
                        else if (imageExts.contains(ext)) cache.totalImageSize += size
                        else if (audioExts.contains(ext)) cache.totalAudioSize += size
                        else if (docExts.contains(ext)) cache.totalDocSize += size

                        if (path.startsWith(rootDir)) {
                            val relativePath = path.substring(rootDir.length).trimStart('/')
                            val parts = relativePath.split('/')
                            if (parts.size > 1) {
                                val topDir = parts[0]
                                folderSizeMap[topDir] = (folderSizeMap[topDir] ?: 0L) + size
                                folderCountMap[topDir] = (folderCountMap[topDir] ?: 0) + 1
                            }

                            var currentPath = rootDir
                            for (i in 0 until parts.size - 1) {
                                val folderName = parts[i]
                                currentPath += if (currentPath.endsWith("/")) folderName else "/$folderName"
                                val folder = cache.allFoldersMap.getOrPut(currentPath) {
                                    FolderItem().apply { this.path = currentPath; this.name = folderName }
                                }
                                folder.size += size
                                folder.itemCount++
                            }
                        }

                        cache.allFiles.add(FileItem().apply {
                            this.name = name
                            this.path = File(path).parent ?: ""
                            this.size = size
                            this.fullPath = path
                        })

                        if (size > 50 * 1024) {
                            sizeMap.getOrPut(size) { mutableListOf() }.add(path)
                        }

                        if (size > 50 * 1024 * 1024L) {
                            cache.largeFiles.add(FileItem().apply {
                                this.name = name
                                this.path = File(path).parent ?: ""
                                this.size = size
                                this.fullPath = path
                            })
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Scan Error", e)
            }

            folderSizeMap.forEach { (name, size) ->
                cache.storageFolders.add(FolderItem().apply {
                    this.name = name
                    this.path = File(rootDir, name).absolutePath
                    this.size = size
                    this.itemCount = folderCountMap[name] ?: 0
                })
            }
            cache.storageFolders.sortByDescending { it.size }

            sizeMap.forEach { (size, paths) ->
                if (paths.size > 1) {
                    cache.duplicateGroups.add(DuplicateGroup().apply {
                        fileName = File(paths[0]).name
                        this.size = size
                        count = paths.size
                        filePaths.addAll(paths)
                    })
                }
            }
            cache.duplicateGroups.sortByDescending { it.size * (it.count - 1) }

            // Apps scan (simplified for background)
            try {
                val pm = ctx.packageManager
                val pkgs = pm.getInstalledPackages(0)
                pkgs.forEach { pkg ->
                    pkg.applicationInfo?.let { appInfo ->
                        val appItem = AppItem().apply {
                            name = pm.getApplicationLabel(appInfo).toString()
                            packageName = pkg.packageName
                            val apk = File(appInfo.sourceDir)
                            size = if (apk.exists()) apk.length() else 0
                        }
                        cache.apps.add(appItem)
                    }
                }
                cache.apps.sortByDescending { it.size }
            } catch (e: Exception) {}
        }
    }

    private lateinit var containerLayout: LinearLayout
    private lateinit var loadingView: View
    private var loadingStatus: TextView? = null
    private var isDataLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)
        containerLayout = view.findViewById(R.id.container_analysis)
        loadingView = view.findViewById(R.id.loading)
        loadingStatus = view.findViewById(R.id.loading_status)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateToolbarTitle()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                fragmentManager?.popBackStack()
            }
        })
        
        if (cachedData != null && cachedData!!.isValid() && isAnalysisComplete) {
            loadFromCache()
        } else {
            startAnalysis()
        }
    }

    private fun updateToolbarTitle() {
        val titleText = getString(R.string.root_analysis)
        (activity as? DocumentsActivity)?.setAnalysisMode(true, titleText)
    }

    override fun onResume() {
        super.onResume()
        updateToolbarTitle()
    }

    private fun startAnalysis() {
        if (!Utils.isActivityAlive(activity)) return
        
        isAnalysisRunning = true
        isAnalysisComplete = false
        isDataLoaded = false
        
        if (cachedData == null) {
            cachedData = AnalysisCache()
        }
        cachedData?.clear()
        
        containerLayout.removeAllViews()
        
        // Show placeholders
        addItemToView(AnalysisItem(AnalysisItem.TYPE_STORAGE, R.drawable.ic_root_internal, "Internal storage", "Calculating...", 0, true))
        addItemToView(AnalysisItem(AnalysisItem.TYPE_DUPLICATE, R.drawable.ic_root_document, "Duplicate files", "Scanning...", 0, true))
        addItemToView(AnalysisItem(AnalysisItem.TYPE_LARGE_FILES, R.drawable.ic_root_folder, "Large files", "Scanning...", 0, true))
        addItemToView(AnalysisItem(AnalysisItem.TYPE_APP_MANAGER, R.drawable.ic_root_apps, "App manager", "Loading...", 0, true))

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val results = analyzeAllData()
                if (results != null && isAdded) {
                    addItemToView(results.storage)
                    addItemToView(results.duplicates)
                    addItemToView(results.largeFiles)
                    addItemToView(results.apps)
                    
                    isAnalysisComplete = true
                    cachedData?.markAnalysisComplete()
                }
            } catch (e: Exception) {
                if (isAdded) android.util.Log.e(TAG, "Analysis Error", e)
            } finally {
                if (isAdded) {
                    isAnalysisRunning = false
                    isDataLoaded = true
                    loadingView.visibility = View.GONE
                }
            }
        }
    }

    private fun loadFromCache() {
        val results = buildResultsFromCache() ?: return
        loadingView.visibility = View.GONE
        containerLayout.removeAllViews()
        
        addItemToView(results.storage)
        addItemToView(results.duplicates)
        addItemToView(results.largeFiles)
        addItemToView(results.apps)
        
        isDataLoaded = true
    }

    private fun buildResultsFromCache(): CombinedResults? {
        val ctx = context ?: return null
        val cache = cachedData ?: return null
        
        // Storage
        val rootPath = Environment.getExternalStorageDirectory().path
        val stat = StatFs(rootPath)
        val used = stat.totalBytes - stat.availableBytes
        val storageItem = AnalysisItem(AnalysisItem.TYPE_STORAGE, R.drawable.ic_root_internal, "Internal storage", 
            "${Formatter.formatFileSize(ctx, used)} / ${Formatter.formatFileSize(ctx, stat.totalBytes)}", 
            if (stat.totalBytes > 0) ((used * 100) / stat.totalBytes).toInt() else 0, false)
        
        cache.storageFolders.take(3).forEach { f ->
            storageItem.subItems.add(SubItem(f.name, "${f.itemCount} items", Formatter.formatFileSize(ctx, f.size), R.drawable.ic_root_folder, f.path))
        }
        storageItem.moreCount = if (cache.storageFolders.size > 3) cache.storageFolders.size - 3 else 0

        // Duplicates
        val totalWasted = cache.duplicateGroups.sumOf { it.size * (it.count - 1) }
        val duplicatesItem = AnalysisItem(AnalysisItem.TYPE_DUPLICATE, R.drawable.ic_root_document, "Duplicate files", 
            "${cache.duplicateGroups.sumOf { it.count - 1 }} duplicates • ${Formatter.formatFileSize(ctx, totalWasted)} wasted", 0, false)
        cache.duplicateGroups.take(3).forEach { g ->
            duplicatesItem.subItems.add(SubItem(g.fileName, "${g.count} copies found", Formatter.formatFileSize(ctx, g.size * (g.count - 1)) + " wasted", getFileIcon(g.fileName), g.filePaths[0]))
        }
        duplicatesItem.moreCount = if (cache.duplicateGroups.size > 3) cache.duplicateGroups.size - 3 else 0

        // Large Files
        val largeFilesSize = cache.largeFiles.sumOf { it.size }
        val largeFilesItem = AnalysisItem(AnalysisItem.TYPE_LARGE_FILES, R.drawable.ic_root_folder, "Large files", 
            "${cache.largeFiles.size} files • ${Formatter.formatFileSize(ctx, largeFilesSize)}", 0, false)
        cache.largeFiles.take(3).forEach { f ->
            largeFilesItem.subItems.add(SubItem(f.name, f.path, Formatter.formatFileSize(ctx, f.size), getFileIcon(f.name), f.fullPath))
        }
        largeFilesItem.moreCount = if (cache.largeFiles.size > 3) cache.largeFiles.size - 3 else 0

        // Apps
        val totalAppSize = cache.apps.sumOf { it.size }
        storageItem.appsSize = totalAppSize
        storageItem.videosSize = cache.totalVideoSize
        storageItem.imagesSize = cache.totalImageSize
        storageItem.audioSize = cache.totalAudioSize
        storageItem.docsSize = cache.totalDocSize
        storageItem.totalSizeBytes = cache.totalStorageBytes

        val appsItemReal = AnalysisItem(AnalysisItem.TYPE_APP_MANAGER, R.drawable.ic_root_apps, "App manager", 
            "${cache.apps.size} apps • ${Formatter.formatFileSize(ctx, totalAppSize)}", 0, false)
        cache.apps.take(3).forEach { app ->
            appsItemReal.subItems.add(SubItem(app.name, app.packageName, Formatter.formatFileSize(ctx, app.size), R.drawable.ic_root_apps, app.packageName, isApp = true))
        }
        appsItemReal.moreCount = if (cache.apps.size > 3) cache.apps.size - 3 else 0

        return CombinedResults(storageItem, duplicatesItem, largeFilesItem, appsItemReal)
    }

    override fun onDestroy() {
        (activity as? DocumentsActivity)?.setAnalysisMode(false)
        super.onDestroy()
    }

    // --- Analysis Logic ---

    // --- Analysis Logic ---

    data class CombinedResults(
        val storage: AnalysisItem,
        val duplicates: AnalysisItem,
        val largeFiles: AnalysisItem,
        val apps: AnalysisItem
    )

    private suspend fun analyzeAllData(): CombinedResults? = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext null
        
        try {
            // 1. Storage Info
            val rootPath = Environment.getExternalStorageDirectory().path
            val stat = try {
                StatFs(rootPath)
            } catch (e: Exception) {
                null
            }
            
            val total = stat?.totalBytes ?: 0L
            val avail = stat?.availableBytes ?: 0L
            val used = total - avail
            val percentage = if (total > 0) ((used * 100) / total).toInt() else 0
            
            val storageItem = AnalysisItem(
                AnalysisItem.TYPE_STORAGE,
                R.drawable.ic_root_internal,
                "Internal storage",
                "${Formatter.formatFileSize(ctx, used)} / ${Formatter.formatFileSize(ctx, total)}",
                percentage,
                false
            )

            val duplicatesItem = AnalysisItem(AnalysisItem.TYPE_DUPLICATE, R.drawable.ic_root_document, "Duplicate files", "", 0, false)
            val largeFilesItem = AnalysisItem(AnalysisItem.TYPE_LARGE_FILES, R.drawable.ic_root_folder, "Large files", "", 0, false)
            val appsItem = AnalysisItem(AnalysisItem.TYPE_APP_MANAGER, R.drawable.ic_root_apps, "App manager", "", 0, false)

            var totalImageSize = 0L
            var totalVideoSize = 0L
            var totalAudioSize = 0L
            var totalDocSize = 0L

            val folderSizeMap = mutableMapOf<String, Long>()
            val folderCountMap = mutableMapOf<String, Int>()
            val sizeMap = mutableMapOf<Long, MutableList<String>>()
            
            val rootDir = Environment.getExternalStorageDirectory().absolutePath
            var largeFilesSize = 0L
            var largeCount = 0
            
            cachedData?.largeFiles?.clear()
            cachedData?.duplicateGroups?.clear()

            // Single pass over MediaStore
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA
            )
            
            try {
                ctx.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection, null, null, null
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                    val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    
                    val videoExts = setOf("mp4", "avi", "mkv", "mov", "webm", "3gp")
                    val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp")
                    val audioExts = setOf("mp3", "wav", "m4a", "flac")
                    val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "txt", "ppt", "pptx")

                    var count = 0
                    while (cursor.moveToNext()) {
                        if (++count % 100 == 0) yield() // Allow cancellation

                        val size = cursor.getLong(sizeIdx)
                        val path = cursor.getString(dataIdx) ?: continue
                        val name = cursor.getString(nameIdx) ?: "Unknown"
                        val ext = name.substringAfterLast('.', "").lowercase()

                        // Breakdown
                        when {
                            videoExts.contains(ext) -> totalVideoSize += size
                            imageExts.contains(ext) -> totalImageSize += size
                            audioExts.contains(ext) -> totalAudioSize += size
                            docExts.contains(ext) -> totalDocSize += size
                        }

                        // Storage calculation
                        if (path.startsWith(rootDir)) {
                            val relativePath = path.substring(rootDir.length).trimStart('/')
                            val parts = relativePath.split('/')
                            var currentPath = rootDir
                            for (i in 0 until parts.size - 1) {
                                val folderName = parts[i]
                                currentPath += if (currentPath.endsWith("/")) folderName else "/$folderName"
                                
                                val folder = cachedData?.allFoldersMap?.getOrPut(currentPath) {
                                    FolderItem().apply {
                                        this.path = currentPath
                                        this.name = folderName
                                    }
                                }
                                folder?.size = (folder?.size ?: 0L) + size
                                folder?.itemCount = (folder?.itemCount ?: 0) + 1
                                
                                // topDir aggregation for the main list
                                if (i == 0) {
                                    folderSizeMap[folderName] = (folderSizeMap[folderName] ?: 0L) + size
                                    folderCountMap[folderName] = (folderCountMap[folderName] ?: 0) + 1
                                }
                            }
                        }

                        // Store all files for detail view
                        cachedData?.allFiles?.add(FileItem().apply {
                            this.name = name
                            this.path = try { File(path).parent ?: "" } catch(e: Exception) { "" }
                            this.size = size
                            this.fullPath = path
                        })

                        // Duplicates (size-based candidate)
                        if (size > 50 * 1024) {
                            sizeMap.getOrPut(size) { mutableListOf() }.add(path)
                        }

                        // Large Files
                        if (size > LARGE_FILE_THRESHOLD) {
                            largeCount++
                            largeFilesSize += size
                            cachedData?.largeFiles?.add(FileItem().apply {
                                this.name = name
                                this.path = try { File(path).parent ?: "" } catch(e: Exception) { "" }
                                this.size = size
                                this.fullPath = path
                            })
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Analysis", "MediaStore Query Error", e)
            }

            // Processing Storage Folders
            val sortedFolders = folderSizeMap.filter { it.value > 0 }.toList().sortedByDescending { it.second }
            storageItem.moreCount = if (sortedFolders.size > 3) sortedFolders.size - 3 else 0
            cachedData?.storageFolders?.clear()
            sortedFolders.forEach { (name, size) ->
                cachedData?.storageFolders?.add(FolderItem().apply {
                    this.name = name
                    this.path = File(rootDir, name).absolutePath
                    this.size = size
                    this.itemCount = folderCountMap[name] ?: 0
                })
            }
            sortedFolders.take(3).forEach { (name, size) ->
                storageItem.subItems.add(SubItem(name, "${folderCountMap[name] ?: 0} items", Formatter.formatFileSize(ctx, size), R.drawable.ic_root_folder, File(rootDir, name).absolutePath))
            }

            // Processing Duplicates
            var totalWasted = 0L
            var duplicateCount = 0
            sizeMap.forEach { (size, paths) ->
                if (paths.size > 1) {
                    totalWasted += size * (paths.size - 1)
                    duplicateCount += (paths.size - 1)
                    cachedData?.duplicateGroups?.add(DuplicateGroup().apply {
                        fileName = try { File(paths[0]).name } catch (e: Exception) { "Unknown" }
                        this.size = size
                        count = paths.size
                        filePaths.addAll(paths)
                    })
                }
            }
            cachedData?.duplicateGroups?.sortByDescending { it.size * (it.count - 1) }
            duplicatesItem.summary = "$duplicateCount duplicates • ${Formatter.formatFileSize(ctx, totalWasted)} wasted"
            duplicatesItem.moreCount = if (cachedData!!.duplicateGroups.size > 3) cachedData!!.duplicateGroups.size - 3 else 0
            cachedData?.duplicateGroups?.take(3)?.forEach { g ->
                duplicatesItem.subItems.add(SubItem(g.fileName, "${g.count} copies found", Formatter.formatFileSize(ctx, g.size * (g.count - 1)) + " wasted", getFileIcon(g.fileName), g.filePaths[0]))
            }

            // Finalizing Large Files
            cachedData?.largeFiles?.sortByDescending { it.size }
            largeFilesItem.summary = "$largeCount files • ${Formatter.formatFileSize(ctx, largeFilesSize)}"
            largeFilesItem.moreCount = if (cachedData!!.largeFiles.size > 3) cachedData!!.largeFiles.size - 3 else 0
            cachedData?.largeFiles?.take(3)?.forEach { f ->
                largeFilesItem.subItems.add(SubItem(f.name, f.path, Formatter.formatFileSize(ctx, f.size), getFileIcon(f.name), f.fullPath))
            }

            // 2. Apps Info
            try {
                val pm = ctx.packageManager
                val pkgs = pm.getInstalledPackages(0)
                var totalAppSize = 0L
                cachedData?.apps?.clear()
                pkgs.forEach { pkg ->
                    yield()
                    try {
                        pkg.applicationInfo?.let { appInfo ->
                            val appItem = AppItem().apply {
                                name = pm.getApplicationLabel(appInfo).toString()
                                packageName = pkg.packageName
                                val apk = File(appInfo.sourceDir)
                                size = if (apk.exists()) apk.length() else 0
                            }
                            totalAppSize += appItem.size
                            cachedData?.apps?.add(appItem)
                        }
                    } catch (e: Exception) {
                        // Skip individual app error
                    }
                }
                cachedData?.apps?.sortByDescending { it.size }
                storageItem.appsSize = totalAppSize
                appsItem.summary = "${pkgs.size} apps • ${Formatter.formatFileSize(ctx, totalAppSize)}"
                appsItem.moreCount = if (pkgs.size > 3) pkgs.size - 3 else 0
                cachedData?.apps?.take(3)?.forEach { app ->
                    appsItem.subItems.add(SubItem(app.name, app.packageName, Formatter.formatFileSize(ctx, app.size), R.drawable.ic_root_apps, app.packageName, isApp = true))
                }
            } catch (e: Exception) {
                android.util.Log.e("Analysis", "Apps Query Error", e)
            }

            // Storage breakdown finalize
            storageItem.videosSize = totalVideoSize
            storageItem.imagesSize = totalImageSize
            storageItem.audioSize = totalAudioSize
            storageItem.docsSize = totalDocSize
            storageItem.totalSizeBytes = total

            cachedData?.totalVideoSize = totalVideoSize
            cachedData?.totalImageSize = totalImageSize
            cachedData?.totalAudioSize = totalAudioSize
            cachedData?.totalDocSize = totalDocSize
            cachedData?.totalStorageBytes = total

            return@withContext CombinedResults(storageItem, duplicatesItem, largeFilesItem, appsItem)
        } catch (e: Exception) {
            android.util.Log.e("Analysis", "Fatal Analysis Error", e)
            return@withContext null
        }
    }

    // --- UI Helpers ---

    private fun addItemToView(item: AnalysisItem) {
        if (!isAdded) return
        val existingView = (0 until containerLayout.childCount)
            .map { containerLayout.getChildAt(it) }
            .find { it.tag == item.type }

        if (existingView != null) {
            updateItemView(existingView, item)
        } else {
            val view = if (item.type == AnalysisItem.TYPE_STORAGE) {
                layoutInflater.inflate(R.layout.item_analysis_storage, containerLayout, false)
            } else {
                layoutInflater.inflate(R.layout.item_analysis_categry, containerLayout, false)
            }
            view.tag = item.type
            view.setOnClickListener { handleCardClick(item.type) }
            updateItemView(view, item)
            containerLayout.addView(view)
        }
    }

    private fun updateItemView(view: View, item: AnalysisItem) {
        view.findViewById<TextView>(R.id.title).text = item.title
        view.findViewById<TextView>(R.id.summary).text = item.summary
        val mainIcon = view.findViewById<ImageView>(R.id.icon)
        mainIcon.setImageResource(item.icon)
        
        // Apply category colors to main icons
        val categoryColor = when(item.type) {
            AnalysisItem.TYPE_STORAGE -> 0xFF2196F3.toInt() // Blue
            AnalysisItem.TYPE_DUPLICATE -> 0xFFFFC107.toInt() // Amber
            AnalysisItem.TYPE_LARGE_FILES -> 0xFFFF9800.toInt() // Orange
            AnalysisItem.TYPE_APP_MANAGER -> 0xFF4CAF50.toInt() // Green
            else -> 0xFF757575.toInt() // Grey
        }
        mainIcon.setColorFilter(categoryColor, android.graphics.PorterDuff.Mode.SRC_IN)

        if (item.type == AnalysisItem.TYPE_STORAGE) {
            val barContainer = view.findViewById<View>(R.id.storage_bar_container)
            barContainer?.visibility = if (item.isLoading) View.GONE else View.VISIBLE
            view.findViewById<ProgressBar>(R.id.loading)?.visibility = if (item.isLoading) View.VISIBLE else View.GONE
        } else {
            view.findViewById<ProgressBar>(R.id.loading)?.visibility = if (item.isLoading) View.VISIBLE else View.GONE
        }

        val subContainer = view.findViewById<LinearLayout>(R.id.sub_items_container)
        subContainer.removeAllViews()
        if (!item.isLoading && item.subItems.isNotEmpty()) {
            subContainer.visibility = View.VISIBLE
            item.subItems.forEach { sub ->
                val subView = layoutInflater.inflate(R.layout.item_analysis_sub_item, subContainer, false)
                subView.findViewById<TextView>(R.id.name).text = sub.name
                val pathView = subView.findViewById<TextView>(R.id.path)
                pathView.text = sub.path
                // Hide path/package name for Large Files and App Manager to match requested UI
                pathView.visibility = View.GONE
                subView.findViewById<TextView>(R.id.size).text = sub.size
                val subIcon = subView.findViewById<ImageView>(R.id.icon)
                if (sub.isApp) {
                    try {
                        val pm = context?.packageManager
                        val appIcon = pm?.getApplicationIcon(sub.extra)
                        subIcon.setImageDrawable(appIcon)
                        subIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                        subIcon.clearColorFilter() // Don't tint actual app icons
                    } catch (e: Exception) {
                        subIcon.setImageResource(sub.icon)
                        subIcon.scaleType = ImageView.ScaleType.CENTER
                        subIcon.setColorFilter(0xFF4CAF50.toInt(), android.graphics.PorterDuff.Mode.SRC_IN) // Green for app placeholder
                    }
                } else {
                    subIcon.setImageResource(sub.icon)
                    subIcon.scaleType = ImageView.ScaleType.CENTER
                    
                    // Apply sub-item specific colors
                    val subColor = when(item.type) {
                        AnalysisItem.TYPE_STORAGE -> 0xFF2196F3.toInt() // Blue for folders
                        AnalysisItem.TYPE_DUPLICATE -> 0xFFFFC107.toInt() // Amber for duplicates
                        AnalysisItem.TYPE_LARGE_FILES -> 0xFFFF9800.toInt() // Orange for large files
                        else -> 0xFF757575.toInt()
                    }
                    subIcon.setColorFilter(subColor, android.graphics.PorterDuff.Mode.SRC_IN)
                }

                subView.setOnClickListener {
                    if (sub.isApp) {
                        handleAppClick(sub.extra)
                    } else if (item.type == AnalysisItem.TYPE_STORAGE) {
                        handleFolderClick(sub.extra)
                    } else {
                        handleFileClick(sub.extra)
                    }
                }
                subContainer.addView(subView)
            }

            // Update segmented bar weights
            if (item.type == AnalysisItem.TYPE_STORAGE) {
                val totalUsed = item.totalSizeBytes - (item.totalSizeBytes * (100 - item.percentage) / 100) 
                // Wait, use usedBytes directly if available. For now, use percentage of total.
                val total = item.totalSizeBytes
                if (total > 0) {
                    view.findViewById<View>(R.id.segment_apps).layoutParams = LinearLayout.LayoutParams(0, -1, (item.appsSize * 100f / total))
                    view.findViewById<View>(R.id.segment_videos).layoutParams = LinearLayout.LayoutParams(0, -1, (item.videosSize * 100f / total))
                    view.findViewById<View>(R.id.segment_images).layoutParams = LinearLayout.LayoutParams(0, -1, (item.imagesSize * 100f / total))
                    view.findViewById<View>(R.id.segment_audio).layoutParams = LinearLayout.LayoutParams(0, -1, (item.audioSize * 100f / total))
                    view.findViewById<View>(R.id.segment_docs).layoutParams = LinearLayout.LayoutParams(0, -1, (item.docsSize * 100f / total))
                    
                    val usedPercent = item.percentage.toFloat()
                    val accountsForPercent = (item.appsSize + item.videosSize + item.imagesSize + item.audioSize + item.docsSize) * 100f / total
                    val remainingUsedPercent = if (usedPercent > accountsForPercent) usedPercent - accountsForPercent else 0f
                    view.findViewById<View>(R.id.segment_other).layoutParams = LinearLayout.LayoutParams(0, -1, remainingUsedPercent)
                }
            }
        } else {
            subContainer.visibility = View.GONE
        }

        val moreButton = view.findViewById<TextView>(R.id.btn_more)
        if (moreButton != null) {
            if (!item.isLoading && item.moreCount > 0) {
                moreButton.visibility = View.VISIBLE
                moreButton.text = "+${item.moreCount} MORE"
                moreButton.setOnClickListener { handleCardClick(item.type) }
            } else {
                moreButton.visibility = View.GONE
            }
        }
    }

    private fun handleFileClick(path: String) {
        val fm = fragmentManager ?: return
        // Open detail view with this file highlighted or just open detail?
        // For simplicity, navigating to detail is better if we want consistently
        // but opening directly is also good. AnalysisDetailFragment has the logic.
        // Let's just navigate to the category detail.
        val type = cachedData?.getCategoryForFile(path) ?: return
        handleCardClick(type)
    }

    private fun handleFolderClick(path: String) {
        handleCardClick(AnalysisItem.TYPE_STORAGE)
        // Ideally we'd pass the folder to open, but let's just go to storage detail for now
    }

    private fun handleAppClick(packageName: String) {
        handleCardClick(AnalysisItem.TYPE_APP_MANAGER)
    }

    private fun getFileIcon(name: String): Int {
        val n = name.lowercase()
        return when {
            n.endsWith(".apk") -> R.drawable.ic_root_apps
            n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") -> R.drawable.ic_root_video
            n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a") -> R.drawable.ic_root_audio
            n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".jpeg") -> R.drawable.ic_root_image
            n.endsWith(".zip") || n.endsWith(".rar") || n.endsWith(".7z") -> R.drawable.ic_root_archive
            else -> R.drawable.ic_root_document
        }
    }

    private fun handleCardClick(type: Int) {
        val fm = fragmentManager ?: return
        when(type) {
            AnalysisItem.TYPE_STORAGE -> cachedData?.let { AnalysisDetailFragment.show(fm, "Internal Storage", it.storageFolders, null, null, null) }
            AnalysisItem.TYPE_DUPLICATE -> cachedData?.let { AnalysisDetailFragment.show(fm, "Duplicate Files", null, it.duplicateGroups, null, null) }
            AnalysisItem.TYPE_LARGE_FILES -> cachedData?.let { AnalysisDetailFragment.show(fm, "Large Files", null, null, it.largeFiles, null) }
            AnalysisItem.TYPE_APP_MANAGER -> cachedData?.let { AnalysisDetailFragment.show(fm, "App Manager", null, null, null, it.apps) }
        }
    }


    // --- Data Classes and Inner Classes ---

    class AnalysisItem(
        val type: Int,
        val icon: Int,
        val title: String,
        var summary: String,
        var percentage: Int = 0,
        var isLoading: Boolean = false,
        var moreCount: Int = 0,
        val subItems: MutableList<SubItem> = mutableListOf(),
        var appsSize: Long = 0,
        var videosSize: Long = 0,
        var imagesSize: Long = 0,
        var audioSize: Long = 0,
        var docsSize: Long = 0,
        var otherSize: Long = 0,
        var totalSizeBytes: Long = 0
    ) {
        companion object {
            const val TYPE_STORAGE = 1
            const val TYPE_DUPLICATE = 2
            const val TYPE_LARGE_FILES = 3
            const val TYPE_APP_MANAGER = 4
        }
    }

    data class SubItem(val name: String, val path: String, val size: String, val icon: Int, val extra: String, val isApp: Boolean = false)

    class AnalysisCache {
        @JvmField val storageFolders = mutableListOf<FolderItem>()
        @JvmField val allFoldersMap = mutableMapOf<String, FolderItem>()
        @JvmField val allFiles = mutableListOf<FileItem>()
        @JvmField val duplicateGroups = mutableListOf<DuplicateGroup>()
        @JvmField val largeFiles = mutableListOf<FileItem>()
        @JvmField val apps = mutableListOf<AppItem>()
        private var timestamp = 0L

        var totalVideoSize = 0L
        var totalImageSize = 0L
        var totalAudioSize = 0L
        var totalDocSize = 0L
        var totalStorageBytes = 0L

        fun markAnalysisComplete() { timestamp = System.currentTimeMillis() }
        fun isValid() = System.currentTimeMillis() - timestamp < 5 * 60 * 1000
        fun clear() {
            storageFolders.clear()
            allFoldersMap.clear()
            allFiles.clear()
            duplicateGroups.clear()
            largeFiles.clear()
            apps.clear()
            totalVideoSize = 0
            totalImageSize = 0
            totalAudioSize = 0
            totalDocSize = 0
            totalStorageBytes = 0
            timestamp = 0
        }

        fun getCategoryForFile(path: String): Int {
            if (largeFiles.any { it.fullPath == path }) return AnalysisItem.TYPE_LARGE_FILES
            if (duplicateGroups.any { it.filePaths.contains(path) }) return AnalysisItem.TYPE_DUPLICATE
            return AnalysisItem.TYPE_LARGE_FILES
        }
    }

    class FolderItem : Serializable {
        @JvmField var name: String = ""
        @JvmField var path: String = ""
        @JvmField var size: Long = 0
        @JvmField var itemCount: Int = 0
    }

    class FileItem : Serializable {
        @JvmField var name: String = ""
        @JvmField var path: String = ""
        @JvmField var size: Long = 0
        @JvmField var fullPath: String = ""
    }

    class DuplicateGroup : Serializable {
        @JvmField var fileName: String = ""
        @JvmField var size: Long = 0
        @JvmField var count: Int = 0
        @JvmField var filePaths = ArrayList<String>()
    }

    class AppItem : Serializable {
        @JvmField var name: String = ""
        @JvmField var packageName: String = ""
        @JvmField var size: Long = 0
    }
}
