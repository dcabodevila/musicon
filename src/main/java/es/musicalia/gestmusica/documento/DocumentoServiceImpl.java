package es.musicalia.gestmusica.documento;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
public class DocumentoServiceImpl implements DocumentoService {

    private final ArtistaRepository artistaRepository;
    private final DocumentoRepository documentoRepository;
    private final FileService fileService;
    private final UserService userService;

    public DocumentoServiceImpl(ArtistaRepository artistaRepository, DocumentoRepository documentoRepository, 
                               FileService fileService, UserService userService) {
        this.artistaRepository = artistaRepository;
        this.documentoRepository = documentoRepository;
        this.fileService = fileService;
        this.userService = userService;
    }

    @Transactional
    @Override
    public void guardarDocumento(MultipartFile multipartFile, DocumentoDto documentoDto, String contentType) {
        
        log.debug("Guardando documento con contentType: {}", contentType);
        
        CloudinaryUploadResponse response = determinarTipoSubida(multipartFile, contentType);
        
        log.debug("Respuesta de Cloudinary: {}", response);

        if (response != null && response.getPublicId() != null) {
            Documento documento = new Documento();
            documento.setNombre(multipartFile.getOriginalFilename());
            documento.setUrl(response.getPublicId());
            documento.setResourceType(response.getResourceType());
            documento.setActivo(true);
            documento.setFechaCreacion(java.time.LocalDateTime.now());
            documento.setUsuarioCreacion(this.userService.obtenerUsuarioAutenticado());
            documento.setArtista(artistaRepository.findById(documentoDto.getIdArtista())
                .orElseThrow(() -> new EntityNotFoundException("Artista no encontrado con ID: " + documentoDto.getIdArtista())));

            documentoRepository.save(documento);
            log.debug("Documento guardado en BD: {}", documento);
        } else {
            log.error("Error: respuesta de Cloudinary es null o sin publicId");
            throw new RuntimeException("Error al subir archivo a Cloudinary");
        }
    }

	@Override
	public List<Documento> findByArtistaId(Long idArtista) {
		return this.documentoRepository.findByArtistaIdAndActivoTrue(idArtista).orElse(List.of());
	}

	/**
     * Determina el tipo de subida según el contentType y la extensión del archivo
     */
    private CloudinaryUploadResponse determinarTipoSubida(MultipartFile multipartFile, String contentType) {
        String filename = multipartFile.getOriginalFilename();
        
        if (filename == null) {
            log.warn("Nombre de archivo es null, usando subida privada por defecto");
            return fileService.guardarFicheroPrivado(multipartFile);
        }
        
        String lowerFilename = filename.toLowerCase();
        
        // Verificar por contentType primero
        if ("application/pdf".equals(contentType)) {
            log.debug("Subiendo como PDF basado en contentType");
            return fileService.uploadPdf(multipartFile);
        }
        
        if ("application/zip".equals(contentType)) {
            log.debug("Subiendo como ZIP basado en contentType");
            return fileService.uploadZip(multipartFile);
        }
        
        // Verificar por extensión de archivo
        if (lowerFilename.endsWith(".pdf")) {
            log.debug("Subiendo como PDF basado en extensión");
            return fileService.uploadPdf(multipartFile);
        }
        
        if (lowerFilename.endsWith(".zip") || lowerFilename.endsWith(".rar") || lowerFilename.endsWith(".7z")) {
            log.debug("Subiendo como archivo comprimido basado en extensión");
            return fileService.uploadZip(multipartFile);
        }
        
        // Para otros tipos de documento
        if (esDocumentoOfice(lowerFilename)) {
            log.debug("Subiendo como documento de oficina");
            return fileService.uploadDocumento(multipartFile, "documentos/oficina");
        }
        
        // Por defecto, usar subida privada
        log.debug("Subiendo como archivo privado genérico");
        return fileService.guardarFicheroPrivado(multipartFile);
    }
    
    /**
     * Verifica si es un documento de oficina (Word, Excel, PowerPoint)
     */
    private boolean esDocumentoOfice(String filename) {
        return filename.endsWith(".doc") || filename.endsWith(".docx") ||
               filename.endsWith(".xls") || filename.endsWith(".xlsx") ||
               filename.endsWith(".ppt") || filename.endsWith(".pptx") ||
               filename.endsWith(".odt") || filename.endsWith(".ods") ||
               filename.endsWith(".odp");
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        documentoRepository.findById(id).ifPresent(documento -> {
//            fileService.deleteFile(documento.getUrl(), documento.getResourceType());
			documento.setActivo(false);
			documento.setUsuarioModificacion(this.userService.obtenerUsuarioAutenticado());
			documento.setFechaModificacion(java.time.LocalDateTime.now());
            documentoRepository.save(documento);

        });
    }

    @Override
    public Documento findById(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento no encontrado con ID: " + id));
    }
}