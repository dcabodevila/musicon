package es.musicalia.gestmusica.listado;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ListadoChartDataFactory {

    public List<Map<String, Object>> from(List<ListadosPorMesDto> listadosPorMes) {
        if (listadosPorMes == null || listadosPorMes.isEmpty()) {
            return defaultData();
        }

        return listadosPorMes.stream()
                .map(dto -> Map.<String, Object>of(
                        "mes", dto.getMes(),
                        "cantidad", dto.getCantidad()))
                .toList();
    }

    private List<Map<String, Object>> defaultData() {
        return List.of(Map.of(
                "mes", "Sin datos",
                "cantidad", 0L
        ));
    }
}
