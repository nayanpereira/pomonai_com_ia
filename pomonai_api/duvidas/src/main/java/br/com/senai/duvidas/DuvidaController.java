package br.com.senai.duvidas;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/duvida")
@CrossOrigin(origins = "*")
public class DuvidaController {

    private static final Logger log = LoggerFactory.getLogger(DuvidaController.class);

    @Autowired
    private DuvidaRepository repository;

    @Autowired
    private AiService aiService;

    @GetMapping
    public List<Duvida> listar() {
        return repository.findAllByOrderByDataHoraDesc();
    }

    @PostMapping
    public ResponseEntity<Duvida> cadastrar(@RequestBody @Valid DadosCadastroDuvida dados) {
        Duvida novaDuvida = new Duvida(dados);

        try {
            // Aciona o RAG para gerar a resposta com base nos documentos
            novaDuvida.setRespostaIa(aiService.responderDuvida(dados.texto()));
        } catch (Exception e) {
            // A IA falhou, mas a dúvida continua indo para o FAQ
            log.error("Falha ao gerar resposta da IA", e);
            novaDuvida.setRespostaIa(null);
        }

        repository.save(novaDuvida);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaDuvida);
    }
}