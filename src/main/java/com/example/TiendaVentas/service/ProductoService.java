package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.Categoria;
import com.example.TiendaVentas.model.Producto;
import com.example.TiendaVentas.repository.CategoriaRepository;
import com.example.TiendaVentas.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarVisibles() {
        return productoRepository.findProductosVisibles();
    }

    public Optional<Producto> buscarPorId(int id) {
        return productoRepository.findById(id);
    }

    public Optional<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreIgnoreCase(nombre);
    }

    public Producto guardar(Producto producto) {
        if (producto.getCategoriaEntidad() != null && producto.getCategoriaEntidad().getId() == 0) {
            return guardarConCategoria(producto, producto.getCategoria());
        }

        return productoRepository.save(producto);
    }

    public Producto guardarConCategoria(Producto producto, String nombreCategoria) {
        Categoria categoria = categoriaRepository.findByNombreIgnoreCase(nombreCategoria)
                .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada: " + nombreCategoria));
        producto.setCategoria(categoria);
        return productoRepository.save(producto);
    }

    public boolean existeNombre(String nombre) {
        return productoRepository.existsByNombreIgnoreCase(nombre);
    }

    public boolean existeNombreEnOtroId(String nombre, int id) {
        return productoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
    }

    public void desactivar(int id) {
        productoRepository.findById(id).ifPresent(producto -> {
            producto.setActivo(false);
            productoRepository.save(producto);
        });
    }

    public boolean activar(int id) {
        return productoRepository.findById(id).map(producto -> {
            if (!producto.isCategoriaActiva()) {
                return false;
            }

            producto.setActivo(true);
            productoRepository.save(producto);
            return true;
        }).orElse(false);
    }
}
