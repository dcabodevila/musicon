package es.musicalia.gestmusica.cloudinary;

import com.cloudinary.ArchiveParams;
import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;


    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public CloudinaryUploadResponse uploadFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapToUploadResponse(cloudinary.uploader().upload(file, getParams()));

    }

    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, getParams());
    }

    private Map getParams(){
        return ObjectUtils.asMap(
                "use_filename", true,
                "unique_filename", false,
                "overwrite", true
        );
    }

    public CloudinaryUploadResponse mapToUploadResponse(Map<String, Object> uploadResult) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(uploadResult, CloudinaryUploadResponse.class);
    }

    public CloudinaryUploadResponse uploadPrivateFile(File file) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "resource_type", "raw",
                    "type", "private", // Marca el archivo como privado
                    "access_mode", "authenticated", // Requiere autenticación para acceder
                    "folder", "documentos" // Organiza en carpetas
            );

            Map<?, ?> result = cloudinary.uploader().upload(file, uploadParams);

            return CloudinaryUploadResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .resourceType((String) result.get("resource_type"))
                    .format((String) result.get("format"))
                    .build();

        } catch (Exception e) {
            throw new CloudinaryException("Error al subir archivo privado", e);
        }
    }


    public byte[] downloadPrivateFile(String publicId, String resourceType) {
        try {
            // Obtener información del archivo
            ApiResponse resource = cloudinary.api().resource(publicId,
                    ObjectUtils.asMap(
                            "resource_type", "raw",  // Usar "raw" ya que así los subes
                            "type", "private"        // IMPORTANTE: especificar que es privado
                    ));


            String secureUrl = (String) resource.get("secure_url");

            // Descargar usando la URL segura
            URL url = new URL(secureUrl);
            return url.openStream().readAllBytes();

        } catch (Exception e) {
            log.error("Error al descargar archivo privado: " + publicId, e);
            throw new CloudinaryException("Error al descargar archivo privado", e);
        }
    }

    public Map deletePrivateFile(String publicId, String resourceType) throws IOException {
        return cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("type", "private", "resource_type", resourceType));
    }




}
