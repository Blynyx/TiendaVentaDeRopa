package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.ItemCarrito;
import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.model.Producto;
import com.example.TiendaVentas.model.Usuario;
import com.example.TiendaVentas.repository.PedidoRepository;
import com.example.TiendaVentas.repository.ProductoRepository;
import com.example.TiendaVentas.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    public static final int LIMITE_PEDIDOS_ACTIVOS_REPARTIDOR = 5;
    public static final double COSTO_DELIVERY_NORMAL = 5;
    public static final double COSTO_DELIVERY_EXPRESS = 10;
    public static final double MONTO_ENVIO_NORMAL_GRATIS = 150;
    public static final double MONTO_ENVIO_EXPRESS_REBAJADO = 150;
    public static final double MONTO_ENVIO_EXPRESS_GRATIS = 300;
    public static final String MOTIVO_OTRO = "Otro";

    private static final List<String> MOTIVOS_CANCELACION_CLIENTE = Arrays.asList(
            "Ya no deseo el producto",
            "Me equivoque al realizar el pedido",
            "Encontre otra alternativa",
            "Tiempo de entrega muy largo",
            "Problemas con el pago",
            MOTIVO_OTRO
    );
    private static final List<String> MOTIVOS_CANCELACION_REPARTIDOR = Arrays.asList(
            "Cliente no responde",
            "Direccion incorrecta",
            "Pedido no localizable",
            "Problema de transporte",
            "Problema de seguridad en la entrega",
            MOTIVO_OTRO
    );
    private static final List<String> MOTIVOS_CANCELACION_ADMINISTRADOR = Arrays.asList(
            "Producto sin stock",
            "Error operativo",
            "Solicitud del cliente",
            "Incumplimiento de politicas",
            "Error de registro del pedido",
            MOTIVO_OTRO
    );

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Pedido> buscarPorId(int id) {
        return pedidoRepository.findById(id);
    }

    public List<Pedido> listarOrdenadosPorPrioridad() {
        return ordenarPedidosPorPrioridad(pedidoRepository.findAll());
    }

    public List<Pedido> buscarPorUsuario(int usuarioId) {
        return ordenarPedidosPorPrioridad(pedidoRepository.findByUsuarioId(usuarioId));
    }

    public List<Pedido> buscarPorRepartidor(int repartidorId) {
        return ordenarPedidosPorPrioridad(pedidoRepository.findByRepartidorId(repartidorId));
    }

    public List<String> getMotivosCancelacionCliente() {
        return MOTIVOS_CANCELACION_CLIENTE;
    }

    public List<String> getMotivosCancelacionRepartidor() {
        return MOTIVOS_CANCELACION_REPARTIDOR;
    }

    public List<String> getMotivosCancelacionAdministrador() {
        return MOTIVOS_CANCELACION_ADMINISTRADOR;
    }

    @Transactional
    public boolean actualizarPreciosCarrito(List<ItemCarrito> items) {
        if (items == null) {
            return false;
        }

        boolean cambio = false;

        for (ItemCarrito item : items) {
            Optional<Producto> productoActual = buscarProductoActual(item);

            if (productoActual.isEmpty()) {
                continue;
            }

            Producto producto = productoActual.get();

            if (Double.compare(item.getPrecioUnitario(), producto.getPrecio()) != 0) {
                item.setPrecioUnitario(producto.getPrecio());
                cambio = true;
            }

            item.setProducto(producto);
        }

        return cambio;
    }

    public String validarProductosVisibles(List<ItemCarrito> items) {
        if (items == null || items.isEmpty()) {
            return "Tu carrito esta vacio";
        }

        for (ItemCarrito item : items) {
            Optional<Producto> productoActual = buscarProductoActual(item);

            if (productoActual.isEmpty() || !productoVisible(productoActual.get())) {
                String nombreProducto = item.getProducto() == null ? "Producto no disponible" : item.getProducto().getNombre();
                return "El producto " + nombreProducto + " ya no esta disponible en el catalogo. Actualice su carrito para continuar.";
            }
        }

        return null;
    }

    public synchronized String validarStockDisponible(List<ItemCarrito> items) {
        if (items == null || items.isEmpty()) {
            return "Tu carrito esta vacio";
        }

        StringBuilder mensaje = new StringBuilder();

        for (ItemCarrito item : items) {
            Optional<Producto> productoActual = buscarProductoActual(item);

            if (productoActual.isEmpty() || item.getCantidad() <= 0) {
                return "Ya no existe stock suficiente para completar la compra. Actualice su carrito para continuar.";
            }

            Producto producto = productoActual.get();
            item.setProducto(producto);

            if (item.getCantidad() > producto.getStock()) {
                if (mensaje.length() == 0) {
                    mensaje.append("Ya no existe stock suficiente para completar la compra. ");
                } else {
                    mensaje.append(" ");
                }

                if (producto.getStock() == 0) {
                    mensaje.append("El producto ")
                            .append(producto.getNombre())
                            .append(" ya no tiene stock disponible. Disponible: 0 unidades.");
                } else {
                    mensaje.append("Producto: ")
                            .append(producto.getNombre())
                            .append(". Cantidad solicitada: ")
                            .append(item.getCantidad())
                            .append(". Cantidad disponible: ")
                            .append(producto.getStock())
                            .append(".");
                }
            }
        }

        if (mensaje.length() > 0) {
            mensaje.append(" Actualice su carrito para continuar.");
            return mensaje.toString();
        }

        return null;
    }

    @Transactional
    public synchronized Pedido crearPedidoSiStockDisponible(Usuario usuario, List<ItemCarrito> items,
                                                            String direccion, String tipoEnvio,
                                                            String metodoPago, String celularCliente,
                                                            String dniDestinatario,
                                                            String nombreDestinatario,
                                                            String referenciaDireccion) {
        String mensajeStock = validarStockDisponible(items);

        if (mensajeStock != null) {
            return null;
        }

        List<ItemCarrito> copiaItems = new ArrayList<>();

        for (ItemCarrito item : items) {
            Producto producto = productoRepository.findById(item.getProducto().getId()).orElse(null);

            if (producto == null || item.getCantidad() > producto.getStock()) {
                return null;
            }

            ItemCarrito copia = new ItemCarrito(producto, item.getCantidad());
            copia.setPrecioUnitario(producto.getPrecio());
            copiaItems.add(copia);
            producto.setStock(producto.getStock() - item.getCantidad());
        }

        double subtotal = redondear(calcularSubtotalItems(copiaItems));
        double costoDelivery = calcularCostoDelivery(tipoEnvio, subtotal);
        int descuentoPorcentaje = calcularPorcentajeDescuento(subtotal);
        double montoDescuento = calcularMontoDescuento(subtotal);

        Usuario usuarioPedido = usuarioRepository.findById(usuario.getId()).orElse(usuario);

        Pedido pedido = new Pedido(
                0,
                usuarioPedido,
                copiaItems,
                direccion,
                tipoEnvio,
                costoDelivery,
                metodoPago,
                Pedido.PENDIENTE,
                calcularTotalPedido(tipoEnvio, subtotal),
                descuentoPorcentaje,
                montoDescuento,
                celularCliente,
                dniDestinatario,
                nombreDestinatario,
                referenciaDireccion
        );
        pedido.setSubtotalProductos(subtotal);
        pedido.setPromocionDeliveryAplicada(obtenerPromocionDelivery(tipoEnvio, subtotal));
        pedido.setDescripcionDescuentoAplicado(obtenerDescripcionDescuentoAplicado(subtotal));
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void guardar(Pedido pedido) {
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void cancelarPedido(Pedido pedido, boolean quitarRepartidor, String canceladoPor,
                               String canceladoPorNombre, String motivoCancelacion) {
        if (pedido == null || Pedido.CANCELADO.equals(pedido.getEstado())) {
            return;
        }

        restaurarStockPedido(pedido);
        pedido.setEstado(Pedido.CANCELADO);
        pedido.marcarPagoReembolsadoSiCorresponde();
        pedido.anularPagoContraEntregaSiCorresponde();

        if (quitarRepartidor) {
            pedido.setRepartidor(null);
        }

        pedido.setCanceladoPor(canceladoPor);
        pedido.setCanceladoPorNombre(canceladoPorNombre);
        pedido.setMotivoCancelacion(motivoCancelacion);
        pedido.setFechaCancelado(LocalDateTime.now());
        pedidoRepository.save(pedido);
    }

    @Transactional
    public void restaurarStockPedido(Pedido pedido) {
        if (pedido == null) {
            return;
        }

        pedido.getPedidoItems().forEach(item -> {
            Producto producto = productoRepository.findById(item.getProducto().getId()).orElse(null);

            if (producto != null) {
                producto.setStock(producto.getStock() + item.getCantidad());
            }
        });
    }

    public boolean repartidorAlcanzoLimitePedidosActivos(int repartidorId) {
        return contarPedidosActivosPorRepartidor(repartidorId) >= LIMITE_PEDIDOS_ACTIVOS_REPARTIDOR;
    }

    public int contarPedidosActivosPorRepartidor(int repartidorId) {
        int total = 0;

        for (Pedido pedido : pedidoRepository.findByRepartidorId(repartidorId)) {
            if (pedido.isActivoParaRepartidor()) {
                total++;
            }
        }

        return total;
    }

    public boolean usuarioTienePedidosActivos(int usuarioId) {
        for (Pedido pedido : pedidoRepository.findByUsuarioId(usuarioId)) {
            if (Pedido.PENDIENTE.equals(pedido.getEstado())
                    || Pedido.ASIGNADO.equals(pedido.getEstado())
                    || Pedido.EN_CAMINO.equals(pedido.getEstado())) {
                return true;
            }
        }

        return false;
    }

    public boolean repartidorTienePedidosActivos(int repartidorId) {
        return contarPedidosActivosPorRepartidor(repartidorId) > 0;
    }

    public String validarMotivoCancelacion(String motivo, String detalle, List<String> motivosPermitidos) {
        if (motivo == null || motivo.trim().isEmpty() || !motivosPermitidos.contains(motivo)) {
            return "Debe seleccionar un motivo de cancelacion valido.";
        }

        if (MOTIVO_OTRO.equals(motivo) && (detalle == null || detalle.trim().isEmpty())) {
            return "Debe ingresar el detalle adicional cuando seleccione Otro.";
        }

        return null;
    }

    public String construirMotivoCancelacion(String motivo, String detalle) {
        if (MOTIVO_OTRO.equals(motivo)) {
            return motivo + ": " + detalle.trim();
        }

        if (detalle != null && !detalle.trim().isEmpty()) {
            return motivo + " - " + detalle.trim();
        }

        return motivo;
    }

    public double calcularCostoDelivery(String tipoEnvio, double subtotalProductos) {
        if (Pedido.ENVIO_EXPRESS.equalsIgnoreCase(tipoEnvio)) {
            if (subtotalProductos >= MONTO_ENVIO_EXPRESS_GRATIS) {
                return 0;
            }

            if (subtotalProductos >= MONTO_ENVIO_EXPRESS_REBAJADO) {
                return 5;
            }

            return COSTO_DELIVERY_EXPRESS;
        }

        if (subtotalProductos >= MONTO_ENVIO_NORMAL_GRATIS) {
            return 0;
        }

        return COSTO_DELIVERY_NORMAL;
    }

    public int calcularPorcentajeDescuento(double subtotalProductos) {
        if (subtotalProductos >= 800) {
            return 20;
        }

        if (subtotalProductos >= 500) {
            return 15;
        }

        if (subtotalProductos >= 300) {
            return 10;
        }

        if (subtotalProductos >= 150) {
            return 5;
        }

        return 0;
    }

    public double calcularMontoDescuento(double subtotalProductos) {
        return redondear(subtotalProductos * calcularPorcentajeDescuento(subtotalProductos) / 100);
    }

    public double calcularTotalPedido(String tipoEnvio, double subtotalProductos) {
        return redondear(subtotalProductos - calcularMontoDescuento(subtotalProductos)
                + calcularCostoDelivery(tipoEnvio, subtotalProductos));
    }

    public String obtenerPromocionDelivery(String tipoEnvio, double subtotalProductos) {
        double costo = calcularCostoDelivery(tipoEnvio, subtotalProductos);

        if (costo == 0) {
            return "Delivery gratis";
        }

        if (Pedido.ENVIO_EXPRESS.equalsIgnoreCase(tipoEnvio) && subtotalProductos >= MONTO_ENVIO_EXPRESS_REBAJADO) {
            return "Delivery express rebajado";
        }

        return "Tarifa regular";
    }

    public String obtenerDescripcionDescuentoAplicado(double subtotalProductos) {
        int porcentaje = calcularPorcentajeDescuento(subtotalProductos);

        if (porcentaje == 0) {
            return "Sin descuento aplicado";
        }

        return "Descuento " + porcentaje + "% por compra mayor a S/" + obtenerMetaDescuento(porcentaje);
    }

    public List<String> generarMensajesPromocion(double subtotalProductos) {
        List<String> mensajes = new ArrayList<>();
        agregarMensajeEnvio(mensajes, subtotalProductos, MONTO_ENVIO_NORMAL_GRATIS,
                "Te faltan S/", " para obtener envio normal gratis.",
                "Tu pedido tiene envio normal gratis.");
        agregarMensajeEnvio(mensajes, subtotalProductos, MONTO_ENVIO_EXPRESS_GRATIS,
                "Te faltan S/", " para obtener envio express gratis.",
                "Tu pedido tiene envio express gratis.");
        agregarMensajeDescuento(mensajes, subtotalProductos);
        return mensajes;
    }

    public double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public double calcularSubtotalItems(List<ItemCarrito> items) {
        double total = 0;

        if (items == null) {
            return total;
        }

        for (ItemCarrito item : items) {
            total += item.getSubtotal();
        }

        return total;
    }

    private Optional<Producto> buscarProductoActual(ItemCarrito item) {
        if (item == null || item.getProducto() == null) {
            return Optional.empty();
        }

        return productoRepository.findById(item.getProducto().getId());
    }

    private boolean productoVisible(Producto producto) {
        return producto != null
                && producto.isActivo()
                && producto.getCategoriaEntidad() != null
                && producto.getCategoriaEntidad().isActiva();
    }

    private List<Pedido> ordenarPedidosPorPrioridad(List<Pedido> origen) {
        List<Pedido> resultado = new ArrayList<>(origen);
        resultado.sort(Comparator
                .comparing(Pedido::isExpress).reversed()
                .thenComparing(Pedido::getFechaCompra, Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }

    private void agregarMensajeEnvio(List<String> mensajes, double subtotalProductos, double meta,
                                     String prefijo, String sufijo, String positivo) {
        if (subtotalProductos >= meta) {
            mensajes.add(positivo);
            return;
        }

        mensajes.add(prefijo + redondear(meta - subtotalProductos) + sufijo);
    }

    private void agregarMensajeDescuento(List<String> mensajes, double subtotalProductos) {
        int porcentaje = calcularPorcentajeDescuento(subtotalProductos);

        if (porcentaje > 0) {
            mensajes.add("Tu pedido tiene " + porcentaje + "% de descuento.");
        }

        double siguienteMeta = siguienteMetaDescuento(subtotalProductos);

        if (siguienteMeta > 0) {
            mensajes.add("Te faltan S/" + redondear(siguienteMeta - subtotalProductos)
                    + " para obtener " + calcularPorcentajeDescuento(siguienteMeta) + "% de descuento.");
        }
    }

    private double siguienteMetaDescuento(double subtotalProductos) {
        if (subtotalProductos < 150) {
            return 150;
        }

        if (subtotalProductos < 300) {
            return 300;
        }

        if (subtotalProductos < 500) {
            return 500;
        }

        if (subtotalProductos < 800) {
            return 800;
        }

        return 0;
    }

    private int obtenerMetaDescuento(int porcentaje) {
        if (porcentaje >= 20) {
            return 800;
        }

        if (porcentaje >= 15) {
            return 500;
        }

        if (porcentaje >= 10) {
            return 300;
        }

        if (porcentaje >= 5) {
            return 150;
        }

        return 0;
    }
}
