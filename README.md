[![Publish package to the Maven Central Repository](https://github.com/pdf2tiff/pdf2tiff/actions/workflows/publish-to-maven.yaml/badge.svg)](https://github.com/pdf2tiff/pdf2tiff/actions/workflows/publish-to-maven.yaml)

Convert PDF to multi-page Tiff, using pdfbox and twelvemonkeys.

# Regular conversion

```java
// suggested dpi: 300, suggested compression: Deflate
Pdf2Tiff.INSTANCE.pdf2Tiff("my.pdf","my.tiff",300,"Deflate",ImageType.RGB);
```

# Conversion with size control

If you want to control the size of the tiff, you can use the following code:

```java
SizeControlParams sizeControl =
    new SizeControlParams.Builder()
        .maxFileSize(15000L) // 15KB max size
        .qualityParam(new QualityParams(300, "Deflate", ImageType.RGB)) // high quality
        .qualityParam(new QualityParams(200, "Deflate", ImageType.GRAY)) // if the above is above 15KB, try this
        .qualityParam(new QualityParams(100, "Deflate", ImageType.BINARY)) // if the above is above 15KB, try this
        .qualityParam(new QualityParams(50, "Deflate", ImageType.BINARY)) // if the above is under 15KB, this one won't be used
        .filePair("sample.pdf", "output.tiff")
        .build();

Pdf2Tiff.INSTANCE.pdf2Tiff(sizeControl);
```

# Maven dependency
```xml

<dependency>
    <groupId>io.github.pdf2tiff</groupId>
    <artifactId>pdf2tiff</artifactId>
    <version>${pdf2tiff.version}</version>
</dependency>
```

# Versions
Check for versions here: https://central.sonatype.com/artifact/io.github.pdf2tiff/pdf2tiff/versions