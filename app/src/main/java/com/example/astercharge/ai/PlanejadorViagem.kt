package com.example.astercharge.ai

import com.example.astercharge.core.ChargingEngine
import com.example.astercharge.data.CarregadorInfo
import com.example.astercharge.data.StatusSessao
import com.example.astercharge.util.CondicaoSolar
import com.example.astercharge.util.PrecificacaoSolar
import com.example.astercharge.util.RotaCalculada
import org.osmdroid.util.GeoPoint
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class OpcaoEletroposto(
    val carregador: CarregadorInfo,
    val distanciaKm: Double,
    val deslocamentoMin: Int,
    val esperaMin: Int,
    val tempoRecargaMin: Int,
    val kwh: Double
)

data class PlanoViagem(
    val rota: RotaCalculada,
    val idaEVolta: Boolean,
    val distanciaTotalKm: Double,
    val bateriaAtual: Double,
    val autonomiaAtualKm: Double,
    val energiaViagemKwh: Double,
    val bateriaMinimaNecessaria: Double,
    val viagemExigeMaisQueUmaCarga: Boolean,
    val precisaRecarregar: Boolean,
    val bateriaAlvo: Double,
    val bateriaAoChegarNoDestino: Double,
    val bateriaAoFinalDoPercurso: Double,
    val opcao: OpcaoEletroposto?,
    val tarifaKwh: Double,
    val custoEstimado: Double,
    val saidaParaEletropostoMillis: Long?,
    val chegadaNoEletropostoMillis: Long?,
    val inicioRecargaMillis: Long?,
    val fimRecargaMillis: Long?,
    val saidaMillis: Long,
    val chegadaMillis: Long,
    val saidaInformadaViavel: Boolean,
    val saidaMaisCedoPossivelMillis: Long?,
    val observacoes: List<String>
)

object PlanejadorViagem {

    const val RESERVA_MINIMA_PERCENTUAL = 10.0
    const val FOLGA_RECARGA_PERCENTUAL = 15.0
    const val MARGEM_ANTES_DA_SAIDA_MIN = 10
    const val VELOCIDADE_ATE_POSTO_KMH = 30.0
    const val FATOR_DISTANCIA_URBANA = 1.3
    const val ESPERA_BASE_OCUPADO_MIN = 20
    const val ESPERA_POR_PESSOA_NA_FILA_MIN = 15
    const val TAXA_CONEXAO = 2.50
    const val CONSUMO_PADRAO_KM_POR_KWH = 6.0

    fun estimarTempoAteAlvo(
        bateriaInicial: Double,
        bateriaAlvo: Double,
        potenciaNominalKw: Double,
        capacidadeBateriaKwh: Double
    ): Int {
        if (bateriaInicial >= bateriaAlvo) return 0

        var bateria = bateriaInicial
        var kwh = 0.0
        var minutos = 0

        while (bateria < bateriaAlvo && minutos < 1000) {
            val passo = ChargingEngine.simularMinuto(
                bateriaAtual = bateria,
                kwhEntregueAtual = kwh,
                potenciaNominalKw = potenciaNominalKw,
                capacidadeBateriaKwh = capacidadeBateriaKwh
            )
            bateria = passo.bateria
            kwh = passo.kwhEntregue
            minutos++
        }

        return minutos
    }

    fun tarifaEstimada(
        carregador: CarregadorInfo,
        hora: Int,
        condicaoSolar: CondicaoSolar?
    ): Double {
        var tarifa = carregador.precoBaseKwh
        if (hora in 17..22) tarifa *= 1.20
        if (hora in 0..6) tarifa *= 0.85
        return tarifa * (1 - PrecificacaoSolar.descontoPercentual(hora, condicaoSolar))
    }

    fun tarifaNoInstante(
        carregador: CarregadorInfo,
        instanteMillis: Long,
        agoraMillis: Long,
        condicaoSolar: CondicaoSolar?
    ): Double {
        val hora = horaDoDia(instanteMillis)
        val condicao = if (hora == horaDoDia(agoraMillis)) condicaoSolar else null
        return tarifaEstimada(carregador, hora, condicao)
    }

    fun horaDoDia(millis: Long): Int {
        return Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
    }

