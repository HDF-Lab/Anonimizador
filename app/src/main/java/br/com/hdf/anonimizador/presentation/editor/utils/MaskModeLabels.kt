package br.com.hdf.anonimizador.presentation.editor.utils

import androidx.annotation.StringRes
import br.com.hdf.anonimizador.R
import br.com.hdf.anonimizador.domain.model.MaskMode

@StringRes
fun MaskMode.toDisplayLabel(): Int = when (this) {
    MaskMode.IRREVERSIBLE -> R.string.mask_mode_irreversible_label
    MaskMode.REVERSIBLE -> R.string.mask_mode_reversible_label
}