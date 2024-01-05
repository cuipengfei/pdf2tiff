package org.github.cpf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.apache.pdfbox.io.RandomAccessReadBuffer.createBufferFromStream;

public class Pdf2BufferedImages {

    private final int dpi;

    public Pdf2BufferedImages(int dpi) {
        this.dpi = dpi;
    }

    public List<BufferedImage> pdf2BufferedImages(InputStream pdfInputStream) throws IOException {
        ArrayList<BufferedImage> images = new ArrayList<>();

        PDDocument pdf = Loader.loadPDF(createBufferFromStream(pdfInputStream));
        PDFRenderer renderer = new PDFRenderer(pdf);

        int numberOfPages = pdf.getNumberOfPages();
        for (int i = 0; i < numberOfPages; i++) {
            BufferedImage bufferedImage = renderer.renderImageWithDPI(i, dpi);
            images.add(bufferedImage);
        }

        return images;
    }
}
