package br.com.senai.duvidas;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para download e visualização do PDF das dúvidas/FAQ.
 */
@RestController
@RequestMapping("/api/faq")
public class FaqPdfController {

    private final FaqExportService faqExportService;

    public FaqPdfController(FaqExportService faqExportService) {
        this.faqExportService = faqExportService;
    }

    /**
     * Endpoint GET para gerar e visualizar o PDF do FAQ.
     * Exemplo de acesso: GET http://localhost:8080/api/faq/pdf
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> baixarFaqPdf() {
        byte[] pdfBytes = faqExportService.gerarFaqPdf();

        // inline = abre no navegador; para forçar download, troque por ContentDisposition.attachment()
        ContentDisposition disposition = ContentDisposition.inline()
                .filename("faq-duvidas.pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}