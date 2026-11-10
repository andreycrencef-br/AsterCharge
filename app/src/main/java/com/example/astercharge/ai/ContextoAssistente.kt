package com.example.astercharge.ai

import com.example.astercharge.data.CarregadorInfo
import com.example.astercharge.data.SessaoRecarga
import com.example.astercharge.util.CondicaoSolar
import com.example.astercharge.util.PrecificacaoSolar
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ContextoAssistente(
    val agoraMillis: Long,
    val nomeUsuario: String,
    val localizacao: GeoPoint?,
    val enderecoAtual: String?,
    val bateriaVeiculoPercentual: Double,
    val capacidadeBateriaKwh: Double,
    val consumoKmPorKwh: Double,
    val sessao: SessaoRecarga,
    val carregadorSelecionado: CarregadorInfo,
    val carregadores: List<CarregadorInfo>,
    val idsEmManutencao: List<String>,
    val filaPorCarregador: Map<String, Int>,
    val condicaoSolar: CondicaoSolar?
) {

    val autonomiaKm: Double
        get() = bateriaVeiculoPercentual / 100.0 * capacidadeBateriaKwh * consumoKmPorKwh

    fun statusDe(carregador: CarregadorInfo): String {
        return when {
            idsEmManutencao.contains(carregador.id) -> "em manutenção"
            PlanejadorViagem.estaOcupado(carregador) -> "ocupado"
            else -> "livre"
        }
    }

    fun paraTexto(): String {
        val formato = SimpleDateFormat("EEEE, dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
        val hora = PlanejadorViagem.horaDoDia(agoraMillis)

        val textoLocalizacao = if (localizacao != null) {
            val endereco = if (enderecoAtual != null) " ($enderecoAtual)" else ""
            "latitude ${"%.5f".format(Locale.US, localizacao.latitude)}, " +
                "longitude ${"%.5f".format(Locale.US, localizacao.longitude)}$endereco"
        } else {
            "indisponível (o usuário pode não ter concedido a permissão de localização ou o GPS está desligado)"
        }

        val proximos = if (localizacao != null) {
            carregadores
                .sortedBy { PlanejadorViagem.distanciaKm(localizacao, it) }
                .take(5)
                .joinToString("\n") { c ->
                    val distancia = PlanejadorViagem.distanciaKm(localizacao, c)
                    "- ${c.nome} (id ${c.id}): ${"%.1f".format(Locale.US, distancia)} km em linha reta, ${statusDe(c)}, " +
                        "${c.tipo}, ${c.potenciaNominalKw} kW"
                }
        } else {
            "indisponível sem localização"
        }

        val tarifaSelecionada = PlanejadorViagem.tarifaNoInstante(
            carregadorSelecionado,
            agoraMillis,
            agoraMillis,
            condicaoSolar
        )

        val descontoSolar = PlanejadorViagem.horaDoDia(agoraMillis).let {
            PrecificacaoSolar.descontoPercentual(it, condicaoSolar)
        }

        return """
Data e hora atuais: ${formato.format(Date(agoraMillis))}
Hora cheia atual: ${hora}h
Usuário: $nomeUsuario

Localização atual do usuário: $textoLocalizacao

Veículo do usuário:
- Bateria atual: ${"%.0f".format(Locale.US, bateriaVeiculoPercentual)}%
- Capacidade da bateria: ${"%.0f".format(Locale.US, capacidadeBateriaKwh)} kWh
- Consumo médio considerado: ${"%.1f".format(Locale.US, consumoKmPorKwh)} km por kWh
- Autonomia estimada com a carga atual: ${"%.0f".format(Locale.US, autonomiaKm)} km

Sessão de recarga:
- Recarga ativa: ${if (sessao.ativa) "sim" else "não"}
- Energia entregue: ${"%.2f".format(Locale.US, sessao.kwhEntregue)} kWh
- Tempo de sessão: ${sessao.tempoDecorridoMinutos} minutos
- Tempo restante estimado: ${sessao.tempoRestanteEstimadoMinutos} minutos
- Potência atual: ${"%.1f".format(Locale.US, sessao.potenciaAtualKw)} kW
- Custo atual: R$ ${"%.2f".format(Locale.US, sessao.custoTotal)}
- Multa de ociosidade: R$ ${"%.2f".format(Locale.US, sessao.multaOciosidade)}
- Utilização da rede: ${"%.1f".format(Locale.US, sessao.percentualUtilizacaoRede)}%
- Throttle ativo: ${if (sessao.throttleAtivo) "sim" else "não"}

Eletroposto selecionado no mapa:
- ${carregadorSelecionado.nome} (id ${carregadorSelecionado.id}), ${carregadorSelecionado.tipo}, ${carregadorSelecionado.potenciaNominalKw} kW
- Situação: ${statusDe(carregadorSelecionado)}
- Fila: ${filaPorCarregador[carregadorSelecionado.id] ?: 0} pessoa(s)
- Tarifa estimada agora: R$ ${"%.2f".format(Locale.US, tarifaSelecionada)} por kWh (desconto solar atual de ${(descontoSolar * 100).toInt()}%)

Eletropostos mais próximos:
$proximos
        """.trimIndent()
    }
}
