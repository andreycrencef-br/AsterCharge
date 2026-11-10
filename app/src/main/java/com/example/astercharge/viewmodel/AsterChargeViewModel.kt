package com.example.astercharge.viewmodel

import androidx.lifecycle.ViewModel
import com.example.astercharge.core.ChargingEngine
import com.example.astercharge.data.CartaoInfo
import com.example.astercharge.data.HistoricoSessao
import com.example.astercharge.data.MetodoPagamento
import com.example.astercharge.data.Pagamento
import com.example.astercharge.data.SessaoRecarga
import com.example.astercharge.data.StatusPagamento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AsterChargeViewModel : ViewModel() {

    companion object {
        const val TAXA_CONEXAO = 2.50
        const val MULTA_OCIOSIDADE_POR_MINUTO = 5.00
        const val TOLERANCIA_OCIOSIDADE_MINUTOS = 2
        const val LIMITE_OCIOSIDADE_MINUTOS = 10
    }

    private val _sessao = MutableStateFlow(SessaoRecarga())
    val sessao: StateFlow<SessaoRecarga> = _sessao.asStateFlow()

    private val _historico = MutableStateFlow<List<HistoricoSessao>>(emptyList())
    val historico: StateFlow<List<HistoricoSessao>> = _historico.asStateFlow()

    private val _pagamento = MutableStateFlow<Pagamento?>(null)
    val pagamento: StateFlow<Pagamento?> = _pagamento.asStateFlow()

    // ------------------------------------------------------------
    // CARTÃO (novo)
    // ------------------------------------------------------------
    private val _cartao = MutableStateFlow<CartaoInfo?>(null)
    val cartao: StateFlow<CartaoInfo?> = _cartao.asStateFlow()

    fun salvarCartao(novoCartao: CartaoInfo) {
        _cartao.value = novoCartao
    }

    fun removerCartao() {
        _cartao.value = null
    }

    private val _historicoChat =
        MutableStateFlow(
            listOf(
                "Olá! Sou o assistente virtual do AsterCharge! Como posso ajudar?"
            )
        )

    val historicoChat: StateFlow<List<String>> =
        _historicoChat.asStateFlow()

    fun adicionarMensagemChat(
        mensagem: String
    ) {

        _historicoChat.value =
            _historicoChat.value + mensagem
    }
    fun iniciarSessao(
        idCarregador: String,
        potenciaNominalKw: Double,
        bateriaInicial: Double,
        capacidadeBateriaKwh: Double = 60.0,
        tarifa: Double
    ) {

        val estimativa =
            ChargingEngine.estimarTempo(
                bateriaInicial =
                    bateriaInicial,

                potenciaNominalKw =
                    potenciaNominalKw,

                capacidadeBateriaKwh =
                    capacidadeBateriaKwh
            )

        _sessao.value =
            SessaoRecarga(

                ativa = true,

                finalizada = false,

                idCarregador =
                    idCarregador,

                bateriaInicial =
                    bateriaInicial,

                bateriaAtual =
                    bateriaInicial,

                capacidadeBateriaKwh =
                    capacidadeBateriaKwh,

                potenciaNominalKw =
                    potenciaNominalKw,

                potenciaAtualKw =
                    potenciaNominalKw,

                tarifaAtual =
                    tarifa,

                taxaConexao =
                    TAXA_CONEXAO,

                tempoRestanteEstimadoMinutos =
                    estimativa
            )

        _pagamento.value = null
    }

    /**
     * Avança exatamente 1 minuto da simulação.
     * A UI pode chamar este método a cada 1 segundo real.
     */
    fun simularUmMinuto() {

        val atual = _sessao.value

        if (!atual.ativa) return

        if (atual.bateriaAtual >= 100.0) {
            atualizarOciosidade()
            return
        }

        val passo = ChargingEngine.simularMinuto(
            bateriaAtual = atual.bateriaAtual,
            kwhEntregueAtual = atual.kwhEntregue,
            potenciaNominalKw = atual.potenciaNominalKw,
            capacidadeBateriaKwh = atual.capacidadeBateriaKwh
        )

        val novoTempo =
            atual.tempoDecorridoMinutos + 1

        val custoEnergia =
            passo.kwhEntregue * atual.tarifaAtual

        val estimativa =
            if (passo.bateria < 100.0) {
                ChargingEngine.estimarTempo(
                    bateriaInicial = passo.bateria,
                    potenciaNominalKw = atual.potenciaNominalKw,
                    capacidadeBateriaKwh = atual.capacidadeBateriaKwh
                )
            } else {
                0
            }

        _sessao.value = atual.copy(
            bateriaAtual = passo.bateria,
            kwhEntregue = passo.kwhEntregue,
            tempoDecorridoMinutos = novoTempo,
            tempoRestanteEstimadoMinutos = estimativa,
            potenciaAtualKw = passo.potenciaEfetivaKw,
            percentualUtilizacaoRede = passo.percentualRede,
            throttleAtivo = passo.throttleAtivo,
            custoEnergia = custoEnergia,
            emOciosidade = passo.bateria >= 100.0,
            etapaCCCV = passo.etapaCCCV
        )
    }

    private fun atualizarOciosidade() {

        val atual = _sessao.value

        val novoTempo =
            atual.minutosOciosidade + 1

        val multa =
            if (novoTempo > TOLERANCIA_OCIOSIDADE_MINUTOS) {
                (novoTempo - TOLERANCIA_OCIOSIDADE_MINUTOS) *
                    MULTA_OCIOSIDADE_POR_MINUTO
            } else {
                0.0
            }

        _sessao.value = atual.copy(
            bateriaAtual = 100.0,
            potenciaAtualKw = 0.0,
            emOciosidade = true,
            minutosOciosidade = novoTempo,
            multaOciosidade = multa,
            tempoDecorridoMinutos =
                atual.tempoDecorridoMinutos + 1
        )

        if (novoTempo >= LIMITE_OCIOSIDADE_MINUTOS) {
            finalizarSessao()
        }
    }

    fun finalizarSessao() {

        val atual = _sessao.value

        if (atual.finalizada) return

        val historico = HistoricoSessao(
            id = "SES-${System.currentTimeMillis()}",
            data = "Hoje",
            local = "AsterCharge",
            carregador = atual.idCarregador ?: "-",
            bateriaInicial = atual.bateriaInicial.toInt(),
            bateriaFinal = atual.bateriaAtual.toInt().coerceAtMost(100),
            kwh = atual.kwhEntregue,
            tempoMinutos = atual.tempoDecorridoMinutos,
            tarifaMedia = atual.tarifaAtual,
            taxaConexao = atual.taxaConexao,
            multaOciosidade = atual.multaOciosidade,
            custoTotal = atual.custoTotal,
            metodoPagamento = MetodoPagamento.PIX,
            statusPagamento = StatusPagamento.PENDENTE
        )

        _historico.value =
            listOf(historico) + _historico.value

        _sessao.value = atual.copy(
            ativa = false,
            finalizada = true,
            potenciaAtualKw = 0.0
        )
    }

    /**
     * Cria um pagamento. Para CARTAO, exige que já exista um cartão salvo
     * (retorna false se não houver, para a UI pedir o cadastro do cartão).
     */
    fun criarPagamento(
        metodo: MetodoPagamento
    ): Boolean {

        if (metodo == MetodoPagamento.CARTAO && _cartao.value == null) {
            return false
        }

        val sessaoAtual = _sessao.value

        _pagamento.value = Pagamento(
            id = "PAY-${System.currentTimeMillis()}",
            metodo = metodo,
            status = StatusPagamento.APROVADO,
            valor = sessaoAtual.custoTotal,
            identificador =
                if (metodo == MetodoPagamento.PIX) {
                    "PIX-ASTER-${System.currentTimeMillis()}"
                } else {
                    _cartao.value?.numeroMascarado ?: "**** ****"
                }
        )

        return true
    }

    fun resetarSessao() {
        _sessao.value = SessaoRecarga()
        _pagamento.value = null
    }
}
