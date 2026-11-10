package com.example.astercharge

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.astercharge.auth.AuthViewModel
import com.example.astercharge.auth.TelaLogin
import com.example.astercharge.data.CarregadorInfo
import com.example.astercharge.data.HistoricoSessao
import com.example.astercharge.data.ItemFila
import com.example.astercharge.data.MetodoPagamento
import com.example.astercharge.data.Pagamento
import com.example.astercharge.data.CartaoInfo
import com.example.astercharge.data.PerfilAcesso
import com.example.astercharge.data.StatusSessao
import com.example.astercharge.data.SessaoRecarga
import com.example.astercharge.data.Usuario
import com.example.astercharge.ui.config.SettingsViewModel
import com.example.astercharge.ui.config.TelaConfiguracoes
import com.example.astercharge.ui.operador.GestaoViewModel
import com.example.astercharge.ui.operador.TelaDashboard
import com.example.astercharge.ui.operador.TelaEletropostos
import com.example.astercharge.ui.operador.TelaGestao
import com.example.astercharge.ui.pagamento.CartaoVisual
import com.example.astercharge.ui.pagamento.TelaAdicionarCartao
import com.example.astercharge.ui.pagamento.TelaPagamentoPix
import com.example.astercharge.ui.theme.AsterChargeTheme
import com.example.astercharge.util.MapaIntents
import com.example.astercharge.util.LocalizacaoAtual
import com.example.astercharge.util.ServicoRotas
import com.example.astercharge.util.SolicitarPermissaoLocalizacaoAoAbrir
import com.example.astercharge.util.rememberLocalizacaoUsuario
import com.example.astercharge.util.CondicaoSolar
import com.example.astercharge.util.PrecificacaoSolar
import com.example.astercharge.util.ServicoSolar
import com.example.astercharge.viewmodel.AsterChargeViewModel
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import com.example.astercharge.ai.GeminiService
import com.example.astercharge.ai.ContextoAssistente
import com.example.astercharge.ai.PlanejadorViagem
import com.google.firebase.Firebase
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding

val VermelhoGoodWe = Color(0xFFDF0000)
val VermelhoParar = Color(0xFFDF0000)
val LaranjaThrottle = Color(0xFFFFA726)
val VerdeLivre = Color(0xFF2E7D32)
val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

val BrancoCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val CinzaTexto: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextoPrincipal: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val VermelhoClaroBg: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF4A2020)
    } else {
        Color(0xFFFFECEC)
    }

