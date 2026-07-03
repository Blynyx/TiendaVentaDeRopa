package com.example.TiendaVentas.controller;

import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.service.PedidoService;
import com.example.TiendaVentas.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controlador de usuarios: registro, login con sesion y CRUD administrativo.
@Controller
public class UsuarioController {

    private static final String USUARIO_ACTUAL = "usuarioActual";
    private static final Map<Integer, List<HttpSession>> SESIONES_USUARIO = new HashMap<>();
    private static UsuarioService usuarioService;
    private final PedidoService pedidoService;

    public UsuarioController(UsuarioService usuarioService, PedidoService pedidoService) {
        UsuarioController.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        Usuario usuario = getUsuarioActual(session);

        if (usuario != null) {
            if (esAdmin(usuario)) {
                return "redirect:/gestion";
            }

            if (esRepartidor(usuario)) {
                return "redirect:/delivery/pedidos";
            }

            return "redirect:/principal";
        }

        model.addAttribute("mensaje", "");
        return "thymeleaf/login";
    }

    @GetMapping("/login/ingresar")
    public String ingresar(@RequestParam String correo,
                           @RequestParam(defaultValue = "") String password,
                           HttpSession session,
                           Model model) {
        Usuario usuario = usuarioService.buscarPorCorreo(correo).orElse(null);

        if (usuario != null && usuario.getPassword().equals(password)) {
            session.setAttribute(USUARIO_ACTUAL, usuario);
            registrarSesionUsuario(usuario.getId(), session);

            if (esAdmin(usuario)) {
                return "redirect:/gestion";
            }

            if (esRepartidor(usuario)) {
                return "redirect:/delivery/pedidos";
            }

            return "redirect:/principal";
        }

        model.addAttribute("mensaje", "Correo o contrasena incorrectos");
        return "thymeleaf/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        quitarSesion(session);
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/registro")
    public String registro(@RequestParam(defaultValue = "") String mensaje, Model model) {
        model.addAttribute("mensaje", mensaje);
        return "thymeleaf/registro";
    }

    @GetMapping("/registro/guardar")
    public String guardarRegistro(@RequestParam String nombre,
                                  @RequestParam(defaultValue = "") String apellido,
                                  @RequestParam String correo,
                                  @RequestParam String dni,
                                  @RequestParam(defaultValue = "") String telefono,
                                  @RequestParam String password,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        List<String> errores = validarUsuario(nombre, apellido, correo, dni, telefono, null, Usuario.ROL_CLIENTE);

        if (!errores.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", errores));
            return "redirect:/registro";
        }


        if (!dniValido(dni)) {
            redirectAttributes.addAttribute("mensaje", "DNI debe contener solo números y tener longitud válida.");
            return "redirect:/registro";
        }

        Usuario usuario = new Usuario(0, nombre, apellido, correo, dni,
                telefono, password, Usuario.ROL_CLIENTE);
        usuario = usuarioService.guardar(usuario);
        session.setAttribute(USUARIO_ACTUAL, usuario);
        registrarSesionUsuario(usuario.getId(), session);
        return "redirect:/principal";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        Usuario usuario = getUsuarioActual(session);

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        return "thymeleaf/perfil";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "") String mensaje,
                                 @RequestParam(defaultValue = "TODOS") String rol,
                                 HttpSession session,
                                 Model model) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        String rolSolicitado = rol == null || rol.trim().isEmpty() ? "TODOS" : rol.trim();
        String rolFiltro = Usuario.normalizarRol(rolSolicitado);
        List<Usuario> usuarios = usuarioService.listarTodos();

        if (!"TODOS".equalsIgnoreCase(rolSolicitado)) {
            usuarios = usuarios.stream()
                    .filter(usuario -> usuario.getRol().equalsIgnoreCase(rolFiltro))
                    .toList();
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("rolFiltro", "TODOS".equalsIgnoreCase(rolSolicitado) ? "TODOS" : rolFiltro);
        return "thymeleaf/crud_usuarios";
    }

