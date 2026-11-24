package br.com.hdf.anonimizador.domain.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WhatsAppNoisePolicyTest {

    private lateinit var policy: WhatsAppNoisePolicy

    @Before
    fun setUp() {
        policy = WhatsAppNoisePolicy()
    }

    @Test
    fun `deve identificar marcador de midia como ruido`() {
        val entry = RawChatEntry("10:30", "Ana", "<mídia oculta>")
        assertTrue(policy.isNoise(entry))
    }

    @Test
    fun `deve identificar mensagem apagada como ruido`() {
        val entry = RawChatEntry("10:30", "Ana", "Esta mensagem foi apagada")
        assertTrue(policy.isNoise(entry))
    }

    @Test
    fun `deve identificar aviso de criptografia como ruido`() {
        val entry = RawChatEntry(
            "10:30",
            null,
            "As mensagens e as chamadas são protegidas com a criptografia de ponta a ponta"
        )
        assertTrue(policy.isNoise(entry))
    }

    @Test
    fun `deve identificar evento de sistema sem remetente`() {
        val entry = RawChatEntry("10:30", null, "João adicionou Maria")
        assertTrue(policy.isNoise(entry))
    }

    @Test
    fun `deve preservar evento textual com remetente`() {
        val entry = RawChatEntry("10:30", "Ana", "João adicionou Maria")
        assertFalse(policy.isNoise(entry))
    }

    @Test
    fun `deve preservar mensagem comum`() {
        val entry = RawChatEntry("10:30", "Ana", "Olá, tudo bem?")
        assertFalse(policy.isNoise(entry))
    }

    @Test
    fun `deve remover corpo vazio`() {
        val entry = RawChatEntry("10:30", "Ana", "   ")
        assertTrue(policy.isNoise(entry))
    }

    @Test
    fun `deve remover caracteres invisiveis da sanitizacao`() {
        val sanitized = policy.sanitizeInvisibleChars("Ana\u200e Silva\u202eb")
        assertTrue(sanitized == "Ana Silvab")
    }
}
