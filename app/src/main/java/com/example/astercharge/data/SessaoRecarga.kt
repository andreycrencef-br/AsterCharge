package com.example.astercharge.data

data class SessaoRecarga(
    val ativa: Boolean = false,
    val finalizada: Boolean = false,
    val idCarregador: String? = null,
    val bateriaInicial: Double = 10.0,
    val bateriaAtual: Double = 10.0,
    val capacidadeBateriaKwh: Double = 60.0,
    val kwhEntregue: Double = 0.0,
    val tempoDecorridoMinutos: Int = 0,
    val tempoRestanteEstimadoMinutos: Int = 0,
    val potenciaNominalKw: Double = 0.0,
    val potenciaAtualKw: Double = 0.0,
    val tarifaAtual: Double = 0.0,
    val custoEnergia: Double = 0.0,
    val taxaConexao: Double = 2.50,
    val multaOciosidade: Double = 0.0,
    val percentualUtilizacaoRede: Double = 0.0,
    val throttleAtivo: Boolean = false,
    val emOciosidade: Boolean = false,
    val minutosOciosidade: Int = 0,
    val etapaCCCV: String = "CC"
) {
    val custoTotal: Double
        get() = custoEnergia + taxaConexao + multaOciosidade
}
