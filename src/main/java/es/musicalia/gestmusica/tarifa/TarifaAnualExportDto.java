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
    @ExcelColumn(value = "Enero", order = 1)
    private String enero;

    @ExcelColumn(value = "Ene", order = 2)
    private String eneroValor;

    // Febrero
    @ExcelColumn(value = "Febrero", order = 3)
    private String febrero;

    @ExcelColumn(value = "Feb", order = 4)
    private String febreroValor;

    // Marzo
    @ExcelColumn(value = "Marzo", order = 5)
    private String marzo;

    @ExcelColumn(value = "Mar", order = 6)
    private String marzoValor;

    // Abril
    @ExcelColumn(value = "Abril", order = 7)
    private String abril;

    @ExcelColumn(value = "Abr", order = 8)
    private String abrilValor;

    // Mayo
    @ExcelColumn(value = "Mayo", order = 9)
    private String mayo;

    @ExcelColumn(value = "May", order = 10)
    private String mayoValor;

    // Junio
    @ExcelColumn(value = "Junio", order = 11)
    private String junio;

    @ExcelColumn(value = "Jun", order = 12)
    private String junioValor;

    // Julio
    @ExcelColumn(value = "Julio", order = 13)
    private String julio;

    @ExcelColumn(value = "Jul", order = 14)
    private String julioValor;

    // Agosto
    @ExcelColumn(value = "Agosto", order = 15)
    private String agosto;

    @ExcelColumn(value = "Ago", order = 16)
    private String agostoValor;

    // Septiembre
    @ExcelColumn(value = "Septiembre", order = 17)
    private String septiembre;

    @ExcelColumn(value = "Sep", order = 18)
    private String septiembreValor;

    // Octubre
    @ExcelColumn(value = "Octubre", order = 19)
    private String octubre;

    @ExcelColumn(value = "Oct", order = 20)
    private String octubreValor;

    // Noviembre
    @ExcelColumn(value = "Noviembre", order = 21)
    private String noviembre;

    @ExcelColumn(value = "Nov", order = 22)
    private String noviembreValor;

    // Diciembre
    @ExcelColumn(value = "Diciembre", order = 23)
    private String diciembre;

    @ExcelColumn(value = "Dic", order = 24)
    private String diciembreValor;
}
