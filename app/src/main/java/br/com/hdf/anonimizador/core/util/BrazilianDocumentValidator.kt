package br.com.hdf.anonimizador.core.util

object BrazilianDocumentValidator {

    fun isValidCpf(raw: String): Boolean {
        val digits = raw.filter(Char::isDigit)
        if (digits.length != 11 || digits.all { it == digits[0] }) return false

        val firstCheck = calculateMod11(digits.substring(0, 9), startWeight = 10)
        val secondCheck = calculateMod11(digits.substring(0, 10), startWeight = 11)
        return digits[9].digitToInt() == firstCheck && digits[10].digitToInt() == secondCheck
    }

    fun isValidCnpj(raw: String): Boolean {
        val digits = raw.filter(Char::isDigit)
        if (digits.length != 14 || digits.all { it == digits[0] }) return false

        val weights1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val weights2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

        val check1 = calculateWeightedMod11(digits.substring(0, 12), weights1)
        val check2 = calculateWeightedMod11(digits.substring(0, 13), weights2)
        return digits[12].digitToInt() == check1 && digits[13].digitToInt() == check2
    }

    fun isValidLuhn(raw: String): Boolean {
        val digits = raw.filter(Char::isDigit)
        if (digits.length !in 13..19) return false

        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun calculateMod11(base: String, startWeight: Int): Int {
        var sum = 0
        base.forEachIndexed { index, char ->
            sum += char.digitToInt() * (startWeight - index)
        }
        val remainder = (sum * 10) % 11
        return if (remainder == 10) 0 else remainder
    }

    private fun calculateWeightedMod11(base: String, weights: IntArray): Int {
        var sum = 0
        base.forEachIndexed { index, char ->
            sum += char.digitToInt() * weights[index]
        }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }
}
