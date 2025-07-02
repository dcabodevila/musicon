package es.musicalia.gestmusica.file;


import es.musicalia.gestmusica.cloudinary.CloudinaryException;
import es.musicalia.gestmusica.cloudinary.CloudinaryService;
import es.musicalia.gestmusica.cloudinary.CloudinaryUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

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
			log.error("Error al subir archivo a Cloudinary", e);
			throw new CloudinaryException("Error al subir archivo a Cloudinary", e);
		}
	}


	@Override
	public void deleteFile(String pathFileName) {
		try {
			cloudinaryService.deleteFile(pathFileName);
		} catch (Exception e) {
			log.error("Error al eliminar archivo de Cloudinary", e);
			throw new CloudinaryException("Error al eliminar archivo de Cloudinary", e);
		}
	}

	@Override
	public byte[] getPrivateFileBytes(String publicId, String resourceType) {
		try {
			return cloudinaryService.downloadPrivateFile(publicId, resourceType);
		} catch (Exception e) {
			log.error("Error al descargar archivo privado: " + publicId, e);
			throw new CloudinaryException("Error al descargar archivo privado", e);
		}
	}

	@Override
	public void deletePrivateFile(String publicId, String resourceType) {
		try {
			cloudinaryService.deletePrivateFile(publicId, resourceType);
			log.info("Archivo privado eliminado: {}", publicId);
		} catch (Exception e) {
			log.error("Error al eliminar archivo privado: " + publicId, e);
			throw new CloudinaryException("Error al eliminar archivo privado", e);
		}
	}



	@Override
	public CloudinaryUploadResponse guardarFicheroPrivado(MultipartFile multipartFile) {
		if (multipartFile != null && !multipartFile.isEmpty()) {
			return this.savePrivateCloudinaryFile(multipartFile);
		}
		return null;
	}

	private CloudinaryUploadResponse savePrivateCloudinaryFile(MultipartFile multipartFile) throws CloudinaryException {
		try {
			File tempFile = File.createTempFile("temp", multipartFile.getOriginalFilename());
			multipartFile.transferTo(tempFile);

			CloudinaryUploadResponse uploadResult = cloudinaryService.uploadPrivateFile(tempFile);
			tempFile.delete();

			log.info("Archivo privado subido: {}", uploadResult);
			return uploadResult;

		} catch (Exception e) {
			log.error("Error al subir archivo privado a Cloudinary", e);
			throw new CloudinaryException("Error al subir archivo privado a Cloudinary", e);
		}
	}


}




