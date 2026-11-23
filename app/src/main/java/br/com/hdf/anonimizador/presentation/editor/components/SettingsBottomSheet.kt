package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.presentation.editor.utils.toDisplayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    enabledTypes: Set<PiiType>,
    maskingMode: MaskMode,
    cleanWhatsAppNoise: Boolean,
    isProcessing: Boolean,
    onTogglePiiType: (PiiType) -> Unit,
    onMaskingModeChanged: (MaskMode) -> Unit,
    onToggleWhatsAppNoise: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configurações de anonimização",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Escolha quais informações deseja proteger.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Tipos de dados pessoais.
            Text(
                text = "Tipos de dados",
                style = MaterialTheme.typography.titleMedium
            )

            PiiType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !isProcessing
                        ) {
                            onTogglePiiType(type)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type in enabledTypes,
                        onCheckedChange = null,
                        enabled = !isProcessing
                    )

                    Text(
                        text = type.toDisplayLabel(),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider()

            // Modo de mascaramento.
            Text(
                text = "Modo de anonimização",
                style = MaterialTheme.typography.titleMedium
            )

            MaskMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !isProcessing
                        ) {
                            onMaskingModeChanged(mode)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = maskingMode == mode,
                        onClick = {
                            onMaskingModeChanged(mode)
                        },
                        enabled = !isProcessing
                    )

                    Text(
                        text = mode.toDisplayLabel(),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider()

            // Limpeza de ruídos do WhatsApp.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !isProcessing
                    ) {
                        onToggleWhatsAppNoise(!cleanWhatsAppNoise)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Limpar ruídos do WhatsApp",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Remove linhas de sistema e mensagens automáticas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = cleanWhatsAppNoise,
                    onCheckedChange = null,
                    enabled = !isProcessing
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
