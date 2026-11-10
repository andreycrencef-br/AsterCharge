package com.example.astercharge.data

/**
 * Representa um cartão vinculado à conta.
 *
 * IMPORTANTE (segurança/PCI-DSS):
 * Nunca guardamos o número completo do cartão nem o CVV. Guardamos apenas
 * os 4 últimos dígitos (para exibição) e um "token" simulado que, em uma
 * versão de produção, seria devolvido por um gateway de pagamento
 * (Stripe, Mercado Pago, PagSeguro etc.) depois da tokenização do cartão.
 * Veja o README para como plugar um gateway real.
 */
data class CartaoInfo(
    val id: String,
    val nomeTitular: String,
    val ultimosDigitos: String,
    val bandeira: String,
    val validadeMesAno: String,
    val tokenPagamento: String
) {
    val numeroMascarado: String
        get() = "**** **** **** $ultimosDigitos"
}
