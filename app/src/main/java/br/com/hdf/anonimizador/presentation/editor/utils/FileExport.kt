package br.com.hdf.anonimizador.presentation.editor.utils

import android.content.Context
import android.net.Uri

fun writeTextToUri(
    context: Context,
    uri: Uri,
    content: String
): Boolean {
    return runCatching {
        val outputStream = context.contentResolver
            .openOutputStream(uri)
            ?: return false

        outputStream.bufferedWriter(Charsets.UTF_8).use {
            it.write(content)
        }

        true
    }.getOrDefault(false)
}