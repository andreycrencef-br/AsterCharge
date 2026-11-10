package com.example.astercharge.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.astercharge.data.PerfilAcesso
import com.example.astercharge.data.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val preferencias =
        application.getSharedPreferences("astercharge_auth", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()

    init {
        auth.currentUser?.let { firebaseUser ->
            _usuario.value = Usuario(
                uid = firebaseUser.uid,
                nome = firebaseUser.displayName ?: "Usuário AsterCharge",
                email = firebaseUser.email ?: "",
                perfilAcesso = lerPerfil(firebaseUser.uid)
            )
        }
    }

    private fun chavePerfil(uid: String): String = "perfil_$uid"

    private fun salvarPerfil(uid: String, perfil: PerfilAcesso) {
        if (uid.isBlank()) return
        preferencias.edit().putString(chavePerfil(uid), perfil.name).apply()
    }

    private fun lerPerfil(uid: String): PerfilAcesso {
        val salvo = preferencias.getString(chavePerfil(uid), null)
        return PerfilAcesso.values().firstOrNull { it.name == salvo } ?: PerfilAcesso.USUARIO
    }

    fun limparErro() {
        _erro.value = null
    }

    fun login(email: String, senha: String, perfilSelecionado: PerfilAcesso) {
        if (email.isBlank() || senha.isBlank()) {
            _erro.value = "Preencha e-mail e senha."
            return
        }
        _carregando.value = true
        viewModelScope.launch {
            try {
                val resultado = auth.signInWithEmailAndPassword(email.trim(), senha).await()
                val firebaseUser = resultado.user
                val uid = firebaseUser?.uid ?: ""
                salvarPerfil(uid, perfilSelecionado)
                _usuario.value = Usuario(
                    uid = uid,
                    nome = firebaseUser?.displayName ?: "Usuário AsterCharge",
                    email = firebaseUser?.email ?: email,
                    perfilAcesso = perfilSelecionado
                )
            } catch (e: Exception) {
                _erro.value = traduzirErro(e.message)
            } finally {
                _carregando.value = false
            }
        }
    }

    fun cadastrar(nome: String, email: String, senha: String, perfilSelecionado: PerfilAcesso) {
        if (nome.isBlank() || email.isBlank() || senha.length < 6) {
            _erro.value = "Preencha nome, e-mail e uma senha com pelo menos 6 caracteres."
            return
        }
        _carregando.value = true
        viewModelScope.launch {
            try {
                val resultado = auth.createUserWithEmailAndPassword(email.trim(), senha).await()
                resultado.user?.updateProfile(
                    userProfileChangeRequest { displayName = nome }
                )?.await()

                val uid = resultado.user?.uid ?: ""
                salvarPerfil(uid, perfilSelecionado)
                _usuario.value = Usuario(
                    uid = uid,
                    nome = nome,
                    email = email.trim(),
                    perfilAcesso = perfilSelecionado
                )
            } catch (e: Exception) {
                _erro.value = traduzirErro(e.message)
            } finally {
                _carregando.value = false
            }
        }
    }

    fun sair() {
        auth.signOut()
        _usuario.value = null
    }

    fun atualizarNomePerfil(novoNome: String) {
        if (novoNome.isBlank()) return
        viewModelScope.launch {
            try {
                auth.currentUser?.updateProfile(
                    userProfileChangeRequest { displayName = novoNome }
                )?.await()
                _usuario.value = _usuario.value?.copy(nome = novoNome)
            } catch (e: Exception) {
                _erro.value = traduzirErro(e.message)
            }
        }
    }

    private fun traduzirErro(mensagem: String?): String {
        return when {
            mensagem == null -> "Não foi possível concluir. Tente novamente."
            mensagem.contains("badly formatted", ignoreCase = true) -> "E-mail inválido."
            mensagem.contains("email address is already in use", ignoreCase = true) -> "Esse e-mail já está cadastrado."
            mensagem.contains("password is invalid", ignoreCase = true) -> "Senha incorreta."
            mensagem.contains("no user record", ignoreCase = true) -> "Não existe conta com esse e-mail."
            mensagem.contains("network", ignoreCase = true) -> "Sem conexão com a internet."
            else -> mensagem
        }
    }
}
