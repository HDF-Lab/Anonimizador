package br.com.hdf.anonimizador.domain.usecase

import br.com.hdf.anonimizador.domain.model.ManualTextReplacementCommand
import br.com.hdf.anonimizador.domain.model.ManualTextReplacementResult
import br.com.hdf.anonimizador.domain.model.AppliedManualTextReplacementCommand

class ApplyManualTextReplacementsUseCase {

    operator fun invoke(
        text: String,
        commands: List<ManualTextReplacementCommand>
    ): ManualTextReplacementResult {
        var updatedText = text
        val appliedCommands = mutableListOf<AppliedManualTextReplacementCommand>()

        commands.forEach { command ->
            val appliedCommand = command.execute(updatedText)
            if (appliedCommand != null) {
                updatedText = appliedCommand.resultText
                appliedCommands.add(appliedCommand)
            }
        }

        return ManualTextReplacementResult(
            text = updatedText,
            appliedCommands = appliedCommands
        )
    }
}
