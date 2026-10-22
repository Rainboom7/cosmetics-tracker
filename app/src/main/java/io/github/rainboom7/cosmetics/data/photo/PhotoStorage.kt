package io.github.rainboom7.cosmetics.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The user's own photos live in `filesDir/photos`, so they survive after the gallery item or
 * temporary camera file is gone. Items store `file://` URIs of these files.
 */
class PhotoStorage(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** A new empty file for the camera to write into. */
    fun newPhotoFile(): File = File(dir, "${UUID.randomUUID()}.jpg")

    /** A `content://` URI of [file] for `ActivityResultContracts.TakePicture`. */
    fun contentUriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Copies a picked image (e.g. from the photo picker) into app storage, returns its `file://` URI. */
    suspend fun importFrom(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = newPhotoFile()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open $uri" }
            target.outputStream().use { input.copyTo(it) }
        }
        Uri.fromFile(target).toString()
    }

    fun uriOf(file: File): String = Uri.fromFile(file).toString()

    /** Deletes a photo of ours; URIs that are not in our folder are ignored. */
    suspend fun delete(uri: String) = withContext(Dispatchers.IO) {
        val path = Uri.parse(uri).path ?: return@withContext
        val file = File(path)
        if (file.parentFile?.canonicalPath == dir.canonicalPath) file.delete()
    }
}
