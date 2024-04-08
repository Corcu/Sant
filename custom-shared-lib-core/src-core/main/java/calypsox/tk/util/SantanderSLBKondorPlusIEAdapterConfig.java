/**
 *
 */
package calypsox.tk.util;

import calypsox.tk.bo.document.JMSKPlusDocumentSender;
import com.calypso.tk.util.IEAdapter;

/**
 * @author fperezur
 *
 */
public class SantanderSLBKondorPlusIEAdapterConfig extends
        SantanderKondorPlusIEAdapterConfig {

    /**
     * Default constructor.
     */
    public SantanderSLBKondorPlusIEAdapterConfig() {
        setAdapterType(JMSKPlusDocumentSender.ADAPTER);
    }


    @Override
    public IEAdapter getSenderIEAdapter() {
        if (!isSender()) {
            return null;
        }
        if ("file".equals(this.properties.getProperty("adapter"))) {
            this.santIEAdapter = new FileIEAdapter(IEAdapterMode.WRITE);
        } else {
            this.santIEAdapter = new SantanderSLBKondorPlusIEAdapter(IEAdapterMode.WRITE);
        }
        this.santIEAdapter.setIEAdapterConfig(this);

        return this.santIEAdapter;
    }

}
