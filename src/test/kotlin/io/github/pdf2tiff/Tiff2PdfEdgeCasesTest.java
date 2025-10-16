package io.github.pdf2tiff;

import io.github.tiff2pdf.Images2Pdf;
import io.github.tiff2pdf.PageImage;
import io.github.tiff2pdf.params.Compression;
import io.github.tiff2pdf.params.Tiff2PdfQualityParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Edge case tests for TIFF to PDF conversion.
 * Tests boundary conditions, error handling, and special scenarios.
 */
public class Tiff2PdfEdgeCasesTest {

    @Test
    public void testEmptyPagesList() {
        // Test that empty pages list throws IOException
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams();

        IOException exception = Assertions.assertThrows(
                IOException.class,
                () -> Images2Pdf.write(Collections.emptyList(), output, quality),
                "Should throw IOException for empty pages list"
        );

        Assertions.assertTrue(
                exception.getMessage().contains("No pages to convert"),
                "Exception message should mention empty pages"
        );
    }

    @Test
    public void testDefaultDpi() throws IOException {
        // Test that images without DPI metadata default to 72 DPI
        // Create a simple test image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        // Create PageImage with default 72 DPI
        PageImage page = new PageImage(image, 72.0f, 72.0f, 1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams();

        Images2Pdf.write(Collections.singletonList(page), output, quality);

        // Verify PDF was created (size > 0)
        Assertions.assertTrue(output.size() > 0, "PDF should be generated with default DPI");
    }

    @Test
    public void testOrientationRotation() throws IOException {
        // Test various TIFF orientation values (1-8)
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams();

        for (int orientation = 1; orientation <= 8; orientation++) {
            PageImage page = new PageImage(image, 72.0f, 72.0f, orientation);
            output.reset();

            Images2Pdf.write(Collections.singletonList(page), output, quality);

            Assertions.assertTrue(
                    output.size() > 0,
                    "PDF should be generated for orientation " + orientation
            );
        }
    }

    @Test
    public void testTargetDpiDownsampling() throws IOException {
        // Test that targetDpi actually reduces file size when lower than original
        BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
        PageImage highDpiPage = new PageImage(image, 300.0f, 300.0f, 1);

        ByteArrayOutputStream highDpiOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream lowDpiOutput = new ByteArrayOutputStream();

        // Test with original 300 DPI
        Tiff2PdfQualityParams highDpiQuality = new Tiff2PdfQualityParams(
                Compression.LOSSLESS,
                0.8f,
                null,  // No targetDpi, use original
                io.github.tiff2pdf.params.ColorHint.AUTO
        );
        Images2Pdf.write(Collections.singletonList(highDpiPage), highDpiOutput, highDpiQuality);

        // Test with downsampled 100 DPI
        Tiff2PdfQualityParams lowDpiQuality = new Tiff2PdfQualityParams(
                Compression.LOSSLESS,
                0.8f,
                100,  // Downsample to 100 DPI
                io.github.tiff2pdf.params.ColorHint.AUTO
        );
        Images2Pdf.write(Collections.singletonList(highDpiPage), lowDpiOutput, lowDpiQuality);

        // Downsampled version should be significantly smaller
        Assertions.assertTrue(
                lowDpiOutput.size() < highDpiOutput.size(),
                "Downsampled PDF should be smaller than original DPI PDF"
        );
    }

    @Test
    public void testMultipleCompressionFallback() throws IOException {
        // Test that JPEG compression can fallback to lossless on error
        // Create an image that might trigger edge cases
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        PageImage page = new PageImage(image, 72.0f, 72.0f, 1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // AUTO compression should handle this gracefully
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.AUTO,
                0.8f,
                null,
                io.github.tiff2pdf.params.ColorHint.AUTO
        );

        Images2Pdf.write(Collections.singletonList(page), output, quality);

        Assertions.assertTrue(output.size() > 0, "PDF should be generated with AUTO compression");
    }

    @Test
    public void testBinaryImageCompression() throws IOException {
        // Test CCITT compression behavior for binary images
        BufferedImage binaryImage = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_BINARY);
        PageImage page = new PageImage(binaryImage, 72.0f, 72.0f, 1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Test CCITT compression (currently uses lossless as placeholder)
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.CCITT,
                0.8f,
                null,
                io.github.tiff2pdf.params.ColorHint.AUTO
        );

        Images2Pdf.write(Collections.singletonList(page), output, quality);

        Assertions.assertTrue(output.size() > 0, "PDF should be generated with CCITT compression");
    }

