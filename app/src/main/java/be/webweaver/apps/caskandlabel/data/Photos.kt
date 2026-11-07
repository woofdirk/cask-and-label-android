package be.webweaver.apps.caskandlabel.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/** Label photos: a camera file to write into, and a JPEG small enough for the bucket (max 5 MB, image/jpeg). */
object Photos {
    private const val MAX_SIDE = 2000

    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("label-", ".jpg", dir)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /** Decodes, turns upright (EXIF), scales the longest side to at most 2000 px and encodes as JPEG. */
    fun jpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        var quality = 85
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            quality -= 10
        } while (bytes.size > 4_500_000 && quality > 30)
        return bytes
    }
}
