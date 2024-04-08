package calypsox.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.mapping.core.UploaderContextProvider;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.IEAdapter;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventDataUploaderAck;
import calypsox.tk.event.PSEventProduct;
import calypsox.tk.export.SantDataExportBuilder;
import calypsox.tk.export.ack.BondDefICACKProcessor;
import calypsox.tk.export.ack.DUPAckProcessor;
import calypsox.tools.santfilesaver.SantFileSaver;
import calypsox.tools.santfilesaver.SantFileSaverException;

public class BondDefICExportEngine extends MultipleDestinationExportEngine {
    String EVENT_ACTION = "NEW";
    public static String ENGINE_NAME = "BondDefICExportEngine";
    
    private static String SANTFILESAVER_EXPORT_DIR = "export";
    private static String SANTFILESAVER_EXPORT_DIR_TO_SEND = "export_to_send_temp";
    private static String SANTFILESAVER_PROP_FILENAME_SUFFIX = "santfilesaver.properties";
    private static final String BASEDIR_PROPERTY = "basedir";

    public BondDefICExportEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        this.engineName = ENGINE_NAME;
        this.format = "UploaderXML";
     }
    
    private Bond getBondFromEvent(PSEventProduct productEvent) {
    	if (productEvent == null) return null;
    	
    	Product product = productEvent.getProduct();
    	if (product != null && product instanceof Bond) {
    		return (Bond)product;
    	}
    	
    	if (!Util.isEmpty(productEvent.getRefInterna())) {
    		try {
				product = DSConnection.getDefault().getRemoteProduct().getProductByCode(CollateralStaticAttributes.BOND_SEC_CODE_REF_INTERNA, productEvent.getRefInterna());
				
				if (product != null && product instanceof Bond) {
		    		return (Bond)product;
		    	}
			} catch (CalypsoServiceException e) {
				return null;
			}
    	}
    	
    	return null;
    }
    
    public static String trim(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null) {
                
            	if (line.contains("CalypsoUploadDocument") || line.contains("<CalypsoProduct") || line.contains("<CalypsoCashFlow")) {
            		result.append("\n");
            	}
            	result.append(line.trim());
            }
            result.append("\n");
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
        	try {
				reader.close();
			} catch (IOException e) {
				
			}
        }
    }
    
    private void randomAccessFileWrite(String reportPath, String content, String fileName) throws IOException {
        RandomAccessFile randomAccessFile = null;
        try {
          randomAccessFile = new RandomAccessFile(reportPath + File.separator + fileName, "rw");
          randomAccessFile.writeBytes(content);
          randomAccessFile.close();
        } catch (Exception e) {
          throw e;
        } finally {
          if (randomAccessFile != null) {
            randomAccessFile.close();
          }
        }
      }

    @Override
    public boolean process(PSEvent event) {
    	if (event instanceof PSEventDataUploaderAck){
    		PSEventDataUploaderAck dupAckEvent = (PSEventDataUploaderAck) event;
            ((BondDefICACKProcessor)this.getAckProcessor(null)).processAckEvent((dupAckEvent).getCalypsoDupAck());
            
            try {
				SantFileSaver.saveFile(getEngineName().toLowerCase(), "ack", "xml", "bond_ack", event.getLongId(), dupAckEvent.getOriginalMessage());
			} catch (SantFileSaverException e1) {
				Log.error(getEngineName(), "Error saving message in a file.");
			}
    	}
    	else if (event instanceof PSEventProduct) {
    		UploaderContextProvider.addAttributeValue("EventAction", EVENT_ACTION);
    		PSEventProduct productEvent = (PSEventProduct)event;
    		Bond product = getBondFromEvent(productEvent);
    		
    		if (product != null && !Util.isEmpty(this.format)) {
    			SantDataExportBuilder deBuilder = new SantDataExportBuilder(this.getConfigObject(), this.format);
    			String xmlBond = deBuilder.exportInUploaderXML(product);
    			xmlBond = trim(xmlBond);
    			
    			final String messageSendingActivated = LocalCache.getDomainValueComment(DSConnection.getDefault(), "domainName", "ICFlujosMessageSendingActivated");
    			if (!Util.isEmpty(messageSendingActivated) && "YES".equalsIgnoreCase(messageSendingActivated)) {
    				deBuilder.sendUpdateData();
    			}
    			
    			try {
    				String refInterna = product.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_REF_INTERNA);
    				if (Util.isEmpty(refInterna)) {
    					refInterna = "";
    				}
    				SantFileSaver.saveFile(getEngineName().toLowerCase(), SANTFILESAVER_EXPORT_DIR, "xml", "bond_export_" + refInterna, product.getLongId(), xmlBond);

    				String santFileSaverPropertiesFile = getEngineName().toLowerCase() + "." + SANTFILESAVER_EXPORT_DIR + "." + SANTFILESAVER_PROP_FILENAME_SUFFIX;
    				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(santFileSaverPropertiesFile);
    				if (is != null) {
    					final Properties properties = new Properties();
    				    properties.load(is);
    				    
    				    String baseDir = properties.getProperty(BASEDIR_PROPERTY);
    				    String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    				    String filePath = baseDir + SANTFILESAVER_EXPORT_DIR_TO_SEND + "/";
    				    String fileName = "bond_export_" + refInterna + "_" + product.getLongId() + "_" + timestamp + ".xml";
    				    
    				    randomAccessFileWrite(filePath, xmlBond, fileName);
    				}
    				
    			} catch (SantFileSaverException | IOException e) {
    				Log.error(getEngineName(), "Error saving message in a file: " + e.toString());
    			}
    		}
    		UploaderContextProvider.unsetUploaderContext();
    		
    		return consumeEvent(event);
    	}
    	
        return super.process(event);
    }
    
    private boolean consumeEvent(final PSEvent event) {
        try {
            DSConnection.getDefault().getRemoteTrade().eventProcessed(event.getLongId(), getEngineName());
        } catch (final Exception e) {
            Log.error(this, e);
            return false;
        }
        return true;
    }

    private DataExporterConfig getConfigObject() {
        this.properties.put("ExporterConfig", buildConfigName());
        return new DataExporterConfig(this.properties, this.engineName, this.getPricingEnv(),  0L, EVENT_ACTION);
    }

    private String buildConfigName(){
        String confName = this.propertyFile;
        if (confName != null && confName.contains(".")) {
            confName = confName.substring(0, confName.indexOf("."));
        }
        return confName;
    }
    
    @Override
    public DUPAckProcessor getAckProcessor(IEAdapter adapter) {
        return new BondDefICACKProcessor();
    }
}
