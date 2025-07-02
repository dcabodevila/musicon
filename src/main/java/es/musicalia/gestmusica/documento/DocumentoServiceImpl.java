package es.musicalia.gestmusica.documento;


import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class DocumentoServiceImpl implements DocumentoService {

	private final ArtistaRepository artistaRepository;
	private final DocumentoRepository documentoRepository;
	private final FileService fileService;
	private final UserService userService;


	public DocumentoServiceImpl(ArtistaRepository artistaRepository, DocumentoRepository documentoRepository, FileService fileService, UserService userService) {
		this.artistaRepository = artistaRepository;
		this.documentoRepository = documentoRepository;
		this.fileService = fileService;
        this.userService = userService;
    }


	@Transactional
	@Override
	public void guardarDocumento(MultipartFile multipartFile, DocumentoDto documentoDto) {
		final CloudinaryUploadResponse response = this.fileService.guardarFicheroPrivado(multipartFile);

		if (response.getPublicId() != null) {
			Documento documento = new Documento();
			documento.setNombre(multipartFile.getOriginalFilename());
			documento.setUrl(response.getPublicId());
			documento.setResourceType(response.getResourceType());
			documento.setActivo(true);
			documento.setFechaCreacion(java.time.LocalDateTime.now());
			documento.setUsuarioCreacion(this.userService.obtenerUsuarioAutenticado());
			documento.setArtista(artistaRepository.findById(documentoDto.getIdArtista()).orElseThrow(() -> new EntityNotFoundException("Artista no encontrado con ID: " + documentoDto.getIdArtista())));

			documentoRepository.save(documento);
		}
	}

	@Override
	public List<Documento> findByArtistaId(Long idArtista) {
		return documentoRepository.findByArtistaId(idArtista).orElseGet(List::of);
	}

	@Transactional
	@Override
	public void deleteById(Long id) {
		documentoRepository.findById(id).ifPresent(documento -> {
			fileService.deletePrivateFile(documento.getUrl(), documento.getResourceType());
			documentoRepository.deleteById(id);
		});
	}

	@Override
	public Documento findById(Long id) {
		return documentoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Documento no encontrado con ID: " + id));
	}

}
