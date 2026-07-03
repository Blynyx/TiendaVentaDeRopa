package com.example.TiendaVentas.repository;

import com.example.TiendaVentas.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, int id);

    Optional<Usuario> findByDniAndRol(String dni, String rol);

    boolean existsByDniAndRol(String dni, String rol);

    boolean existsByDniAndRolAndIdNot(String dni, String rol, int id);

    List<Usuario> findByRol(String rol);
}
