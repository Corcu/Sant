package calypsox.engine;

import calypsox.tk.export.ack.DUPAckProcessor;
import calypsox.tk.util.TibcoQueueIEAdapterConfig;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.engine.DataExportServiceEngine;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.export.DataExportBuilder;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;

import java.util.*;
import java.util.Map.Entry;


public class MultipleDestinationExportEngine extends DataExportServiceEngine implements IEAdapterListener {

	
	public static final String DV_DISABLED_GATEWAY="gateway.disabled";
	
	protected String _configName;
	protected HashMap<String, Properties> propertiesPerInterface = null;
	protected HashMap<String, DataExporterConfig> dataExporterConfigsPerGateway = null;
	protected ArrayList<IEAdapterConfig> ackIEAdapterConfigs = null;
	
	protected IEAdapter ackIEAdapter;
	protected IEAdapterConfig ackIEAdapterConfig;

	public static final String LOG_CATEGORY = "MultipleDestinationExportEngine";

	public MultipleDestinationExportEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
		this.engineName="MultipleDestinationExportEngine";
		this.format="AdviceDocUploaderXML";
	}

	public void exportTrade(Object tradeObject) {
		if (tradeObject == null)
			return;
		if (!Util.isEmpty(this.format)) {
			//Object adviceDocId=null;
			for (DataExporterConfig exporterConfig : this.getExporterConfigObjects().values()) {	
				(new DataExportBuilder(exporterConfig, this.format)).buildExporterData(tradeObject);
			}
		}
	}
	

	public String getConfigName() {
		if (!Util.isEmpty(this.propertyFile) && !this.propertyFile.endsWith(".properties"))
			return this.propertyFile + ".properties";
		else
			return this.propertyFile;
	}

	public boolean initAckAdapters() {
		for(IEAdapterConfig config : getAckConfigObjects()) {
			IEAdapter receiver = config.getReceiverIEAdapter();
	        if (receiver == null) {
	            Log.error(LOG_CATEGORY, "Error while creating ack receiver");
	        }
	        receiver.setListener(this);
	        try {
				receiver.init();
			} catch (ConnectException e) {
				Log.error(LOG_CATEGORY, e);
			}
	        config.checkTimer();	
		}
		return true;
	}
	
	protected HashMap<String, Properties> getPropertiesPerGateway() {

		if (propertiesPerInterface == null) {
			if (properties != null) {
				propertiesPerInterface = new HashMap<String, Properties>();
				for (Entry<Object, Object> prop : properties.entrySet()) {
					String key = prop.getKey().toString();
					if (!key.contains("."))
						continue;
					String prefix = key.split("\\.")[0];
					String gateway = getGatewayFromKey(prefix);
					Properties props = propertiesPerInterface.get(gateway);
					if (props==null) {
						props = new Properties();
						props.put("gateway", gateway);
						if(!isGatewayDisabled(gateway))
							propertiesPerInterface.put(gateway, props);
					}
					String newKey = key.substring(key.indexOf(".") + 1);
					props.put(newKey, prop.getValue());
				}
			}
		}

		return propertiesPerInterface;
	}
	
	protected boolean isGatewayDisabled(String gateway) {
		Vector<String> disabledGateways = LocalCache.getDomainValues(DSConnection.getDefault(), getEngineName() + "." + DV_DISABLED_GATEWAY);
		if(disabledGateways==null)
			return false;
		return disabledGateways.contains(gateway);
	}
	
	protected String getGatewayFromKey(String key) {
		return key.toUpperCase();
	}

	protected HashMap<String, DataExporterConfig> getExporterConfigObjects() {
		if (dataExporterConfigsPerGateway == null) {
			dataExporterConfigsPerGateway = new HashMap<String, DataExporterConfig>();
			for (Map.Entry<String, Properties> props : getPropertiesPerGateway().entrySet()) {
				String opMode = props.getValue().getProperty("jms.opmode");
				Integer opModeInt = TibcoQueueIEAdapterConfig.SENDER;
				if(opMode!=null) {
					opModeInt=Integer.parseInt(opMode);
				}
				if(opModeInt.equals(TibcoQueueIEAdapterConfig.SENDER)
						|| opModeInt.equals(TibcoQueueIEAdapterConfig.BOTH)) {
					Properties propSender = (Properties)props.getValue().clone();
					propSender.setProperty("jms.opmode", ""+TibcoQueueIEAdapterConfig.SENDER);
					DataExporterConfig exporterDataConfig = new DataExporterConfig(propSender, this.engineName,
							this.getPricingEnv(), 0L, null);
					dataExporterConfigsPerGateway.put(props.getKey(),exporterDataConfig);
				}
			}
		}
		return dataExporterConfigsPerGateway;

	}
	
	
	protected ArrayList<IEAdapterConfig> getAckConfigObjects() {
		if (ackIEAdapterConfigs == null) {
			ackIEAdapterConfigs = new ArrayList<IEAdapterConfig>();
			for (Properties props : getPropertiesPerGateway().values()) {
				String opMode = props.getProperty("jms.opmode");
				Integer opModeInt = TibcoQueueIEAdapterConfig.SENDER;
				if(opMode!=null) {
					opModeInt=Integer.parseInt(opMode);
				}
				if(opModeInt.equals(TibcoQueueIEAdapterConfig.RECEIVER)
						|| opModeInt.equals(TibcoQueueIEAdapterConfig.BOTH)) {
					UploaderIEAdapterConfig config = new UploaderIEAdapterConfig();
					Properties propReceiver = (Properties)props.clone();
					propReceiver.setProperty("jms.opmode", ""+TibcoQueueIEAdapterConfig.RECEIVER);
					config.setProperties(propReceiver);
					ackIEAdapterConfigs.add(config);
				}
			}
		}
		return ackIEAdapterConfigs;

	}
	
	@Override
	public void processDomainChange(PSEventDomainChange ad) {
	}

	@Override
	public boolean newMessage(IEAdapter adapter, String string) {
		return true;
	}

	@Override
	public boolean newMessage(IEAdapter adapter, ExternalMessage message) {
		Log.info(LOG_CATEGORY, "Ack received : " + message.getText());
		getAckProcessor(adapter).process(message);
		return true;
	}
	
	public DUPAckProcessor getAckProcessor(IEAdapter adapter) {
		return null;
	}

	@Override
	public void onDisconnect(IEAdapter arg0) {
		try {
			arg0.stop();
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
		
	}
	
	public void stop() {
		for(IEAdapterConfig config : getAckConfigObjects()) {
			IEAdapter receiver = config.getReceiverIEAdapter();
	        if (receiver == null) {
	            Log.error(LOG_CATEGORY, "Error while stopping ack receiver");
	        }
	        else {
		        try {
					receiver.stop();
				} catch (Exception e) {
					Log.error(LOG_CATEGORY, e);
				}
	        }
	        config.checkTimer();	
		}
		super.stop();
	}
	
	protected boolean beforeStart() throws ConnectException {
		boolean result = super.beforeStart();
		if(result)
			this.initAckAdapters();
		return result;
	}

}
