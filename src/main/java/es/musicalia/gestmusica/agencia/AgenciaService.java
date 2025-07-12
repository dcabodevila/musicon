package es.musicalia.gestmusica.agencia;

import java.util.List;
import java.util.Set;

public interface AgenciaService {
    List<AgenciaRecord> findAllAgenciasForUser();

    List<AgenciaRecord> listaAgenciasRecordActivasTarifasPublicas();

    Agencia saveAgencia(AgenciaDto agenciaDto);

    AgenciaRecord findAgenciaRecordById(Long idAgencia);

    List<AgenciaRecord> findMisAgencias(Set<Long> idsMisAgencias);

    List<AgenciaRecord> findOtrasAgencias(Set<Long> idsMisAgencias);

    AgenciaDto findAgenciaDtoById(Long idAgencia);

}
