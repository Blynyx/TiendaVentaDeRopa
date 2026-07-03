package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.ItemCarrito;
import com.example.TiendaVentas.model.Producto;
import com.example.TiendaVentas.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Controlador del carrito: agrega, muestra y valida cantidades contra el stock.
@Controller
public class CarritoController {

    private final ProductoService productoService;

    public CarritoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/carrito")
    public String verCarrito(@RequestParam(defaultValue = "") String mensaje,
                             HttpSession session,
                             Model model) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        List<ItemCarrito> carrito = obtenerCarrito(session);
        String mensajeStock = validarStockDisponible(carrito);
        model.addAttribute("items", carrito);
        model.addAttribute("total", calcularTotal(carrito));
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("stockDisponible", mensajeStock == null);
        return "thymeleaf/carrito";
    }

    @GetMapping("/carrito/agregar")
    public String agregarAlCarrito(@RequestParam int productoId,
                                   @RequestParam(defaultValue = "1") int cantidad,
                                   HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        Optional<Producto> productoActual = productoService.buscarPorId(productoId);

        if (productoActual.isEmpty() || cantidad <= 0) {
            return "redirect:/catalogo?mensaje=Producto no valido";
        }

        Producto producto = productoActual.get();

        if (!productoVisible(producto)) {
            return "redirect:/catalogo?mensaje=Producto no disponible";
        }

        if (producto.getStock() == 0) {
            return "redirect:/catalogo?mensaje=Producto agotado";
        }

        List<ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito itemExistente = buscarItem(carrito, productoId);
        int cantidadActual = itemExistente == null ? 0 : itemExistente.getCantidad();

        if (cantidadActual + cantidad > producto.getStock()) {
            return "redirect:/catalogo?mensaje=No hay stock suficiente";
        }

        if (itemExistente == null) {
            carrito.add(new ItemCarrito(producto, cantidad));
        } else {
            itemExistente.setCantidad(itemExistente.getCantidad() + cantidad);
        }

        return "redirect:/carrito";
    }

    @GetMapping("/carrito/aumentar")
    public String aumentarCantidad(@RequestParam int productoId, HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        List<ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito item = buscarItem(carrito, productoId);
        Optional<Producto> productoActual = productoService.buscarPorId(productoId);

        if (item != null && productoActual.isPresent() && productoVisible(productoActual.get())
                && item.getCantidad() < productoActual.get().getStock()) {
            item.setProducto(productoActual.get());
            item.setCantidad(item.getCantidad() + 1);
        } else {
            return "redirect:/carrito?mensaje=No hay mas stock disponible";
        }

        return "redirect:/carrito";
    }

    @GetMapping("/carrito/disminuir")
    public String disminuirCantidad(@RequestParam int productoId, HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        List<ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito item = buscarItem(carrito, productoId);

        if (item != null) {
            if (item.getCantidad() <= 1) {
                carrito.remove(item);
            } else {
                item.setCantidad(item.getCantidad() - 1);
            }
        }

        return "redirect:/carrito";
    }

    @GetMapping("/carrito/eliminar")
    public String eliminarDelCarrito(@RequestParam int productoId, HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        obtenerCarrito(session).removeIf(item -> item.getProducto().getId() == productoId);
        return "redirect:/carrito";
    }

    @GetMapping("/carrito/vaciar")
    public String vaciarCarrito(HttpSession session) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        obtenerCarrito(session).clear();
        return "redirect:/carrito";
    }

    @SuppressWarnings("unchecked")
    public static List<ItemCarrito> obtenerCarrito(HttpSession session) {
        List<ItemCarrito> carrito = (List<ItemCarrito>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito);
        }

        return carrito;
    }

    public static double calcularTotal(List<ItemCarrito> carrito) {
        double total = 0;

        for (ItemCarrito item : carrito) {
            total += item.getSubtotal();
        }

        return total;
    }

    private ItemCarrito buscarItem(List<ItemCarrito> carrito, int productoId) {
        for (ItemCarrito item : carrito) {
            if (item.getProducto().getId() == productoId) {
                return item;
            }
        }

        return null;
    }

    private String validarStockDisponible(List<ItemCarrito> carrito) {
        if (carrito == null || carrito.isEmpty()) {
            return "Tu carrito esta vacio";
        }

        StringBuilder mensaje = new StringBuilder();

        for (ItemCarrito item : carrito) {
            Optional<Producto> productoActual = productoService.buscarPorId(item.getProducto().getId());

            if (productoActual.isEmpty() || item.getCantidad() <= 0 || !productoVisible(productoActual.get())) {
                return "Ya no existe stock suficiente para completar la compra. Actualice su carrito para continuar.";
            }

            Producto producto = productoActual.get();
            item.setProducto(producto);

            if (item.getCantidad() > producto.getStock()) {
                if (mensaje.length() == 0) {
                    mensaje.append("Ya no existe stock suficiente para completar la compra. ");
                } else {
                    mensaje.append(" ");
                }

                if (producto.getStock() == 0) {
                    mensaje.append("El producto ")
                            .append(producto.getNombre())
                            .append(" ya no tiene stock disponible. Disponible: 0 unidades.");
                } else {
                    mensaje.append("Producto: ")
                            .append(producto.getNombre())
                            .append(". Cantidad solicitada: ")
                            .append(item.getCantidad())
                            .append(". Cantidad disponible: ")
                            .append(producto.getStock())
                            .append(".");
                }
            }
        }

        if (mensaje.length() > 0) {
            mensaje.append(" Actualice su carrito para continuar.");
            return mensaje.toString();
        }

        return null;
    }

    private boolean productoVisible(Producto producto) {
        return producto != null
                && producto.isActivo()
                && producto.getCategoriaEntidad() != null
                && producto.getCategoriaEntidad().isActiva();
    }
}
