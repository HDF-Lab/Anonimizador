package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val MAX_PREVIEW_CHARS = 15_000

@Composable
fun OutputCard(
    sanitizedText: String,
    totalEntities: Int,
    removedSystemLinesCount: Int,
    isLargeOutput: Boolean
) {
    val preview = if (isLargeOutput) {
        sanitizedText.take(MAX_PREVIEW_CHARS) +
                "\n\nPrévia limitada. Salve o arquivo " +
                "para obter o resultado completo."
    } else {
        sanitizedText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$totalEntities dados identificados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (removedSystemLinesCount > 0) {
                Text(
                    text = "$removedSystemLinesCount linhas de sistema removidas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            SelectionContainer {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isLargeOutput) {
                Text(
                    text = "O texto completo será incluído no arquivo TXT.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

