package es.musicalia.gestmusica.file;


import es.musicalia.gestmusica.cloudinary.CloudinaryException;
import es.musicalia.gestmusica.cloudinary.CloudinaryService;
import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {
	private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

	private final CloudinaryService cloudinaryService;

    public FileServiceImpl(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }


	@Override
    public String guardarFichero(MultipartFile multipartFile) {

		if (multipartFile !=null && !multipartFile.isEmpty()){
			CloudinaryUploadResponse response = this.saveCloudinaryFile(multipartFile);
			return response.getUrl();
		}
		return null;
	}



	private CloudinaryUploadResponse saveCloudinaryFile(MultipartFile multipartFile) throws CloudinaryException {
		try {
			// Convierte MultipartFile a File temporal
			File tempFile = File.createTempFile("temp", multipartFile.getOriginalFilename());
			multipartFile.transferTo(tempFile);

			// Subir archivo a Cloudinary
			CloudinaryUploadResponse uploadResult = cloudinaryService.uploadFile(tempFile);

			// Elimina el archivo temporal
			tempFile.delete();

			return uploadResult;
		} catch (Exception e) {
			logger.error("Error al subir archivo a Cloudinary", e);
			throw new CloudinaryException("Error al subir archivo a Cloudinary", e);
		}
	}

	@Cacheable(value = "files", key = "#pathFileName")
	public byte[] getImageFileBytes(String pathFileName) throws IOException {
		Path filePath = Paths.get("image/", pathFileName);

		// Validar si el archivo existe antes de intentar leerlo
		if (!Files.exists(filePath)) {
			throw new FileNotFoundException("El archivo no existe: " + filePath);
		}

		// Usar try-with-resources para asegurar la liberación de recursos
		try (InputStream inputStream = Files.newInputStream(filePath)) {
			return inputStream.readAllBytes();
		}
	}



}




