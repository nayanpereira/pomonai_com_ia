# Pomonai

Pomonai é um aplicativo de estudos que combina um temporizador Pomodoro com um assistente de dúvidas baseado em IA. O aluno envia um PDF com o material da aula, faz perguntas durante a sessão de estudo e recebe respostas geradas a partir desse material. Cada dúvida vai para uma página de FAQ, onde o professor pode acrescentar a sua própria resposta.

O projeto foi desenvolvido por Nayan como parte do curso de Java FullStack do SENAI.

## Como funciona

Ao anexar um PDF, a API lê o documento, divide o texto em blocos menores, gera um vetor numérico (embedding) para cada bloco usando o Gemini e guarda tudo no ChromaDB. Quando o aluno envia uma dúvida, a API procura no ChromaDB os trechos mais parecidos com a pergunta e pede ao Gemini que responda com base neles. Se o material não trata do assunto, a IA responde com conhecimento geral e avisa que aquela parte não vem do PDF.

A dúvida e a resposta da IA são salvas no MySQL e aparecem na página de FAQ da API, em `/faq`. Lá o professor pode escrever ou editar a sua resposta, que fica salva junto da dúvida, e também definir a categoria de cada dúvida. A página `/dashboard` mostra o total de dúvidas e as mais recentes. O FAQ completo pode ser exportado em PDF.

## Tecnologias

A API usa Java 25, Spring Boot 3.5, Spring AI 1.1, MySQL 8 com migrations do Flyway, ChromaDB como banco vetorial, a API do Google Gemini para o chat e os embeddings, Thymeleaf para as páginas de FAQ e dashboard, OpenHTMLtoPDF para gerar o PDF do FAQ e springdoc para a documentação Swagger. O aplicativo é feito em React Native com Expo e roda no navegador e no celular.

## Pré-requisitos

- JDK 25
- MySQL 8 rodando na porta 3306
- Python 3.10 ou superior (para o ChromaDB)
- Git
- Node.js (para o aplicativo)
- Uma chave de API gratuita do Google AI Studio

## Ordem de execução

São três serviços, e eles devem ser iniciados nesta ordem:

1. **ChromaDB** (banco vetorial)
2. **API** (Spring Boot)
3. **Aplicativo** (Expo)

O MySQL também precisa estar rodando antes de subir a API.

Você pode abrir **um terminal para cada serviço** no VS Code (menu **Terminal > New Terminal**, e use o botão **+** ou o ícone de dividir terminal para abrir mais). Assim cada serviço fica rodando no seu terminal, e você vê os logs de todos ao mesmo tempo.

---

## 1. ChromaDB (banco de dados vetorial)

O servidor do ChromaDB fica em um repositório separado: https://github.com/nayanpereira/chroma

### Baixar e preparar (só na primeira vez)

Em uma pasta fora do projeto da API, clone o repositório:

```
git clone https://github.com/nayanpereira/chroma.git
cd chroma
```

Crie o ambiente virtual, que isola as dependências do projeto:

```
python -m venv venv
```

> **Nota (PowerShell):** se a execução de scripts estiver desabilitada, libere a sessão atual com:
>
> ```
> Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
> ```

Instale as dependências:

```
.\venv\Scripts\pip install -r requirements.txt
```

Se o arquivo `requirements.txt` não existir, instale direto:

```
.\venv\Scripts\pip install chromadb
```

### Iniciar o servidor

Dentro da pasta `chroma`, execute:

```
.\venv\Scripts\chroma run --path .\dados --host 127.0.0.1 --port 8000
```

O servidor escuta apenas em `127.0.0.1`, o que mantém o banco isolado da rede local. A pasta `dados` é criada automaticamente quando a primeira coleção é gerada e é onde os vetores ficam guardados.

Deixe esse terminal aberto. Para encerrar o servidor, use `Ctrl+C`.

### Testar a conexão

Com o servidor rodando, confirme que ele responde:

- URL base: `http://127.0.0.1:8000`
- Heartbeat: `http://127.0.0.1:8000/api/v2/heartbeat`

Pelo PowerShell (em outro terminal):

```
Invoke-RestMethod -Uri "http://127.0.0.1:8000/api/v2/heartbeat"
```

---

## 2. API (Spring Boot)

### Conexão com o ChromaDB

O arquivo `src/main/resources/application.properties` da API precisa apontar para o servidor do Chroma:

```
spring.ai.vectorstore.chroma.client.host=http://127.0.0.1
spring.ai.vectorstore.chroma.client.port=8000
```

