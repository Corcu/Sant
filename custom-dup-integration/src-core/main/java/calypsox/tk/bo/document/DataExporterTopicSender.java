package calypsox.tk.bo.document;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterListener;

import calypsox.tk.util.JMSTopicIEAdapter;
import calypsox.tk.util.SantanderDataExporterTopicIEAdapterConfig;
import calypsox.tk.util.SantanderJMSQueueTopicIEAdapter;

public class DataExporterTopicSender implements IEAdapterListener {
	
	public static final String LOG_CATEGORY = "DataExporterTopicSender";
	
	IEAdapter _mqSender;

	public void onDisconnect(IEAdapter adapter) {
		Log.error("EXPORTER", " DataExporterMQSender :Message Adapter disconnected, waited to be connected again ");
	}

	public boolean newMessage(IEAdapter adapter, String mess) {
		return true;
	}

	public boolean newMessage(IEAdapter adapter, ExternalMessage mess) {
		return true;
	}

	public DataExporterTopicSender(String propertyFileName, String sourceName) {
		
		SantanderDataExporterTopicIEAdapterConfig config = new SantanderDataExporterTopicIEAdapterConfig();
		if (config != null && config.isConfigured()) {
			this._mqSender = config.getSenderIEAdapter();
			this._mqSender.setListener(this);
		} else {
			Log.error(this.getClass().getSimpleName(), "*** DataExporter MQ sender not configured properly for "
					+ sourceName + " Please check file " + propertyFileName + new JDatetime());
		}
	}

	public DataExporterTopicSender(Properties properties, String sourceName) {
		SantanderDataExporterTopicIEAdapterConfig config = new SantanderDataExporterTopicIEAdapterConfig();
		config.setProperties(properties);
		if (config != null && config.isConfigured()) {
			this._mqSender = config.getSenderIEAdapter();
			this._mqSender.setListener(this);
		} else {
			Log.error(this.getClass().getSimpleName(), "*** DataExporter MQ sender not configured properly for "
					+ sourceName + " Please check file Properties in property file");
		}
	}

	public boolean isOnline() {
		return this._mqSender != null && this._mqSender.isOnline();
	}
	
	public void init(List<String> errors) {
		try {
			this._mqSender.init();
		} catch (ConnectException e) {
			Log.error(LOG_CATEGORY, e);
			errors.add(e.getMessage());
		}
	}

	public boolean send(DSConnection ds, String message, Map<String, String> contextMap, List<String> errors) {
		boolean process = false;
		String dataSource = (String) contextMap.get("SourceName");

		try {
			if (!this.isOnline()) 
                    init(errors);
            
			if (this._mqSender instanceof SantanderJMSQueueTopicIEAdapter) {
				process = ((SantanderJMSQueueTopicIEAdapter) this._mqSender).write(message, contextMap);
				JMSTopicIEAdapter outputAdapter = ((SantanderJMSQueueTopicIEAdapter) this._mqSender).getOutputAdapter();
				if(outputAdapter!=null)
					outputAdapter.stop();
			}
		} catch (Exception arg7) {
			Log.error(this.getClass().getSimpleName(),
					"Error while sending data in the queue from " + dataSource + ". Exception is : " + arg7);
			errors.add("Error while sending data in the queue from " + dataSource + ". Exception is : "
					+ arg7.getMessage());
		}

		return process;
	}

}
