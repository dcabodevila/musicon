package es.musicalia.gestmusica.reportes;

import es.musicalia.gestmusica.usuario.EnvioEmailException;

public interface ReporteService {

    void enviarReportePorIdAgencia(Long idAgencia) throws EnvioEmailException;
}
