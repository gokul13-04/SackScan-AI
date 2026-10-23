package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {
    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 85

    /**
     * Resizes the bitmap so that neither width nor height exceeds MAX_DIMENSION (1024px),
     * maintaining the original aspect ratio.
     */
    fun resizeToMax1024(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        if (maxSide <= MAX_DIMENSION) {
            return source
        }

        val scale = MAX_DIMENSION.toFloat() / maxSide
        val targetWidth = (width * scale).toInt()
        val targetHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Compresses the bitmap to JPEG format and returns the Base64 encoded string.
     */
    fun toBase64Jpeg(bitmap: Bitmap): String {
        val resized = resizeToMax1024(bitmap)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Decodes a Bitmap from an Android content Uri, handling orientation and downsampling.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxSide = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            contentResolver.openInputStream(uri)?.use { stream ->
                val decoded = BitmapFactory.decodeStream(stream, null, decodeOptions) ?: return null
                resizeToMax1024(decoded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
