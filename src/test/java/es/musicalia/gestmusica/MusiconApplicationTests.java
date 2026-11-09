package es.musicalia.gestmusica;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
		"mailgun.api-key=test-key",
		"orquestas.api.username=test-user",
		"orquestas.api.password=test-password"
})
@ActiveProfiles("dev")
class MusiconApplicationTests {

	@MockitoBean
	private Cloudinary cloudinary;

	@Test
	void contextLoads() {
	}

}
