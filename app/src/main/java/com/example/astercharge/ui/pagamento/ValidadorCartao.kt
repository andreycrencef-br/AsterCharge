package com.example.astercharge.ui.pagamento

import java.util.Calendar

/**
 * Validações client-side de cartão de crédito/débito.
 *
 * Isso NÃO substitui um gateway de pagamento real. Antes de cobrar de
 * verdade, o número completo do cartão precisa ser enviado, via SDK do
 * gateway (ex.: Stripe), diretamente para o servidor do gateway — nunca
 * para o seu próprio backend. Veja o README, seção "Cartão real".
 */
object ValidadorCartao {

    /** Algoritmo de Luhn: confirma que o número de cartão é matematicamente válido. */
    fun numeroValido(numero: String): Boolean {
        val digitos = numero.filter { it.isDigit() }
        if (digitos.length !in 13..19) return false

        var soma = 0
        var dobrar = false

        for (i in digitos.length - 1 downTo 0) {
            var d = digitos[i] - '0'
            if (dobrar) {
                d *= 2
                if (d > 9) d -= 9
            }
            soma += d
            dobrar = !dobrar
        }

        return soma % 10 == 0
    }

    fun detectarBandeira(numero: String): String {
        val digitos = numero.filter { it.isDigit() }
        return when {
            digitos.startsWith("4") -> "Visa"
            digitos.take(2).toIntOrNull()?.let { it in 51..55 } == true -> "Mastercard"
            digitos.take(6).toIntOrNull()?.let { it in 400000..431999 } == true -> "Elo"
            digitos.startsWith("34") || digitos.startsWith("37") -> "American Express"
            digitos.startsWith("6") -> "Elo/Discover"
            else -> "Cartão"
        }
    }

    /** Espera o formato MM/AA. Retorna false se a data já passou ou é inválida. */
    fun validadeValida(mesAno: String): Boolean {
        val partes = mesAno.split("/")
        if (partes.size != 2) return false

        val mes = partes[0].toIntOrNull() ?: return false
        val anoDigitado = partes[1].toIntOrNull() ?: return false
        if (mes !in 1..12) return false

        val ano = if (anoDigitado < 100) 2000 + anoDigitado else anoDigitado

        val agora = Calendar.getInstance()
        val anoAtual = agora.get(Calendar.YEAR)
        val mesAtual = agora.get(Calendar.MONTH) + 1

        return ano > anoAtual || (ano == anoAtual && mes >= mesAtual)
    }

    fun cvvValido(cvv: String): Boolean {
        val digitos = cvv.filter { it.isDigit() }
        return digitos.length in 3..4
    }

    fun nomeValido(nome: String): Boolean {
        return nome.trim().length >= 3 && nome.trim().contains(" ")
    }

    /** Formata "4111111111111111" -> "4111 1111 1111 1111" enquanto o usuário digita. */
    fun formatarNumeroDigitado(entrada: String): String {
        val digitos = entrada.filter { it.isDigit() }.take(19)
        return digitos.chunked(4).joinToString(" ")
    }

    /** Formata "1225" -> "12/25" enquanto o usuário digita. */
    fun formatarValidadeDigitada(entrada: String): String {
        val digitos = entrada.filter { it.isDigit() }.take(4)
        return if (digitos.length <= 2) digitos
        else "${digitos.take(2)}/${digitos.drop(2)}"
    }
}
