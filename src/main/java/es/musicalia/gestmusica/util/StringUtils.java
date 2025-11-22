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

    public static String capitalizarPalabrasConMayusculas(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder();

        for (String word : input.split("\\s+")) {

            // ¿Contiene alguna mayúscula?
            boolean contieneMayuscula = word.chars()
                    .anyMatch(Character::isUpperCase);

            if (contieneMayuscula) {
                String normalizada =
                        word.substring(0, 1).toUpperCase() +
                                (word.length() > 1 ? word.substring(1).toLowerCase() : "");
                sb.append(normalizada);
            } else {
                sb.append(word);
            }

            sb.append(" ");
        }

        return sb.toString().trim();
    }

}
