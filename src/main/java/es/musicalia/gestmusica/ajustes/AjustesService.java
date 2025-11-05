package es.musicalia.gestmusica.ajustes;


import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AjustesService {


    @Transactional(readOnly = false)
    Ajustes saveAjustesDto(AjustesDto ajustesDto, Usuario usuario);

    AjustesDto getAjustesByIdUsuario(Long idUsuario);

    List<AjustesDto> getAllAjustesByIdUsuario(Long idUsuario);

    AjustesDto getAjustesById(Long id);

    AjustesDto getAjustesByIdAjuste(Long idAjuste);
}
