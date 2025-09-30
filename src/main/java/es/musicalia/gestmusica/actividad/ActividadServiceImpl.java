package es.musicalia.gestmusica.actividad;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActividadServiceImpl implements ActividadService {

    private final TarifaRepository tarifaRepository;
    private final OcupacionRepository ocupacionRepository;

	public ActividadServiceImpl(TarifaRepository tarifaRepository, OcupacionRepository ocupacionRepository){

        this.tarifaRepository = tarifaRepository;
        this.ocupacionRepository = ocupacionRepository;
    }


    @Override
    public List<ActividadRecord> findActividadTarifas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(1);

        return this.tarifaRepository.findActividadTarifasConConteo(fechaLimite);
    }

    @Override
    public List<ActividadRecord> findActividadOcupaciones() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(1);

        return this.ocupacionRepository.findActividadOcupacionesConConteo(fechaLimite);
    }
}
