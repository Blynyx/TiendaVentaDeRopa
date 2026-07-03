package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.service.DeliveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/delivery/pedidos")
    public String pedidosAsignados(HttpSession session, Model model) {
        Usuario repartidor = UsuarioController.getRepartidorActual(session);

        if (repartidor == null) {
            return "redirect:/login";
        }

        model.addAttribute("repartidor", repartidor);
        model.addAttribute("pedidos", deliveryService.buscarPedidosActivosPorRepartidor(repartidor.getId()));
        return "thymeleaf/delivery_pedidos";
    }

    @GetMapping("/delivery/pedidos/{id}")
    public String detallePedido(@PathVariable int id,
                                @RequestParam(defaultValue = "") String mensaje,
                                HttpSession session,
                                Model model) {
        Usuario repartidor = UsuarioController.getRepartidorActual(session);

        if (repartidor == null) {
            return "redirect:/login";
        }

        Pedido pedido = deliveryService.buscarPedidoGestionable(id, repartidor.getId()).orElse(null);

        if (pedido == null) {
            return "redirect:/delivery/pedidos";
        }

        model.addAttribute("repartidor", repartidor);
        model.addAttribute("pedido", pedido);
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("motivosCancelacionRepartidor", deliveryService.getMotivosCancelacionRepartidor());
        return "thymeleaf/delivery_detalle_pedido";
    }

    @PostMapping("/delivery/pedidos/{id}/estado")
    public String actualizarEstado(@PathVariable int id,
                                   @RequestParam String estado,
                                   @RequestParam(defaultValue = "") String motivo,
                                   @RequestParam(defaultValue = "") String detalleMotivo,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Usuario repartidor = UsuarioController.getRepartidorActual(session);

        if (repartidor == null) {
            return "redirect:/login";
        }

        Pedido pedido = deliveryService.buscarPedidoGestionable(id, repartidor.getId()).orElse(null);

        if (pedido == null || !deliveryService.transicionValida(pedido, estado)) {
            return "redirect:/delivery/pedidos";
        }

        if (Pedido.CANCELADO.equals(estado)) {
            String errorMotivo = deliveryService.validarMotivoCancelacion(motivo, detalleMotivo);

            if (errorMotivo != null) {
                redirectAttributes.addAttribute("mensaje", errorMotivo);
                return "redirect:/delivery/pedidos/" + id;
            }
        }

        deliveryService.actualizarEstado(id, repartidor.getId(), estado, motivo, detalleMotivo);
        return "redirect:/delivery/pedidos";
    }

    @GetMapping("/delivery/historial")
    public String historial(HttpSession session, Model model) {
        Usuario repartidor = UsuarioController.getRepartidorActual(session);

        if (repartidor == null) {
            return "redirect:/login";
        }

        model.addAttribute("repartidor", repartidor);
        model.addAttribute("pedidos", deliveryService.buscarHistorialPorRepartidor(repartidor.getId()));
        return "thymeleaf/delivery_historial";
    }
}
