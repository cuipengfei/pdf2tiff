Convert PDF to multi-page Tiff, using pdfbox and twelvemonkeys.

Example

```java
pdf2Tiff("pdf-file-name.pdf", "out.tiff", dpi, compression);
// or
pdf2Tiff(pdfInputStream, tiffOutputStream, dpi, compression);

// suggested dpi 300, suggested compression Deflate
```
