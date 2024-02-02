package io.github.pdf2tiff

import org.apache.pdfbox.rendering.ImageType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class Pdf2TiffTest {
    @Test
    fun pdf2TiffTest() {
        Pdf2Tiff.pdf2Tiff(
            FileSizeControl.Builder()
                .maxFileSize(15000)
                .qualityParam(QualityParams(300, "Deflate", ImageType.RGB))
                .qualityParam(QualityParams(200, "Deflate", ImageType.GRAY))
                .qualityParam(QualityParams(100, "JPEG", ImageType.GRAY))
                .qualityParam(QualityParams(100, "Deflate", ImageType.BINARY))
                .qualityParam(QualityParams(50, "Deflate", ImageType.BINARY))
                .filePair("src/test/resources/sample.pdf", "output.tiff")
                .build()
        )

        val outputFileSize = Files.size(Paths.get("output.tiff"))
        Assertions.assertTrue(outputFileSize > 0)
        Assertions.assertTrue(outputFileSize < 15000)
    }
}