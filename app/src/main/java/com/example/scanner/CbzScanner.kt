package com.example.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cbz.CbzArchiveManager
import com.example.data.model.BookEntity
import com.example.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class CbzScanner(
    private val context: Context,
    private val repository: BookRepository
) {
    companion object {
        private const val TAG = "CbzScanner"
        private val CBZ_EXTENSIONS = setOf("cbz")
        private val ZIP_EXTENSIONS = setOf("zip")
        private val ALL_EXTENSIONS = setOf("cbz", "zip")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }

    data class ScanProgress(
        val isScanning: Boolean,
        val statusMessage: String,
        val foundCount: Int,
        val currentFile: String = "",
        val currentStep: Int = 0,
        val totalSteps: Int = 0
    )

    /**
     * Scans the entire device across all storage volumes and directories to find all CBZ files.
     */
    suspend fun scanStorage(
        onProgress: (ScanProgress) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val discoveredPaths = mutableSetOf<String>()
        val visitedDirs = mutableSetOf<String>()

        onProgress(
            ScanProgress(
                isScanning = true,
                statusMessage = "Starting whole-device scan for comic archives...",
                foundCount = 0
            )
        )

        // 1. Fast Index Scan via MediaStore.Files
        try {
            onProgress(
                ScanProgress(
                    isScanning = true,
                    statusMessage = "Checking system media index for CBZ files...",
                    foundCount = discoveredPaths.size
                )
            )
            queryMediaStore(discoveredPaths)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        // 2. Comprehensive File System Traversal of All Storage Roots & SD Cards
        val storageRoots = getAllStorageRoots()
        Log.d(TAG, "Storage roots to scan: ${storageRoots.map { it.absolutePath }}")

        for (root in storageRoots) {
            try {
                if (root.exists() && root.canRead()) {
                    onProgress(
                        ScanProgress(
                            isScanning = true,
                            statusMessage = "Scanning storage volume: ${root.name.ifEmpty { root.absolutePath }}...",
                            foundCount = discoveredPaths.size
                        )
                    )
                    scanDirectoryRecursive(
                        directory = root,
                        results = discoveredPaths,
                        visitedDirs = visitedDirs,
                        currentDepth = 0,
                        maxDepth = 30,
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning root ${root.absolutePath}", e)
            }
        }

        // 3. Scan user-registered custom folders (Storage Access Framework tree URIs)
        val userFolders = repository.getAllFoldersList()
        for (folder in userFolders) {
            try {
                onProgress(
                    ScanProgress(
                        isScanning = true,
                        statusMessage = "Scanning custom folder: ${folder.displayName}...",
                        foundCount = discoveredPaths.size
                    )
                )
                scanTreeUri(Uri.parse(folder.uriString), discoveredPaths)
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning tree uri ${folder.uriString}", e)
            }
        }

        // 4. Process all discovered files and populate/update the library
        val totalToProcess = discoveredPaths.size
        var processedCount = 0
        var addedCount = 0

        onProgress(
            ScanProgress(
                isScanning = true,
                statusMessage = "Discovered $totalToProcess potential comic files. Cataloging...",
                foundCount = totalToProcess,
                totalSteps = totalToProcess
            )
        )

        for (path in discoveredPaths) {
            processedCount++
            val title = extractBookTitle(path)
            onProgress(
                ScanProgress(
                    isScanning = true,
                    statusMessage = "Processing $processedCount of $totalToProcess: $title",
                    foundCount = addedCount,
                    currentFile = title,
                    currentStep = processedCount,
                    totalSteps = totalToProcess
                )
            )

            val book = processCbzFile(path)
            if (book != null) {
                addedCount++
            }
        }

        // 5. Clean up missing local files from database that were deleted from disk
        try {
            val allExisting = repository.getAllBooksSync()
            for (book in allExisting) {
                if (book.filePath.startsWith("/") && !File(book.filePath).exists()) {
                    Log.d(TAG, "Removing deleted file from library: ${book.filePath}")
                    repository.deleteBookById(book.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning missing books", e)
        }

        onProgress(
            ScanProgress(
                isScanning = false,
                statusMessage = if (addedCount > 0) "Whole device scan complete: $addedCount comics in library" else "Whole device scan complete: No CBZ files found",
                foundCount = addedCount,
                currentStep = totalToProcess,
                totalSteps = totalToProcess
            )
        )

        addedCount
    }

    /**
     * Inspects and imports a single CBZ file (e.g. from file picker or intent).
     */
    suspend fun importSingleCbz(pathOrUri: String): BookEntity? = withContext(Dispatchers.IO) {
        processCbzFile(pathOrUri)
    }

    /**
     * Queries MediaStore.Files across external storage for any indexed CBZ or comic archives.
     */
    private fun queryMediaStore(results: MutableSet<String>) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        )

        val selection = "(${MediaStore.MediaColumns.DATA} LIKE '%.cbz' OR " +
                "${MediaStore.MediaColumns.DATA} LIKE '%.CBZ' OR " +
                "${MediaStore.MediaColumns.DATA} LIKE '%.zip' OR " +
                "${MediaStore.MediaColumns.DATA} LIKE '%.ZIP' OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%.cbz' OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE '%.CBZ') AND " +
                "${MediaStore.MediaColumns.SIZE} > 0"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null) {
                val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val idIdx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val filePath = if (dataIdx >= 0) cursor.getString(dataIdx) else null
                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""

                    if (!filePath.isNullOrBlank()) {
                        val file = File(filePath)
                        if (file.exists() && file.canRead()) {
                            if (filePath.endsWith(".cbz", ignoreCase = true)) {
                                results.add(file.absolutePath)
                            } else if (filePath.endsWith(".zip", ignoreCase = true) && isLikelyComicZip(file)) {
                                results.add(file.absolutePath)
                            }
                            continue
                        }
                    }

                    // Fallback to Content URI if direct file path is not accessible
                    if (idIdx >= 0) {
                        val id = cursor.getLong(idIdx)
                        val contentUri = ContentUris.withAppendedId(uri, id)
                        if (displayName.endsWith(".cbz", ignoreCase = true)) {
                            results.add(contentUri.toString())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in queryMediaStore", e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Resolves all root directories to scan:
     * - Primary user storage (e.g. /storage/emulated/0)
     * - SD Cards / USB OTG (e.g. /storage/XXXX-XXXX)
     * - Secondary external directories
     */
    private fun getAllStorageRoots(): List<File> {
        val roots = LinkedHashSet<File>()

        try {
            // 1. Primary external storage root (/storage/emulated/0)
            val primary = Environment.getExternalStorageDirectory()
            if (primary != null && primary.exists()) {
                roots.add(primary)
            }

            // 2. Discover all mounted storage volumes from /storage
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.canRead()) {
                val subDirs = storageDir.listFiles()
                if (subDirs != null) {
                    for (sub in subDirs) {
                        val name = sub.name.lowercase()
                        if (name == "self" || name == "knox-emulated") continue
                        if (name == "emulated") {
                            sub.listFiles()?.filter { it.isDirectory && it.canRead() }?.forEach {
                                roots.add(it)
                            }
                        } else if (sub.isDirectory && sub.canRead()) {
                            // SD Card or secondary volume (e.g. /storage/1234-5678)
                            roots.add(sub)
                        }
                    }
                }
            }

            // 3. App external directories provide paths to external SD cards
            val extDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (extDir in extDirs) {
                if (extDir != null) {
                    val path = extDir.absolutePath
                    val androidIndex = path.indexOf("/Android")
                    if (androidIndex > 0) {
                        val volumeRoot = File(path.substring(0, androidIndex))
                        if (volumeRoot.exists() && volumeRoot.canRead()) {
                            roots.add(volumeRoot)
                        }
                    }
                }
            }

            // 4. Standard public folders as direct checkpoints
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads != null && downloads.exists()) roots.add(downloads)
            val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documents != null && documents.exists()) roots.add(documents)
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (pictures != null && pictures.exists()) roots.add(pictures)
        } catch (e: Exception) {
            Log.e(TAG, "Error enumerating storage roots", e)
        }

        return roots.filter { it.exists() && it.canRead() }
    }

    /**
     * Recursively walks all folders in a directory to find any .cbz or comic .zip files.
     */
    private fun scanDirectoryRecursive(
        directory: File,
        results: MutableSet<String>,
        visitedDirs: MutableSet<String>,
        currentDepth: Int,
        maxDepth: Int,
        onProgress: (ScanProgress) -> Unit
    ) {
        if (currentDepth > maxDepth) return

        // Prevent infinite loops from symlinks or circular mounts
        val canonicalPath = try {
            directory.canonicalPath
        } catch (e: Exception) {
            directory.absolutePath
        }

        if (!visitedDirs.add(canonicalPath)) {
            return
        }

        val files = try {
            directory.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        for (file in files) {
            if (file.isDirectory) {
                val dirName = file.name
                // Skip hidden folders (e.g. .thumbnails, .cache, .git)
                if (dirName.startsWith(".")) continue

                // Skip system private app sandboxes (Android/data and Android/obb)
                if (dirName.equals("Android", ignoreCase = true) &&
                    (file.parentFile?.name == "0" || file.parentFile?.parentFile?.name == "storage")
                ) {
                    continue
                }

                // Skip system runtime virtual directories
                if (dirName.equals("proc", ignoreCase = true) ||
                    dirName.equals("sys", ignoreCase = true) ||
                    dirName.equals("dev", ignoreCase = true)
                ) {
                    continue
                }

                // Recurse into child directory
                scanDirectoryRecursive(file, results, visitedDirs, currentDepth + 1, maxDepth, onProgress)
            } else {
                val ext = file.extension.lowercase()
                if (ext in CBZ_EXTENSIONS) {
                    results.add(file.absolutePath)
                    if (results.size % 5 == 0) {
                        onProgress(
                            ScanProgress(
                                isScanning = true,
                                statusMessage = "Found ${results.size} CBZ files... (Checking ${directory.name})",
                                foundCount = results.size
                            )
                        )
                    }
                } else if (ext in ZIP_EXTENSIONS) {
                    // Check if zip contains comic images
                    if (isLikelyComicZip(file)) {
                        results.add(file.absolutePath)
                    }
                }
            }
        }
    }

    /**
     * Quickly checks if a zip archive contains image files (making it a comic book archive).
     */
    private fun isLikelyComicZip(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                var imageCount = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory) {
                        val ext = entry.name.substringAfterLast('.', "").lowercase()
                        if (ext in IMAGE_EXTENSIONS) {
                            imageCount++
                            if (imageCount >= 2) return true
                        }
                    }
                }
                imageCount > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Traverses a DocumentTree URI recursively.
     */
    private fun scanTreeUri(treeUri: Uri, results: MutableSet<String>) {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            scanDocumentChildrenRecursive(treeUri, childrenUri, results, 0, 30)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning tree uri $treeUri", e)
        }
    }

    private fun scanDocumentChildrenRecursive(
        treeUri: Uri,
        parentDocUri: Uri,
        results: MutableSet<String>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(parentDocUri, projection, null, null, null)
            if (cursor != null) {
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: ""
                    val mimeType = cursor.getString(mimeIndex) ?: ""

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (!displayName.startsWith(".")) {
                            val subChildUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                            scanDocumentChildrenRecursive(treeUri, subChildUri, results, currentDepth + 1, maxDepth)
                        }
                    } else {
                        val ext = displayName.substringAfterLast('.', "").lowercase()
                        if (ext in ALL_EXTENSIONS) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            results.add(docUri.toString())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query document children", e)
        } finally {
            cursor?.close()
        }
    }

    private suspend fun processCbzFile(pathOrUri: String): BookEntity? {
        try {
            val existing = repository.getBookByPath(pathOrUri)
            val title = extractBookTitle(pathOrUri)
            val fileSize = getFileSize(pathOrUri)
            val lastModified = getFileLastModified(pathOrUri)

            // Fast path: If already indexed with same size and valid cover, avoid expensive extraction
            if (existing != null && existing.pageCount > 0 && existing.coverPath != null) {
                if (File(existing.coverPath).exists() && (fileSize == 0L || existing.fileSize == fileSize)) {
                    return existing
                }
            }

            // Inspect archive for page count and generate thumbnail
            val entries = CbzArchiveManager.getPageEntries(context, pathOrUri)
            if (entries.isEmpty()) {
                // Not a valid comic archive (no readable images)
                return null
            }

            val pageCount = entries.size
            val coverPath = CbzArchiveManager.extractThumbnail(context, pathOrUri, entries.first().entryName)

            val book = if (existing != null) {
                existing.copy(
                    title = if (existing.title.isBlank()) title else existing.title,
                    fileSize = fileSize,
                    lastModified = lastModified,
                    pageCount = pageCount,
                    coverPath = coverPath ?: existing.coverPath
                )
            } else {
                BookEntity(
                    filePath = pathOrUri,
                    title = title,
                    fileSize = fileSize,
                    lastModified = lastModified,
                    pageCount = pageCount,
                    currentPage = 1,
                    progressPercent = 0f,
                    isFavorite = false,
                    lastOpenedTime = 0L,
                    readingMode = "DEFAULT",
                    readingDirection = "DEFAULT",
                    coverPath = coverPath
                )
            }

            if (existing != null) {
                repository.updateBook(book)
            } else {
                repository.insertBook(book)
            }
            return book
        } catch (e: Exception) {
            Log.e(TAG, "Error processing CBZ file $pathOrUri", e)
            return null
        }
    }

    private fun extractBookTitle(pathOrUri: String): String {
        val fileName = if (pathOrUri.startsWith("content://")) {
            var name: String? = null
            try {
                val uri = Uri.parse(pathOrUri)
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            name ?: Uri.parse(pathOrUri).lastPathSegment ?: "Comic"
        } else {
            File(pathOrUri).name
        }
        return fileName.substringBeforeLast('.')
    }

    private fun getFileSize(pathOrUri: String): Long {
        return try {
            if (pathOrUri.startsWith("content://")) {
                var size = 0L
                context.contentResolver.query(
                    Uri.parse(pathOrUri),
                    arrayOf(android.provider.OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (idx >= 0) size = cursor.getLong(idx)
                    }
                }
                size
            } else {
                File(pathOrUri).length()
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFileLastModified(pathOrUri: String): Long {
        return try {
            if (pathOrUri.startsWith("content://")) {
                System.currentTimeMillis()
            } else {
                File(pathOrUri).lastModified()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
