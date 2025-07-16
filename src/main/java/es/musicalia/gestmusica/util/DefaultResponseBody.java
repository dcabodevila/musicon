package es.musicalia.gestmusica.util;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DefaultResponseBody {
    private String message;
    private boolean success;
    private String messageType;


}
