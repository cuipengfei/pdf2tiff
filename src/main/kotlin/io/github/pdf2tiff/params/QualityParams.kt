package io.github.pdf2tiff.params

import org.apache.pdfbox.rendering.ImageType

data class QualityParams(
    val dpi: Int,
    val compression: String,
    val imgType: ImageType
)
