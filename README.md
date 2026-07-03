# TiendaVentas - Spring Boot MVC con JSP y H2

Proyecto academico de tienda online de ropa usando Spring Boot MVC, JSP, JPA y base de datos H2 en memoria.

## Estructura principal

- `controller`: controladores MVC con `@Controller`.
- `model`: entidades y modelos del sistema.
- `repository`: repositorios Spring Data JPA.
- `service`: servicios de negocio y consultas a repositorios.
- `webapp/WEB-INF/jsp`: vistas JSP.
- `resources/static/css`: estilos CSS.
- `resources/data.sql`: datos semilla para demostracion.

## Controladores

- `ProductoController`: catalogo, buscador, filtro por categoria y CRUD de productos.
- `CategoriaController`: gestion de categorias.
- `UsuarioController`: login, registro y gestion de usuarios por rol.
- `CarritoController`: carrito en sesion HTTP, validacion de stock y visibilidad con productos H2.
- `PedidoController`: checkout, creacion de pedidos, cancelaciones, historial y seguimiento.
- `DeliveryController`: pedidos asignados, cambios de estado e historial del repartidor.
- `PaginaController`: paginas generales y dashboard de metricas.

## Rutas importantes

- `/login`: inicio de sesion.
- `/registro`: registro de cliente.
- `/principal`: pagina principal del cliente.
- `/catalogo`: catalogo con buscador y filtro.
- `/carrito`: carrito con cantidades y total.
- `/checkout`: formulario de compra.
- `/mis-pedidos`: historial del cliente.
- `/pedido/estado`: seguimiento de pedido.
- `/delivery/pedidos`: modulo de repartidor.
- `/metricas`: dashboard administrativo.
- `/gestion`: panel administrativo.
- `/h2-console`: consola web de H2.

## Persistencia

El sistema usa H2 en memoria con JPA. Los datos semilla se cargan desde `data.sql` al iniciar la aplicacion.

## Flujo de compra

1. El usuario entra al catalogo.
2. Busca o filtra productos visibles.
3. Agrega productos al carrito almacenado en sesion.
4. El carrito valida stock y visibilidad desde H2.
5. El checkout revalida stock y precios antes de confirmar.
6. Se crea el pedido y se descuenta stock.
7. El administrador asigna repartidor.
8. El repartidor cambia el estado a `En camino` y luego a `Entregado`, o cancela segun reglas vigentes.

## Estados del pedido

- Pendiente
- Asignado
- En camino
- Entregado
- Cancelado

## JSP y Model

Los controladores envian datos a las JSP usando `Model`:

```java
model.addAttribute("productos", productos);
```

Las JSP muestran datos con expresiones EL:

```jsp
${producto.nombre}
```

Y recorren listas con JSTL:

```jsp
<c:forEach var="producto" items="${productos}">
    ${producto.nombre}
</c:forEach>
```
