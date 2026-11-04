package br.com.senai.duvidas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FaqController {

    private final DuvidaRepository repository;
    private final CategoriaRepository categoriaRepository;

    public FaqController(DuvidaRepository repository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
    }

    // Envia os dados para o HTML
    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("duvidas", repository.findAllByOrderByDataHoraDesc());
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "faq";
    }

    // O professor acrescenta (ou edita) a resposta de uma dúvida
    @PostMapping("/faq/{id}/resposta")
    public String responder(@PathVariable Long id, @RequestParam("resposta") String resposta) {
        repository.findById(id).ifPresent(duvida -> {
            String texto = resposta == null ? "" : resposta.trim();
            duvida.setRespostaProfessor(texto.isEmpty() ? null : texto);
            repository.save(duvida);
        });
        return "redirect:/faq";
    }

    // Define (ou remove) a categoria de uma dúvida
    @PostMapping("/faq/{id}/categoria")
    public String definirCategoria(@PathVariable Long id,
                                   @RequestParam(value = "categoriaId", required = false) Long categoriaId) {
        repository.findById(id).ifPresent(duvida -> {
            if (categoriaId == null) {
                duvida.setCategoria(null);
            } else {
                categoriaRepository.findById(categoriaId).ifPresent(duvida::setCategoria);
            }
            repository.save(duvida);
        });
        return "redirect:/faq";
    }

    // Exclui a dúvida do banco de dados
    @PostMapping("/faq/excluir")
    public String excluir(@RequestParam("id") Long id) {
        repository.deleteById(id);
        return "redirect:/faq";
    }
}