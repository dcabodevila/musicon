package es.musicalia.gestmusica.ocupacion;

import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OcupacionListadoJrxmlTest {

    private static final String REPRESENTANTE = "Representante Test";

    @Test
    void listadoOcupacionesJrxml_compilaConParametroYCampoDeImporte() throws Exception {
        JasperReport jasperReport = compileReport();

        assertThat(jasperReport.getParameters())
                .anySatisfy(parameter -> assertThat(parameter.getName()).isEqualTo("MOSTRAR_IMPORTES"));
        assertThat(jasperReport.getFields())
                .anySatisfy(field -> assertThat(field.getName()).isEqualTo("importe"));
    }

    @Test
    void sinMostrarImportes_noImprimeColumnaImporteYRepresentanteOcupaElHueco() throws Exception {
        List<JRPrintText> textos = fillReport(false);

        assertThat(textos).noneMatch(texto -> "Importe".equals(texto.getFullText()));
        assertThat(textos).noneMatch(texto -> "1500".equals(texto.getFullText()));
        assertThat(textos)
                .filteredOn(texto -> REPRESENTANTE.equals(texto.getFullText()))
                .singleElement()
                .satisfies(texto -> assertThat(texto.getWidth()).isEqualTo(160));
    }

    @Test
    void conMostrarImportes_imprimeColumnaImporteJuntoARepresentante() throws Exception {
        List<JRPrintText> textos = fillReport(true);

        assertThat(textos).anyMatch(texto -> "Importe".equals(texto.getFullText()));
        assertThat(textos).anyMatch(texto -> "1500".equals(texto.getFullText()));
        assertThat(textos)
                .filteredOn(texto -> REPRESENTANTE.equals(texto.getFullText()))
                .singleElement()
                .satisfies(texto -> assertThat(texto.getWidth()).isEqualTo(110));
    }

    private JasperReport compileReport() throws Exception {
        try (InputStream reportStream = getClass().getClassLoader().getResourceAsStream("listado_ocupaciones.jrxml")) {
            assertThat(reportStream).isNotNull();
            return JasperCompileManager.compileReport(reportStream);
        }
    }

    private List<JRPrintText> fillReport(boolean mostrarImportes) throws Exception {
        OcupacionExcelDto ocupacion = OcupacionExcelDto.builder()
                .id(1L)
                .artista("Artista Test")
                .fecha("01/01/2027")
                .localidad("Localidad")
                .municipio("Municipio")
                .provincia("Provincia")
                .estado("Confirmado")
                .nombreComercialRepresentante(REPRESENTANTE)
                .telefonoRepresentante("600000000")
                .importe("1500")
                .build();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("MOSTRAR_IMPORTES", mostrarImportes);

        JasperPrint jasperPrint = JasperFillManager.fillReport(
                compileReport(), parametros, new JRBeanCollectionDataSource(List.of(ocupacion)));

        return jasperPrint.getPages().stream()
                .flatMap(page -> page.getElements().stream())
                .filter(JRPrintText.class::isInstance)
                .map(JRPrintText.class::cast)
                .toList();
    }
}
