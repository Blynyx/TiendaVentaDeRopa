package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.Categoria;
import com.example.TiendaVentas.repository.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivaTrue();
    }

    public Optional<Categoria> buscarPorId(int id) {
        return categoriaRepository.findById(id);
    }

    public Optional<Categoria> buscarPorNombre(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre);
    }

    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public boolean existeNombre(String nombre) {
        return categoriaRepository.existsByNombreIgnoreCase(nombre);
    }

    public boolean existeNombreEnOtroId(String nombre, int id) {
        return categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
    }

    public void ocultar(int id) {
        categoriaRepository.findById(id).ifPresent(categoria -> {
            categoria.setActiva(false);
            categoriaRepository.save(categoria);
        });
    }

    public void reactivar(int id) {
        categoriaRepository.findById(id).ifPresent(categoria -> {
            categoria.setActiva(true);
            categoriaRepository.save(categoria);
        });
    }

    public boolean categoriaActiva(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre)
                .map(Categoria::isActiva)
                .orElse(false);
    }
}
