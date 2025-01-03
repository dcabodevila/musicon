package es.musicalia.gestmusica.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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

}
