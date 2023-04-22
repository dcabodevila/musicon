package es.musicalia.gestmusica.file;


import es.musicalia.gestmusica.agencia.*;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.FileUploadUtil;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;



@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

	public String guardarFichero(MultipartFile multipartFile, String uploadDir) throws IOException {
		if (multipartFile !=null && !multipartFile.isEmpty()){
			String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
			saveFile(uploadDir, fileName, multipartFile);
			return uploadDir.concat("/").concat(fileName);
		}
		return null;
	}

	public String guardarFicheroYEliminarAnteriores(MultipartFile multipartFile, String uploadDir) throws IOException {

		if (multipartFile !=null && !multipartFile.isEmpty()){
			deleteAllFilesInFolder(uploadDir);
			String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
			saveFile(uploadDir, fileName, multipartFile);
			return uploadDir.concat("/").concat(fileName);
		}
		return null;
	}

	@CacheEvict(value = "files", key = "#pathFileName")
	private void saveFile(String uploadDir, String fileName,
								MultipartFile multipartFile) throws IOException {
		Path uploadPath = Paths.get(uploadDir);

		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		try (InputStream inputStream = multipartFile.getInputStream()) {
			Path filePath = uploadPath.resolve(fileName);
			Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ioe) {
			throw new IOException("Could not save image file: " + fileName, ioe);
		}
	}
	@Cacheable(value = "files", key = "#pathFileName")
	public byte[] getImageFileBytes(String pathFileName) throws IOException {
		Path filePath = Paths.get("image/".concat(pathFileName));
		byte[] fileBytes = Files.readAllBytes(filePath);
		return fileBytes;
	}

	@Cacheable(value = "files", key = "#pathFileName")
	public byte[] getFileBytes(String path, String pathFileName) throws IOException {
		Path filePath = Paths.get(path.concat(pathFileName));
		byte[] fileBytes = Files.readAllBytes(filePath);
		return fileBytes;
	}


	private void deleteAllFilesInFolder(String folder) throws IOException {
		Path directory = Paths.get(folder);
		Files.list(directory)
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
	}


}




