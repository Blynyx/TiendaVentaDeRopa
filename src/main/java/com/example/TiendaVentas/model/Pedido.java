package com.example.TiendaVentas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Pedido simple: guarda usuario, productos, delivery, estado y total.
@Entity
@Table(name = "pedidos")
public class Pedido {

    public static final String PENDIENTE = "Pendiente";
    public static final String ASIGNADO = "Asignado";
    public static final String EN_CAMINO = "En camino";
    public static final String ENTREGADO = "Entregado";
    public static final String CANCELADO = "Cancelado";
    public static final String PAGO_PENDIENTE = "Pendiente";
    public static final String PAGO_PAGADO = "Pagado";
    public static final String PAGO_REEMBOLSADO = "Reembolsado";
    public static final String PAGO_ANULADO = "Anulado";
    public static final String ENVIO_NORMAL = "Normal";
    public static final String ENVIO_EXPRESS = "Express";
    public static final String PAGO_CONTRA_ENTREGA = "Contra entrega";
    public static final String PAGO_TARJETA = "Tarjeta";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PedidoItem> items = new ArrayList<>();
    private String direccion;
    private String tipoEnvio;
    private double costoDelivery;
    private String metodoPago;
    private String estadoPago;
    private String estado;
    @ManyToOne
    @JoinColumn(name = "repartidor_id")
    private Usuario repartidor;
    private int descuentoPorcentaje;
    private double montoDescuento;
    private String promocionDeliveryAplicada;
    private String descripcionDescuentoAplicado;
    private String celularCliente;
    private String dniDestinatario;
    private String nombreDestinatario;
    private String referenciaDireccion;
    private double subtotalProductos;
    private double total;
    private LocalDateTime fechaCompra;
    private LocalDateTime fechaPreparacion;
    private LocalDateTime fechaCamino;
    private LocalDateTime fechaEntregado;
    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelado;
    private String canceladoPor;
    private String canceladoPorNombre;
    private String motivoCancelacion;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public Pedido() {
    }

    public Pedido(int id, Usuario usuario, List<ItemCarrito> items, String direccion, String tipoEnvio, String estado, double total) {
        this.id = id;
        this.usuario = usuario;
        setItems(items);
        this.direccion = direccion;
        this.tipoEnvio = tipoEnvio;
        this.estado = estado;
        this.subtotalProductos = calcularSubtotalItems();
        this.total = total;
        this.fechaCompra = LocalDateTime.now();
    }

    public Pedido(int id, Usuario usuario, List<ItemCarrito> items, String direccion, String tipoEnvio,
                  double costoDelivery, String metodoPago, String estado, double total) {
        this(id, usuario, items, direccion, tipoEnvio, estado, total);
        this.costoDelivery = costoDelivery;
        this.metodoPago = metodoPago;
        this.estadoPago = calcularEstadoPagoInicial(metodoPago);
    }

    public Pedido(int id, Usuario usuario, List<ItemCarrito> items, String direccion, String tipoEnvio, String estado,
                  double total, String celularCliente, String dniDestinatario, String nombreDestinatario,
                  String referenciaDireccion) {
        this(id, usuario, items, direccion, tipoEnvio, estado, total);
        this.celularCliente = celularCliente;
        this.dniDestinatario = dniDestinatario;
        this.nombreDestinatario = nombreDestinatario;
        this.referenciaDireccion = referenciaDireccion;
    }

    public Pedido(int id, Usuario usuario, List<ItemCarrito> items, String direccion, String tipoEnvio,
                  double costoDelivery, String metodoPago, String estado, double total, String celularCliente,
                  String dniDestinatario, String nombreDestinatario, String referenciaDireccion) {
        this(id, usuario, items, direccion, tipoEnvio, costoDelivery, metodoPago, estado, total);
        this.celularCliente = celularCliente;
        this.dniDestinatario = dniDestinatario;
        this.nombreDestinatario = nombreDestinatario;
        this.referenciaDireccion = referenciaDireccion;
    }

