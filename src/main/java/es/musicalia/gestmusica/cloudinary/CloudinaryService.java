package es.musicalia.gestmusica.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;


    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Subir archivo público - ideal para imágenes de perfil, banners, etc.
     * Mantiene compatibilidad con el tier gratuito
     */
    public CloudinaryUploadResponse uploadPublicFile(File file) throws IOException {
        log.debug("Subiendo archivo público: {}", file.getName());
        
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "auto",
                "type", "upload",
                "access_mode", "public",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false
        );

        Map<?, ?> result = cloudinary.uploader().upload(file, params);
        return mapToUploadResponse(result);
    }

    /**
     * Subir archivo "pseudo-privado" - archivos en carpeta específica
     * pero técnicamente públicos para mantener el tier gratuito
     * Ideal para documentos, PDFs, etc.
     */
    public CloudinaryUploadResponse uploadPrivateFile(File file, String folder) throws IOException {
        log.debug("Subiendo archivo privado a carpeta '{}': {}", folder, file.getName());
        
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "raw", // Para documentos, PDFs, etc.
                "type", "upload",
                "access_mode", "public", // Mantenemos público para tier gratuito
                "folder", folder != null ? folder : "private",
                "use_filename", true,
                "unique_filename", true,
                "overwrite", false
        );

        Map<?, ?> result = cloudinary.uploader().upload(file, params);
        CloudinaryUploadResponse response = mapToUploadResponse(result);
        
        log.info("Archivo subido como pseudo-privado: {} en carpeta '{}'", 
                response.getPublicId(), folder);
        
        return response;
    }

    /**
     * Subir archivo raw (documentos, PDFs, etc.) con configuración específica
     */
    public CloudinaryUploadResponse uploadRawFile(File file, String folder) throws IOException {
        log.debug("Subiendo archivo raw a carpeta '{}': {}", folder, file.getName());
        
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", "raw",
                "type", "upload",
                "access_mode", "public",
                "folder", folder != null ? folder : "documents",
                "use_filename", true,
                "unique_filename", true
        );

        Map<?, ?> result = cloudinary.uploader().upload(file, params);
        return mapToUploadResponse(result);
    }

    /**
     * Descargar archivo por su public_id
     * Compatible con archivos "pseudo-privados" en tier gratuito
     */
    public byte[] downloadFile(String publicId, String resourceType) throws IOException {
        log.debug("Descargando archivo: {} (tipo: {})", publicId, resourceType);
        
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new IllegalArgumentException("El publicId no puede estar vacío");
        }

        String finalResourceType = (resourceType != null && !resourceType.isEmpty()) 
                ? resourceType : "raw";

        // Generar URL de descarga sin parámetros de autenticación
        String downloadUrl = cloudinary.url()
                .resourceType(finalResourceType)
                .type("upload")
                .secure(true)
                .generate(publicId);

        // Limpiar cualquier parámetro de autenticación que pueda haberse añadido
        downloadUrl = cleanAuthParams(downloadUrl);
        
        log.debug("URL de descarga generada: {}", downloadUrl);
        
        return downloadFromUrl(downloadUrl);
    }

    /**
     * Generar URL pública para acceso directo
     */
    public String generatePublicUrl(String publicId, String resourceType) {
        String finalResourceType = (resourceType != null && !resourceType.isEmpty()) 
                ? resourceType : "raw";
                
        return cloudinary.url()
                .resourceType(finalResourceType)
                .type("upload")
                .secure(true)
                .generate(publicId);
    }

    /**
     * Eliminar archivo de Cloudinary
     */
    public Map<String, Object> deleteFile(String publicId, String resourceType) throws IOException {
        log.debug("Eliminando archivo: {} (tipo: {})", publicId, resourceType);
        
        String finalResourceType = (resourceType != null && !resourceType.isEmpty()) 
                ? resourceType : "raw";
                
        Map<String, Object> result = cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", finalResourceType));
                
        log.info("Archivo eliminado: {} - Resultado: {}", publicId, result);
        return result;
    }

    /**
     * Verificar si un archivo existe en Cloudinary
     */
    public boolean fileExists(String publicId, String resourceType) {
        try {
            String finalResourceType = (resourceType != null && !resourceType.isEmpty()) 
                    ? resourceType : "raw";
                    
            Map<String, Object> params = ObjectUtils.asMap(
                    "resource_type", finalResourceType,
                    "type", "upload"
            );
            
            cloudinary.api().resource(publicId, params);
            return true;
        } catch (Exception e) {
            log.debug("Archivo no encontrado: {} - {}", publicId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtener información detallada de un archivo
     */
    public Map<String, Object> getFileInfo(String publicId, String resourceType) throws Exception {
        String finalResourceType = (resourceType != null && !resourceType.isEmpty()) 
                ? resourceType : "raw";
                
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", finalResourceType,
                "type", "upload"
        );
        
        return (Map<String, Object>) cloudinary.api().resource(publicId, params);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Limpiar parámetros de autenticación de la URL
     */
    private String cleanAuthParams(String url) {
        if (url.contains("?_a=")) {
            url = url.split("\\?_a=")[0];
        }
        if (url.contains("&_a=")) {
            url = url.split("&_a=")[0];
        }
        return url;
    }

    /**
     * Descargar archivo desde URL
     */
    private byte[] downloadFromUrl(String urlString) throws IOException {
        log.debug("Descargando desde URL: {}", urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // Configurar conexión
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "MusicGest-App/1.0");

        int responseCode = connection.getResponseCode();
        log.debug("Código de respuesta HTTP: {}", responseCode);

        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorMsg = String.format("Error HTTP %d al descargar: %s", responseCode, urlString);
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] data = inputStream.readAllBytes();
            log.debug("Descarga completada. Bytes: {}", data.length);
            return data;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Mapear resultado de Cloudinary a objeto de respuesta
     */
    private CloudinaryUploadResponse mapToUploadResponse(Map<?, ?> uploadResult) {
        log.debug("Mapeando resultado de Cloudinary: {}", uploadResult);
        
        return CloudinaryUploadResponse.builder()
                .publicId((String) uploadResult.get("public_id"))
                .version(String.valueOf(uploadResult.get("version")))
                .url((String) uploadResult.get("url"))
                .secureUrl((String) uploadResult.get("secure_url"))
                .resourceType((String) uploadResult.get("resource_type"))
                .format((String) uploadResult.get("format"))
                .type((String) uploadResult.get("type"))
                .build();
    }


}