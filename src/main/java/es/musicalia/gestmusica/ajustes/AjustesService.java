package es.musicalia.gestmusica.ajustes;


import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.transaction.annotation.Transactional;

public interface AjustesService {


    @Transactional(readOnly = false)
    Ajustes saveAjustesDto(AjustesDto ajustesDto, Usuario usuario);

    AjustesDto getAjustesByIdUsuario(Long idUsuario);
}
