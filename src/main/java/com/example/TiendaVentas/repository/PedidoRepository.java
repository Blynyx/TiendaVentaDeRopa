package com.example.TiendaVentas.repository;

import com.example.TiendaVentas.model.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByUsuarioId(int usuarioId);

    List<Pedido> findByRepartidorId(int repartidorId);

    List<Pedido> findByEstado(String estado);
}
