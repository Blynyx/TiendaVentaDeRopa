package com.example.TiendaVentas.config;

import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.model.PedidoItem;
import com.example.TiendaVentas.model.Producto;
import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.repository.PedidoRepository;
import com.example.TiendaVentas.repository.ProductoRepository;
import com.example.TiendaVentas.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PedidoSeedDataInitializer implements CommandLineRunner {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoSeedDataInitializer(PedidoRepository pedidoRepository,
                                     ProductoRepository productoRepository,
                                     UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.count() > 2) {
            return;
        }

        Map<Integer, Producto> productos = cargarProductos();
        Usuario maria = usuarioRepository.findById(2).orElse(null);
        Usuario luis = usuarioRepository.findById(5).orElse(null);
        Usuario rosa = usuarioRepository.findById(6).orElse(null);
        Usuario carlos = usuarioRepository.findById(3).orElse(null);
        Usuario ana = usuarioRepository.findById(4).orElse(null);

        if (productos.size() < 8 || maria == null || luis == null || rosa == null || carlos == null || ana == null) {
            return;
        }

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        List<Pedido> pedidos = new ArrayList<>();
        pedidos.add(crearPedido(maria, carlos, productos, base, 42, 10, Pedido.ENTREGADO, Pedido.ENVIO_NORMAL, Pedido.PAGO_TARJETA, new int[][]{{1, 1}, {6, 1}}));
        pedidos.add(crearPedido(luis, ana, productos, base, 35, 16, Pedido.CANCELADO, Pedido.ENVIO_EXPRESS, Pedido.PAGO_TARJETA, new int[][]{{5, 1}}));
        pedidos.add(crearPedido(rosa, carlos, productos, base, 28, 11, Pedido.ENTREGADO, Pedido.ENVIO_NORMAL, Pedido.PAGO_CONTRA_ENTREGA, new int[][]{{2, 2}, {6, 1}}));
        pedidos.add(crearPedido(maria, ana, productos, base, 21, 18, Pedido.ENTREGADO, Pedido.ENVIO_EXPRESS, Pedido.PAGO_TARJETA, new int[][]{{7, 1}, {8, 1}}));
        pedidos.add(crearPedido(luis, carlos, productos, base, 14, 9, Pedido.EN_CAMINO, Pedido.ENVIO_NORMAL, Pedido.PAGO_TARJETA, new int[][]{{3, 1}, {1, 1}}));
        pedidos.add(crearPedido(rosa, ana, productos, base, 7, 15, Pedido.PENDIENTE, Pedido.ENVIO_NORMAL, Pedido.PAGO_CONTRA_ENTREGA, new int[][]{{4, 1}}));
        pedidos.add(crearPedido(maria, carlos, productos, base, 2, 20, Pedido.ENTREGADO, Pedido.ENVIO_EXPRESS, Pedido.PAGO_TARJETA, new int[][]{{5, 2}, {6, 1}}));

        pedidoRepository.saveAll(pedidos);
    }

    private Map<Integer, Producto> cargarProductos() {
        Map<Integer, Producto> productos = new HashMap<>();

        for (Producto producto : productoRepository.findAll()) {
            productos.put(producto.getId(), producto);
        }

        return productos;
    }

    private Pedido crearPedido(Usuario cliente, Usuario repartidor, Map<Integer, Producto> productos,
                               LocalDateTime base, int diasAtras, int hora, String estado,
                               String tipoEnvio, String metodoPago, int[][] detalle) {
        Pedido pedido = new Pedido();
        LocalDateTime fechaCompra = base.minusDays(diasAtras).withHour(hora);
        List<PedidoItem> items = crearItems(productos, detalle);
        double subtotal = calcularSubtotal(items);
        int descuentoPorcentaje = calcularDescuento(subtotal);
        double montoDescuento = redondear(subtotal * descuentoPorcentaje / 100);
        double costoDelivery = calcularDelivery(subtotal, tipoEnvio);
        double total = redondear(subtotal - montoDescuento + costoDelivery);

        pedido.setUsuario(cliente);
        pedido.setRepartidor(Pedido.PENDIENTE.equals(estado) ? null : repartidor);
        pedido.setDireccion("Av. Demo " + (100 + diasAtras));
        pedido.setTipoEnvio(tipoEnvio);
        pedido.setCostoDelivery(costoDelivery);
        pedido.setMetodoPago(metodoPago);
        pedido.setEstado(estado);
        pedido.setEstadoPago(calcularEstadoPago(estado, metodoPago));
        pedido.setDescuentoPorcentaje(descuentoPorcentaje);
        pedido.setMontoDescuento(montoDescuento);
        pedido.setPromocionDeliveryAplicada(costoDelivery == 0 ? "Delivery gratis" : "Tarifa regular");
        pedido.setDescripcionDescuentoAplicado(descuentoPorcentaje > 0
                ? "Descuento " + descuentoPorcentaje + "% por compra mayor a S/" + metaDescuento(descuentoPorcentaje)
                : "Sin descuento aplicado");
        pedido.setCelularCliente(cliente.getTelefono());
        pedido.setDniDestinatario(cliente.getDni());
        pedido.setNombreDestinatario(cliente.getNombreCompleto());
        pedido.setReferenciaDireccion("Referencia demo " + diasAtras);
        pedido.setSubtotalProductos(subtotal);
        pedido.setTotal(total);
        pedido.setFechaCompra(fechaCompra);
        pedido.setPedidoItems(items);

        if (Pedido.ENTREGADO.equals(estado)) {
            pedido.setFechaPreparacion(fechaCompra.plusHours(1));
            pedido.setFechaCamino(fechaCompra.plusHours(3));
            pedido.setFechaEntregado(fechaCompra.plusHours(6));
        } else if (Pedido.EN_CAMINO.equals(estado)) {
            pedido.setFechaPreparacion(fechaCompra.plusHours(1));
            pedido.setFechaCamino(fechaCompra.plusHours(3));
        } else if (Pedido.CANCELADO.equals(estado)) {
            pedido.setFechaCancelado(fechaCompra.plusHours(2));
            pedido.setCanceladoPor("Cliente");
            pedido.setCanceladoPorNombre(cliente.getNombreCompleto());
            pedido.setMotivoCancelacion(diasAtras % 2 == 0
                    ? "Me equivoque al realizar el pedido"
                    : "Ya no deseo el producto");
        }

        return pedido;
    }

    private List<PedidoItem> crearItems(Map<Integer, Producto> productos, int[][] detalle) {
        List<PedidoItem> items = new ArrayList<>();

        for (int[] linea : detalle) {
            Producto producto = productos.get(linea[0]);
            int cantidad = linea[1];
            PedidoItem item = new PedidoItem(producto, cantidad, producto.getPrecio());
            items.add(item);
        }

        return items;
    }

    private double calcularSubtotal(List<PedidoItem> items) {
        double subtotal = 0;

        for (PedidoItem item : items) {
            subtotal += item.getSubtotal();
        }

        return redondear(subtotal);
    }

    private int calcularDescuento(double subtotal) {
        if (subtotal >= 800) {
            return 20;
        }

        if (subtotal >= 500) {
            return 15;
        }

        if (subtotal >= 300) {
            return 10;
        }

        if (subtotal >= 150) {
            return 5;
        }

        return 0;
    }

    private int metaDescuento(int descuentoPorcentaje) {
        if (descuentoPorcentaje >= 20) {
            return 800;
        }

        if (descuentoPorcentaje >= 15) {
            return 500;
        }

        if (descuentoPorcentaje >= 10) {
            return 300;
        }

        return 150;
    }

    private double calcularDelivery(double subtotal, String tipoEnvio) {
        if (subtotal >= 300) {
            return 0;
        }

        if (subtotal >= 150) {
            return Pedido.ENVIO_EXPRESS.equals(tipoEnvio) ? 5 : 0;
        }

        return Pedido.ENVIO_EXPRESS.equals(tipoEnvio) ? 10 : 5;
    }

    private String calcularEstadoPago(String estado, String metodoPago) {
        if (Pedido.CANCELADO.equals(estado)) {
            return Pedido.PAGO_TARJETA.equals(metodoPago) ? Pedido.PAGO_REEMBOLSADO : Pedido.PAGO_ANULADO;
        }

        return Pedido.PAGO_TARJETA.equals(metodoPago) ? Pedido.PAGO_PAGADO : Pedido.PAGO_PENDIENTE;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
