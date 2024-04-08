
package calypsox.tk.util;

import com.calypso.tk.util.IEAdapter;


public class SantanderDataExporterTopicIEAdapterConfig extends SantanderJMSQueueTopicIEAdapterConfig {

    /**
     * Default constructor.
     */
    public SantanderDataExporterTopicIEAdapterConfig() {
        String adapter = "mcliquidation.out";
        setAdapterType(adapter);
    }

    @Override
	public boolean isConfigured() {
		return this.properties != null;
	}
    
    @Override
    public IEAdapter getSenderIEAdapter() {
        if (!isSender()) {
            return null;
        }
        if ("file".equals(this.properties.getProperty("adapter"))) {
            this.santIEAdapter = new FileIEAdapter(IEAdapterMode.WRITE);
        } else {
            this.santIEAdapter = new SantanderDataExporterTopicIEAdapter(IEAdapterMode.WRITE);
        }
        this.santIEAdapter.setIEAdapterConfig(this);

        return this.santIEAdapter;
    }

}
