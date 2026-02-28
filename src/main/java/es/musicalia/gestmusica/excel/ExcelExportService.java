package es.musicalia.gestmusica.excel;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Servicio para exportar datos a archivos Excel (.xls/.xlsx)
 */
public interface ExcelExportService {

    /**
     * Exporta una lista de DTOs a un archivo Excel
     *
     * @param data Lista de objetos DTO a exportar
     * @param clazz Clase del DTO para obtener metadatos
     * @param sheetName Nombre de la hoja de Excel
     * @param <T> Tipo del DTO
     * @return ByteArrayOutputStream con el contenido del archivo Excel
     */
    <T> ByteArrayOutputStream exportToExcel(List<T> data, Class<T> clazz, String sheetName);

    /**
     * Exporta una lista de DTOs a un archivo Excel con información de encabezado personalizada
     *
     * @param data Lista de objetos DTO a exportar
     * @param clazz Clase del DTO para obtener metadatos
     * @param sheetName Nombre de la hoja de Excel
     * @param headerInfo Información para mostrar en el encabezado (líneas de texto)
     * @param <T> Tipo del DTO
     * @return ByteArrayOutputStream con el contenido del archivo Excel
     */
    <T> ByteArrayOutputStream exportToExcelWithHeader(List<T> data, Class<T> clazz, String sheetName, List<String> headerInfo);

    /**
     * Exporta una lista de DTOs a un archivo Excel con información en columna lateral
     *
     * @param data Lista de objetos DTO a exportar
     * @param clazz Clase del DTO para obtener metadatos
     * @param sheetName Nombre de la hoja de Excel
     * @param sideInfo Información para mostrar en columna lateral derecha
     * @param fieldsToInclude Lista de nombres de campos a incluir (null = todos)
     * @param <T> Tipo del DTO
     * @return ByteArrayOutputStream con el contenido del archivo Excel
     */
    <T> ByteArrayOutputStream exportToExcelWithSideInfo(List<T> data, Class<T> clazz, String sheetName, List<String> sideInfo, List<String> fieldsToInclude);
}
