package io.github.pdf2tiff;

import io.github.pdf2tiff.params.QualityParams;
import io.github.pdf2tiff.params.SizeControlParams;
import org.apache.pdfbox.rendering.ImageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Pdf2TiffTest {
    @Test
    public void sizeControlTest() throws IOException, ClassNotFoundException {
        SizeControlParams sizeControlParams =
                new SizeControlParams.Builder()
                        .maxFileSize(15000)
                        .qualityParam(new QualityParams(300, "Deflate", ImageType.RGB))
                        .qualityParam(new QualityParams(200, "Deflate", ImageType.GRAY))
                        .qualityParam(new QualityParams(100, "JPEG", ImageType.GRAY))
                        .qualityParam(new QualityParams(100, "Deflate", ImageType.BINARY)) // this one is 14kb
                        .qualityParam(new QualityParams(50, "Deflate", ImageType.BINARY)) // this one will not run
                        .filePair("src/test/resources/sample.pdf", "output.tiff")
                        .build();

        Pdf2Tiff.INSTANCE.pdf2Tiff(sizeControlParams);

        long outputFileSize = Files.size(Paths.get("output.tiff"));
        Assertions.assertTrue(outputFileSize > 14000);
        Assertions.assertTrue(outputFileSize < 15000);
    }

    @Test
    public void regularConversionTest() throws IOException, ClassNotFoundException {
        List<String> compressions = Arrays.asList("LZW", "Deflate");

        for (String compression : compressions) {
            regularConvert(300, compression);
            regularConvert(100, compression);
        }
    }

    private void regularConvert(int dpi, String compression) throws IOException, ClassNotFoundException {
        String outputFile = "output-" + dpi + compression + ".tiff";
        Pdf2Tiff.INSTANCE.pdf2Tiff("src/test/resources/sample.pdf", outputFile, dpi, compression, ImageType.RGB);

        long outputFileSize = Files.size(Paths.get(outputFile));
        Assertions.assertTrue(outputFileSize > 0);
    }
}