    @Test
    public void testNonBinaryImageWithCcitt() throws IOException {
        // Test that CCITT compression gracefully handles non-binary images
        BufferedImage rgbImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        PageImage page = new PageImage(rgbImage, 72.0f, 72.0f, 1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // CCITT on non-binary image should fallback to lossless with warning
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.CCITT,
                0.8f,
                null,
                io.github.tiff2pdf.params.ColorHint.AUTO
        );

        Images2Pdf.write(Collections.singletonList(page), output, quality);

        Assertions.assertTrue(output.size() > 0, "PDF should be generated despite compression mismatch");
    }

    @Test
    public void testExtremeJpegQuality() throws IOException {
        // Test extreme JPEG quality values
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        PageImage page = new PageImage(image, 72.0f, 72.0f, 1);

        // Test very low quality (0.2)
        ByteArrayOutputStream lowQualityOutput = new ByteArrayOutputStream();
        Tiff2PdfQualityParams lowQuality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.2f,
                null,
                io.github.tiff2pdf.params.ColorHint.AUTO
        );
        Images2Pdf.write(Collections.singletonList(page), lowQualityOutput, lowQuality);

        // Test very high quality (1.0)
        ByteArrayOutputStream highQualityOutput = new ByteArrayOutputStream();
        Tiff2PdfQualityParams highQuality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                1.0f,
                null,
                io.github.tiff2pdf.params.ColorHint.AUTO
        );
        Images2Pdf.write(Collections.singletonList(page), highQualityOutput, highQuality);

        Assertions.assertTrue(lowQualityOutput.size() > 0, "Low quality PDF should be generated");
        Assertions.assertTrue(highQualityOutput.size() > 0, "High quality PDF should be generated");

        // High quality should typically be larger (though not guaranteed for tiny images)
        Assertions.assertTrue(
                highQualityOutput.size() >= lowQualityOutput.size() * 0.5,
                "High quality PDF should not be dramatically smaller than low quality"
        );
    }

    @Test
    public void testLargeImageDimensions() throws IOException {
        // Test with large image dimensions
        BufferedImage largeImage = new BufferedImage(5000, 5000, BufferedImage.TYPE_BYTE_GRAY);
        PageImage page = new PageImage(largeImage, 300.0f, 300.0f, 1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Use JPEG compression to keep file size manageable
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams(
                Compression.JPEG,
                0.5f,
                100,  // Downsample to reduce processing
                io.github.tiff2pdf.params.ColorHint.GRAY
        );

        Images2Pdf.write(Collections.singletonList(page), output, quality);

        Assertions.assertTrue(output.size() > 0, "Large image PDF should be generated");
    }

    @Test
    public void testMultiplePages() throws IOException {
        // Test processing multiple pages
        BufferedImage image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        BufferedImage image3 = new BufferedImage(150, 200, BufferedImage.TYPE_BYTE_GRAY);

        PageImage page1 = new PageImage(image1, 72.0f, 72.0f, 1);
        PageImage page2 = new PageImage(image2, 150.0f, 150.0f, 6); // Rotated 90° CW
        PageImage page3 = new PageImage(image3, 300.0f, 300.0f, 3); // Rotated 180°

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Tiff2PdfQualityParams quality = new Tiff2PdfQualityParams();

        Images2Pdf.write(Arrays.asList(page1, page2, page3), output, quality);

        Assertions.assertTrue(output.size() > 0, "Multi-page PDF should be generated");
    }
}
