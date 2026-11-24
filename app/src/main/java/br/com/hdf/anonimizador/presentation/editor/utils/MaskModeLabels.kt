package br.com.hdf.anonimizador.presentation.editor.utils

import br.com.hdf.anonimizador.domain.model.MaskMode

fun MaskMode.toDisplayLabel(): String = when (this) {
    MaskMode.IRREVERSIBLE -> "Irreversível"
    MaskMode.REVERSIBLE -> "Com mapa temporário (RAM)"
}