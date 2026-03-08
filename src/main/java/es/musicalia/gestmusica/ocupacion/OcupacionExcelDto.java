package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.excel.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionExcelDto {

    @ExcelColumn(value = "ID", order = 1)
    private Long id;

    @ExcelColumn(value = "Artista", order = 2)
    private String artista;

    @ExcelColumn(value = "Fecha", order = 3)
    private String fecha;

    @ExcelColumn(value = "Localidad", order = 4)
    private String localidad;

    @ExcelColumn(value = "Municipio", order = 5)
    private String municipio;

    @ExcelColumn(value = "Provincia", order = 6)
    private String provincia;

    @ExcelColumn(value = "Matinal", order = 7)
    private String matinal;

    @ExcelColumn(value = "Solo Matinal", order = 8)
    private String soloMatinal;

    @ExcelColumn(value = "Nombre Comercial Representante", order = 9)
    private String nombreComercialRepresentante;

    @ExcelColumn(value = "Teléfono Representante", order = 10)
    private String telefonoRepresentante;
}
