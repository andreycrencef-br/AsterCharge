package com.example.astercharge.ui.pagamento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astercharge.data.CartaoInfo
import java.util.UUID

/**
 * Diálogo para cadastrar um cartão.
 *
 * Faz validação real (Luhn, validade, CVV) no aparelho. O número completo
 * NUNCA é salvo: ao "Salvar", só ficam os 4 últimos dígitos + um token
 * fictício (que, em produção, viria do gateway de pagamento).
 */
@Composable
fun TelaAdicionarCartao(
    onSalvar: (CartaoInfo) -> Unit,
    onFechar: () -> Unit
) {
    var numero by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var nomeTitular by remember { mutableStateOf("") }
    var tentouEnviar by remember { mutableStateOf(false) }

    val numeroOk = ValidadorCartao.numeroValido(numero)
    val validadeOk = ValidadorCartao.validadeValida(validade)
    val cvvOk = ValidadorCartao.cvvValido(cvv)
    val nomeOk = ValidadorCartao.nomeValido(nomeTitular)
    val bandeira = remember(numero) { ValidadorCartao.detectarBandeira(numero) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Adicionar cartão", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CartaoVisual(
                    numeroMascarado = if (numero.isBlank()) "•••• •••• •••• ••••" else numero,
                    nomeTitular = nomeTitular.ifBlank { "NOME DO TITULAR" },
                    validade = validade.ifBlank { "MM/AA" },
                    bandeira = bandeira
                )

                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = ValidadorCartao.formatarNumeroDigitado(it) },
                    label = { Text("Número do cartão") },
                    singleLine = true,
                    isError = tentouEnviar && !numeroOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (tentouEnviar && !numeroOk) {
                    Text("Número de cartão inválido.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = validade,
                        onValueChange = { validade = ValidadorCartao.formatarValidadeDigitada(it) },
                        label = { Text("Validade (MM/AA)") },
                        singleLine = true,
                        isError = tentouEnviar && !validadeOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 4) cvv = it.filter { c -> c.isDigit() } },
                        label = { Text("CVV") },
                        singleLine = true,
                        isError = tentouEnviar && !cvvOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (tentouEnviar && (!validadeOk || !cvvOk)) {
                    Text("Verifique a validade e o CVV.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = nomeTitular,
                    onValueChange = { nomeTitular = it },
                    label = { Text("Nome do titular (como está no cartão)") },
                    singleLine = true,
                    isError = tentouEnviar && !nomeOk,
                    modifier = Modifier.fillMaxWidth()
                )
                if (tentouEnviar && !nomeOk) {
                    Text("Informe nome e sobrenome.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Text(
                    "Por segurança, guardamos apenas os últimos 4 dígitos. O número completo não fica salvo no aparelho.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tentouEnviar = true
                if (numeroOk && validadeOk && cvvOk && nomeOk) {
                    val digitos = numero.filter { it.isDigit() }
                    onSalvar(
                        CartaoInfo(
                            id = UUID.randomUUID().toString(),
                            nomeTitular = nomeTitular.trim(),
                            ultimosDigitos = digitos.takeLast(4),
                            bandeira = bandeira,
                            validadeMesAno = validade,
                            // Em produção: token devolvido pelo SDK do gateway de pagamento.
                            tokenPagamento = "TOK-${UUID.randomUUID()}"
                        )
                    )
                }
            }) {
                Text("Salvar cartão")
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

@Composable
fun CartaoVisual(
    numeroMascarado: String,
    nomeTitular: String,
    validade: String,
    bandeira: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2B2B2B), Color(0xFF4A4A4A))
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(bandeira, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                numeroMascarado,
                color = Color.White,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("TITULAR", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    Text(nomeTitular.uppercase(), color = Color.White, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("VALIDADE", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    Text(validade, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
