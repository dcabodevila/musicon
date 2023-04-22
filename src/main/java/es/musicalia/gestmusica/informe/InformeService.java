package es.musicalia.gestmusica.informe;


import java.util.Map;

public interface InformeService {

    byte[] imprimirInforme(Map<String, Object> parametros, String fileNameToExport, String fileReport);

}
