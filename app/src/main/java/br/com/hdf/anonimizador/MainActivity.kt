package br.com.hdf.anonimizador

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import br.com.hdf.anonimizador.domain.model.DocumentSource
import br.com.hdf.anonimizador.core.security.WindowPrivacyGuard
import br.com.hdf.anonimizador.presentation.editor.AnonymizerScreen
import br.com.hdf.anonimizador.presentation.editor.AnonymizerViewModel
import br.com.hdf.anonimizador.presentation.share.ShareIntentExtractor
import br.com.hdf.anonimizador.presentation.share.SharedPayload
import br.com.hdf.anonimizador.ui.theme.AnonimizadorOfflineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AnonymizerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowPrivacyGuard.protectSensitiveWindow(this)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            AnonimizadorOfflineTheme {
                AnonymizerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        when (val payload = ShareIntentExtractor.extract(incomingIntent)) {
            is SharedPayload.DocumentUri -> viewModel.onFileSelected(DocumentSource(payload.uri.toString()))
            is SharedPayload.PlainText -> viewModel.onInputTextChanged(payload.text)
            SharedPayload.Empty -> Unit
        }
    }
}
