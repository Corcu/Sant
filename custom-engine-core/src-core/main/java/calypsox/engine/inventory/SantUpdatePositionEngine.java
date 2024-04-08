package calypsox.engine.inventory;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventSantGDPosition;
import calypsox.tk.util.gdisponible.GDisponibleUtil;
import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import java.rmi.RemoteException;

/**
 * Positions from Gestion Disponible will be imported into Calypso by the mean of this Engine, called
 * SantUpdatePositionEngine
 *
 * @author Patrice Guerrido & Guillermo Solano
 * @version 1.0
 */
public class SantUpdatePositionEngine extends Engine {

    /**
     * Name of the service
     */
    public static final String ENGINE_NAME = "SANT_UpdatePositionEngine";

    /**
     * Activates testing mode (file watcher simulating queue)
     */
    private static final String TESTING_MODE = "TESTING_MODE";

    /**
     * SantPosAdapter
     */
    private final SantPositionAdapter santPosAdapter;

    /**
     * Constructor
     *
     * @param dsCon
     * @param hostName
     * @param esPort
     */
    public SantUpdatePositionEngine(final DSConnection dsCon, final String hostName, final int esPort) {
        super(dsCon, hostName, esPort);
        this.santPosAdapter = new SantPositionAdapter();
    }

    /**
     * Start of the engine
     */
    @Override
    public boolean start(final boolean batch) throws ConnectException {
        String testingMode = getEngineParam(TESTING_MODE, TESTING_MODE, "FALSE");

        boolean testing = testingMode.equals("TRUE");

        this.santPosAdapter.start(testing);

        return super.start(batch);
    }

    @Override
    public boolean process(final PSEvent event) {
        boolean result = true;
        if (event instanceof PSEventSantGDPosition) {
            handleEvent((PSEventSantGDPosition) event);
            try {
                this._ds.getRemoteTrade().eventProcessed(event.getLongId(), ENGINE_NAME);
            } catch (RemoteException e) {
                Log.error(SantUpdatePositionEngine.class.getName(), e);
                result = false;
            }
        } else if (event instanceof PSEventDomainChange) {
            processDomainChange((PSEventDomainChange) event);
        }
        return result;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /**
     * @see com.calypso.engine.Engine#processEvent(com.calypso.tk.event.PSEvent)
     */
    public void handleEvent(PSEventSantGDPosition event) {
        PSEventSantGDPosition maturedSecPositions = (PSEventSantGDPosition) event;

        JMSQueueMessage jmsQueueMessage = new JMSQueueMessage();
        // put specific reference for internal messages (not acknowledged
        // messages)
        jmsQueueMessage
                .setReference(GDisponibleUtil.SANT_GD_MATURE_SEC_POS_REFERENCE);
        jmsQueueMessage.setText(maturedSecPositions.getPositionMessage());
        this.santPosAdapter.newMessage(this.santPosAdapter.getAdapter(),
                jmsQueueMessage);
    }
}
