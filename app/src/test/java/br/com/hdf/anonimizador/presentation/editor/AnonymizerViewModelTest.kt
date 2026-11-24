package br.com.hdf.anonimizador.presentation.editor

import br.com.hdf.anonimizador.data.masking.SafeTextMasker
import br.com.hdf.anonimizador.domain.detector.DefaultSpanConsolidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractionException
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.ChatParseResult
import br.com.hdf.anonimizador.domain.model.ChatPlatform
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.parser.ChatExportParser
import br.com.hdf.anonimizador.domain.usecase.ApplyManualTextReplacementsUseCase
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
        assertEquals("[REMOVIDO]", state.manualRedactionMarker)
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
    fun `altera marcador para vetos futuros sem reprocessar resultado`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("Texto de origem")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()
        val originalResult = viewModel.uiState.value.sanitizedText

        viewModel.onSettingsApplied(
            enabledTypes = viewModel.uiState.value.enabledTypes,
            maskingMode = viewModel.uiState.value.maskingMode,
            cleanWhatsAppNoise = viewModel.uiState.value.cleanWhatsAppNoise,
            manualRedactionMarker = " [CONFIDENCIAL] "
        )

        assertEquals("[CONFIDENCIAL]", viewModel.uiState.value.manualRedactionMarker)

        assertEquals(originalResult, viewModel.uiState.value.sanitizedText)
        assertFalse(viewModel.uiState.value.isProcessing)

        viewModel.replaceSelectedResult(
            target = "Texto",
            replacement = viewModel.uiState.value.manualRedactionMarker,
            occurrenceIndex = 0
        )
        advanceUntilIdle()
        assertEquals("[CONFIDENCIAL] de origem", viewModel.uiState.value.sanitizedText)

        viewModel.onSettingsApplied(
            enabledTypes = viewModel.uiState.value.enabledTypes,
            maskingMode = viewModel.uiState.value.maskingMode,
            cleanWhatsAppNoise = viewModel.uiState.value.cleanWhatsAppNoise,
            manualRedactionMarker = " "
        )

        assertEquals("[CONFIDENCIAL]", viewModel.uiState.value.manualRedactionMarker)

        viewModel.onSettingsApplied(
            enabledTypes = viewModel.uiState.value.enabledTypes,
            maskingMode = viewModel.uiState.value.maskingMode,
            cleanWhatsAppNoise = viewModel.uiState.value.cleanWhatsAppNoise,
            manualRedactionMarker = "x".repeat(50)
        )

        assertEquals(40, viewModel.uiState.value.manualRedactionMarker.length)
    }

    @Test
    fun `deve preservar substituicao manual quando o trecho continua no resultado`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("CPF: 529.982.247-25")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[DOCUMENTO]", 0)
        advanceUntilIdle()
        assertEquals("CPF: [DOCUMENTO]", viewModel.uiState.value.sanitizedText)

        viewModel.onMaskingModeChanged(MaskMode.REVERSIBLE)
        advanceUntilIdle()

        assertEquals("CPF: [DOCUMENTO]", viewModel.uiState.value.sanitizedText)
    }

    @Test
    fun `deve descartar substituicao manual quando o trecho nao existe apos reprocessar`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("CPF: 529.982.247-25")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[DOCUMENTO]", 0)
        advanceUntilIdle()
        viewModel.onTogglePiiType(PiiType.CPF)
        advanceUntilIdle()

        assertEquals("CPF: 529.982.247-25", viewModel.uiState.value.sanitizedText)

        viewModel.onTogglePiiType(PiiType.CPF)
        advanceUntilIdle()

        assertEquals("CPF: [CPF_1]", viewModel.uiState.value.sanitizedText)
    }

    @Test
    fun `deve substituir todas as ocorrencias informadas no resultado`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("ana@example.com")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceAllInResult("[EMAIL_1]", "[EMAIL]")
        advanceUntilIdle()

        assertEquals("[EMAIL]", viewModel.uiState.value.sanitizedText)
    }

    @Test
    fun `conta remocoes manuais e atualiza a contagem ao desfazer e refazer`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("textotexto")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceAllInResult("texto", "")
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.sanitizedText)
        assertEquals(2, viewModel.uiState.value.manuallyRemovedCount)

        viewModel.undoManualReplacement()
        advanceUntilIdle()
        assertEquals("textotexto", viewModel.uiState.value.sanitizedText)
        assertEquals(0, viewModel.uiState.value.manuallyRemovedCount)

        viewModel.redoManualReplacement()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.sanitizedText)
        assertEquals(2, viewModel.uiState.value.manuallyRemovedCount)
    }

    @Test
    fun `deve desfazer e refazer substituicao manual`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("CPF: 529.982.247-25")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[DOCUMENTO]", 0)
        advanceUntilIdle()
        assertEquals("CPF: [DOCUMENTO]", viewModel.uiState.value.sanitizedText)
        assertEquals(1, viewModel.uiState.value.manuallyRemovedCount)
        assertTrue(viewModel.uiState.value.canUndoManualReplacement)
        assertFalse(viewModel.uiState.value.canRedoManualReplacement)

        viewModel.undoManualReplacement()
        advanceUntilIdle()
        assertEquals("CPF: [CPF_1]", viewModel.uiState.value.sanitizedText)
        assertFalse(viewModel.uiState.value.canUndoManualReplacement)
        assertTrue(viewModel.uiState.value.canRedoManualReplacement)

        viewModel.redoManualReplacement()
        advanceUntilIdle()
        assertEquals("CPF: [DOCUMENTO]", viewModel.uiState.value.sanitizedText)
        assertTrue(viewModel.uiState.value.canUndoManualReplacement)
        assertFalse(viewModel.uiState.value.canRedoManualReplacement)
    }

    @Test
    fun `nova substituicao limpa a pilha de refazer`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("CPF: 529.982.247-25, CPF: 529.982.247-25")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[DOCUMENTO]", 0)
        advanceUntilIdle()
        viewModel.undoManualReplacement()
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[IDENTIFICADOR]", 0)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sanitizedText.contains("[IDENTIFICADOR]"))
        assertTrue(viewModel.uiState.value.canUndoManualReplacement)
        assertFalse(viewModel.uiState.value.canRedoManualReplacement)
    }

    @Test
    fun `deve limpar substituicoes manuais quando a entrada mudar`() = runTest {
        val viewModel = createViewModel(
            extractor = FakeDocumentExtractor("CPF: 529.982.247-25")
        )
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        viewModel.replaceSelectedResult("[CPF_1]", "[DOCUMENTO]", 0)
        advanceUntilIdle()
        assertEquals("CPF: [DOCUMENTO]", viewModel.uiState.value.sanitizedText)

        viewModel.onInputTextChanged("Outro texto")
        assertFalse(viewModel.uiState.value.canUndoManualReplacement)
        assertFalse(viewModel.uiState.value.canRedoManualReplacement)
        viewModel.onFileSelected(DocumentSource("content://document/test.txt"))
        advanceUntilIdle()

        assertEquals("CPF: [CPF_1]", viewModel.uiState.value.sanitizedText)
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

        val parser = object : ChatExportParser {
            override val platform: ChatPlatform = ChatPlatform.WHATSAPP
            override val participantPiiType: PiiType? = PiiType.PARTICIPANTE_WHATSAPP

            override fun canParse(rawText: String): Boolean = false
            override suspend fun parse(
                rawText: String,
                cleanNoise: Boolean
            ): ChatParseResult =
                ChatParseResult(false, rawText, 0)
        }

        val useCase = AnonymizeContentUseCase(
            chatExportParsers = setOf(parser),
            detectors = setOf(detector),
            spanConsolidator = DefaultSpanConsolidator(),
            textMasker = SafeTextMasker()
        )

        return AnonymizerViewModel(
            anonymizeContentUseCase = useCase,
            applyManualTextReplacementsUseCase = ApplyManualTextReplacementsUseCase(),
            documentExtractor = extractor,
            computationDispatcher = dispatcher
        )
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
