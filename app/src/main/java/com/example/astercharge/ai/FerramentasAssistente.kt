package com.example.astercharge.ai

import com.example.astercharge.data.CarregadorInfo
import com.example.astercharge.util.ServicoRotas
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FerramentasAssistente(private val contexto: ContextoAssistente) {

    companion object {

        val declaracoes: List<FunctionDeclaration> = listOf(
            FunctionDeclaration(
                "calcular_rota",
                "Calcula distância e tempo de viagem de carro entre a localização atual do usuário e um destino, " +
                    "considerando o trânsito previsto para o horário de saída.",
                mapOf(
                    "destino" to Schema.string("Nome ou endereço do destino, por exemplo: Aeroporto de Congonhas, São Paulo."),
                    "horario_saida" to Schema.string("Horário de saída no formato HH:mm de 24 horas. Omitir para sair agora."),
                    "saida_em_minutos" to Schema.integer("Quantos minutos a partir de agora o usuário vai sair. Alternativa ao horario_saida.")
                ),
                listOf("horario_saida", "saida_em_minutos")
            ),
            FunctionDeclaration(
                "planejar_viagem",
                "Planeja uma viagem de carro elétrico: calcula rota com trânsito, consumo de energia, bateria na chegada, " +
                    "se é preciso recarregar, qual eletroposto usar, quanto tempo e quanto custa a recarga, e a que horas " +
                    "o usuário deve sair para o eletroposto e começar a carregar para conseguir sair no horário desejado.",
                mapOf(
                    "destino" to Schema.string("Nome ou endereço do destino."),
                    "horario_saida" to Schema.string("Horário desejado de saída para o destino no formato HH:mm de 24 horas. Omitir se o usuário não informou."),
                    "saida_em_minutos" to Schema.integer("Quantos minutos a partir de agora o usuário quer sair. Alternativa ao horario_saida."),
                    "ida_e_volta" to Schema.boolean("Verdadeiro se o usuário vai voltar sem recarregar no destino.")
                ),
                listOf("horario_saida", "saida_em_minutos", "ida_e_volta")
            ),
            FunctionDeclaration(
                "estimar_recarga",
                "Estima tempo, energia e custo para recarregar o veículo do usuário até uma bateria alvo em um eletroposto.",
                mapOf(
                    "bateria_alvo_percentual" to Schema.integer("Percentual de bateria desejado, de 1 a 100."),
                    "id_eletroposto" to Schema.string("Id do eletroposto. Omitir para usar o eletroposto selecionado no mapa."),
                    "bateria_inicial_percentual" to Schema.integer("Percentual inicial de bateria. Omitir para usar a bateria atual do veículo.")
                ),
                listOf("id_eletroposto", "bateria_inicial_percentual")
            ),
            FunctionDeclaration(
                "listar_eletropostos_proximos",
                "Lista os eletropostos mais próximos da localização atual do usuário, com situação, potência, vagas, fila e tarifa atual.",
                mapOf(
                    "limite" to Schema.integer("Quantidade máxima de eletropostos, de 1 a 8.")
                ),
                listOf("limite")
            )
        )
    }

    suspend fun executar(nome: String, argumentos: Map<String, JsonElement>): JsonObject {
        return try {
            when (nome) {
                "calcular_rota" -> calcularRota(argumentos)
                "planejar_viagem" -> planejarViagem(argumentos)
                "estimar_recarga" -> estimarRecarga(argumentos)
                "listar_eletropostos_proximos" -> listarEletropostos(argumentos)
                else -> erro("Ferramenta desconhecida: $nome")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            erro("Falha ao executar $nome: ${e.message}")
        }
    }

    private suspend fun calcularRota(argumentos: Map<String, JsonElement>): JsonObject {
        val textoDestino = argumentos.texto("destino") ?: return erro("Informe o destino.")
        val origem = contexto.localizacao
            ?: return erro("Localização atual indisponível. Peça ao usuário para permitir o acesso à localização e ligar o GPS.")

        val saida = interpretarSaida(argumentos) ?: contexto.agoraMillis
        val destino = ServicoRotas.geocodificar(textoDestino, origem)
            ?: return erro("Não encontrei o destino \"$textoDestino\". Peça um endereço mais específico.")
        val rota = ServicoRotas.calcularRota(origem, destino, saida)
            ?: return erro("Não foi possível calcular a rota agora. Serviço de rotas indisponível.")

        return buildJsonObject {
            put("destino_encontrado", rota.destino.nome)
            put("distancia_km", arredondar(rota.distanciaKm, 1))
            put("duracao_sem_transito_min", rota.duracaoSemTransitoMin)
            put("duracao_com_transito_min", rota.duracaoComTransitoMin)
            put("fonte_transito", rota.fonteTransito)
            put("saida_prevista", formatarHorario(saida))
            put("chegada_prevista", formatarHorario(saida + rota.duracaoComTransitoMin * 60_000L))
        }
    }

    private suspend fun planejarViagem(argumentos: Map<String, JsonElement>): JsonObject {
        val textoDestino = argumentos.texto("destino") ?: return erro("Informe o destino.")
        val origem = contexto.localizacao
            ?: return erro("Localização atual indisponível. Peça ao usuário para permitir o acesso à localização e ligar o GPS.")

        val saidaInformada = interpretarSaida(argumentos)
        val idaEVolta = argumentos["ida_e_volta"]?.jsonPrimitive?.booleanOrNull ?: false

        val destino = ServicoRotas.geocodificar(textoDestino, origem)
            ?: return erro("Não encontrei o destino \"$textoDestino\". Peça um endereço mais específico.")
        val rota = ServicoRotas.calcularRota(origem, destino, saidaInformada ?: contexto.agoraMillis)
            ?: return erro("Não foi possível calcular a rota agora. Serviço de rotas indisponível.")

        val plano = PlanejadorViagem.planejar(
            rota = rota,
            origem = origem,
            bateriaAtual = contexto.bateriaVeiculoPercentual,
            capacidadeKwh = contexto.capacidadeBateriaKwh,
            consumoKmPorKwh = contexto.consumoKmPorKwh,
            idaEVolta = idaEVolta,
            saidaInformadaMillis = saidaInformada,
            agoraMillis = contexto.agoraMillis,
            carregadores = contexto.carregadores,
            idsEmManutencao = contexto.idsEmManutencao,
            filas = contexto.filaPorCarregador,
            condicaoSolar = contexto.condicaoSolar
        )

        return buildJsonObject {
            put("destino_encontrado", rota.destino.nome)
            put("distancia_ida_km", arredondar(rota.distanciaKm, 1))
            put("distancia_total_considerada_km", arredondar(plano.distanciaTotalKm, 1))
            put("ida_e_volta", plano.idaEVolta)
            put("duracao_sem_transito_min", rota.duracaoSemTransitoMin)
            put("duracao_com_transito_min", rota.duracaoComTransitoMin)
            put("fonte_transito", rota.fonteTransito)
            put("bateria_atual_percentual", arredondar(plano.bateriaAtual, 0))
            put("autonomia_atual_km", arredondar(plano.autonomiaAtualKm, 0))
            put("energia_da_viagem_kwh", arredondar(plano.energiaViagemKwh, 1))
            put("bateria_minima_para_viagem_com_reserva_percentual", arredondar(plano.bateriaMinimaNecessaria, 0))
            put("precisa_recarregar", plano.precisaRecarregar)
            put("bateria_alvo_da_recarga_percentual", arredondar(plano.bateriaAlvo, 0))
            put("bateria_ao_chegar_no_destino_percentual", arredondar(plano.bateriaAoChegarNoDestino, 0))
            put("bateria_ao_final_do_percurso_percentual", arredondar(plano.bateriaAoFinalDoPercurso, 0))
            put("horario_de_saida_para_o_destino", formatarHorario(plano.saidaMillis))
            put("horario_de_chegada_ao_destino", formatarHorario(plano.chegadaMillis))
            put("horario_de_saida_informado_e_viavel", plano.saidaInformadaViavel)

            plano.saidaMaisCedoPossivelMillis?.let {
                put("saida_mais_cedo_possivel_para_o_destino", formatarHorario(it))
            }

            plano.opcao?.let { opcao ->
                put("eletroposto_recomendado", buildJsonObject {
                    put("id", opcao.carregador.id)
                    put("nome", opcao.carregador.nome)
                    put("tipo", opcao.carregador.tipo)
                    put("potencia_kw", opcao.carregador.potenciaNominalKw)
                    put("situacao", contexto.statusDe(opcao.carregador))
                    put("distancia_ate_ele_km", arredondar(opcao.distanciaKm, 1))
                    put("tempo_de_deslocamento_min", opcao.deslocamentoMin)
                    put("espera_estimada_min", opcao.esperaMin)
                    put("tempo_de_recarga_min", opcao.tempoRecargaMin)
                    put("energia_a_recarregar_kwh", arredondar(opcao.kwh, 1))
                    put("tarifa_estimada_kwh", arredondar(plano.tarifaKwh, 2))
                    put("custo_estimado_com_taxa_de_conexao", arredondar(plano.custoEstimado, 2))
                })
                plano.saidaParaEletropostoMillis?.let { put("sair_para_o_eletroposto_as", formatarHorario(it)) }
                plano.chegadaNoEletropostoMillis?.let { put("chegar_no_eletroposto_as", formatarHorario(it)) }
                plano.inicioRecargaMillis?.let { put("iniciar_recarga_as", formatarHorario(it)) }
                plano.fimRecargaMillis?.let { put("recarga_termina_as", formatarHorario(it)) }
            }

            put("observacoes", buildJsonArray {
                plano.observacoes.forEach { add(JsonPrimitive(it)) }
                add(JsonPrimitive("Estimativa baseada em consumo médio de ${arredondar(contexto.consumoKmPorKwh, 1)} km/kWh, reserva de segurança de ${PlanejadorViagem.RESERVA_MINIMA_PERCENTUAL.roundToInt()}%, folga de ${PlanejadorViagem.FOLGA_RECARGA_PERCENTUAL.roundToInt()} pontos percentuais na meta de recarga e margem de ${PlanejadorViagem.MARGEM_ANTES_DA_SAIDA_MIN} minutos antes da saída."))
            })
        }
    }

    private fun estimarRecarga(argumentos: Map<String, JsonElement>): JsonObject {
        val alvo = argumentos.numero("bateria_alvo_percentual")?.let { min(100.0, max(1.0, it)) }
            ?: return erro("Informe a bateria alvo em percentual.")
        val inicial = argumentos.numero("bateria_inicial_percentual") ?: contexto.bateriaVeiculoPercentual

        val idInformado = argumentos.texto("id_eletroposto")
        val carregador = if (idInformado != null) {
            contexto.carregadores.firstOrNull { it.id.equals(idInformado, ignoreCase = true) }
                ?: return erro("Eletroposto $idInformado não encontrado.")
        } else {
            contexto.carregadorSelecionado
        }

        if (inicial >= alvo) {
            return buildJsonObject {
                put("mensagem", "A bateria já está em ${arredondar(inicial, 0)}%, igual ou acima do alvo de ${arredondar(alvo, 0)}%.")
            }
        }

        val minutos = PlanejadorViagem.estimarTempoAteAlvo(
            bateriaInicial = inicial,
            bateriaAlvo = alvo,
            potenciaNominalKw = carregador.potenciaNominalKw,
            capacidadeBateriaKwh = contexto.capacidadeBateriaKwh
        )
        val kwh = (alvo - inicial) / 100.0 * contexto.capacidadeBateriaKwh
        val termino = contexto.agoraMillis + minutos * 60_000L
        val tarifa = PlanejadorViagem.tarifaNoInstante(carregador, contexto.agoraMillis, contexto.agoraMillis, contexto.condicaoSolar)
        val custo = kwh * tarifa + PlanejadorViagem.TAXA_CONEXAO
        val autonomiaGanha = (alvo - inicial) / 100.0 * contexto.capacidadeBateriaKwh * contexto.consumoKmPorKwh

        return buildJsonObject {
            put("eletroposto", carregador.nome)
            put("situacao_do_eletroposto", contexto.statusDe(carregador))
            put("bateria_inicial_percentual", arredondar(inicial, 0))
            put("bateria_alvo_percentual", arredondar(alvo, 0))
            put("tempo_de_recarga_min", minutos)
            put("energia_kwh", arredondar(kwh, 1))
            put("autonomia_ganha_km", arredondar(autonomiaGanha, 0))
            put("tarifa_estimada_kwh", arredondar(tarifa, 2))
            put("custo_estimado_com_taxa_de_conexao", arredondar(custo, 2))
            put("terminaria_as_se_iniciar_agora", formatarHorario(termino))
        }
    }

    private fun listarEletropostos(argumentos: Map<String, JsonElement>): JsonObject {
        val origem = contexto.localizacao
            ?: return erro("Localização atual indisponível. Peça ao usuário para permitir o acesso à localização e ligar o GPS.")
        val limite = (argumentos.numero("limite")?.toInt() ?: 5).coerceIn(1, 8)

        val ordenados = contexto.carregadores
            .sortedBy { PlanejadorViagem.distanciaKm(origem, it) }
            .take(limite)

        return buildJsonObject {
            put("eletropostos", buildJsonArray {
                ordenados.forEach { add(descreverEletroposto(it, origem)) }
            })
        }
    }

    private fun descreverEletroposto(carregador: CarregadorInfo, origem: org.osmdroid.util.GeoPoint): JsonObject {
        val tarifa = PlanejadorViagem.tarifaNoInstante(
            carregador,
            contexto.agoraMillis,
            contexto.agoraMillis,
            contexto.condicaoSolar
        )
        return buildJsonObject {
            put("id", carregador.id)
            put("nome", carregador.nome)
            put("tipo", carregador.tipo)
            put("potencia_kw", carregador.potenciaNominalKw)
            put("distancia_linha_reta_km", arredondar(PlanejadorViagem.distanciaKm(origem, carregador), 1))
            put("situacao", contexto.statusDe(carregador))
            put("vagas_disponiveis", carregador.vagasDisponiveis)
            put("vagas_totais", carregador.vagasTotais)
            put("pessoas_na_fila", contexto.filaPorCarregador[carregador.id] ?: 0)
            put("tarifa_atual_kwh", arredondar(tarifa, 2))
        }
    }

    private fun interpretarSaida(argumentos: Map<String, JsonElement>): Long? {
        val minutos = argumentos.numero("saida_em_minutos")
        if (minutos != null) {
            return contexto.agoraMillis + max(0.0, minutos).toLong() * 60_000L
        }

        val texto = argumentos.texto("horario_saida") ?: return null
        val encontrado = Regex("(\\d{1,2})(?:\\s*[:hH]\\s*(\\d{2}))?").find(texto) ?: return null
        val hora = encontrado.groupValues[1].toInt()
        val minuto = encontrado.groupValues[2].ifEmpty { "0" }.toInt()
        if (hora !in 0..23 || minuto !in 0..59) return null

        val calendario = Calendar.getInstance().apply {
            timeInMillis = contexto.agoraMillis
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendario.timeInMillis < contexto.agoraMillis - 60_000L) {
            calendario.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendario.timeInMillis
    }

    private fun formatarHorario(millis: Long): String {
        val alvo = Calendar.getInstance().apply { timeInMillis = millis }
        val hoje = Calendar.getInstance().apply { timeInMillis = contexto.agoraMillis }
        val mesmoDia = alvo.get(Calendar.YEAR) == hoje.get(Calendar.YEAR) &&
            alvo.get(Calendar.DAY_OF_YEAR) == hoje.get(Calendar.DAY_OF_YEAR)
        val padrao = if (mesmoDia) "HH:mm" else "HH:mm 'do dia' dd/MM"
        return SimpleDateFormat(padrao, Locale.forLanguageTag("pt-BR")).format(Date(millis))
    }

    private fun erro(mensagem: String): JsonObject {
        return buildJsonObject { put("erro", mensagem) }
    }

    private fun arredondar(valor: Double, casas: Int): Double {
        var fator = 1.0
        repeat(casas) { fator *= 10.0 }
        return Math.round(valor * fator) / fator
    }

    private fun Map<String, JsonElement>.texto(chave: String): String? {
        val primitivo = this[chave] as? JsonPrimitive ?: return null
        return primitivo.content.trim().ifEmpty { null }
    }

    private fun Map<String, JsonElement>.numero(chave: String): Double? {
        val primitivo = this[chave] as? JsonPrimitive ?: return null
        return primitivo.doubleOrNull
    }
}
