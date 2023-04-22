package es.musicalia.gestmusica.informe;

import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
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
		try {
			String reportFile = "classpath:".concat(fileReport);

			JasperPrint empReport =
					JasperFillManager.fillReport
							(
									JasperCompileManager.compileReport(ResourceUtils.getFile(reportFile)
											.getAbsolutePath())
									, parametros
									, dataSource.getConnection()
							);

			return JasperExportManager.exportReportToPdf(empReport);
		} catch (JRException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
