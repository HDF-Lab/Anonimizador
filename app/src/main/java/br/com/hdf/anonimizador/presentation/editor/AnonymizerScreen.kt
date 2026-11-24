
package br.com.hdf.anonimizador.presentation.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.hdf.anonimizador.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.domain.model.MaskMode
import br.com.hdf.anonimizador.domain.model.PiiType
import br.com.hdf.anonimizador.presentation.editor.components.EmptyResultCard
import br.com.hdf.anonimizador.presentation.editor.components.ErrorCard
import br.com.hdf.anonimizador.presentation.editor.components.InputDocumentCard
import br.com.hdf.anonimizador.presentation.editor.components.OutputCard
import br.com.hdf.anonimizador.presentation.editor.components.ProcessingCard
import br.com.hdf.anonimizador.presentation.editor.components.ResultActionsBar
import br.com.hdf.anonimizador.presentation.editor.components.SettingsBottomSheet
import br.com.hdf.anonimizador.presentation.editor.utils.writeTextToUri
import br.com.hdf.anonimizador.ui.theme.AnonimizadorOfflineTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PREVIEW_CHARS = 15_000
private const val MAX_CLIPBOARD_CHARS = 250_000

/**
 * Tela principal do anonimizador.
 *
 * Responsabilidades:
 * - Observar o estado do ViewModel.
 * - Configurar os seletores de arquivo.
 * - Encaminhar eventos para o ViewModel.
 * - Disponibilizar as ações de copiar e salvar.
 */
