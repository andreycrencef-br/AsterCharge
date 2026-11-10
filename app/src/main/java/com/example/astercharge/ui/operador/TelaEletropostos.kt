package com.example.astercharge.ui.operador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astercharge.data.CarregadorInfo
import com.example.astercharge.data.StatusSessao
import com.example.astercharge.util.MapaIntents
import com.example.astercharge.util.rememberLocalizacaoUsuario
import java.util.Locale
import java.util.UUID

private val VerdeLivre = Color(0xFF5FA97A)
private val VermelhoOcupado = Color(0xFFE07A7A)
private val CinzaManutencao = Color(0xFF9E9E9E)

private val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val BrancoCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

private val CinzaTexto: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val TextoPrincipal: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable
fun TelaEletropostos(
    lista: List<CarregadorInfo>,
    idsEmManutencao: List<String>,
    onAlternarManutencao: (String) -> Unit,
    onAdicionarCarregador: (CarregadorInfo) -> Unit
) {
    var carregadorSelecionado by remember { mutableStateOf<CarregadorInfo?>(null) }
    var mostrarNovoEletroposto by remember { mutableStateOf(false) }

    carregadorSelecionado?.let { carregador ->
        DialogoDetalhesCarregador(
            carregador = carregador,
            emManutencao = idsEmManutencao.contains(carregador.id),
            onAlternarManutencao = {
                onAlternarManutencao(carregador.id)
                carregadorSelecionado = null
            },
            onFechar = { carregadorSelecionado = null }
        )
    }

    if (mostrarNovoEletroposto) {
        DialogoNovoEletroposto(
            onSalvar = { novo ->
                onAdicionarCarregador(novo)
                mostrarNovoEletroposto = false
            },
            onFechar = { mostrarNovoEletroposto = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(FundoClaro)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Eletropostos",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                modifier = Modifier.padding(20.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(lista, key = { it.id }) { carregador ->
                    val emManutencao = idsEmManutencao.contains(carregador.id)
                    val ocupado = carregador.statusSessao == StatusSessao.ATIVA ||
                        carregador.statusSessao == StatusSessao.THROTTLE

                    LinhaCarregador(
                        carregador = carregador,
                        emManutencao = emManutencao,
                        ocupado = ocupado,
                        onClick = { carregadorSelecionado = carregador }
                    )
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { mostrarNovoEletroposto = true },
            containerColor = VerdeLivre,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Adicionar eletroposto", tint = Color.White)
        }
    }
}

@Composable
private fun LinhaCarregador(
    carregador: CarregadorInfo,
    emManutencao: Boolean,
    ocupado: Boolean,
    onClick: () -> Unit
) {
    val (corIcone, icone, rotulo) = when {
        emManutencao -> Triple(CinzaManutencao, Icons.Filled.Build, "Manutenção")
        ocupado -> Triple(VermelhoOcupado, Icons.Filled.Bolt, "Ocupado")
        else -> Triple(VerdeLivre, Icons.Filled.Bolt, "Livre")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BrancoCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(corIcone, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(carregador.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextoPrincipal)
                Text(rotulo, color = corIcone, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DialogoDetalhesCarregador(
    carregador: CarregadorInfo,
    emManutencao: Boolean,
    onAlternarManutencao: () -> Unit,
    onFechar: () -> Unit
) {
    val context = LocalContext.current

    val abrirNoMapa = {
        MapaIntents.abrirSeletorDeNavegacao(
            context,
            carregador.latitude,
            carregador.longitude,
            carregador.nome
        )
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(carregador.nome, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                LinhaInfo("Tipo", carregador.tipo)
                LinhaInfo("Potência nominal", "${carregador.potenciaNominalKw} kW")
                LinhaInfo("Preço base", "R$ ${"%.2f".format(carregador.precoBaseKwh)}/kWh")
                LinhaInfo("Vagas", "${carregador.vagasDisponiveis}/${carregador.vagasTotais}")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = abrirNoMapa)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Coordenadas", color = CinzaTexto, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = VerdeLivre,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            String.format(Locale.US, "%.5f, %.5f", carregador.latitude, carregador.longitude),
                            color = VerdeLivre,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = abrirNoMapa,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir no mapa")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAlternarManutencao) {
                Text(if (emManutencao) "Reativar eletroposto" else "Colocar em manutenção")
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Fechar") }
        }
    )
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(rotulo, color = CinzaTexto, fontSize = 13.sp)
        Text(valor, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun DialogoNovoEletroposto(
    onSalvar: (CarregadorInfo) -> Unit,
    onFechar: () -> Unit
) {
    val tipos = listOf("DC Ultra rápido", "DC Rápido", "AC Semi rápido", "AC Lento")

    var nome by remember { mutableStateOf("") }
    var tipoIndice by remember { mutableStateOf(1) }
    var potencia by remember { mutableStateOf("60") }
    var preco by remember { mutableStateOf("4.50") }
    var vagas by remember { mutableStateOf("2") }
    var latitude by remember { mutableStateOf("-23.5733") }
    var longitude by remember { mutableStateOf("-46.6235") }
    var tentouSalvar by remember { mutableStateOf(false) }

    val (localizacaoUsuario, buscarLocalizacao) = rememberLocalizacaoUsuario()

    LaunchedEffect(localizacaoUsuario) {
        localizacaoUsuario?.let {
            latitude = it.latitude.toString()
            longitude = it.longitude.toString()
        }
    }

    val nomeValido = nome.trim().length >= 3
    val potenciaValida = potencia.toDoubleOrNull() != null && (potencia.toDoubleOrNull() ?: 0.0) > 0
    val precoValido = preco.toDoubleOrNull() != null && (preco.toDoubleOrNull() ?: 0.0) > 0
    val vagasValidas = vagas.toIntOrNull() != null && (vagas.toIntOrNull() ?: 0) > 0
    val coordenadasValidas = latitude.toDoubleOrNull() != null && longitude.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Novo eletroposto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = tentouSalvar && !nomeValido,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tipos.forEachIndexed { indice, tipo ->
                        FilterChip(
                            selected = tipoIndice == indice,
                            onClick = { tipoIndice = indice },
                            label = { Text(tipo, fontSize = 10.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = potencia,
                        onValueChange = { potencia = it },
                        label = { Text("Potência (kW)") },
                        singleLine = true,
                        isError = tentouSalvar && !potenciaValida,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = preco,
                        onValueChange = { preco = it },
                        label = { Text("Preço R$/kWh") },
                        singleLine = true,
                        isError = tentouSalvar && !precoValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = vagas,
                    onValueChange = { vagas = it },
                    label = { Text("Número de vagas") },
                    singleLine = true,
                    isError = tentouSalvar && !vagasValidas,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        isError = tentouSalvar && !coordenadasValidas,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        isError = tentouSalvar && !coordenadasValidas,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = { buscarLocalizacao() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Usar minha localização atual", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tentouSalvar = true
                if (nomeValido && potenciaValida && precoValido && vagasValidas && coordenadasValidas) {
                    val vagasInt = vagas.toInt()
                    onSalvar(
                        CarregadorInfo(
                            id = "OP-${UUID.randomUUID().toString().take(8)}",
                            nome = nome.trim(),
                            tipo = tipos[tipoIndice],
                            potenciaNominalKw = potencia.toDouble(),
                            precoBaseKwh = preco.toDouble(),
                            potenciaAtualKw = potencia.toDouble(),
                            latitude = latitude.toDouble(),
                            longitude = longitude.toDouble(),
                            vagasTotais = vagasInt,
                            vagasDisponiveis = vagasInt,
                            statusSessao = StatusSessao.LIVRE
                        )
                    )
                }
            }) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

