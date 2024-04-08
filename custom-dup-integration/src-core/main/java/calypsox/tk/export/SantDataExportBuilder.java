package calypsox.tk.export;

import com.calypso.tk.export.AbstractDataExporter;
import com.calypso.tk.export.DataExporterConfig;

public class SantDataExportBuilder extends com.calypso.tk.export.DataExportBuilder {

	public SantDataExportBuilder(AbstractDataExporter dataExporter) {
		super(dataExporter);
	}

	public SantDataExportBuilder(DataExporterConfig dataExporterConfig, String format) {
		super(dataExporterConfig, format);
	}

	@Override
	public void sendUpdateData() {
		super.sendUpdateData();
	}

}
