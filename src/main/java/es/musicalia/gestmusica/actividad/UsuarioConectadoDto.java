package es.musicalia.gestmusica.actividad;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.Duration;

@Data
public class UsuarioConectadoDto {
    private String nombre;
    private String nombreCompleto;
    private LocalDateTime ultimaActividad;
    private String sessionId;

    public String getEstado() {
        if (ultimaActividad == null) return "Desconocido";
        
        LocalDateTime ahora = LocalDateTime.now();
        long minutosInactivo = Duration.between(ultimaActividad, ahora).toMinutes();
        
        if (minutosInactivo < 5) {
            return "Activo";
        } else if (minutosInactivo < 15) {
            return "Inactivo";
        } else {
            return "Desconectado";
        }
    }
    
    public String getClaseEstado() {
        String estado = getEstado();
        switch (estado) {
            case "Activo": return "success";
            case "Inactivo": return "warning";
            default: return "danger";
        }
    }
    
    public String getTiempoInactividad() {
        if (ultimaActividad == null) return "Desconocido";
        
        LocalDateTime ahora = LocalDateTime.now();
        Duration duracion = Duration.between(ultimaActividad, ahora);
        long minutos = duracion.toMinutes();
        
        if (minutos < 1) {
            return "Menos de 1 minuto";
        } else if (minutos < 60) {
            return minutos + " minuto" + (minutos > 1 ? "s" : "");
        } else {
            long horas = duracion.toHours();
            return horas + " hora" + (horas > 1 ? "s" : "");
        }
    }
}
