package com.example.astercharge.data

enum class PerfilAcesso {
    USUARIO,
    OPERADOR
}

data class Usuario(
    val uid: String,
    val nome: String,
    val email: String,
    val perfilAcesso: PerfilAcesso = PerfilAcesso.USUARIO
)
