package io.github.tiff2pdf.params

/**
 * Color conversion hint for TIFF to PDF conversion.
 */
enum class ColorHint {
    /**
     * Automatic color mode selection based on image type
     */
    AUTO,

    /**
     * Convert to RGB
     */
    RGB,

    /**
     * Convert to grayscale
     */
    GRAY,

    /**
     * Convert to binary (black and white)
     */
    BINARY
}
