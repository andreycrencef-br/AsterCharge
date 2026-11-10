package com.example.astercharge.data

data class ItemFila(
    val id: String,
    val nomeUsuario: String,
    val idCarregador: String,
    val ordemEntrada: Long,
    val bateriaAtual: Int = 10,
    val tempoEntrada: String = ""
)
