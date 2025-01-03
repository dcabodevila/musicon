package es.musicalia.gestmusica.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {


    String guardarFichero(MultipartFile multipartFile);
    byte[] getImageFileBytes(String pathFileName) throws IOException;
}
