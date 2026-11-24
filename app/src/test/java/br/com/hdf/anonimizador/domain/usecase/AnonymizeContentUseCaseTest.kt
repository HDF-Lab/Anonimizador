package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.data.masking.SafeTextMasker
import br.com.hdf.anonimizador.domain.detector.DefaultSpanConsolidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.model.AnonymizeRequest
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.domain.model.WhatsAppParseResult
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnonymizeContentUseCaseTest {

    private lateinit var parser: FakeWhatsAppParser

    @Before
    fun setUp() {
        parser = FakeWhatsAppParser()
    }

    @Test
    fun `deve retornar resultado vazio sem processar entrada vazia`() = runTest {
        val detector = RecordingDetector()
        val useCase = createUseCase(detector)

        val output = useCase(
            AnonymizeRequest(
                rawText = "   ",
                enabledTypes = setOf(PiiType.CPF)
            )
        )

        assertEquals("", output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
        assertEquals(0, detector.detectCallCount)
        assertEquals(0, parser.parseCallCount)
    }

    @Test
    fun `deve detectar e mascarar somente o tipo habilitado`() = runTest {
        val text = "CPF: 12345678901"
        val match = PiiMatch(
            startIndex = 5,
            endIndex = text.length,
            originalValue = "12345678901",
            type = PiiType.CPF
        )
        val detector = RecordingDetector(listOf(match))
        val useCase = createUseCase(detector)

        val output = useCase(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.CPF),
                mode = MaskMode.IRREVERSIBLE
            )
        )

        assertEquals("CPF: [CPF_1]", output.sanitizedText)
        assertEquals(mapOf(PiiType.CPF to 1), output.entityCounts)
        assertTrue(output.reversalVault.isEmpty())
        assertEquals(setOf(PiiType.CPF), detector.lastEnabledTypes)
    }

    @Test
    fun `deve gerar mapa reversivel no modo reversivel`() = runTest {
        val text = "Email: ana@example.com"
        val originalValue = "ana@example.com"
        val match = PiiMatch(
            startIndex = 7,
            endIndex = text.length,
            originalValue = originalValue,
            type = PiiType.EMAIL
        )
        val useCase = createUseCase(RecordingDetector(listOf(match)))

        val output = useCase(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.EMAIL),
                mode = MaskMode.REVERSIBLE
            )
        )

        assertEquals("Email: [EMAIL_1]", output.sanitizedText)
        assertEquals(
            mapOf("[EMAIL_1]" to originalValue),
            output.reversalVault
        )
    }

    @Test
    fun `deve limpar WhatsApp e mascarar participantes quando habilitado`() = runTest {
        val participantMatch = PiiMatch(
            startIndex = 0,
            endIndex = 4,
            originalValue = "João",
            type = PiiType.PARTICIPANTE_WHATSAPP
        )
        parser = FakeWhatsAppParser(
            canParse = true,
            parseResult = WhatsAppParseResult(
                isWhatsAppFormat = true,
                normalizedText = "João disse: oi",
                removedSystemLinesCount = 2,
                participantMatches = listOf(participantMatch)
            )
        )
        val useCase = createUseCase(RecordingDetector())

        val output = useCase(
            AnonymizeRequest(
                rawText = "texto exportado do WhatsApp",
                enabledTypes = setOf(PiiType.PARTICIPANTE_WHATSAPP),
                cleanWhatsAppNoise = true
            )
        )

        assertEquals("[PARTICIPANTE_WHATSAPP_1] disse: oi", output.sanitizedText)
        assertEquals(mapOf(PiiType.PARTICIPANTE_WHATSAPP to 1), output.entityCounts)
        assertEquals(2, output.removedSystemLinesCount)
        assertEquals(1, parser.parseCallCount)
    }

    @Test
    fun `deve mascarar participantes no texto original quando limpeza de ruido estiver desabilitada`() = runTest {
        val text = "01/01/2026 10:30 - João: João confirmou."
        val participantMatches = listOf(
            PiiMatch(
                startIndex = text.indexOf("João"),
                endIndex = text.indexOf("João") + "João".length,
                originalValue = "João",
                type = PiiType.PARTICIPANTE_WHATSAPP
            ),
            PiiMatch(
                startIndex = text.lastIndexOf("João"),
                endIndex = text.lastIndexOf("João") + "João".length,
                originalValue = "João",
                type = PiiType.PARTICIPANTE_WHATSAPP
            )
        )
        parser = FakeWhatsAppParser(
            canParse = true,
            parseResult = WhatsAppParseResult(
                isWhatsAppFormat = true,
                normalizedText = text,
                removedSystemLinesCount = 0,
                participantMatches = participantMatches
            )
        )

        val output = createUseCase(RecordingDetector())(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.PARTICIPANTE_WHATSAPP),
                cleanWhatsAppNoise = false
            )
        )

        assertEquals(
            "01/01/2026 10:30 - [PARTICIPANTE_WHATSAPP_1]: [PARTICIPANTE_WHATSAPP_1] confirmou.",
            output.sanitizedText
        )
        assertEquals(0, output.removedSystemLinesCount)
        assertEquals(1, parser.parseCallCount)
        assertEquals(false, parser.lastCleanNoise)
    }

    @Test
    fun `deve preservar texto quando WhatsApp nao estiver habilitado para participantes`() = runTest {
        val participantMatch = PiiMatch(
            startIndex = 0,
            endIndex = 4,
            originalValue = "João",
            type = PiiType.PARTICIPANTE_WHATSAPP
        )
        parser = FakeWhatsAppParser(
            canParse = true,
            parseResult = WhatsAppParseResult(
                isWhatsAppFormat = true,
                normalizedText = "João disse: oi",
                removedSystemLinesCount = 1,
                participantMatches = listOf(participantMatch)
            )
        )
        val useCase = createUseCase(RecordingDetector())

        val output = useCase(
            AnonymizeRequest(
                rawText = "texto exportado do WhatsApp",
                enabledTypes = setOf(PiiType.CPF),
                cleanWhatsAppNoise = true
            )
        )

        assertEquals("João disse: oi", output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
        assertEquals(1, output.removedSystemLinesCount)
    }

    @Test
    fun `deve retornar texto original quando nao houver tipos habilitados`() = runTest {
        val text = "123.456.789-00"
        val detector = RecordingDetector(
            listOf(
                PiiMatch(
                    startIndex = 0,
                    endIndex = text.length,
                    originalValue = text,
                    type = PiiType.CPF
                )
            )
        )

        val output = createUseCase(detector)(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = emptySet()
            )
        )

        assertEquals(text, output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
        assertEquals(0, detector.detectCallCount)
    }

    @Test
    fun `deve preservar texto quando nenhum PII for detectado`() = runTest {
        val text = "Texto sem dados pessoais"

        val output = createUseCase(RecordingDetector())(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.CPF, PiiType.EMAIL)
            )
        )

        assertEquals(text, output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
    }

    @Test
    fun `deve ignorar intervalo detectado com conteudo inconsistente`() = runTest {
        val text = "CPF: 12345678901"
        val invalidMatch = PiiMatch(
            startIndex = 5,
            endIndex = 16,
            originalValue = "conteudo-divergente",
            type = PiiType.CPF
        )

        val output = createUseCase(RecordingDetector(listOf(invalidMatch)))(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.CPF)
            )
        )

        assertEquals(text, output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
    }

    @Test
    fun `deve resolver sobreposicoes mantendo apenas o maior intervalo`() = runTest {
        val text = "ana@example.com"
        val smaller = PiiMatch(
            startIndex = 0,
            endIndex = 3,
            originalValue = "ana",
            type = PiiType.NOME_PROPRIO,
            confidence = 0.9f
        )
        val larger = PiiMatch(
            startIndex = 0,
            endIndex = text.length,
            originalValue = text,
            type = PiiType.EMAIL,
            confidence = 0.8f
        )

        val output = createUseCase(RecordingDetector(listOf(smaller, larger)))(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.EMAIL, PiiType.NOME_PROPRIO)
            )
        )

        assertEquals("[EMAIL_1]", output.sanitizedText)
        assertEquals(mapOf(PiiType.EMAIL to 1), output.entityCounts)
    }

    @Test
    fun `deve reutilizar tag para a mesma ocorrencia original`() = runTest {
        val text = "ana@example.com ana@example.com"
        val first = PiiMatch(
            startIndex = 0,
            endIndex = 15,
            originalValue = "ana@example.com",
            type = PiiType.EMAIL
        )
        val second = PiiMatch(
            startIndex = 16,
            endIndex = 31,
            originalValue = "ana@example.com",
            type = PiiType.EMAIL
        )

        val output = createUseCase(RecordingDetector(listOf(second, first)))(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.EMAIL),
                mode = MaskMode.REVERSIBLE
            )
        )

        assertEquals("[EMAIL_1] [EMAIL_1]", output.sanitizedText)
        assertEquals(mapOf(PiiType.EMAIL to 2), output.entityCounts)
        assertEquals(mapOf("[EMAIL_1]" to "ana@example.com"), output.reversalVault)
    }

    @Test
    fun `deve nao limpar WhatsApp quando configuracao estiver desabilitada`() = runTest {
        parser = FakeWhatsAppParser(
            canParse = true,
            parseResult = WhatsAppParseResult(
                isWhatsAppFormat = true,
                normalizedText = "texto limpo",
                removedSystemLinesCount = 5
            )
        )
        val text = "01/01/2026 10:30 - Ana: oi"

        val output = createUseCase(RecordingDetector())(
            AnonymizeRequest(
                rawText = text,
                enabledTypes = setOf(PiiType.CPF),
                cleanWhatsAppNoise = false
            )
        )

        assertEquals(text, output.sanitizedText)
        assertEquals(0, output.removedSystemLinesCount)
        assertEquals(0, parser.parseCallCount)
    }

    @Test
    fun `deve propagar cancelamento do detector`() = runTest {
        val detector = CancellingDetector()

        try {
            createUseCase(detector)(
                AnonymizeRequest(
                    rawText = "Texto para cancelar",
                    enabledTypes = setOf(PiiType.CPF)
                )
            )
            org.junit.Assert.fail("A operacao deveria ter sido cancelada")
        } catch (_: kotlinx.coroutines.CancellationException) {
            assertTrue(true)
        }
    }

    private fun createUseCase(detector: PiiDetector): AnonymizeContentUseCase {
        return AnonymizeContentUseCase(
            whatsAppParser = parser,
            detectors = setOf(detector),
            spanConsolidator = DefaultSpanConsolidator(),
            textMasker = SafeTextMasker()
        )
    }

    private class RecordingDetector(
        private val matches: List<PiiMatch> = emptyList()
    ) : PiiDetector {

        override val priority: Int = 100
        var detectCallCount: Int = 0
            private set
        var lastEnabledTypes: Set<PiiType> = emptySet()
            private set

        override suspend fun detect(
            text: String,
            enabledTypes: Set<PiiType>
        ): List<PiiMatch> {
            detectCallCount++
            lastEnabledTypes = enabledTypes
            return matches
        }
    }

    private class CancellingDetector : PiiDetector {

        override val priority: Int = 100

        override suspend fun detect(
            text: String,
            enabledTypes: Set<PiiType>
        ): List<PiiMatch> {
            throw kotlinx.coroutines.CancellationException("cancelado pelo teste")
        }
    }

    private class FakeWhatsAppParser(
        private val canParse: Boolean = false,
        private val parseResult: WhatsAppParseResult = WhatsAppParseResult(
            isWhatsAppFormat = true,
            normalizedText = "",
            removedSystemLinesCount = 0
        )
    ) : WhatsAppParser {

        var parseCallCount: Int = 0
            private set
        var lastCleanNoise: Boolean? = null
            private set

        override fun canParse(rawText: String): Boolean = canParse

        override suspend fun parseAndClean(
            rawText: String,
            cleanNoise: Boolean
        ): WhatsAppParseResult {
            parseCallCount++
            lastCleanNoise = cleanNoise
            return parseResult
        }
    }
}
