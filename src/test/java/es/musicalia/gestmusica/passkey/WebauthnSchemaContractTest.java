package es.musicalia.gestmusica.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The primary EntityManagerFactory applies no camelCase-to-underscores naming strategy (DatabaseConfig), so every
 * WebAuthn entity field must name its column explicitly, and that column must exist in the manual SQL script that
 * Hibernate validates against on startup.
 */
class WebauthnSchemaContractTest {

    private static final Path SCRIPT = Path.of("sql/1.7.0/01_webauthn_passkeys.sql");

    @Test
    void webauthnUsuario_debeMapearExactamenteLasColumnasDelScript() throws IOException {
        assertThat(columnasMapeadas(WebauthnUsuario.class)).isEqualTo(columnasDelScript(WebauthnUsuario.class));
    }

    @Test
    void webauthnCredencial_debeMapearExactamenteLasColumnasDelScript() throws IOException {
        assertThat(columnasMapeadas(WebauthnCredencial.class)).isEqualTo(columnasDelScript(WebauthnCredencial.class));
    }

    private static Set<String> columnasMapeadas(Class<?> entity) {
        Set<String> columnas = new LinkedHashSet<>();
        for (Field field : entity.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            String nombre = column != null ? column.name() : joinColumn != null ? joinColumn.name() : "";
            assertThat(nombre)
                    .as("%s.%s must declare its column name explicitly", entity.getSimpleName(), field.getName())
                    .isNotBlank();
            columnas.add(nombre.toLowerCase(Locale.ROOT));
        }
        assertThat(entity.getDeclaredFields()).anyMatch(f -> f.isAnnotationPresent(Id.class));
        return columnas;
    }

    private static Set<String> columnasDelScript(Class<?> entity) throws IOException {
        Table table = entity.getAnnotation(Table.class);
        String sql = Files.readString(SCRIPT);
        Matcher create = Pattern.compile(
                "CREATE TABLE IF NOT EXISTS " + table.schema() + "\\." + table.name() + " \\((.*?)\\n\\);",
                Pattern.DOTALL).matcher(sql);
        assertThat(create.find()).as("CREATE TABLE for %s in %s", table.name(), SCRIPT).isTrue();
        Set<String> columnas = new LinkedHashSet<>();
        for (String linea : create.group(1).split("\\n")) {
            Matcher column = Pattern.compile("^\\s*([a-z_]+)\\s+").matcher(linea);
            if (column.find()) {
                columnas.add(column.group(1));
            }
        }
        return columnas;
    }
}
