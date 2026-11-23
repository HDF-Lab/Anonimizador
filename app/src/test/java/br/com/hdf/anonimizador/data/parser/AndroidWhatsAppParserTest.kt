package br.com.hdf.anonimizador.data.parser

import br.com.hdf.anonimizador.domain.model.PiiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidWhatsAppParserTest {

    private lateinit var parser: AndroidWhatsAppParser

    @Before
    fun setUp() {
        parser = AndroidWhatsAppParser()
    }

    @Test
    fun `deve reconhecer arquivo exportado do WhatsApp`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!
            01/01/2026 10:31 - João: Tudo bem?
        """.trimIndent()

        assertTrue(parser.canParse(text))
    }

    @Test
    fun `nao deve reconhecer texto comum`() {
        val text = """
            Olá, tudo bem?
            Este é um texto comum.
        """.trimIndent()

        assertFalse(parser.canParse(text))
    }

    @Test
    fun `nao deve reconhecer texto vazio`() {
        assertFalse(parser.canParse(""))
        assertFalse(parser.canParse("   "))
    }

    @Test
    fun `deve normalizar mensagens preservando participantes`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!
            01/01/2026 10:31 - João: Tudo bem?
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertTrue(result.isWhatsAppFormat)

        assertEquals(
            """
                01/01/2026 10:30 - Ana: Olá!
                01/01/2026 10:31 - João: Tudo bem?
            """.trimIndent(),
            result.normalizedText
        )
    }

    @Test
    fun `deve gerar participantMatches para cada ocorrencia de participante`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!
            01/01/2026 10:31 - João: Tudo bem?
            01/01/2026 10:32 - Ana: Sim!
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertEquals(3, result.participantMatches.size)

        assertEquals(
            listOf("Ana", "João", "Ana"),
            result.participantMatches.map { it.originalValue }
        )

        assertTrue(
            result.participantMatches.all {
                it.type == PiiType.PARTICIPANTE_WHATSAPP
            }
        )
    }

    @Test
    fun `deve calcular indices corretos dos participantes`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!
            01/01/2026 10:31 - João: Tudo bem?
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        result.participantMatches.forEach { match ->
            assertEquals(
                match.originalValue,
                result.normalizedText.substring(
                    match.startIndex,
                    match.endIndex
                )
            )
        }
    }

    @Test
    fun `deve preservar mensagens com multiplas linhas`() {
        val text = """
            01/01/2026 10:30 - Ana: Primeira linha
            Segunda linha
            Terceira linha
            01/01/2026 10:31 - João: Outra mensagem
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertTrue(
            result.normalizedText.contains(
                "Ana: Primeira linha\nSegunda linha\nTerceira linha"
            )
        )

        assertEquals(2, result.participantMatches.size)
    }

    @Test
    fun `deve manter nomes com espacos`() {
        val text = """
            01/01/2026 10:30 - Maria Silva: Olá!
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertEquals(
            "Maria Silva",
            result.participantMatches.single().originalValue
        )
    }

    @Test
    fun `deve retornar texto normalizado quando nao for WhatsApp`() {
        val text = "  Texto comum sem formato WhatsApp  "

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertFalse(result.isWhatsAppFormat)
        assertEquals("Texto comum sem formato WhatsApp", result.normalizedText)
        assertTrue(result.participantMatches.isEmpty())
        assertEquals(0, result.removedSystemLinesCount)
    }

    @Test
    fun `deve ignorar linhas em branco entre mensagens`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!

            01/01/2026 10:31 - João: Tudo bem?
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertEquals(2, result.participantMatches.size)
        assertTrue(result.normalizedText.contains("Ana: Olá!"))
        assertTrue(result.normalizedText.contains("João: Tudo bem?"))
    }

    @Test
    fun `deve manter texto de mensagem sem alterar o conteudo`() {
        val text = """
            01/01/2026 10:30 - Ana: CPF 123.456.789-00
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertTrue(
            result.normalizedText.contains("CPF 123.456.789-00")
        )
    }

    @Test
    fun `deve manter ocorrencias repetidas do mesmo participante`() {
        val text = """
            01/01/2026 10:30 - Ana: Olá!
            01/01/2026 10:31 - Ana: Tudo bem?
        """.trimIndent()

        val result = kotlinx.coroutines.runBlocking {
            parser.parseAndClean(text)
        }

        assertEquals(2, result.participantMatches.size)
        assertEquals(
            listOf("Ana", "Ana"),
            result.participantMatches.map { it.originalValue }
        )
    }
}
