package es.musicalia.gestmusica.cloudinary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudinaryUploadResponse {

    private String publicId;
    private String version;
    private String url;
    private String secureUrl;
    private String assetFolder;

    @Override
    public String toString() {
        return "CloudinaryUploadResponse{" +
                "publicId='" + publicId + '\'' +
                ", version='" + version + '\'' +
                ", url='" + url + '\'' +
                ", secureUrl='" + secureUrl + '\'' +
                ", assetFolder='" + assetFolder + '\'' +
                '}';
    }

}
