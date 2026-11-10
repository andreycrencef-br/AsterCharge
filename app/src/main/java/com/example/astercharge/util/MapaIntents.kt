package com.example.astercharge.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * Não precisa de API key: usamos os esquemas de URI públicos do Android
 * (geo:) e do Waze (waze://) para abrir os apps de navegação instalados.
 * Se nenhum estiver instalado, caímos para o navegador com o link do
 * Google Maps.
 */
object MapaIntents {

    fun abrirNoGoogleMaps(context: Context, latitude: Double, longitude: Double, nome: String) {
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(nome)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            abrirNoNavegador(context, latitude, longitude)
        }
    }

    fun abrirNoWaze(context: Context, latitude: Double, longitude: Double) {
        val uri = "waze://?ll=$latitude,$longitude&navigate=yes".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.waze")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Waze não instalado: leva o usuário para instalar, ou cai no navegador.
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://waze.com/ul?ll=$latitude,$longitude&navigate=yes".toUri()
                    )
                )
            } catch (e2: ActivityNotFoundException) {
                abrirNoNavegador(context, latitude, longitude)
            }
        }
    }

    private fun abrirNoNavegador(context: Context, latitude: Double, longitude: Double) {
        val uri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    /**
     * Mostra o seletor nativo do Android com todos os apps de navegação
     * instalados (Waze, Google Maps, etc). É a forma mais simples de dar
     * ao usuário a escolha, sem termos que checar app por app.
     */
    fun abrirSeletorDeNavegacao(context: Context, latitude: Double, longitude: Double, nome: String) {
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(nome)})".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(Intent.createChooser(intent, "Ir até $nome com"))
        } catch (e: ActivityNotFoundException) {
            abrirNoNavegador(context, latitude, longitude)
        }
    }
}
