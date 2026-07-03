package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.repository.PedidoRepository;
import com.example.TiendaVentas.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoService pedidoService;

    public DeliveryService(PedidoRepository pedidoRepository, UsuarioRepository usuarioRepository,
                           PedidoService pedidoService) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoService = pedidoService;
    }

    public List<Pedido> buscarPedidosActivosPorRepartidor(int repartidorId) {
        return ordenarPedidosPorPrioridad(filtrarPorRepartidor(repartidorId, true));
    }

    public List<Pedido> buscarHistorialPorRepartidor(int repartidorId) {
        return ordenarPedidosPorPrioridad(filtrarPorRepartidor(repartidorId, false));
    }

    public Optional<Pedido> buscarPedidoGestionable(int pedidoId, int repartidorId) {
        return pedidoRepository.findById(pedidoId)
                .filter(pedido -> puedeGestionar(repartidorId, pedido));
    }

    public List<String> getMotivosCancelacionRepartidor() {
        return pedidoService.getMotivosCancelacionRepartidor();
    }

    public String validarMotivoCancelacion(String motivo, String detalle) {
        return pedidoService.validarMotivoCancelacion(
                motivo,
                detalle,
                pedidoService.getMotivosCancelacionRepartidor()
        );
    }

    public String construirMotivoCancelacion(String motivo, String detalle) {
        return pedidoService.construirMotivoCancelacion(motivo, detalle);
    }

    @Transactional
    public boolean actualizarEstado(int pedidoId, int repartidorId, String estadoDestino,
                                    String motivo, String detalleMotivo) {
        Optional<Pedido> pedidoGestionable = buscarPedidoGestionable(pedidoId, repartidorId);

        if (pedidoGestionable.isEmpty()) {
            return false;
        }

        Pedido pedido = pedidoGestionable.get();

        if (!transicionValida(pedido, estadoDestino)) {
            return false;
        }

        Usuario repartidor = usuarioRepository.findById(repartidorId).orElse(null);

        if (Pedido.CANCELADO.equals(estadoDestino)) {
            String motivoConstruido = construirMotivoCancelacion(motivo, detalleMotivo);
            pedidoService.cancelarPedido(
                    pedido,
                    false,
                    "Repartidor",
                    repartidor == null ? "" : repartidor.getNombreCompleto(),
                    motivoConstruido
            );
            return true;
        }

        if (Pedido.EN_CAMINO.equals(estadoDestino)) {
            pedido.setEstado(estadoDestino);
            pedido.setFechaCamino(LocalDateTime.now());
            pedidoRepository.save(pedido);
            return true;
        }

        if (Pedido.ENTREGADO.equals(estadoDestino)) {
            pedido.setEstado(estadoDestino);
            pedido.setFechaEntregado(LocalDateTime.now());
            pedidoRepository.save(pedido);
            return true;
        }

        return false;
    }

    public boolean transicionValida(Pedido pedido, String estadoDestino) {
        if (pedido == null || pedido.isFinalizado()) {
            return false;
        }

        if (Pedido.ASIGNADO.equals(pedido.getEstado())) {
            return Pedido.EN_CAMINO.equals(estadoDestino) || Pedido.CANCELADO.equals(estadoDestino);
        }

        if (Pedido.EN_CAMINO.equals(pedido.getEstado())) {
            return Pedido.ENTREGADO.equals(estadoDestino) || Pedido.CANCELADO.equals(estadoDestino);
        }

        return false;
    }

    private boolean puedeGestionar(int repartidorId, Pedido pedido) {
        return pedido != null
                && pedido.getRepartidor() != null
                && pedido.getRepartidor().getId() == repartidorId;
    }

    private List<Pedido> filtrarPorRepartidor(int repartidorId, boolean activos) {
        List<Pedido> resultado = new ArrayList<>();

        for (Pedido pedido : pedidoRepository.findByRepartidorId(repartidorId)) {
            if (activos && pedido.isActivoParaRepartidor()) {
                resultado.add(pedido);
            }

            if (!activos && pedido.isFinalizado()) {
                resultado.add(pedido);
            }
        }

        return resultado;
    }

    private List<Pedido> ordenarPedidosPorPrioridad(List<Pedido> origen) {
        List<Pedido> resultado = new ArrayList<>(origen);
        resultado.sort(Comparator
                .comparing(Pedido::isExpress).reversed()
                .thenComparing(Pedido::getFechaCompra, Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }
}
