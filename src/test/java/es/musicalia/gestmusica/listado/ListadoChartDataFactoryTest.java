package es.musicalia.gestmusica.listado;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListadoChartDataFactoryTest {

    private final ListadoChartDataFactory factory = new ListadoChartDataFactory();

    @Test
    void from_conDatos_mapeaMesYCantidad() {
        List<ListadosPorMesDto> input = List.of(
                new ListadosPorMesDto("Enero", 3L),
                new ListadosPorMesDto("Febrero", 7L)
        );

        List<Map<String, Object>> result = factory.from(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("mes", "Enero").containsEntry("cantidad", 3L);
        assertThat(result.get(1)).containsEntry("mes", "Febrero").containsEntry("cantidad", 7L);
    }

    @Test
    void from_sinDatos_devuelveFallbackSinDatosCero() {
        List<Map<String, Object>> result = factory.from(List.of());

        assertThat(result).containsExactly(Map.of("mes", "Sin datos", "cantidad", 0L));
    }
}
