
package calypsox.tk.util;

import com.calypso.tk.core.Log;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterConfig;


public class SantanderDataExporterTopicIEAdapter extends SantanderJMSQueueTopicIEAdapter {

	public SantanderDataExporterTopicIEAdapter(IEAdapterMode mode) {
		super(mode);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.IEAdapter#setIEAdapterConfig(com.calypso.tk.util. IEAdapterConfig)
	 */
	@Override
	public void setIEAdapterConfig(final IEAdapterConfig config) {
		super.setIEAdapterConfig(config);
		try {
			if (isSender()) {
				this.outputAdapter = new JMSTopicIEAdapter(JMSTopicIEAdapter.SENDER);
				this.outputAdapter.setIEAdapterConfig(config);
			}
			if (isReceiver()) {
				this.inputAdapter = new JMSQueueIEAdapter(JMSQueueIEAdapter.RECEIVER);
				this.inputAdapter.setIEAdapterConfig(config);
			}
		} catch (final ConnectException exception) {
			Log.error(Log.ERR, exception);
		}
	}
	
	

}
