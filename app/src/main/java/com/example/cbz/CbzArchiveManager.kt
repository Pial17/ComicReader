package com.example.cbz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object CbzArchiveManager {
    private const val TAG = "CbzArchiveManager"

    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    /**
     * Resolves a file path or content URI into a local File.
     * If it's already a direct file path that exists, returns that File.
     * If it's a content URI, copies it into a temp file cache if needed for ZipFile access.
     */
    suspend fun getLocalFile(context: Context, pathOrUri: String): File? = withContext(Dispatchers.IO) {
        try {
            if (pathOrUri.startsWith("/")) {
                val file = File(pathOrUri)
                if (file.exists() && file.canRead()) {
                    return@withContext file
                }
            } else if (pathOrUri.startsWith("file://")) {
                val uri = Uri.parse(pathOrUri)
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        return@withContext file
                    }
                }
            }

            // If it's a content URI or inaccessible file path, stream into a cached temp file
            val uri = pathOrUri.toUri()
            val tempDir = File(context.cacheDir, "cbz_active").apply { mkdirs() }
            val hash = md5(pathOrUri)
            val tempFile = File(tempDir, "$hash.cbz")

            // Check if tempFile exists and is valid
            if (tempFile.exists() && tempFile.length() > 0) {
                return@withContext tempFile
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local file for $pathOrUri", e)
            null
        }
    }

    /**
     * Inspects a CBZ archive and returns all image entries in natural sorted order.
     */
    suspend fun getPageEntries(context: Context, pathOrUri: String): List<CbzPageEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CbzPageEntry>()
        try {
            val localFile = getLocalFile(context, pathOrUri)
            if (localFile != null && localFile.exists()) {
                ZipFile(localFile).use { zip ->
                    val zipEntries = zip.entries()
                    val rawList = mutableListOf<ZipEntry>()
                    while (zipEntries.hasMoreElements()) {
                        val entry = zipEntries.nextElement()
                        if (!entry.isDirectory && isValidImageEntry(entry.name)) {
                            rawList.add(entry)
                        }
                    }

                    // Sort using natural alphanumeric order
                    rawList.sortWith { a, b ->
                        NaturalOrderComparator.INSTANCE.compare(a.name, b.name)
                    }

                    rawList.forEachIndexed { index, entry ->
                        val fileName = entry.name.substringAfterLast('/')
                        entries.add(
                            CbzPageEntry(
                                index = index,
                                entryName = entry.name,
                                displayName = fileName,
                                size = entry.size
                            )
                        )
                    }
                }
            } else {
                // Fallback to streaming ZipInputStream for content URI if temp file wasn't created
                val uri = pathOrUri.toUri()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ZipInputStream(stream).use { zipStream ->
                        val rawNames = mutableListOf<String>()
                        var ze: ZipEntry? = zipStream.nextEntry
                        while (ze != null) {
                            if (!ze.isDirectory && isValidImageEntry(ze.name)) {
                                rawNames.add(ze.name)
                            }
                            zipStream.closeEntry()
                            ze = zipStream.nextEntry
                        }
                        rawNames.sortWith(NaturalOrderComparator.INSTANCE)
                        rawNames.forEachIndexed { index, name ->
                            entries.add(
                                CbzPageEntry(
                                    index = index,
                                    entryName = name,
                                    displayName = name.substringAfterLast('/'),
                                    size = 0L
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read entries from $pathOrUri", e)
        }
        entries
    }

    private fun isValidImageEntry(name: String): Boolean {
        if (name.startsWith("__MACOSX/") || name.contains("/__MACOSX/")) return false
        val fileName = name.substringAfterLast('/')
        if (fileName.startsWith(".")) return false
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_EXTENSIONS
    }

    /**
     * Extracts the first page or specified page to generate a thumbnail.
     * Caches the result on disk in context.cacheDir/cbz_thumbs/
     */
    suspend fun extractThumbnail(
        context: Context,
        pathOrUri: String,
        entryName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val thumbDir = File(context.cacheDir, "cbz_thumbs").apply { mkdirs() }
            val hash = md5(pathOrUri)
            val thumbFile = File(thumbDir, "${hash}.webp")

            if (thumbFile.exists() && thumbFile.length() > 0) {
                return@withContext thumbFile.absolutePath
            }

            val targetEntryName = entryName ?: getPageEntries(context, pathOrUri).firstOrNull()?.entryName
            if (targetEntryName == null) return@withContext null

            val localFile = getLocalFile(context, pathOrUri)
            var thumbBitmap: Bitmap? = null

            if (localFile != null && localFile.exists()) {
                ZipFile(localFile).use { zip ->
                    val entry = zip.getEntry(targetEntryName) ?: return@withContext null
                    zip.getInputStream(entry).use { input ->
                        thumbBitmap = decodeSampledBitmapFromStream(input, 360, 540)
                    }
                }
            } else {
                // Stream lookup
                val uri = pathOrUri.toUri()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ZipInputStream(stream).use { zipStream ->
                        var ze = zipStream.nextEntry
                        while (ze != null) {
                            if (ze.name == targetEntryName) {
                                thumbBitmap = decodeSampledBitmapFromStream(zipStream, 360, 540)
                                break
                            }
                            zipStream.closeEntry()
                            ze = zipStream.nextEntry
                        }
                    }
                }
            }

            if (thumbBitmap != null) {
                FileOutputStream(thumbFile).use { out ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        thumbBitmap!!.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                    } else {
                        thumbBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                }
                thumbBitmap!!.recycle()
                return@withContext thumbFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for $pathOrUri", e)
        }
        null
    }

    /**
     * Loads a specific page bitmap from the CBZ archive with memory-safe downsampling.
     */
    suspend fun loadPageBitmap(
        context: Context,
        pathOrUri: String,
        entryName: String,
        maxWidth: Int = 1800,
        maxHeight: Int = 2600
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val localFile = getLocalFile(context, pathOrUri)
            if (localFile != null && localFile.exists()) {
                ZipFile(localFile).use { zip ->
                    val entry = zip.getEntry(entryName) ?: return@withContext null
                    // Read into byte array or stream
                    zip.getInputStream(entry).use { stream ->
                        val bytes = stream.readBytes()
                        return@withContext decodeSampledBitmapFromBytes(bytes, maxWidth, maxHeight)
                    }
                }
            } else {
                val uri = pathOrUri.toUri()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ZipInputStream(stream).use { zipStream ->
                        var ze = zipStream.nextEntry
                        while (ze != null) {
                            if (ze.name == entryName) {
                                val bytes = zipStream.readBytes()
                                return@withContext decodeSampledBitmapFromBytes(bytes, maxWidth, maxHeight)
                            }
                            zipStream.closeEntry()
                            ze = zipStream.nextEntry
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load page $entryName from $pathOrUri", e)
        }
        null
    }

    private fun decodeSampledBitmapFromBytes(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(data, 0, data.size, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        return try {
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (oom: OutOfMemoryError) {
            Log.w(TAG, "OOM decoding bitmap, retrying with RGB_565 and higher sample size")
            options.inSampleSize *= 2
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }
    }

    private fun decodeSampledBitmapFromStream(stream: InputStream, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bytes = stream.readBytes()
        return decodeSampledBitmapFromBytes(bytes, reqWidth, reqHeight)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun clearThumbnailCache(context: Context): Boolean {
        return try {
            val thumbDir = File(context.cacheDir, "cbz_thumbs")
            if (thumbDir.exists()) {
                thumbDir.listFiles()?.forEach { it.delete() }
            }
            val activeDir = File(context.cacheDir, "cbz_active")
            if (activeDir.exists()) {
                activeDir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }

    fun getThumbnailCacheSize(context: Context): Long {
        var totalSize = 0L
        val thumbDir = File(context.cacheDir, "cbz_thumbs")
        if (thumbDir.exists()) {
            thumbDir.listFiles()?.forEach { totalSize += it.length() }
        }
        val activeDir = File(context.cacheDir, "cbz_active")
        if (activeDir.exists()) {
            activeDir.listFiles()?.forEach { totalSize += it.length() }
        }
        return totalSize
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
