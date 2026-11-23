
package br.com.hdf.anonimizador.data.ingestion

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import br.com.hdf.anonimizador.di.IoDispatcher
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.model.DocumentSource
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import javax.inject.Inject

class AndroidDocumentExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DocumentExtractor {

    private val contentResolver: ContentResolver =
        context.contentResolver

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    companion object {
        private const val MAX_FILE_BYTES = 20L * 1024 * 1024
        private const val MAX_TEXT_BYTES = 10 * 1024 * 1024
        private const val MAX_PDF_TEXT_CHARS = 10 * 1024 * 1024
        private const val MAX_ZIP_SCAN_BYTES = 50 * 1024 * 1024
        private const val MAX_ZIP_ENTRIES = 1_000
        private const val BUFFER_SIZE = 8 * 1024
    }

    override suspend fun extractText(
        source: DocumentSource
    ): String = withContext(ioDispatcher) {

        val contextJob = currentCoroutineContext()

        contextJob.ensureActive()

        val uri = source.uriString.toUri()
        val mimeType = contentResolver.getType(uri)
            .orEmpty()
            .lowercase()

        val fileName = resolveDisplayName(uri).lowercase()

        validateFileSize(uri)

        contextJob.ensureActive()

        val result = when {
            isZipFile(mimeType, fileName) ->
                extractTxtFromWhatsAppZip(uri)

            isPdfFile(mimeType, fileName) ->
                extractTextFromPdf(uri)

            isPlainTextFile(mimeType, fileName) ->
                extractPlainText(uri)

            else -> throw IllegalArgumentException(
                "Formato de arquivo não suportado."
            )
        }

        contextJob.ensureActive()

        result.trim()
    }

    private suspend fun extractTxtFromWhatsAppZip(
        uri: Uri
    ): String {

        openStream(uri).use { rawStream ->

            val boundedStream = LimitedInputStream(
                input = rawStream,
                maxBytes = MAX_FILE_BYTES
            )

            ZipInputStream(boundedStream).use { zipStream ->

                var entryCount = 0
                var scannedBytes = 0L

                while (true) {

                    currentCoroutineContext().ensureActive()

                    val entry = zipStream.nextEntry
                        ?: break

                    entryCount++

                    if (entryCount > MAX_ZIP_ENTRIES) {
                        throw IllegalStateException(
                            "O ZIP contém entradas demais."
                        )
                    }

                    if (entry.isDirectory) {
                        zipStream.closeEntry()
                        continue
                    }

                    if (
                        entry.name.endsWith(
                            ".txt",
                            ignoreCase = true
                        )
                    ) {
                        return readBoundedUtf8(zipStream)
                    }

                    scannedBytes += discardZipEntry(
                        zipStream,
                        MAX_ZIP_SCAN_BYTES - scannedBytes
                    )

                    zipStream.closeEntry()

                    if (scannedBytes > MAX_ZIP_SCAN_BYTES) {
                        throw IllegalStateException(
                            "O conteúdo do ZIP excede o limite de inspeção."
                        )
                    }
                }
            }
        }

        throw IllegalArgumentException(
            "Nenhum arquivo .txt de conversa encontrado dentro do .zip."
        )
    }

    private suspend fun discardZipEntry(
        inputStream: InputStream,
        remainingLimit: Long
    ): Long {

        val buffer = ByteArray(BUFFER_SIZE)
        var totalRead = 0L

        while (true) {

            currentCoroutineContext().ensureActive()

            val bytesRead = inputStream.read(buffer)

            if (bytesRead == -1) {
                break
            }

            totalRead += bytesRead

            if (totalRead > remainingLimit) {
                throw IllegalStateException(
                    "O conteúdo descompactado do ZIP excede o limite permitido."
                )
            }
        }

        return totalRead
    }

