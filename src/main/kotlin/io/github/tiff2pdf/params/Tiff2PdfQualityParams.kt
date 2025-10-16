package io.github.tiff2pdf.params

/**
 * Quality parameters for TIFF to PDF conversion.
 *
 * @param compression compression strategy
 * @param jpegQuality JPEG compression quality (0.2-1.0), only used when compression is JPEG or AUTO
 * @param targetDpi target DPI for resampling, null means use original TIFF DPI
 * @param colorHint color conversion hint
 */
data class Tiff2PdfQualityParams @JvmOverloads constructor(
    val compression: Compression = Compression.AUTO,
    val jpegQuality: Float = 0.8f,
    val targetDpi: Int? = null,
    val colorHint: ColorHint = ColorHint.AUTO
) {
    init {
        require(jpegQuality in 0.2f..1.0f) {
            "JPEG quality must be between 0.2 and 1.0, got $jpegQuality"
        }
        targetDpi?.let {
            require(it > 0) {
                "Target DPI must be positive, got $it"
            }
        }
    }
}
