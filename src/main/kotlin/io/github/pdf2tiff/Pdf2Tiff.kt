package io.github.pdf2tiff

import io.github.pdf2tiff.params.SizeControlParams
import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.rendering.ImageType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
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
     * @param imgType image type, such as "RGB", "BGR", "GRAY"
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
        log.info("PDF input stream to tiff output stream, dpi: $dpi, compression: $compression, image type: $imgType")

        val bufferedImages = Pdf2Images(dpi, imgType).pdf2BufferedImages(input)
        Images2Tiff(compression).bufferedImages2TiffOutputStream(bufferedImages, output)
    }

    /**
     * Convert from file to file, this lib will close them after conversion.
     *
     * @param pdfPath pdf file path
     * @param tiffPath tiff output file path
     * @param dpi pdf to image quality
     * @param compression tiff compression
     * @param imgType image type, such as "RGB", "BGR", "GRAY"
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
        Files.newInputStream(Paths.get(pdfPath)).use { input ->
            Files.newOutputStream(Paths.get(tiffPath)).use { output ->
                pdf2Tiff(input, output, dpi, compression, imgType)
            }
        }
    }

    fun pdf2Tiff(sizeControlParams: SizeControlParams) {
        if (sizeControlParams.isFilePair()) {
            Files.newInputStream(Paths.get(sizeControlParams.sourceFile!!)).use { input ->
                Files.newOutputStream(Paths.get(sizeControlParams.destFile!!)).use { output ->
                    sizeControlParams.sourceInputStream = input
                    sizeControlParams.destOutputStream = output
                    convertWithSizeControl(sizeControlParams)
                }
            }
        } else if (sizeControlParams.isStreamPair()) {
            convertWithSizeControl(sizeControlParams)
        }
    }

    private fun convertWithSizeControl(sizeControlParams: SizeControlParams) {
        val byteArrayInputStream = ByteArrayInputStream(IOUtils.toByteArray(sizeControlParams.sourceInputStream))
        val byteArrayOutputStream = ByteArrayOutputStream()

        sizeControlParams.qualityParams.forEach {
            byteArrayInputStream.reset()
            byteArrayOutputStream.reset()

            pdf2Tiff(
                byteArrayInputStream, byteArrayOutputStream,
                it.dpi, it.compression, it.imgType
            )

            if (isSizeOk(byteArrayOutputStream, sizeControlParams)) return
        }

        log.info("last quality params still exceed the limit, use the last one")
        byteArrayOutputStream.writeTo(sizeControlParams.destOutputStream)
    }

    private fun isSizeOk(
        byteArrayOutputStream: ByteArrayOutputStream,
        sizeControlParams: SizeControlParams
    ): Boolean {
        val size = byteArrayOutputStream.size()
        log.info("Converted file size: $size, max file size: ${sizeControlParams.maxFileSize}")

        if (size <= sizeControlParams.maxFileSize) {
            log.info("file size is within the limit, won't try next")
            byteArrayOutputStream.writeTo(sizeControlParams.destOutputStream)
            return true
        } else {
            log.info("will try next quality params if any")
            return false
        }
    }
}