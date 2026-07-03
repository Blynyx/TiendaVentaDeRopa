package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.service.MetricasService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// Controlador para paginas simples y metricas.
@Controller
public class PaginaController {

    private final MetricasService metricasService;

    public PaginaController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/login";
    }

    @GetMapping("/principal")
    public String principal(HttpSession session, Model model) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);

        if (usuario == null) {
            return "redirect:/login";
        }

        if (UsuarioController.esAdmin(usuario)) {
            return "redirect:/gestion";
        }

        if (UsuarioController.esRepartidor(usuario)) {
            return "redirect:/delivery/pedidos";
        }

        model.addAttribute("usuario", usuario.getNombre());
        model.addAttribute("mensaje", "Bienvenido a Urban Style");
        return "thymeleaf/main";
    }

    @GetMapping("/gestion")
    public String gestion(HttpSession session) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        return "thymeleaf/gestion";
    }

    @GetMapping("/contacto")
    public String contacto(HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        return "thymeleaf/contacto";
    }

    @GetMapping("/metricas")
    public String metricas(HttpSession session, Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("ventasPorDia", metricasService.obtenerVentasPorDia());
        model.addAttribute("pedidosCreadosPorDia", metricasService.obtenerPedidosCreadosPorDia());
        model.addAttribute("pedidosEntregadosPorDia", metricasService.obtenerPedidosEntregadosPorDia());
        model.addAttribute("cancelacionesPorDia", metricasService.obtenerCancelacionesPorDia());
        model.addAttribute("ingresosAcumuladosPorDia", metricasService.obtenerIngresosAcumuladosPorDia());
        return "thymeleaf/metricas";
    }

    @GetMapping("/publicidad")
    public String publicidad(HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        return "thymeleaf/publicidad";
    }

}
