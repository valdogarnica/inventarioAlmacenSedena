# Sistema de Inventario de Almacén

Sistema de gestión de inventario con préstamos de herramientas desarrollado en Java con SQLite e interfaz gráfica moderna usando FlatLaf.

## Características

- ✅ Gestión completa de herramientas (agregar, buscar, consultar stock)
- ✅ Sistema de préstamos con seguimiento de clientes y empleados
- ✅ Carrito de préstamos para múltiples herramientas
- ✅ Gestión automática de stock (descuento en préstamos, aumento en ingresos)
- ✅ Configuración flexible de base de datos (selección de carpeta y base de datos)
- ✅ Interfaz gráfica moderna e intuitiva con FlatLaf
- ✅ Registro completo de préstamos y devoluciones

## Requisitos

- Java 11 o superior
- Maven 3.6 o superior
- NetBeans IDE (recomendado) o cualquier IDE compatible con Java

## Instalación

1. Clonar o descargar el proyecto
2. Abrir el proyecto en NetBeans
3. NetBeans detectará automáticamente el archivo `pom.xml` y descargará las dependencias (FlatLaf y SQLite JDBC)

## Configuración

### Primera vez

1. Ejecutar la aplicación
2. Se abrirá automáticamente el diálogo de configuración de base de datos
3. Seleccionar la carpeta donde se almacenarán las bases de datos
4. Crear una nueva base de datos o seleccionar una existente
5. Hacer clic en "Conectar"

### Configurar Empleado

1. Al iniciar la aplicación, se solicitará el nombre del empleado
2. También se puede cambiar desde el botón "Cambiar" en la parte superior

## Uso

### Buscar Herramientas

1. Ingresar el nombre o categoría de la herramienta en el campo de búsqueda
2. Presionar Enter o hacer clic en "Buscar"
3. Los resultados aparecerán en la tabla

### Realizar un Préstamo

1. Buscar la herramienta deseada
2. Seleccionar la herramienta en la tabla
3. Ingresar la cantidad a prestar
4. Hacer clic en "Agregar al Carrito"
5. Repetir para agregar más herramientas
6. Hacer clic en "Realizar Préstamo"
7. Ingresar el nombre del cliente
8. Confirmar el préstamo

### Agregar Herramientas

1. Menú: Gestión → Agregar Herramienta
2. Completar el formulario
3. Hacer clic en "Guardar"

### Registrar Ingreso de Stock

1. Menú: Gestión → Registrar Ingreso de Stock
2. Seleccionar la herramienta
3. Ingresar la cantidad a agregar
4. Hacer clic en "Registrar Ingreso"

### Ver Préstamos

1. Menú: Gestión → Ver Préstamos
2. Se mostrarán todos los préstamos realizados
3. Para marcar como devuelto, seleccionar el préstamo y hacer clic en "Marcar como Devuelto"

## Estructura del Proyecto

```
src/main/java/com/almacen/
├── Main.java                    # Punto de entrada de la aplicación
├── model/                       # Modelos de datos
│   ├── Herramienta.java
│   ├── Prestamo.java
│   └── ItemCarrito.java
├── database/                    # Gestión de base de datos
│   └── DatabaseManager.java
└── ui/                          # Interfaces gráficas
    ├── MainWindow.java          # Ventana principal
    ├── ConfiguracionDBDialog.java
    ├── ConfirmarPrestamoDialog.java
    ├── AgregarHerramientaDialog.java
    ├── VerPrestamosDialog.java
    └── RegistrarIngresoDialog.java
```

## Base de Datos

El sistema utiliza SQLite y crea automáticamente las siguientes tablas:

- **herramientas**: Almacena información de las herramientas
- **prestamos**: Registra todos los préstamos realizados

### Datos de Ejemplo

Al crear una nueva base de datos, se insertan automáticamente herramientas de ejemplo:
- Pala
- Pico
- Martillo
- Destornillador
- Llave inglesa
- Taladro
- Sierra
- Nivel

## Tecnologías Utilizadas

- **Java 11**: Lenguaje de programación
- **SQLite**: Base de datos embebida
- **FlatLaf 3.6.1**: Librería de Look and Feel moderno para Swing
- **Maven**: Gestión de dependencias

## Notas

- La aplicación guarda automáticamente la última carpeta de bases de datos seleccionada
- El stock se actualiza automáticamente al realizar préstamos o ingresos
- Los préstamos pueden ser marcados como devueltos, lo que restaura el stock automáticamente
- El sistema valida que haya suficiente stock antes de permitir un préstamo

## Licencia

Este proyecto es de uso libre para fines educativos y comerciales.
