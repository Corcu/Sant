package calypsox.tk.export;

import java.util.Map;

import com.github.dozermapper.core.loader.api.BeanMappingBuilder;

import com.calypso.apps.common.CalypsoMLConfiguration;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.apps.common.adapter.DefaultCalypsoSessionAdapter;
import com.calypso.apps.datamigration.adapter.EnhancedExporterAdapter;
import com.calypso.apps.datamigration.adapter.EnhancedImporterAdapter;
import com.calypso.apps.datamigration.adapter.ExporterAdapterV2;
import com.calypso.jaxb.bridge.Translator;
import com.calypso.processing.DefaultResourceManager;
import com.calypso.processing.Resource;
import com.calypso.processing.ResourceException;
import com.calypso.processing.ResourceManager;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.WarningMessage;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.service.BaseCalypsoConnection;
import com.calypso.tk.service.DefaultCalypsoSession;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.util.UploadFactory;
import com.calypso.tk.util.ConnectException;

public class CalypsoMLDataExporter extends AdviceDocUploaderXMLDataExporter {

	final String outputTS = Util.datetimeToString(new JDatetime(), "yyyyMMdd_HHmmssSSS");

	Map<String, String> additionalJMSAttributes;

	public static final String LOG_CATEGORY = "PositionDataExporter";

	protected static ExporterAdapterV2 exporterAdapter = null;
	protected static DefaultCalypsoSessionAdapter sourceSessionAdapter = new DefaultCalypsoSessionAdapter();
	protected static CalypsoMLConfiguration cmlConfig = null;
	protected static ResourceManager resourceManager = null;
	protected static boolean initialConfig = true;

	protected static void setExporterAdapter(ExporterAdapterV2 expAdapter) {
		exporterAdapter = expAdapter;
	}
	
	protected static void initConfig() throws AdapterException {
		cmlConfig.getXmlConfigurationEditorAdapter();
	}
	
	protected static DefaultCalypsoSessionAdapter getSourceSessionAdapter() throws AdapterException {
		return sourceSessionAdapter;
	}

	protected static ResourceManager getResourceManager() {
		if (resourceManager == null) {
			resourceManager = new DefaultResourceManager();
		}
		return resourceManager;
	}

	protected static Translator<Object, ?> getTranslator(Object objectToTranslate) throws AdapterException {
		Translator<Object, ?> translator = exporterAdapter.getConfigurationEditorAdapter().getJAXBConfigurationAdapter()
				.getConfiguration().getTranslator(objectToTranslate.getClass());
		if (translator == null) {
			Log.error(LOG_CATEGORY, "No translator available for class " + objectToTranslate.getClass());
		}
		return translator;
	}

	protected ExporterAdapterV2 buildExporterAdapter() throws AdapterException {
		if (exporterAdapter == null) {
			try {
				BaseCalypsoConnection connection = new BaseCalypsoConnection(this.getDs());
				DefaultCalypsoSession session = new DefaultCalypsoSession(connection);
				exporterAdapter = (ExporterAdapterV2) getCalypsoMLConfig().buildExporterAdapter(session);
			} catch (ConnectException e) {
				Log.error(LOG_CATEGORY, e);
			}
		}
		return exporterAdapter;
	}

	public static CalypsoMLConfiguration getCalypsoMLConfig() throws AdapterException {
		if (cmlConfig == null) {
			cmlConfig = new CalypsoMLConfiguration();
			try {
				ResourceManager e = getResourceManager();

				Resource migratorConfigResource = e.findResource("default-migrator-config.xml");
				Resource importerConfigResource = e.findResource("default-import-config.xml");
				Resource exporterConfigResource = e.findResource("default-export-config.xml");
				Resource identifiersConfigResource = e.findResource("default-identifiers-config.xml");

				if (migratorConfigResource.canRead()) {
					cmlConfig.addTranslatorsConfigurationFile(migratorConfigResource.getLocation());
				} else {
					cmlConfig.addTranslatorsConfigurationFile(importerConfigResource.getLocation());
					cmlConfig.addTranslatorsConfigurationFile(exporterConfigResource.getLocation());
				}
				cmlConfig.addPersistenceConfigurationFile(exporterConfigResource.getLocation());
				cmlConfig.addProcessorConfigurationFile(importerConfigResource.getLocation());
				cmlConfig.addIdentifiersConfigurationFile(identifiersConfigResource.getLocation());

				Resource dvFile = getResourceManager().findResource("default-domain-value-config.xml");
				cmlConfig.addDVFieldConfigurationFile(dvFile.getLocation());

				cmlConfig.loadSupportedModuleConfig();

			} catch (ResourceException arg6) {
				throw new AdapterException("Error during MigratorAdapter initialization.", arg6);
			} catch (AdapterException arg7) {
				throw new AdapterException("Error during MigratorAdapter initialization.", arg7);
			}
		}

		if (cmlConfig.getImporterAdapterClass() == null) {
			cmlConfig.setImporterAdapterClass(EnhancedImporterAdapter.class);
		}
		if (cmlConfig.getExporterAdapterClass() == null) {
			cmlConfig.setExporterAdapterClass(EnhancedExporterAdapter.class);
		}
		if (initialConfig) {
			initConfig();
			initialConfig = false;
		}
		return cmlConfig;
	}

	public CalypsoMLDataExporter(DataExporterConfig exporterConfig) {
		super(exporterConfig);
		this.setExportSource("CalypsoML");
	}

	public static BeanMappingBuilder getUploaderCalypsoBeanMappingBuilder(Trade trade) {
		Class<?> builderClass = UploadFactory.getUploaderCalypsoBeanMappingBuilderClass(trade.getProductType(),
				"TradeMappingBuilder");

		if (builderClass == null) {
			builderClass = UploadFactory.getUploaderCalypsoBeanMappingBuilderClass(trade.getProductFamily(),
					"TradeMappingBuilder");
		}

		return UploadFactory.getBeanMappingBuilderObject(builderClass);
	}

	private void createTradeData(Trade trade) {

		try {
			buildExporterAdapter();
			Translator<Object, ?> translator = getTranslator(trade);
			Object o = translator.translate(trade);
			this.setDataToSend(o.toString());
		} catch (AdapterException e) {
			Log.error(LOG_CATEGORY, e);
		} catch (IllegalArgumentException e) {
			Log.error(LOG_CATEGORY, e);
		} catch (WarningMessage e) {
			Log.warn(LOG_CATEGORY, e);
		} catch (ErrorMessage e) {
			Log.error(LOG_CATEGORY, e);
		}

	}

	@Override
	public void exportTrade(Trade trade) {
		super.exportTrade(trade);
		this.setIgnoreObject(false);
		createBOMessage();
		fillTradeInfo(trade);
    	this.getBoMessage().setStatus(Status.valueOf("TO_BE_SENT"));
		String fileName = "Trade" + "_" + this.getExportSource() + "_" + this.getExportFormat() + "_"
				+ CalypsoIDAPIUtil.getId(trade) + "_" + trade.getVersion() + "_" + this.outputTS;
		this.setFileName(fileName);
		this.createTradeData(trade);
	}
	
	protected boolean isObjectEligible(Object object) {
		return false;
	}

	@Override
	public void createUpddateBOMessage() {

	}


}
