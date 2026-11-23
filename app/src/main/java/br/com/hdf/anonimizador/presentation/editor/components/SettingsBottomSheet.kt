
package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
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
    onEnabledTypesChanged: (Set<PiiType>) -> Unit,
    onMaskingModeChanged: (MaskMode) -> Unit,
    onToggleWhatsAppNoise: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val allTypes = PiiType.entries.toSet()
    val allSelected = enabledTypes.containsAll(allTypes)
    val noneSelected = enabledTypes.isEmpty()

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

            if (isProcessing) {
                Text(
                    text = "Atualizando resultado...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Tipos de dados pessoais
            Text(
                text = "Tipos de dados",
                style = MaterialTheme.typography.titleMedium
            )

            // Ações em lote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        onEnabledTypesChanged(allTypes)
                    },
                    enabled = !allSelected
                ) {
                    Text("Selecionar todos")
                }

                TextButton(
                    onClick = {
                        onEnabledTypesChanged(emptySet())
                    },
                    enabled = !noneSelected
                ) {
                    Text("Desmarcar todos")
                }
            }

            // Seleção individual
            PiiType.entries.forEach { type ->
                val isChecked = type in enabledTypes
                val label = type.toDisplayLabel()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isChecked,
                            role = Role.Checkbox,
                            onValueChange = {
                                onTogglePiiType(type)
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null
                    )

                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider()

            // Modo de anonimização
            Text(
                text = "Modo de anonimização",
                style = MaterialTheme.typography.titleMedium
            )

            MaskMode.entries.forEach { mode ->
                val isSelected = maskingMode == mode

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                onMaskingModeChanged(mode)
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null
                    )

                    Text(
                        text = mode.toDisplayLabel(),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider()

            // Limpeza de ruídos do WhatsApp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = cleanWhatsAppNoise,
                        role = Role.Switch,
                        onValueChange = onToggleWhatsAppNoise
                    )
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
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
