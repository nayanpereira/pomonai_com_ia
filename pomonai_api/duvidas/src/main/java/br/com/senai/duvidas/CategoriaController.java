package br.com.senai.duvidas;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaRepository repository;

    public CategoriaController(CategoriaRepository repository) {
        this.repository = repository;
    }

    // Abre o formulário: GET /categorias/nova
    @GetMapping("/nova")
    public String formulario() {
        return "formulario-categoria";
    }

    // Recebe o formulário: POST /categorias/salvar
    @PostMapping("/salvar")
    public String salvar(@RequestParam("nome") String nome) {
        String nomeLimpo = nome == null ? "" : nome.trim();

        if (!nomeLimpo.isEmpty() && !repository.existsByNomeIgnoreCase(nomeLimpo)) {
            repository.save(new Categoria(nomeLimpo));
        }

        return "redirect:/faq";
    }
}