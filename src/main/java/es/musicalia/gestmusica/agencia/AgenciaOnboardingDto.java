package es.musicalia.gestmusica.agencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el onboarding automático de agencias.
 * Simplificado para el flujo inicial de creación de agencia.
 */
@Data
public class AgenciaOnboardingDto {

    @NotBlank(message = "El nombre de la agencia es obligatorio")
    @Size(max = 250, message = "El nombre no puede exceder 250 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción de la agencia es obligatoria")
    @Size(max = 250, message = "La descripción no puede exceder 250 caracteres")
    private String descripcion;

    @NotNull(message = "La provincia es obligatoria")
    private Long idProvincia;

    private String telefono;
}
