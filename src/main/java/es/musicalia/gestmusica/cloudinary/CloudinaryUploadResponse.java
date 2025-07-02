package es.musicalia.gestmusica.cloudinary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class CloudinaryUploadResponse {

    private String publicId;
    private String version;
    private String url;
    private String secureUrl;
    private String assetFolder;
    private String resourceType; // Agregar este campo
    private String format; // Agregar este campo
    private String type; // Agregar este campo (public, private, etc.)

    @Override
    public String toString() {
        return "CloudinaryUploadResponse{" +
                "publicId='" + publicId + '\'' +
                ", version='" + version + '\'' +
                ", url='" + url + '\'' +
                ", secureUrl='" + secureUrl + '\'' +
                ", assetFolder='" + assetFolder + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", format='" + format + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
