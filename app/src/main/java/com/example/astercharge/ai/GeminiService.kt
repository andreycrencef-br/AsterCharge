package com.example.astercharge.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class GeminiService {

    companion object {
        private const val LIMITE_MENSAGENS_HISTORICO = 10
        private const val LIMITE_RODADAS_FERRAMENTAS = 4
        private const val TEMPO_MAXIMO_MS = 60_000L

        private val INSTRUCAO_DO_SISTEMA = """
Você é o assistente virtual oficial do aplicativo AsterCharge.

O AsterCharge é um aplicativo de gerenciamento e monitoramento de recarga de veículos elétricos. O sistema possui carregadores AC e DC com diferentes potências e preços.

A recarga utiliza um modelo CC-CV. Durante a fase inicial, o carregador pode trabalhar próximo da sua potência disponível. Após aproximadamente 80% da bateria, a potência é reduzida para proteger a bateria, representando a etapa de tensão constante do processo CC-CV.

O AsterCharge possui gerenciamento de demanda da rede elétrica através de Smart Grid. A capacidade máxima considerada pela simulação da rede é de 200 kW. Quando a utilização da rede fica entre 80% e 95%, o sistema aplica throttle e reduz a potência do carregador para aproximadamente 75%. Quando ultrapassa 95%, reduz para aproximadamente 50%.

A tarifação considera o preço base do carregador, o horário do dia, a utilização da rede e a condição solar. O horário de pico é entre 17h e 22h, com acréscimo de 20% na tarifa. Entre 0h e 6h existe desconto de 15%. Entre 8h e 17h pode haver desconto por geração solar. Existe também uma taxa fixa de conexão de R$ 2,50.

Quando a bateria chega a 100%, a recarga de energia deve ser encerrada. Se o veículo permanecer conectado após o término, pode ocorrer cobrança de ociosidade.

A fila dos carregadores funciona pelo princípio FIFO: o primeiro usuário que entra na fila é o primeiro a ser atendido.

Você recebe, junto de cada pergunta, o contexto atual do usuário: data e hora, localização, bateria do veículo, sessão de recarga e eletropostos próximos. Use esses dados como fonte de verdade.

Você tem ferramentas para consultar rota e trânsito, planejar viagens com recarga, estimar recargas e listar eletropostos próximos. Regras para usar as ferramentas:
- Sempre que a pergunta envolver ir a algum lugar, tempo de viagem, trânsito, horário de saída, ou quando carregar antes de sair, use a ferramenta planejar_viagem ou calcular_rota. Nunca invente tempo de viagem, distância ou horários.
- Para perguntas sobre quanto tempo ou quanto custa carregar até certa porcentagem, use estimar_recarga.
- Para perguntas sobre postos livres ou próximos, use listar_eletropostos_proximos.
- Não faça contas de horário de cabeça quando uma ferramenta puder calcular. Repasse os horários que a ferramenta retornar.
- Se o usuário disser uma hora de saída, envie no formato HH:mm de 24 horas. Se disser algo como daqui a 2 horas, use saida_em_minutos.
- Se a ferramenta retornar erro, explique o problema de forma simples e diga o que o usuário pode fazer, por exemplo permitir a localização ou informar um endereço mais específico.
- Se faltar o destino, pergunte qual é o destino antes de planejar.
- Deixe claro que tempos de trânsito, espera em fila e consumo são estimativas, principalmente quando a fonte for uma estimativa por horário.
- A bateria do veículo neste protótipo é simulada pelo aplicativo; trate o valor informado como a bateria atual do carro.

Regras para responder:
- Responda sempre em português do Brasil.
- Não invente valores que não estejam no contexto ou no retorno das ferramentas. Se uma informação não estiver disponível, diga que ela não está disponível no momento.
- Não use Markdown. Não use #, *, **, _, ` ou outros símbolos de formatação. Não use listas com asteriscos ou hifens no início de linha.
- Use apenas texto simples e parágrafos curtos. Normalmente responda em no máximo 3 parágrafos, e para planejamento de viagem pode usar até 4.
- Se o usuário fizer uma pergunta simples, responda de forma simples.
- Em planos de viagem, diga primeiro a resposta principal, como a que horas sair para o eletroposto e a que horas a recarga começa, e depois os detalhes de bateria, custo e chegada.
        """.trimIndent()
    }

    private val modelo =
        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-3.6-flash",
            tools = listOf(Tool.functionDeclarations(FerramentasAssistente.declaracoes)),
            systemInstruction = content { text(INSTRUCAO_DO_SISTEMA) }
        )

    suspend fun perguntar(
        pergunta: String,
        contexto: ContextoAssistente,
        historico: List<String> = emptyList()
    ): String {
        return try {
            val resposta = withTimeoutOrNull(TEMPO_MAXIMO_MS) {
                consultar(pergunta, contexto, historico) }
            resposta ?: "A consulta demorou mais do que o esperado. Tente novamente em instantes."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Não consegui conectar ao suporte Gemini: ${e.message}"
        }
    }

    private suspend fun consultar(
        pergunta: String,
        contexto: ContextoAssistente,
        historico: List<String>
    ): String {
        val ferramentas = FerramentasAssistente(contexto)
        val chat = modelo.startChat(history = converterHistorico(historico))

        val mensagem = """
CONTEXTO ATUAL DO USUÁRIO
${contexto.paraTexto()}

PERGUNTA DO USUÁRIO
$pergunta
        """.trimIndent()

        var resposta = chat.sendMessage(mensagem)
        var rodadas = 0

        while (resposta.functionCalls.isNotEmpty() && rodadas < LIMITE_RODADAS_FERRAMENTAS) {
            val respostas = resposta.functionCalls.map { chamada ->
                FunctionResponsePart(
                    chamada.name,
                    ferramentas.executar(nome = chamada.name, argumentos = chamada.args)
                )
            }

            resposta = chat.sendMessage(content("user") {
                respostas.forEach { part(data = it) }
            })
            rodadas++
        }

        return resposta.text ?: "Não consegui gerar uma resposta."
    }

    private fun converterHistorico(historico: List<String>): List<Content> {
        val semSaudacao = historico.drop(1)
        val completas = if (semSaudacao.size % 2 == 0) semSaudacao else semSaudacao.dropLast(1)
        val recentes = completas.takeLast(LIMITE_MENSAGENS_HISTORICO * 2)

        return recentes.mapIndexed { indice, texto ->
            content(if (indice % 2 == 0) "user" else "model") { text(texto) }
        }
    }
}
