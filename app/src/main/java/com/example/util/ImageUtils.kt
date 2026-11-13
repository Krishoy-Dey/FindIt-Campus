package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {

    private const val MAX_DIMENSION_PX = 720
    private const val JPEG_QUALITY = 68
    private const val MAX_BASE64_LENGTH = 340_000

    suspend fun compressUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@withContext null

            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > MAX_DIMENSION_PX * 2 ||
                boundsOptions.outHeight / sampleSize > MAX_DIMENSION_PX * 2
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val maxSide = max(decoded.width, decoded.height)
            val scaledBitmap = if (maxSide > MAX_DIMENSION_PX) {
                val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            var quality = JPEG_QUALITY
            var encoded: String
            do {
                val out = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 12
            } while (encoded.length > MAX_BASE64_LENGTH && quality >= 30)

            scaledBitmap.recycle()
            if (encoded.length > MAX_BASE64_LENGTH) null else encoded
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
