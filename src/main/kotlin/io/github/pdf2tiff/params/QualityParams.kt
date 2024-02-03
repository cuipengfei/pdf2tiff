package io.github.pdf2tiff.params

import org.apache.pdfbox.rendering.ImageType

/**
 * Quality parameters for pdf to tiff conversion.
 *
 * @param dpi pdf to image quality
 * @param compression tiff compression
 * @param imgType image type, such as "RGB", "BGR", "GRAY"
 */
data class QualityParams(
    val dpi: Int,
    val compression: String,
    val imgType: ImageType
)
