package es.musicalia.gestmusica.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de exportación a Excel
 */
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    @Override
    public <T> ByteArrayOutputStream exportToExcel(List<T> data, Class<T> clazz, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            
            // Obtener campos anotados con @ExcelColumn
            List<FieldColumnInfo> columnInfos = getAnnotatedFields(clazz);
            
            if (columnInfos.isEmpty()) {
                throw new IllegalArgumentException("La clase " + clazz.getName() + " no tiene campos anotados con @ExcelColumn");
            }
            
            // Crear estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // Crear fila de encabezados
            createHeaderRow(sheet, columnInfos, headerStyle);
            
            // Crear filas de datos
            createDataRows(sheet, data, columnInfos, dataStyle);
            
            // Ajustar ancho de columnas
            autoSizeColumns(sheet, columnInfos.size());
            
            // Escribir a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream;
            
        } catch (IOException e) {
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }
    
    /**
     * Obtiene los campos anotados con @ExcelColumn ordenados por orden
     */
    private <T> List<FieldColumnInfo> getAnnotatedFields(Class<T> clazz) {
        List<FieldColumnInfo> columnInfos = new ArrayList<>();
        
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                field.setAccessible(true);
                columnInfos.add(new FieldColumnInfo(field, annotation.value(), annotation.order()));
            }
        }
        
        // Ordenar por el atributo order
        return columnInfos.stream()
                .sorted(Comparator.comparingInt(FieldColumnInfo::order))
                .collect(Collectors.toList());
    }
    
    /**
     * Crea el estilo para las celdas de encabezado
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Crea el estilo para las celdas de datos
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Crea la fila de encabezados
     */
    private void createHeaderRow(Sheet sheet, List<FieldColumnInfo> columnInfos, CellStyle headerStyle) {
        createHeaderRow(sheet, columnInfos, headerStyle, 0);
    }

    /**
     * Crea la fila de encabezados en una fila específica
     */
    private void createHeaderRow(Sheet sheet, List<FieldColumnInfo> columnInfos, CellStyle headerStyle, int rowNum) {
        Row headerRow = sheet.createRow(rowNum);
        headerRow.setHeight((short) 500);

        for (int i = 0; i < columnInfos.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnInfos.get(i).columnName());
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Crea las filas de datos
     */
    private <T> void createDataRows(Sheet sheet, List<T> data, List<FieldColumnInfo> columnInfos, CellStyle dataStyle) {
        createDataRows(sheet, data, columnInfos, dataStyle, 1);
    }

    /**
     * Crea las filas de datos empezando desde una fila específica
     */
    private <T> void createDataRows(Sheet sheet, List<T> data, List<FieldColumnInfo> columnInfos, CellStyle dataStyle, int startRow) {
        int rowNum = startRow;
        
        for (T item : data) {
            Row row = sheet.createRow(rowNum++);
            
            for (int i = 0; i < columnInfos.size(); i++) {
                Cell cell = row.createCell(i);
                FieldColumnInfo columnInfo = columnInfos.get(i);
                
                try {
                    Object value = columnInfo.field().get(item);
                    setCellValue(cell, value);
                    cell.setCellStyle(dataStyle);
                } catch (IllegalAccessException e) {
                    cell.setCellValue("");
                    cell.setCellStyle(dataStyle);
                }
            }
        }
    }
    
    @Override
    public <T> ByteArrayOutputStream exportToExcelWithHeader(List<T> data, Class<T> clazz, String sheetName, List<String> headerInfo) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Obtener campos anotados con @ExcelColumn
            List<FieldColumnInfo> columnInfos = getAnnotatedFields(clazz);

            if (columnInfos.isEmpty()) {
                throw new IllegalArgumentException("La clase " + clazz.getName() + " no tiene campos anotados con @ExcelColumn");
            }

            // Crear estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle infoStyle = createInfoStyle(workbook);

            int currentRow = 0;

            // Crear filas de información de encabezado
            if (headerInfo != null && !headerInfo.isEmpty()) {
                for (String info : headerInfo) {
                    Row infoRow = sheet.createRow(currentRow++);
                    Cell infoCell = infoRow.createCell(0);
                    infoCell.setCellValue(info);
                    infoCell.setCellStyle(infoStyle);
                }
                // Línea en blanco después del encabezado
                currentRow++;
            }

            // Crear fila de encabezados de columnas
            createHeaderRow(sheet, columnInfos, headerStyle, currentRow++);

            // Crear filas de datos
            createDataRows(sheet, data, columnInfos, dataStyle, currentRow);

            // Ajustar ancho de columnas
            autoSizeColumns(sheet, columnInfos.size());

            // Escribir a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream;

        } catch (IOException e) {
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }

    /**
     * Crea el estilo para las celdas de información
     */
    private CellStyle createInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Establece el valor de una celda según el tipo de dato
     * Detecta automáticamente si un String es numérico
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            String stringValue = value.toString();
            // Intentar parsear como número
            if (isNumeric(stringValue)) {
                try {
                    double numericValue = Double.parseDouble(stringValue);
                    cell.setCellValue(numericValue);
                } catch (NumberFormatException e) {
                    cell.setCellValue(stringValue);
                }
            } else {
                cell.setCellValue(stringValue);
            }
        }
    }

    @Override
    public <T> ByteArrayOutputStream exportToExcelWithSideInfo(List<T> data, Class<T> clazz, String sheetName, List<String> sideInfo, List<String> fieldsToInclude) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Obtener campos anotados con @ExcelColumn
            List<FieldColumnInfo> allColumnInfos = getAnnotatedFields(clazz);

            if (allColumnInfos.isEmpty()) {
                throw new IllegalArgumentException("La clase " + clazz.getName() + " no tiene campos anotados con @ExcelColumn");
            }

            // Filtrar columnas si se especifica fieldsToInclude
            List<FieldColumnInfo> columnInfos = allColumnInfos;
            if (fieldsToInclude != null && !fieldsToInclude.isEmpty()) {
                columnInfos = allColumnInfos.stream()
                    .filter(info -> fieldsToInclude.contains(info.field().getName()))
                    .collect(Collectors.toList());
            }

            // Crear estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle infoStyle = createInfoStyle(workbook);

            // Crear fila de encabezados de columnas
            createHeaderRow(sheet, columnInfos, headerStyle, 0);

            // Crear filas de datos
            createDataRows(sheet, data, columnInfos, dataStyle, 1);

            // Añadir información lateral en columna a la derecha
            if (sideInfo != null && !sideInfo.isEmpty()) {
                int infoColumn = columnInfos.size() + 1; // Columna después de los datos, con espacio
                for (int i = 0; i < sideInfo.size(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        row = sheet.createRow(i);
                    }
                    Cell infoCell = row.createCell(infoColumn);
                    infoCell.setCellValue(sideInfo.get(i));
                    infoCell.setCellStyle(infoStyle);
                }
            }

            // Ajustar ancho de columnas de datos
            autoSizeColumns(sheet, columnInfos.size());

            // Ajustar ancho de columna de información
            if (sideInfo != null && !sideInfo.isEmpty()) {
                int infoColumn = columnInfos.size() + 1;
                sheet.autoSizeColumn(infoColumn);
                int currentWidth = sheet.getColumnWidth(infoColumn);
                sheet.setColumnWidth(infoColumn, Math.max(currentWidth + 1000, 6000));
            }

            // Escribir a ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream;

        } catch (IOException e) {
            throw new RuntimeException("Error al generar archivo Excel", e);
        }
    }

    /**
     * Verifica si un string representa un valor numérico
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        // Eliminar espacios en blanco
        str = str.trim();
        // Verificar si es un número válido (permite negativos y decimales)
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * Ajusta automáticamente el ancho de las columnas
     */
    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Añadir un poco de padding adicional
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
        }
    }
    
    /**
     * Record para almacenar información de campo y columna
     */
    private record FieldColumnInfo(Field field, String columnName, int order) {
    }
}
