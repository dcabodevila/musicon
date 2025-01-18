package es.musicalia.gestmusica.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class GestmusicaUtils {

    public static boolean isUserAutheticated() {
        return !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
    }
}
