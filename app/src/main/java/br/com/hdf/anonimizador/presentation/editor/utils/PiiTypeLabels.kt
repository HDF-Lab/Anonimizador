package br.com.hdf.anonimizador.presentation.editor.utils

import br.com.hdf.anonimizador.domain.model.PiiType

fun PiiType.toDisplayLabel(): String = when (this) {
    PiiType.CPF -> "CPF"
    PiiType.CNPJ -> "CNPJ"
    PiiType.CHAVE_PIX -> "Chave PIX aleatória no formato UUID"
    PiiType.TELEFONE_BR -> "Telefone"
    PiiType.EMAIL -> "E-mail"
    PiiType.CARTAO_CREDITO -> "Cartão"
    PiiType.PLACA_VEICULO -> "Placa"
    PiiType.ENDERECO_IP -> "Endereço IP"
    PiiType.PARTICIPANTE_WHATSAPP -> "Participante"
    PiiType.ENDERECO_FISICO -> "Endereço"
    PiiType.NOME_PROPRIO -> "Nome"
    PiiType.CNH -> "CNH"
}