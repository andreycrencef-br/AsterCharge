package com.example.astercharge.ui.config

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Application.dataStore by preferencesDataStore(name = "astercharge_config")

/**
 * Guarda as preferências do app (modo escuro, notificações) em disco com
 * DataStore, para que sobrevivam ao fechar o app. Requer a dependência
 * androidx.datastore:datastore-preferences (veja o README).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private object Chaves {
        val MODO_ESCURO = booleanPreferencesKey("modo_escuro")
        val ALERTA_SESSAO = booleanPreferencesKey("alerta_sessao")
        val NOTICIAS_OFERTAS = booleanPreferencesKey("noticias_ofertas")
    }

    private val _modoEscuro = MutableStateFlow(false)
    val modoEscuro: StateFlow<Boolean> = _modoEscuro.asStateFlow()

    private val _alertaSessao = MutableStateFlow(true)
    val alertaSessao: StateFlow<Boolean> = _alertaSessao.asStateFlow()

    private val _noticiasOfertas = MutableStateFlow(false)
    val noticiasOfertas: StateFlow<Boolean> = _noticiasOfertas.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = application.dataStore.data
            prefs.collect { preferencias ->
                _modoEscuro.value = preferencias[Chaves.MODO_ESCURO] ?: false
                _alertaSessao.value = preferencias[Chaves.ALERTA_SESSAO] ?: true
                _noticiasOfertas.value = preferencias[Chaves.NOTICIAS_OFERTAS] ?: false
            }
        }
    }

    fun alternarModoEscuro(ativo: Boolean): Unit {
        _modoEscuro.value = ativo
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[Chaves.MODO_ESCURO] = ativo }
        }
    }

    fun alternarAlertaSessao(ativo: Boolean): Unit {
        _alertaSessao.value = ativo
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[Chaves.ALERTA_SESSAO] = ativo }
        }
    }

    fun alternarNoticiasOfertas(ativo: Boolean): Unit {
        _noticiasOfertas.value = ativo
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[Chaves.NOTICIAS_OFERTAS] = ativo }
        }
    }
}
