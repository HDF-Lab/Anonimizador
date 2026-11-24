package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrazilRegexPiiDetectorTest {

    private lateinit var detector: BrazilRegexPiiDetector

    @Before
    fun setUp() {
        detector = BrazilRegexPiiDetector()
    }

    @Test
    fun `deve detectar CPF valido e calcular indice exato`() = runTest {
        val text = "CPF: 529.982.247-25"

        val matches = detector.detect(text, setOf(PiiType.CPF))

        assertEquals(1, matches.size)
        assertEquals(PiiType.CPF, matches[0].type)
        assertEquals("529.982.247-25", matches[0].originalValue)
        assertEquals(text.indexOf("529.982.247-25"), matches[0].startIndex)
        assertEquals(text.length, matches[0].endIndex)
    }

    @Test
    fun `deve rejeitar CPF invalido`() = runTest {
        val matches = detector.detect(
            "CPF: 111.111.111-11",
            setOf(PiiType.CPF)
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `deve detectar CNPJ valido`() = runTest {
        val matches = detector.detect(
            "CNPJ: 11.444.777/0001-61",
            setOf(PiiType.CNPJ)
        )

        assertEquals(1, matches.size)
        assertEquals(PiiType.CNPJ, matches[0].type)
        assertEquals("11.444.777/0001-61", matches[0].originalValue)
    }

    @Test
    fun `deve detectar email telefone IP e chave PIX`() = runTest {
        val text = "ana@example.com (11) 91234-5678 192.168.0.1 123e4567-e89b-12d3-a456-426614174000"

        val matches = detector.detect(
            text,
            setOf(PiiType.EMAIL, PiiType.TELEFONE_BR, PiiType.ENDERECO_IP, PiiType.CHAVE_PIX)
        )

        assertEquals(setOf(PiiType.EMAIL, PiiType.TELEFONE_BR, PiiType.ENDERECO_IP, PiiType.CHAVE_PIX), matches.map { it.type }.toSet())
        assertTrue(matches.all { it.isValidFor(text) })
    }

    @Test
    fun `deve validar cartao com Luhn`() = runTest {
        val valid = detector.detect("4111 1111 1111 1111", setOf(PiiType.CARTAO_CREDITO))
        val invalid = detector.detect("4111 1111 1111 1112", setOf(PiiType.CARTAO_CREDITO))

        assertEquals(1, valid.size)
        assertTrue(invalid.isEmpty())
    }

    @Test
    fun `deve detectar placa antiga e mercosul`() = runTest {
        val text = "ABC1234 ABC1D23"

        val matches = detector.detect(text, setOf(PiiType.PLACA_VEICULO))

        assertEquals(2, matches.size)
        assertTrue(matches.all { it.type == PiiType.PLACA_VEICULO })
        assertTrue(matches.all { it.isValidFor(text) })
    }

    @Test
    fun `deve ignorar tipos desabilitados`() = runTest {
        val text = "529.982.247-25 ana@example.com"

        val matches = detector.detect(text, setOf(PiiType.EMAIL))

        assertEquals(1, matches.size)
        assertEquals(PiiType.EMAIL, matches[0].type)
    }

    @Test
    fun `deve retornar lista vazia para entrada vazia ou tipos vazios`() = runTest {
        assertTrue(detector.detect("", setOf(PiiType.CPF)).isEmpty())
        assertTrue(detector.detect("529.982.247-25", emptySet()).isEmpty())
    }
}
