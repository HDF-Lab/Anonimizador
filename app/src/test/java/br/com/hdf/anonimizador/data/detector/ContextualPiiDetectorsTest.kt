package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualPiiDetectorsTest {

    @Test
    fun `detecta numero CNH somente em campo identificado`() = runTest {
        val detector = CnhRegexPiiDetector()
        val text = "CNH: 12345678901\nOutro número: 98765432109"

        val matches = detector.detect(text, setOf(PiiType.CNH))

        assertEquals(1, matches.size)
        assertEquals("12345678901", matches.single().originalValue)
        assertTrue(matches.single().isValidFor(text))
        assertTrue(detector.detect(text, emptySet()).isEmpty())
    }

    @Test
    fun `detecta nome em campo explícito sem consumir pontuação ou campo seguinte`() = runTest {
        val detector = NameRegexPiiDetector()
        val text = "Nome completo: Ana Maria da Silva, CPF: 123"

        val matches = detector.detect(text, setOf(PiiType.NOME_PROPRIO))

        assertEquals(1, matches.size)
        assertEquals("Ana Maria da Silva", matches.single().originalValue)
        assertEquals(text.indexOf("Ana Maria"), matches.single().startIndex)
        assertTrue(matches.single().isValidFor(text))
        assertTrue(detector.detect(text, setOf(PiiType.CPF)).isEmpty())
    }

    @Test
    fun `detecta endereço rotulado com logradouro reconhecido`() = runTest {
        val detector = PhysicalAddressRegexPiiDetector()
        val text = "Endereço: Av. Paulista, 1000, São Paulo."

        val matches = detector.detect(text, setOf(PiiType.ENDERECO_FISICO))

        assertEquals(1, matches.size)
        assertEquals("Av. Paulista, 1000, São Paulo", matches.single().originalValue)
        assertTrue(matches.single().isValidFor(text))
    }

    @Test
    fun `nao detecta endereço sem tipo de logradouro reconhecido`() = runTest {
        val detector = PhysicalAddressRegexPiiDetector()
        val matches = detector.detect(
            "Endereço: Centro, São Paulo",
            setOf(PiiType.ENDERECO_FISICO)
        )

        assertTrue(matches.isEmpty())
    }
}