    @GetMapping("/usuarios/nuevo")
    public String nuevoUsuario(@RequestParam(defaultValue = "") String mensaje,
                               HttpSession session,
                               Model model) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("mensaje", mensaje);
        model.addAttribute("roles", rolesPermitidos());
        return "thymeleaf/agregar_usuario";
    }

    @GetMapping("/usuarios/guardar")
    public String guardarUsuario(@RequestParam String nombre,
                                 @RequestParam(defaultValue = "") String apellido,
                                 @RequestParam String correo,
                                 @RequestParam String dni,
                                 @RequestParam(defaultValue = "") String telefono,
                                 @RequestParam String password,
                                 @RequestParam String rol,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        rol = Usuario.normalizarRol(rol);
        List<String> erroresUsuarioAdmin = validarUsuario(nombre, apellido, correo, dni, telefono, null, rol);

        if (!erroresUsuarioAdmin.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", erroresUsuarioAdmin));
            return "redirect:/usuarios/nuevo";
        }

        if (!dniValido(dni)) {
            redirectAttributes.addAttribute("mensaje", "DNI debe contener solo números y tener longitud válida.");
            return "redirect:/usuarios/nuevo";
        }

        usuarioService.guardar(new Usuario(0, nombre, apellido, correo, dni, telefono, password, rol));
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/editar")
    public String editarUsuario(@RequestParam int id,
                                @RequestParam(defaultValue = "") String mensaje,
                                HttpSession session,
                                Model model) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuarioService.buscarPorId(id).orElse(null));
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("roles", rolesPermitidos());
        return "thymeleaf/editar_usuario";
    }

    @GetMapping("/usuarios/actualizar")
    public String actualizarUsuario(@RequestParam int id,
                                    @RequestParam String nombre,
                                    @RequestParam(defaultValue = "") String apellido,
                                    @RequestParam String correo,
                                    @RequestParam String dni,
                                    @RequestParam(defaultValue = "") String telefono,
                                    @RequestParam String password,
                                    @RequestParam String rol,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        Usuario usuarioActual = getUsuarioActual(session);
        Usuario usuario = usuarioService.buscarPorId(id).orElse(null);
        rol = Usuario.normalizarRol(rol);
        List<String> erroresActualizacion = validarUsuario(nombre, apellido, correo, dni, telefono, id, rol);

        if (usuarioActual != null && usuarioActual.getId() == id
                && usuario != null && !usuario.getRol().equalsIgnoreCase(rol)) {
            erroresActualizacion.add("No puede modificar su propio rol.");
        }

        if (!erroresActualizacion.isEmpty()) {
            redirectAttributes.addAttribute("mensaje", String.join(" | ", erroresActualizacion));
            return "redirect:/usuarios/editar?id=" + id;
        }

        if (!dniValido(dni)) {
            redirectAttributes.addAttribute("mensaje", "DNI debe contener solo números y tener longitud válida.");
            return "redirect:/usuarios/editar?id=" + id;
        }

        if (usuario != null) {
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setCorreo(correo);
            usuario.setDni(dni);
            usuario.setTelefono(telefono);
            usuario.setPassword(password);
            usuario.setRol(rol);
            usuarioService.guardar(usuario);
        }

        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam int id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (!adminLogueado(session)) {
            return "redirect:/login";
        }

        Usuario usuarioActual = getUsuarioActual(session);

        if (usuarioActual != null && usuarioActual.getId() == id) {
            redirectAttributes.addAttribute("mensaje", "No puede eliminar su propia cuenta. Solicite a otro administrador que realice esta acción.");
            return "redirect:/usuarios";
        }

        Usuario usuarioEliminar = usuarioService.buscarPorId(id).orElse(null);

        if (usuarioEliminar != null && usuarioEliminar.isAdministradorPrincipal()) {
            redirectAttributes.addAttribute("mensaje", "No se puede eliminar el administrador principal.");
            return "redirect:/usuarios";
        }

        if (pedidoService.usuarioTienePedidosActivos(id)) {
            redirectAttributes.addAttribute("mensaje", "No se puede eliminar el usuario porque tiene pedidos activos.");
            return "redirect:/usuarios";
        }

        if (usuarioEliminar != null && usuarioEliminar.isRepartidor()
                && pedidoService.repartidorTienePedidosActivos(id)) {
            redirectAttributes.addAttribute("mensaje", "No se puede eliminar el repartidor porque tiene pedidos activos asignados.");
            return "redirect:/usuarios";
        }

        usuarioService.eliminarPorId(id);
        cerrarSesionesUsuario(id);

        if (usuarioActual != null && usuarioActual.getId() == id) {
            return "redirect:/login";
        }

        return "redirect:/usuarios";
    }

    public static Usuario getUsuarioActual(HttpSession session) {
        Usuario usuario;

        try {
            usuario = (Usuario) session.getAttribute(USUARIO_ACTUAL);
        } catch (IllegalStateException e) {
            return null;
        }

        if (usuario == null) {
            return null;
        }

        if (usuarioService == null) {
            return usuario;
        }

        if (!usuarioService.existePorId(usuario.getId())) {
            quitarSesion(session);
            session.invalidate();
            return null;
        }

        return usuarioService.buscarPorId(usuario.getId()).orElse(usuario);
    }

    public static Usuario getRepartidorActual(HttpSession session) {
        Usuario usuario = getUsuarioActual(session);
        return usuario != null && esRepartidor(usuario) ? usuario : null;
    }

    public static boolean adminLogueado(HttpSession session) {
        Usuario usuario = getUsuarioActual(session);
        return usuario != null && esAdmin(usuario);
    }

    public static boolean clienteLogueado(HttpSession session) {
        Usuario usuario = getUsuarioActual(session);
        return usuario != null && esCliente(usuario);
    }

    public static boolean repartidorLogueado(HttpSession session) {
        return getRepartidorActual(session) != null;
    }

    public static boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.isAdministrador();
    }

    public static boolean esCliente(Usuario usuario) {
        return usuario != null && usuario.isCliente();
    }

    public static boolean esRepartidor(Usuario usuario) {
        return usuario != null && usuario.isRepartidor();
    }

    private boolean dniValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }


    private boolean telefonoValido(String telefono) {
        return telefono != null && telefono.matches("\\d{9}");
    }

    private List<String> validarUsuario(String nombre, String apellido, String correo, String dni,
                                        String telefono, Integer idActual, String rol) {
        List<String> errores = new ArrayList<>();

        if (nombre == null || nombre.trim().isEmpty()) {
            errores.add("El nombre es obligatorio.");
        }

        if (apellido == null || apellido.trim().isEmpty()) {
            errores.add("El apellido es obligatorio.");
        }

        if (correo == null || correo.trim().isEmpty()) {
            errores.add("El correo es obligatorio.");
        } else if (idActual == null ? usuarioService.existeCorreo(correo) : usuarioService.existeCorreoEnOtroId(correo, idActual)) {
            errores.add(mensajeCorreoDuplicado(rol));
        }

        if (!dniValido(dni)) {
            errores.add("DNI debe contener exactamente 8 digitos numericos.");
        } else if (idActual == null
                ? usuarioService.existeDniPorRol(dni, rol)
                : usuarioService.existeDniPorRolEnOtroId(dni, rol, idActual)) {
            errores.add(mensajeDniDuplicado(rol));
        }

        if (!telefonoValido(telefono)) {
            errores.add("Telefono debe contener exactamente 9 digitos numericos.");
        }

        if (!rolValido(rol)) {
            errores.add("Rol no valido.");
        }

        return errores;
    }

    private boolean rolValido(String rol) {
        return Usuario.ROL_CLIENTE.equals(rol)
                || Usuario.ROL_ADMINISTRADOR.equals(rol)
                || Usuario.ROL_REPARTIDOR.equals(rol);
    }

    private List<String> rolesPermitidos() {
        List<String> roles = new ArrayList<>();
        roles.add(Usuario.ROL_CLIENTE);
        roles.add(Usuario.ROL_ADMINISTRADOR);
        roles.add(Usuario.ROL_REPARTIDOR);
        return roles;
    }

    private String mensajeCorreoDuplicado(String rol) {
        if (Usuario.ROL_ADMINISTRADOR.equalsIgnoreCase(rol)) {
            return "Ya existe un administrador con ese correo.";
        }

        if (Usuario.ROL_REPARTIDOR.equalsIgnoreCase(rol)) {
            return "Ya existe un repartidor con ese correo.";
        }

        return "Ya existe un usuario con ese correo.";
    }

    private String mensajeDniDuplicado(String rol) {
        if (Usuario.ROL_ADMINISTRADOR.equalsIgnoreCase(rol)) {
            return "Ya existe un administrador con ese DNI.";
        }

        if (Usuario.ROL_REPARTIDOR.equalsIgnoreCase(rol)) {
            return "Ya existe un repartidor con ese DNI.";
        }

        return "Ya existe un usuario con ese DNI.";
    }

    public static void cerrarSesionesUsuario(int usuarioId) {
        cerrarSesiones(SESIONES_USUARIO.remove(usuarioId));
    }

    private static void registrarSesionUsuario(int usuarioId, HttpSession session) {
        quitarSesion(session);
        SESIONES_USUARIO.computeIfAbsent(usuarioId, id -> new ArrayList<>()).add(session);
    }

    private static void cerrarSesiones(List<HttpSession> sesiones) {
        if (sesiones == null) {
            return;
        }

        for (HttpSession sesion : sesiones) {
            try {
                sesion.invalidate();
            } catch (IllegalStateException e) {
                // La sesion ya estaba cerrada.
            }
        }
    }

    private static void quitarSesion(HttpSession session) {
        quitarSesionDeMapa(session, SESIONES_USUARIO);
    }

    private static void quitarSesionDeMapa(HttpSession session, Map<Integer, List<HttpSession>> sesionesPorCuenta) {
        for (List<HttpSession> sesiones : sesionesPorCuenta.values()) {
            sesiones.remove(session);
        }
    }
}
