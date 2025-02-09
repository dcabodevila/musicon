package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.usuario.Usuario;
import java.util.List;

public interface AgenciaService {
    List<AgenciaDto> findAllAgenciasForUser(final Usuario usuario);

    List<AgenciaRecord> listaAgenciasRecordActivasTarifasPublicas();

    Agencia saveAgencia(AgenciaDto agenciaDto);
    AgenciaDto findAgenciaDtoById(Long idAgencia);

}