    fun estaOcupado(carregador: CarregadorInfo): Boolean {
        return carregador.statusSessao == StatusSessao.ATIVA ||
            carregador.statusSessao == StatusSessao.THROTTLE
    }

    fun distanciaKm(origem: GeoPoint, carregador: CarregadorInfo): Double {
        return origem.distanceToAsDouble(GeoPoint(carregador.latitude, carregador.longitude)) / 1000.0
    }

    fun montarOpcao(
        carregador: CarregadorInfo,
        origem: GeoPoint,
        bateriaInicial: Double,
        bateriaAlvo: Double,
        capacidadeKwh: Double,
        fila: Int
    ): OpcaoEletroposto {
        val distancia = distanciaKm(origem, carregador) * FATOR_DISTANCIA_URBANA
        val deslocamento = max(3, (distancia / VELOCIDADE_ATE_POSTO_KMH * 60.0).roundToInt())
        val espera = if (estaOcupado(carregador)) {
            ESPERA_BASE_OCUPADO_MIN + fila * ESPERA_POR_PESSOA_NA_FILA_MIN
        } else {
            fila * ESPERA_POR_PESSOA_NA_FILA_MIN
        }
        val tempoRecarga = estimarTempoAteAlvo(
            bateriaInicial = bateriaInicial,
            bateriaAlvo = bateriaAlvo,
            potenciaNominalKw = carregador.potenciaNominalKw,
            capacidadeBateriaKwh = capacidadeKwh
        )
        val kwh = max(0.0, (bateriaAlvo - bateriaInicial) / 100.0 * capacidadeKwh)

        return OpcaoEletroposto(
            carregador = carregador,
            distanciaKm = distancia,
            deslocamentoMin = deslocamento,
            esperaMin = espera,
            tempoRecargaMin = tempoRecarga,
            kwh = kwh
        )
    }

    fun melhorOpcao(
        carregadores: List<CarregadorInfo>,
        idsEmManutencao: List<String>,
        origem: GeoPoint,
        bateriaInicial: Double,
        bateriaAlvo: Double,
        capacidadeKwh: Double,
        filas: Map<String, Int>
    ): OpcaoEletroposto? {
        return carregadores
            .filter { !idsEmManutencao.contains(it.id) }
            .map {
                montarOpcao(
                    carregador = it,
                    origem = origem,
                    bateriaInicial = bateriaInicial,
                    bateriaAlvo = bateriaAlvo,
                    capacidadeKwh = capacidadeKwh,
                    fila = filas[it.id] ?: 0
                )
            }
            .minByOrNull { it.deslocamentoMin + it.esperaMin + it.tempoRecargaMin }
    }

