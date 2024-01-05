package org.github.cuipengfei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.Files.newOutputStream;

public class Pdf2Tiff {

    private static final int DEFAULT_DPI = 300;
    private static final String DEFAULT_COMPRESSION = "Deflate";

    public static void pdf2Tiff(InputStream input, OutputStream output)
            throws IOException, ClassNotFoundException {
        pdf2Tiff(input, output, DEFAULT_DPI, DEFAULT_COMPRESSION);
    }

    public static void pdf2Tiff(String pdfPath, String tiffPath)
            throws IOException, ClassNotFoundException {
        pdf2Tiff(pdfPath, tiffPath, DEFAULT_DPI, DEFAULT_COMPRESSION);
    }

    public static void pdf2Tiff(InputStream input, OutputStream output, int dpi, String compression)
            throws IOException, ClassNotFoundException {
        Pdf2BufferedImages pdf2Images = new Pdf2BufferedImages(dpi);
        BufferedImages2Tiff images2Tiff = new BufferedImages2Tiff();

        images2Tiff.bufferedImages2TiffOutputStream(pdf2Images.pdf2BufferedImages(input), output, compression);
    }

    public static void pdf2Tiff(String pdfPath, String tiffPath, int dpi, String compression)
            throws IOException, ClassNotFoundException {
        InputStream input = Files.newInputStream(Paths.get(pdfPath));
        OutputStream output = newOutputStream(Paths.get(tiffPath));

        pdf2Tiff(input, output, dpi, compression);

        input.close();
        output.close();
    }
}
