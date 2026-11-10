package com.example.astercharge.ui.operador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val VerdeAster = Color(0xFF5FA97A)
private val VermelhoAcrescimo = Color(0xFFE07A7A)

private val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val BrancoCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

private val CinzaTexto: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val TextoPrincipal: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable
fun TelaGestao(gestaoViewModel: GestaoViewModel) {

    val descontoSolarMaximo by gestaoViewModel.descontoSolarMaximo.collectAsState()
    val acrescimoPico by gestaoViewModel.acrescimoPico.collectAsState()
    val redistribuicaoAtiva by gestaoViewModel.redistribuicaoAtiva.collectAsState()

    var mostrarParametros by remember { mutableStateOf(false) }
    var mostrarRedistribuicao by remember { mutableStateOf(false) }

    val tarifaReferencia = GestaoViewModel.TARIFA_REFERENCIA_KWH
    val tarifaDesconto = tarifaReferencia * (1 - descontoSolarMaximo)
    val tarifaAcrescimo = tarifaReferencia * (1 + acrescimoPico)

    if (mostrarParametros) {
        DialogoParametrosTarifa(
            descontoSolarMaximo = descontoSolarMaximo,
            acrescimoPico = acrescimoPico,
            onSalvar = { desconto, acrescimo ->
                gestaoViewModel.definirDescontoSolarMaximo(desconto)
                gestaoViewModel.definirAcrescimoPico(acrescimo)
                mostrarParametros = false
            },
            onFechar = { mostrarParametros = false }
        )
    }

    if (mostrarRedistribuicao) {
        AlertDialog(
            onDismissRequest = { mostrarRedistribuicao = false },
            title = { Text("Redistribuição de carga", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Quando ativa, o sistema sugere que motoristas em fila sejam " +
                            "direcionados a eletropostos livres próximos durante picos de " +
                            "demanda, em vez de esperar no mesmo ponto."
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Redistribuição automática", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = redistribuicaoAtiva,
                            onCheckedChange = { gestaoViewModel.alternarRedistribuicao(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = VerdeAster)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarRedistribuicao = false }) { Text("Fechar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoClaro)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Gestão", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        }

        item {
            LinhaGestao(
                icone = Icons.Filled.Percent,
                titulo = "Parâmetro das tarifas",
                subtitulo = "Desconto solar e tarifa por horário",
                onClick = { mostrarParametros = true }
            )
        }

        item {
            LinhaGestao(
                icone = Icons.Filled.Sync,
                titulo = "Redistribuição",
                subtitulo = "Redireciona recargas em picos de demanda" +
                    if (redistribuicaoAtiva) " • Ativa" else " • Desativada",
                onClick = { mostrarRedistribuicao = true }
            )
        }

        item {
            Text(
                "Tarifas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            LinhaTarifa(
                icone = Icons.Filled.WbSunny,
                corIcone = VerdeAster,
                titulo = "Desconto",
                subtitulo = "Normal + Solar",
                valor = tarifaDesconto
            )
        }

        item {
            LinhaTarifa(
                icone = Icons.Filled.Bolt,
                corIcone = VerdeAster,
                titulo = "Tarifa Normal",
                subtitulo = "Pico + Solar | Normal sem Solar",
                valor = tarifaReferencia
            )
        }

        item {
            LinhaTarifa(
                icone = Icons.Filled.TrendingUp,
                corIcone = VermelhoAcrescimo,
                titulo = "Acréscimo",
                subtitulo = "Pico sem Solar",
                valor = tarifaAcrescimo
            )
        }
    }
}

@Composable
private fun LinhaGestao(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BrancoCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(VerdeAster, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextoPrincipal)
                Text(subtitulo, fontSize = 12.sp, color = CinzaTexto)
            }
        }
    }
}

@Composable
private fun LinhaTarifa(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    corIcone: Color,
    titulo: String,
    subtitulo: String,
    valor: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrancoCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(corIcone, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextoPrincipal)
                Text(subtitulo, fontSize = 11.sp, color = CinzaTexto)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                String.format(Locale.US, "R$ %.2f", valor),
                modifier = Modifier.width(96.dp),
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextoPrincipal
            )
        }
    }
}

@Composable
private fun DialogoParametrosTarifa(
    descontoSolarMaximo: Double,
    acrescimoPico: Double,
    onSalvar: (Double, Double) -> Unit,
    onFechar: () -> Unit
) {
    var desconto by remember { mutableStateOf(descontoSolarMaximo.toFloat()) }
    var acrescimo by remember { mutableStateOf(acrescimoPico.toFloat()) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Parâmetro das tarifas", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Desconto solar máximo: ${(desconto * 100).toInt()}%")
                Slider(
                    value = desconto,
                    onValueChange = { desconto = it },
                    valueRange = 0f..0.5f,
                    colors = SliderDefaults.colors(thumbColor = VerdeAster, activeTrackColor = VerdeAster)
                )
                Spacer(Modifier.height(12.dp))
                Text("Acréscimo em horário de pico: ${(acrescimo * 100).toInt()}%")
                Slider(
                    value = acrescimo,
                    onValueChange = { acrescimo = it },
                    valueRange = 0f..0.5f,
                    colors = SliderDefaults.colors(thumbColor = VermelhoAcrescimo, activeTrackColor = VermelhoAcrescimo)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(desconto.toDouble(), acrescimo.toDouble()) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

