package org.github.cuipengfei;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.imageio.ImageIO.createImageOutputStream;
import static javax.imageio.ImageIO.getImageWritersByFormatName;

@Slf4j
public class BufferedImages2Tiff {

    /**
     * Convert from a list of buffered images to tiff output stream
     *
     * @param bufferedImages a list of buffered images
     * @param output         tiff output stream
     * @param compression    compression type, such as "LZW", "JPEG", "PackBits", "Deflate"
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be located
     */
    public void bufferedImages2TiffOutputStream(
            List<BufferedImage> bufferedImages, OutputStream output, String compression)
            throws IOException, ClassNotFoundException {

        Optional<ImageWriter> tiffWriterOptional = findTiffWriter();

        if (tiffWriterOptional.isPresent()) {
            ImageWriter tiffWriter = tiffWriterOptional.get();
            ImageWriteParam writeParam = configWriteParam(tiffWriter, compression);

            try (ImageOutputStream imageOutputStream = createImageOutputStream(output)) {
                tiffWriter.setOutput(imageOutputStream);
                tiffWriter.prepareWriteSequence(null);

                for (BufferedImage bufferedImage : bufferedImages) {
                    tiffWriter.writeToSequence(new IIOImage(bufferedImage, null, null), writeParam);
                }

                tiffWriter.endWriteSequence();
            }
            tiffWriter.dispose();
            log.info("Buffered images to tiff output stream, number of pages: {}", bufferedImages.size());
        } else {
            throw new ClassNotFoundException("Can not find tiff writer class");
        }
    }

    private static ImageWriteParam configWriteParam(ImageWriter tiffWriter, String compression) {
        ImageWriteParam writeParam = tiffWriter.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType(compression);
        return writeParam;
    }

    private static Optional<ImageWriter> findTiffWriter() {
        List<ImageWriter> actualList = new ArrayList<>();
        getImageWritersByFormatName("TIFF").forEachRemaining(actualList::add);
        log.info("Find tiff writer class: {}", actualList);
        return actualList.stream().filter(writer -> writer instanceof TIFFImageWriter).findFirst();
    }
}
