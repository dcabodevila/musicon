package es.musicalia.gestmusica.documento;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentoService {

    @Transactional
    void guardarDocumento(MultipartFile multipartFile, DocumentoDto documentoDto);

    List<Documento> findByArtistaId(Long idArtista);


    @Transactional
    void deleteById(Long id);

    Documento findById(Long id);
}
