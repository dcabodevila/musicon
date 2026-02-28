package es.musicalia.gestmusica.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar campos que deben ser exportados a Excel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelColumn {
    
    /**
     * Nombre de la columna en el archivo Excel
     */
    String value();
    
    /**
     * Orden de la columna (menor = más a la izquierda)
     */
    int order() default 999;
}