@Composable
fun AnonymizerScreen(
    viewModel: AnonymizerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Gerenciador de área de transferência do Android.
    val clipboardManager = context.getSystemService(
        Context.CLIPBOARD_SERVICE
    ) as ClipboardManager

    // Seleção do documento de entrada.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        uri?.let { selectedUri ->

            // Mantém a permissão de leitura para uso posterior.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            viewModel.onFileSelected(
                DocumentSource(selectedUri.toString())
            )
        }
    }

    // Seleção do destino para salvar o resultado.
    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->

        uri?.let { destinationUri ->

            val contentToSave = uiState.sanitizedText

            coroutineScope.launch {

                val saved = try {
                    withContext(Dispatchers.IO) {
                        writeTextToUri(
                            context = context,
                            uri = destinationUri,
                            content = contentToSave
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    false
                }

                val message = if (saved) {
                    context.getString(R.string.save_txt_success)
                } else {
                    context.getString(R.string.save_txt_error)
                }

                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    AnonymizerContent(
        uiState = uiState,

        onInputTextChanged = viewModel::onInputTextChanged,
        onTogglePiiType = viewModel::onTogglePiiType,
        onMaskingModeChanged = viewModel::onMaskingModeChanged,
        onToggleWhatsAppNoise = viewModel::onToggleWhatsAppNoise,
        onEnabledTypesChanged = viewModel::onEnabledTypesChanged,
        onSettingsApplied = viewModel::onSettingsApplied,
        onReplaceSelectedResult = viewModel::replaceSelectedResult,
        onReplaceFirstResult = viewModel::replaceFirstInResult,
        onReplaceAllResult = viewModel::replaceAllInResult,
        onUndoManualReplacement = viewModel::undoManualReplacement,
        onRedoManualReplacement = viewModel::redoManualReplacement,

        onCopyClick = {
            val text = uiState.sanitizedText

            when {
                text.isBlank() -> Unit

                text.length > MAX_CLIPBOARD_CHARS -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.copy_text_too_large),
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    runCatching {
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText(
                                context.getString(R.string.clipboard_anonymized_text_label),
                                text
                            )
                        )
                    }.onSuccess {
                        Toast.makeText(
                            context,
                            context.getString(R.string.copy_result_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        Toast.makeText(
                            context,
                            context.getString(R.string.copy_result_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        },

        onSaveTxtClick = {
            if (uiState.sanitizedText.isNotBlank()) {
                saveTxtLauncher.launch(context.getString(R.string.default_export_file_name))
            }
        },

        onClearClick = viewModel::clearSession,

        onPickFileClick = {
            filePickerLauncher.launch(
                arrayOf(
                    "text/plain",
                    "application/pdf",
                    "application/zip",
                    "application/x-zip-compressed"
                )
            )
        }
    )
}

/**
 * Organiza os componentes da tela.
 *
 * Não acessa diretamente o ViewModel.
 * Recebe os dados e eventos por parâmetros.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AnonymizerContent(
    uiState: AnonymizerUiState,
    onInputTextChanged: (String) -> Unit,
    onTogglePiiType: (PiiType) -> Unit,
    onMaskingModeChanged: (MaskMode) -> Unit,
    onToggleWhatsAppNoise: (Boolean) -> Unit,
    onEnabledTypesChanged: (Set<PiiType>) -> Unit,
    onSettingsApplied: (Set<PiiType>, MaskMode, Boolean, String) -> Unit = { _, _, _, _ -> },
    onReplaceSelectedResult: (String, String, Int) -> Unit = { _, _, _ -> },
    onReplaceFirstResult: (String, String) -> Unit = { _, _ -> },
    onReplaceAllResult: (String, String) -> Unit = { _, _ -> },
    onUndoManualReplacement: () -> Unit = {},
    onRedoManualReplacement: () -> Unit = {},
    onCopyClick: () -> Unit,
    onClearClick: () -> Unit,
    onPickFileClick: () -> Unit,
    onSaveTxtClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showClearDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val hasInput = uiState.inputText.isNotBlank()
    val hasOutput = uiState.sanitizedText.isNotBlank()
    val hasResult = hasOutput ||
            uiState.manuallyRemovedCount > 0 ||
            uiState.canUndoManualReplacement ||
            uiState.canRedoManualReplacement

    val isLargeInput =
        uiState.inputText.length > MAX_PREVIEW_CHARS

    val isLargeOutput =
        uiState.sanitizedText.length > MAX_PREVIEW_CHARS
    val redactionMarker = uiState.manualRedactionMarker

    var pendingSelectedReplacement by remember(uiState.sanitizedText) {
        mutableStateOf<SelectedTextReplacement?>(null)
    }

    val totalEntities =
        uiState.entityCounts.values.sum()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {

            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.screen_title),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = stringResource(R.string.screen_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },

                actions = {
                    if ((hasInput || hasOutput)) {
                        IconButton(
                            onClick = {
                                showClearDialog = true
                            },
                            enabled = !uiState.isProcessing
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(
                                    R.string.clear_document_content_description
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            showSettingsSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(
                                R.string.settings_content_description
                            )
                        )
                    }


                }
            )

        },

        floatingActionButton = {
            val selectedReplacement = pendingSelectedReplacement
            if (
                !isLargeOutput &&
                !uiState.isProcessing &&
                selectedReplacement != null &&
                selectedReplacement.target.isNotEmpty() &&
                selectedReplacement.target != redactionMarker
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.hide_selected_text))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    FloatingActionButton(
                        onClick = {
                            onReplaceSelectedResult(
                                selectedReplacement.target,
                                redactionMarker,
                                selectedReplacement.occurrenceIndex
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FindReplace,
                            contentDescription = stringResource(R.string.hide_selected_text)
                        )
                    }
                }
            }
        },

        bottomBar = {
            if (hasOutput) {
                ResultActionsBar(
                    isProcessing = uiState.isProcessing,

                    // O botão continua disponível para informar
                    // quando o conteúdo ultrapassa o limite.
                    canCopy = !uiState.isProcessing,

                    onCopyClick = onCopyClick,
                    onSaveClick = onSaveTxtClick
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InputDocumentCard(
                inputText = uiState.inputText,
                isLargeInput = isLargeInput,
                isProcessing = uiState.isProcessing,
                onInputTextChanged = onInputTextChanged,
                onPickFileClick = onPickFileClick
            )


            // 3. Estado de processamento.
            if (uiState.isProcessing) {
                ProcessingCard()
            }

            // 4. Mensagem de erro.
            uiState.errorMessage?.let { error ->
                ErrorCard(message = error)
            }

            // 5. Resultado ou estado vazio.
            if (hasResult) {

                OutputCard(
                    sanitizedText = uiState.sanitizedText,
                    totalEntities = totalEntities,
                    removedNoiseCount =
                        uiState.removedNoiseCount,
                    manuallyRemovedCount = uiState.manuallyRemovedCount,
                    redactionMarker = redactionMarker,
                    isLargeOutput = isLargeOutput,
                    isProcessing = uiState.isProcessing,
                    canUndo = uiState.canUndoManualReplacement,
                    canRedo = uiState.canRedoManualReplacement,
                    onSelectedReplacementChanged = { target, occurrenceIndex ->
                        pendingSelectedReplacement = SelectedTextReplacement(
                            target = target,
                            occurrenceIndex = occurrenceIndex
                        )
                    },
                    onReplaceFirst = onReplaceFirstResult,
                    onReplaceAll = onReplaceAllResult,
                    onUndo = onUndoManualReplacement,
                    onRedo = onRedoManualReplacement
                )

            } else if (
                hasInput &&
                !uiState.isProcessing &&
                uiState.errorMessage == null
            ) {

                EmptyResultCard()
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            enabledTypes = uiState.enabledTypes,
            maskingMode = uiState.maskingMode,
            cleanWhatsAppNoise = uiState.cleanWhatsAppNoise,
            manualRedactionMarker = uiState.manualRedactionMarker,
            isProcessing = uiState.isProcessing,

            onSettingsApplied = onSettingsApplied,

            onDismiss = {
                showSettingsSheet = false
            }
        )
    }

    // Confirmação antes de limpar o conteúdo.
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },

            title = {
                Text(stringResource(R.string.clear_document_dialog_title))
            },

            text = {
                Text(stringResource(R.string.clear_document_dialog_message))
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearClick()
                    }
                ) {
                    Text(stringResource(R.string.clear_button))
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

}

private data class SelectedTextReplacement(
    val target: String,
    val occurrenceIndex: Int
)


// -----------------------------------------------------
// Previews
// -----------------------------------------------------

@Preview(
    name = "Modo Claro",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Modo Escuro",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AnonymizerScreenPreview() {
    AnonimizadorOfflineTheme {
        AnonymizerContent(
            uiState = AnonymizerUiState(
                inputText = "João: Meu CPF é 123.456.789-00 " +
                        "e o PIX é joao@email.com",

                sanitizedText = "[PARTICIPANTE_1]: Meu CPF é " +
                        "[CPF_OCULTO] e o PIX é [EMAIL_OCULTO]",

                cleanWhatsAppNoise = true,
                removedNoiseCount = 2
            ),

            onEnabledTypesChanged = {},
            onInputTextChanged = {},
            onTogglePiiType = {},
            onMaskingModeChanged = {},
            onToggleWhatsAppNoise = {},
            onCopyClick = {},
            onClearClick = {},
            onPickFileClick = {},
            onSaveTxtClick = {}
        )
    }
}
