package com.seeker.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Optional wheel background: images pushed over adb into the app's own external files dir, which
 * needs no storage permission.
 *   One image:  adb push bg.jpg /sdcard/Android/data/com.seeker.tv/files/background
 *               adb shell am broadcast -a com.seeker.tv.SET --ez background true    (false deletes it)
 *   Slideshow:  adb push photos/. /sdcard/Android/data/com.seeker.tv/files/backgrounds/
 *               adb shell am broadcast -a com.seeker.tv.SET --es slideshow 30       (seconds, or off)
 * Each image is decoded into a 1920x1080 RGB_565 bitmap, center-cropped, with the dim baked in, so
 * drawing it is one bitmap blit and the wheel still only redraws on input (and once per slide).
 * Slides are cached as ready-made 1920x1080 JPEGs, so after the first loop a slide change is one
 * small decode instead of a full-size photo.
 */
object Background {
    private const val TAG = "SeekerTV"
    private const val W = 1920
    private const val H = 1080
    private const val DIM = 0xC0   // black at 75% over the image keeps the dim dot text readable on light images
    private val IMAGE = Regex(".*\\.(jpe?g|png|webp)", RegexOption.IGNORE_CASE)

    fun file(ctx: Context): File? = ctx.getExternalFilesDir(null)?.let { File(it, "background") }

    fun clear(ctx: Context) { file(ctx)?.delete() }

    /** Slideshow images, sorted by name; empty if the folder is missing. */
    fun slides(ctx: Context): List<File> {
        val dir = ctx.getExternalFilesDir(null)?.let { File(it, "backgrounds") } ?: return emptyList()
        return dir.listFiles { f -> f.isFile && IMAGE.matches(f.name) }?.sortedBy { it.name } ?: emptyList()
    }

    /** A slide, from the cache when we've seen it before. Blocking; call off the main thread. */
    fun loadSlide(ctx: Context, f: File): Bitmap? {
        val c = cached(ctx, f)
        if (c.isFile) {
            val b = BitmapFactory.decodeFile(c.path, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            if (b != null && b.width == W && b.height == H) return b
        }
        val b = load(f) ?: return null
        try {
            c.parentFile?.mkdirs()
            val tmp = File(c.path + ".tmp")
            FileOutputStream(tmp).use { b.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            tmp.renameTo(c)
        } catch (e: Exception) {
            Log.w(TAG, "slide cache write failed (${f.name}): $e")
        }
        return b
    }

    /** Drops cached slides whose photo was removed or replaced. */
    fun pruneCache(ctx: Context, slides: List<File>) {
        val keep = slides.map { cached(ctx, it).name }.toSet()
        File(ctx.cacheDir, "slides").listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    // name + size + mtime: a replaced photo gets a new entry; DIM too, so a new dim re-renders
    private fun cached(ctx: Context, f: File) =
        File(File(ctx.cacheDir, "slides"), "${f.name}-${f.length()}-${f.lastModified()}-$DIM.jpg")

    /** The single background image. Blocking decode; call off the main thread. */
    fun load(ctx: Context): Bitmap? = file(ctx)?.let { load(it) }

    /** Blocking decode; call off the main thread. Null if the file is missing or can't be read. */
    fun load(f: File): Bitmap? {
        if (!f.isFile) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val rot = rotation(f)
            // after rotation the long side may swap; sample against the rotated size
            val sw = if (rot % 180 == 0) bounds.outWidth else bounds.outHeight
            val sh = if (rot % 180 == 0) bounds.outHeight else bounds.outWidth
            var sample = 1
            while (sw / (sample * 2) >= W && sh / (sample * 2) >= H) sample *= 2
            var src = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565  // half the peak memory of a 12 MP photo
            }) ?: return null
            if (rot != 0) {
                val r = Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
                if (r !== src) src.recycle()
                src = r
            }
            // center-crop to 16:9
            val scale = max(W.toFloat() / src.width, H.toFloat() / src.height)
            val cw = (W / scale).toInt()
            val ch = (H / scale).toInt()
            val cx = (src.width - cw) / 2
            val cy = (src.height - ch) / 2
            val out = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565)
            val c = Canvas(out)
            c.drawBitmap(src, Rect(cx, cy, cx + cw, cy + ch), Rect(0, 0, W, H), Paint(Paint.FILTER_BITMAP_FLAG))
            c.drawColor(DIM shl 24)
            src.recycle()
            out
        } catch (e: Exception) {
            Log.w(TAG, "background load failed (${f.name}): $e")
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "background too big (${f.name}): $e")
            null
        }
    }

    /** Phone photos are stored sideways with an EXIF orientation tag; flips/transposes are ignored. */
    private fun rotation(f: File): Int = try {
        when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) { 0 }
}
