package tv.roam.client.feature.common

import com.google.zxing.PlanarYUVLuminanceSource
import java.nio.ByteBuffer

/**
 * Builds a ZXing luminance source from a Camera Y plane (or any equivalent
 * row-padded byte buffer). Pure byte handling with no Android UI dependency.
 */
internal fun createYPlaneLuminanceSource(
    buffer: ByteBuffer,
    width: Int,
    height: Int,
    rowStride: Int,
): PlanarYUVLuminanceSource {
    val bufferCopy = buffer.duplicate()
    val bytes = ByteArray(bufferCopy.remaining())
    bufferCopy.get(bytes)
    // Camera HALs may pad each Y row. ZXing advances rows by dataWidth, so the
    // reported row stride must be used instead of the visible image width.
    return PlanarYUVLuminanceSource(
        bytes,
        rowStride,
        height,
        0,
        0,
        width,
        height,
        false,
    )
}
