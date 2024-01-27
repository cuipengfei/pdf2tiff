package com.github.cuipengfei

import org.apache.pdfbox.rendering.ImageType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

object Pdf2Tiff {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    private const val DEFAULT_DPI = 300
    private const val DEFAULT_COMPRESSION = "Deflate"
    private val DEFAULT_IMAGE_TYPE = ImageType.RGB

    /**
     * Convert from stream to stream, you need to close them after use.
     *
     * @param input pdf input stream
     * @param output tiff output stream
     * @param dpi pdf to image quality
     * @param compression tiff compression
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    fun pdf2Tiff(
        input: InputStream,
        output: OutputStream,
        dpi: Int = DEFAULT_DPI,
        compression: String = DEFAULT_COMPRESSION,
        imgType: ImageType = DEFAULT_IMAGE_TYPE
    ) {
        val pdf2Images = Pdf2BufferedImages(dpi, imgType)
        val images2Tiff = BufferedImages2Tiff()

        images2Tiff.bufferedImages2TiffOutputStream(pdf2Images.pdf2BufferedImages(input), output, compression)
    }

    /**
     * Convert from file to file, this lib will close them after conversion.
     *
     * @param pdfPath pdf file path
     * @param tiffPath tiff output file path
     * @param dpi pdf to image quality
     * @param compression tiff compression
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    fun pdf2Tiff(
        pdfPath: String,
        tiffPath: String,
        dpi: Int = DEFAULT_DPI,
        compression: String = DEFAULT_COMPRESSION,
        imgType: ImageType = DEFAULT_IMAGE_TYPE
    ) {
        log.info("PDF to tiff, pdf path: $pdfPath, tiff path: $tiffPath, dpi: $dpi, compression: $compression")

        Files.newInputStream(Paths.get(pdfPath)).use { input ->
            Files.newOutputStream(Paths.get(tiffPath)).use { output ->
                pdf2Tiff(input, output, dpi, compression, imgType)
            }
        }
    }
}