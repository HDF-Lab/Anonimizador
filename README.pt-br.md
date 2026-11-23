
# 🛡️ Anonimizador Offline — LGPD & Privacidade

Aplicativo Android **offline e de código aberto** para higienização e anonimização de dados pessoais (PII — Personally Identifiable Information) em textos, documentos PDF e conversas exportadas do WhatsApp.

O objetivo é permitir que o usuário remova ou substitua informações pessoais antes de compartilhar conteúdos com terceiros ou ferramentas de Inteligência Artificial.

---

## 🔒 Segurança e Privacidade

A privacidade é um princípio central do projeto.

- **Sem permissão de Internet:** o aplicativo é projetado para não realizar comunicação direta com servidores externos. O `AndroidManifest.xml` deve ser mantido sem a permissão `android.permission.INTERNET`.
- **Processamento local:** a extração, detecção e anonimização são realizadas no próprio dispositivo.
- **Sem telemetria:** o projeto não deve incorporar SDKs de analytics, publicidade ou rastreamento.
- **Proteção de tela:** utilização de `FLAG_SECURE` para restringir capturas de tela e gravações de tela nas telas protegidas, quando habilitado.
- **Dados temporários:** o mapeamento de anonimização reversível é destinado à sessão atual, sem necessidade de envio para servidores.

> A ausência da permissão de Internet reduz a superfície de comunicação de rede do aplicativo, mas não substitui uma auditoria das dependências, permissões, armazenamento e fluxos de compartilhamento do Android.

---

## ✨ Funcionalidades

### 📄 Importação de documentos

- **TXT:** leitura de arquivos de texto UTF-8.
- **PDF:** extração local de texto utilizando Apache PDFBox Android.
- **ZIP:** leitura de arquivos `.txt` contidos em exportações do WhatsApp.
- **Compartilhamento Android:** integração com o mecanismo de compartilhamento do sistema, conforme os filtros de intent configurados no aplicativo.

### 💬 Tratamento de conversas do WhatsApp

- Identificação e remoção de ruídos automáticos.
- Tratamento de avisos de sistema e mensagens de conteúdo indisponível, conforme as regras do parser.
- Identificação de participantes.
- Substituição dos nomes por identificadores como `[PARTICIPANTE_1]`, `[PARTICIPANTE_2]` etc.

### 🔎 Detecção de informações pessoais

Os detectores trabalham com os tipos habilitados pelo usuário.

- CPF e CNPJ, quando habilitados como tipos independentes.
- Chave PIX UUID (chave aleatória).
- Telefones brasileiros.
- E-mails.
- Endereços IPv4.
- Cartões de crédito, com validação pelo algoritmo de Luhn.
- Placas de veículos, nos padrões suportados pelo detector.

A detecção utiliza expressões regulares e validadores específicos para os formatos aplicáveis.

> A detecção é baseada em padrões conhecidos e não garante a identificação de todas as informações pessoais presentes em um documento.

### 🎭 Modos de mascaramento

**Irreversível**

- Substitui os valores identificados por etiquetas.
- Não gera um mapeamento de reversão.
- Destinado ao compartilhamento de conteúdo anonimizado.

**Reversível (sessão)**

- Substitui os valores por etiquetas.
- Mantém um mapeamento entre etiquetas e valores originais.
- O mapeamento deve permanecer restrito à sessão atual.
- A reversão depende da preservação segura desse mapeamento.

---

## 🛡️ Proteção no processamento de arquivos

O extrator de documentos implementa limites para reduzir riscos de consumo excessivo de recursos.

| Recurso | Limite configurado |
|---|---:|
| Arquivo de entrada | 20 MB |
| Texto TXT extraído | 10 MB |
| Texto extraído do PDF | 10 MB |
| Entradas ZIP | 1.000 |
| Inspeção de conteúdo descompactado | 50 MB |

O processamento ZIP possui verificações de tamanho e quantidade de entradas para reduzir riscos associados a arquivos excessivamente grandes ou compactados de forma maliciosa.

O PDFBox pode utilizar memória adicional para carregar estruturas internas do documento. Portanto, os limites de entrada e de texto extraído não representam um limite absoluto de memória.

---

## 🏗️ Arquitetura e tecnologias

O projeto utiliza princípios de **Clean Architecture**, **MVVM** e **UDF (Unidirectional Data Flow)**.

### Organização por camadas

| Camada | Responsabilidade |
|---|---|
| `core` | Validadores e utilitários compartilhados |
| `data` | Detectores, extratores e implementação do mascaramento |
| `di` | Módulos de injeção de dependência |
| `domain` | Modelos, contratos, políticas e casos de uso |
| `presentation` | ViewModel, estado da interface e telas Compose |

### Stack tecnológica

- **Linguagem:** Kotlin
- **Interface:** Jetpack Compose
- **Design System:** Material Design 3
- **Injeção de dependência:** Hilt (Dagger)
- **Assincronismo:** Kotlin Coroutines e StateFlow
- **Processamento PDF:** Apache PDFBox Android
- **Testes:** JUnit e ferramentas de teste compatíveis com as dependências do projeto.

### Estrutura de pacotes

```text
br.com.hdf.anonimizador
├── core/
│   ├── util/           # Validadores e utilitários
│   └── security/       # Proteções da janela
├── data/
│   ├── detector/       # Detectores de PII
│   ├── ingestion/      # Extração TXT, PDF e ZIP
│   ├── masking/        # Mascaramento de texto
│   └── parser/         # Parser de WhatsApp
├── di/                 # Módulos Hilt e dispatchers
├── domain/
│   ├── detector/       # Contratos de detecção
│   ├── ingestion/      # Contratos de extração
│   ├── masking/        # Contratos e consolidação
│   ├── model/          # Modelos de domínio
│   ├── parser/         # Contratos de parsing
│   └── usecase/        # Casos de uso
└── presentation/
    └── editor/         # ViewModel e interface Compose
```

---

## 🔄 Fluxo de anonimização

1. O usuário informa um texto ou importa um documento.
2. O conteúdo é preparado pelo parser, quando aplicável.
3. Os detectores identificam possíveis informações pessoais.
4. O `SpanConsolidator` valida os intervalos e resolve sobreposições.
5. O `SafeTextMasker` substitui os valores identificados.
6. O resultado é apresentado ao usuário para revisão e compartilhamento.

A consolidação prioriza o maior intervalo em caso de sobreposição, utilizando confiança e prioridade do detector como critérios de desempate.

---

## 🚀 Como compilar

1. Clone o repositório:

```bash
git clone <URL_DO_REPOSITORIO>
```

2. Abra o projeto no Android Studio.
3. Utilize a versão do JDK e do Android Gradle Plugin configurada no projeto.
4. Sincronize as dependências Gradle.
5. Execute os testes:

```bash
./gradlew test
```

6. Compile o aplicativo:

```bash
./gradlew assembleDebug
```

---

## 🧪 Limitações conhecidas

- Detectores baseados em padrões não garantem identificação completa de dados pessoais.
- PDFs digitalizados como imagem podem não produzir texto extraível sem OCR.
- A extração de ZIP considera o primeiro arquivo `.txt` encontrado.
- Arquivos com codificação diferente de UTF-8 podem apresentar problemas de interpretação.
- O mascaramento depende da qualidade dos resultados dos detectores.
- O usuário deve revisar o conteúdo anonimizado antes de compartilhá-lo.

---

## 📄 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [LICENSE](LICENSE) para obter os termos de utilização.
