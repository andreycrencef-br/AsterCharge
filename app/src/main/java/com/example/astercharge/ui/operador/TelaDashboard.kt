package com.example.astercharge.ui.operador

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.random.Random

private val VerdeAster = Color(0xFF5FA97A)
private val RosaBarra = Color(0xFFE8A0A0)
private val AzulLinha = Color(0xFF4C7FBF)

private val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val BrancoCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

private val TextoPrincipal: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable
fun TelaDashboard(
    receitaDoDia: Double,
    recargasAtivas: Int,
    energiaSolarDisponivel: Boolean,
    totalEletropostos: Int
) {
    // Série simulada de demanda ao longo do dia — não temos histórico
    // real de séries temporais no protótipo, então geramos valores
    // plausíveis uma vez por sessão de app (não muda a cada recomposição).
    val demandaDoDia = remember {
        List(6) { Random.nextInt(30, 100) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoClaro)
            .padding(20.dp)
    ) {
        Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CartaoKpi(
                titulo = "Receita do dia",
                valor = String.format(Locale.US, "R$ %.2f", receitaDoDia),
                modifier = Modifier.weight(1f)
            )
            CartaoKpi(
                titulo = "Recargas ativas",
                valor = recargasAtivas.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CartaoKpi(
                titulo = "Energia solar",
                valor = if (energiaSolarDisponivel) "Disponível" else "Indisponível",
                modifier = Modifier.weight(1f)
            )
            CartaoKpi(
                titulo = "Eletropostos",
                valor = totalEletropostos.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrancoCard),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Demanda do dia", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextoPrincipal)
                Spacer(Modifier.height(16.dp))
                GraficoDemanda(valores = demandaDoDia)
            }
        }
    }
}

@Composable
private fun CartaoKpi(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BrancoCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, color = VerdeAster, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        }
    }
}

@Composable
private fun GraficoDemanda(valores: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        if (valores.isEmpty()) return@Canvas

        val maximo = (valores.maxOrNull() ?: 1).coerceAtLeast(1)
        val larguraBarra = size.width / (valores.size * 2f)
        val espaco = larguraBarra

        val caminhoLinha = Path()

        valores.forEachIndexed { indice, valor ->
            val alturaBarra = (valor.toFloat() / maximo) * size.height
            val x = espaco / 2 + indice * (larguraBarra + espaco)

            drawRect(
                color = RosaBarra,
                topLeft = Offset(x, size.height - alturaBarra),
                size = Size(larguraBarra, alturaBarra)
            )

            val xCentro = x + larguraBarra / 2
            val yPonto = size.height - alturaBarra

            if (indice == 0) {
                caminhoLinha.moveTo(xCentro, yPonto)
            } else {
                caminhoLinha.lineTo(xCentro, yPonto)
            }
        }

        drawPath(
            path = caminhoLinha,
            color = AzulLinha,
            style = Stroke(width = 4f)
        )
    }
}
