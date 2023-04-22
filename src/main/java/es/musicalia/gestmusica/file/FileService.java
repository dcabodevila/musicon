package es.musicalia.gestmusica.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {

    String guardarFichero(MultipartFile multipartFile, String uploadDir) throws IOException;
    String guardarFicheroYEliminarAnteriores(MultipartFile multipartFile, String uploadDir) throws IOException;
    byte[] getImageFileBytes(String pathFileName) throws IOException;
    byte[] getFileBytes(String path, String pathFileName) throws IOException;
}
