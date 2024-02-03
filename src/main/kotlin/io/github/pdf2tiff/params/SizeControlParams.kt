package io.github.pdf2tiff.params

import java.io.InputStream
import java.io.OutputStream

/**
 * Parameters for controlling the size of the output.
 *
 * @param maxFileSize maximum file size in bytes
 * @param qualityParams list of quality parameters
 * @param sourceFile source file path
 * @param destFile destination file path
 * @param sourceInputStream source input stream
 * @param destOutputStream destination output stream
 */
data class SizeControlParams(
    val maxFileSize: Long,
    val qualityParams: List<QualityParams>,

    val sourceFile: String?,
    val destFile: String?,

    var sourceInputStream: InputStream?,
    var destOutputStream: OutputStream?
) {

    fun isFilePair() = sourceFile != null && destFile != null
    fun isStreamPair() = sourceInputStream != null && destOutputStream != null

    class Builder {
        private var maxFileSize: Long = 0
        private var qualityParams: MutableList<QualityParams> = mutableListOf()

        private var sourceFile: String? = null
        private var destFile: String? = null

        private var sourceInputStream: InputStream? = null
        private var destOutputStream: OutputStream? = null

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

        fun build(): SizeControlParams {
            val isFileSet = sourceFile != null && destFile != null
            val isStreamSet = sourceInputStream != null && destOutputStream != null

            require(isFileSet || isStreamSet) {
                "Either both sourceFile and destFile or both sourceInputStream and destOutputStream must be set"
            }

            return SizeControlParams(
                maxFileSize, qualityParams,
                sourceFile, destFile,
                sourceInputStream, destOutputStream
            )
        }
    }
}