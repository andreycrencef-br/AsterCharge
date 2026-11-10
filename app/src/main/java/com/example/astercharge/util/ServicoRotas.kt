package com.example.astercharge.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

object ConfigApi {
    const val GOOGLE_ROUTES_API_KEY = ""
}

data class DestinoResolvido(
    val nome: String,
    val latitude: Double,
    val longitude: Double
)

data class RotaCalculada(
    val destino: DestinoResolvido,
    val distanciaKm: Double,
    val duracaoSemTransitoMin: Int,
    val duracaoComTransitoMin: Int,
    val fonteTransito: String
)

object ServicoRotas {

    private const val USER_AGENT = "AsterCharge/1.0 (projeto academico)"

    suspend fun geocodificar(consulta: String, perto: GeoPoint?): DestinoResolvido? {
        val texto = URLEncoder.encode(consulta, "UTF-8")
        val viewbox = perto?.let {
            val esquerda = it.longitude - 0.6
            val direita = it.longitude + 0.6
            val topo = it.latitude + 0.6
            val base = it.latitude - 0.6
            "&viewbox=$esquerda,$topo,$direita,$base"
        } ?: ""

        val resposta = requisitar(
            "https://nominatim.openstreetmap.org/search?q=$texto&format=json&limit=1&countrycodes=br$viewbox"
        ) ?: return null

        return try {
            val itens = JSONArray(resposta)
            if (itens.length() == 0) {
                null
            } else {
                val item = itens.getJSONObject(0)
                DestinoResolvido(
                    nome = item.optString("display_name", consulta).split(",").take(3).joinToString(",").trim(),
                    latitude = item.getString("lat").toDouble(),
                    longitude = item.getString("lon").toDouble()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun enderecoDe(ponto: GeoPoint): String? {
        val resposta = requisitar(
            "https://nominatim.openstreetmap.org/reverse?lat=${ponto.latitude}&lon=${ponto.longitude}&format=json&zoom=17"
        ) ?: return null

        return try {
            val endereco = JSONObject(resposta).optJSONObject("address")
            if (endereco == null) {
                null
            } else {
                val via = endereco.optString("road", "")
                val bairro = endereco.optString("suburb", endereco.optString("neighbourhood", ""))
                val cidade = endereco.optString(
                    "city",
                    endereco.optString("town", endereco.optString("municipality", ""))
                )
                listOf(via, bairro, cidade).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun calcularRota(
        origem: GeoPoint,
        destino: DestinoResolvido,
        saidaMillis: Long
    ): RotaCalculada? {
        if (ConfigApi.GOOGLE_ROUTES_API_KEY.isNotBlank()) {
            val rotaGoogle = rotaGoogle(origem, destino, saidaMillis)
            if (rotaGoogle != null) return rotaGoogle
        }
        return rotaOsrm(origem, destino, saidaMillis)
    }

    private suspend fun rotaGoogle(
        origem: GeoPoint,
        destino: DestinoResolvido,
        saidaMillis: Long
    ): RotaCalculada? {
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val partida = max(saidaMillis, System.currentTimeMillis() + 60_000L)

        val ponto = { latitude: Double, longitude: Double ->
            JSONObject().put(
                "location",
                JSONObject().put(
                    "latLng",
                    JSONObject().put("latitude", latitude).put("longitude", longitude)
                )
            )
        }

        val corpo = JSONObject()
            .put("origin", ponto(origem.latitude, origem.longitude))
            .put("destination", ponto(destino.latitude, destino.longitude))
            .put("travelMode", "DRIVE")
            .put("routingPreference", "TRAFFIC_AWARE")
            .put("departureTime", formato.format(partida))
            .toString()

        val resposta = requisitar(
            url = "https://routes.googleapis.com/directions/v2:computeRoutes",
            metodo = "POST",
            corpo = corpo,
            cabecalhos = mapOf(
                "Content-Type" to "application/json",
                "X-Goog-Api-Key" to ConfigApi.GOOGLE_ROUTES_API_KEY,
                "X-Goog-FieldMask" to "routes.duration,routes.staticDuration,routes.distanceMeters"
            )
        ) ?: return null

        return try {
            val rota = JSONObject(resposta).getJSONArray("routes").getJSONObject(0)
            val comTransito = rota.getString("duration").removeSuffix("s").toDouble()
            val semTransito = rota.optString("staticDuration", rota.getString("duration"))
                .removeSuffix("s")
                .toDouble()
            RotaCalculada(
                destino = destino,
                distanciaKm = rota.getDouble("distanceMeters") / 1000.0,
                duracaoSemTransitoMin = (semTransito / 60.0).roundToInt(),
                duracaoComTransitoMin = (comTransito / 60.0).roundToInt(),
                fonteTransito = "Google Routes (trânsito previsto para o horário)"
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun rotaOsrm(
        origem: GeoPoint,
        destino: DestinoResolvido,
        saidaMillis: Long
    ): RotaCalculada? {
        val resposta = requisitar(
            "https://router.project-osrm.org/route/v1/driving/" +
                "${origem.longitude},${origem.latitude};${destino.longitude},${destino.latitude}?overview=false"
        ) ?: return null

        return try {
            val rota = JSONObject(resposta).getJSONArray("routes").getJSONObject(0)
            val semTransito = rota.getDouble("duration") / 60.0
            val fator = fatorTransitoPorHorario(saidaMillis)
            RotaCalculada(
                destino = destino,
                distanciaKm = rota.getDouble("distance") / 1000.0,
                duracaoSemTransitoMin = semTransito.roundToInt(),
                duracaoComTransitoMin = (semTransito * fator).roundToInt(),
                fonteTransito = "estimativa por horário (fator de trânsito ${String.format(Locale.US, "%.2f", fator)})"
            )
        } catch (e: Exception) {
            null
        }
    }

    fun fatorTransitoPorHorario(saidaMillis: Long): Double {
        val calendario = Calendar.getInstance().apply { timeInMillis = saidaMillis }
        val hora = calendario.get(Calendar.HOUR_OF_DAY) + calendario.get(Calendar.MINUTE) / 60.0
        val diaUtil = calendario.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY

        return when {
            diaUtil && hora in 6.5..9.5 -> 1.6
            diaUtil && hora in 16.5..20.0 -> 1.75
            diaUtil && hora in 11.5..14.0 -> 1.2
            !diaUtil && hora in 11.0..19.0 -> 1.2
            else -> 1.0
        }
    }

    private suspend fun requisitar(
        url: String,
        metodo: String = "GET",
        corpo: String? = null,
        cabecalhos: Map<String, String> = emptyMap()
    ): String? = withContext(Dispatchers.IO) {
        try {
            val conexao = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = metodo
                setRequestProperty("User-Agent", USER_AGENT)
                cabecalhos.forEach { (chave, valor) -> setRequestProperty(chave, valor) }
                if (corpo != null) {
                    doOutput = true
                    outputStream.use { it.write(corpo.toByteArray(Charsets.UTF_8)) }
                }
            }
            if (conexao.responseCode !in 200..299) {
                null
            } else {
                conexao.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
