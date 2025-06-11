# Proyecto Amazon - Guía de Ejecución

## Requisitos previos

- **Java 17 o superior** (recomendado Java 21)
- **Maven 3.8+**
- **Navegador web** (para acceder a las aplicaciones)
- **Puerto libre 8080, 8081 y 8082** en tu máquina

---

## Estructura de carpetas

- `/camundaEngine` → Motor de procesos Camunda BPM (Spring Boot)
- `/amazonApp` → Aplicación web para administración (Spring Boot)
- `/clienteApp` → Aplicación web para clientes (Spring Boot)

---

## 1. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd <carpeta_del_proyecto>
```

---

## 2. Compilar todos los proyectos

Desde la raíz del proyecto, ejecuta:

```bash
mvn clean install
```

---

## 3. Ejecutar los proyectos

**Abre tres terminales diferentes** (o usa procesos en segundo plano):

### a) Iniciar el motor Camunda

```bash
cd camundaEngine
mvn spring-boot:run
```
- Acceso a Camunda Web: [http://localhost:8080](http://localhost:8080)
- Usuario por defecto: `admin` / `123` (ajusta según tu configuración)

### b) Iniciar la aplicación de administración (AmazonApp)

```bash
cd amazonApp
mvn spring-boot:run
```
- Acceso: [http://localhost:8082](http://localhost:8082)

### c) Iniciar la aplicación de clientes (ClienteApp)

```bash
cd clienteApp
mvn spring-boot:run
```
- Acceso: [http://localhost:8081](http://localhost:8081)

---

## 4. Flujo de uso

1. **Cliente** inicia un proceso desde [http://localhost:8081](http://localhost:8081) (por ejemplo, devolución o compra).
2. **Camunda** orquesta el proceso y muestra las tareas pendientes en la web de administración [http://localhost:8082](http://localhost:8082).
3. **Administrador** gestiona las tareas y el flujo avanza según las reglas BPMN y DMN.
4. Puedes monitorear y administrar los procesos desde la consola de Camunda en [http://localhost:8080](http://localhost:8080).

---

## 5. Notas técnicas

- Si cambias los puertos, actualiza los archivos `application.properties` en cada subproyecto.
- Los procesos BPMN y las reglas DMN están en la carpeta `camundaEngine/src/main/resources`.
- Si tienes problemas de dependencias, ejecuta `mvn clean install` en cada subcarpeta.

---

## 6. Tecnologías usadas

- **Spring Boot** (para backend y web)
- **Thymeleaf** (para vistas HTML)
- **Camunda BPM** (motor de procesos)
- **Maven** (gestión de dependencias)

---
