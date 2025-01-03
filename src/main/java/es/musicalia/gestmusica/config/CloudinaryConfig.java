package es.musicalia.gestmusica.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        Cloudinary cloudinary = new Cloudinary(System.getenv("CLOUDINARY_URL"));
        cloudinary.config.secure = true;
        return cloudinary;
    }

}
