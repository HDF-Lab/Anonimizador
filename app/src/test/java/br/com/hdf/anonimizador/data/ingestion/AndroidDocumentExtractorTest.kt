package br.com.hdf.anonimizador.data.ingestion

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import br.com.hdf.anonimizador.domain.model.DocumentSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AndroidDocumentExtractorTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        resolver = mockk(relaxed = true)
        uri = mockk(relaxed = true)

        every { context.contentResolver } returns resolver
        every { context.applicationContext } returns context

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns uri

    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `deve extrair texto de arquivo TXT`() {
        val extractor = createExtractor()
        val nameCursor = createCursor(name = "texto.txt", size = 12L)

        every { resolver.getType(uri) } returns "text/plain"
        every { resolver.query(any(), any(), any(), any(), any()) } returns nameCursor
        every { resolver.openInputStream(uri) } returns
                ByteArrayInputStream("Texto pessoal".toByteArray())

        val result = runBlocking {
            extractor.extractText(DocumentSource("content://text/test.txt"))
        }

        assertEquals("Texto pessoal", result)
    }

    @Test
    fun `deve extrair primeiro TXT dentro do ZIP`() {
        val extractor = createExtractor()
        val zipBytes = createZip("conversa.txt", "Mensagem exportada")
        val nameCursor = createCursor(
            name = "conversa.zip",
            size = zipBytes.size.toLong()
        )

        every { resolver.getType(uri) } returns "application/zip"
        every { resolver.query(any(), any(), any(), any(), any()) } returns nameCursor
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(zipBytes)

        val result = runBlocking {
            extractor.extractText(DocumentSource("content://zip/conversa.zip"))
        }

        assertEquals("Mensagem exportada", result)
    }

    @Test
    fun `deve rejeitar formato nao suportado`() {
        val extractor = createExtractor()
        val nameCursor = createCursor(name = "dados.bin", size = 10L)

        every { resolver.getType(uri) } returns "application/octet-stream"
        every { resolver.query(any(), any(), any(), any(), any()) } returns nameCursor

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                extractor.extractText(DocumentSource("content://binary/dados.bin"))
            }
        }
    }

    @Test
    fun `deve rejeitar ZIP sem arquivo TXT`() {
        val extractor = createExtractor()
        val zipBytes = createZip("dados.json", "{}")
        val nameCursor = createCursor(
            name = "dados.zip",
            size = zipBytes.size.toLong()
        )

        every { resolver.getType(uri) } returns "application/zip"
        every { resolver.query(any(), any(), any(), any(), any()) } returns nameCursor
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(zipBytes)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                extractor.extractText(DocumentSource("content://zip/dados.zip"))
            }
        }
    }

    private fun createExtractor(): AndroidDocumentExtractor =
        AndroidDocumentExtractor(context, Dispatchers.Unconfined)

    private fun createCursor(name: String, size: Long): Cursor = mockk {
        every { moveToFirst() } returns true
        every { getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { getColumnIndex(OpenableColumns.SIZE) } returns 1
        every { getString(0) } returns name
        every { isNull(1) } returns false
        every { getLong(1) } returns size
        every { close() } returns Unit
    }

    private fun createZip(entryName: String, content: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
