package org.github.cuipengfei;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter;

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

public class BufferedImages2Tiff {

    public void bufferedImages2TiffOutputStream(List<BufferedImage> bufferedImages, OutputStream output, String compression)
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
        return actualList.stream().filter(writer -> writer instanceof TIFFImageWriter).findFirst();
    }
}
