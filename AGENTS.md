# Contexto do Projeto e Especificações Técnicas (Back-End)

## 1. Visão Geral do Projeto (Macro)
Este projeto nasce de uma parceria estratégica entre a **Ford** e a **FIAP** com foco em **Inteligência Competitiva Automotiva** (Desafio 01). 

### O Problema de Negócio:
Atualmente, o mapeamento de especificações técnicas de veículos concorrentes no mercado automotivo é um processo altamente manual, impreciso e demorado (consome cerca de 1 hora por versão avaliada), dependendo de buscas pulverizadas em sites, revistas especializadas, YouTube e visitas a concessionárias. 

### A Solução Esperada:
Desenvolver uma plataforma capaz de centralizar e padronizar dados técnicos da concorrência automotiva. A partir de uma entrada simples (Marca, Modelo, Versão e Atributos desejados), o sistema deve consolidar uma lista de especificações clara, organizada e diretamente comparável.

---

## 2. Escopo do Back-End & Requisitos da Sprint
O objetivo central deste microsserviço é fornecer a fundação robusta de APIs, regras de negócio, persistência de dados e integrações necessárias para sustentar a inteligência do ecossistema.

O desenvolvimento deve seguir rigorosamente os critérios de avaliação da arquitetura:

### A. Integração por Web Services & APIs
*   **Contrato Restful:** Implementação de APIs baseadas estritamente no padrão RESTful.
*   **Semântica HTTP:** Uso preciso e adequado dos métodos HTTP (`GET` para consultas, `POST` para criação/processamento, `PUT`/`PATCH` para atualizações, `DELETE` para remoções) e seus respectivos códigos de status (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`).
*   **Documentação Viva:** Toda a API deve ser documentada utilizando **Swagger/OpenAPI**, servindo como contrato de integração claro para as camadas de apresentação (Front-end/Agentes).

### B. Arquitetura Orientada a Serviços (SOA)
*   **Modularidade:** Organização do código baseada em serviços independentes, fracamente acoplados e altamente reutilizáveis.
*   **Separação Clara de Camadas:** Isolamento estrito de responsabilidades:
    *   **Camada de Apresentação/Controladores (`Controller`):** Validação de entrada de dados (DTOs) e exposição dos endpoints.
    *   **Camada de Serviço (`Service`):** Onde reside o fluxo do motor de busca, regras de negócio e orquestração.
    *   **Camada de Dados (`Repository`):** Interface de comunicação exclusiva com o banco de dados.

### C. Padrões, Boas Práticas & Resiliência
*   **Formatos de Dados:** Uso de JSON como formato padrão de tráfego de dados na API.
*   **Tratamento Global de Exceções:** Implementação de um manipulador de exceções global (`@ControllerAdvice`) para capturar erros, evitando vazamento de stack traces de infraestrutura e retornando payloads de erro padronizados e amigáveis para o cliente.

### D. Camada de Dados & Persistência
*   **Banco de Dados:** PostgreSQL.
*   **Gerenciamento de Migrações:** Uso obrigatório de uma ferramenta de versionamento de banco de dados (ex: Flyway ou Liquibase) para garantir o controle evolutivo do esquema do banco de dados.

---

## 3. Stack Tecnológica Obrigatória
Ao gerar código, configurações ou correções, utilize estritamente a stack abaixo:
*   **Linguagem:** Java 25 (Latest LTS)
*   **Framework:** Spring Boot 4.x (Latest LTS correspondente ao Java 25)
*   **Persistência:** Spring Data JPA / Hibernate
*   **Banco de Dados:** PostgreSQL 
*   **Ambiente de Dev/Infra:** Docker & Docker Compose (para subir a instância do Postgres localmente de forma isolada)

---

## 4. Regras de Negócio Cruciais para a IA (Critérios de Aceite)

1. **Flexibilidade de Filtros:** A API de processamento/busca deve obrigatoriamente aceitar uma lista dinâmica de atributos técnicos definidos livremente pelo usuário (ex: "Motor", "Potência", "Preço"), além dos campos mandatórios de pivotagem: `Marca`, `Modelo` e `Versão`.
2. **Saída Padronizada e Consistente:** O payload de saída gerado pelo Back-end deve manter estritamente a mesma estrutura de chaves/campos, independente do veículo consultado.
3. **Tratamento de Dados Ausentes:** Caso um atributo solicitado pelo usuário não seja encontrado na varredura da concorrência, a API **não** deve ocultar o campo. Ela deve retornar o campo explicitamente marcado como nulo, string vazia ou `"Não disponível"`.
4. **Massa de Teste e Validação OBRIGATÓRIA:** 
    O benchmark de sucesso absoluto do sistema é a capacidade de mapear com precisão as especificações da **Ford Ranger Raptor**. O sistema deve estar preparado para lidar estruturalmente com dados complexos de performance como o deste exemplo:
    *   *Motor:* V6 3.0L Nano bi turbo
    *   *Potência:* 397cv @ 5650 RPM
    *   *Torque:* 583 Nm @ 3500 RPM
    *   *Transmissão:* AT de 10 velocidades e paddle shifters
    *   *Modos de condução:* Normal, Sport, Escorregadio, Lama, Areia, Rock Crawl, Baja
    *   *Preço de referência:* R$ 499.000

---

## 5. Instruções de Prompt para o Agente (Como me ajudar)
*   Ao gerar classes de entidade, lembre-se de incluir as anotações do JPA e criar os arquivos de migração correspondentes (SQL).
*   Sempre use as novas features do Java 25 onde fizer sentido (como *Records* para DTOs, *Pattern Matching*, novos métodos de coleções, etc.).
*   Garanta que todo novo controlador criado contenha as anotações necessárias para que o Swagger documente o endpoint automaticamente.