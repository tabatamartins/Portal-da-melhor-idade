package br.edu.ifsp.portalmelhoridade.controller;

import br.edu.ifsp.portalmelhoridade.service.CatalogoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

    private final CatalogoService catalogo;

    public PaginaController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("catalogo", catalogo.catalogo());
        return "index";
    }
}
