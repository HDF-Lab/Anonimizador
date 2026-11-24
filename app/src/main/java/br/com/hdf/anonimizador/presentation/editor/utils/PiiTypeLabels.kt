package br.com.hdf.anonimizador.presentation.editor.utils

import androidx.annotation.StringRes
import br.com.hdf.anonimizador.R
import br.com.hdf.anonimizador.domain.model.PiiType

@StringRes
fun PiiType.toDisplayLabel(): Int = when (this) {
    PiiType.CPF -> R.string.pii_type_cpf
    PiiType.CNPJ -> R.string.pii_type_cnpj
    PiiType.CHAVE_PIX -> R.string.pii_type_pix_key
    PiiType.TELEFONE_BR -> R.string.pii_type_phone
    PiiType.EMAIL -> R.string.pii_type_email
    PiiType.CARTAO_CREDITO -> R.string.pii_type_credit_card
    PiiType.PLACA_VEICULO -> R.string.pii_type_license_plate
    PiiType.ENDERECO_IP -> R.string.pii_type_ip_address
    PiiType.PARTICIPANTE_WHATSAPP -> R.string.pii_type_whatsapp_participant
    PiiType.ENDERECO_FISICO -> R.string.pii_type_physical_address
    PiiType.NOME_PROPRIO -> R.string.pii_type_person_name
    PiiType.CNH -> R.string.pii_type_drivers_license
}