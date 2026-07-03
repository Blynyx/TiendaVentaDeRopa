package com.example.TiendaVentas.repository;

import com.example.TiendaVentas.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, int id);

    @Query("select p from Producto p where p.activo = true and p.categoria.activa = true")
    List<Producto> findProductosVisibles();

    @Query("""
            select p from Producto p
            where p.activo = true
              and p.categoria.activa = true
              and lower(p.nombre) like lower(concat('%', :busqueda, '%'))
            """)
    List<Producto> findProductosVisiblesPorNombre(@Param("busqueda") String busqueda);

    @Query("""
            select p from Producto p
            where p.activo = true
              and p.categoria.activa = true
              and lower(p.categoria.nombre) = lower(:categoria)
            """)
    List<Producto> findProductosVisiblesPorCategoria(@Param("categoria") String categoria);
}
