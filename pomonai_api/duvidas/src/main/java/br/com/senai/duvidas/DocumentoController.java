package br.com.senai.duvidas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documentos")
@CrossOrigin(origins = "http://localhost:8081")
public class DocumentoController {

    @Autowired
    private AiService aiService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            aiService.salvarDocumento(arquivo);
            return ResponseEntity.ok("Documento processado e salvo no ChromaDB com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar PDF: " + e.getMessage());
        }
    }
}