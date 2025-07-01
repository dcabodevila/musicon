package es.musicalia.gestmusica.mail;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {

    @Email(message = "El email de destino no es válido")
    @NotBlank(message = "El email de destino es obligatorio")
    private String to;

    private List<String> cc;

    private List<String> bcc;

    @NotBlank(message = "El asunto es obligatorio")
    private String subject;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    private String plainContent;

    @Builder.Default
    private boolean isHtml = false;

    private List<String> attachments;
}
