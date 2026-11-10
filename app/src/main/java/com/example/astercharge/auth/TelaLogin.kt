package com.example.astercharge.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astercharge.data.PerfilAcesso

private val VerdeAster = Color(0xFF5FA97A)

private val FundoClaro: Color
    @Composable get() = MaterialTheme.colorScheme.background

private val SuperficieCard: Color
    @Composable get() = MaterialTheme.colorScheme.surface

private val TextoSecundario: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun TelaLogin(authViewModel: AuthViewModel) {

    var modoCadastro by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf(PerfilAcesso.USUARIO) }

    val carregando by authViewModel.carregando.collectAsState()
    val erro by authViewModel.erro.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FundoClaro
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(VerdeAster.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = VerdeAster, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("AsterCharge", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SuperficieCard),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Text(
                    if (modoCadastro) "Criar conta" else "Entrar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))

                if (modoCadastro) {
                    Text("Nome", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Text("Email", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))

                Text("Senha", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))

                Text("Perfil de acesso", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeletorPerfil(
                        rotulo = "Usuário",
                        selecionado = perfil == PerfilAcesso.USUARIO,
                        modifier = Modifier.weight(1f)
                    ) { perfil = PerfilAcesso.USUARIO }

                    SeletorPerfil(
                        rotulo = "Operador",
                        selecionado = perfil == PerfilAcesso.OPERADOR,
                        modifier = Modifier.weight(1f)
                    ) { perfil = PerfilAcesso.OPERADOR }
                }

                if (erro != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(erro ?: "", color = Color(0xFFDF0000), fontSize = 13.sp)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        authViewModel.limparErro()
                        if (modoCadastro) {
                            authViewModel.cadastrar(nome, email, senha, perfil)
                        } else {
                            authViewModel.login(email, senha, perfil)
                        }
                    },
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeAster)
                ) {
                    if (carregando) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (modoCadastro) "Cadastrar" else "Entrar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (modoCadastro) "Já tem uma conta? " else "Não tem uma conta? ",
                        color = TextoSecundario,
                        fontSize = 13.sp
                    )
                    Text(
                        if (modoCadastro) "Entrar" else "Cadastrar-se",
                        color = VerdeAster,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            authViewModel.limparErro()
                            modoCadastro = !modoCadastro
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun SeletorPerfil(
    rotulo: String,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selecionado) VerdeAster.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (selecionado) VerdeAster else TextoSecundario
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(rotulo, fontSize = 12.sp)
        }
    }
}

