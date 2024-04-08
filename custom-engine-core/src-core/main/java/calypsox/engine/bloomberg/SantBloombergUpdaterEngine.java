package calypsox.engine.bloomberg;

import calypsox.engine.inventory.SantUpdatePositionEngine;
import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.util.SantBloombergUtil;
import calypsox.tk.util.ScheduledTaskSANT_BLOOMBERG_TAGGING;
import com.calypso.engine.Engine;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.scheduling.service.RemoteSchedulingService;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ScheduledTask;

import java.rmi.RemoteException;

// Project: Bloomberg tagging

/**
 * SantBloombergUpdaterEngine
 *
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Jos? Luis F. Luque <joseluis.f.luque@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com>
 */
public class SantBloombergUpdaterEngine extends Engine {

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_BloombergUpdaterEngine";

    /**
     * Constants SANT_BLOOMBERG_TAGGING
     */
    private static final String[] SANT_BLOOMBERG_TAGGING = {
            "SANT_BLOOMBERG_TAGGING_FIXEDINCOME",
            "SANT_BLOOMBERG_TAGGING_EQUITY"};

    public static int BLOOMBERG_PRODUCT_TYPE_FIXEDINCOME = 0;
    public static int BLOOMBERG_PRODUCT_TYPE_EQUITY = 1;

    // private SantBloombergUtil util;

    /*
     * Used to JUnit
     */
    // public void setSantBloombergUtil(SantBloombergUtil util) {
    // this.util = util;
    // }

    /**
     * Constructor
     *
     * @param dsCon
     * @param hostName
     * @param esPort
     */
    public SantBloombergUpdaterEngine(final DSConnection dsCon,
                                      final String hostName, final int esPort) {
        super(dsCon, hostName, esPort);
    }

    /**
     * Start of the engine
     */
    @Override
    public boolean start(final boolean batch) throws ConnectException {
        return super.start(batch);
    }

    @Override
    public boolean process(final PSEvent event) {
        boolean result = true;
        if (event instanceof PSEventBloombergUpdate) {
            handleEvent((PSEventBloombergUpdate) event);
            try {
                this._ds.getRemoteTrade().eventProcessed(event.getLongId(),
                        ENGINE_NAME);

            } catch (RemoteException e) {
                Log.error(SantUpdatePositionEngine.class.getName(), e);
                result = false;
            }
        }

        return result;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /**
     */
    public void handleEvent(final PSEventBloombergUpdate event) {
        int tipo = event.getTipo();
        String isin = event.getTituloId();

        SantBloombergUtil util = new SantBloombergUtil();

        ScheduledTask st = getScheduledTask(SANT_BLOOMBERG_TAGGING[tipo]);
        String filePath = getFilePath(st);
        String fileName = getFileName(st);

        JDate date = JDate.getNow();

        util.processOneOrSeveralProducts(filePath, fileName, date, isin);
    }

    private ScheduledTask getScheduledTask(String externalReference) {
        ScheduledTask st = null;

        try {
            RemoteSchedulingService schedulingService = DSConnection
                    .getDefault().getService(RemoteSchedulingService.class);
            st = schedulingService
                    .getScheduledTaskByExternalReference(externalReference);
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not retrieve Scheduled Task with external reference \"%s\"",
                    externalReference);
            Log.error(this, message, e);
        }

        return st;
    }

    private String getFilePath(ScheduledTask scheduledTask) {
        return scheduledTask
                .getAttribute(ScheduledTaskSANT_BLOOMBERG_TAGGING.FILEPATH);
    }

    private String getFileName(ScheduledTask scheduledTask) {
        return scheduledTask.getAttribute(
                ScheduledTaskSANT_BLOOMBERG_TAGGING.FILENAME_FULL);
    }

}
