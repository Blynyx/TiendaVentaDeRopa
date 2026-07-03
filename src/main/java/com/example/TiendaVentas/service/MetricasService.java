package com.example.TiendaVentas.service;

import com.example.TiendaVentas.model.Pedido;
import com.example.TiendaVentas.repository.PedidoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class MetricasService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PedidoRepository pedidoRepository;

    public MetricasService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public Map<String, Object> obtenerVentasPorDia() {
        return construirSerie(obtenerVentasAgrupadasPorFecha());
    }

    public Map<String, Object> obtenerPedidosCreadosPorDia() {
        return construirSerie(obtenerPedidosCreadosPorFecha());
    }

    public Map<String, Object> obtenerPedidosEntregadosPorDia() {
        return construirSerie(obtenerPedidosEntregadosPorFecha());
    }

    public Map<String, Object> obtenerCancelacionesPorDia() {
        return construirSerie(obtenerCancelacionesPorFecha());
    }

    public Map<String, Object> obtenerIngresosAcumuladosPorDia() {
        Map<LocalDate, Number> ventasPorDia = obtenerVentasAgrupadasPorFecha();
        Map<LocalDate, Number> acumuladoPorDia = new TreeMap<>();
        double acumulado = 0;

        for (Map.Entry<LocalDate, Number> entry : ventasPorDia.entrySet()) {
            acumulado += entry.getValue().doubleValue();
            acumuladoPorDia.put(entry.getKey(), acumulado);
        }

        return construirSerie(acumuladoPorDia);
    }

    private Map<LocalDate, Number> obtenerVentasAgrupadasPorFecha() {
        Map<LocalDate, Number> datos = new TreeMap<>();
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            LocalDate fechaVenta = obtenerFechaVentaReal(pedido);

            if (fechaVenta != null) {
                sumarMonto(datos, fechaVenta, pedido.getTotal());
            }
        }

        return datos;
    }

    private boolean debeContarseComoVenta(Pedido pedido) {
        if (pedido == null || !Pedido.PAGO_PAGADO.equals(pedido.getEstadoPago())) {
            return false;
        }

        if (pedido.isPagoTarjeta()) {
            return true;
        }

        if (Pedido.PAGO_CONTRA_ENTREGA.equalsIgnoreCase(pedido.getMetodoPago())) {
            return pedido.isEntregado();
        }

        return false;
    }

    private LocalDate obtenerFechaVentaReal(Pedido pedido) {
        if (!debeContarseComoVenta(pedido)) {
            return null;
        }

        if (pedido.isPagoTarjeta()) {
            LocalDateTime fechaCompra = pedido.getFechaCompra();
            return fechaCompra != null ? fechaCompra.toLocalDate() : null;
        }

        LocalDateTime fechaEntregado = pedido.getFechaEntregado();
        return fechaEntregado != null ? fechaEntregado.toLocalDate() : null;
    }

    private Map<LocalDate, Number> obtenerPedidosCreadosPorFecha() {
        Map<LocalDate, Number> datos = new TreeMap<>();
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            LocalDateTime fechaCompra = pedido.getFechaCompra();

            if (fechaCompra != null) {
                LocalDate fecha = fechaCompra.toLocalDate();
                aumentarContador(datos, fecha);
            }
        }

        return datos;
    }

    private Map<LocalDate, Number> obtenerPedidosEntregadosPorFecha() {
        Map<LocalDate, Number> datos = new TreeMap<>();
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            LocalDateTime fechaEntregado = pedido.getFechaEntregado();

            if (fechaEntregado != null) {
                LocalDate fecha = fechaEntregado.toLocalDate();
                aumentarContador(datos, fecha);
            }
        }

        return datos;
    }

    private Map<LocalDate, Number> obtenerCancelacionesPorFecha() {
        Map<LocalDate, Number> datos = new TreeMap<>();
        List<Pedido> pedidos = pedidoRepository.findAll();

        for (Pedido pedido : pedidos) {
            LocalDateTime fechaCancelado = pedido.getFechaCancelado();

            if (fechaCancelado != null) {
                LocalDate fecha = fechaCancelado.toLocalDate();
                aumentarContador(datos, fecha);
            }
        }

        return datos;
    }

    private void aumentarContador(Map<LocalDate, Number> datos, LocalDate fecha) {
        if (datos.containsKey(fecha)) {
            int cantidadActual = datos.get(fecha).intValue();
            datos.put(fecha, cantidadActual + 1);
        } else {
            datos.put(fecha, 1);
        }
    }

    private void sumarMonto(Map<LocalDate, Number> datos, LocalDate fecha, double monto) {
        if (datos.containsKey(fecha)) {
            double totalActual = datos.get(fecha).doubleValue();
            datos.put(fecha, totalActual + monto);
        } else {
            datos.put(fecha, monto);
        }
    }

    private Map<String, Object> construirSerie(Map<LocalDate, Number> datos) {
        Map<String, Object> serie = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Number> valores = new ArrayList<>();

        for (Map.Entry<LocalDate, Number> entry : datos.entrySet()) {
            labels.add(entry.getKey().format(FORMATO_FECHA));
            valores.add(entry.getValue());
        }

        serie.put("labels", labels);
        serie.put("valores", valores);
        return serie;
    }
}
