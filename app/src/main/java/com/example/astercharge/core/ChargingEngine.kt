package com.example.astercharge.core

data class ChargingStepResult(
    val bateria: Double,
    val kwhEntregue: Double,
    val potenciaEfetivaKw: Double,
    val percentualRede: Double,
    val throttleAtivo: Boolean,
    val etapaCCCV: String
)

object ChargingEngine {

    const val CAPACIDADE_REDE_KW = 200.0
    const val DEMANDA_EXTERNA_KW = 100.0

    /**
     * Calcula a potência permitida pela Smart Grid.
     *
     * Regra do protótipo:
     * - até 80% da rede: potência nominal;
     * - acima de 80%: reduz progressivamente;
     * - nunca ultrapassa a capacidade restante da rede.
     */
    fun calcularPotenciaSmartGrid(
        potenciaSolicitadaKw: Double,
        demandaExternaKw: Double = DEMANDA_EXTERNA_KW,
        capacidadeRedeKw: Double = CAPACIDADE_REDE_KW
    ): Pair<Double, Boolean> {

        val capacidadeDisponivel =
            (capacidadeRedeKw - demandaExternaKw).coerceAtLeast(0.0)

        val potenciaBruta =
            potenciaSolicitadaKw.coerceAtMost(capacidadeDisponivel)

        val utilizacaoAntesDoThrottle =
            ((demandaExternaKw + potenciaBruta) / capacidadeRedeKw) * 100.0

        val potenciaPermitida = when {
            utilizacaoAntesDoThrottle <= 80.0 ->
                potenciaBruta

            utilizacaoAntesDoThrottle <= 95.0 ->
                potenciaBruta * 0.75

            else ->
                potenciaBruta * 0.50
        }

        return Pair(
            potenciaPermitida.coerceAtMost(capacidadeDisponivel),
            utilizacaoAntesDoThrottle > 80.0
        )
    }

    /**
     * Curva CC-CV simplificada para o protótipo acadêmico.
     */
    fun calcularPotenciaCCCV(
        bateria: Double,
        potenciaDisponivelKw: Double
    ): Pair<Double, String> {

        return when {
            bateria < 80.0 ->
                Pair(potenciaDisponivelKw, "CC")

            bateria < 90.0 ->
                Pair(potenciaDisponivelKw * 0.70, "CV")

            bateria < 95.0 ->
                Pair(potenciaDisponivelKw * 0.50, "CV")

            bateria < 100.0 ->
                Pair(potenciaDisponivelKw * 0.30, "CV")

            else ->
                Pair(0.0, "CONCLUÍDA")
        }
    }

    fun simularMinuto(
        bateriaAtual: Double,
        kwhEntregueAtual: Double,
        potenciaNominalKw: Double,
        capacidadeBateriaKwh: Double
    ): ChargingStepResult {

        val (potenciaSmartGrid, throttle) =
            calcularPotenciaSmartGrid(
                potenciaSolicitadaKw = potenciaNominalKw
            )

        val (potenciaFinal, etapa) =
            calcularPotenciaCCCV(
                bateria = bateriaAtual,
                potenciaDisponivelKw = potenciaSmartGrid
            )

        val energiaPorMinuto = potenciaFinal / 60.0

        val novoKwh =
            kwhEntregueAtual + energiaPorMinuto

        val aumentoBateria =
            if (capacidadeBateriaKwh > 0.0) {
                (energiaPorMinuto / capacidadeBateriaKwh) * 100.0
            } else {
                0.0
            }

        val novaBateria =
            (bateriaAtual + aumentoBateria).coerceAtMost(100.0)

        val percentualRede =
            ((DEMANDA_EXTERNA_KW + potenciaFinal) / CAPACIDADE_REDE_KW) * 100.0

        return ChargingStepResult(
            bateria = novaBateria,
            kwhEntregue = novoKwh,
            potenciaEfetivaKw = potenciaFinal,
            percentualRede = percentualRede,
            throttleAtivo = throttle,
            etapaCCCV = etapa
        )
    }

    /**
     * Estima o tempo usando o mesmo motor CC-CV + Smart Grid
     * usado na simulação.
     */
    fun estimarTempo(
        bateriaInicial: Double,
        potenciaNominalKw: Double,
        capacidadeBateriaKwh: Double
    ): Int {

        if (bateriaInicial >= 100.0) return 0

        var bateria = bateriaInicial
        var kwh = 0.0
        var minutos = 0

        while (bateria < 100.0 && minutos < 1000) {

            val passo = simularMinuto(
                bateriaAtual = bateria,
                kwhEntregueAtual = kwh,
                potenciaNominalKw = potenciaNominalKw,
                capacidadeBateriaKwh = capacidadeBateriaKwh
            )

            bateria = passo.bateria
            kwh = passo.kwhEntregue
            minutos++
        }

        return minutos
    }
}
