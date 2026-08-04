package com.zubora.taijuki.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Stores the onboarding avatar as a downscaled JPEG in app-internal storage,
 * keyed by a fixed filename (mirrors image-slot.js's own re-encode-on-drop
 * behavior, minus its persistence sidecar — one avatar per install here).
 */
object AvatarStore {
    private const val FILE_NAME = "avatar.jpg"
    private const val MAX_DIM = 512

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext false
            val orientation = readOrientation(bytes)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext false
            val rotated = applyOrientation(decoded, orientation)
            val scaled = downscale(rotated, MAX_DIM)
            FileOutputStream(file(context)).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun load(context: Context): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    private fun readOrientation(bytes: ByteArray): Int = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun downscale(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val scale = maxDim.toFloat() / maxOf(w, h)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
