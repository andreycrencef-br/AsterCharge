package com.example.astercharge.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.util.GeoPoint

object LocalizacaoAtual {

    fun temPermissao(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun obter(context: Context): GeoPoint? {
        if (!temPermissao(context)) return null

        val cliente = LocationServices.getFusedLocationProviderClient(context)

        return try {
            val fonteCancelamento = CancellationTokenSource()
            val atual = withTimeoutOrNull(6000L) {
                cliente.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    fonteCancelamento.token
                ).await()
            }
            if (atual == null) fonteCancelamento.cancel()

            val local = atual ?: cliente.lastLocation.await()
            if (local != null) GeoPoint(local.latitude, local.longitude) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun SolicitarPermissaoLocalizacaoAoAbrir() {
    val context = LocalContext.current

    val lancador = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (!LocalizacaoAtual.temPermissao(context)) {
            lancador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Composable
fun rememberLocalizacaoUsuario(): Pair<GeoPoint?, () -> Unit> {
    val context = LocalContext.current
    var localizacao by remember { mutableStateOf<GeoPoint?>(null) }

    val clienteLocalizacao = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun buscarLocalizacao() {
        if (!LocalizacaoAtual.temPermissao(context)) return
        try {
            clienteLocalizacao.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    localizacao = GeoPoint(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
        }
    }

    val lancadorPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val concedida = resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            resultado[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) buscarLocalizacao()
    }

    fun solicitar() {
        if (LocalizacaoAtual.temPermissao(context)) {
            buscarLocalizacao()
        } else {
            lancadorPermissao.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    return Pair(localizacao, ::solicitar)
}
