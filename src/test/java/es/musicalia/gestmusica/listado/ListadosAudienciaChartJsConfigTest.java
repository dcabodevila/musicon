package es.musicalia.gestmusica.listado;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ListadosAudienciaChartJsConfigTest {

    private static final Path SCRIPT_PATH = Path.of("src/main/resources/static/js/listados-audiencia.js");

    @Test
    void audienciasChart_runtimeConfig_defineBaselineCeroExplicito() throws IOException {
        String script = Files.readString(SCRIPT_PATH);

        Pattern baselinePattern = Pattern.compile(
                "scales\\s*:\\s*\\{[\\s\\S]*?yAxes\\s*:\\s*\\[[\\s\\S]*?beginAtZero\\s*:\\s*true[\\s\\S]*?min\\s*:\\s*0",
                Pattern.MULTILINE
        );

        assertThat(baselinePattern.matcher(script).find())
                .as("El gráfico web de audiencias debe declarar beginAtZero:true y min:0 en el eje Y")
                .isTrue();
    }

    @Test
    void audienciasChart_flujoActualizacion_consumeEndpointChartData() throws IOException {
        String script = Files.readString(SCRIPT_PATH);

        assertThat(script)
                .contains("url: '/listado/audiencias/aggregated-data'")
                .contains("reinitializeChart();");
    }
}
