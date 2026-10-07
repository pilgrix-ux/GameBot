package com.itsaky.androidide.apk

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile

data class ApkInspection(
    val jobId: String,
    val originalFile: File,
    val sizeBytes: Long,
    val dexCount: Int,
    val hasManifest: Boolean,
    val hasResourcesTable: Boolean,
    val nativeLibraryCount: Int,
    val assetCount: Int,
    val entryCount: Int,
)

data class ApkJob(
    val id: String,
    val directory: File,
    val inspection: ApkInspection,
)

class ApkJobManager(private val context: Context) {
    fun createJob(uri: Uri, displayName: String?): ApkJob {
        val id = UUID.randomUUID().toString()
        val directory = File(context.cacheDir, "apk-jobs/$id").apply { mkdirs() }
        val original = File(directory, sanitizeFileName(displayName ?: "uploaded.apk"))

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(original).use { output -> input.copyTo(output) }
        } ?: error("Unable to read the selected APK.")

        require(original.length() > 0) { "The selected APK is empty." }
        require(original.length() <= MAX_APK_BYTES) { "APK is larger than the 250 MB limit." }

        val inspection = inspect(id, original)
        return ApkJob(id, directory, inspection)
    }

    private fun inspect(jobId: String, original: File): ApkInspection {
        var dexCount = 0
        var hasManifest = false
        var hasResourcesTable = false
        var nativeLibraryCount = 0
        var assetCount = 0
        var entryCount = 0

        ZipFile(original).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                entryCount++
                when {
                    name == "AndroidManifest.xml" -> hasManifest = true
                    name == "resources.arsc" -> hasResourcesTable = true
                    name.matches(Regex("classes\\d*\\.dex")) -> dexCount++
                    name.startsWith("lib/") && name.endsWith(".so") -> nativeLibraryCount++
                    name.startsWith("assets/") && !name.endsWith("/") -> assetCount++
                }
            }
        }

        require(hasManifest) { "The selected file does not contain an AndroidManifest.xml." }
        require(dexCount > 0) { "The selected file does not contain Android bytecode." }

        return ApkInspection(
            jobId = jobId,
            originalFile = original,
            sizeBytes = original.length(),
            dexCount = dexCount,
            hasManifest = hasManifest,
            hasResourcesTable = hasResourcesTable,
            nativeLibraryCount = nativeLibraryCount,
            assetCount = assetCount,
            entryCount = entryCount,
        )
    }

    private fun sanitizeFileName(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "uploaded.apk" }
        return base.replace(Regex("[^A-Za-z0-9._-]"), "_").let {
            if (it.lowercase().endsWith(".apk")) it else "$it.apk"
        }
    }

    companion object {
        private const val MAX_APK_BYTES = 250L * 1024L * 1024L
    }
}
