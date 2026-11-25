package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.domain.model.ManualTextReplacementCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApplyManualTextReplacementsUseCaseTest {

    private val useCase = ApplyManualTextReplacementsUseCase()

    @Test
    fun `substitui somente a ocorrencia selecionada`() {
        val command = ManualTextReplacementCommand.ReplaceOccurrence(
            target = "marcador",
            replacement = "removido",
            occurrenceIndex = 1
        )

        val result = useCase("marcador e marcador", listOf(command))

        assertEquals("marcador e removido", result.text)
        assertEquals(listOf(command), result.appliedCommands.map { it.command })
    }

    @Test
    fun `substitui somente a primeira ocorrencia`() {
        val command = ManualTextReplacementCommand.ReplaceOccurrence(
            target = "x",
            replacement = "y",
            occurrenceIndex = 0
        )

        val result = useCase("x x x", listOf(command))

        assertEquals("y x x", result.text)
    }

    @Test
    fun `substitui todas as ocorrencias`() {
        val command = ManualTextReplacementCommand.ReplaceAll(
            target = "x",
            replacement = "y"
        )

        val result = useCase("x x x", listOf(command))

        assertEquals("y y y", result.text)
    }

    @Test
    fun `reaplica operacoes compativeis e descarta as sem correspondencia`() {
        val matchingCommand = ManualTextReplacementCommand.ReplaceAll(
            target = "[EMAIL_1]",
            replacement = "[EMAIL]"
        )
        val missingCommand = ManualTextReplacementCommand.ReplaceAll(
            target = "[CPF_1]",
            replacement = "[CPF]"
        )

        val result = useCase(
            text = "[EMAIL_1] e [EMAIL_1]",
            commands = listOf(matchingCommand, missingCommand)
        )

        assertEquals("[EMAIL] e [EMAIL]", result.text)
        assertEquals(listOf(matchingCommand), result.appliedCommands.map { it.command })
    }

    @Test
    fun `rejeita alvo vazio`() {
        val command = ManualTextReplacementCommand.ReplaceAll(
            target = "",
            replacement = "valor"
        )

        assertThrows(IllegalArgumentException::class.java) {
            useCase("texto", commands = listOf(command))
        }
    }

    @Test
    fun `rejeita indice de ocorrencia negativo`() {
        val command = ManualTextReplacementCommand.ReplaceOccurrence(
            target = "x",
            replacement = "y",
            occurrenceIndex = -1
        )

        assertThrows(IllegalArgumentException::class.java) {
            useCase("x", commands = listOf(command))
        }
    }

    @Test
    fun `desfaz comando preservando o resultado anterior`() {
        val result = useCase(
            text = "CPF [CPF_1] e email [EMAIL_1]",
            commands = listOf(
                ManualTextReplacementCommand.ReplaceAll("[CPF_1]", "[DOCUMENTO]")
            )
        )

        val undone = result.appliedCommands.single().undo(result.text)

        assertEquals("CPF [CPF_1] e email [EMAIL_1]", undone)
    }

    @Test
    fun `desfaz comando de substituir todas as ocorrencias`() {
        val result = useCase(
            text = "x x x",
            commands = listOf(
                ManualTextReplacementCommand.ReplaceAll("x", "valor")
            )
        )

        val undone = result.appliedCommands.single().undo(result.text)

        assertEquals("x x x", undone)
    }

    @Test
    fun `desfaz comandos em ordem inversa mesmo quando os comprimentos mudam`() {
        val result = useCase(
            text = "x x",
            commands = listOf(
                ManualTextReplacementCommand.ReplaceAll("x", "valor"),
                ManualTextReplacementCommand.ReplaceOccurrence("valor", "ok", 1)
            )
        )

        val afterUndoSecond = requireNotNull(
            result.appliedCommands.last().undo(result.text)
        )
        val afterUndoFirst = result.appliedCommands.first().undo(afterUndoSecond)

        assertEquals("valor valor", afterUndoSecond)
        assertEquals("x x", afterUndoFirst)
    }

    @Test
    fun `nao desfaz comando aplicado a um texto diferente`() {
        val result = useCase(
            text = "original",
            commands = listOf(
                ManualTextReplacementCommand.ReplaceOccurrence("original", "alterado", 0)
            )
        )

        val undone = result.appliedCommands.single().undo("texto diferente")

        assertEquals(null, undone)
    }
}
