package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Producto;
import com.example.TiendaVentas.service.CategoriaService;
import com.example.TiendaVentas.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controlador de productos y catalogo migrado a JPA/H2.
@Controller
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/productos")
    public String listarProductos(@RequestParam(defaultValue = "") String mensaje,
                                  HttpSession session,
                                  Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/crud_productos";
    }

    @GetMapping("/productos/nuevo")
    public String nuevoProducto(@RequestParam(defaultValue = "") String mensaje,
                                HttpSession session,
                                Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/agregar_producto";
    }

    @GetMapping("/productos/guardar")
    public String guardarProducto(@RequestParam String nombre,
                                  @RequestParam(defaultValue = "") String descripcion,
                                  @RequestParam double precio,
                                  @RequestParam String categoria,
                                  @RequestParam(defaultValue = "10") int stock,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        List<String> errores = validarProducto(nombre, precio, stock, null);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/productos/nuevo";
        }

        Producto producto = new Producto(
                0,
                nombre,
                descripcion,
                precio,
                categoria,
                stock,
                obtenerIniciales(nombre)
        );

        productoService.guardarConCategoria(producto, categoria);
        return "redirect:/productos";
    }

    @GetMapping("/productos/editar")
    public String editarProducto(@RequestParam int id,
                                 @RequestParam(defaultValue = "") String mensaje,
                                 HttpSession session,
                                 Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("producto", productoService.buscarPorId(id).orElse(null));
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/editar_producto";
    }

    @GetMapping("/productos/actualizar")
    public String actualizarProducto(@RequestParam int id,
                                     @RequestParam String nombre,
                                     @RequestParam(defaultValue = "") String descripcion,
                                     @RequestParam double precio,
                                     @RequestParam String categoria,
                                     @RequestParam(defaultValue = "0") int stock,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        List<String> errores = validarProducto(nombre, precio, stock, id);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/productos/editar?id=" + id;
        }

        Producto producto = productoService.buscarPorId(id).orElse(null);

        if (producto != null) {
            producto.setNombre(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecio(precio);
            producto.setStock(stock);
            producto.setImagen(obtenerIniciales(nombre));
            productoService.guardarConCategoria(producto, categoria);
        }

        return "redirect:/productos";
    }

    @PostMapping("/productos/eliminar")
    public String eliminarProducto(@RequestParam int id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        productoService.desactivar(id);

        redirectAttributes.addAttribute("mensaje", "El producto fue desactivado correctamente.");
        return "redirect:/productos";
    }

    @PostMapping("/productos/reactivar")
    public String reactivarProducto(@RequestParam int id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        if (!productoService.activar(id)) {
            redirectAttributes.addAttribute("mensaje", "No se puede activar un producto porque su categoría está desactivada.");
            return "redirect:/productos";
        }

        redirectAttributes.addAttribute("mensaje", "El producto fue activado correctamente.");
        return "redirect:/productos";
    }

    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(defaultValue = "") String busqueda,
                           @RequestParam(defaultValue = "Todas") String categoria,
                           @RequestParam(defaultValue = "") String mensaje,
                           HttpSession session,
                           Model model) {
        if (!UsuarioController.clienteLogueado(session)) {
            return "redirect:/login";
        }

        if (!categoria.equals("Todas") && !categoriaService.categoriaActiva(categoria)) {
            categoria = "Todas";
        }

        model.addAttribute("productos", filtrarProductos(busqueda, categoria));
        model.addAttribute("categorias", categoriaService.listarActivas());
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/catalogo";
    }

    private List<Producto> filtrarProductos(String busqueda, String categoria) {
        List<Producto> resultado = new ArrayList<>();

        for (Producto producto : productoService.listarVisibles()) {
            boolean coincideNombre = producto.getNombre().toLowerCase().contains(busqueda.toLowerCase());
            boolean coincideCategoria = categoria.equals("Todas")
                    || (producto.getCategoria() != null && producto.getCategoria().equalsIgnoreCase(categoria));

            if (coincideNombre && coincideCategoria) {
                resultado.add(producto);
            }
        }

        return resultado;
    }

    private String obtenerIniciales(String texto) {
        String limpio = texto.trim();

        if (limpio.length() >= 2) {
            return limpio.substring(0, 2).toUpperCase();
        }

        return limpio.toUpperCase();
    }

    private String validarPrecioYStock(double precio, int stock) {
        if (precio <= 0) {
            return "El precio debe ser mayor a 0.";
        }

        if (stock < 0) {
            return "El stock no puede ser negativo.";
        }

        return "";
    }

    private List<String> validarProducto(String nombre, double precio, int stock, Integer idActual) {
        List<String> errores = new ArrayList<>();

        if (nombre == null || nombre.trim().isEmpty()) {
            errores.add("El nombre del producto es obligatorio.");
        } else if (idActual == null ? productoService.existeNombre(nombre) : productoService.existeNombreEnOtroId(nombre, idActual)) {
            errores.add("Ya existe un producto con ese nombre.");
        }

        String mensajePrecioStock = validarPrecioYStock(precio, stock);

        if (!mensajePrecioStock.isEmpty()) {
            errores.add(mensajePrecioStock);
        }

        return errores;
    }
}
