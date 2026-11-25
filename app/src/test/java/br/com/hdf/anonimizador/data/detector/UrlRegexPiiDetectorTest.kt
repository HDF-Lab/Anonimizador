package br.com.hdf.anonimizador.data.detector

import br.com.hdf.anonimizador.domain.model.PiiType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrlRegexPiiDetectorTest {

    private lateinit var detector: UrlRegexPiiDetector

    @Before
    fun setUp() {
        detector = UrlRegexPiiDetector()
    }

    @Test
    fun `detecta URLs http https e www preservando pontuacao externa`() = runTest {
        val text = "Veja https://example.com/path?q=1. e www.example.org!"

        val matches = detector.detect(text, setOf(PiiType.LINK))

        assertEquals(
            listOf("https://example.com/path?q=1", "www.example.org"),
            matches.map { it.originalValue }
        )
        assertTrue(matches.all { it.type == PiiType.LINK && it.isValidFor(text) })
    }

    @Test
    fun `nao detecta links quando o tipo estiver desabilitado`() = runTest {
        val matches = detector.detect(
            "https://example.com",
            setOf(PiiType.EMAIL)
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `detecta URI geo com coordenadas`() = runTest {
        val text = "geo:-23.5505,-46.6333"

        val matches = detector.detect(text, setOf(PiiType.LINK))

        assertEquals(text, matches.single().originalValue)
        assertTrue(matches.single().isValidFor(text))
    }
}
