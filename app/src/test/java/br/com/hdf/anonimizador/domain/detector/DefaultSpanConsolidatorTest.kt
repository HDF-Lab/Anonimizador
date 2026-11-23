package br.com.hdf.anonimizador.domain.detector

import br.com.hdf.anonimizador.domain.model.PiiMatch
import br.com.hdf.anonimizador.domain.model.PiiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultSpanConsolidatorTest {

    private lateinit var consolidator: DefaultSpanConsolidator

    private val text = "x".repeat(100)

    @Before
    fun setUp() {
        consolidator = DefaultSpanConsolidator()
    }

    @Test
    fun `deve retornar lista vazia quando nao houver matches`() {
        val result = consolidator.consolidate(
            text = text,
            matches = emptyList()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deve retornar lista vazia quando texto estiver vazio`() {
        val result = consolidator.consolidate(
            text = "",
            matches = listOf(createMatch(0, 1))
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deve preservar um unico match`() {
        val match = createMatch(0, 11)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(match)
        )

        assertEquals(listOf(match), result)
    }

    @Test
    fun `deve preservar matches sem sobreposicao`() {
        val first = createMatch(0, 11, PiiType.CPF)
        val second = createMatch(15, 30, PiiType.EMAIL)
        val third = createMatch(35, 50, PiiType.TELEFONE_BR)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(first, second, third)
        )

        assertEquals(listOf(first, second, third), result)
    }

    @Test
    fun `deve preservar intervalos adjacentes`() {
        val first = createMatch(0, 10)
        val second = createMatch(10, 20)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(first, second)
        )

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun `deve escolher o match com maior intervalo`() {
        val smaller = createMatch(2, 8, PiiType.EMAIL)
        val larger = createMatch(0, 12, PiiType.CPF)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(smaller, larger)
        )

        assertEquals(listOf(larger), result)
    }

    @Test
    fun `deve escolher maior intervalo independentemente da confianca`() {
        val smaller = createMatch(
            start = 0,
            end = 8,
            confidence = 1.0f
        )

        val larger = createMatch(
            start = 0,
            end = 12,
            confidence = 0.5f
        )

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(smaller, larger)
        )

        assertEquals(listOf(larger), result)
    }

    @Test
    fun `deve usar maior confianca quando intervalos tiverem mesmo tamanho`() {
        val lessConfident = createMatch(
            start = 0,
            end = 11,
            confidence = 0.7f
        )

        val moreConfident = createMatch(
            start = 1,
            end = 12,
            confidence = 0.9f
        )

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(lessConfident, moreConfident)
        )

        assertEquals(listOf(moreConfident), result)
    }

    @Test
    fun `deve usar maior prioridade quando comprimento e confianca forem iguais`() {
        val lowerPriority = createMatch(
            start = 0,
            end = 11,
            confidence = 0.9f,
            priority = 50
        )

        val higherPriority = createMatch(
            start = 1,
            end = 12,
            confidence = 0.9f,
            priority = 100
        )

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(lowerPriority, higherPriority)
        )

        assertEquals(listOf(higherPriority), result)
    }

    @Test
    fun `deve usar ordinal do tipo como criterio de desempate`() {
        val first = createMatch(
            start = 0,
            end = 11,
            type = PiiType.CPF
        )

        val second = createMatch(
            start = 0,
            end = 11,
            type = PiiType.EMAIL
        )

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(second, first)
        )

        assertEquals(listOf(first), result)
    }

    @Test
    fun `deve descartar intervalos menores aninhados`() {
        val outer = createMatch(0, 20)
        val middle = createMatch(3, 15)
        val inner = createMatch(5, 10)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(inner, middle, outer)
        )

        assertEquals(listOf(outer), result)
    }

    @Test
    fun `deve preservar matches nao conflitantes em sobreposicao encadeada`() {
        val first = createMatch(0, 5)
        val middle = createMatch(4, 9)
        val last = createMatch(8, 13)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(first, middle, last)
        )

        assertEquals(listOf(first, last), result)
    }

    @Test
    fun `deve descartar matches menores que conflitam com um maior`() {
        val first = createMatch(0, 5)
        val second = createMatch(5, 10)
        val larger = createMatch(0, 10)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(first, second, larger)
        )

        assertEquals(listOf(larger), result)
    }

    @Test
    fun `deve ignorar intervalos invalidos`() {
        val negativeStart = createMatch(-1, 5)
        val zeroLength = createMatch(5, 5)
        val inverted = createMatch(10, 5)
        val outsideText = createMatch(95, 105)
        val valid = createMatch(15, 25)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(
                negativeStart,
                zeroLength,
                inverted,
                outsideText,
                valid
            )
        )

        assertEquals(listOf(valid), result)
    }

    @Test
    fun `deve ignorar match cujo valor original nao corresponde ao texto`() {
        val invalid = createMatch(
            start = 0,
            end = 5,
            originalValue = "abcde"
        )

        val valid = createMatch(10, 15)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(invalid, valid)
        )

        assertEquals(listOf(valid), result)
    }

    @Test
    fun `deve retornar matches ordenados pela posicao inicial`() {
        val first = createMatch(0, 5)
        val second = createMatch(10, 15)
        val third = createMatch(20, 25)

        val result = consolidator.consolidate(
            text = text,
            matches = listOf(third, first, second)
        )

        assertEquals(listOf(first, second, third), result)
    }

    private fun createMatch(
        start: Int,
        end: Int,
        type: PiiType = PiiType.CPF,
        confidence: Float = 1.0f,
        priority: Int = 0,
        originalValue: String = "x".repeat(
            (end - start).coerceAtLeast(0)
        )
    ) = PiiMatch(
        startIndex = start,
        endIndex = end,
        originalValue = originalValue,
        type = type,
        confidence = confidence,
        detectorPriority = priority
    )
}
