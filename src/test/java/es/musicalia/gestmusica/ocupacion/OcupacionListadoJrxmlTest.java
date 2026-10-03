package es.musicalia.gestmusica.ocupacion;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OcupacionListadoJrxmlTest {

    @Test
    void listadoOcupacionesJrxml_compilaConParametroYCampoDeImporte() throws Exception {
        try (InputStream reportStream = getClass().getClassLoader().getResourceAsStream("listado_ocupaciones.jrxml")) {
            assertThat(reportStream).isNotNull();

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            assertThat(jasperReport.getParameters())
                    .anySatisfy(parameter -> assertThat(parameter.getName()).isEqualTo("MOSTRAR_IMPORTES"));
            assertThat(jasperReport.getFields())
                    .anySatisfy(field -> assertThat(field.getName()).isEqualTo("importe"));
        }
    }
}