    public Pedido(int id, Usuario usuario, List<ItemCarrito> items, String direccion, String tipoEnvio,
                  double costoDelivery, String metodoPago, String estado, double total, int descuentoPorcentaje,
                  double montoDescuento, String celularCliente, String dniDestinatario, String nombreDestinatario,
                  String referenciaDireccion) {
        this(id, usuario, items, direccion, tipoEnvio, costoDelivery, metodoPago, estado, total,
                celularCliente, dniDestinatario, nombreDestinatario, referenciaDireccion);
        this.descuentoPorcentaje = descuentoPorcentaje;
        this.montoDescuento = montoDescuento;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<ItemCarrito> getItems() {
        List<ItemCarrito> resultado = new ArrayList<>();

        for (PedidoItem item : items) {
            resultado.add(item.toItemCarrito());
        }

        return resultado;
    }

    public void setItems(List<ItemCarrito> items) {
        this.items.clear();

        if (items == null) {
            return;
        }

        for (ItemCarrito item : items) {
            PedidoItem pedidoItem = PedidoItem.desdeItemCarrito(item);
            pedidoItem.setPedido(this);
            this.items.add(pedidoItem);
        }

        this.subtotalProductos = calcularSubtotalItems();
    }

    public List<PedidoItem> getPedidoItems() {
        return items;
    }

    public void setPedidoItems(List<PedidoItem> pedidoItems) {
        this.items.clear();

        if (pedidoItems == null) {
            return;
        }

        for (PedidoItem item : pedidoItems) {
            item.setPedido(this);
            this.items.add(item);
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipoEnvio() {
        return tipoEnvio;
    }

    public void setTipoEnvio(String tipoEnvio) {
        this.tipoEnvio = tipoEnvio;
    }

    public double getCostoDelivery() {
        return costoDelivery;
    }

    public void setCostoDelivery(double costoDelivery) {
        this.costoDelivery = costoDelivery;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstadoPago() {
        if (estadoPago == null || estadoPago.trim().isEmpty()) {
            return calcularEstadoPagoInicial(metodoPago);
        }

        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getRepartidor() {
        return repartidor;
    }

    public void setRepartidor(Usuario repartidor) {
        this.repartidor = repartidor;
    }

    public int getDescuentoPorcentaje() {
        return descuentoPorcentaje;
    }

    public void setDescuentoPorcentaje(int descuentoPorcentaje) {
        this.descuentoPorcentaje = descuentoPorcentaje;
    }

    public double getMontoDescuento() {
        return montoDescuento;
    }

    public void setMontoDescuento(double montoDescuento) {
        this.montoDescuento = montoDescuento;
    }

    public String getCelularCliente() {
        return celularCliente;
    }

    public void setCelularCliente(String celularCliente) {
        this.celularCliente = celularCliente;
    }

    public String getDniDestinatario() {
        return dniDestinatario;
    }

    public void setDniDestinatario(String dniDestinatario) {
        this.dniDestinatario = dniDestinatario;
    }

    public String getNombreDestinatario() {
        return nombreDestinatario;
    }

    public void setNombreDestinatario(String nombreDestinatario) {
        this.nombreDestinatario = nombreDestinatario;
    }

    public String getReferenciaDireccion() {
        return referenciaDireccion;
    }

    public void setReferenciaDireccion(String referenciaDireccion) {
        this.referenciaDireccion = referenciaDireccion;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getSubtotalProductos() {
        if (subtotalProductos > 0 || items.isEmpty()) {
            return subtotalProductos;
        }

        return calcularSubtotalItems();
    }

    public void setSubtotalProductos(double subtotalProductos) {
        this.subtotalProductos = subtotalProductos;
    }

    public double getSubtotalConDescuento() {
        return getSubtotalProductos() - montoDescuento;
    }

    public String getPromocionDelivery() {
        if (promocionDeliveryAplicada != null && !promocionDeliveryAplicada.trim().isEmpty()) {
            return promocionDeliveryAplicada;
        }

        if (costoDelivery == 0) {
            return "Delivery gratis";
        }

        if (ENVIO_EXPRESS.equalsIgnoreCase(tipoEnvio) && getSubtotalProductos() >= 150) {
            return "Delivery express rebajado";
        }

        return "Tarifa regular";
    }

    public String getPromocionDeliveryAplicada() {
        return getPromocionDelivery();
    }

    public void setPromocionDeliveryAplicada(String promocionDeliveryAplicada) {
        this.promocionDeliveryAplicada = promocionDeliveryAplicada;
    }

    public String getDescripcionDescuentoAplicado() {
        if (descripcionDescuentoAplicado != null && !descripcionDescuentoAplicado.trim().isEmpty()) {
            return descripcionDescuentoAplicado;
        }

        if (descuentoPorcentaje > 0) {
            return "Descuento " + descuentoPorcentaje + "% por compra mayor a S/"
                    + obtenerMetaDescuento(descuentoPorcentaje);
        }

        return "Sin descuento aplicado";
    }

    public void setDescripcionDescuentoAplicado(String descripcionDescuentoAplicado) {
        this.descripcionDescuentoAplicado = descripcionDescuentoAplicado;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public LocalDateTime getFechaPreparacion() {
        return fechaPreparacion;
    }

    public void setFechaPreparacion(LocalDateTime fechaPreparacion) {
        this.fechaPreparacion = fechaPreparacion;
    }

    public LocalDateTime getFechaCamino() {
        return fechaCamino;
    }

    public void setFechaCamino(LocalDateTime fechaCamino) {
        this.fechaCamino = fechaCamino;
    }

    public LocalDateTime getFechaEntregado() {
        return fechaEntregado;
    }

    public void setFechaEntregado(LocalDateTime fechaEntregado) {
        this.fechaEntregado = fechaEntregado;
    }

    public LocalDateTime getFechaCancelado() {
        return fechaCancelado;
    }

    public void setFechaCancelado(LocalDateTime fechaCancelado) {
        this.fechaCancelado = fechaCancelado;
    }

    public String getCanceladoPor() {
        if (canceladoPor == null || canceladoPor.trim().isEmpty()) {
            return "sistema";
        }

        return canceladoPor;
    }

    public void setCanceladoPor(String canceladoPor) {
        this.canceladoPor = canceladoPor;
    }

    public String getCanceladoPorNombre() {
        if (canceladoPorNombre == null || canceladoPorNombre.trim().isEmpty()) {
            return "";
        }

        return canceladoPorNombre;
    }

    public void setCanceladoPorNombre(String canceladoPorNombre) {
        this.canceladoPorNombre = canceladoPorNombre;
    }

    public String getMotivoCancelacion() {
        if (motivoCancelacion == null || motivoCancelacion.trim().isEmpty()) {
            return "Sin motivo registrado";
        }

        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public String getFechaCompraTexto() {
        return formatearFecha(fechaCompra);
    }

    public String getHoraCompraTexto() {
        return formatearHora(fechaCompra);
    }

    public String getFechaPreparacionTexto() {
        return formatearFecha(fechaPreparacion);
    }

    public String getHoraPreparacionTexto() {
        return formatearHora(fechaPreparacion);
    }

    public String getFechaCaminoTexto() {
        return formatearFecha(fechaCamino);
    }

    public String getHoraCaminoTexto() {
        return formatearHora(fechaCamino);
    }

    public String getFechaEntregadoTexto() {
        return formatearFecha(fechaEntregado);
    }

    public String getHoraEntregadoTexto() {
        return formatearHora(fechaEntregado);
    }

    public String getFechaCanceladoTexto() {
        return formatearFecha(fechaCancelado);
    }

    public String getHoraCanceladoTexto() {
        return formatearHora(fechaCancelado);
    }

    public boolean isSolicitudRecibida() {
        return PENDIENTE.equals(estado);
    }

    public boolean isEnPreparacion() {
        return ASIGNADO.equals(estado) || EN_CAMINO.equals(estado) || ENTREGADO.equals(estado);
    }

    public boolean isEnCamino() {
        return EN_CAMINO.equals(estado) || ENTREGADO.equals(estado);
    }

    public boolean isEntregado() {
        return ENTREGADO.equals(estado);
    }

    public boolean isCancelado() {
        return CANCELADO.equals(estado);
    }

    public boolean isPagoTarjeta() {
        return PAGO_TARJETA.equalsIgnoreCase(metodoPago);
    }

    public boolean isCancelablePorCliente() {
        return PENDIENTE.equals(estado) || ASIGNADO.equals(estado);
    }

    public boolean isExpress() {
        return ENVIO_EXPRESS.equalsIgnoreCase(tipoEnvio);
    }

    public boolean isNoEditable() {
        return ASIGNADO.equals(estado) || EN_CAMINO.equals(estado) || ENTREGADO.equals(estado);
    }

    public boolean isDesasignable() {
        return ASIGNADO.equals(estado);
    }

    public boolean isActivoParaRepartidor() {
        return ASIGNADO.equals(estado) || EN_CAMINO.equals(estado);
    }

    public boolean isFinalizado() {
        return ENTREGADO.equals(estado) || CANCELADO.equals(estado);
    }

    public String getEstadoColor() {
        if (PENDIENTE.equals(estado)) {
            return "#f97316";
        }

        if (ASIGNADO.equals(estado)) {
            return "#2563eb";
        }

        if (EN_CAMINO.equals(estado)) {
            return "#7c3aed";
        }

        if (ENTREGADO.equals(estado)) {
            return "#16a34a";
        }

        if (CANCELADO.equals(estado)) {
            return "#dc2626";
        }

        return "#64748b";
    }

    public String getEstadoBadgeStyle() {
        return "display:inline-block;padding:4px 10px;border-radius:999px;color:white;font-weight:700;background:"
                + getEstadoColor();
    }

    public void marcarPagoReembolsadoSiCorresponde() {
        if (PAGO_PAGADO.equals(getEstadoPago())) {
            estadoPago = PAGO_REEMBOLSADO;
        }
    }

    public void anularPagoContraEntregaSiCorresponde() {
        if (PAGO_CONTRA_ENTREGA.equalsIgnoreCase(metodoPago)) {
            estadoPago = PAGO_ANULADO;
        }
    }

    private String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "Pendiente";
        }

        return fecha.format(FECHA);
    }

    private String formatearHora(LocalDateTime fecha) {
        if (fecha == null) {
            return "";
        }

        return fecha.format(HORA);
    }

    private String calcularEstadoPagoInicial(String metodoPago) {
        if (PAGO_TARJETA.equalsIgnoreCase(metodoPago)) {
            return PAGO_PAGADO;
        }

        return PAGO_PENDIENTE;
    }

    private double calcularSubtotalItems() {
        double subtotal = 0;

        for (PedidoItem item : items) {
            subtotal += item.getSubtotal();
        }

        return subtotal;
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
