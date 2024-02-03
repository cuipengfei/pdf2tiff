package io.github.pdf2tiff

import io.github.pdf2tiff.params.QualityParams
import io.github.pdf2tiff.params.SizeControlParams
import org.apache.pdfbox.rendering.ImageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class Pdf2TiffTest {
    @Test
    fun sizeControlTest() {
        Pdf2Tiff.pdf2Tiff(
            SizeControlParams.Builder()
                .maxFileSize(15000)
                .qualityParam(QualityParams(300, "Deflate", ImageType.RGB))
                .qualityParam(QualityParams(200, "Deflate", ImageType.GRAY))
                .qualityParam(QualityParams(100, "JPEG", ImageType.GRAY))
                .qualityParam(QualityParams(100, "Deflate", ImageType.BINARY)) // this one is 14kb
                .qualityParam(QualityParams(50, "Deflate", ImageType.BINARY)) // this one will not run
                .filePair("src/test/resources/sample.pdf", "output.tiff")
                .build()
        )

        val outputFileSize = Files.size(Paths.get("output.tiff"))
        Assertions.assertTrue(outputFileSize > 14000)
        Assertions.assertTrue(outputFileSize < 15000)
    }

    @Test
    fun regularConversionTest() {
        val compressions = listOf(
            "LZW",
            "Deflate"
        )

        for (compression in compressions) {
            tryConvert(300, compression)
            tryConvert(100, compression)
        }
    }

    private fun tryConvert(dpi: Int, compression: String) {
        val outputFile = "output-$dpi$compression.tiff"
        Pdf2Tiff.pdf2Tiff("src/test/resources/sample.pdf", outputFile, dpi, compression, ImageType.RGB)

        val outputFileSize = Files.size(Paths.get(outputFile))
        Assertions.assertTrue(outputFileSize > 0)
    }
}