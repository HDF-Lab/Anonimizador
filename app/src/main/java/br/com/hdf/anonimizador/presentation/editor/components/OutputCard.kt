package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import br.com.hdf.anonimizador.R
import androidx.compose.ui.res.pluralStringResource

private const val MAX_PREVIEW_CHARS = 15_000

@Composable
fun OutputCard(
    sanitizedText: String,
    totalEntities: Int,
    removedNoiseCount: Int,
    manuallyRemovedCount: Int,
    redactionMarker: String,
    isLargeOutput: Boolean,
    isProcessing: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectedReplacementChanged: (String, String, Int) -> Unit,
    onReplaceFirst: (String, String) -> Unit,
    onReplaceAll: (String, String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    var selectedTextField by remember(sanitizedText) {
        mutableStateOf(TextFieldValue(text = sanitizedText))
    }
    var searchText by remember { mutableStateOf("") }
    val preview = sanitizedText.take(MAX_PREVIEW_CHARS)
    val detectedCountLabel = pluralStringResource(
        R.plurals.detected_data_count,
        totalEntities,
        totalEntities
    )
    val manuallyRedactedCountLabel = pluralStringResource(
        R.plurals.manual_redaction_count,
        manuallyRemovedCount,
        manuallyRemovedCount
    )

    fun publishSelectedReplacement(
        textField: TextFieldValue
    ) {
        val selection = textField.selection
        val selectedText = if (selection.collapsed) {
            ""
        } else {
            textField.text.substring(selection.min, selection.max)
        }
        val occurrenceIndex = countOccurrencesBefore(
            text = sanitizedText,
            target = selectedText,
            endIndex = selection.min
        )
        onSelectedReplacementChanged(selectedText, redactionMarker, occurrenceIndex)
    }

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Resultado anonimizado",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.anonymized_result_counts,
                            detectedCountLabel,
                            manuallyRedactedCountLabel
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = canUndo && !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Desfazer")
                }
                OutlinedButton(
                    onClick = onRedo,
                    enabled = canRedo && !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refazer")
                }
            }

            if (removedNoiseCount > 0) {
                Text(
                    text = "$removedNoiseCount itens automáticos removidos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (manuallyRemovedCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.manually_removed_count,
                        manuallyRemovedCount,
                        manuallyRemovedCount
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.privacy_output_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.privacy_manual_replace_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLargeOutput) {
                Text(
                    text = "Localizar e substituir atua no resultado completo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Localizar no resultado") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onReplaceFirst(searchText, redactionMarker)
                        },
                        enabled = !isProcessing &&
                                searchText.isNotEmpty() &&
                                searchText != redactionMarker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ocultar primeira")
                    }
                    Button(
                        onClick = {
                            onReplaceAll(searchText, redactionMarker)
                        },
                        enabled = !isProcessing &&
                                searchText.isNotEmpty() &&
                                searchText != redactionMarker,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ocultar todas")
                    }
                }
            } else {
                Text(
                    text = "Selecione no resultado abaixo o trecho que deseja ocultar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (sanitizedText.isBlank() && manuallyRemovedCount > 0) {
                    Text(
                        text = stringResource(R.string.manual_result_fully_removed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                } else if (sanitizedText.isBlank() && (canUndo || canRedo)) {
                    Text(
                        text = stringResource(R.string.manual_result_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                } else if (isLargeOutput) {
                    SelectionContainer {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    BasicTextField(
                        value = selectedTextField,
                        onValueChange = {
                            selectedTextField = it
                            publishSelectedReplacement(it)
                        },
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }

            if (isLargeOutput) {
                Text(
                    text = "Prévia limitada a 15.000 caracteres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun countOccurrencesBefore(
    text: String,
    target: String,
    endIndex: Int
): Int {
    if (target.isEmpty()) return 0

    var count = 0
    var searchFrom = 0
    val limit = endIndex.coerceIn(0, text.length)

    while (searchFrom + target.length <= limit) {
        val matchIndex = text.indexOf(target, searchFrom)
        if (matchIndex < 0 || matchIndex + target.length > limit) break
        count++
        searchFrom = matchIndex + target.length
    }

    return count
}
