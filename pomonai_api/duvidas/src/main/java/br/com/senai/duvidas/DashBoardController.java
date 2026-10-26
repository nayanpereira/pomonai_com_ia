package br.com.senai.duvidas;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashBoardController {

    private final DuvidaRepository duvidaRepository;

    public DashBoardController(DuvidaRepository duvidaRepository) {
        this.duvidaRepository = duvidaRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Métricas e estatísticas iniciais para o Dashboard
        long totalDuvidas = duvidaRepository.count();
        var ultimasDuvidas = duvidaRepository.findAllByOrderByDataHoraDesc();

        model.addAttribute("totalDuvidas", totalDuvidas);
        model.addAttribute("ultimasDuvidas", ultimasDuvidas);
        
        return "dashboard";
    }
}
