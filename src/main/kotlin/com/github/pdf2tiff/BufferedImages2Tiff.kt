package com.github.pdf2tiff

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

class BufferedImages2Tiff {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Convert from a list of buffered images to tiff output stream
     *
     * @param bufferedImages a list of buffered images
     * @param output         tiff output stream
     * @param compression    compression type, such as "LZW", "JPEG", "PackBits", "Deflate"
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    fun bufferedImages2TiffOutputStream(
        bufferedImages: List<BufferedImage>,
        output: OutputStream,
        compression: String
    ) {
        val tiffWriterOptional = findTiffWriter()

        if (tiffWriterOptional.isPresent) {
            ImageIO.createImageOutputStream(output).use { imageOutputStream ->
                val tiffWriter = tiffWriterOptional.get()
                val writeParam = configWriteParam(tiffWriter, compression)

                tiffWriter.output = imageOutputStream
                tiffWriter.prepareWriteSequence(null)

                for (bufferedImage in bufferedImages) {
                    tiffWriter.writeToSequence(IIOImage(bufferedImage, null, null), writeParam)
                }

                tiffWriter.endWriteSequence()
                tiffWriter.dispose()
            }

            log.info("Buffered images to tiff output stream, number of pages: ${bufferedImages.size}")
        } else {
            throw ClassNotFoundException("Can not find tiff writer class")
        }
    }

    private fun configWriteParam(tiffWriter: ImageWriter, compression: String): ImageWriteParam {
        val writeParam = tiffWriter.defaultWriteParam
        writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
        writeParam.compressionType = compression
        return writeParam
    }

    private fun findTiffWriter(): Optional<ImageWriter> {
        val tiffWriters = ImageIO.getImageWritersByFormatName("TIFF").asSequence()
        val first = tiffWriters.filterIsInstance<TIFFImageWriter>().firstOrNull()
        log.info("Try to find tiff writer class: $first")
        return first.toOptional()
    }

    private fun <T : Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)
}