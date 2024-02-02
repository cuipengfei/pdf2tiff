package io.github.pdf2tiff

import java.io.InputStream
import java.io.OutputStream

data class FileSizeControl(
    val maxFileSize: Long,
    val qualityParams: List<QualityParams>,
    val sourceFile: String?,
    val destFile: String?,
    var sourceInputStream: InputStream?,
    var destOutputStream: OutputStream?
) {
    fun isFilePair() = sourceFile != null && destFile != null
    fun isStreamPair() = sourceInputStream != null && destOutputStream != null

    data class Builder(
        var maxFileSize: Long = 0,
        var qualityParams: MutableList<QualityParams> = mutableListOf(),
        var sourceFile: String? = null,
        var destFile: String? = null,
        var sourceInputStream: InputStream? = null,
        var destOutputStream: OutputStream? = null
    ) {
        fun maxFileSize(maxFileSize: Long) = apply { this.maxFileSize = maxFileSize }
        fun qualityParams(qualityParams: List<QualityParams>) = apply { this.qualityParams.addAll(qualityParams) }
        fun qualityParam(qualityParam: QualityParams) = apply { this.qualityParams.add(qualityParam) }
        fun filePair(sourceFile: String, destFile: String) = apply {
            this.sourceFile = sourceFile
            this.destFile = destFile
        }

        fun streamPair(sourceInputStream: InputStream, destOutputStream: OutputStream) = apply {
            this.sourceInputStream = sourceInputStream
            this.destOutputStream = destOutputStream
        }

        fun build(): FileSizeControl {
            require(sourceFile != null && destFile != null || sourceInputStream != null && destOutputStream != null) {
                "Either both sourceFile and destFile or both sourceInputStream and destOutputStream must be set"
            }
            return FileSizeControl(
                maxFileSize,
                qualityParams,
                sourceFile,
                destFile,
                sourceInputStream,
                destOutputStream
            )
        }
    }
}