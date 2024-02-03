package io.github.pdf2tiff

import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer.createBufferFromStream
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream

/**
 * PDF to images
 *
 * @param dpi pdf to image quality
 * @param imgType image type, such as "RGB", "BGR", "GRAY"
 */
class Pdf2Images(private val dpi: Int, private val imgType: ImageType) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * PDF input stream to a list of buffered images
     *
     * @param pdfInputStream pdf input stream
     * @return a list of buffered images
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun pdf2BufferedImages(pdfInputStream: InputStream): List<BufferedImage> {
        val images = ArrayList<BufferedImage>()

        Loader.loadPDF(createBufferFromStream(pdfInputStream)).use { pdf ->
            val renderer = PDFRenderer(pdf)

            val numberOfPages = pdf.numberOfPages
            for (i in 0 until numberOfPages) {
                val bufferedImage = renderer.renderImageWithDPI(i, dpi.toFloat(), imgType)
                images.add(bufferedImage)
            }
            log.info("Taken $numberOfPages pages of images from PDF")
        }

        return images
    }
}