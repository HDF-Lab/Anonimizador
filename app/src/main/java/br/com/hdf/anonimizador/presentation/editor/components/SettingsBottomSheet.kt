
package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import br.com.hdf.anonimizador.R
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
    onSettingsApplied: (Set<PiiType>, MaskMode, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val allTypes = PiiType.entries.toSet()
    var pendingEnabledTypes by remember(enabledTypes) { mutableStateOf(enabledTypes) }
    var pendingMaskingMode by remember(maskingMode) { mutableStateOf(maskingMode) }
    var pendingCleanWhatsAppNoise by remember(cleanWhatsAppNoise) {
        mutableStateOf(cleanWhatsAppNoise)
    }
    var dataSectionExpanded by remember { mutableStateOf(true) }
    var maskingSectionExpanded by remember { mutableStateOf(false) }
    var whatsappSectionExpanded by remember { mutableStateOf(false) }

    val allSelected = pendingEnabledTypes.containsAll(allTypes)
    val noneSelected = pendingEnabledTypes.isEmpty()

    fun applyAndDismiss() {
        onSettingsApplied(
            pendingEnabledTypes,
            pendingMaskingMode,
            pendingCleanWhatsAppNoise
        )
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::applyAndDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Personalize o que será protegido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Atualizando resultado…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = dataSectionExpanded,
                            role = Role.Button,
                            onValueChange = { dataSectionExpanded = it }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tipos de dados · ${pendingEnabledTypes.size}/${allTypes.size}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (dataSectionExpanded) "−" else "+",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        if (dataSectionExpanded) {
                            Text(
                                text = stringResource(R.string.privacy_pii_types_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { pendingEnabledTypes = allTypes },
                                    enabled = !allSelected,
                                    label = { Text("Selecionar todos") }
                                )
                                AssistChip(
                                    onClick = { pendingEnabledTypes = emptySet() },
                                    enabled = !noneSelected,
                                    label = { Text("Desmarcar") }
                                )
                            }

                            PiiType.entries.forEach { type ->
                                val isChecked = type in pendingEnabledTypes
                                val label = type.toDisplayLabel()

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = isChecked,
                                            role = Role.Checkbox,
                                            onValueChange = {
                                                pendingEnabledTypes =
                                                    if (type in pendingEnabledTypes) {
                                                        pendingEnabledTypes - type
                                                    } else {
                                                        pendingEnabledTypes + type
                                                    }
                                            }
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null
                                        )
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(start = 8.dp),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isChecked) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = maskingSectionExpanded,
                                role = Role.Button,
                                onValueChange = { maskingSectionExpanded = it }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modo de anonimização · ${pendingMaskingMode.toDisplayLabel()}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (maskingSectionExpanded) "−" else "+",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            if (maskingSectionExpanded) {
                                MaskMode.entries.forEach { mode ->
                                    val isSelected = pendingMaskingMode == mode

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = { pendingMaskingMode = mode }
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = null
                                                )
                                                Text(
                                                    text = mode.toDisplayLabel(),
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            Text(
                                                text = stringResource(
                                                    when (mode) {
                                                        MaskMode.IRREVERSIBLE ->
                                                            R.string.privacy_mode_irreversible

                                                        MaskMode.REVERSIBLE ->
                                                            R.string.privacy_mode_reversible
                                                    }
                                                ),
                                                modifier = Modifier.padding(
                                                    start = 48.dp,
                                                    end = 8.dp,
                                                    bottom = 8.dp
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = pendingCleanWhatsAppNoise,
                                    role = Role.Switch,
                                    onValueChange = { pendingCleanWhatsAppNoise = it }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (pendingCleanWhatsAppNoise) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Limpar ruídos do WhatsApp",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.privacy_whatsapp_noise_explanation
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = pendingCleanWhatsAppNoise,
                                    onCheckedChange = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                }
            }
        }
    }
}
