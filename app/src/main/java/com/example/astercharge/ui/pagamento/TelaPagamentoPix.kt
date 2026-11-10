package com.example.astercharge.ui.pagamento

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

private enum class EstadoPix { AGUARDANDO, PROCESSANDO, APROVADO }

/**
 * Simulação de pagamento via PIX: gera um "QR Code" (desenhado localmente,
 * não é um código real escaneável) e um código copia-e-cola falso, e só
 * confirma o pagamento depois que o usuário toca em "Já paguei" — para
 * ficar mais interativo do que aprovar na hora.
 */
@Composable
fun TelaPagamentoPix(
    valor: Double,
    onConfirmado: () -> Unit,
    onFechar: () -> Unit
) {
    var estado by remember { mutableStateOf(EstadoPix.AGUARDANDO) }

    val codigoPix = remember {
        "00020126580014BR.GOV.BCB.PIX0136" +
            UUID.randomUUID().toString().replace("-", "") +
            "5204000053039865802BR5913ASTERCHARGE6009SAO" +
            "PAULO62070503***6304${Random.nextInt(1000, 9999)}"
    }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(estado) {
        if (estado == EstadoPix.PROCESSANDO) {
            delay(1600)
            estado = EstadoPix.APROVADO
            delay(900)
            onConfirmado()
        }
    }

    AlertDialog(
        onDismissRequest = { if (estado != EstadoPix.PROCESSANDO) onFechar() },
        title = { Text("Pagar com PIX", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    String.format(Locale.US, "R$ %.2f", valor),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                when (estado) {
                    EstadoPix.AGUARDANDO -> {
                        QrCodeFalso(seed = codigoPix)
                        Text(
                            "Escaneie com o app do seu banco ou copie o código abaixo.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(codigoPix))
                                Toast.makeText(context, "Código PIX copiado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar código PIX", fontSize = 13.sp)
                        }

                        Text(
                            "Este QR Code é uma simulação para fins acadêmicos — não é uma cobrança real.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    EstadoPix.PROCESSANDO -> {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Confirmando pagamento...", color = Color.Gray)
                        Spacer(Modifier.height(24.dp))
                    }

                    EstadoPix.APROVADO -> {
                        Spacer(Modifier.height(16.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(56.dp)
                        )
                        Text("Pagamento aprovado!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (estado == EstadoPix.AGUARDANDO) {
                TextButton(onClick = { estado = EstadoPix.PROCESSANDO }) {
                    Text("Já paguei")
                }
            }
        },
        dismissButton = {
            if (estado == EstadoPix.AGUARDANDO) {
                TextButton(onClick = onFechar) { Text("Cancelar") }
            }
        }
    )
}

/**
 * Desenha um "QR code" fake (padrão determinístico a partir da seed), só
 * para dar a sensação visual de um QR de verdade. Não é decodificável.
 */
@Composable
private fun QrCodeFalso(seed: String) {
    val tamanhoGrade = 21
    val random = remember(seed) { Random(seed.hashCode()) }
    val celulas = remember(seed) {
        BooleanArray(tamanhoGrade * tamanhoGrade) { random.nextBoolean() }
    }

    Canvas(
        modifier = Modifier
            .size(180.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        val ladoCelula = size.width / tamanhoGrade

        for (linha in 0 until tamanhoGrade) {
            for (coluna in 0 until tamanhoGrade) {
                val ehCantoFinder =
                    (linha < 7 && coluna < 7) ||
                        (linha < 7 && coluna >= tamanhoGrade - 7) ||
                        (linha >= tamanhoGrade - 7 && coluna < 7)

                val preenchido = if (ehCantoFinder) {
                    val borda = linha % 7 == 0 || coluna % 7 == 0 ||
                        (linha % 7 in 2..4 && coluna % 7 in 2..4)
                    borda
                } else {
                    celulas[linha * tamanhoGrade + coluna]
                }

                if (preenchido) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(coluna * ladoCelula, linha * ladoCelula),
                        size = Size(ladoCelula, ladoCelula)
                    )
                }
            }
        }
    }
}
