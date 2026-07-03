package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(int id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo);
    }

    public Optional<Usuario> buscarPorDniYRol(String dni, String rol) {
        return usuarioRepository.findByDniAndRol(dni, Usuario.normalizarRol(rol));
    }

    public List<Usuario> listarRepartidores() {
        return usuarioRepository.findByRol(Usuario.ROL_REPARTIDOR);
    }

    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void eliminarPorId(int id) {
        usuarioRepository.deleteById(id);
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreoIgnoreCase(correo);
    }

    public boolean existeCorreoEnOtroId(String correo, int id) {
        return usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id);
    }

    public boolean existeDniPorRol(String dni, String rol) {
        return usuarioRepository.existsByDniAndRol(dni, Usuario.normalizarRol(rol));
    }

    public boolean existeDniPorRolEnOtroId(String dni, String rol, int id) {
        return usuarioRepository.existsByDniAndRolAndIdNot(dni, Usuario.normalizarRol(rol), id);
    }

    public boolean existePorId(int id) {
        return usuarioRepository.existsById(id);
    }
}
