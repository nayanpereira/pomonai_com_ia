package br.com.senai.duvidas;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class CategoriaApiController {

    private final CategoriaRepository repository;

    public CategoriaApiController(CategoriaRepository repository) {
        this.repository = repository;
    }

    // GET /api/categorias
    @GetMapping
    public List<Categoria> listar() {
        return repository.findAll();
    }
}