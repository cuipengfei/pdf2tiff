package io.github.tiff2pdf

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadata

/**
 * Reads multi-page TIFF files and extracts page images with DPI metadata.
 */
object Tiff2Images {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    // TIFF Field Tag Numbers (TIFF 6.0 Specification)
    private const val TIFF_TAG_X_RESOLUTION = 282  // Horizontal resolution in pixels per ResolutionUnit
    private const val TIFF_TAG_Y_RESOLUTION = 283  // Vertical resolution in pixels per ResolutionUnit
    private const val TIFF_TAG_RESOLUTION_UNIT = 296  // Unit of measurement: 2=inch, 3=centimeter
    private const val TIFF_TAG_ORIENTATION = 274  // Image orientation (1=normal, 2-8=rotated/flipped)

    // Resolution Unit Values (TIFF ResolutionUnit tag)
    private const val RESOLUTION_UNIT_INCH = 2  // Pixels per inch (DPI)
    private const val RESOLUTION_UNIT_CENTIMETER = 3  // Pixels per centimeter

    // Conversion Factors
    private const val CM_TO_INCH = 2.54f  // 1 inch = 2.54 centimeters
    private const val MM_TO_INCH = 25.4f  // 1 inch = 25.4 millimeters

    // Default DPI when metadata is missing
    private const val DEFAULT_DPI = 72.0f  // Standard screen resolution

    /**
     * Reads a multi-page TIFF from input stream and returns a list of page images.
     *
     * @param input TIFF input stream
     * @return list of page images with DPI metadata
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    @Throws(IOException::class)
    fun read(input: InputStream): List<PageImage> {
        val pages: MutableList<PageImage> = mutableListOf<PageImage>()

        val readers = ImageIO.getImageReadersByFormatName("TIFF")
        if (!readers.hasNext()) {
            throw IOException("No TIFF image reader found. Ensure TwelveMonkeys imageio-tiff is on the classpath.")
        }

        val reader = readers.next()
        try {
            ImageIO.createImageInputStream(input).use { imageInputStream ->
                reader.input = imageInputStream

                val numPages = reader.getNumImages(true)
                log.info("Reading $numPages pages from TIFF")

                for (i in 0 until numPages) {
                    val image: BufferedImage = reader.read(i)
                    val metadata: IIOMetadata = reader.getImageMetadata(i)

                    val (dpiX, dpiY) = extractDpi(metadata)
                    val orientation = extractOrientation(metadata)

                    pages.add(PageImage(image, dpiX, dpiY, orientation))
                    log.debug("Page $i: ${image.width}x${image.height}, DPI: $dpiX x $dpiY, orientation: $orientation")
                }
            }
        } finally {
            reader.dispose()
        }

        return pages
    }

    /**
     * Extracts DPI from TIFF metadata.
     * Tries TIFF native tree first, then falls back to standard ImageIO tree.
     *
     * @param metadata image metadata
     * @return pair of (dpiX, dpiY), defaults to 72.0 if not found
     */
    private fun extractDpi(metadata: IIOMetadata): Pair<Float, Float> {
        // Try TIFF native format first
        val tiffDpi = extractDpiFromTiffTree(metadata)
        if (tiffDpi != null) return tiffDpi

        // Fallback to standard ImageIO metadata
        val standardDpi = extractDpiFromStandardTree(metadata)
        if (standardDpi != null) return standardDpi

        // Default to standard screen resolution if not found
        log.warn("No DPI information found in TIFF metadata, defaulting to $DEFAULT_DPI DPI")
        return DEFAULT_DPI to DEFAULT_DPI
    }

    /**
     * Extracts DPI from TIFF native metadata tree.
     *
     * Reads TIFF IFD (Image File Directory) tags directly:
     * - Tag 282: XResolution (horizontal pixels per unit)
     * - Tag 283: YResolution (vertical pixels per unit)
     * - Tag 296: ResolutionUnit (2=inch, 3=centimeter)
     *
     * @param metadata TIFF image metadata
     * @return pair of (dpiX, dpiY) or null if fields not found
     */
    private fun extractDpiFromTiffTree(metadata: IIOMetadata): Pair<Float, Float>? {
        val nativeNames = metadata.metadataFormatNames
        val tiffFormat = nativeNames.find { it.contains("tiff", ignoreCase = true) }
            ?: return null

        try {
            val root = metadata.getAsTree(tiffFormat)
            var xRes: Float? = null
            var yRes: Float? = null
            var resUnit = RESOLUTION_UNIT_INCH // Default to inches

            // Find TIFF IFD (Image File Directory) and read field tags
            val ifd = findNode(root, "TIFFIFD") ?: root
            val fields = getChildNodes(ifd)

            for (field in fields) {
                val number = field.attributes?.getNamedItem("number")?.nodeValue?.toIntOrNull()
                when (number) {
                    TIFF_TAG_X_RESOLUTION -> xRes = parseTiffRational(field)
                    TIFF_TAG_Y_RESOLUTION -> yRes = parseTiffRational(field)
                    TIFF_TAG_RESOLUTION_UNIT -> resUnit = parseTiffShort(field) ?: RESOLUTION_UNIT_INCH
                }
            }

            if (xRes != null && yRes != null) {
                // Convert to DPI (pixels per inch) if needed
                val factor = when (resUnit) {
                    RESOLUTION_UNIT_CENTIMETER -> CM_TO_INCH // cm to inches
                    else -> 1.0f // already in inches or unknown unit
                }
                return (xRes * factor) to (yRes * factor)
            }
        } catch (e: Exception) {
            log.debug("Failed to extract DPI from TIFF native tree: ${e.message}")
        }

        return null
    }

