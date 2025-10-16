package io.github.tiff2pdf

import io.github.tiff2pdf.params.ColorHint
import io.github.tiff2pdf.params.Compression
import io.github.tiff2pdf.params.Tiff2PdfQualityParams
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.OutputStream

/**
 * Converts BufferedImage sequences to PDF documents.
 */
object Images2Pdf {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Writes a list of page images to a PDF output stream.
     *
     * @param pages list of page images with DPI metadata
     * @param output PDF output stream
     * @param quality quality parameters
     * @throws java.io.IOException if an I/O error occurs
     */
    @JvmStatic
    @Throws(IOException::class)
    fun write(pages: List<PageImage>, output: OutputStream, quality: Tiff2PdfQualityParams) {
        if (pages.isEmpty()) {
            throw IOException("No pages to convert")
        }

        PDDocument().use { doc ->
            pages.forEachIndexed { index, pageImage ->
                log.debug("Processing page $index: ${pageImage.image.width}x${pageImage.image.height}")
                addPageToDocument(doc, pageImage, quality)
            }

            doc.save(output)
            log.info("Saved PDF with ${pages.size} pages")
        }
    }

    /**
     * Adds a single page image to the PDF document.
     */
    private fun addPageToDocument(doc: PDDocument, pageImage: PageImage, quality: Tiff2PdfQualityParams) {
        var image = pageImage.image

        // Apply orientation transformation if needed
        image = applyOrientation(image, pageImage.orientation)

        // Apply color conversion if needed
        image = applyColorHint(image, quality.colorHint)

        // Calculate effective DPI
        val effectiveDpiX = quality.targetDpi?.toFloat() ?: pageImage.dpiX
        val effectiveDpiY = quality.targetDpi?.toFloat() ?: pageImage.dpiY

        // Apply DPI resampling if targetDpi is lower than original
        if (quality.targetDpi != null) {
            val scaleFactor = quality.targetDpi.toFloat() / maxOf(pageImage.dpiX, pageImage.dpiY)
            if (scaleFactor < 1.0f) {
                image = resampleImage(image, scaleFactor)
            }
        }

        // Calculate page size in PDF points (72 points per inch)
        val widthPoints = image.width * 72f / effectiveDpiX
        val heightPoints = image.height * 72f / effectiveDpiY

        // Create page with calculated dimensions
        val page = PDPage(PDRectangle(widthPoints, heightPoints))
        doc.addPage(page)

        // Embed image in PDF with configured compression
        val pdImage = createPDImageWithCompression(doc, image, quality)

        // Draw image on page
        PDPageContentStream(doc, page).use { contentStream ->
            contentStream.drawImage(pdImage, 0f, 0f, widthPoints, heightPoints)
        }
    }

    /**
     * Creates a PDImageXObject from BufferedImage using the specified compression strategy.
     *
     * Compression strategies:
     * - JPEG: Lossy compression with configurable quality (requires alpha removal)
     * - LOSSLESS: No quality loss, larger file size (Flate/Deflate)
     * - CCITT: For binary images (currently uses Lossless as placeholder, CCITT G4 planned)
     * - AUTO: Intelligently chooses based on image type (binary→Lossless, others→JPEG with fallback)
     *
     * @param doc PDF document to attach the image to
     * @param image source BufferedImage
     * @param quality compression and quality parameters
     * @return PDImageXObject ready to be drawn on a PDF page
     */
    private fun createPDImageWithCompression(
        doc: PDDocument,
        image: BufferedImage,
        quality: Tiff2PdfQualityParams
    ): PDImageXObject {
        return when (quality.compression) {
            Compression.JPEG -> {
                val rgbImage = removeAlpha(image)
                JPEGFactory.createFromImage(doc, rgbImage, quality.jpegQuality)
            }

            Compression.LOSSLESS -> {
                LosslessFactory.createFromImage(doc, image)
            }

            Compression.CCITT -> {
                // For MVP, use lossless for binary images
                // Future: implement true CCITT G4 encoding
                if (isBinaryImage(image)) {
                    LosslessFactory.createFromImage(doc, convertImageType(image, BufferedImage.TYPE_BYTE_BINARY))
                } else {
                    log.warn("CCITT compression requested for non-binary image, using lossless")
                    LosslessFactory.createFromImage(doc, image)
                }
            }

            Compression.AUTO -> {
                if (isBinaryImage(image)) {
                    // Binary images: use lossless (future: CCITT G4)
                    LosslessFactory.createFromImage(doc, convertImageType(image, BufferedImage.TYPE_BYTE_BINARY))
                } else {
                    // Try JPEG, fallback to lossless if it fails
                    try {
                        val rgbImage = removeAlpha(image)
                        JPEGFactory.createFromImage(doc, rgbImage, quality.jpegQuality)
                    } catch (e: Exception) {
                        log.warn("JPEG compression failed (${e.javaClass.simpleName}: ${e.message}), falling back to lossless")
                        log.debug("Full exception trace for JPEG compression failure", e)
                        LosslessFactory.createFromImage(doc, image)
                    }
                }
            }
        }
    }

