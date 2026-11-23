package br.com.hdf.anonimizador.presentation.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun ResultActionsBar(
    isProcessing: Boolean,
    canCopy: Boolean,
    onCopyClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveClick,
                enabled = !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Salvar TXT")
            }

            Button(
                onClick = onCopyClick,
                enabled = !isProcessing && canCopy,
                modifier = Modifier.weight(1f)
            ) {
                Text("Copiar resultado")
            }
        }
    }
}