INSERT INTO usuarios (id, nombre, apellido, correo, dni, telefono, password, rol, administrador_principal)
VALUES
(1, 'Juan', 'Perez', 'admin@gmail.com', '10000001', '900000001', '123', 'ADMINISTRADOR', true),
(2, 'Maria', 'Lopez', 'maria@gmail.com', '20000002', '900000002', '123', 'CLIENTE', false),
(3, 'Carlos', 'Ramos', 'carlos@delivery.com', '30000003', '987654321', '123', 'REPARTIDOR', false),
(4, 'Ana', 'Torres', 'ana@delivery.com', '40000004', '912345678', '123', 'REPARTIDOR', false),
(5, 'Luis', 'Vargas', 'luis@gmail.com', '50000005', '955111222', '123', 'CLIENTE', false),
(6, 'Rosa', 'Diaz', 'rosa@gmail.com', '60000006', '944222333', '123', 'CLIENTE', false);

INSERT INTO categorias (id, nombre, descripcion, activa)
VALUES
(1, 'Polos', 'Camisetas urbanas para uso diario', true),
(2, 'Pantalones', 'Jeans y joggers de estilo casual', true),
(3, 'Calzado', 'Zapatillas para looks urbanos', true),
(4, 'Accesorios', 'Gorras, morrales y complementos', true),
(5, 'Casacas', 'Prendas exteriores para temporada urbana', true),
(6, 'Liquidacion', 'Categoria oculta para demostrar reactivacion', false);

INSERT INTO productos (id, nombre, descripcion, precio, categoria_id, stock, imagen, activo)
VALUES
(1, 'Polo oversize negro', 'Algodon suave, corte amplio y estilo urbano.', 35, 1, 15, 'PO', true),
(2, 'Polo basico blanco', 'Prenda comoda para combinar con cualquier outfit.', 25, 1, 20, 'PB', true),
(3, 'Jeans slim azul', 'Denim resistente con acabado moderno.', 89, 2, 10, 'JS', true),
(4, 'Jogger cargo beige', 'Pantalon practico con bolsillos laterales.', 75, 2, 8, 'JC', true),
(5, 'Zapatillas urbanas', 'Calzado ligero para caminar todo el dia.', 120, 3, 6, 'ZU', true),
(6, 'Gorra street', 'Accesorio clasico para completar el look.', 28, 4, 18, 'GS', true),
(7, 'Casaca denim azul', 'Casaca clasica para looks casuales.', 140, 5, 7, 'CD', true),
(8, 'Mochila urbana', 'Mochila resistente para uso diario.', 65, 4, 12, 'MU', true),
(9, 'Polo vintage gris', 'Producto inactivo para demostrar activacion.', 32, 1, 5, 'PV', false),
(10, 'Short liquidacion', 'Producto asociado a categoria oculta.', 45, 6, 9, 'SL', true);

INSERT INTO pedidos (
    id, usuario_id, repartidor_id, direccion, tipo_envio, costo_delivery, metodo_pago, estado_pago,
    estado, descuento_porcentaje, monto_descuento, promocion_delivery_aplicada,
    descripcion_descuento_aplicado, celular_cliente, dni_destinatario, nombre_destinatario,
    referencia_direccion, subtotal_productos, total, fecha_compra, fecha_preparacion, fecha_camino, fecha_entregado,
    fecha_cancelacion, cancelado_por, cancelado_por_nombre, motivo_cancelacion
)
VALUES
(1001, 2, NULL, 'Av. Primavera 123', 'Normal', 5, 'Contra entrega', 'Pendiente',
 'Pendiente', 0, 0, 'Tarifa regular', 'Sin descuento aplicado', '900000002',
 '20000002', 'Maria Lopez', 'Frente al parque', 95, 95,
 '2026-06-01 10:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(1002, 5, 3, 'Jr. Los Olivos 456', 'Express', 0, 'Tarjeta', 'Pagado',
 'Entregado', 5, 8.80, 'Delivery gratis', 'Descuento 5% por compra mayor a S/150',
 '955111222', '50000005', 'Luis Vargas', 'Piso 2', 176,
 167.20, '2026-05-28 09:30:00', '2026-05-28 10:00:00',
 '2026-05-28 11:00:00', '2026-05-28 13:00:00', NULL, NULL, NULL, NULL);

INSERT INTO pedido_items (id, pedido_id, producto_id, cantidad, precio_unitario, subtotal)
VALUES
(1, 1001, 1, 2, 35, 70),
(2, 1001, 2, 1, 25, 25),
(3, 1002, 5, 1, 120, 120),
(4, 1002, 6, 2, 28, 56);

ALTER TABLE usuarios ALTER COLUMN id RESTART WITH 7;
ALTER TABLE categorias ALTER COLUMN id RESTART WITH 7;
ALTER TABLE productos ALTER COLUMN id RESTART WITH 11;
ALTER TABLE pedidos ALTER COLUMN id RESTART WITH 1003;
ALTER TABLE pedido_items ALTER COLUMN id RESTART WITH 5;
