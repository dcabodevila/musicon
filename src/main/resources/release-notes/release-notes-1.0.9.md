# Exportación a Excel del listado de ocupaciones

Si eres agencia, ahora puedes exportar a Excel el listado de ocupaciones filtradas desde la pantalla de gestión de ocupaciones.

## Cómo usar la funcionalidad

1. Accede a **Ocupaciones** desde el menú principal.

2. Filtra las ocupaciones que deseas exportar utilizando los filtros disponibles:
   - **Agencia**: Selecciona la agencia
   - **Artista**: Selecciona el artista (o todos los artistas de la agencia)
   - **Fechas actuación**: Define el rango de fechas desde/hasta

3. Pulsa el botón **Buscar** para aplicar los filtros.

4. Una vez aplicados los filtros, pulsa el botón **Descargar Excel** ubicado junto al botón "Nueva ocupación".

![export-ocupacionees.png](/img/release-notes/export-ocupacionees.png)

5. El archivo Excel se descargará automáticamente con el nombre `Ocupaciones_[fecha_hora].xlsx`.

## Información incluida en el Excel

El archivo Excel exportado contiene las siguientes columnas para cada ocupación filtrada:

- **ID**: Identificador de la ocupación
- **Artista**: Nombre del artista
- **Fecha**: Fecha de la actuación (formato dd-mm-yyyy)
- **Localidad**: Localidad de la actuación
- **Municipio**: Municipio de la actuación
- **Provincia**: Provincia de la actuación
- **Matinal**: Indica si es matinal (Sí/No)
- **Solo Matinal**: Indica si es solo matinal (Sí/No)
- **Nombre Comercial Representante**: Nombre comercial del representante que creó la ocupación
- **Teléfono Representante**: Teléfono del representante que creó la ocupación

**Nota**: Las ocupaciones se exportan ordenadas por fecha de actuación de forma ascendente.



