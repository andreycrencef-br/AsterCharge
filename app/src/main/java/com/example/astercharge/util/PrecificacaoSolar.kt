package com.example.astercharge.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Condição solar/climática atual, usada para a precificação dinâmica.
 *
 * @property disponivel se a consulta à API deu certo (sem internet, cai em
 *   um cálculo de reserva baseado só no horário — ver [PrecificacaoSolar]).
 * @property irradiacaoWm2 irradiância solar (W/m²) — quanto maior, mais sol.
 * @property nebulosidadePercentual cobertura de nuvens (0–100%).
 */
data class CondicaoSolar(
    val disponivel: Boolean,
    val irradiacaoWm2: Double,
    val nebulosidadePercentual: Double
)

/**
 * Busca a irradiância solar e a nebulosidade atuais via Open-Meteo
 * (https://open-meteo.com), uma API meteorológica pública e gratuita que
 * não exige chave de API nem cadastro. Coordenadas fixas em São Paulo,
 * mesma região usada no mapa de demonstração.
 *
 * Esse dado é o que alimenta o principal diferencial do AsterCharge: a
 * tarifa fica mais barata quando existe geração solar aproveitável.
 */
object ServicoSolar {

    private const val LATITUDE = -23.5733
    private const val LONGITUDE = -46.6235

    suspend fun buscarCondicaoAtual(): CondicaoSolar = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$LATITUDE&longitude=$LONGITUDE" +
                    "&current=shortwave_radiation,cloud_cover" +
                    "&timezone=America%2FSao_Paulo"
            )

            val conexao = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }

            val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
            val atual = JSONObject(corpo).getJSONObject("current")

            CondicaoSolar(
                disponivel = true,
                irradiacaoWm2 = atual.optDouble("shortwave_radiation", 0.0),
                nebulosidadePercentual = atual.optDouble("cloud_cover", 100.0)
            )
        } catch (e: Exception) {
            // Sem internet ou API fora do ar: a precificação cai para uma
            // estimativa só pelo horário (ver PrecificacaoSolar).
            CondicaoSolar(disponivel = false, irradiacaoWm2 = 0.0, nebulosidadePercentual = 100.0)
        }
    }
}

/**
 * Regra de negócio da precificação dinâmica por energia solar.
 *
 * Ideia: entre 8h e 17h (janela de geração solar relevante no Brasil),
 * se o céu estiver favorável (alta irradiância / baixa nebulosidade),
 * o motorista recebe um desconto na tarifa — incentivando o carregamento
 * durante o pico de geração solar (menor impacto na rede/energia mais
 * "limpa" disponível).
 */
object PrecificacaoSolar {

    val JANELA_SOLAR = 8..17

    fun estaNaJanelaSolar(hora: Int): Boolean = hora in JANELA_SOLAR

    /** Retorna o desconto como fração (0.0 a 0.20 = até 20%). */
    fun descontoPercentual(hora: Int, condicao: CondicaoSolar?): Double {
        if (!estaNaJanelaSolar(hora)) return 0.0

        if (condicao == null || !condicao.disponivel) {
            // Sem dado de clima: estimativa conservadora só pelo horário
            // (meio do dia = mais provável ter sol forte).
            return if (hora in 10..15) 0.08 else 0.03
        }

        val irradiacao = condicao.irradiacaoWm2
        val nublado = condicao.nebulosidadePercentual

        return when {
            irradiacao >= 600 && nublado <= 30 -> 0.20
            irradiacao >= 300 && nublado <= 60 -> 0.12
            irradiacao >= 120 -> 0.05
            else -> 0.0
        }
    }

    fun descricaoCondicao(hora: Int, condicao: CondicaoSolar?): String {
        if (!estaNaJanelaSolar(hora)) {
            return "Fora do horário de geração solar (8h–17h)."
        }
        if (condicao == null || !condicao.disponivel) {
            return "Sem conexão para consultar o clima agora; aplicando estimativa por horário."
        }
        return "Irradiância: ${condicao.irradiacaoWm2.toInt()} W/m² • Nuvens: ${condicao.nebulosidadePercentual.toInt()}%"
    }
}
