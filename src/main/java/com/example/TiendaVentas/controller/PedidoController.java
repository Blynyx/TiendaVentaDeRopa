package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.ItemCarrito;
import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.service.PedidoService;
import com.example.TiendaVentas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controlador de pedidos: compra, mis compras, seguimiento y gestion admin.
@Controller
public class PedidoController {

    private static final String CHECKOUT_TOKEN = "checkoutToken";
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PedidoController(PedidoService pedidoService, UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(defaultValue = "") String mensaje,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        List<ItemCarrito> carrito = CarritoController.obtenerCarrito(session);
        if (pedidoService.actualizarPreciosCarrito(carrito) && mensaje.isEmpty()) {
            mensaje = "Algunos precios cambiaron mientras realizabas tu compra. El carrito fue actualizado.";
        }

        String mensajeVisibilidad = pedidoService.validarProductosVisibles(carrito);

        if (mensajeVisibilidad != null) {
            redirectAttributes.addAttribute("mensaje", mensajeVisibilidad);
            return "redirect:/carrito";
        }

        String mensajeStock = pedidoService.validarStockDisponible(carrito);

        if (mensajeStock != null) {
            redirectAttributes.addAttribute("mensaje", mensajeStock);
            return "redirect:/carrito";
        }

        double subtotal = pedidoService.redondear(CarritoController.calcularTotal(carrito));
        int descuentoPorcentaje = pedidoService.calcularPorcentajeDescuento(subtotal);
        double montoDescuento = pedidoService.calcularMontoDescuento(subtotal);
        double costoNormal = pedidoService.calcularCostoDelivery(Pedido.ENVIO_NORMAL, subtotal);
        double costoExpress = pedidoService.calcularCostoDelivery(Pedido.ENVIO_EXPRESS, subtotal);
        model.addAttribute("items", carrito);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("descuentoPorcentaje", descuentoPorcentaje);
        model.addAttribute("montoDescuento", montoDescuento);
        model.addAttribute("subtotalConDescuento", pedidoService.redondear(subtotal - montoDescuento));
        model.addAttribute("costoNormal", costoNormal);
        model.addAttribute("costoExpress", costoExpress);
        model.addAttribute("promocionNormal", pedidoService.obtenerPromocionDelivery(Pedido.ENVIO_NORMAL, subtotal));
        model.addAttribute("promocionExpress", pedidoService.obtenerPromocionDelivery(Pedido.ENVIO_EXPRESS, subtotal));
        model.addAttribute("totalNormal", pedidoService.calcularTotalPedido(Pedido.ENVIO_NORMAL, subtotal));
        model.addAttribute("totalExpress", pedidoService.calcularTotalPedido(Pedido.ENVIO_EXPRESS, subtotal));
        model.addAttribute("total", pedidoService.calcularTotalPedido(Pedido.ENVIO_NORMAL, subtotal));
        model.addAttribute("mensajesPromocion", pedidoService.generarMensajesPromocion(subtotal));
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("envioNormal", Pedido.ENVIO_NORMAL);
        model.addAttribute("envioExpress", Pedido.ENVIO_EXPRESS);
        model.addAttribute("pagoContraEntrega", Pedido.PAGO_CONTRA_ENTREGA);
        model.addAttribute("pagoTarjeta", Pedido.PAGO_TARJETA);
        String checkoutToken = UUID.randomUUID().toString();
        session.setAttribute(CHECKOUT_TOKEN, checkoutToken);
        model.addAttribute("checkoutToken", checkoutToken);
        return "thymeleaf/checkout";
    }

    @PostMapping("/pedidos/crear")
    public String crearPedido(@RequestParam String direccion,
                              @RequestParam String tipoEnvio,
                              @RequestParam(defaultValue = "") String metodoPago,
                              @RequestParam(defaultValue = "") String numeroTarjeta,
                              @RequestParam(defaultValue = "") String fechaVencimiento,
                              @RequestParam(defaultValue = "") String cvv,
                              @RequestParam String celularCliente,
                              @RequestParam String dniDestinatario,
                              @RequestParam String nombreDestinatario,
                              @RequestParam String referenciaDireccion,
                              @RequestParam(defaultValue = "") String checkoutToken,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        List<String> errores = validarDatosCheckout(direccion, celularCliente, dniDestinatario,
                nombreDestinatario, referenciaDireccion, metodoPago, numeroTarjeta, cvv);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/checkout";
        }

        if (!datosDeliveryValidos(direccion, celularCliente, dniDestinatario, nombreDestinatario, referenciaDireccion)) {
            return "redirect:/checkout?mensaje=Completa los datos obligatorios con celular y DNI numericos";
        }

