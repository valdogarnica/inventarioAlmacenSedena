# Sistema de Inventario de Almacén

Sistema de gestión de inventario con préstamos de herramientas desarrollado en Java con SQLite e interfaz gráfica moderna usando FlatLaf.

## Características

- ✅ Menú lateral paginado: cada sección es una página (Nuevo préstamo, Préstamos registrados, Inventario, Remisiones, Proveedores, Categorías/tipos/unidades, Reportes, Configuración). Atajos Ctrl+1 … Ctrl+8
- ✅ Remisiones de proveedor: se registra el número de remisión (texto libre), proveedor, obra, quién envía, quién recibe, fecha y todos los materiales con su cantidad y unidad
- ✅ Cada material pertenece a su proveedor y además se acumula: el mismo material de dos proveedores se guarda en dos registros (cada uno con su stock), y la vista "Total por material" del inventario suma ambos y muestra cuánto tiene cada proveedor
- ✅ Proveedores con sus materiales y sus remisiones
- ✅ Catálogos administrables de Categorías, Tipos y Unidades (se eligen en combos y se pueden agregar desde los formularios)
- ✅ Búsqueda en todas las listas desplegables: al abrir un combo aparece "Escriba para buscar…" y lo que se teclea filtra las opciones (sin importar mayúsculas ni acentos; Enter elige el resultado)
- ✅ Sistema de préstamos con carrito, devoluciones parciales y fotos
- ✅ Reportes PDF con fecha de generación, unidades y numeración de páginas
- ✅ Configuración flexible de base de datos (selección de carpeta y base de datos)
- ✅ Interfaz gráfica moderna con FlatLaf

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
3. Seleccionar la carpeta donde se guardará toda la información (solo se configura una vez)
4. Crear una nueva base de datos o seleccionar una existente
5. Hacer clic en "Conectar"

Cada base de datos tiene su propia carpeta de fotos dentro de esa carpeta, con el mismo nombre seguido de "Fotos" (por ejemplo `inventario.db` → `inventarioFotos`). Se crea automáticamente al crear o elegir la base. Las fotos tomadas antes de este cambio se siguen encontrando en la carpeta de fotos que estaba configurada.

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

### Registrar una remisión de proveedor

1. Página **Remisiones** → **Nueva remisión**
2. Elegir el proveedor (o registrarlo con **Nuevo**), capturar el número de remisión si la hoja lo trae, obra, quién envía, quién recibe y la fecha
3. Capturar cada elemento con su cantidad y unidad (categoría, tipo y descripción son opcionales). Al escribir un material que el proveedor ya tiene, la columna *Estado* indica "Existente" y muestra cómo quedará el stock; si no, se creará como material nuevo de ese proveedor
4. **Guardar remisión**. Se puede abrir el comprobante en PDF

Para corregir una remisión: **Ver / editar** en la lista, marcar **Habilitar edición**, cambiar datos o materiales (agregar, quitar o cambiar cantidades) y **Guardar cambios**. El stock se ajusta a la diferencia; si se quiere quitar más de lo que hay en existencia (porque ya está prestado), no se guarda y se indica qué material falta.

Ejemplo: si llega *material1* × 5 de *proveedor1* y después *material1* × 10 de *proveedor2*, el inventario muestra dos registros: *material1 [proveedor1]* con 5 y *material1 [proveedor2]* con 10. Una nueva remisión de *proveedor1* con *material1* × 3 deja ese registro en 8.

### Agregar materiales / herramientas

1. Página **Inventario** → **Agregar material**
2. Completar nombre, proveedor, categoría, tipo, unidad y stock inicial
3. Hacer clic en "Guardar"

### Registrar ingreso de stock sin remisión

1. Página **Inventario** → **Registrar ingreso**
2. Seleccionar el material (se muestra su proveedor) e ingresar la cantidad

### Ver préstamos

1. Página **Préstamos registrados** → **Préstamos activos**
2. Para registrar una devolución, seleccionar el préstamo y hacer clic en "Registrar devolución"

### Reportes

En la página **Reportes**:

- **Reporte general de inventario**: todos los materiales con categoría, tipo, unidad, proveedor, disponible, prestado, total, fecha de alta y resumen por unidad
- **Inventario por proveedor**
- **Entradas por remisión** en un rango de fechas
- **Préstamos activos (general)**
- **Reporte por herramienta**: total de la herramienta sumando proveedores con el desglose de cada uno, sus entradas por remisión y sus préstamos activos con fecha, unidad y categoría

## Estructura del Proyecto

```
src/main/java/com/almacen/
├── Main.java                    # Punto de entrada de la aplicación
├── model/                       # Modelos (Herramienta, Proveedor, Remision, DetalleRemision, Prestamo...)
├── database/
│   ├── DatabaseManager.java     # Todo el acceso a SQLite y las migraciones
│   └── Catalogo.java            # Catálogos: categorías, tipos y unidades
├── report/
│   ├── PdfReportBuilder.java    # Encabezado, tablas y paginación de los PDF
│   └── ReportesPdf.java         # Todos los reportes PDF
└── ui/
    ├── MainWindow.java          # Ventana principal con menú lateral paginado
    ├── InventarioPanel, RemisionesPanel, ProveedoresPanel,
    │   CatalogosPanel, ReportesPanel   # Páginas del menú
    ├── RegistrarRemisionDialog, RemisionDetalleDialog
    ├── MaterialFormDialog, ProveedorFormDialog
    └── ... (préstamos, devoluciones, configuración)
```

## Base de Datos

El sistema utiliza SQLite y crea automáticamente las tablas:

- **proveedores**: nombre, contacto y teléfono
- **remisiones** y **detalle_remisiones**: cada remisión con sus materiales, cantidades y unidades
- **herramientas**: materiales/herramientas; cada registro pertenece a un proveedor (`proveedor_id`) y tiene categoría, tipo, unidad, stock y fecha de alta
- **categorias**, **tipos**, **unidades**: catálogos
- **prestamos**, **detalle_prestamos**, **historial_devoluciones**

### Actualización de bases existentes

Al abrir una base de datos de la versión anterior, el sistema la actualiza sin borrar datos: agrega las tablas y columnas nuevas, pasa los proveedores escritos como texto a la tabla de proveedores y deja a los materiales existentes con unidad *Pieza* y tipo *Herramienta* (se pueden cambiar después). Esto se hace una sola vez (`PRAGMA user_version = 2`). Se recomienda respaldar el archivo `.db` antes de abrirlo con la nueva versión.

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
