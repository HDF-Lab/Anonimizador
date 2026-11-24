package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Documento",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            FilledTonalButton(
                onClick = onPickFileClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir arquivo TXT, PDF ou ZIP")
            }

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
                        .heightIn(min = 170.dp, max = 220.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                )
                Text(
                    text = "Processamento local e offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}