    fun planejar(
        rota: RotaCalculada,
        origem: GeoPoint,
        bateriaAtual: Double,
        capacidadeKwh: Double,
        consumoKmPorKwh: Double,
        idaEVolta: Boolean,
        saidaInformadaMillis: Long?,
        agoraMillis: Long,
        carregadores: List<CarregadorInfo>,
        idsEmManutencao: List<String>,
        filas: Map<String, Int>,
        condicaoSolar: CondicaoSolar?
    ): PlanoViagem {
        val consumo = if (consumoKmPorKwh > 0.0) consumoKmPorKwh else CONSUMO_PADRAO_KM_POR_KWH
        val distanciaTotal = rota.distanciaKm * if (idaEVolta) 2.0 else 1.0
        val energiaKwh = distanciaTotal / consumo
        val percentualViagem = energiaKwh / capacidadeKwh * 100.0
        val percentualIda = rota.distanciaKm / consumo / capacidadeKwh * 100.0
        val bateriaMinima = percentualViagem + RESERVA_MINIMA_PERCENTUAL
        val precisaRecarregar = bateriaAtual < bateriaMinima
        val exigeMaisQueUmaCarga = bateriaMinima > 100.0
        val alvo = if (precisaRecarregar) min(100.0, ceil(bateriaMinima + FOLGA_RECARGA_PERCENTUAL)) else bateriaAtual
        val autonomia = bateriaAtual / 100.0 * capacidadeKwh * consumo

        val observacoes = mutableListOf<String>()
        val duracaoViagemMillis = rota.duracaoComTransitoMin * 60_000L
        val margemMillis = MARGEM_ANTES_DA_SAIDA_MIN * 60_000L

        val opcao = if (precisaRecarregar) {
            melhorOpcao(carregadores, idsEmManutencao, origem, bateriaAtual, alvo, capacidadeKwh, filas)
        } else {
            null
        }

        var saida = saidaInformadaMillis ?: agoraMillis
        var saidaParaPosto: Long? = null
        var chegadaNoPosto: Long? = null
        var inicioRecarga: Long? = null
        var fimRecarga: Long? = null
        var saidaMaisCedo: Long? = null
        var saidaViavel = true
        var tarifa = 0.0
        var custo = 0.0

        if (opcao != null) {
            val blocoMillis = (opcao.deslocamentoMin + opcao.esperaMin + opcao.tempoRecargaMin) * 60_000L + margemMillis
            saidaMaisCedo = agoraMillis + blocoMillis

            if (saidaInformadaMillis == null) {
                saida = saidaMaisCedo
            } else {
                saidaViavel = saidaInformadaMillis >= saidaMaisCedo - 60_000L
            }

            val saidaEfetiva = if (saidaViavel) saida else saidaMaisCedo
            saidaParaPosto = saidaEfetiva - blocoMillis
            chegadaNoPosto = saidaParaPosto + opcao.deslocamentoMin * 60_000L
            inicioRecarga = chegadaNoPosto + opcao.esperaMin * 60_000L
            fimRecarga = inicioRecarga + opcao.tempoRecargaMin * 60_000L

            tarifa = tarifaNoInstante(opcao.carregador, inicioRecarga, agoraMillis, condicaoSolar)
            custo = opcao.kwh * tarifa + TAXA_CONEXAO

            val horaInicio = horaDoDia(inicioRecarga)
            val horaFim = horaDoDia(fimRecarga)
            if (horaInicio in 17..22 || horaFim in 17..22) {
                observacoes.add("A recarga cai no horário de pico (17h às 22h), com acréscimo de 20% na tarifa.")
            }
            if (opcao.esperaMin > 0) {
                observacoes.add(
                    "O eletroposto escolhido está ocupado ou com fila; a espera de ${opcao.esperaMin} min é uma estimativa."
                )
            }
            if (!saidaViavel) {
                observacoes.add("O horário de saída informado não deixa tempo suficiente para a recarga necessária.")
            }
        } else if (precisaRecarregar) {
            observacoes.add("Nenhum eletroposto disponível foi encontrado para a recarga.")
        }

        if (exigeMaisQueUmaCarga) {
            observacoes.add(
                "A viagem exige mais do que uma bateria cheia com a reserva de segurança; será preciso uma parada extra de recarga no trajeto."
            )
        }

        val saidaFinal = if (opcao != null && !saidaViavel) saidaMaisCedo ?: saida else saida
        val bateriaNaSaida = if (precisaRecarregar) alvo else bateriaAtual

        return PlanoViagem(
            rota = rota,
            idaEVolta = idaEVolta,
            distanciaTotalKm = distanciaTotal,
            bateriaAtual = bateriaAtual,
            autonomiaAtualKm = autonomia,
            energiaViagemKwh = energiaKwh,
            bateriaMinimaNecessaria = bateriaMinima,
            viagemExigeMaisQueUmaCarga = exigeMaisQueUmaCarga,
            precisaRecarregar = precisaRecarregar,
            bateriaAlvo = alvo,
            bateriaAoChegarNoDestino = bateriaNaSaida - percentualIda,
            bateriaAoFinalDoPercurso = bateriaNaSaida - percentualViagem,
            opcao = opcao,
            tarifaKwh = tarifa,
            custoEstimado = custo,
            saidaParaEletropostoMillis = saidaParaPosto,
            chegadaNoEletropostoMillis = chegadaNoPosto,
            inicioRecargaMillis = inicioRecarga,
            fimRecargaMillis = fimRecarga,
            saidaMillis = saidaFinal,
            chegadaMillis = saidaFinal + duracaoViagemMillis,
            saidaInformadaViavel = saidaViavel,
            saidaMaisCedoPossivelMillis = saidaMaisCedo,
            observacoes = observacoes
        )
    }
}
