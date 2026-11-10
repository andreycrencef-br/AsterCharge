package com.example.astercharge.data

enum class StatusSessao {
    LIVRE,
    ATIVA,
    THROTTLE,
    ENCERRADA
}

data class CarregadorInfo(
    val id: String,
    val nome: String,
    val tipo: String,
    val potenciaNominalKw: Double,
    val precoBaseKwh: Double,
    val potenciaAtualKw: Double = potenciaNominalKw,
    val statusSessao: StatusSessao = StatusSessao.LIVRE,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val vagasTotais: Int = 1,
    val vagasDisponiveis: Int = 1
)
