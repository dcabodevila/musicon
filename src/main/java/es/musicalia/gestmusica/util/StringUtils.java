package es.musicalia.gestmusica.util;

public class StringUtils {

    public static String removeHttp(String input) {
        if (input.startsWith("http://")) {
            return input.substring(7);
        }
        else if (input.startsWith("https://")) {
            return input.substring(8);
        }
        return input;
    }
}
