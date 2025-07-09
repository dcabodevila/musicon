package es.musicalia.gestmusica.informe;

import es.musicalia.gestmusica.listado.TipoReportEnum;
import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InformeServiceImpl implements InformeService {

	private final DataSource dataSource;
	private JasperReport compiledReportTarifaAnual;
	private JasperReport compiledReportListadoSinOcupacion;
	private JasperReport compiledReportListadoSinOcupacionVertical;
	private JasperReport compiledReportListadoConOcupacion;
	private JasperReport compiledReportListadoConOcupacionVertical;

	public InformeServiceImpl(DataSource dataSource){
		this.dataSource =dataSource;
	}

	@PostConstruct
	public void init() throws IOException, JRException {
		try (InputStream reportStream = getClass().getResourceAsStream("/".concat(TipoReportEnum.TARIFA_CON_OCUPACION_HORIZONTAL.getNombreFicheroReport()))) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportTarifaAnual = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/".concat(TipoReportEnum.LISTADO_SIN_OCUPACION_HORIZONTAL.getNombreFicheroReport()) )) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoSinOcupacion = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/".concat(TipoReportEnum.LISTADO_SIN_OCUPACION_VERTICAL.getNombreFicheroReport()))) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoSinOcupacionVertical = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/".concat(TipoReportEnum.LISTADO_CON_OCUPACION_HORIZONTAL.getNombreFicheroReport()))) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoConOcupacion = JasperCompileManager.compileReport(reportStream);
		}
		try (InputStream reportStream = getClass().getResourceAsStream("/".concat(TipoReportEnum.LISTADO_CON_OCUPACION_VERTICAL.getNombreFicheroReport()))) {
			if (reportStream == null) {
				throw new FileNotFoundException("No se encontró el reporte en el classpath");
			}
			// Se compila una sola vez
			compiledReportListadoConOcupacionVertical = JasperCompileManager.compileReport(reportStream);
		}

	}

	private JasperReport getCompiledReport(String fileReport) {

		switch (fileReport) {
			case  "tarifa_anual_horizontal_ocupacion.jrxml":
				return compiledReportTarifaAnual;
			case "listado_sin_ocupacion2.jrxml":
				return compiledReportListadoSinOcupacion;
			case "listado_con_ocupacion.jrxml":
				return compiledReportListadoConOcupacion;
			case "listado_con_ocupacion_vertical.jrxml":
				return compiledReportListadoConOcupacionVertical;
			case "listado_sin_ocupacion_vertical2.jrxml":
				return compiledReportListadoSinOcupacionVertical;
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
