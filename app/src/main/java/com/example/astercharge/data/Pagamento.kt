package com.example.astercharge.data

enum class MetodoPagamento {
    CARTAO,
    PIX
}

enum class StatusPagamento {
    PENDENTE,
    APROVADO,
    RECUSADO
}

data class Pagamento(
    val id: String,
    val metodo: MetodoPagamento,
    val status: StatusPagamento,
    val valor: Double,
    val identificador: String
)
