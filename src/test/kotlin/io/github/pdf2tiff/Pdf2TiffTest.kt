package io.github.pdf2tiff

import org.apache.pdfbox.rendering.ImageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class Pdf2TiffTest {

    @Test
    fun pdf2Tiff() {
        val compressions = listOf(
            "LZW",
            "Deflate"
        )

        for (compression in compressions) {
            tryConvert(300, compression)
            tryConvert(100, compression)
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun tryConvert(dpi: Int, compression: String) {
        val outputFile = "output-$dpi$compression.tiff"
        Pdf2Tiff.pdf2Tiff("src/test/resources/sample.pdf", outputFile, dpi, compression, ImageType.RGB)

        val outputFileSize = Files.size(Paths.get(outputFile))
        Assertions.assertTrue(outputFileSize > 0)
    }
}