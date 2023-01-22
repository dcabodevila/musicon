package es.musicalia.gestmusica.file;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaDto;
import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {

    String guardarFichero(MultipartFile multipartFile, String uploadDir) throws IOException;
    String guardarFicheroYEliminarAnteriores(MultipartFile multipartFile, String uploadDir) throws IOException;
    byte[] getFileBytes(String pathFileName) throws IOException;
}
