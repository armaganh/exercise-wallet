# Servicio de bono monedero
En Playtomic tenemos un servicio de bono monedero. Los jugadores pueden recargar ese bono con su tarjeta de crédito y gastar ese dinero en la plataforma (reservas, alquiler de raquetas, ...).

Ese servicio tiene las siguientes operaciones:
- Puedes consultar el saldo.
- Puedes cargar dinero. En este caso haciendo un cobro una pasarela pasarela de pagos de terceros (stripe, paypal, redsys).
- Puedes gastar el saldo en compras en nuestra plataforma.
- Puedes devolver esas compras y recuperar el saldo.
- Puedes consultar el histórico de gastos y devoluciones.

Este ejercicio consiste en construir una prueba de concepto simplificada de este servicio de bono monedero. Sólo tienes que implementar las siguientes operaciones:

El ejercicio consiste en que programes endpoints para:
1. Consultar un bono monedero por su identificador.
1. Recargar dinero en ese bono a través de un servicio de pago de terceros.

La estructura básica que te proponemos para monedero es su identificador y su saldo actual. Si consideras que necesitas más campos,
añádelos sin problemas. Lo discutiremos en la entrevista.

Para que puedas ir al grano, te damos un proyecto maven con una aplicación Spring Boot 2.x, con las dependencias básicas y una
base de datos H2. Tienes perfiles de develop y test.

Hemos incluído una clase que simularía la llamada a la pasarela de pago real (StripePaymentService).
Esta clase está llamando a un simulador desplegado en uno de nuestros entornos que devolverá errores http 422 bajo ciertas condiciones.

Ten en cuenta que es un servicio que conviviría en un entorno de microservicios y alta disponibilidad. La concurrencia es importante. 

Le puedes dedicar el tiempo que quieras, pero hemos estimado que 4 horas es suficiente para demostrar [los requisitos del puesto.](OFFER.md)
No hace falta que lo documentes pero puedes anotar todo lo que quieras como explicación o justificación de lo que hagas o dejes sin hacer.
Tampoco necesitas hacer testing de todo, pero sí nos gustaría ver varios tipos de test.