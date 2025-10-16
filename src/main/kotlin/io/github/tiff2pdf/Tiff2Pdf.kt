package io.github.tiff2pdf

import io.github.tiff2pdf.params.Tiff2PdfQualityParams
import io.github.tiff2pdf.params.Tiff2PdfSizeControlParams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*

/**
 * TIFF to PDF converter.
 *
 * Provides methods to convert multi-page TIFF files to PDF documents with configurable quality
 * and size control parameters.
 */
object Tiff2Pdf {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Converts TIFF input stream to PDF output stream with specified quality parameters.
     *
     * @param input TIFF input stream
     * @param output PDF output stream
     * @param quality quality parameters (default: AUTO compression, 0.8 JPEG quality)
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun tiff2Pdf(
        input: InputStream,
        output: OutputStream,
        quality: Tiff2PdfQualityParams = Tiff2PdfQualityParams()
    ) {
        log.info("Converting TIFF to PDF with quality parameters: $quality")

        // Read all pages from TIFF
        val pages: List<PageImage> = Tiff2Images.read(input)
        log.info("Read ${pages.size} pages from TIFF")

        // Write pages to PDF
        Images2Pdf.write(pages, output, quality)
        log.info("Conversion completed successfully")
    }

    /**
     * Converts TIFF file to PDF file with specified quality parameters.
     *
     * @param tiffPath source TIFF file path
     * @param pdfPath destination PDF file path
     * @param quality quality parameters (default: AUTO compression, 0.8 JPEG quality)
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun tiff2Pdf(
        tiffPath: String,
        pdfPath: String,
        quality: Tiff2PdfQualityParams = Tiff2PdfQualityParams()
    ) {
        log.info("Converting TIFF file '$tiffPath' to PDF file '$pdfPath'")

        FileInputStream(tiffPath).use { input ->
            FileOutputStream(pdfPath).use { output ->
                tiff2Pdf(input, output, quality)
            }
        }
    }

    /**
     * Converts TIFF to PDF with size control.
     *
     * Tries multiple quality parameters in order until the output size is within the maximum file size.
     * If all parameters exceed the limit, uses the last (lowest quality) parameter.
     *
     * @param sizeControl size control parameters
     * @throws IOException if an I/O error occurs
     */
    @JvmStatic
    @Throws(IOException::class)
    fun tiff2Pdf(sizeControl: Tiff2PdfSizeControlParams) {
        log.info("Converting TIFF to PDF with size control: max size = ${sizeControl.maxFileSize} bytes")

        // Read all pages from TIFF (reuse existing read logic)
        val pages: List<PageImage> = if (sizeControl.isFilePair()) {
            FileInputStream(sizeControl.sourceFile!!).use { input ->
                Tiff2Images.read(input)
            }
        } else {
            Tiff2Images.read(sizeControl.sourceInputStream!!)
        }

        log.info("Read ${pages.size} pages from TIFF, trying ${sizeControl.qualityParams.size} quality settings")

        // Try each quality parameter in order
        var lastOutput: ByteArrayOutputStream? = null

        for ((index, qualityParam) in sizeControl.qualityParams.withIndex()) {
            log.info("Attempt ${index + 1}/${sizeControl.qualityParams.size} with quality: $qualityParam")

            try {
                // Generate PDF with this quality setting using base conversion logic
                val baos = ByteArrayOutputStream()
                Images2Pdf.write(pages, baos, qualityParam)

                val size = baos.size()
                log.info("Generated PDF size: $size bytes (limit: ${sizeControl.maxFileSize})")

                if (size <= sizeControl.maxFileSize) {
                    log.info("Size is within limit, using this quality setting")
                    writeOutput(baos, sizeControl)
                    return
                }

                lastOutput = baos
            } catch (e: Exception) {
                log.warn("Failed to generate PDF with quality param ${index + 1}: ${e.message}")
                log.warn("Exception details", e)
                // Continue trying next quality parameter
            }
        }

        // All attempts exceeded the limit or failed, use the last successful output
        if (lastOutput != null) {
            log.warn("All quality settings exceeded size limit, using lowest quality output (${lastOutput.size()} bytes)")
            writeOutput(lastOutput, sizeControl)
        } else {
            throw IOException("Failed to generate PDF with all quality parameters")
        }
    }

    /**
     * Writes the output to the destination stream or file.
     */
    private fun writeOutput(baos: ByteArrayOutputStream, sizeControl: Tiff2PdfSizeControlParams) {
        if (sizeControl.isFilePair()) {
            FileOutputStream(sizeControl.destFile!!).use { output ->
                baos.writeTo(output)
            }
            log.info("PDF written to file: ${sizeControl.destFile}")
        } else {
            baos.writeTo(sizeControl.destOutputStream!!)
            log.info("PDF written to output stream")
        }
    }
}
