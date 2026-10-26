package br.com.senai.duvidas;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    // O plano gratuito do Gemini limita os tokens de embedding por minuto,
    // então os blocos do PDF são enviados em lotes, com pausa entre eles.
    private static final int TAMANHO_LOTE = 20;
    private static final long PAUSA_ENTRE_LOTES_MS = 30_000;
    private static final long ESPERA_RETRY_MS = 60_000;
    private static final int MAX_TENTATIVAS = 3;

    // Tempo máximo de espera pela resposta do Gemini ao responder uma dúvida
    private static final long TIMEOUT_IA_SEGUNDOS = 60;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AiService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public String responderDuvida(String pergunta) {
        long inicio = System.nanoTime();
        log.info("Dúvida recebida, buscando trechos no ChromaDB...");

        // 1. Busca os trechos mais relevantes no ChromaDB
        List<Document> documentosRelevantes = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(pergunta)
                        .topK(3)
                        .build());

        log.info("Busca no ChromaDB concluída em {} ms ({} trechos). Chamando o Gemini...",
                (System.nanoTime() - inicio) / 1_000_000, documentosRelevantes.size());

        // 2. Junta o texto dos documentos encontrados
        String contexto = documentosRelevantes.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 3. System prompt limitando a IA ao material
        String instrucaoSistema = """
                Você é a IA assistente do aplicativo de estudos Pomonai.
                Responda à dúvida do aluno de forma clara, didática e objetiva, em português.

                Regras:
                1. Priorize o CONTEXTO abaixo, que vem do material de estudo enviado pelo aluno e pelo professor.
                2. Se o contexto responder a dúvida, use-o e não diga que algo está fora do material.
                3. Se o contexto não tratar do assunto ou for insuficiente, responda com seu conhecimento geral,
                   de forma curta e correta, começando com: 'Isso não está no material enviado, mas posso explicar:'.
                4. Se você não tiver certeza da resposta, diga isso com honestidade em vez de inventar.

                CONTEXTO:
                {contexto}
                """;

        // 4. Envia para o Gemini, com limite de tempo
        CompletableFuture<String> chamada = CompletableFuture.supplyAsync(() ->
                chatClient.prompt()
                        .system(s -> s.text(instrucaoSistema).param("contexto", contexto))
                        .user(pergunta)
                        .call()
                        .content());

        try {
            String resposta = chamada.get(TIMEOUT_IA_SEGUNDOS, TimeUnit.SECONDS);
            log.info("Gemini respondeu. Tempo total: {} ms", (System.nanoTime() - inicio) / 1_000_000);
            return resposta;
        } catch (TimeoutException e) {
            chamada.cancel(true);
            log.error("Gemini não respondeu em {}s", TIMEOUT_IA_SEGUNDOS);
            throw new IllegalStateException("O Gemini não respondeu em " + TIMEOUT_IA_SEGUNDOS + "s", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha ao gerar resposta no Gemini", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        }
    }

    public void salvarDocumento(MultipartFile arquivo) throws IOException {
        final String nomeArquivo = arquivo.getOriginalFilename();
        ByteArrayResource resource = new ByteArrayResource(arquivo.getBytes()) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        };

        PagePdfDocumentReader leitorPdf = new PagePdfDocumentReader(resource);

        // Divide o texto em blocos menores para a busca vetorial
        TokenTextSplitter formatador = new TokenTextSplitter();
        List<Document> fragmentos = formatador.apply(leitorPdf.get());

        log.info("PDF '{}' dividido em {} blocos", nomeArquivo, fragmentos.size());

        // Gera os embeddings e salva no ChromaDB, em lotes
        for (int inicio = 0; inicio < fragmentos.size(); inicio += TAMANHO_LOTE) {
            int fim = Math.min(inicio + TAMANHO_LOTE, fragmentos.size());
            adicionarComRetry(fragmentos.subList(inicio, fim));
            log.info("Blocos {} a {} de {} salvos", inicio + 1, fim, fragmentos.size());

            if (fim < fragmentos.size()) {
                dormir(PAUSA_ENTRE_LOTES_MS);
            }
        }
    }

    private void adicionarComRetry(List<Document> lote) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                vectorStore.add(lote);
                return;
            } catch (RuntimeException e) {
                if (tentativa >= MAX_TENTATIVAS || !limiteExcedido(e)) {
                    throw e;
                }
                log.warn("Limite do Gemini atingido (tentativa {}). Aguardando 60s...", tentativa);
                dormir(ESPERA_RETRY_MS);
            }
        }
    }

    private boolean limiteExcedido(Throwable erro) {
        while (erro != null) {
            String mensagem = erro.getMessage();
            if (mensagem != null && (mensagem.contains("429") || mensagem.contains("quota"))) {
                return true;
            }
            erro = erro.getCause();
        }
        return false;
    }

    private void dormir(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        }
    }
}