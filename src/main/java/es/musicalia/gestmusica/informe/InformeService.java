package es.musicalia.gestmusica.informe;


import net.sf.jasperreports.engine.JRDataSource;

import java.util.Map;

public interface InformeService {

    byte[] imprimirInforme(Map<String, Object> parametros, String fileNameToExport, String fileReport);

    byte[] imprimirInformeConDataSource(Map<String, Object> parametros, String fileNameToExport, String fileReport, JRDataSource dataSource);

}
