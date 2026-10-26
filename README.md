# Pomonai

Pomonai é um aplicativo de estudos que combina um temporizador Pomodoro com um assistente de dúvidas baseado em IA. O aluno envia um PDF com o material da aula, faz perguntas durante a sessão de estudo e recebe respostas geradas a partir desse material. Cada dúvida vai para uma página de FAQ, onde o professor pode acrescentar a sua própria resposta.

O projeto foi desenvolvido por Nayan como parte do curso de Java FullStack do SENAI.

## Como funciona

Ao anexar um PDF, a API lê o documento, divide o texto em blocos menores, gera um vetor numérico (embedding) para cada bloco usando o Gemini e guarda tudo no ChromaDB. Quando o aluno envia uma dúvida, a API procura no ChromaDB os trechos mais parecidos com a pergunta e pede ao Gemini que responda com base neles. Se o material não trata do assunto, a IA responde com conhecimento geral e avisa que aquela parte não vem do PDF.

A dúvida e a resposta da IA são salvas no MySQL e aparecem na página de FAQ da API, em `/faq`. Lá o professor pode escrever ou editar a sua resposta, que fica salva junto da dúvida. A página `/dashboard` mostra o total de dúvidas e as mais recentes.

## Tecnologias

A API usa Java 25, Spring Boot 3.5, Spring AI 1.1, MySQL 8 com migrations do Flyway, ChromaDB como banco vetorial, a API do Google Gemini para o chat e os embeddings, Thymeleaf para as páginas de FAQ e dashboard e springdoc para a documentação Swagger. O aplicativo é feito em React Native com Expo e roda no navegador e no celular.

## Pré-requisitos

Para rodar o projeto você precisa do JDK 25, do MySQL 8 rodando na porta 3306, do Docker para subir o ChromaDB, do Node.js para o aplicativo e de uma chave de API gratuita do Google AI Studio, que pode ser criada em https://aistudio.google.com/apikey.

## Configuração

Primeiro, suba o ChromaDB em um contêiner. O volume nomeado guarda os dados fora da pasta do projeto, então nada do banco vetorial vai parar no repositório.

```
docker run -d --name chroma -p 8000:8000 -v chroma-data:/data chromadb/chroma
```

Depois, crie o arquivo de segredos. As senhas e chaves nunca ficam no `application.properties`: elas são lidas de um arquivo `secrets.properties` que está no `.gitignore` e não é enviado ao GitHub. Dentro da pasta `duvidas`, copie o modelo e preencha com os seus valores.

```
copy src\main\resources\secrets.properties.example src\main\resources\secrets.properties
```

No Linux ou no macOS, use `cp` no lugar de `copy` e barras normais no caminho. O arquivo tem três valores: o usuário do MySQL, a senha do MySQL e a chave do Gemini.

```
DB_USERNAME=root
DB_PASSWORD=sua_senha_do_mysql
GEMINI_API_KEY=sua_chave_do_google_ai_studio
```

Se preferir, você pode definir esses mesmos nomes como variáveis de ambiente em vez de usar o arquivo. Não é preciso criar o banco de dados: a API cria o schema `pomonaidb` na primeira execução e o Flyway aplica as migrations sozinho.

## Rodando a API

Dentro da pasta `duvidas`, execute:

```
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`. A API sobe em http://localhost:8080. Se aparecer um erro dizendo que não foi possível resolver `DB_PASSWORD` ou `GEMINI_API_KEY`, o arquivo `secrets.properties` não foi criado ou está no lugar errado.

As rotas principais são `POST /documentos/upload` para enviar o PDF (campo `arquivo`), `GET /duvida` e `POST /duvida` para listar e cadastrar dúvidas, `/faq` para a página do professor, `/dashboard` para o painel e `/swagger-ui.html` para a documentação interativa.

## Rodando o aplicativo

Na pasta do aplicativo Expo, instale as dependências e inicie o servidor de desenvolvimento.

```
npm install
npx expo start
```

Abra a versão web pelo terminal do Expo. As URLs da API ficam no início do arquivo da tela principal, nas constantes `API_URL_DUVIDA` e `API_URL_UPLOAD`. Para testar em um celular físico, troque `localhost` pelo endereço IP do computador na rede local.

## Observações

O plano gratuito do Gemini limita a quantidade de tokens de embedding por minuto. Por isso o envio de PDFs grandes é feito em lotes, com pausas, e pode levar de um a dois minutos. A resposta da IA a uma dúvida tem um limite de 60 segundos: se o Gemini não responder nesse prazo, a dúvida é salva sem resposta da IA para que o professor possa responder.

A página `/faq` não tem login, então qualquer pessoa com acesso à API consegue responder como professor. Antes de colocar o projeto em um servidor público, é preciso proteger essa rota. O arquivo `secrets.properties` é copiado para a pasta `target` durante a compilação, por isso um `.jar` gerado localmente não deve ser compartilhado.