const val CAPACIDADE_MAXIMA_REDE_KW = 200.0
const val CAPACIDADE_BATERIA_KWH = 60.0
const val BATERIA_INICIAL = 10
const val LIMITE_THROTTLE_1 = 80.0
const val LIMITE_THROTTLE_2 = 95.0
const val MULTA_OCIOSIDADE_POR_MINUTO = 5.00
const val TAXA_CONEXAO_FIXA = 2.50
const val LIMITE_FILA = 20

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        setContent {
            AppRaiz()
        }
    }
}
@Composable
fun AppRaiz() {
    val authViewModel: AuthViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val usuarioLogado by authViewModel.usuario.collectAsState()
    val modoEscuro by settingsViewModel.modoEscuro.collectAsState()

    AsterChargeTheme(darkTheme = modoEscuro, dynamicColor = false) {
        val usuario = usuarioLogado
        if (usuario == null) {
            TelaLogin(authViewModel)
        } else {
            AppPrincipalContainer(
                usuarioLogado = usuario,
                authViewModel = authViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/**
 * Gera carregadores extras com tipo, potência, vagas e status sorteados,
 * espalhados a uma distância pequena e aleatória do ponto base — só para
 * o mapa da demonstração parecer mais vivo/realista, com vários pontos
 * livres e ocupados ao mesmo tempo.
 */
private fun gerarCarregadoresDinamicos(
    quantidade: Int,
    latBase: Double,
    lonBase: Double
): List<CarregadorInfo> {
    val tipos = listOf(
        Triple("DC Ultra rápido", 120.0, 5.60),
        Triple("DC Rápido", 60.0, 4.90),
        Triple("AC Semi rápido", 22.0, 4.10),
        Triple("AC Lento", 7.4, 3.40)
    )

    return (1..quantidade).map { indice ->
        val (tipo, potencia, precoBase) = tipos.random()
        val vagas = Random.nextInt(1, 4)
        val statusSorteado = when (Random.nextInt(0, 3)) {
            0 -> StatusSessao.ATIVA
            1 -> StatusSessao.THROTTLE
            else -> StatusSessao.LIVRE
        }
        val vagasDisponiveis = if (statusSorteado == StatusSessao.LIVRE) {
            vagas
        } else {
            Random.nextInt(0, vagas)
        }

        val deltaLat = (Random.nextDouble(-1.0, 1.0)) * 0.013
        val deltaLon = (Random.nextDouble(-1.0, 1.0)) * 0.013

        CarregadorInfo(
            id = "G-D${indice}",
            nome = "AsterPoint Extra $indice",
            tipo = tipo,
            potenciaNominalKw = potencia,
            precoBaseKwh = precoBase,
            potenciaAtualKw = if (statusSorteado == StatusSessao.LIVRE) potencia else potencia * 0.7,
            latitude = latBase + deltaLat,
            longitude = lonBase + deltaLon,
            vagasTotais = vagas,
            vagasDisponiveis = vagasDisponiveis,
            statusSessao = statusSorteado
        )
    }
}

@Composable
fun AppPrincipalContainer(
    usuarioLogado: Usuario,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel
) {

    val viewModel: AsterChargeViewModel = viewModel()
    val sessaoAtual by viewModel.sessao.collectAsState()
    val historicoChat by viewModel.historicoChat.collectAsState()
    val sessao by viewModel.sessao.collectAsState()
    val listaHistorico by viewModel.historico.collectAsState()
    val pagamento by viewModel.pagamento.collectAsState()
    val cartaoSalvo by viewModel.cartao.collectAsState()

    val modoEscuro by settingsViewModel.modoEscuro.collectAsState()
    val alertaSessao by settingsViewModel.alertaSessao.collectAsState()
    val noticiasOfertas by settingsViewModel.noticiasOfertas.collectAsState()

    val isOperador = usuarioLogado.perfilAcesso == PerfilAcesso.OPERADOR

    var telaAtual by remember {
        mutableStateOf(if (isOperador) "dashboard" else "mapa")
    }

    val idsEmManutencao = remember { mutableStateListOf<String>() }

    val gestaoViewModel: GestaoViewModel = viewModel()
    var mostrarDialogoCartao by remember { mutableStateOf(false) }
    var mostrarDialogoPix by remember { mutableStateOf(false) }

    val listaCarregadores = remember {
        val base = mutableStateListOf(
            CarregadorInfo(
                id = "G-01",
                nome = "GoodWe Vaga Ultra",
                tipo = "DC Rápido",
                potenciaNominalKw = 150.0,
                precoBaseKwh = 5.20,
                potenciaAtualKw = 150.0,
                latitude = -23.5733,
                longitude = -46.6235,
                vagasTotais = 2,
                vagasDisponiveis = 0,

                statusSessao = StatusSessao.ATIVA
            ),
            CarregadorInfo(
                id = "G-02",
                nome = "GoodWe Smart",
                tipo = "AC Semi rápido",
                potenciaNominalKw = 22.0,
                precoBaseKwh = 4.20,
                potenciaAtualKw = 22.0,
                latitude = -23.5745,
                longitude = -46.6250,
                vagasTotais = 2,
                vagasDisponiveis = 2,
                statusSessao = StatusSessao.LIVRE
            ),
            CarregadorInfo(
                id = "G-03",
                nome = "GoodWe Eco",
                tipo = "AC Lento",
                potenciaNominalKw = 7.4,
                precoBaseKwh = 3.50,
                potenciaAtualKw = 7.4,
                latitude = -23.5720,
                longitude = -46.6220,
                vagasTotais = 1,
                vagasDisponiveis = 1,
                statusSessao = StatusSessao.LIVRE
            )
        )
        base.addAll(gerarCarregadoresDinamicos(quantidade = 5, latBase = -23.5733, lonBase = -46.6235))
        base
    }

    var carregadorSelecionado by remember { mutableStateOf(listaCarregadores[1]) }

    val filaEspera = remember { mutableStateListOf<ItemFila>() }
    var proximaOrdemFila by remember { mutableStateOf(0L) }

    var horaDoDia by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    LaunchedEffect(Unit) {
        while (true) {
            horaDoDia = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(60_000)
        }
    }

    var condicaoSolar by remember { mutableStateOf<CondicaoSolar?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            condicaoSolar = ServicoSolar.buscarCondicaoAtual()
            delay(5 * 60_000)
        }
    }

    val descontoSolar = remember(horaDoDia, condicaoSolar) {
        PrecificacaoSolar.descontoPercentual(horaDoDia, condicaoSolar)
    }

    val descricaoSolar = remember(horaDoDia, condicaoSolar) {
        PrecificacaoSolar.descricaoCondicao(horaDoDia, condicaoSolar)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            val candidatos = listaCarregadores.filter {
                !(sessao.ativa && sessao.idCarregador == it.id) && !idsEmManutencao.contains(it.id)
            }
            val alvo = candidatos.randomOrNull() ?: continue
            val indice = listaCarregadores.indexOfFirst { it.id == alvo.id }
            if (indice == -1) continue

            val novoStatus = if (alvo.statusSessao == StatusSessao.LIVRE) {
                StatusSessao.ATIVA
            } else {
                StatusSessao.LIVRE
            }

            val novasVagas = if (novoStatus == StatusSessao.LIVRE) {
                alvo.vagasTotais
            } else {
                (alvo.vagasDisponiveis - 1).coerceAtLeast(0)
            }

            listaCarregadores[indice] = alvo.copy(
                statusSessao = novoStatus,
                vagasDisponiveis = novasVagas
            )
        }
    }

    fun statusDoCarregador(id: String): CarregadorInfo {
        return listaCarregadores.firstOrNull { it.id == id } ?: carregadorSelecionado
    }

    fun carregadorEstaOcupado(carregador: CarregadorInfo): Boolean {
        val atual = statusDoCarregador(carregador.id)
        return atual.statusSessao == StatusSessao.ATIVA ||
            atual.statusSessao == StatusSessao.THROTTLE ||
            idsEmManutencao.contains(carregador.id)
    }

    fun ehMinhaSessaoAtiva(carregador: CarregadorInfo): Boolean {
        return sessao.ativa && sessao.idCarregador == carregador.id
    }

    fun atualizarCarregadorAtual(novoCarregador: CarregadorInfo) {
        val indice = listaCarregadores.indexOfFirst { it.id == novoCarregador.id }
        if (indice != -1) {
            listaCarregadores[indice] = novoCarregador
        }
        if (carregadorSelecionado.id == novoCarregador.id) {
            carregadorSelecionado = novoCarregador
        }
    }

    /** Tarifa sem o desconto solar — usada só para mostrar o "de/por" na UI. */
    fun calcularTarifaBase(carregador: CarregadorInfo): Double {
        var tarifa = carregador.precoBaseKwh

        if (horaDoDia in 17..22) {
            tarifa *= 1.20
        }

        if (horaDoDia in 0..6) {
            tarifa *= 0.85
        }

        if (sessao.ativa && sessao.percentualUtilizacaoRede > 80.0) {
            tarifa *= 1.10
        }

        return tarifa
    }

    fun calcularTarifa(carregador: CarregadorInfo): Double {
        return calcularTarifaBase(carregador) * (1 - descontoSolar)
    }

    LaunchedEffect(sessao.ativa) {
        while (sessao.ativa) {
            delay(1000)
            viewModel.simularUmMinuto()
        }
    }

    val demandaTotalKw by remember {
        derivedStateOf {
            100.0 + sessao.potenciaAtualKw
        }
    }

    val percentualUtilizacaoRede by remember {
        derivedStateOf {
            (demandaTotalKw / CAPACIDADE_MAXIMA_REDE_KW) * 100.0
        }
    }

    if (mostrarDialogoCartao) {
        TelaAdicionarCartao(
            onSalvar = { cartao ->
                viewModel.salvarCartao(cartao)
                mostrarDialogoCartao = false
            },
            onFechar = { mostrarDialogoCartao = false }
        )
    }

    if (mostrarDialogoPix) {
        TelaPagamentoPix(
            valor = sessao.custoTotal,
            onConfirmado = {
                viewModel.criarPagamento(MetodoPagamento.PIX)
                mostrarDialogoPix = false
            },
            onFechar = { mostrarDialogoPix = false }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BrancoCard,
                tonalElevation = 8.dp
            ) {
                if (isOperador) {
                    NavigationBarItem(
                        selected = telaAtual == "dashboard",
                        onClick = { telaAtual = "dashboard" },
                        label = { RotuloAba("Dashboard") },
                        icon = {
                            Icon(
                                Icons.Filled.Dashboard,
                                contentDescription = "Dashboard",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )

                    NavigationBarItem(
                        selected = telaAtual == "eletropostos",
                        onClick = { telaAtual = "eletropostos" },
                        label = { RotuloAba("Postos") },
                        icon = {
                            Icon(
                                Icons.Filled.EvStation,
                                contentDescription = "Eletropostos",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )

                    NavigationBarItem(
                        selected = telaAtual == "gestao",
                        onClick = { telaAtual = "gestao" },
                        label = { RotuloAba("Gestão") },
                        icon = {
                            Icon(
                                Icons.Filled.ManageAccounts,
                                contentDescription = "Gestão",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )
                } else {
                    NavigationBarItem(
                        selected = telaAtual == "mapa",
                        onClick = { telaAtual = "mapa" },
                        label = { RotuloAba("Mapa") },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_mapa),
                                contentDescription = "Mapa",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )

                    NavigationBarItem(
                        selected = telaAtual == "sessao",
                        onClick = { telaAtual = "sessao" },
                        label = { RotuloAba("Sessão") },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_recarga),
                                contentDescription = "Sessão",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )

                    NavigationBarItem(
                        selected = telaAtual == "home",
                        onClick = { telaAtual = "home" },
                        label = { RotuloAba("Pagamento") },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_dashboard),
                                contentDescription = "Pagamentos",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VermelhoGoodWe,
                            selectedTextColor = VermelhoGoodWe
                        )
                    )
                }

                NavigationBarItem(
                    selected = telaAtual == "suporte",
                    onClick = { telaAtual = "suporte" },
                    label = { RotuloAba("Suporte") },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_suporte),
                            contentDescription = "Suporte",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VermelhoGoodWe,
                        selectedTextColor = VermelhoGoodWe
                    )
                )

                NavigationBarItem(
                    selected = telaAtual == "config",
                    onClick = { telaAtual = "config" },
                    label = { RotuloAba("Config.") },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Configurações",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VermelhoGoodWe,
                        selectedTextColor = VermelhoGoodWe
                    )
                )
            }
        }
    ) { paddingValores ->
        Box(Modifier.padding(paddingValores)) {
            when (telaAtual) {
                "dashboard" -> {
                    TelaDashboard(
                        receitaDoDia = listaHistorico.sumOf { it.custoTotal },
                        recargasAtivas = listaCarregadores.count {
                            it.statusSessao == StatusSessao.ATIVA || it.statusSessao == StatusSessao.THROTTLE
                        },
                        energiaSolarDisponivel = descontoSolar > 0.0,
                        totalEletropostos = listaCarregadores.size
                    )
                }

                "eletropostos" -> {
                    TelaEletropostos(
                        lista = listaCarregadores,
                        idsEmManutencao = idsEmManutencao,
                        onAlternarManutencao = { id ->
                            if (idsEmManutencao.contains(id)) {
                                idsEmManutencao.remove(id)
                            } else {
                                idsEmManutencao.add(id)
                            }
                        },
                        onAdicionarCarregador = { novo ->
                            listaCarregadores.add(novo)
                        }
                    )
                }

                "gestao" -> {
                    TelaGestao(
                        gestaoViewModel = gestaoViewModel
                    )
                }

                "home" -> {
                    TelaHome(
                        historico = listaHistorico,
                        carregandoAgora = sessao.ativa,
                        kwhAtual = sessao.kwhEntregue,
                        bateria = sessao.bateriaAtual.toInt(),
                        demandaRede = demandaTotalKw,
                        usoRede = percentualUtilizacaoRede,
                        pagamento = pagamento,
                        cartao = cartaoSalvo,
                        onAdicionarCartao = { mostrarDialogoCartao = true },
                        onPagarPix = {
                            mostrarDialogoPix = true
                        },
                        onPagarCartao = {
                            val sucesso = viewModel.criarPagamento(MetodoPagamento.CARTAO)
                            if (!sucesso) {
                                mostrarDialogoCartao = true
                            }
                        }
                    )
                }

                "mapa" -> {
                    TelaMapa(
                        carregador = carregadorSelecionado,
                        lista = listaCarregadores,
                        filaTamanho = filaEspera.count {
                            it.idCarregador == carregadorSelecionado.id
                        },
                        tarifaCalculada = calcularTarifa(carregadorSelecionado),
                        tarifaSemDesconto = calcularTarifaBase(carregadorSelecionado),
                        descontoSolar = descontoSolar,
                        descricaoSolar = descricaoSolar,
                        carregadorOcupado = carregadorEstaOcupado(carregadorSelecionado),
                        minhaSessaoAtiva = ehMinhaSessaoAtiva(carregadorSelecionado),
                        onSelecionarCarregador = {
                            carregadorSelecionado = it
                        },
                        onEntrarNaFila = {
                            val filaDesteCarregador = filaEspera.count {
                                it.idCarregador == carregadorSelecionado.id
                            }

                            if (filaDesteCarregador < LIMITE_FILA) {
                                proximaOrdemFila++
                                filaEspera.add(
                                    ItemFila(
                                        id = "FILA-$proximaOrdemFila",
                                        nomeUsuario = usuarioLogado.nome,
                                        idCarregador = carregadorSelecionado.id,
                                        ordemEntrada = proximaOrdemFila
                                    )
                                )
                            }
                        },
                        onIniciarRecarga = {
                            if (!carregadorEstaOcupado(carregadorSelecionado)) {
                                val indiceFila = filaEspera
                                    .withIndex()
                                    .filter {
                                        it.value.idCarregador == carregadorSelecionado.id
                                    }
                                    .minByOrNull {
                                        it.value.ordemEntrada
                                    }
                                    ?.index ?: -1

                                if (indiceFila != -1) {
                                    filaEspera.removeAt(indiceFila)
                                }

                                val tarifa = calcularTarifa(carregadorSelecionado)

                                viewModel.iniciarSessao(
                                    idCarregador = carregadorSelecionado.id,
                                    potenciaNominalKw = carregadorSelecionado.potenciaNominalKw,
                                    bateriaInicial = BATERIA_INICIAL.toDouble(),
                                    capacidadeBateriaKwh = CAPACIDADE_BATERIA_KWH,
                                    tarifa = tarifa
                                )

                                atualizarCarregadorAtual(
                                    carregadorSelecionado.copy(
                                        potenciaAtualKw = carregadorSelecionado.potenciaNominalKw,
                                        statusSessao = StatusSessao.ATIVA,
                                        vagasDisponiveis = (carregadorSelecionado.vagasDisponiveis - 1).coerceAtLeast(0)
                                    )
                                )

                                telaAtual = "sessao"
                            }
                        },
                        onVerMinhaSessao = {
                            telaAtual = "sessao"
                        }
                    )
                }

                "sessao" -> {
                    TelaSessao(
                        recargaAtiva = sessao.ativa,
                        carregador = carregadorSelecionado.copy(
                            potenciaAtualKw = sessao.potenciaAtualKw,
                            statusSessao = if (sessao.throttleAtivo) {
                                StatusSessao.THROTTLE
                            } else if (sessao.ativa) {
                                StatusSessao.ATIVA
                            } else {
                                StatusSessao.ENCERRADA
                            }
                        ),
                        kwh = sessao.kwhEntregue,
                        tempoSimuladoMinutos = sessao.tempoDecorridoMinutos,
                        tempoRestanteEstimado = sessao.tempoRestanteEstimadoMinutos,
                        custo = sessao.custoTotal,
                        bateria = sessao.bateriaAtual.toInt(),
                        multa = sessao.multaOciosidade,
                        onPararRecarga = {
                            viewModel.finalizarSessao()

                            atualizarCarregadorAtual(
                                statusDoCarregador(carregadorSelecionado.id).copy(
                                    potenciaAtualKw = 0.0,
                                    statusSessao = StatusSessao.LIVRE,
                                    vagasDisponiveis = (statusDoCarregador(carregadorSelecionado.id).vagasDisponiveis + 1)
                                        .coerceAtMost(statusDoCarregador(carregadorSelecionado.id).vagasTotais)
                                )
                            )
                            telaAtual = "home"
                        },
                        onNavegarParaMapa = {
                            telaAtual = "mapa"
                        }
                    )
                }

                "suporte" -> {
                    TelaSuporte(
                        sessao = sessaoAtual,
                        carregadorSelecionado = carregadorSelecionado,
                        carregadores = listaCarregadores.toList(),
                        idsEmManutencao = idsEmManutencao.toList(),
                        filaPorCarregador = filaEspera.groupingBy { it.idCarregador }.eachCount(),
                        condicaoSolar = condicaoSolar,
                        nomeUsuario = usuarioLogado.nome,
                        bateriaVeiculo = if (sessaoAtual.ativa || sessaoAtual.finalizada) {
                            sessaoAtual.bateriaAtual
                        } else {
                            BATERIA_INICIAL.toDouble()
                        },
                        historicoChat = historicoChat,
                        onAdicionarMensagem = { mensagem ->
                            viewModel.adicionarMensagemChat(mensagem)
                        }
                    )
                }

                "config" -> {
                    TelaConfiguracoes(
                        usuario = usuarioLogado,
                        modoEscuro = modoEscuro,
                        alertaSessao = alertaSessao,
                        noticiasOfertas = noticiasOfertas,
                        onAlternarModoEscuro = settingsViewModel::alternarModoEscuro,
                        onAlternarAlertaSessao = settingsViewModel::alternarAlertaSessao,
                        onAlternarNoticiasOfertas = settingsViewModel::alternarNoticiasOfertas,
                        onSalvarPerfil = { novoNome ->
                            authViewModel.atualizarNomePerfil(novoNome)
                        },
                        onSair = {
                            authViewModel.sair()
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun RotuloAba(texto: String) {
    Text(
        text = texto,
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun TelaHome(
    historico: List<HistoricoSessao>,
    pagamento: Pagamento?,
    cartao: CartaoInfo?,
    onAdicionarCartao: () -> Unit,
    onPagarPix: () -> Unit,
    onPagarCartao: () -> Unit,
    carregandoAgora: Boolean,
    kwhAtual: Double,
    bateria: Int,
    demandaRede: Double,
    usoRede: Double
) {

    LazyColumn(

        modifier =
            Modifier
                .fillMaxSize()
                .background(FundoClaro)
                .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = "Pagamento",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {

            if (cartao != null) {
                CartaoVisual(
                    numeroMascarado = cartao.numeroMascarado,
                    nomeTitular = cartao.nomeTitular,
                    validade = cartao.validadeMesAno,
                    bandeira = cartao.bandeira
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAdicionarCartao) {
                    Text("Trocar cartão", color = VermelhoGoodWe)
                }
            } else {
                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                BrancoCard
                        ),

                    shape =
                        RoundedCornerShape(20.dp),

                    elevation =
                        CardDefaults.cardElevation(
                            2.dp
                        )
                ) {

                    Column(

                        modifier =
                            Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(

                            imageVector =
                                Icons.Filled.CreditCard,

                            contentDescription = null,

                            modifier =
                                Modifier.size(56.dp),

                            tint =
                                CinzaTexto.copy(
                                    alpha = 0.6f
                                )
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "Nenhum cartão cadastrado.",

                            color =
                                CinzaTexto,

                            fontSize =
                                15.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onAdicionarCartao,
                            colors = ButtonDefaults.buttonColors(containerColor = VermelhoGoodWe),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Adicionar cartão", color = Color.White)
                        }
                    }
                }
            }
        }

        item {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(

                    onClick = onPagarCartao,

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                BrancoCard
                        ),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Text(
                        text =
                            "Pagar Cartão",

                        color =
                            CinzaTexto,

                        fontSize =
                            14.sp
                    )
                }

                Button(

                    onClick = onPagarPix,

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                BrancoCard
                        ),

                    shape =
                        RoundedCornerShape(16.dp)
                ) {

                    Text(
                        text =
                            "Pagar com PIX",

                        color =
                            CinzaTexto,

                        fontSize =
                            14.sp
                    )
                }
            }
        }

        item {

            pagamento?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = VermelhoClaroBg
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Pagamento ${it.status}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            String.format(Locale.US, "R$ %.2f • %s", it.valor, it.identificador),
                            color = CinzaTexto
                        )
                    }
                }
            }
        }

        item {

            Text(
                text =
                    "Histórico de sessões",

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier.padding(
                        top = 8.dp
                    )
            )
        }

        items(historico.size) { index ->

            val sessao =
                historico[index]

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            BrancoCard
                    ),

                shape =
                    RoundedCornerShape(16.dp),

                elevation =
                    CardDefaults.cardElevation(
                        1.dp
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Card(

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        VermelhoClaroBg
                                ),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),

                            modifier =
                                Modifier.size(48.dp)
                        ) {

                            Box(

                                modifier =
                                    Modifier.fillMaxSize(),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(

                                    painter =
                                        painterResource(
                                            R.drawable.ic_recarga
                                        ),

                                    contentDescription =
                                        null,

                                    modifier =
                                        Modifier.size(24.dp),

                                    tint =
                                        VermelhoGoodWe
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(16.dp)
                        )

                        Column {

                            Text(
                                text =
                                    sessao.local,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    15.sp
                            )

                            Text(
                                text =
                                    sessao.data,

                                fontSize =
                                    13.sp,

                                color =
                                    CinzaTexto
                            )
                        }
                    }

                    Text(
                        text =
                            String.format(Locale.US, "R$ %.2f", sessao.custoTotal),

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}
private enum class FiltroMapa { TODOS, LIVRES, OCUPADOS }

@Composable
fun TelaMapa(
    carregador: CarregadorInfo,
    lista: List<CarregadorInfo>,
    filaTamanho: Int,
    tarifaCalculada: Double,
    tarifaSemDesconto: Double,
    descontoSolar: Double,
    descricaoSolar: String,
    carregadorOcupado: Boolean,
    minhaSessaoAtiva: Boolean,
    onSelecionarCarregador:
        (CarregadorInfo) -> Unit,
    onEntrarNaFila: () -> Unit,
    onIniciarRecarga: () -> Unit,
    onVerMinhaSessao: () -> Unit
) {

    val context = LocalContext.current
    var filtro by remember { mutableStateOf(FiltroMapa.TODOS) }
    var mostrarAvisoSolar by remember { mutableStateOf(false) }
    val (localizacaoUsuario, buscarLocalizacao) = rememberLocalizacaoUsuario()

    val solarAtivo = descontoSolar > 0.0

    if (mostrarAvisoSolar) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoSolar = false },
            icon = { Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFF9A825)) },
            title = { Text("Horário de energia solar!", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Você está carregando em um horário de boa geração solar. " +
                            "Sua tarifa tem ${(descontoSolar * 100).toInt()}% de desconto nesta sessão."
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        String.format(
                            Locale.US,
                            "De R$ %.2f/kWh por R$ %.2f/kWh",
                            tarifaSemDesconto,
                            tarifaCalculada
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(descricaoSolar, fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarAvisoSolar = false
                    onIniciarRecarga()
                }) { Text("Iniciar recarga") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAvisoSolar = false }) { Text("Cancelar") }
            }
        )
    }

    val listaFiltrada = remember(lista, filtro) {
        when (filtro) {
            FiltroMapa.TODOS -> lista
            FiltroMapa.LIVRES -> lista.filter { it.statusSessao == StatusSessao.LIVRE }
            FiltroMapa.OCUPADOS -> lista.filter {
                it.statusSessao == StatusSessao.ATIVA || it.statusSessao == StatusSessao.THROTTLE
            }
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        AndroidMapView(

            modifier =
                Modifier.fillMaxSize(),

            latitude =
                -23.5733,

            longitude =
                -46.6235,

            listaCarregadores =
                listaFiltrada,

            localizacaoUsuario =
                localizacaoUsuario,

            onSelecionarCarregador =
                onSelecionarCarregador
        )

        if (solarAtivo) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFF9A825))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Energia solar ativa: -${(descontoSolar * 100).toInt()}% na tarifa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF6B4E00)
                        )
                        Text(descricaoSolar, fontSize = 10.sp, color = Color(0xFF6B4E00))
                    }
                }
            }
        }

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            FiltroChip(
                texto = "Todos",
                selecionado = filtro == FiltroMapa.TODOS
            ) { filtro = FiltroMapa.TODOS }

            FiltroChip(
                texto = "Livres",
                selecionado = filtro == FiltroMapa.LIVRES
            ) { filtro = FiltroMapa.LIVRES }

            FiltroChip(
                texto = "Ocupados",
                selecionado = filtro == FiltroMapa.OCUPADOS
            ) { filtro = FiltroMapa.OCUPADOS }

            Spacer(Modifier.weight(1f))

            FloatingActionButton(
                onClick = { buscarLocalizacao() },
                containerColor = BrancoCard,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Minha localização", tint = VermelhoGoodWe)
            }
        }

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(16.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        BrancoCard
                ),

            shape =
                RoundedCornerShape(24.dp),

            elevation =
                CardDefaults.cardElevation(
                    6.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(24.dp)
            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text =
                                carregador.nome,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                22.sp
                        )

                        Spacer(
                            Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "${carregador.tipo} • " +
                                        "${carregador.potenciaNominalKw} kW",

                            color =
                                CinzaTexto,

                            fontSize =
                                14.sp
                        )

                        if (solarAtivo) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "De R$ %.2f por R$ %.2f/kWh ☀️",
                                    tarifaSemDesconto,
                                    tarifaCalculada
                                ),
                                color = Color(0xFFB8860B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text =
                                    String.format(
                                        Locale.US,
                                        "R$ %.2f/kWh",
                                        tarifaCalculada
                                    ),

                                color =
                                    CinzaTexto,

                                fontSize =
                                    14.sp
                            )
                        }

                        Text(
                            text =
                                "${carregador.vagasDisponiveis}/${carregador.vagasTotais} vagas disponíveis",

                            color =
                                CinzaTexto,

                            fontSize =
                                13.sp
                        )

                        if (filaTamanho > 0) {

                            Text(
                                text =
                                    "Fila: $filaTamanho veículo(s)",

                                color =
                                    VermelhoGoodWe,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    13.sp,

                                modifier =
                                    Modifier.padding(
                                        top = 4.dp
                                    )
                            )
                        }
                    }

                    Card(

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (carregadorOcupado) {
                                        Color(0xFFFFE0E0)
                                    } else {
                                        Color(0xFFE3F5E8)
                                    }
                            ),

                        shape =
                            RoundedCornerShape(50)
                    ) {

                        Text(

                            text =
                                if (minhaSessaoAtiva) {
                                    "SUA SESSÃO"
                                } else if (carregadorOcupado) {
                                    "OCUPADO"
                                } else {
                                    "LIVRE"
                                },

                            color =
                                if (carregadorOcupado) VermelhoGoodWe else VerdeLivre,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                12.sp,

                            modifier =
                                Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                        )
                    }
                }

                Spacer(
                    Modifier.height(16.dp)
                )

                OutlinedButton(
                    onClick = {
                        MapaIntents.abrirSeletorDeNavegacao(
                            context,
                            carregador.latitude,
                            carregador.longitude,
                            carregador.nome
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Como chegar")
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                when {
                    minhaSessaoAtiva -> {
                        Button(
                            onClick = onVerMinhaSessao,
                            colors = ButtonDefaults.buttonColors(containerColor = VermelhoGoodWe),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Text(
                                "Ver sessão em andamento",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    carregadorOcupado -> {

                        OutlinedButton(

                            onClick =
                                onEntrarNaFila,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),

                            shape =
                                RoundedCornerShape(16.dp)
                        ) {

                            Text(
                                "Entrar na fila",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    else -> {

                        Button(

                            onClick = {
                                if (solarAtivo) {
                                    mostrarAvisoSolar = true
                                } else {
                                    onIniciarRecarga()
                                }
                            },

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        VermelhoGoodWe
                                ),

                            shape =
                                RoundedCornerShape(16.dp),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                        ) {

                            Text(
                                "Iniciar recarga",

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    18.sp,

                                color =
                                    Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroChip(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selecionado) VermelhoGoodWe else BrancoCard
        ),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            texto,
            color = if (selecionado) Color.White else CinzaTexto
        )
    }
}
@Composable
fun TelaSessao(
    recargaAtiva: Boolean,
    carregador: CarregadorInfo,
    kwh: Double,
    tempoSimuladoMinutos: Int,
    tempoRestanteEstimado: Int,
    custo: Double,
    bateria: Int,
    multa: Double,
    onPararRecarga: () -> Unit,
    onNavegarParaMapa: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(FundoClaro)
                .padding(24.dp),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        if (!recargaAtiva) {

            Icon(

                painter =
                    painterResource(
                        R.drawable.ic_recarga
                    ),

                contentDescription =
                    null,

                modifier =
                    Modifier.size(96.dp),

                tint =
                    CinzaTexto.copy(
                        alpha = 0.3f
                    )
            )

            Spacer(
                Modifier.height(24.dp)
            )

            Text(
                text =
                    if (bateria >= 100) {
                        "Recarga concluída!"
                    } else {
                        "Nenhuma sessão ativa"
                    },

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(

                text =
                    if (bateria >= 100) {
                        "A bateria chegou a 100% e o carregador foi desligado."
                    } else {
                        "Vá ao mapa e selecione um eletroposto para iniciar uma recarga."
                    },

                color =
                    CinzaTexto,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.padding(
                        horizontal = 32.dp
                    )
            )

            Spacer(
                Modifier.height(32.dp)
            )

            Button(

                onClick =
                    onNavegarParaMapa,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            VermelhoGoodWe
                    ),

                shape =
                    RoundedCornerShape(24.dp),

                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(54.dp)
            ) {

                Text(
                    "Ir ao mapa",

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        16.sp,

                    color =
                        Color.White
                )
            }

        } else {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            BrancoCard
                    ),

                shape =
                    RoundedCornerShape(20.dp),

                elevation =
                    CardDefaults.cardElevation(
                        4.dp
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    val throttleAtivo =
                        carregador.statusSessao ==
                                StatusSessao.THROTTLE

                    Text(

                        text =
                            if (throttleAtivo) {
                                "GERENCIAMENTO DE CARGA ATIVO"
                            } else {
                                "SESSÃO EM ANDAMENTO"
                            },

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            if (throttleAtivo) {
                                LaranjaThrottle
                            } else {
                                VermelhoGoodWe
                            }
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(

                        text =
                            String.format(
                                Locale.US,
                                "%.2f",
                                kwh
                            ),

                        fontSize =
                            56.sp,

                        fontWeight =
                            FontWeight.Black
                    )

                    Text(
                        text =
                            "Energia Acumulada (kWh)",

                        fontSize =
                            13.sp,

                        color =
                            CinzaTexto
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(

                        progress = {
                            bateria / 100f
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "$bateria%",

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            VermelhoGoodWe
                    )

                    if (multa > 0) {

                        Text(

                            text =
                                String.format(
                                    Locale.US,
                                    "Taxa de Ociosidade: R$ %.2f",
                                    multa
                                ),

                            color =
                                VermelhoParar,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                14.sp,

                            modifier =
                                Modifier.padding(
                                    top = 4.dp
                                )
                        )
                    }

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                vertical = 16.dp
                            )
                    )

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {

                        InformacaoSessao(
                            titulo = "Decorrido",
                            valor =
                                "$tempoSimuladoMinutos min"
                        )

                        InformacaoSessao(
                            titulo = "Bateria",
                            valor =
                                "$bateria%"
                        )

                        InformacaoSessao(
                            titulo = "Restante",
                            valor =
                                if (bateria >= 100) {
                                    "Completa"
                                } else {
                                    "$tempoRestanteEstimado min"
                                }
                        )

                        InformacaoSessao(
                            titulo = "Total",
                            valor =
                                String.format(
                                    Locale.US,
                                    "R$ %.2f",
                                    custo
                                )
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )

            Button(

                onClick =
                    onPararRecarga,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            VermelhoParar
                    ),

                shape =
                    RoundedCornerShape(14.dp),

                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp)
            ) {

                Text(
                    "Interromper & Faturar",

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InformacaoSessao(
    titulo: String,
    valor: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                titulo,

            fontSize =
                11.sp,

            color =
                CinzaTexto
        )

        Text(
            text =
                valor,

            fontWeight =
                FontWeight.Bold,

            fontSize =
                16.sp
        )
    }
}

@Composable
fun TelaSuporte(
    sessao: SessaoRecarga,
    carregadorSelecionado: CarregadorInfo,
    carregadores: List<CarregadorInfo>,
    idsEmManutencao: List<String>,
    filaPorCarregador: Map<String, Int>,
    condicaoSolar: CondicaoSolar?,
    nomeUsuario: String,
    bateriaVeiculo: Double,
    historicoChat: List<String>,
    onAdicionarMensagem: (String) -> Unit
) {

    var textoDigitado by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiService() }
    val estadoLista = rememberLazyListState()

    SolicitarPermissaoLocalizacaoAoAbrir()

    val sugestoes = remember {
        listOf(
            "Vou sair às 18h para o Aeroporto de Congonhas. Que horas devo carregar?",
            "Qual eletroposto livre está mais perto de mim?",
            "Quanto tempo e quanto custa carregar até 80%?",
            "Consigo chegar na Avenida Paulista com a bateria atual?"
        )
    }

    LaunchedEffect(historicoChat.size, carregando) {
        val total = historicoChat.size + if (carregando) 1 else 0
        if (total > 0) {
            estadoLista.animateScrollToItem(total - 1)
        }
    }

    fun enviar(texto: String) {
        val mensagem = texto.trim()

        if (mensagem.isEmpty() || carregando) return

        val historicoAnterior = historicoChat

        onAdicionarMensagem(mensagem)
        textoDigitado = ""
        carregando = true

        coroutineScope.launch {
            var resposta = "Não consegui responder agora. Tente novamente."

            try {
                val agora = System.currentTimeMillis()
                val localizacao = LocalizacaoAtual.obter(context)
                val endereco = localizacao?.let { ServicoRotas.enderecoDe(it) }

                val contexto = ContextoAssistente(
                    agoraMillis = agora,
                    nomeUsuario = nomeUsuario,
                    localizacao = localizacao,
                    enderecoAtual = endereco,
                    bateriaVeiculoPercentual = bateriaVeiculo,
                    capacidadeBateriaKwh = CAPACIDADE_BATERIA_KWH,
                    consumoKmPorKwh = PlanejadorViagem.CONSUMO_PADRAO_KM_POR_KWH,
                    sessao = sessao,
                    carregadorSelecionado = carregadorSelecionado,
                    carregadores = carregadores,
                    idsEmManutencao = idsEmManutencao,
                    filaPorCarregador = filaPorCarregador,
                    condicaoSolar = condicaoSolar
                )

                resposta = geminiService.perguntar(
                    pergunta = mensagem,
                    contexto = contexto,
                    historico = historicoAnterior
                )
            } finally {
                onAdicionarMensagem(resposta)
                carregando = false
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FundoClaro)
                .imePadding()
                .padding(16.dp)
    ) {

        Text(
            text = "Suporte",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Assistente Virtual com localização, rota e bateria",
            fontSize = 13.sp,
            color = CinzaTexto
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            state = estadoLista,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(historicoChat.size) { index ->

                val mensagem = historicoChat[index]
                val ehBot = index % 2 == 0

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment =
                        if (ehBot) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        }
                ) {

                    Text(
                        text = mensagem,
                        modifier =
                            Modifier
                                .background(
                                    color =
                                        if (ehBot) {
                                            BrancoCard
                                        } else {
                                            VermelhoClaroBg
                                        },
                                    shape =
                                        RoundedCornerShape(14.dp).copy(
                                            bottomStart =
                                                if (ehBot) {
                                                    CornerSize(0.dp)
                                                } else {
                                                    CornerSize(14.dp)
                                                }
                                        )
                                )
                                .padding(14.dp),
                        color = TextoPrincipal,
                        fontSize = 14.sp
                    )
                }
            }

            if (carregando) {

                item {

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {

                        Text(
                            text = "Consultando localização, rota e bateria...",
                            modifier =
                                Modifier
                                    .background(
                                        BrancoCard,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(14.dp),
                            color = CinzaTexto,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (historicoChat.size <= 1 && !carregando) {

            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(sugestoes) { sugestao ->
                    SugestaoChip(
                        texto = sugestao,
                        onClick = { textoDigitado = sugestao }
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = textoDigitado,
                onValueChange = { textoDigitado = it },
                placeholder = { Text("Digite sua dúvida...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                enabled = !carregando
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { enviar(textoDigitado) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = VermelhoGoodWe
                    ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = !carregando
            ) {

                Icon(
                    painter = painterResource(R.drawable.ic_suporte),
                    contentDescription = "Enviar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SugestaoChip(
    texto: String,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(50))
                .background(BrancoCard)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            color = TextoPrincipal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AndroidMapView(

    modifier: Modifier,

    latitude: Double,

    longitude: Double,

    listaCarregadores:
    List<CarregadorInfo>,

    localizacaoUsuario: GeoPoint? = null,

    onSelecionarCarregador:
        (CarregadorInfo) -> Unit
) {

    AndroidView(

        modifier =
            modifier,

        factory = { context ->

            Configuration
                .getInstance()
                .userAgentValue =
                "AsterCharge/1.0"

            MapView(context).apply {

                setTileSource(
                    TileSourceFactory.MAPNIK
                )

                setMultiTouchControls(
                    true
                )

                controller.setZoom(
                    15.0
                )

                controller.setCenter(
                    GeoPoint(
                        latitude,
                        longitude
                    )
                )

                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
            }
        },

        update = { mapView ->

            mapView.overlays.clear()

            listaCarregadores.forEach { carregador ->

                val coordenadas =
                    GeoPoint(
                        carregador.latitude,
                        carregador.longitude
                    )

                val corStatus = when (carregador.statusSessao) {
                    StatusSessao.LIVRE -> VerdeLivre
                    StatusSessao.THROTTLE -> LaranjaThrottle
                    else -> VermelhoGoodWe
                }

                val marcador =
                    Marker(mapView)

                marcador.position =
                    coordenadas

                marcador.title =
                    carregador.nome

                marcador.snippet =
                    "${carregador.tipo} • " +
                            "${carregador.potenciaNominalKw} kW • " +
                            if (carregador.statusSessao == StatusSessao.LIVRE) "Livre" else "Ocupado"

                ContextCompat.getDrawable(mapView.context, R.drawable.ic_pin_carregador)?.let { base ->
                    val colorido = DrawableCompat.wrap(base.mutate())
                    DrawableCompat.setTint(colorido, corStatus.toArgb())
                    marcador.icon = colorido
                }

                marcador.setOnMarkerClickListener {

                        marker,
                        _ ->

                    onSelecionarCarregador(
                        carregador
                    )

                    marker.showInfoWindow()

                    true
                }

                mapView.overlays.add(
                    marcador
                )
            }

            localizacaoUsuario?.let { posicaoUsuario ->
                val marcadorUsuario = Marker(mapView)
                marcadorUsuario.position = posicaoUsuario
                marcadorUsuario.title = "Você está aqui"

                ContextCompat.getDrawable(mapView.context, R.drawable.ic_minha_localizacao)?.let {
                    marcadorUsuario.icon = it
                }

                mapView.overlays.add(marcadorUsuario)
                mapView.controller.animateTo(posicaoUsuario)
            }

            mapView.invalidate()
        }
    )
}

