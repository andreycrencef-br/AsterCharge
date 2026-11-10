package com.example.astercharge.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astercharge.data.Usuario

private val VerdeAster = Color(0xFF5FA97A)

// Cores reativas ao tema: usar propriedades com getter @Composable em vez de
// valores fixos faz o modo escuro funcionar de verdade nesta tela.
private val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val BrancoCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

private val CinzaTexto: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val TextoPrincipal: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

/** Texto de placeholder (lorem ipsum) só para preencher a política de privacidade na demo. */
private const val TEXTO_POLITICA_PRIVACIDADE = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.

Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

O AsterCharge coleta apenas os dados necessários para o funcionamento do aplicativo (e-mail de cadastro, histórico de sessões de recarga e método de pagamento selecionado). Nenhum dado de cartão completo é armazenado no dispositivo ou em nossos servidores.

Este texto é um placeholder (lorem ipsum) incluído apenas para fins de demonstração acadêmica. Antes de uma publicação real, ele deve ser substituído pela política de privacidade oficial do AsterCharge."""

@Composable
fun TelaConfiguracoes(
    usuario: Usuario?,
    modoEscuro: Boolean,
    alertaSessao: Boolean,
    noticiasOfertas: Boolean,
    onAlternarModoEscuro: (Boolean) -> Unit,
    onAlternarAlertaSessao: (Boolean) -> Unit,
    onAlternarNoticiasOfertas: (Boolean) -> Unit,
    onSalvarPerfil: (String) -> Unit,
    onSair: () -> Unit
) {
    var mostrarEditarPerfil by remember { mutableStateOf(false) }
    var mostrarPolitica by remember { mutableStateOf(false) }

    if (mostrarEditarPerfil) {
        DialogoEditarPerfil(
            nomeAtual = usuario?.nome ?: "",
            emailAtual = usuario?.email ?: "",
            onSalvar = { novoNome ->
                onSalvarPerfil(novoNome)
                mostrarEditarPerfil = false
            },
            onFechar = { mostrarEditarPerfil = false }
        )
    }

    if (mostrarPolitica) {
        AlertDialog(
            onDismissRequest = { mostrarPolitica = false },
            title = { Text("Política de Privacidade", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    TEXTO_POLITICA_PRIVACIDADE,
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarPolitica = false }) { Text("Fechar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoClaro)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Configurações", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        }

        item {
            SecaoCard(titulo = "Perfil") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(VerdeAster, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(usuario?.nome ?: "Visitante", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextoPrincipal)
                        Text(usuario?.email ?: "-", color = CinzaTexto, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                LinhaClicavel(
                    icone = Icons.Filled.Email,
                    texto = "Editar perfil",
                    corTexto = VerdeAster,
                    onClick = { mostrarEditarPerfil = true }
                )
            }
        }

        item {
            SecaoCard(titulo = "Aparência") {
                LinhaComSwitch(
                    icone = Icons.Filled.DarkMode,
                    texto = "Modo escuro",
                    checked = modoEscuro,
                    onCheckedChange = onAlternarModoEscuro
                )
            }
        }

        item {
            SecaoCard(titulo = "Notificações") {
                LinhaComSwitch(
                    icone = Icons.Filled.Notifications,
                    texto = "Alerta de sessão",
                    checked = alertaSessao,
                    onCheckedChange = onAlternarAlertaSessao
                )
                Spacer(Modifier.height(8.dp))
                LinhaComSwitch(
                    icone = Icons.Filled.Email,
                    texto = "Notícias e ofertas",
                    checked = noticiasOfertas,
                    onCheckedChange = onAlternarNoticiasOfertas
                )
            }
        }

        item {
            SecaoCard(titulo = "Sobre") {
                LinhaClicavel(
                    icone = Icons.Filled.Shield,
                    texto = "Política de Privacidade",
                    onClick = { mostrarPolitica = true }
                )
                Spacer(Modifier.height(4.dp))
                Text("Versão do app: 2.0", color = CinzaTexto, fontSize = 12.sp)
            }
        }

        item {
            OutlinedButton(
                onClick = onSair,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDF0000))
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sair da conta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DialogoEditarPerfil(
    nomeAtual: String,
    emailAtual: String,
    onSalvar: (String) -> Unit,
    onFechar: () -> Unit
) {
    var nome by remember { mutableStateOf(nomeAtual) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Editar perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emailAtual,
                    onValueChange = {},
                    label = { Text("E-mail") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Por enquanto só é possível alterar o nome. Trocar o e-mail exige reautenticação por segurança.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nome.isNotBlank()) onSalvar(nome.trim()) }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SecaoCard(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrancoCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextoPrincipal)
            Spacer(Modifier.height(12.dp))
            conteudo()
        }
    }
}

@Composable
private fun LinhaComSwitch(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = CinzaTexto, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, color = TextoPrincipal)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VerdeAster)
        )
    }
}

@Composable
private fun LinhaClicavel(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    corTexto: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val corFinal = if (corTexto != Color.Unspecified) corTexto else TextoPrincipal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = if (corTexto != Color.Unspecified) corTexto else CinzaTexto, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, color = corFinal, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CinzaTexto)
    }
}
