package calypsox.engine;

import calypsox.tk.event.PSEventDataUploaderAck;
import calypsox.tk.export.ack.DUPAckProcessor;
import calypsox.tk.export.ack.RateIndexACKProcessor;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.export.DataExportBuilder;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.mapping.core.UploaderContextProvider;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;

/**
 * @author aalonsop
 */
public class RateIndexExportEngine extends MultipleDestinationExportEngine {


    public RateIndexExportEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        this.engineName="RateIndexExportEngine";
        this.format="UploaderXML";
    }

    @Override
    public boolean process(PSEvent event) {
        if(event instanceof PSEventDataUploaderAck){
            ((RateIndexACKProcessor)this.getAckProcessor(null))
                    .processAckEvent(((PSEventDataUploaderAck) event).getCalypsoDupAck());
        }
        return super.process(event);
    }

    @Override
    public void processDomainChange(PSEventDomainChange ad) {
        Object eventObject;
        if(PSEventDomainChange.RATE_INDEX==ad.getType()) {
            UploaderContextProvider.addAttributeValue("EventAction", ad.getActionString());
            eventObject = ad.getObject();
            if (!Util.isEmpty(this.format)) {
                (new DataExportBuilder(this.getConfigObject(ad), this.format)).buildExporterData(eventObject);
            }
            UploaderContextProvider.unsetUploaderContext();
        }
    }

    private DataExporterConfig getConfigObject(PSEventDomainChange ev) {
        this.properties.put("ExporterConfig", buildConfigName());
        return new DataExporterConfig(this.properties, this.engineName, this.getPricingEnv(),  0L, ev.getActionString());
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
        return new RateIndexACKProcessor();
    }

}
