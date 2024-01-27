Convert PDF to multi-page Tiff, using pdfbox and twelvemonkeys.

Example

```java
// call from java
Pdf2Tiff.INSTANCE.pdf2Tiff("my.pdf", "my.tiff", dpi, compression, ImageType.RGB);
// suggested dpi 300, suggested compression Deflate
```

```xml
<dependency>
  <groupId>com.github.cuipengfei</groupId>
  <artifactId>pdf2tiff</artifactId>
  <version>1.3</version>
</dependency>
```
