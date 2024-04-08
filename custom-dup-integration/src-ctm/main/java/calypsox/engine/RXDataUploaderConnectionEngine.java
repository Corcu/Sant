package calypsox.engine;

import calypsox.camel.engine.CamelBasedEngine;
import calypsox.ctm.camel.DummyRouteBuilder;
import calypsox.ctm.rx.RxDatauploaderAdapter;
import calypsox.tk.camel.AbstractCamelRouteBuilder;
import calypsox.tk.upload.pricingenv.UploaderPricingEnvHolderHandler;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventUpload;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class RXDataUploaderConnectionEngine extends CamelBasedEngine {

    public RXDataUploaderConnectionEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Launches datauploader's upload process for PSEventUpload events.
     * Reactive upload seems to be conflicting with uploader's thread pool. Disabled by now.
     * @param event
     */
    @Override
    public boolean process(PSEvent event) {
        return Optional.ofNullable(event)
                .filter(ev -> ev instanceof PSEventUpload)
                .map(ev -> addEngineNameToEvent((PSEventUpload) ev))
                .map(RxDatauploaderAdapter::processEvent)
                .orElse(false);
    }

    /**
     * Need to subscribe to some kind of observable that will emit error events
     * @param event
     */
    @Override
    protected void countBadEvent(PSEvent event) {
        super.countBadEvent(event);
    }

    @Override
    public AbstractCamelRouteBuilder getCamelRouteBuilder() {
        return new DummyRouteBuilder();
    }

    /**
     * //TODO Schedulers start and shutdown may affect other Calypso RX Modules. Need to review this carefully.
     */
    @Override
    public boolean start() throws ConnectException {
        //Schedulers.start();
        UploaderPricingEnvHolderHandler.initOficcialAccPricingEnvHolder();
        return super.start();
    }

    /**
     * Forces RX schedulers shutdown.
     * //TODO Apply shutdown policy to avoid data losing
     */
    @Override
    public void stop() {
        //Schedulers.shutdown();
        super.stop();
    }

    private PSEventUpload addEngineNameToEvent(PSEventUpload uploadEvent){
        uploadEvent.setEngineName(this.getEngineName());
        return uploadEvent;
    }
}
