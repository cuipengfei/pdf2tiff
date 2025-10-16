package io.github.tiff2pdf

import java.awt.image.BufferedImage

/**
 * Represents a single page from a TIFF file with its metadata.
 *
 * @param image the buffered image for this page
 * @param dpiX horizontal DPI (dots per inch)
 * @param dpiY vertical DPI (dots per inch)
 * @param orientation TIFF orientation tag value (1-8), default is 1 (normal)
 */
data class PageImage(
    val image: BufferedImage,
    val dpiX: Float,
    val dpiY: Float,
    val orientation: Int = 1
)