    private suspend fun extractTextFromPdf(
        uri: Uri
    ): String {

        openStream(uri).use { inputStream ->

            val boundedStream = LimitedInputStream(
                input = inputStream,
                maxBytes = MAX_FILE_BYTES
            )

            PDDocument.load(boundedStream).use { document ->

                currentCoroutineContext().ensureActive()

                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }

                val writer = BoundedStringWriter(
                    maxChars = MAX_PDF_TEXT_CHARS
                )

                stripper.writeText(document, writer)

                currentCoroutineContext().ensureActive()

                return writer.toString()
            }
        }
    }

    private suspend fun extractPlainText(
        uri: Uri
    ): String {

        openStream(uri).use { inputStream ->

            val boundedStream = LimitedInputStream(
                input = inputStream,
                maxBytes = MAX_FILE_BYTES
            )

            return readBoundedUtf8(boundedStream)
        }
    }

    private suspend fun readBoundedUtf8(
        inputStream: InputStream
    ): String {

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var totalRead = 0

        while (true) {

            currentCoroutineContext().ensureActive()

            val bytesRead = inputStream.read(buffer)

            if (bytesRead == -1) {
                break
            }

            totalRead += bytesRead

            if (totalRead > MAX_TEXT_BYTES) {
                throw IllegalStateException(
                    "O arquivo de texto excede o limite de 10 MB."
                )
            }

            output.write(buffer, 0, bytesRead)
        }

        return output.toString(
            StandardCharsets.UTF_8.name()
        )
    }

    private fun validateFileSize(uri: Uri) {

        val size = resolveFileSize(uri) ?: return

        if (size < 0L) {
            throw IllegalStateException(
                "O tamanho informado pelo provedor é inválido."
            )
        }

        if (size > MAX_FILE_BYTES) {
            throw IllegalStateException(
                "O arquivo excede o limite permitido de 20 MB."
            )
        }
    }

    private fun resolveFileSize(uri: Uri): Long? {

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->

            if (!cursor.moveToFirst()) {
                return null
            }

            val sizeIndex = cursor.getColumnIndex(
                OpenableColumns.SIZE
            )

            if (sizeIndex == -1 || cursor.isNull(sizeIndex)) {
                return null
            }

            return cursor.getLong(sizeIndex)
        }

        return null
    }

    private fun resolveDisplayName(uri: Uri): String {

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            val nameIndex = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex).orEmpty()
            }
        }

        return uri.lastPathSegment.orEmpty()
    }

    private fun openStream(uri: Uri): InputStream =
        contentResolver.openInputStream(uri)
            ?: throw IllegalStateException(
                "Não foi possível abrir o arquivo."
            )

    private fun isZipFile(
        mimeType: String,
        fileName: String
    ): Boolean =
        mimeType.contains("zip") ||
                fileName.endsWith(".zip")

    private fun isPdfFile(
        mimeType: String,
        fileName: String
    ): Boolean =
        mimeType == "application/pdf" ||
                fileName.endsWith(".pdf")

    private fun isPlainTextFile(
        mimeType: String,
        fileName: String
    ): Boolean =
        mimeType.startsWith("text/") ||
                fileName.endsWith(".txt")

    private class LimitedInputStream(
        input: InputStream,
        private val maxBytes: Long
    ) : FilterInputStream(input) {

        private var bytesRead = 0L

        override fun read(): Int {

            val value = `in`.read()

            if (value != -1) {
                countBytes(1)
            }

            return value
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int
        ): Int {

            val count = `in`.read(
                buffer,
                offset,
                length
            )

            if (count > 0) {
                countBytes(count)
            }

            return count
        }

        private fun countBytes(count: Int) {

            bytesRead += count

            if (bytesRead > maxBytes) {
                throw IllegalStateException(
                    "O arquivo excede o limite permitido de 20 MB."
                )
            }
        }
    }

    private class BoundedStringWriter(
        private val maxChars: Int
    ) : Writer() {

        private val builder = StringBuilder()

        override fun write(
            chars: CharArray,
            offset: Int,
            length: Int
        ) {

            require(offset >= 0 && length >= 0 &&
                    offset + length <= chars.size) {
                "Intervalo inválido de escrita."
            }

            if (builder.length + length > maxChars) {
                throw IllegalStateException(
                    "O texto extraído do PDF excede o limite permitido."
                )
            }

            builder.append(chars, offset, length)
        }

        override fun flush() = Unit

        override fun close() = Unit

        override fun toString(): String = builder.toString()
    }
}
