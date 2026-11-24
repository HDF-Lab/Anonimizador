package br.com.hdf.anonimizador.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrazilianDocumentValidatorTest {

    @Test
    fun `deve validar CPF formatado valido`() {
        assertTrue(BrazilianDocumentValidator.isValidCpf("529.982.247-25"))
    }

    @Test
    fun `deve rejeitar CPF com digitos repetidos`() {
        assertFalse(BrazilianDocumentValidator.isValidCpf("111.111.111-11"))
    }

    @Test
    fun `deve rejeitar CPF com tamanho invalido`() {
        assertFalse(BrazilianDocumentValidator.isValidCpf("529982247"))
    }

    @Test
    fun `deve validar CNPJ formatado valido`() {
        assertTrue(BrazilianDocumentValidator.isValidCnpj("11.444.777/0001-61"))
    }

    @Test
    fun `deve rejeitar CNPJ com digitos repetidos`() {
        assertFalse(BrazilianDocumentValidator.isValidCnpj("11.111.111/1111-11"))
    }

    @Test
    fun `deve rejeitar CNPJ com tamanho invalido`() {
        assertFalse(BrazilianDocumentValidator.isValidCnpj("114447770001"))
    }

    @Test
    fun `deve validar numero de cartao por Luhn`() {
        assertTrue(BrazilianDocumentValidator.isValidLuhn("4111 1111 1111 1111"))
    }

    @Test
    fun `deve rejeitar numero de cartao invalido`() {
        assertFalse(BrazilianDocumentValidator.isValidLuhn("4111 1111 1111 1112"))
    }

    @Test
    fun `deve rejeitar tamanho invalido para Luhn`() {
        assertFalse(BrazilianDocumentValidator.isValidLuhn("123"))
        assertFalse(BrazilianDocumentValidator.isValidLuhn("12345678901234567890"))
    }
}
