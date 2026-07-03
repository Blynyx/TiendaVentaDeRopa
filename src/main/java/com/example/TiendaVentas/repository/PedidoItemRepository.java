package com.example.TiendaVentas.repository;

import com.example.TiendaVentas.model.PedidoItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoItemRepository extends JpaRepository<PedidoItem, Integer> {

    List<PedidoItem> findByPedidoId(int pedidoId);
}
