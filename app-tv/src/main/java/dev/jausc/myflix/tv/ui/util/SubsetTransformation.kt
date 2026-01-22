package dev.jausc.myflix.tv.ui.util

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Coil transformation that crops a rectangular subset from an image.
 * Used for extracting individual thumbnails from trickplay tile grids.
 *
 * @param x X offset in pixels (left edge of crop region)
 * @param y Y offset in pixels (top edge of crop region)
 * @param cropWidth Width of the crop region in pixels
 * @param cropHeight Height of the crop region in pixels
 */
class SubsetTransformation(
    private val x: Int,
    private val y: Int,
    private val cropWidth: Int,
    private val cropHeight: Int,
) : Transformation() {

    override val cacheKey: String = "SubsetTransformation($x,$y,$cropWidth,$cropHeight)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Clamp values to ensure we don't read outside the image bounds
        val safeX = x.coerceIn(0, (input.width - 1).coerceAtLeast(0))
        val safeY = y.coerceIn(0, (input.height - 1).coerceAtLeast(0))
        val safeWidth = cropWidth.coerceIn(1, input.width - safeX)
        val safeHeight = cropHeight.coerceIn(1, input.height - safeY)

        return Bitmap.createBitmap(input, safeX, safeY, safeWidth, safeHeight)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubsetTransformation) return false
        return x == other.x && y == other.y &&
            cropWidth == other.cropWidth && cropHeight == other.cropHeight
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + cropWidth
        result = 31 * result + cropHeight
        return result
    }
}
