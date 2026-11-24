package br.com.hdf.anonimizador.data.masking

import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafeTextMaskerTest {

    private lateinit var masker: SafeTextMasker

    @Before
    fun setUp() {
        masker = SafeTextMasker()
    }

    @Test
    fun `deve preservar texto quando nao houver matches`() {
        val text = "Texto sem dados pessoais"

        val output = runBlocking {
            masker.mask(text, emptyList(), MaskMode.IRREVERSIBLE)
        }

        assertEquals(text, output.sanitizedText)
        assertTrue(output.entityCounts.isEmpty())
        assertTrue(output.reversalVault.isEmpty())
    }

    @Test
    fun `deve mascarar texto ao redor de um match`() {
        val text = "Ana111.222.333-44"
        val cpf = PiiMatch(3, 17, "111.222.333-44", PiiType.CPF)

        val output = runBlocking {
            masker.mask(text, listOf(cpf), MaskMode.IRREVERSIBLE)
        }

        assertEquals("Ana[CPF_1]", output.sanitizedText)
        assertEquals(mapOf(PiiType.CPF to 1), output.entityCounts)
    }

    @Test
    fun `deve numerar ocorrencias diferentes do mesmo tipo`() {
        val text = "111.222.333-44 555.666.777-88"
        val first = PiiMatch(0, 14, "111.222.333-44", PiiType.CPF)
        val second = PiiMatch(15, 29, "555.666.777-88", PiiType.CPF)

        val output = runBlocking {
            masker.mask(text, listOf(second, first), MaskMode.IRREVERSIBLE)
        }

        assertEquals("[CPF_1] [CPF_2]", output.sanitizedText)
        assertEquals(mapOf(PiiType.CPF to 2), output.entityCounts)
    }

    @Test
    fun `deve reutilizar tag para valor original repetido`() {
        val text = "111.222.333-44 111.222.333-44"
        val first = PiiMatch(0, 14, "111.222.333-44", PiiType.CPF)
        val second = PiiMatch(15, 29, "111.222.333-44", PiiType.CPF)

        val output = runBlocking {
            masker.mask(text, listOf(first, second), MaskMode.REVERSIBLE)
        }

        assertEquals("[CPF_1] [CPF_1]", output.sanitizedText)
        assertEquals(mapOf(PiiType.CPF to 2), output.entityCounts)
        assertEquals(mapOf("[CPF_1]" to "111.222.333-44"), output.reversalVault)
    }

    @Test
    fun `deve rejeitar intervalo fora do texto`() {
        val text = "Ana"
        val invalid = PiiMatch(0, 5, "AnaXX", PiiType.NOME_PROPRIO)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                masker.mask(text, listOf(invalid), MaskMode.IRREVERSIBLE)
            }
        }
    }

    @Test
    fun `deve rejeitar valor original divergente do texto`() {
        val text = "Maria"
        val invalid = PiiMatch(0, 5, "Joana", PiiType.NOME_PROPRIO)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                masker.mask(text, listOf(invalid), MaskMode.IRREVERSIBLE)
            }
        }
    }

    @Test
    fun `deve rejeitar intervalos sobrepostos`() {
        val text = "ana@example.com"
        val first = PiiMatch(0, 3, "ana", PiiType.NOME_PROPRIO)
        val second = PiiMatch(2, 15, "a@example.com", PiiType.EMAIL)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                masker.mask(text, listOf(first, second), MaskMode.IRREVERSIBLE)
            }
        }
    }
}
