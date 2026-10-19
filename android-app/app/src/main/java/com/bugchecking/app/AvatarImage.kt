package com.bugchecking.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

internal object AvatarImage {
    const val DEVICE_SIZE = 46
    const val DEVICE_BYTES = DEVICE_SIZE * DEVICE_SIZE * 2
    private const val MAX_SOURCE_SIDE = 4096

    fun decode(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_SOURCE_SIDE) {
                    val ratio = MAX_SOURCE_SIDE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * ratio).roundToInt(), (info.size.height * ratio).roundToInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SOURCE_SIDE) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("无法读取头像图片")
        }
    }

    fun cropSquare(source: Bitmap, viewport: IntSize, scale: Float, offset: Offset): Bitmap {
        require(viewport.width > 0 && viewport.height > 0)
        val side = minOf(viewport.width, viewport.height).toFloat()
        val baseScale = max(side / source.width, side / source.height)
        val totalScale = baseScale * scale.coerceAtLeast(1f)
        val cropSide = (side / totalScale).coerceAtMost(minOf(source.width, source.height).toFloat())
        val centerX = source.width / 2f - offset.x / totalScale
        val centerY = source.height / 2f - offset.y / totalScale
        val left = (centerX - cropSide / 2f).coerceIn(0f, source.width - cropSide)
        val top = (centerY - cropSide / 2f).coerceIn(0f, source.height - cropSide)
        val size = cropSide.roundToInt().coerceAtLeast(1)
        return Bitmap.createBitmap(source, left.roundToInt().coerceAtMost(source.width - size),
            top.roundToInt().coerceAtMost(source.height - size), size, size)
    }

    fun clampOffset(source: Bitmap, viewport: IntSize, scale: Float, offset: Offset): Offset {
        if (viewport.width == 0 || viewport.height == 0) return Offset.Zero
        val side = minOf(viewport.width, viewport.height).toFloat()
        val baseScale = max(side / source.width, side / source.height)
        val maxX = ((source.width * baseScale * scale - side) / 2f).coerceAtLeast(0f)
        val maxY = ((source.height * baseScale * scale - side) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    fun save(context: Context, bitmap: Bitmap, previousPath: String?): String {
        val directory = File(context.filesDir, "profile").apply { mkdirs() }
        val destination = File(directory, "avatar_${System.currentTimeMillis()}.png")
        FileOutputStream(destination).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "头像保存失败" }
        }
        previousPath?.let(::File)?.takeIf { it.absolutePath != destination.absolutePath }?.delete()
        return destination.absolutePath
    }

    fun load(path: String?): Bitmap? = path?.takeIf(String::isNotBlank)?.let { BitmapFactory.decodeFile(it) }

    fun loadPreview(path: String?, targetSide: Int = 256): Bitmap? {
        val source = path?.takeIf(String::isNotBlank) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetSide) sample *= 2
        return BitmapFactory.decodeFile(source, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun remove(path: String?) { path?.let(::File)?.delete() }

    fun deviceRgb565(path: String?): ByteArray? {
        val original = load(path) ?: return null
        val square = Bitmap.createBitmap(DEVICE_SIZE, DEVICE_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(square).apply {
            drawColor(Color.rgb(0x10, 0x1c, 0x18))
            drawBitmap(original, null, android.graphics.Rect(0, 0, DEVICE_SIZE, DEVICE_SIZE), null)
        }
        if (original !== square) original.recycle()
        val pixels = IntArray(DEVICE_SIZE * DEVICE_SIZE)
        square.getPixels(pixels, 0, DEVICE_SIZE, 0, 0, DEVICE_SIZE, DEVICE_SIZE)
        square.recycle()
        val output = ByteArray(DEVICE_BYTES)
        pixels.forEachIndexed { index, color ->
            val value = rgb888ToRgb565(Color.red(color), Color.green(color), Color.blue(color))
            output[index * 2] = value.toByte()
            output[index * 2 + 1] = (value ushr 8).toByte()
        }
        return output
    }

    internal fun rgb888ToRgb565(red: Int, green: Int, blue: Int): Int =
        ((red and 0xf8) shl 8) or ((green and 0xfc) shl 3) or (blue ushr 3)
}
