package es.musicalia.gestmusica.file;

import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {


    String guardarFichero(MultipartFile multipartFile);

    void deleteFile(String pathFileName);

    byte[] getPrivateFileBytes(String publicId, String resourceType);

    void deletePrivateFile(String publicId, String resourceType);

    CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile);
}
