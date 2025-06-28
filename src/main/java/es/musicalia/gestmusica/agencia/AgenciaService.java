package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.usuario.Usuario;
import java.util.List;
import java.util.Set;

public interface AgenciaService {
    List<AgenciaDto> findAllAgenciasForUser(final Usuario usuario);

    List<AgenciaRecord> listaAgenciasRecordActivasTarifasPublicas();

    Agencia saveAgencia(AgenciaDto agenciaDto);

    List<AgenciaDto> findMisAgencias(Set<Long> idsMisAgencias);

    List<AgenciaDto> findOtrasAgencias(Set<Long> idsMisAgencias);

    AgenciaDto findAgenciaDtoById(Long idAgencia);

}
