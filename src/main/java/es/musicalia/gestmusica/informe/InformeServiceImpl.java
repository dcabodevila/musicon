package es.musicalia.gestmusica.informe;

import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InformeServiceImpl implements InformeService {

	private final DataSource dataSource;
	private JasperReport compiledReportTarifaAnual;
	private JasperReport compiledReportListadoSinOcupacion;
	private JasperReport compiledReportListadoConOcupacion;

	public InformeServiceImpl(DataSource dataSource){
		this.dataSource =dataSource;
	}

	@PostConstruct
	public void init() throws IOException, JRException {
		try (InputStream reportStream = getClass().getResourceAsStream("/tarifa_anual_horizontal_ocupacion.jrxml")) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportTarifaAnual = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/listado_sin_ocupacion2.jrxml")) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoSinOcupacion = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/listado_con_ocupacion.jrxml")) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoConOcupacion = JasperCompileManager.compileReport(reportStream);
		}
	}

	private JasperReport getCompiledReport(String fileReport) {
		switch (fileReport) {
			case "tarifa_anual_horizontal_ocupacion.jrxml":
				return compiledReportTarifaAnual;
			case "listado_sin_ocupacion2.jrxml":
				return compiledReportListadoSinOcupacion;
			case "listado_con_ocupacion.jrxml":
				return compiledReportListadoConOcupacion;
			default:
				throw new IllegalArgumentException("Reporte no soportado: " + fileReport);
		}
	}

	public byte[] imprimirInforme(Map<String, Object> parametros, String fileNameToExport, String fileReport) {

		JasperReport compiledReport = getCompiledReport(fileReport);
		try (Connection conn = dataSource.getConnection()) {
				// Llena el reporte con datos
				JasperPrint empReport = JasperFillManager.fillReport(
						compiledReport,
						parametros,
						conn
				);
				return JasperExportManager.exportReportToPdf(empReport);
			}


		catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

}
