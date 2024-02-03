package io.github.pdf2tiff

import io.github.pdf2tiff.params.SizeControlParams
import org.apache.pdfbox.io.IOUtils.toByteArray
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

    /**
     * Convert with size control
     *
     * @param sizeControl size control params
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    fun pdf2Tiff(sizeControl: SizeControlParams) {
        if (sizeControl.isFilePair()) {
            Files.newInputStream(Paths.get(sizeControl.sourceFile!!)).use { input ->
                Files.newOutputStream(Paths.get(sizeControl.destFile!!)).use { output ->
                    sizeControl.sourceInputStream = input
                    sizeControl.destOutputStream = output
                    convertWithSizeControl(sizeControl)
                }
            }
        } else if (sizeControl.isStreamPair()) {
            convertWithSizeControl(sizeControl)
        }
    }

    private fun convertWithSizeControl(sizeControl: SizeControlParams) {
        val byteArrayInputStream = ByteArrayInputStream(toByteArray(sizeControl.sourceInputStream))
        val byteArrayOutputStream = ByteArrayOutputStream()

        sizeControl.qualityParams.forEach {
            byteArrayInputStream.reset()
            byteArrayOutputStream.reset()

            pdf2Tiff(
                byteArrayInputStream, byteArrayOutputStream,
                it.dpi, it.compression, it.imgType
            )

            if (isSizeOk(byteArrayOutputStream, sizeControl)) return
        }

        log.info("last quality params still exceed the limit, keep the last one")
        byteArrayOutputStream.writeTo(sizeControl.destOutputStream)
    }

    private fun isSizeOk(
        byteArrayOutputStream: ByteArrayOutputStream,
        sizeControl: SizeControlParams
    ): Boolean {
        val actualSize = byteArrayOutputStream.size()
        val isWithinLimit = actualSize <= sizeControl.maxFileSize

        log.info(
            "Converted size: $actualSize, max file size: ${sizeControl.maxFileSize}, " +
                    if (isWithinLimit) "file size is within the limit, won't try next"
                    else "will try next quality params if any"
        )

        if (isWithinLimit) byteArrayOutputStream.writeTo(sizeControl.destOutputStream)

        return isWithinLimit
    }
}