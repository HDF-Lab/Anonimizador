package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InputDocumentCard(
    inputText: String,
    isLargeInput: Boolean,
    isProcessing: Boolean,
    onInputTextChanged: (String) -> Unit,
    onPickFileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPickFileClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir arquivo TXT, PDF ou ZIP")
            }

            HorizontalDivider()

            if (isLargeInput) {
                Text(
                    text = "Documento carregado",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "${inputText.length} caracteres",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "A edição manual foi desativada " +
                            "para manter a fluidez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 220.dp),
                    label = {
                        Text("Texto original")
                    },
                    placeholder = {
                        Text(
                            "Cole aqui o texto que deseja anonimizar..."
                        )
                    },
                    supportingText = {
                        Text("${inputText.length} caracteres")
                    }
                )
            }

            Text(
                text = "O processamento é realizado pelo aplicativo.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}







