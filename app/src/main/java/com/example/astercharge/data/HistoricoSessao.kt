package com.example.astercharge.data

data class HistoricoSessao(
    val id: String,
    val data: String,
    val local: String,
    val carregador: String,
    val bateriaInicial: Int,
    val bateriaFinal: Int,
    val kwh: Double,
    val tempoMinutos: Int,
    val tarifaMedia: Double,
    val taxaConexao: Double,
    val multaOciosidade: Double,
    val custoTotal: Double,
    val metodoPagamento: MetodoPagamento,
    val statusPagamento: StatusPagamento
)
