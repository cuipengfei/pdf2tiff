package io.github.pdf2tiff;

import io.github.tiff2pdf.params.Compression;
import io.github.tiff2pdf.params.ColorHint;
import io.github.tiff2pdf.params.Tiff2PdfQualityParams;
import io.github.tiff2pdf.params.Tiff2PdfSizeControlParams;
import io.github.tiff2pdf.Tiff2Pdf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Tiff2PdfTest {

    private static final String TEST_TIFF = "src/test/resources/sample.tiff";
    private static final String OUTPUT_PDF = "output-tiff2pdf.pdf";

    @BeforeAll
    public static void setup() throws IOException, ClassNotFoundException {
        // Generate a test TIFF file from the sample PDF if it doesn't exist
        if (!Files.exists(Paths.get(TEST_TIFF))) {
            Pdf2Tiff.INSTANCE.pdf2Tiff(
                    "src/test/resources/sample.pdf",
                    TEST_TIFF,
                    200,
                    "Deflate",
                    org.apache.pdfbox.rendering.ImageType.RGB
            );
        }
    }

    @Test
    public void testFileToFile() throws IOException {
        // Simple file to file conversion with default parameters
        Tiff2Pdf.tiff2Pdf(TEST_TIFF, OUTPUT_PDF);

        long outputSize = Files.size(Paths.get(OUTPUT_PDF));
        Assertions.assertTrue(outputSize > 0, "Output PDF should not be empty");
    }

    @Test
    public void testFileToFileWithQuality() throws IOException {
        // File to file conversion with custom quality parameters
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.7f,
                150,
                ColorHint.GRAY
        );

        Tiff2Pdf.tiff2Pdf(TEST_TIFF, OUTPUT_PDF, quality);

        long outputSize = Files.size(Paths.get(OUTPUT_PDF));
        Assertions.assertTrue(outputSize > 0, "Output PDF should not be empty");
    }

    @Test
    public void testStreamToStream() throws IOException {
        // Stream to stream conversion
        FileInputStream input = new FileInputStream(TEST_TIFF);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Tiff2Pdf.tiff2Pdf(input, output);

        Assertions.assertTrue(output.size() > 0, "Output PDF should not be empty");

        input.close();
    }

    @Test
    public void testStreamToStreamWithQuality() throws IOException {
        // Stream to stream conversion with quality parameters
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.LOSSLESS,
                0.8f,
                null,
                ColorHint.AUTO
        );

        FileInputStream input = new FileInputStream(TEST_TIFF);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Tiff2Pdf.tiff2Pdf(input, output, quality);

        Assertions.assertTrue(output.size() > 0, "Output PDF should not be empty");

        input.close();
    }

    @Test
    public void testSizeControlWithFilePair() throws IOException {
        // Size control with file pair
        Tiff2PdfQualityParams highQuality = new Tiff2PdfQualityParams(
                Compression.LOSSLESS,
                0.9f,
                300,
                ColorHint.RGB
        );

        Tiff2PdfQualityParams mediumQuality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.7f,
                200,
                ColorHint.RGB
        );

        Tiff2PdfQualityParams lowQuality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.5f,
                100,
                ColorHint.GRAY
        );

        Tiff2PdfSizeControlParams sizeControl = new Tiff2PdfSizeControlParams.Builder()
                .maxFileSize(50000L)
                .qualityParam(highQuality)
                .qualityParam(mediumQuality)
                .qualityParam(lowQuality)
                .filePair(TEST_TIFF, OUTPUT_PDF)
                .build();

        Tiff2Pdf.tiff2Pdf(sizeControl);

        long outputSize = Files.size(Paths.get(OUTPUT_PDF));
        Assertions.assertTrue(outputSize > 0, "Output PDF should not be empty");
        // Note: we can't strictly assert <= maxFileSize because the last quality param
        // is used even if it exceeds the limit (as per design)
    }

    @Test
    public void testSizeControlWithStreamPair() throws IOException {
        // Size control with stream pair
        Tiff2PdfQualityParams q1 = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.8f,
                200,
                ColorHint.AUTO
        );

        Tiff2PdfQualityParams q2 = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.5f,
                100,
                ColorHint.GRAY
        );

        FileInputStream input = new FileInputStream(TEST_TIFF);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Tiff2PdfSizeControlParams sizeControl = new Tiff2PdfSizeControlParams.Builder()
                .maxFileSize(30000L)
                .qualityParam(q1)
                .qualityParam(q2)
                .streamPair(input, output)
                .build();

        Tiff2Pdf.tiff2Pdf(sizeControl);

        Assertions.assertTrue(output.size() > 0, "Output PDF should not be empty");

        input.close();
    }

    @Test
    public void testCompressionModes() throws IOException {
        // Test different compression modes
        Compression[] compressions = {
                Compression.AUTO,
                Compression.JPEG,
                Compression.LOSSLESS,
                Compression.CCITT
        };

        for (Compression compression : compressions) {
            Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                    compression,
                    0.8f,
                    null,
                    ColorHint.AUTO
            );

            String outputFile = "output-" + compression.name() + ".pdf";
            Tiff2Pdf.tiff2Pdf(TEST_TIFF, outputFile, quality);

            long outputSize = Files.size(Paths.get(outputFile));
            Assertions.assertTrue(outputSize > 0,
                    "Output PDF with " + compression + " compression should not be empty");
        }
    }

    @Test
    public void testDefaultParameters() throws IOException {
        // Test with default parameters (should use AUTO compression, 0.8 quality)
        Tiff2PdfQualityParams defaultQuality = new Tiff2PdfQualityParams();

        Assertions.assertEquals(Compression.AUTO, defaultQuality.getCompression());
        Assertions.assertEquals(0.8f, defaultQuality.getJpegQuality(), 0.001f);
        Assertions.assertNull(defaultQuality.getTargetDpi());
        Assertions.assertEquals(ColorHint.AUTO, defaultQuality.getColorHint());
    }
}
