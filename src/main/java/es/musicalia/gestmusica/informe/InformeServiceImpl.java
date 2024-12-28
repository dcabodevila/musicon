package es.musicalia.gestmusica.informe;

import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InformeServiceImpl implements InformeService {

	private DataSource dataSource;

	public InformeServiceImpl(DataSource dataSource){
		this.dataSource =dataSource;
	}

	public byte[] imprimirInforme(Map<String, Object> parametros, String fileNameToExport, String fileReport) {
		try (InputStream reportStream = getClass().getResourceAsStream("/" + fileReport)) {

			// Verifica si el archivo existe en el classpath
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el recurso en el classpath: " + fileReport);
			}

			// Compila el reporte a partir del InputStream
			JasperReport compiledReport = JasperCompileManager.compileReport(reportStream);

			// Llena el reporte con datos
			JasperPrint empReport = JasperFillManager.fillReport(
					compiledReport,
					parametros,
					dataSource.getConnection() // tu DataSource
			);

			// Retorna el PDF en un array de bytes
			return JasperExportManager.exportReportToPdf(empReport);

		} catch (JRException | SQLException | IOException e) {
			throw new RuntimeException(e);
		}
    }

}
