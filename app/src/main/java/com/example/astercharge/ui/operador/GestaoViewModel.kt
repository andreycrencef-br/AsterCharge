package com.example.astercharge.ui.operador

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parâmetros de gestão ajustáveis pelo operador (painel "Gestão").
 *
 * Por enquanto isso alimenta apenas o próprio painel do operador — é o
 * "painel de controle" da precificação. Para fazer esses valores mudarem
 * de fato a tarifa cobrada do motorista, basta ler
 * `gestaoViewModel.descontoSolarMaximo` / `acrescimoPico` dentro de
 * `calcularTarifaBase()` em vez das constantes fixas usadas hoje.
 */
class GestaoViewModel : ViewModel() {

    companion object {
        const val TARIFA_REFERENCIA_KWH = 0.52
    }

    private val _descontoSolarMaximo = MutableStateFlow(0.20)
    val descontoSolarMaximo: StateFlow<Double> = _descontoSolarMaximo.asStateFlow()

    private val _acrescimoPico = MutableStateFlow(0.20)
    val acrescimoPico: StateFlow<Double> = _acrescimoPico.asStateFlow()

    private val _redistribuicaoAtiva = MutableStateFlow(true)
    val redistribuicaoAtiva: StateFlow<Boolean> = _redistribuicaoAtiva.asStateFlow()

    fun definirDescontoSolarMaximo(valor: Double) {
        _descontoSolarMaximo.value = valor.coerceIn(0.0, 0.5)
    }

    fun definirAcrescimoPico(valor: Double) {
        _acrescimoPico.value = valor.coerceIn(0.0, 0.5)
    }

    fun alternarRedistribuicao(ativo: Boolean) {
        _redistribuicaoAtiva.value = ativo
    }
}
