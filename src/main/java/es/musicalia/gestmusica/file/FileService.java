package es.musicalia.gestmusica.file;

import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {

    // ==================== MÉTODOS PÚBLICOS ====================
    
    /**
     * Guardar archivo público (imágenes, avatares, etc.)
     * Para archivos que pueden ser visibles públicamente
     */
    String guardarFichero(MultipartFile multipartFile);
    
    /**
     * Guardar archivo público y retornar respuesta completa
     */
    CloudinaryUploadResponse guardarFicheroPublico(MultipartFile multipartFile);

    // ==================== MÉTODOS PRIVADOS ====================
    
    /**
     * Guardar archivo privado en carpeta específica
     * Para documentos sensibles o privados
     */
    CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile);
    
    /**
     * Guardar archivo privado en carpeta personalizada
     */
    CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile, String carpeta);
    
    /**
     * Subir PDF específicamente (optimizado para documentos PDF)
     */
    CloudinaryUploadResponse uploadPdf(MultipartFile file);
    
    /**
     * Subir ZIP específicamente (optimizado para archivos comprimidos)
     */
    CloudinaryUploadResponse uploadZip(MultipartFile file);
    
    /**
     * Subir documento genérico (Word, Excel, etc.)
     */
    CloudinaryUploadResponse uploadDocumento(MultipartFile file, String carpeta);

    // ==================== MÉTODOS DE DESCARGA ====================
    
    /**
     * Obtener bytes de archivo privado
     */
    byte[] getPrivateFileBytes(String publicId, String resourceType, String contentType);
    
    /**
     * Obtener bytes de cualquier archivo (público o privado)
     */
    byte[] getFileBytes(String publicId, String resourceType);
    
    /**
     * Generar URL pública para acceso directo
     */
    String generatePublicUrl(String publicId, String resourceType);

    // ==================== MÉTODOS DE GESTIÓN ====================
    
    /**
     * Eliminar archivo
     */
    void deleteFile(String publicId, String resourceType);
    
    /**
     * Verificar si un archivo existe
     */
    boolean fileExists(String publicId, String resourceType);
    
    /**
     * Obtener información detallada del archivo
     */
    Map<String, Object> getFileInfo(String publicId, String resourceType);
}