package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.excel.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para exportar datos de tarifa anual a Excel
 * Representa una fila del calendario con tarifas y ocupaciones por día
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaAnualExportDto {

    // Campos no exportados - se usan para el encabezado
    private String nombre;
    private String agencia;

    // Enero
    @ExcelColumn(value = "", order = 1)
    private String enero;

    @ExcelColumn(value = "Enero", order = 2)
    private String eneroValor;

    // Febrero
    @ExcelColumn(value = "", order = 3)
    private String febrero;

    @ExcelColumn(value = "Febrero", order = 4)
    private String febreroValor;

    // Marzo
    @ExcelColumn(value = "", order = 5)
    private String marzo;

    @ExcelColumn(value = "Marzo", order = 6)
    private String marzoValor;

    // Abril
    @ExcelColumn(value = "", order = 7)
    private String abril;

    @ExcelColumn(value = "Abril", order = 8)
    private String abrilValor;

    // Mayo
    @ExcelColumn(value = "", order = 9)
    private String mayo;

    @ExcelColumn(value = "Mayo", order = 10)
    private String mayoValor;

    // Junio
    @ExcelColumn(value = "", order = 11)
    private String junio;

    @ExcelColumn(value = "Junio", order = 12)
    private String junioValor;

    // Julio
    @ExcelColumn(value = "", order = 13)
    private String julio;

    @ExcelColumn(value = "Julio", order = 14)
    private String julioValor;

    // Agosto
    @ExcelColumn(value = "", order = 15)
    private String agosto;

    @ExcelColumn(value = "Agosto", order = 16)
    private String agostoValor;

    // Septiembre
    @ExcelColumn(value = "", order = 17)
    private String septiembre;

    @ExcelColumn(value = "Septiembre", order = 18)
    private String septiembreValor;

    // Octubre
    @ExcelColumn(value = "", order = 19)
    private String octubre;

    @ExcelColumn(value = "Octubre", order = 20)
    private String octubreValor;

    // Noviembre
    @ExcelColumn(value = "", order = 21)
    private String noviembre;

    @ExcelColumn(value = "Noviembre", order = 22)
    private String noviembreValor;

    // Diciembre
    @ExcelColumn(value = "", order = 23)
    private String diciembre;

    @ExcelColumn(value = "Diciembre", order = 24)
    private String diciembreValor;
}
