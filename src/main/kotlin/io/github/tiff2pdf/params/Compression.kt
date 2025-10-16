package io.github.tiff2pdf.params

/**
 * Compression strategy for TIFF to PDF conversion.
 */
enum class Compression {
    /**
     * Automatic compression selection:
     * - Binary images: Lossless (CCITT G4 planned for future)
     * - Others: JPEG (with fallback to Lossless if needed)
     */
    AUTO,

    /**
     * JPEG compression with configurable quality
     */
    JPEG,

    /**
     * Lossless compression (Flate/Deflate)
     */
    LOSSLESS,

    /**
     * CCITT Group 4 compression (for binary images)
     * Note: Currently uses Lossless as placeholder. True CCITT G4 implementation planned.
     */
    CCITT
}
