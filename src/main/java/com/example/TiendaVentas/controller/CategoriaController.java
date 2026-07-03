package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Categoria;
import com.example.TiendaVentas.service.CategoriaService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controlador para categorias migrado a JPA/H2.
@Controller
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public String listarCategorias(@RequestParam(defaultValue = "") String mensaje,
                                   HttpSession session,
                                   Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/crud_categorias";
    }

    @GetMapping("/categorias/nueva")
    public String nuevaCategoria(@RequestParam(defaultValue = "") String mensaje,
                                 HttpSession session,
                                 Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/agregar_categoria";
    }

    @GetMapping("/categorias/guardar")
    public String guardarCategoria(@RequestParam String nombre,
                                   @RequestParam String descripcion,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        List<String> errores = validarCategoria(nombre, descripcion, null);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/categorias/nueva";
        }

        categoriaService.guardar(new Categoria(0, nombre, descripcion));
        return "redirect:/categorias";
    }

    @GetMapping("/categorias/editar")
    public String editarCategoria(@RequestParam int id,
                                  @RequestParam(defaultValue = "") String mensaje,
                                  HttpSession session,
                                  Model model) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("categoria", categoriaService.buscarPorId(id).orElse(null));
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/editar_categoria";
    }

    @GetMapping("/categorias/actualizar")
    public String actualizarCategoria(@RequestParam int id,
                                      @RequestParam String nombre,
                                      @RequestParam String descripcion,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        List<String> errores = validarCategoria(nombre, descripcion, id);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/categorias/editar?id=" + id;
        }

        Categoria categoria = categoriaService.buscarPorId(id).orElse(null);

        if (categoria != null) {
            categoria.setNombre(nombre);
            categoria.setDescripcion(descripcion);
            categoriaService.guardar(categoria);
        }

        return "redirect:/categorias";
    }

    @PostMapping("/categorias/eliminar")
    public String eliminarCategoria(@RequestParam int id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        categoriaService.ocultar(id);

        redirectAttributes.addAttribute("mensaje", "La categoria fue ocultada correctamente.");
        return "redirect:/categorias";
    }

    @PostMapping("/categorias/reactivar")
    public String reactivarCategoria(@RequestParam int id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!UsuarioController.adminLogueado(session)) {
            return "redirect:/login";
        }

        categoriaService.reactivar(id);

        redirectAttributes.addAttribute("mensaje", "La categoria fue reactivada correctamente.");
        return "redirect:/categorias";
    }

    private List<String> validarCategoria(String nombre, String descripcion, Integer idActual) {
        List<String> errores = new ArrayList<>();

        if (nombre == null || nombre.trim().isEmpty()) {
            errores.add("El nombre de la categoria es obligatorio.");
        } else if (idActual == null ? categoriaService.existeNombre(nombre) : categoriaService.existeNombreEnOtroId(nombre, idActual)) {
            errores.add("Ya existe una categoria con ese nombre.");
        }

        if (descripcion == null || descripcion.trim().isEmpty()) {
            errores.add("La descripcion de la categoria es obligatoria.");
        }

        return errores;
    }
}
