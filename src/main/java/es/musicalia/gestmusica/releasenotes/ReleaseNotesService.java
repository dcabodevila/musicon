package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.usuario.Usuario;

public interface ReleaseNotesService {

    /**
     * Verifica si el usuario ha leído las release notes de una versión específica
     */
    boolean hasReadReleaseNotes(Long usuarioId, String version);

    /**
     * Marca las release notes de una versión como leídas por un usuario
     */
    void markAsRead(Long usuarioId, String version);

    /**
     * Obtiene la versión actual de la aplicación desde el pom.xml
     */
    String getCurrentVersion();

    /**
     * Obtiene el contenido HTML de las release notes de una versión específica
     */
    String getReleaseNotesContent(String version);

    /**
     * Obtiene el contenido HTML de las release notes filtrado por el rol del usuario
     * @param version La versión de las release notes
     * @param usuario El usuario autenticado
     * @param filterByRole Si es true, filtra el contenido por bloques FOR_ROLES; si es false, muestra todo
     */
    String getReleaseNotesContent(String version, Usuario usuario, boolean filterByRole);

    /**
     * Verifica si el usuario debe ver las release notes de una versión específica
     * basándose en su rol y los roles especificados en el frontmatter del archivo
     */
    boolean shouldShowReleaseNotes(Usuario usuario, String version);

    /**
     * Verifica si hay contenido aplicable para el usuario después de filtrar por rol
     * Retorna true si hay al menos una sección que el usuario puede ver
     */
    boolean hasApplicableContent(Usuario usuario, String version);
}