    /**
     * Removes alpha channel from image (required for JPEG compression).
     * JPEG does not support transparency, so we composite onto white background.
     */
    private fun removeAlpha(image: BufferedImage): BufferedImage {
        if (image.type == BufferedImage.TYPE_INT_RGB ||
            image.type == BufferedImage.TYPE_BYTE_GRAY
        ) {
            return image
        }

        val rgbImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val g = rgbImage.createGraphics()
        try {
            // Fill with white background
            g.color = Color.WHITE
            g.fillRect(0, 0, image.width, image.height)
            // Draw original image on top
            g.drawImage(image, 0, 0, null)
        } finally {
            g.dispose()
        }
        return rgbImage
    }

    /**
     * Applies color conversion hint to the image.
     */
    private fun applyColorHint(image: BufferedImage, hint: ColorHint): BufferedImage {
        return when (hint) {
            ColorHint.RGB -> convertImageType(image, BufferedImage.TYPE_INT_RGB)
            ColorHint.GRAY -> convertImageType(image, BufferedImage.TYPE_BYTE_GRAY)
            ColorHint.BINARY -> convertImageType(image, BufferedImage.TYPE_BYTE_BINARY)
            ColorHint.AUTO -> image // No conversion
        }
    }

    /**
     * Applies TIFF orientation transformation to the image.
     *
     * TIFF Orientation values (TIFF 6.0 specification):
     * - 1: Normal (0° rotation)
     * - 2: Flipped horizontally
     * - 3: Rotated 180°
     * - 4: Flipped vertically
     * - 5: Rotated 90° CCW + flipped horizontally
     * - 6: Rotated 90° CW
     * - 7: Rotated 90° CW + flipped horizontally
     * - 8: Rotated 90° CCW
     *
     * @param image source image
     * @param orientation TIFF orientation tag value
     * @return transformed image (or original if orientation == 1)
     */
    private fun applyOrientation(image: BufferedImage, orientation: Int): BufferedImage {
        if (orientation == 1) return image // Normal orientation

        val transform = AffineTransform()
        val width = image.width
        val height = image.height
        var newWidth = width
        var newHeight = height

        when (orientation) {
            2 -> {
                // Flip horizontally
                transform.scale(-1.0, 1.0)
                transform.translate(-width.toDouble(), 0.0)
            }
            3 -> {
                // Rotate 180°
                transform.translate(width.toDouble(), height.toDouble())
                transform.rotate(Math.PI)
            }
            4 -> {
                // Flip vertically
                transform.scale(1.0, -1.0)
                transform.translate(0.0, -height.toDouble())
            }
            5 -> {
                // Rotate 90° CCW + flip horizontally
                newWidth = height
                newHeight = width
                transform.rotate(-Math.PI / 2)
                transform.scale(-1.0, 1.0)
                transform.translate(-height.toDouble(), -width.toDouble())
            }
            6 -> {
                // Rotate 90° CW
                newWidth = height
                newHeight = width
                transform.translate(height.toDouble(), 0.0)
                transform.rotate(Math.PI / 2)
            }
            7 -> {
                // Rotate 90° CW + flip horizontally
                newWidth = height
                newHeight = width
                transform.scale(-1.0, 1.0)
                transform.translate(-height.toDouble(), 0.0)
                transform.rotate(Math.PI / 2)
            }
            8 -> {
                // Rotate 90° CCW
                newWidth = height
                newHeight = width
                transform.translate(0.0, width.toDouble())
                transform.rotate(-Math.PI / 2)
            }
            else -> {
                log.warn("Unknown TIFF orientation value: $orientation, treating as normal")
                return image
            }
        }

        val rotated = BufferedImage(newWidth, newHeight, image.type)
        val g = rotated.createGraphics()
        try {
            g.drawImage(image, transform, null)
        } finally {
            g.dispose()
        }
        return rotated
    }

    /**
     * Resamples the image by the given scale factor using bilinear interpolation.
     *
     * @param image source image
     * @param scaleFactor scale factor (< 1.0 for downsampling)
     * @return resampled image
     */
    private fun resampleImage(image: BufferedImage, scaleFactor: Float): BufferedImage {
        val newWidth = (image.width * scaleFactor).toInt().coerceAtLeast(1)
        val newHeight = (image.height * scaleFactor).toInt().coerceAtLeast(1)

        val resampled = BufferedImage(newWidth, newHeight, image.type)
        val g = resampled.createGraphics()
        try {
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
            )
            g.drawImage(image, 0, 0, newWidth, newHeight, null)
        } finally {
            g.dispose()
        }

        log.debug("Resampled image from ${image.width}x${image.height} to ${newWidth}x${newHeight} (scale: $scaleFactor)")
        return resampled
    }

    /**
     * Converts image to the specified BufferedImage type.
     *
     * If the image is already the target type, returns it unchanged.
     * Otherwise, creates a new BufferedImage of the target type and renders the original image onto it.
     *
     * @param image source image
     * @param targetType target BufferedImage type constant (e.g., TYPE_INT_RGB, TYPE_BYTE_GRAY)
     * @return converted image (or original if already correct type)
     */
    private fun convertImageType(image: BufferedImage, targetType: Int): BufferedImage {
        if (image.type == targetType) return image

        val converted = BufferedImage(image.width, image.height, targetType)
        val g = converted.createGraphics()
        try {
            g.drawImage(image, 0, 0, null)
        } finally {
            g.dispose()
        }
        return converted
    }

    /**
     * Checks if an image is binary (1-bit black and white).
     */
    private fun isBinaryImage(image: BufferedImage): Boolean {
        return image.type == BufferedImage.TYPE_BYTE_BINARY ||
                image.colorModel.pixelSize == 1
    }
}