package es.musicalia.gestmusica.ocupacion;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OcupacionMostrarImportesTest {

    private final OcupacionController controller = new OcupacionController(
            null, null, null, null, null, null);

    @Test
    void toDataTableRow_mostrarImportesTrue_incluyeImporte() {
        OcupacionListRecord ocupacion = crearOcupacion("1500");

        Map<String, Object> row = controller.toDataTableRow(ocupacion, true);

        assertThat(row.get("importe")).isEqualTo("1500");
    }

    @Test
    void toDataTableRow_mostrarImportesFalse_noIncluyeImporte() {
        OcupacionListRecord ocupacion = crearOcupacion("1500");

        Map<String, Object> row = controller.toDataTableRow(ocupacion, false);

        assertThat(row.get("importe")).isEqualTo("");
    }

    private OcupacionListRecord crearOcupacion(String importe) {
        return new OcupacionListRecord(
                1L,
                LocalDateTime.now(),
                1L,
                "Artista",
                importe,
                true,
                "Boda",
                "Provincia",
                "Municipio",
                "Localidad",
                false,
                false,
                "Confirmado",
                1L,
                "Usuario",
                null,
                null,
                LocalDateTime.now());
    }
}