### Arquivo de segredos (`secrets.properties`)

As senhas e chaves nunca ficam no `application.properties`: elas são lidas de um arquivo `secrets.properties`, que está no `.gitignore` e não é enviado ao GitHub.

Dentro da pasta `duvidas`, copie o modelo:

```
copy src\main\resources\secrets.properties.example src\main\resources\secrets.properties
```

No Linux ou no macOS, use `cp` no lugar de `copy` e barras normais no caminho.

Abra o `secrets.properties` e substitua os valores de exemplo pelos seus:

```
DB_USERNAME=root
DB_PASSWORD=sua_senha_do_mysql
GEMINI_API_KEY=sua_chave_do_google_ai_studio
```

- **`DB_USERNAME`**: o usuário do seu MySQL (geralmente `root`).
- **`DB_PASSWORD`**: a senha desse usuário no MySQL.
- **`GEMINI_API_KEY`**: a chave de API do Gemini. Para obtê-la, entre em https://aistudio.google.com/app/apikey, faça login com a sua conta Google, clique em **Create API key** e copie a chave gerada para o arquivo.

Não é preciso criar o banco de dados manualmente: a API cria o schema `pomonaidb` na primeira execução e o Flyway aplica as migrations sozinho. Se o seu MySQL não estiver em `localhost:3306`, ajuste a URL de conexão no `application.properties`.

Se preferir, você pode definir esses mesmos três nomes como variáveis de ambiente em vez de usar o arquivo.

### Iniciar a API

Em um novo terminal, dentro da pasta `duvidas`, execute:

```
./mvnw spring-boot:run
```

No Windows (PowerShell), use:

```
.\mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`. Na primeira execução, o log deve mostrar o Flyway aplicando as migrations e, no fim, a mensagem `Started DuvidasApplication`.

Se aparecer um erro dizendo que não foi possível resolver `DB_PASSWORD` ou `GEMINI_API_KEY`, o arquivo `secrets.properties` não foi criado ou está no lugar errado. Se aparecer erro de conexão com o Chroma, confirme que o passo 1 está rodando.

### Rotas principais

| Rota | Descrição |
|---|---|
| `POST /documentos/upload` | Envia o PDF de estudo (campo `arquivo`) |
| `GET /duvida` | Lista as dúvidas |
| `POST /duvida` | Cadastra uma dúvida (campos `texto`, `tipoPausa` e, opcionalmente, `categoriaId`) |
| `GET /api/categorias` | Lista as categorias em JSON |
| `/categorias/nova` | Formulário para cadastrar uma categoria |
| `/faq` | Página do professor: responder dúvidas e definir categorias |
| `/api/faq/pdf` | Gera e exibe o PDF do FAQ |
| `/dashboard` | Painel com o total de dúvidas e as mais recentes |
| `/swagger-ui.html` | Documentação interativa da API |

---

## 3. Aplicativo (Expo)

Em um terceiro terminal, na pasta do aplicativo Expo, instale as dependências e inicie o servidor de desenvolvimento:

```
npm install
npx expo start
```

Abra a versão web pelo terminal do Expo. As URLs da API ficam no início do arquivo da tela principal, nas constantes `API_URL_DUVIDA` e `API_URL_UPLOAD`. Para testar em um celular físico, troque `localhost` pelo endereço IP do computador na rede local.

---

## Resumo: três terminais no VS Code

| Terminal | Pasta | Comando |
|---|---|---|
| 1. ChromaDB | `chroma` | `.\venv\Scripts\chroma run --path .\dados --host 127.0.0.1 --port 8000` |
| 2. API | `duvidas` | `.\mvnw.cmd spring-boot:run` |
| 3. Aplicativo | pasta do app Expo | `npx expo start` |

Para parar tudo, use `Ctrl+C` em cada terminal.

## Observações

O plano gratuito do Gemini limita a quantidade de tokens de embedding por minuto. Por isso o envio de PDFs grandes é feito em lotes, com pausas, e pode levar de um a dois minutos. A resposta da IA a uma dúvida tem um limite de 60 segundos: se o Gemini não responder nesse prazo, a dúvida é salva sem resposta da IA para que o professor possa responder.

A página `/faq` não tem login, então qualquer pessoa com acesso à API consegue responder como professor. Antes de colocar o projeto em um servidor público, é preciso proteger essa rota. O arquivo `secrets.properties` é copiado para a pasta `target` durante a compilação, por isso um `.jar` gerado localmente não deve ser compartilhado.