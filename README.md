[![Publish package to the Maven Central Repository](https://github.com/pdf2tiff/pdf2tiff/actions/workflows/publish-to-maven.yaml/badge.svg)](https://github.com/pdf2tiff/pdf2tiff/actions/workflows/publish-to-maven.yaml)

Convert PDF to multi-page Tiff, using pdfbox and twelvemonkeys.

Example

```java
// call the converter from java
// suggested dpi: 300, suggested compression: Deflate
Pdf2Tiff.INSTANCE.pdf2Tiff("my.pdf", "my.tiff", 300, "Deflate", ImageType.RGB);
```

```xml

<dependency>
    <groupId>io.github.pdf2tiff</groupId>
    <artifactId>pdf2tiff</artifactId>
    <version>1.5.2</version>
</dependency>
```
