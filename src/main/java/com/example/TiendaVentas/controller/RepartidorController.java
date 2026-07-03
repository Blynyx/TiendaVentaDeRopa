package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.service.PedidoService;
import com.example.TiendaVentas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RepartidorController {

    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;

    public RepartidorController(UsuarioService usuarioService, PedidoService pedidoService) {
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
    }

    @GetMapping({"/repartidores", "/admin/repartidores"})
    public String listar() {
        return "redirect:/usuarios";
    }

    @GetMapping({"/repartidores/nuevo", "/admin/repartidores/nuevo"})
    public String nuevo() {
        return "redirect:/usuarios/nuevo";
    }

    @GetMapping("/repartidores/guardar")
    public String guardar() {
        return "redirect:/usuarios/nuevo";
    }

    @GetMapping({"/repartidores/editar", "/admin/repartidores/editar/{id}"})
    public String editar(@RequestParam(required = false) Integer repartidorId,
                         @PathVariable(name = "id", required = false) Integer id) {
        Integer idFinal = repartidorId != null ? repartidorId : id;
        return idFinal == null ? "redirect:/usuarios" : "redirect:/usuarios/editar?id=" + idFinal;
    }

    @GetMapping("/repartidores/actualizar")
    public String actualizar() {
        return "redirect:/usuarios";
    }

    @PostMapping({"/repartidores/eliminar", "/admin/repartidores/eliminar/{id}"})
    public String eliminar(@RequestParam(required = false) Integer repartidorId,
                           @PathVariable(name = "id", required = false) Integer id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        Integer idFinal = repartidorId != null ? repartidorId : id;

        if (idFinal == null) {
            return "redirect:/usuarios";
        }

        Usuario usuario = usuarioService.buscarPorId(idFinal).orElse(null);

        if (usuario == null || !usuario.isRepartidor()) {
            return "redirect:/usuarios";
        }

        if (pedidoService.repartidorTienePedidosActivos(idFinal)) {
            redirectAttributes.addAttribute("mensaje", "No se puede eliminar el repartidor porque tiene pedidos activos asignados.");
            return "redirect:/usuarios";
        }

        usuarioService.eliminarPorId(idFinal);
        UsuarioController.cerrarSesionesUsuario(idFinal);
        return "redirect:/usuarios";
    }

    @GetMapping("/admin/repartidores/{id}/pedidos")
    public String pedidosAsignados(@PathVariable int id, HttpSession session, Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        Usuario repartidor = usuarioService.buscarPorId(id)
                .filter(Usuario::isRepartidor)
                .orElse(null);

        if (repartidor == null) {
            return "redirect:/usuarios";
        }

        model.addAttribute("repartidor", repartidor);
        model.addAttribute("pedidos", pedidoService.buscarPorRepartidor(id));
        return "thymeleaf/repartidor_pedidos";
    }
}
