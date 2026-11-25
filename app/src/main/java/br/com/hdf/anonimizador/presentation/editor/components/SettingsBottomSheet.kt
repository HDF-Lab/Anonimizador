
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
import androidx.compose.material3.OutlinedTextField
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
import br.com.hdf.anonimizador.presentation.editor.MAX_MANUAL_REDACTION_MARKER_LENGTH
import br.com.hdf.anonimizador.presentation.editor.utils.toDisplayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    enabledTypes: Set<PiiType>,
    maskingMode: MaskMode,
    cleanWhatsAppNoise: Boolean,
    manualRedactionMarker: String,
    isProcessing: Boolean,
    onSettingsApplied: (Set<PiiType>, MaskMode, Boolean, String) -> Unit,
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
    var pendingManualRedactionMarker by remember(manualRedactionMarker) {
        mutableStateOf(manualRedactionMarker)
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
            pendingCleanWhatsAppNoise,
            pendingManualRedactionMarker
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
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
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
                        text = stringResource(R.string.settings_updating_result),
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
                                text = stringResource(
                                    R.string.settings_data_types_count,
                                    pendingEnabledTypes.size,
                                    allTypes.size
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    if (dataSectionExpanded) {
                                        R.string.collapse_section
                                    } else {
                                        R.string.expand_section
                                    }
                                ),
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
                                    label = {
                                        Text(stringResource(R.string.select_all_button))
                                    }
                                )
                                AssistChip(
                                    onClick = { pendingEnabledTypes = emptySet() },
                                    enabled = !noneSelected,
                                    label = {
                                        Text(stringResource(R.string.deselect_all_button))
                                    }
                                )
                            }

                            PiiType.entries.forEach { type ->
                                val isChecked = type in pendingEnabledTypes
                                val label = stringResource(type.toDisplayLabel())

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
                                    text = stringResource(
                                        R.string.settings_masking_mode,
                                        stringResource(pendingMaskingMode.toDisplayLabel())
                                    ),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(
                                        if (maskingSectionExpanded) {
                                            R.string.collapse_section
                                        } else {
                                            R.string.expand_section
                                        }
                                    ),
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
                                                    text = stringResource(mode.toDisplayLabel()),
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.manual_redaction_settings_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.manual_redaction_settings_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = pendingManualRedactionMarker,
                                    onValueChange = {
                                        pendingManualRedactionMarker =
                                            it.take(MAX_MANUAL_REDACTION_MARKER_LENGTH)
                                    },
                                    enabled = !isProcessing,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = {
                                        Text(stringResource(R.string.manual_redaction_marker_label))
                                    },
                                    supportingText = {
                                        Text(
                                            stringResource(
                                                R.string.manual_redaction_marker_supporting_text,
                                                pendingManualRedactionMarker.length,
                                                MAX_MANUAL_REDACTION_MARKER_LENGTH
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    isError = pendingManualRedactionMarker.isBlank()
                                )
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
                                        text = stringResource(R.string.clean_whatsapp_noise_title),
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
