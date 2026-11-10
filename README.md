# AsterCharge — Smart Grid & EV Analytics Platform

O **AsterCharge** é uma plataforma inteligente de simulação, monitoramento e otimização analítica para recarga de veículos elétricos (EV). O projeto foi desenhado para resolver desafios reais de infraestrutura de energia, combinando modelagem matemática de baterias, pipelines de dados climáticos em tempo real e um Agente de IA Generativa contextualizado.

---

## Arquitetura

### 1. Motor de Simulação Analítica e Modelagem Matemática (`ChargingEngine.kt`)
*   **Modelagem de Curva CC-CV:** Implementação computacional que simula as fases de corrente constante e tensão constante de baterias de íon de lítio. O algoritmo reduz dinamicamente a eficiência e potência de recarga ao ultrapassar os limiares de 80%, 90% e 95% de carga para preservação da saúde da bateria.
*   **Balanceamento Baseado em Smart Grid:** Simulação reativa de estresse da rede elétrica com capacidade teto de 200 kW. O sistema aplica técnicas de *throttle* automatizado, reduzindo a potência disponível dos carregadores para 75% ou 50% quando a demanda global atinge níveis críticos (80% e 95% de uso da malha), mitigando riscos de bleocaute.

### 2. Pipeline de Ingestão de Dados e Precificação Dinâmica (`PrecificacaoSolar.kt`)
*   **Consumo de API Meteorológica:** Integração com a API pública *Open-Meteo* para ingestão de variáveis em tempo real de irradiância solar ($W/m^2$) e índice de nebulosidade geoespacial.
*   **Algoritmo de Tarifação Inteligente:** Mapeamento de janelas de geração solar (08h às 17h) versus horários de pico de consumo da rede (17h às 22h, com acréscimo de 20% na tarifa). O sistema cruza os dados climáticos com o estresse da rede para incentivar o carregamento em horários de energia limpa através de descontos dinâmicos de até 20%.

### 3. Agente de IA com Processamento de Contexto Expandido (`ContextoAssistente.kt` & `GeminiService.kt`)
*   **LLM Orientado a Ferramentas (Function Calling):** Uso avançado do SDK do Google Gemini para criar um agente assistente capaz de tomar decisões autônomas. Em vez de apenas responder textos, a IA avalia o contexto do usuário e dispara chamadas de funções mapeadas (`Tool.functionDeclarations`).
*   **Payload Estruturado:** A IA toma decisões baseando-se em um estado consolidado enviado a cada requisição: coordenadas geográficas do usuário, telemetria da bateria do veículo (capacidade kWh e autonomia estimada), status de filas de espera dos eletropostos (regra FIFO) e condições solares atuais.

---

## Tecnologias Utilizadas

*   **Linguagem Core:** Kotlin
*   **UI Dinâmica:** Jetpack Compose (Arquitetura declarativa e reativa para gráficos de demanda)
*   **Inteligência Artificial:** Firebase Vertex AI SDK & Google Gemini (Modelos Fundacionais)
*   **Serviços Geoespaciais:** OpenStreetMap / Nominatim API (Geocodificação e endereçamento reverso)
*   **Dados e Clima:** API REST Open-Meteo (Dados abertos de irradiância)

---

## O que este projeto demonstra?

1.  **Governança e Boas Práticas de Segurança:** Arquitetura configurada para mascaramento de dados sensíveis de pagamento (compliance conceitual com PCI-DSS) e isolamento de chaves privadas de nuvem através de estruturas de segurança de build no Gradle (`.gitignore`).
2.  **Tomada de Decisão Baseada em Dados:** Criação de regras de negócio complexas que transformam dados brutos de APIs (clima e localização) em inteligência financeira (tarifação dinâmica).
3.  **Desenvolvimento Assistido (Vibecoding):** Demonstração prática de adaptabilidade tecnológica e engenharia de prompt avançada, utilizando IA para acelerar o desenvolvimento de arquiteturas modernas de software.