        if (!metodoPagoValido(metodoPago)) {
            return "redirect:/checkout?mensaje=Selecciona un metodo de pago valido";
        }

        if (!numeroTarjetaValido(metodoPago, numeroTarjeta)) {
            return "redirect:/checkout?mensaje=El número de tarjeta debe tener 16 dígitos.";
        }

        if (!cvvValido(metodoPago, cvv)) {
            return "redirect:/checkout?mensaje=El CVV debe tener 3 digitos.";
        }

        if (tarjetaVencida(metodoPago, fechaVencimiento)) {
            return "redirect:/checkout?mensaje=La tarjeta está vencida.";
        }

        List<ItemCarrito> carrito = CarritoController.obtenerCarrito(session);

        synchronized (session) {
            String tokenSesion = (String) session.getAttribute(CHECKOUT_TOKEN);

            if (tokenSesion == null || !tokenSesion.equals(checkoutToken)) {
                redirectAttributes.addAttribute("mensaje", "El pedido ya fue procesado o la sesion de checkout vencio.");
                return "redirect:/carrito";
            }

            if (carrito.isEmpty()) {
                return "redirect:/carrito?mensaje=Tu carrito esta vacio";
            }

            if (pedidoService.actualizarPreciosCarrito(carrito)) {
                redirectAttributes.addAttribute("mensaje", "Algunos precios cambiaron mientras realizabas tu compra. El carrito fue actualizado.");
                return "redirect:/checkout";
            }

            String mensajeVisibilidad = pedidoService.validarProductosVisibles(carrito);

            if (mensajeVisibilidad != null) {
                redirectAttributes.addAttribute("mensaje", mensajeVisibilidad);
                return "redirect:/carrito";
            }

            String mensajeStock = pedidoService.validarStockDisponible(carrito);

            if (mensajeStock != null) {
                redirectAttributes.addAttribute("mensaje", mensajeStock);
                return "redirect:/carrito";
            }

            session.removeAttribute(CHECKOUT_TOKEN);
            Pedido pedido = pedidoService.crearPedidoSiStockDisponible(
                    usuario,
                    carrito,
                    direccion,
                    tipoEnvio,
                    metodoPago,
                    celularCliente,
                    dniDestinatario,
                    nombreDestinatario,
                    referenciaDireccion
            );

            if (pedido == null) {
                redirectAttributes.addAttribute("mensaje", "Ya no existe stock suficiente para completar la compra. Actualice su carrito para continuar.");
                return "redirect:/carrito";
            }

            carrito.clear();
            return "redirect:/pedido/detalle?id=" + pedido.getId();
        }
    }

    @GetMapping("/mis-compras")
    public String misCompras(@RequestParam(defaultValue = "") String mensaje,
                             HttpSession session,
                             Model model) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        model.addAttribute("pedidos", pedidoService.buscarPorUsuario(usuario.getId()));
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("motivosCancelacionCliente", pedidoService.getMotivosCancelacionCliente());
        return "thymeleaf/mis_compras";
    }

    @PostMapping("/pedido/cancelar")
    public String cancelarPedidoCliente(@RequestParam int id,
                                        @RequestParam(defaultValue = "") String motivo,
                                        @RequestParam(defaultValue = "") String detalleMotivo,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        if (pedido == null || pedido.getUsuario().getId() != usuario.getId()) {
            return "redirect:/mis-compras";
        }

        if (!pedido.isCancelablePorCliente()) {
            redirectAttributes.addAttribute("mensaje", "El pedido no puede cancelarse en su estado actual.");
            return "redirect:/mis-compras";
        }

        String errorMotivo = pedidoService.validarMotivoCancelacion(
                motivo,
                detalleMotivo,
                pedidoService.getMotivosCancelacionCliente()
        );

        if (errorMotivo != null) {
            redirectAttributes.addAttribute("mensaje", errorMotivo);
            return "redirect:/mis-compras";
        }

        pedidoService.cancelarPedido(pedido, false, "Cliente", usuario.getNombre(),
                pedidoService.construirMotivoCancelacion(motivo, detalleMotivo));
        redirectAttributes.addAttribute("mensaje", "Pedido cancelado correctamente.");
        return "redirect:/mis-compras";
    }

    @GetMapping("/pedido/editar-datos")
    public String editarDatosPedido(@RequestParam int id, HttpSession session, Model model) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        if (pedido == null || pedido.getUsuario().getId() != usuario.getId()) {
            return "redirect:/mis-compras";
        }

        if (!Pedido.PENDIENTE.equals(pedido.getEstado())) {
            return "redirect:/mis-compras?mensaje=Solo puede editar datos de pedidos pendientes.";
        }

        model.addAttribute("pedido", pedido);
        return "thymeleaf/editar_pedido_datos";
    }

    @GetMapping("/pedido/actualizar-datos")
    public String actualizarDatosPedido(@RequestParam int id,
                                        @RequestParam String direccion,
                                        @RequestParam String celularCliente,
                                        @RequestParam String nombreDestinatario,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (usuario == null || !UsuarioController.esCliente(usuario)) {
            return "redirect:/login";
        }

        if (pedido == null || pedido.getUsuario().getId() != usuario.getId()) {
            return "redirect:/mis-compras";
        }

        if (!Pedido.PENDIENTE.equals(pedido.getEstado())) {
            redirectAttributes.addAttribute("mensaje", "El pedido ya fue asignado y no puede modificarse.");
            return "redirect:/mis-compras";
        }

        if (!tieneTexto(direccion) || !tieneTexto(nombreDestinatario)
                || celularCliente == null || !celularCliente.matches("\\d{9}")) {
            redirectAttributes.addAttribute("mensaje", "Complete direccion, destinatario y telefono valido.");
            return "redirect:/pedido/editar-datos?id=" + id;
        }

        pedido.setDireccion(direccion);
        pedido.setCelularCliente(celularCliente);
        pedido.setNombreDestinatario(nombreDestinatario);
        pedidoService.guardar(pedido);
        redirectAttributes.addAttribute("mensaje", "Datos del pedido actualizados correctamente.");
        return "redirect:/pedido/detalle?id=" + id;
    }

    @GetMapping("/pedido/detalle")
    public String detallePedido(@RequestParam int id, HttpSession session, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (!puedeVerPedido(session, pedido)) {
            return "redirect:/login";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("admin", UsuarioController.adminLogueado(session));
        model.addAttribute("delivery", UsuarioController.repartidorLogueado(session));
        model.addAttribute("motivosCancelacionAdmin", pedidoService.getMotivosCancelacionAdministrador());
        return "thymeleaf/detalle_pedido";
    }

    @GetMapping("/admin/pedidos/{id}")
    public String detallePedidoAdmin(@PathVariable int id, HttpSession session, Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (pedido == null) {
            return "redirect:/pedidos";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("admin", true);
        model.addAttribute("motivosCancelacionAdmin", pedidoService.getMotivosCancelacionAdministrador());
        return "thymeleaf/detalle_pedido";
    }

    @GetMapping("/pedido/seguimiento")
    public String seguimientoPedido(@RequestParam int id, HttpSession session, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (!puedeVerPedido(session, pedido)) {
            return "redirect:/login";
        }

        model.addAttribute("pedido", pedido);
        return "thymeleaf/seguimiento";
    }

    @GetMapping("/pedido/estado")
    public String redirigirSeguimientoAntiguo(HttpSession session) {
        if (UsuarioController.adminLogueado(session)) {
            return "redirect:/pedidos";
        }

        return "redirect:/mis-compras";
    }

    @GetMapping({"/pedidos", "/admin/pedidos"})
    public String listarPedidos(@RequestParam(defaultValue = "") String mensaje,
                                HttpSession session,
                                Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("pedidos", pedidoService.listarOrdenadosPorPrioridad());
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("motivosCancelacionAdmin", pedidoService.getMotivosCancelacionAdministrador());
        return "thymeleaf/pedidos";
    }

    @GetMapping({"/pedido/asignar", "/admin/pedidos/{id}/asignar"})
    public String formularioAsignar(@RequestParam(required = false) Integer pedidoId,
                                    @RequestParam(defaultValue = "") String mensaje,
                                    @PathVariable(name = "id", required = false) Integer id,
                                    HttpSession session,
                                    Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        if (pedidoId == null && id == null) {
            return "redirect:/pedidos";
        }

        int idPedido = pedidoId != null ? pedidoId : id;
        Pedido pedido = pedidoService.buscarPorId(idPedido).orElse(null);

        if (pedido == null) {
            return "redirect:/pedidos";
        }

        if (pedido.isFinalizado()) {
            return "redirect:/pedidos?mensaje=No se pueden asignar pedidos cancelados o entregados.";
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("repartidores", usuarioService.listarRepartidores());
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/asignar_pedido";
    }

    @PostMapping("/pedido/asignar/guardar")
    public String asignarPedido(@RequestParam int pedidoId,
                                @RequestParam int repartidorId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        Pedido pedido = pedidoService.buscarPorId(pedidoId).orElse(null);
        Usuario repartidor = usuarioService.buscarPorId(repartidorId)
                .filter(Usuario::isRepartidor)
                .orElse(null);

        if (pedido == null || repartidor == null) {
            redirectAttributes.addAttribute("mensaje", "No se pudo asignar el pedido.");
            return "redirect:/pedidos";
        }

        if (pedido.isFinalizado()) {
            redirectAttributes.addAttribute("mensaje", "No se pueden asignar pedidos cancelados o entregados.");
            return "redirect:/pedidos";
        }

        if (pedido.isNoEditable()) {
            String mensaje = Pedido.ASIGNADO.equals(pedido.getEstado())
                    ? "El pedido ya fue asignado y no puede modificarse."
                    : "No se pueden editar pedidos en proceso de entrega.";
            redirectAttributes.addAttribute("mensaje", mensaje);
            return "redirect:/pedidos";
        }

        if (pedido.getRepartidor() != null) {
            redirectAttributes.addAttribute("pedidoId", pedidoId);
            redirectAttributes.addAttribute("mensaje", "Debe quitar la asignación actual antes de asignar otro repartidor.");
            return "redirect:/pedido/asignar";
        }

        if (mismoDni(pedido.getUsuario().getDni(), repartidor.getDni())) {
            redirectAttributes.addAttribute("pedidoId", pedidoId);
            redirectAttributes.addAttribute("mensaje", "El repartidor no puede entregar un pedido realizado por el mismo DNI.");
            return "redirect:/pedido/asignar";
        }

        if (pedidoService.repartidorAlcanzoLimitePedidosActivos(repartidorId)) {
            redirectAttributes.addAttribute("pedidoId", pedidoId);
            redirectAttributes.addAttribute("mensaje", "El repartidor alcanzó el límite de pedidos activos.");
            return "redirect:/pedido/asignar";
        }

        if (!pedido.isFinalizado()) {
            pedido.setRepartidor(repartidor);

            pedido.setEstado(Pedido.ASIGNADO);
            pedido.setFechaPreparacion(LocalDateTime.now());
            pedidoService.guardar(pedido);
            redirectAttributes.addAttribute("mensaje", "Pedido asignado correctamente.");
            return "redirect:/pedidos";
        }

        redirectAttributes.addAttribute("mensaje", "No se pueden editar pedidos en proceso de entrega.");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedido/desasignar")
    public String desasignarPedido(@RequestParam int pedidoId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        Pedido pedido = pedidoService.buscarPorId(pedidoId).orElse(null);

        if (pedido == null || pedido.getRepartidor() == null) {
            redirectAttributes.addAttribute("mensaje", "No se pudo quitar la asignacion del pedido.");
            return "redirect:/pedidos";
        }

        if (!pedido.isDesasignable()) {
            String mensaje = Pedido.EN_CAMINO.equals(pedido.getEstado())
                    ? "No se pueden editar pedidos en proceso de entrega."
                    : "Solo se pueden desasignar pedidos en estado Asignado.";
            redirectAttributes.addAttribute("mensaje", mensaje);
            return "redirect:/pedidos";
        }

        pedido.setRepartidor(null);
        pedido.setEstado(Pedido.PENDIENTE);
        pedido.setFechaPreparacion(null);
        pedido.setFechaCamino(null);
        pedido.setFechaEntregado(null);
        pedido.setFechaCancelado(null);
        pedido.setCanceladoPor(null);
        pedido.setCanceladoPorNombre(null);
        pedido.setMotivoCancelacion(null);
        pedidoService.guardar(pedido);

        redirectAttributes.addAttribute("mensaje", "Asignación eliminada correctamente.");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedido/cancelar/admin")
    public String cancelarPedidoAdmin(@RequestParam int id,
                                      @RequestParam(defaultValue = "") String motivo,
                                      @RequestParam(defaultValue = "") String detalleMotivo,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Usuario admin = UsuarioController.getUsuarioActual(session);
        Pedido pedido = pedidoService.buscarPorId(id).orElse(null);

        if (admin == null || !UsuarioController.esAdmin(admin)) {
            return "redirect:/login";
        }

        if (pedido == null) {
            return "redirect:/pedidos";
        }

        if (!cancelablePorAdministrador(pedido)) {
            redirectAttributes.addAttribute("mensaje", "El pedido no puede cancelarse en su estado actual.");
            return "redirect:/pedidos";
        }

        String errorMotivo = pedidoService.validarMotivoCancelacion(
                motivo,
                detalleMotivo,
                pedidoService.getMotivosCancelacionAdministrador()
        );

        if (errorMotivo != null) {
            redirectAttributes.addAttribute("mensaje", errorMotivo);
            return "redirect:/pedidos";
        }

        pedidoService.cancelarPedido(pedido, false, "Administrador", admin.getNombre(),
                pedidoService.construirMotivoCancelacion(motivo, detalleMotivo));
        redirectAttributes.addAttribute("mensaje", "Pedido cancelado correctamente.");
        return "redirect:/pedidos";
    }

    private boolean puedeVerPedido(HttpSession session, Pedido pedido) {
        Usuario usuario = UsuarioController.getUsuarioActual(session);

        if (pedido == null) {
            return false;
        }

        if (usuario != null && UsuarioController.esAdmin(usuario)) {
            return true;
        }

        if (usuario != null && UsuarioController.esCliente(usuario)) {
            return pedido.getUsuario().getId() == usuario.getId();
        }

        return usuario != null
                && UsuarioController.esRepartidor(usuario)
                && pedido.getRepartidor() != null
                && pedido.getRepartidor().getId() == usuario.getId();
    }

    private boolean datosDeliveryValidos(String direccion, String celular, String dni, String destinatario, String referencia) {
        return tieneTexto(direccion)
                && tieneTexto(destinatario)
                && tieneTexto(referencia)
                && celular != null
                && celular.matches("\\d{9}")
                && dni != null
                && dni.matches("\\d{8}");
    }


    private List<String> validarDatosCheckout(String direccion, String celular, String dni, String destinatario,
                                              String referencia, String metodoPago, String numeroTarjeta, String cvv) {
        List<String> errores = new ArrayList<>();

        if (!tieneTexto(direccion)) {
            errores.add("La direccion de entrega es obligatoria.");
        }

        if (!tieneTexto(referencia)) {
            errores.add("La referencia de direccion es obligatoria.");
        }

        if (!tieneTexto(destinatario)) {
            errores.add("El nombre del destinatario es obligatorio.");
        }

        if (dni == null || !dni.matches("\\d{8}")) {
            errores.add("DNI debe contener exactamente 8 digitos numericos.");
        }

        if (celular == null || !celular.matches("\\d{9}")) {
            errores.add("Telefono debe contener exactamente 9 digitos numericos.");
        }

        if (!metodoPagoValido(metodoPago)) {
            errores.add("Selecciona un metodo de pago valido.");
        }

        if (!numeroTarjetaValido(metodoPago, numeroTarjeta)) {
            errores.add("El numero de tarjeta debe tener 16 digitos.");
        }

        if (!cvvValido(metodoPago, cvv)) {
            errores.add("El CVV debe tener 3 digitos.");
        }

        return errores;
    }
    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }


    private boolean mismoDni(String dniUsuario, String dniRepartidor) {
        return tieneTexto(dniUsuario)
                && tieneTexto(dniRepartidor)
                && dniUsuario.equals(dniRepartidor);
    }
    private boolean metodoPagoValido(String metodoPago) {
        return Pedido.PAGO_CONTRA_ENTREGA.equalsIgnoreCase(metodoPago)
                || Pedido.PAGO_TARJETA.equalsIgnoreCase(metodoPago);
    }

    private boolean cancelablePorAdministrador(Pedido pedido) {
        return Pedido.PENDIENTE.equals(pedido.getEstado())
                || Pedido.ASIGNADO.equals(pedido.getEstado())
                || Pedido.EN_CAMINO.equals(pedido.getEstado());
    }

    private boolean tarjetaVencida(String metodoPago, String fechaVencimiento) {
        if (!Pedido.PAGO_TARJETA.equalsIgnoreCase(metodoPago)) {
            return false;
        }

        try {
            YearMonth vencimiento = YearMonth.parse(fechaVencimiento);
            return vencimiento.isBefore(YearMonth.now());
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private boolean numeroTarjetaValido(String metodoPago, String numeroTarjeta) {
        if (!Pedido.PAGO_TARJETA.equalsIgnoreCase(metodoPago)) {
            return true;
        }

        return numeroTarjeta != null && numeroTarjeta.matches("\\d{16}");
    }

    private boolean cvvValido(String metodoPago, String cvv) {
        if (!Pedido.PAGO_TARJETA.equalsIgnoreCase(metodoPago)) {
            return true;
        }

        return cvv != null && cvv.matches("\\d{3}");
    }
}
