package org.github.cuipengfei;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;

public class Pdf2Tiff {

    private static final int DEFAULT_DPI = 300;
    private static final String DEFAULT_COMPRESSION = "Deflate";

    /**
     * Convert from stream to stream, you need to close them after use
     *
     * @param input  pdf input stream
     * @param output tiff output tream
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    public static void pdf2Tiff(InputStream input, OutputStream output)
            throws IOException, ClassNotFoundException {
        pdf2Tiff(input, output, DEFAULT_DPI, DEFAULT_COMPRESSION);
    }

    /**
     * Convert from file to file, this lib will close them after conversion
     *
     * @param pdfPath  pdf file path
     * @param tiffPath tiff output file path
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    public static void pdf2Tiff(String pdfPath, String tiffPath)
            throws IOException, ClassNotFoundException {
        pdf2Tiff(pdfPath, tiffPath, DEFAULT_DPI, DEFAULT_COMPRESSION);
    }

    /**
     * Convert from stream to stream, you need to close them after use.
     *
     * @param input       pdf input stream
     * @param output      tiff output stream
     * @param dpi         pdf to image quality
     * @param compression tiff compression
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    public static void pdf2Tiff(InputStream input, OutputStream output, int dpi, String compression)
            throws IOException, ClassNotFoundException {
        Pdf2BufferedImages pdf2Images = new Pdf2BufferedImages(dpi);
        BufferedImages2Tiff images2Tiff = new BufferedImages2Tiff();

        images2Tiff.bufferedImages2TiffOutputStream(pdf2Images.pdf2BufferedImages(input), output, compression);
    }

    /**
     * Convert from file to file, this lib will close them after conversion.
     *
     * @param pdfPath     pdf file path
     * @param tiffPath    tiff output file path
     * @param dpi         pdf to image quality
     * @param compression tiff compression
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    public static void pdf2Tiff(String pdfPath, String tiffPath, int dpi, String compression)
            throws IOException, ClassNotFoundException {
        try (InputStream input = newInputStream(Paths.get(pdfPath));
             OutputStream output = newOutputStream(Paths.get(tiffPath))) {
            pdf2Tiff(input, output, dpi, compression);
        }
    }
}
