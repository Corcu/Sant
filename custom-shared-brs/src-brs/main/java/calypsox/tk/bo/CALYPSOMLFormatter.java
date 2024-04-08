package calypsox.tk.bo;

import calypsox.apps.bo.CalypsoMLTreeViewFrame;
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
import com.calypso.tk.bo.AbstractXMLFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.BaseCalypsoConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DefaultCalypsoSession;
import com.calypso.tk.upload.util.UploadFactory;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.SerialUtil;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

public class CALYPSOMLFormatter extends AbstractXMLFormatter {

	public static String LOG_CATEGORY = "CALYPSOMLFormatter";

	protected static ExporterAdapterV2 exporterAdapter = null;
	protected static DefaultCalypsoSessionAdapter sourceSessionAdapter = new DefaultCalypsoSessionAdapter();
	protected static CalypsoMLConfiguration cmlConfig = null;
	protected static ResourceManager resourceManager = null;
	protected static boolean initialConfig = true;

	protected static void setExporterAdapter(ExporterAdapterV2 expAdapter) {
		exporterAdapter = expAdapter;
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

	public static synchronized void initCalypsoMLConfiguration() throws AdapterException {
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


		if (cmlConfig.getImporterAdapterClass() == null) {
			cmlConfig.setImporterAdapterClass(EnhancedImporterAdapter.class);
		}
		if (cmlConfig.getExporterAdapterClass() == null) {
			cmlConfig.setExporterAdapterClass(EnhancedExporterAdapter.class);
		}

		cmlConfig.getXmlConfigurationEditorAdapter();

	}

	public static CalypsoMLConfiguration getCalypsoMLConfig() throws AdapterException {
		if (cmlConfig == null) {
			initCalypsoMLConfiguration();
		}
		return cmlConfig;
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


	protected ExporterAdapterV2 buildExporterAdapter(DSConnection ds) throws AdapterException {
		if (exporterAdapter == null) {
			try {
				BaseCalypsoConnection connection = new BaseCalypsoConnection(ds);
				DefaultCalypsoSession session = new DefaultCalypsoSession(connection);
				exporterAdapter = (ExporterAdapterV2) getCalypsoMLConfig().buildExporterAdapter(session);
			} catch (ConnectException e) {
				Log.error(LOG_CATEGORY, e);
			}
		}
		return exporterAdapter;
	}

	@Override
	public String format(PricingEnv env, BOMessage message, Trade trade, boolean newDocument, DSConnection dsCon) throws MessageFormatException {
		try {
			buildExporterAdapter(dsCon);
			DatatypeConverter.setDatatypeConverter(new SantDatatypeConverter());
			Trade tradeFromMessage = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
			Translator<Object, ?> translator = getTranslator(tradeFromMessage);
			String gateway = message.getGateway();
			fillTradeInfo(tradeFromMessage,gateway);
			Object o = translator.translate(tradeFromMessage);
			if(o!=null)
				return o.toString();
		} catch (AdapterException e) {
			Log.error(LOG_CATEGORY, e);
		} catch (IllegalArgumentException e) {
			Log.error(LOG_CATEGORY, e);
		} catch (WarningMessage e) {
			Log.warn(LOG_CATEGORY, e);
		} catch (ErrorMessage e) {
			Log.error(LOG_CATEGORY, e);
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, e);
		}
		return null;
	}

	@Override
	public boolean display(PricingEnv env, BOMessage message, DSConnection dsCon, AdviceDocument doc) throws MessageFormatException {
		   Object object = null;
		   if(doc.getIsBinary()) {
		      byte[] e = doc.getBinaryDocument();
		      try {
		         object = SerialUtil.bytes2object(e);
		      } catch (IOException arg8) {
		         Log.debug("UPLOADER", "SerialUtil.bytes2object failed, trying Util.bytes2object", arg8);
		         object = Util.bytes2object(e);
		      } catch (InterruptedException arg9) {
		         Log.error("UPLOADER", arg9);
		      }
		   } else {
		      StringBuffer e1 = doc.getDocument();
		      if(e1 != null) {
		         object = e1.toString();
		      }
		   }
		   try {
			   CalypsoMLTreeViewFrame e2 = new CalypsoMLTreeViewFrame(object, doc);
		      e2.setVisible(true);
		      return true;
		   } catch (Exception arg7) {
		      Log.error(this, arg7);
		      return false;
		   }
		}

		public void fillTradeInfo(Trade trade,String messageGateway){
			addGenericICKeywords(trade);
			if(null!=trade){
				if(trade.getProduct() instanceof PerformanceSwap){
					addPerformanceSwapKeywords(trade);
				}
			}
		}

	protected void addPerformanceSwapKeywords(Trade trade){
		if (trade.getProduct() instanceof PerformanceSwap){
			PerformanceSwap product = (PerformanceSwap) trade.getProduct();

			Fee slPremiumFee = getSLPremiumFee(trade);

			trade.addKeyword("P37BRConfirmationType", "Non-Electronic");
			trade.addKeyword("P37BRSInternal", Boolean.toString(isInternalDeal(trade)));
			trade.addKeyword("P37BRSSeniority", getSecCode(product));
			trade.addKeyword("P37BRSPremiumAmount",getPremiumFeeAmount(slPremiumFee));
			trade.addKeyword("P37BRSPremiumCurrency", slPremiumFee!=null?slPremiumFee.getCurrency():"");
		}
	}


	protected void addGenericICKeywords(Trade trade) {
		Book book = trade.getBook();
		if(book != null){
			LegalEntity le = book.getLegalEntity();
			if(le != null) {
				trade.addKeyword("ICProcessingOrg", le.getCode());
				trade.addKeyword("ICBranch", le.getCountry());
			}
			String desk = book.getAttribute("Desk");
			if(!Util.isEmpty(desk)){
                trade.addKeyword("ICDesk", desk);
            }
			else {
			    trade.addKeyword("ICDesk", "iN0T_3X15Ti");
            }
		}
        Product product = trade.getProduct();
		if(product != null){
		  if(product instanceof Equity){
              Equity equity = (Equity) product;
              String equityType = equity.getSecCode("EQUITY_TYPE");
              if(!Util.isEmpty(equityType) && (equityType.equalsIgnoreCase("CO2") || equityType.equalsIgnoreCase("VCO2") || equityType.equalsIgnoreCase("ETF"))) {
                  trade.addKeyword("ICProduct", equity.getSecCode("EQUITY_TYPE"));
              }
              if(!Util.isEmpty(equityType) && (equityType.equalsIgnoreCase("CO2") || equityType.equalsIgnoreCase("VCO2"))) {
                  trade.addKeyword("ICMarket", equity .getSecCode("Common"));
              }
              else if(!Util.isEmpty(equityType) && (equityType.equalsIgnoreCase("ETF"))) {
                  LegalEntity le = equity.getMarketPlace();
                  if(le!=null) {
                      trade.addKeyword("ICMarket", le.getCode());
                  }
              }
              // Sustituye el valor "Carbon" que finalmente no viene para Voluntarios CO2
              if(!Util.isEmpty(equityType) && (equityType.equalsIgnoreCase("VCO2"))) {
                  trade.addKeyword("Mx_Product_Type", "Carbon");
              }
		  }
		  else if(product instanceof PerformanceSwap){
              trade.addKeyword("ICProduct", "PerformanceSwap");
          }
          else if(product instanceof Bond){
              trade.addKeyword("ICProduct", "Bond");
          }
        }
	}


	public String getSecCode(PerformanceSwap product){
		return product.getReferenceProduct().getSecCode("MurexSENIORITY");
	}
	public String getPremiumFeeAmount(Fee fee){
		String rst = "";
		Fee selectedfee = fee;
		if (selectedfee != null) {
			NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
			DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
			decimalFormat.applyPattern("0.##");
			rst = decimalFormat.format(new Amount(Math.abs(selectedfee.getAmount()),2).get());
		}
		return rst;
	}

	public Fee getSLPremiumFee(Trade trade){
		JDate tradeEnteredDate = trade.getEnteredDate().getJDate(TimeZone.getDefault());
		JDate tradeDatePlus = trade.getEnteredDate().getJDate(TimeZone.getDefault()).addBusinessDays(3, Util.string2Vector("SYSTEM"));
		Fee selectedfee = null;
		Vector<Fee> fees = trade.getFees();
		if (!Util.isEmpty(fees)){
			for (Fee fee : fees) {
				if(fee.getType().equals("PREMIUM")) {
					if (fee.getKnownDate() != null
							&& fee.getKnownDate().equals(tradeEnteredDate)) {
						selectedfee = fee;
					} else if (fee.getFeeDate().gte(tradeEnteredDate)
							&& fee.getFeeDate().lte(tradeDatePlus)) {
						selectedfee = fee;
					}
				}
			}
		}
		//selectedfee.getAmount();
		//selectedfee.getCurrency();
		return selectedfee;

	}

	public boolean isInternalDeal(Trade trade) {
		LegalEntity le = trade.getCounterParty();
		if(le==null)
			return false;
		if (le.equals(trade.getBook().getLegalEntity()))
			return true;
		else
			return trade.getMirrorBook() != null
					&& trade.getMirrorBook().getLegalEntity().equals(le);
	}

}