    /**
     * Extracts DPI from standard ImageIO metadata tree.
     *
     * Reads from javax_imageio_1.0 standard tree:
     * - HorizontalPixelSize: millimeters per pixel (horizontal)
     * - VerticalPixelSize: millimeters per pixel (vertical)
     *
     * Converts to DPI using: DPI = 25.4 mm/inch ÷ mm/pixel
     *
     * @param metadata image metadata
     * @return pair of (dpiX, dpiY) or null if not found
     */
    private fun extractDpiFromStandardTree(metadata: IIOMetadata): Pair<Float, Float>? {
        try {
            val root = metadata.getAsTree("javax_imageio_1.0")
            val dimension = findNode(root, "Dimension") ?: return null

            val hPixelSize = findNode(dimension, "HorizontalPixelSize")
                ?.attributes?.getNamedItem("value")?.nodeValue?.toFloatOrNull()
            val vPixelSize = findNode(dimension, "VerticalPixelSize")
                ?.attributes?.getNamedItem("value")?.nodeValue?.toFloatOrNull()

            if (hPixelSize != null && vPixelSize != null && hPixelSize > 0 && vPixelSize > 0) {
                // PixelSize is in mm/pixel, convert to DPI: DPI = 25.4 mm/inch / (mm/pixel)
                val dpiX = MM_TO_INCH / hPixelSize
                val dpiY = MM_TO_INCH / vPixelSize
                return dpiX to dpiY
            }
        } catch (e: Exception) {
            log.debug("Failed to extract DPI from standard tree: ${e.message}")
        }

        return null
    }

    /**
     * Extracts TIFF orientation tag (tag 274).
     *
     * Orientation values (TIFF 6.0 spec):
     * - 1: Normal (0° rotation)
     * - 2-8: Various rotations and flips
     *
     * @param metadata image metadata
     * @return orientation value (default 1 if not found)
     */
    private fun extractOrientation(metadata: IIOMetadata): Int {
        val nativeNames = metadata.metadataFormatNames
        val tiffFormat = nativeNames.find { it.contains("tiff", ignoreCase = true) }
            ?: return 1

        try {
            val root = metadata.getAsTree(tiffFormat)
            val ifd = findNode(root, "TIFFIFD") ?: root
            val fields = getChildNodes(ifd)

            for (field in fields) {
                val number = field.attributes?.getNamedItem("number")?.nodeValue?.toIntOrNull()
                if (number == TIFF_TAG_ORIENTATION) {
                    return parseTiffShort(field) ?: 1
                }
            }
        } catch (e: Exception) {
            log.debug("Failed to extract orientation: ${e.message}")
        }

        return 1
    }

    /**
     * Parses a TIFF Rational field (numerator/denominator).
     */
    private fun parseTiffRational(field: Node): Float? {
        val rationals = findNode(field, "TIFFRationals") ?: return null
        val rational = findNode(rationals, "TIFFRational") ?: return null

        val rawValue = rational.attributes?.getNamedItem("value")?.nodeValue ?: return null

        // Some implementations store as "numerator/denominator" string
        return if (rawValue.contains("/")) {
            val parts = rawValue.split("/", limit = 2)
            if (parts.size == 2) {
                val num = parts[0].toFloatOrNull() ?: return null
                val den = parts[1].toFloatOrNull() ?: return null
                if (den != 0f) num / den else null
            } else null
        } else {
            rawValue.toFloatOrNull()
        }
    }

    /**
     * Parses a TIFF Short field.
     */
    private fun parseTiffShort(field: Node): Int? {
        val shorts = findNode(field, "TIFFShorts") ?: return null
        val short = findNode(shorts, "TIFFShort") ?: return null
        return short.attributes?.getNamedItem("value")?.nodeValue?.toIntOrNull()
    }

    /**
     * Finds a child node by name.
     */
    private fun findNode(parent: Node, name: String): Node? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeName == name) return child
        }
        return null
    }

    /**
     * Gets all child nodes as a list.
     */
    private fun getChildNodes(parent: Node): List<Node> {
        val result = mutableListOf<Node>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            result.add(children.item(i))
        }
        return result
    }
}
