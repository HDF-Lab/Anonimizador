package br.com.hdf.anonimizador.presentation.share

import android.content.Intent
import android.net.Uri
import android.os.Build

sealed interface SharedPayload {
    data class PlainText(val text: String) : SharedPayload
    data class DocumentUri(val uri: Uri) : SharedPayload
    data object Empty : SharedPayload
}

object ShareIntentExtractor {

    fun extract(intent: Intent?): SharedPayload {
        if (intent == null) return SharedPayload.Empty

        return when (intent.action) {
            Intent.ACTION_SEND -> extractSingle(intent)
            Intent.ACTION_SEND_MULTIPLE -> extractMultiple(intent)
            else -> SharedPayload.Empty
        }
    }

    private fun extractSingle(intent: Intent): SharedPayload {
        val streamUri = extractStreamUri(intent) ?: extractFromClipData(intent)
        if (streamUri != null) {
            return SharedPayload.DocumentUri(streamUri)
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            return SharedPayload.PlainText(sharedText)
        }

        return SharedPayload.Empty
    }

    private fun extractMultiple(intent: Intent): SharedPayload {
        val uris = extractStreamUriList(intent)
        if (uris.isEmpty()) return SharedPayload.Empty

        // Prioriza .zip ou .txt da conversa caso o WhatsApp envie mídias junto
        val prioritizedUri = uris.firstOrNull { uri ->
            val path = uri.toString().lowercase()
            path.endsWith(".zip") || path.endsWith(".txt") || path.endsWith(".pdf")
        } ?: uris.first()

        return SharedPayload.DocumentUri(prioritizedUri)
    }

    private fun extractFromClipData(intent: Intent): Uri? {
        val clipData = intent.clipData ?: return null
        if (clipData.itemCount == 0) return null
        return clipData.getItemAt(0).uri
    }

    @Suppress("DEPRECATION")
    private fun extractStreamUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun extractStreamUriList(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
}
