package es.musicalia.gestmusica.file;

import es.musicalia.gestmusica.cloudinary.CloudinaryException;
import es.musicalia.gestmusica.cloudinary.CloudinaryService;
import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

    private final CloudinaryService cloudinaryService;
    
    // Extensiones válidas para archivos ZIP
    private static final List<String> ZIP_EXTENSIONS = Arrays.asList(".zip", ".rar", ".7z");

    public FileServiceImpl(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    @Override
    public String guardarFichero(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de guardar archivo vacío o nulo");
            return null;
        }

        CloudinaryUploadResponse response = guardarFicheroPublico(multipartFile);
        return response != null ? response.getSecureUrl() : null;
    }

    @Override
    public CloudinaryUploadResponse guardarFicheroPublico(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de guardar archivo público vacío o nulo");
            return null;
        }

        log.debug("Guardando archivo público: {}", multipartFile.getOriginalFilename());
        return executeWithTempFile(multipartFile, tempFile -> {
            try {
                return cloudinaryService.uploadPublicFile(tempFile);
            } catch (IOException e) {
                throw new CloudinaryException("Error al subir archivo público", e);
            }
        });
    }

    // ==================== MÉTODOS PRIVADOS ====================

    @Override
    public CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile) {
        return guardarFicheroPrivado(multipartFile, "private");
    }

    @Override
    public CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile, String carpeta) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de guardar archivo privado vacío o nulo");
            return null;
        }

        String carpetaFinal = (carpeta != null && !carpeta.trim().isEmpty()) ? carpeta : "private";
        log.debug("Guardando archivo privado: {} en carpeta: {}", 
                multipartFile.getOriginalFilename(), carpetaFinal);

        return executeWithTempFile(multipartFile, tempFile -> {
            try {
                return cloudinaryService.uploadPrivateFile(tempFile, carpetaFinal);
            } catch (IOException e) {
                throw new CloudinaryException("Error al subir archivo privado", e);
            }
        });
    }

    @Override
    public CloudinaryUploadResponse uploadPdf(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de subir PDF vacío o nulo");
            return null;
        }

        // Validar que sea un PDF
        String originalFilename = multipartFile.getOriginalFilename();
        if (!isValidPdf(originalFilename)) {
            log.warn("Archivo no es PDF: {}", originalFilename);
            throw new CloudinaryException("El archivo debe ser un PDF (.pdf)");
        }

        log.debug("Subiendo PDF: {}", originalFilename);
        return executeWithTempFile(multipartFile, tempFile -> {
            try {
                return cloudinaryService.uploadRawFile(tempFile, "documentos/pdf");
            } catch (IOException e) {
                throw new CloudinaryException("Error al subir PDF", e);
            }
        });
    }

    @Override
    public CloudinaryUploadResponse uploadZip(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de subir archivo comprimido vacío o nulo");
            return null;
        }

        // Validar que sea un archivo comprimido válido
        String originalFilename = multipartFile.getOriginalFilename();
        if (!isValidZip(originalFilename)) {
            log.warn("Archivo no es un formato comprimido válido: {}", originalFilename);
            throw new CloudinaryException("El archivo debe ser un formato comprimido válido (.zip, .rar, .7z)");
        }

        log.debug("Subiendo archivo comprimido: {}", originalFilename);
        return executeWithTempFile(multipartFile, tempFile -> {
            try {
                return cloudinaryService.uploadRawFile(tempFile, "documentos/comprimidos");
            } catch (IOException e) {
                throw new CloudinaryException("Error al subir archivo comprimido", e);
            }
        });
    }

    @Override
    public CloudinaryUploadResponse uploadDocumento(MultipartFile multipartFile, String carpeta) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("Intento de subir documento vacío o nulo");
            return null;
        }

        String carpetaFinal = (carpeta != null && !carpeta.trim().isEmpty()) ? carpeta : "documentos";
        log.debug("Subiendo documento: {} a carpeta: {}", 
                multipartFile.getOriginalFilename(), carpetaFinal);

        return executeWithTempFile(multipartFile, tempFile -> {
            try {
                return cloudinaryService.uploadRawFile(tempFile, carpetaFinal);
            } catch (IOException e) {
                throw new CloudinaryException("Error al subir documento", e);
            }
        });
    }

    // ==================== MÉTODOS DE DESCARGA ====================

    @Override
    public byte[] getPrivateFileBytes(String publicId, String resourceType, String contentType) {
        return getFileBytes(publicId, resourceType);
    }

    @Override
    public byte[] getFileBytes(String publicId, String resourceType) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new CloudinaryException("PublicId no puede estar vacío");
        }

        try {
            log.debug("Descargando archivo: {} (tipo: {})", publicId, resourceType);
            return cloudinaryService.downloadFile(publicId, resourceType);
        } catch (Exception e) {
            log.error("Error al descargar archivo: {} - {}", publicId, e.getMessage());
            throw new CloudinaryException("Error al descargar archivo", e);
        }
    }

    @Override
    public String generatePublicUrl(String publicId, String resourceType) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new CloudinaryException("PublicId no puede estar vacío");
        }

        try {
            return cloudinaryService.generatePublicUrl(publicId, resourceType);
        } catch (Exception e) {
            log.error("Error al generar URL pública: {} - {}", publicId, e.getMessage());
            throw new CloudinaryException("Error al generar URL pública", e);
        }
    }

    // ==================== MÉTODOS DE GESTIÓN ====================

    @Override
    public void deleteFile(String publicId, String resourceType) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new CloudinaryException("PublicId no puede estar vacío");
        }

        try {
            Map<String, Object> result = cloudinaryService.deleteFile(publicId, resourceType);
            log.info("Archivo eliminado: {} - Resultado: {}", publicId, result.get("result"));
        } catch (Exception e) {
            log.error("Error al eliminar archivo: {} - {}", publicId, e.getMessage());
            throw new CloudinaryException("Error al eliminar archivo", e);
        }
    }

    @Override
    public boolean fileExists(String publicId, String resourceType) {
        if (publicId == null || publicId.trim().isEmpty()) {
            return false;
        }

        try {
            return cloudinaryService.fileExists(publicId, resourceType);
        } catch (Exception e) {
            log.debug("Error al verificar existencia de archivo: {} - {}", publicId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getFileInfo(String publicId, String resourceType) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new CloudinaryException("PublicId no puede estar vacío");
        }

        try {
            return cloudinaryService.getFileInfo(publicId, resourceType);
        } catch (Exception e) {
            log.error("Error al obtener información del archivo: {} - {}", publicId, e.getMessage());
            throw new CloudinaryException("Error al obtener información del archivo", e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Validar si el archivo es un PDF
     */
    private boolean isValidPdf(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        return filename.toLowerCase().endsWith(".pdf");
    }

    /**
     * Validar si el archivo es un formato comprimido válido
     */
    private boolean isValidZip(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        String lowerFilename = filename.toLowerCase();
        return ZIP_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }

    /**
     * Ejecuta una operación con un archivo temporal, garantizando su limpieza
     */
    private CloudinaryUploadResponse executeWithTempFile(MultipartFile multipartFile, 
                                                         TempFileOperation operation) {
        File tempFile = null;
        try {
            // Crear archivo temporal
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = originalFilename != null ? 
                    originalFilename.substring(originalFilename.lastIndexOf('.')) : ".tmp";
            
            tempFile = File.createTempFile("upload_", suffix);
            multipartFile.transferTo(tempFile);

            // Ejecutar operación
            CloudinaryUploadResponse result = operation.execute(tempFile);
            log.debug("Operación completada exitosamente para: {}", originalFilename);
            
            return result;

        } catch (Exception e) {
            log.error("Error en operación con archivo temporal: {}", e.getMessage());
            throw new CloudinaryException("Error al procesar archivo", e);
        } finally {
            // Limpiar archivo temporal
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("No se pudo eliminar archivo temporal: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Interfaz funcional para operaciones con archivos temporales
     */
    @FunctionalInterface
    private interface TempFileOperation {
        CloudinaryUploadResponse execute(File tempFile) throws Exception;
    }

    // ==================== MÉTODOS DEPRECATED (COMPATIBILIDAD) ====================

    /**
     * @deprecated Este método sigue funcionando pero usa los nuevos métodos internamente
     */
    @Deprecated
    private CloudinaryUploadResponse saveCloudinaryFile(MultipartFile multipartFile) {
        return guardarFicheroPublico(multipartFile);
    }

    /**
     * @deprecated Este método sigue funcionando pero usa los nuevos métodos internamente
     */
    @Deprecated
    private CloudinaryUploadResponse savePrivateCloudinaryFile(MultipartFile multipartFile) {
        return guardarFicheroPrivado(multipartFile);
    }
}