package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.model.SocialPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

enum class PhotoFilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    MONO("Mono"),
    WARM("Warm"),
    COOL("Cool"),
    CINEMA("Cinema")
}

enum class AspectCropPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

data class PhotoEditConfig(
    val rotationDegrees: Int = 0,
    val cropPreset: AspectCropPreset = AspectCropPreset.ORIGINAL,
    val filterPreset: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val brightness: Float = 0f,     // -100..100
    val contrast: Float = 1f,       // 0.5..1.5
    val saturation: Float = 1f      // 0.0..2.0
)

object MediaProcessingEngine {

    suspend fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var inSampleSize = 1
            while ((boundsOpts.outWidth / inSampleSize) > maxDimension ||
                (boundsOpts.outHeight / inSampleSize) > maxDimension
            ) {
                inSampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun applyPhotoEdits(
        source: Bitmap,
        config: PhotoEditConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Rotate
        val rotated = if (config.rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(config.rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        // 2. Aspect crop
        val cropped = config.cropPreset.ratio?.let { targetRatio ->
            val currentRatio = rotated.width.toFloat() / rotated.height.toFloat()
            val cropW: Int
            val cropH: Int
            if (currentRatio > targetRatio) {
                cropH = rotated.height
                cropW = (cropH * targetRatio).toInt().coerceAtLeast(1)
            } else {
                cropW = rotated.width
                cropH = (cropW / targetRatio).toInt().coerceAtLeast(1)
            }
            val x = ((rotated.width - cropW) / 2).coerceAtLeast(0)
            val y = ((rotated.height - cropH) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(rotated, x, y, min(cropW, rotated.width - x), min(cropH, rotated.height - y))
        } ?: rotated

        // 3. ColorMatrix (filter + brightness + contrast + saturation)
        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = ColorMatrix()

        when (config.filterPreset) {
            PhotoFilterPreset.ORIGINAL -> Unit
            PhotoFilterPreset.VIVID -> matrix.setSaturation(1.45f)
            PhotoFilterPreset.MONO -> matrix.setSaturation(0f)
            PhotoFilterPreset.WARM -> matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 12f,
                        0f, 1.03f, 0f, 0f, 4f,
                        0f, 0f, 0.88f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            PhotoFilterPreset.COOL -> matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        0.90f, 0f, 0f, 0f, -6f,
                        0f, 1.02f, 0f, 0f, 4f,
                        0f, 0f, 1.18f, 0f, 14f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            PhotoFilterPreset.CINEMA -> {
                matrix.setSaturation(0.82f)
                matrix.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.18f, 0f, 0f, 0f, -12f,
                            0f, 1.10f, 0f, 0f, -10f,
                            0f, 0f, 1.05f, 0f, -6f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        }

        // Saturation adjustment
        val satMatrix = ColorMatrix().apply { setSaturation(config.saturation) }
        matrix.postConcat(satMatrix)

        // Contrast & Brightness adjustment
        val c = config.contrast
        val t = (1f - c) * 128f + config.brightness
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        out
    }

    suspend fun bitmapToCompressedBase64(
        bitmap: Bitmap,
        maxDimension: Int = 720,
        quality: Int = 72
    ): String = withContext(Dispatchers.IO) {
        val scaled = scaleDownBitmap(bitmap, maxDimension)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveBitmapToPrivateFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "media/photos").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        file.absolutePath
    }

    suspend fun copyUriToPrivateMedia(
        context: Context,
        uri: Uri,
        subFolder: String,
        extension: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "media/$subFolder").apply { mkdirs() }
            val file = File(dir, "media_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractVideoMetadata(
        context: Context,
        uri: Uri,
        frameAtMs: Long = 500L
    ): Pair<Long, Bitmap?> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val clampedUs = (frameAtMs.coerceIn(0L, maxOf(0L, durationMs - 100L))) * 1000L
            val frame = retriever.getFrameAtTime(clampedUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            durationMs to frame
        } catch (_: Exception) {
            0L to null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun extractAudioDurationSeconds(
        context: Context,
        uri: Uri
    ): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            (durationMs / 1000L).coerceAtLeast(1L)
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun sharePostNatively(context: Context, post: SocialPost) {
        val shareText = buildString {
            if (post.title.isNotBlank()) append("${post.title}\n")
            if (post.caption.isNotBlank()) append("${post.caption}\n")
            if (post.hashtags.isNotEmpty()) append(post.hashtags.joinToString(" ") { "#$it" } + "\n")
            append("Shared from PulseStream by @${post.authorUsername}")
        }

        val localFile = post.localMediaPath.takeIf { it.isNotBlank() }?.let(::File)
        val intent = if (localFile != null && localFile.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )
            Intent(Intent.ACTION_SEND).apply {
                type = post.mimeType.ifBlank {
                    when (post.mediaType) {
                        "photo" -> "image/jpeg"
                        "video" -> "video/mp4"
                        else -> "audio/*"
                    }
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share ${post.mediaType} post"))
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = min(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
