# Oreo Insight Factory

Backend de referencia para el Hackathon "Oreo Insight Factory". Implementa autenticación JWT, gestión de usuarios y ventas, además de generación de resúmenes semanales usando un LLM vía GitHub Models y envío de correos asíncronos.

## Equipo
- Integrante 1 – Código 000000

> Reemplazar con los nombres reales del equipo antes de entregar.

## Requisitos
- Java 21+
- Maven 3.9+

## Configuración
Las credenciales y servicios externos se inyectan por variables de entorno:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export GITHUB_TOKEN=ghp_xxx
export GITHUB_MODELS_URL=https://models.github.com/v1/chat/completions
export MODEL_ID=gpt-5-mini
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=tu-email@gmail.com
export MAIL_PASSWORD=app-password
```

Para desarrollo se recomienda levantar un servidor SMTP local (por ejemplo [MailDev](https://maildev.github.io/maildev/)).

## Ejecución
```bash
mvn spring-boot:run
```
La API se expone en `http://localhost:8080`.

## Endpoints principales
- `POST /auth/register` – Registro de usuarios CENTRAL o BRANCH
- `POST /auth/login` – Autenticación y entrega de JWT
- `CRUD /sales` – Gestión de ventas con reglas por rol
- `POST /sales/summary/weekly` – Solicitud de resumen estándar (202 Accepted)
- `POST /sales/summary/weekly/premium` – Solicitud premium con HTML, gráficos y PDF
- `GET/GET/{id}/DELETE /users` – Administración de usuarios (solo CENTRAL)

## Flujo Asíncrono
1. El controlador publica un `ReportRequestedEvent`.
2. `SummaryProcessor` escucha el evento con `@Async`, calcula agregados, consulta el LLM y envía el correo.
3. Para el modo premium se construye un email HTML, se genera un gráfico con QuickChart y un PDF usando OpenPDF.

## Testing
```bash
mvn test
```
Incluye 5 pruebas unitarias sobre `SalesAggregationService`.

## Postman
La colección `postman/Oreo Insight Factory.postman_collection.json` contiene un flujo end-to-end con registro, autenticación, CRUD de ventas y solicitud de resumen.

## Notas
- No subas credenciales reales al repositorio.
- Ajusta las variables de entorno en el servicio donde despliegues.
