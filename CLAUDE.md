# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

pdf2tiff 是一个双向 Kotlin/Java 互操作库：
- **PDF→TIFF**: 使用 PDFBox 渲染 + TwelveMonkeys TIFF 编码
- **TIFF→PDF**: 使用 TwelveMonkeys TIFF 解码 + PDFBox 文档生成
- 核心设计：对称 API、流式处理、自适应质量控制

## Build & Test Commands

```bash
# Build the project
mvn clean install

# Run all tests
mvn test

# Run a single test
mvn test -Dtest=Pdf2TiffTest#regularConversionTest

# Package (generates sources + javadoc via Dokka)
mvn package

# Skip GPG signing during local development
mvn clean install -Dgpg.skip=true
```

## Architecture

### Bidirectional Pipeline

**PDF → TIFF 方向**:
1. **Pdf2Tiff** (entry point) - `src/main/kotlin/io/github/pdf2tiff/Pdf2Tiff.kt:12`
   - Kotlin `object` 暴露为 Java 的 `Pdf2Tiff.INSTANCE`
2. **Pdf2Images** - 使用 PDFBox `PDFRenderer` 将 PDF 页面渲染为 BufferedImage 数组
3. **Images2Tiff** - 使用 TwelveMonkeys `ImageWriter` 将 BufferedImage 编码为多页 TIFF

**TIFF → PDF 方向** (对称设计):
1. **Tiff2Pdf** - `src/main/kotlin/io/github/pdf2tiff/Tiff2Pdf.kt`
   - 三种 API 方法与 Pdf2Tiff 完全对称
2. **Tiff2Images** - 使用 TwelveMonkeys `ImageReader` 读取多页 TIFF + 提取 DPI 元数据
   - DPI 提取：优先 TIFF 原生树 (fields 282/283/296)，回退到标准 ImageIO 树
   - 返回 `PageImage(image, dpiX, dpiY, orientation)`
3. **Images2Pdf** - 使用 PDFBox 将 BufferedImage 序列嵌入 PDF
   - 页面尺寸计算: `pixels × 72 / DPI = PDF points`
   - JPEG 压缩需移除 alpha 通道（白底合成）
   - 压缩策略: JPEGFactory (lossy) / LosslessFactory (lossless) / CCITT (MVP 阶段用 lossless 模拟)

### Size Control Logic (两方向通用)

自适应质量降级（`pdf2Tiff(SizeControlParams)` / `tiff2Pdf(Tiff2PdfSizeControlParams)`）:
- 接受多个质量参数（从高到低）
- 使用 ByteArrayOutputStream 缓冲每次尝试
- 按顺序尝试直到输出大小 ≤ maxFileSize，early return
- 如果全部超限，使用最后一个参数的输出 (Pdf2Tiff.kt:108 / Tiff2Pdf.kt:122)

### Parameters

**PDF→TIFF**:
- **DPI**: 300 (高质量) / 100-200 (平衡) / 50 (小尺寸)
- **Compression**: "Deflate" (推荐) / "LZW" / "JPEG"
- **ImageType**: RGB / GRAY / BINARY (PDFBox `ImageType` 枚举)

**TIFF→PDF** (`Tiff2PdfQualityParams`):
- **compression**: `Compression.AUTO` (默认) / JPEG / LOSSLESS / CCITT
- **jpegQuality**: 0.8f (默认), 范围 0.2–1.0
- **targetDpi**: 覆盖 TIFF 原始 DPI，用于缩放（可选）
- **colorHint**: `ColorHint.AUTO` (默认) / RGB / GRAY / BINARY

## Kotlin/Java Interop

- 源码用 Kotlin 编写，测试用 Java（验证互操作性）
- Kotlin `object` 在 Java 中通过 `.INSTANCE` 访问
- 默认参数在 Java 中表现为重载方法（`@JvmOverloads`）
- **关键注解**: `@JvmStatic`（暴露静态方法）、`@JvmOverloads`（生成默认参数重载）
- **API 对称性**: `Tiff2Pdf` 与 `Pdf2Tiff` 保持完全对称的 API 设计（三种方法签名一致）

## Maven Specifics

- Java 8 兼容 (maven.compiler.source/target=8)
- 源码目录: `src/main/kotlin` (非标准 Maven 路径,由 kotlin-maven-plugin 配置)
- 测试目录: `src/test/kotlin` (实际包含 `.java` 文件)
- 使用 Dokka 生成 Javadoc (dokka-maven-plugin:1.9.10)
- 发布到 Maven Central 通过 central-publishing-maven-plugin

## Image Compatibility

pom.xml 包含额外的 imageio 依赖以处理 PDF 内嵌的特殊图像格式:
- `imageio-jpeg`: JPEG 处理增强
- `jai-imageio-jpeg2000`: JPEG 2000 支持
- `jbig2-imageio`: JBIG2 压缩支持

TwelveMonkeys 的 `imageio-tiff` 作为 SPI 提供 TIFF 格式支持（用于 TIFF→PDF 方向）

## Implementation Notes

### TIFF DPI Extraction (Tiff2Images.kt)
- **双策略解析**: 优先从 TIFF 原生树读取字段 282 (XResolution) / 283 (YResolution) / 296 (ResolutionUnit)
- **回退机制**: 如无原生树，从标准 ImageIO 树读取 HorizontalPixelSize / VerticalPixelSize（mm/pixel → DPI）
- **默认值**: 无 DPI 元数据时默认 72 DPI

### JPEG Compression (Images2Pdf.kt)
- **Alpha 通道处理**: `JPEGFactory.createFromImage()` 不支持透明度，必须先调用 `removeAlpha()` 将图像合成到白底
- **类型检查**: 如果已经是 `TYPE_INT_RGB` / `TYPE_BYTE_GRAY`，直接使用

### TIFF Orientation Support (Images2Pdf.kt)
- **完整实现 TIFF 6.0 规范**: 支持全部 8 种 orientation 值 (1=正常, 2-8=旋转/翻转组合)
- **转换机制**: `applyOrientation()` 使用 `AffineTransform` 处理旋转/翻转
- **执行时机**: 在颜色转换和 DPI 重采样之前应用

### TargetDpi Resampling (Images2Pdf.kt)
- **真实重采样**: `resampleImage()` 使用双线性插值执行像素级缩放
- **仅支持向下采样**: 仅当 `scaleFactor < 1.0` 时触发（上采样无法增加真实细节）
- **计算逻辑**: `scaleFactor = targetDpi / max(原始dpiX, 原始dpiY)`
- **行为**: targetDpi > 原始 DPI 时仅改变页面物理尺寸,不放大像素

### Resource Management (Tiff2Images.kt)
- **ImageReader Disposal**: try-finally 确保 `reader.dispose()` 调用
- **避免泄漏**: ImageReader 持有原生缓冲区,必须显式释放

### PDFBox 3.x API Critical Calls
- `LosslessFactory.createFromImage(doc, image)` - **第一个参数必须是 PDDocument**
- `JPEGFactory.createFromImage(doc, image, quality)` - 同上
- 页面尺寸: `PDRectangle(widthPoints, heightPoints)` where `points = pixels × 72 / DPI`
