package br.com.hdf.anonimizador.presentation.editor

import br.com.hdf.anonimizador.data.masking.SafeTextMasker
import br.com.hdf.anonimizador.domain.detector.DefaultSpanConsolidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractionException
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.model.WhatsAppParseResult
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser
import br.com.hdf.anonimizador.domain.usecase.AnonymizeContentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnonymizerViewModelTest {

    private lateinit var dispatcher: CoroutineDispatcher

    @Before
    fun setUp() {
        dispatcher = Dispatchers.Unconfined
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deve anonimizar texto apos debounce`() {
        val viewModel = createViewModel()

        viewModel.onInputTextChanged("CPF: 529.982.247-25")

        runBlocking {
            kotlinx.coroutines.delay(400)
        }

        assertFalse(viewModel.uiState.value.isProcessing)
        assertEquals("CPF: [CPF_1]", viewModel.uiState.value.sanitizedText)
        assertEquals(mapOf(PiiType.CPF to 1), viewModel.uiState.value.entityCounts)
    }

    @Test
    fun `deve limpar estado da sessao`() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputTextChanged("ana@example.com")
        advanceUntilIdle()

        viewModel.clearSession()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertEquals("", state.sanitizedText)
        assertTrue(state.entityCounts.isEmpty())
        assertFalse(state.isProcessing)
    }

    @Test
    fun `deve reprocessar ao trocar tipo habilitado`() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputTextChanged("ana@example.com")
        advanceUntilIdle()

        viewModel.onTogglePiiType(PiiType.EMAIL)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.enabledTypes.contains(PiiType.EMAIL))
        assertEquals("ana@example.com", viewModel.uiState.value.sanitizedText)
    }

    @Test
    fun `deve atualizar texto apos extracao de arquivo`() = runTest {
        val extractor = FakeDocumentExtractor("Texto extraído")
        val viewModel = createViewModel(extractor)

        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        assertEquals("Texto extraído", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun `deve apresentar erro quando arquivo falhar`() = runTest {
        val extractor = FakeDocumentExtractor(error = DocumentExtractionException.UnsupportedFormat())
        val viewModel = createViewModel(extractor)

        viewModel.onFileSelected(DocumentSource("content://document/invalido.txt"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProcessing)
        assertEquals(
            "Formato de arquivo não suportado. Use TXT, PDF ou ZIP.",
            viewModel.uiState.value.errorMessage
        )
    }

    private fun createViewModel(
        extractor: DocumentExtractor = FakeDocumentExtractor()
    ): AnonymizerViewModel {
        val detector = object : PiiDetector {
            override val priority: Int = 100

            override suspend fun detect(
                text: String,
                enabledTypes: Set<PiiType>
            ): List<PiiMatch> {
                if (PiiType.CPF in enabledTypes && text.contains("529.982.247-25")) {
                    val start = text.indexOf("529.982.247-25")
                    return listOf(PiiMatch(start, start + 14, "529.982.247-25", PiiType.CPF))
                }
                if (PiiType.EMAIL in enabledTypes && text.contains("ana@example.com")) {
                    val start = text.indexOf("ana@example.com")
                    return listOf(PiiMatch(start, start + 15, "ana@example.com", PiiType.EMAIL))
                }
                return emptyList()
            }
        }

        val parser = object : WhatsAppParser {
            override fun canParse(rawText: String): Boolean = false
            override suspend fun parseAndClean(rawText: String): WhatsAppParseResult =
                WhatsAppParseResult(false, rawText, 0)
        }

        val useCase = AnonymizeContentUseCase(
            whatsAppParser = parser,
            detectors = setOf(detector),
            spanConsolidator = DefaultSpanConsolidator(),
            textMasker = SafeTextMasker()
        )

        return AnonymizerViewModel(useCase, extractor, dispatcher)
    }

    private class FakeDocumentExtractor(
        private val text: String = "",
        private val error: Exception? = null
    ) : DocumentExtractor {
        override suspend fun extractText(source: DocumentSource): String {
            error?.let { throw it }
            return text
        }
    }
}
