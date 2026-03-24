---
name: Modelo de datos y roles de Musicon/Festia
description: Estructura de usuarios, roles, permisos y entidades core del sistema - base para analisis de producto
type: project
---

El sistema se llama internamente Gestmusica pero la marca publica es **festia.es**.

## Tipos de usuario (TipoUsuarioEnum)
- REPRESENTANTE, ARTISTA, AGENCIA, PUBLICO

## Roles (RolEnum)
- ROL_ADMINISTRADOR (ADMIN)
- ROL_REPRESENTANTE (REPRE - "Agente Pro")
- ROL_AGENTE (AGENTE - "Representante")
- ROL_AGENCIA (AGENCIA)
- ROL_ARTISTA (ARTISTA)

## Entidades clave
- **Usuario**: tiene fechaUltimoAcceso, fechaRegistro, activo, validado, emailVerified, tipoUsuario
- **Artista**: vinculado a usuario y agencia, tiene flag activo, pertenece a comunidades de trabajo (CCAA)
- **Agencia**: vinculada a usuario, tiene artistas, flag activo
- **Ocupacion**: la entidad transaccional core (bookings/fechas), con estados OCUPADO/RESERVADO/PENDIENTE/ANULADO
- **Acceso**: tabla de permisos usuario-agencia-artista-rol
- **AccesoArtista**: permisos granulares usuario-artista
- **Mensaje**: sistema de notificaciones internas

## Infraestructura de notificaciones existente
- Mailgun para email (via MailgunEmailService)
- Sistema de mensajes internos (tabla mensaje)
- Ya existe un job programado: ReporteMensualAgenciaJob (cron el dia 1 de cada mes a las 18h)
- Templates de email existentes: registro, recuperacion password, confirmacion ocupacion, reporte mensual agencia, bienvenida agencia

**Why:** Entender la estructura para analisis de producto y decisiones de roadmap.
**How to apply:** Usar como base para cualquier analisis de features, monetizacion o engagement.
