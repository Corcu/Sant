package calypsox.apps.importer.adapter;

import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.product.TerminationUtil;
import com.calypso.tk.service.PersistenceSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantanderDefaultImporterAdapter extends com.calypso.apps.importer.adapter.DefaultImporterAdapter {

    @SuppressWarnings("rawtypes")
    protected Vector importedObjects = new Vector();

    // tasks generated during the import
    protected List<Task> tasks = new ArrayList<>();

    /**
     * JMSCorrelationID saved because we don't have if the messaage is the mirror and the answer is sent by Calypso when
     * the PSEvent is created
     */
    private String jmsReference;

    public SantanderDefaultImporterAdapter() throws AdapterException {
        super();
    }

    /**
     * This method will translate the jaxbObject and save the translation using the persistenceSession.
     *
     * @param jaxbObject         the object to import.
     * @param persistenceSession the PersistenceSession to use to save the translated object.
     * @throws ErrorMessage if a translation or persistence error occurs.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void importObject(final com.calypso.jaxb.xml.Object jaxbObject,
                                final PersistenceSession persistenceSession) throws ErrorMessage {
        try {
            preTranslate(jaxbObject);
            final Object translated = translateObject(jaxbObject, persistenceSession);
            /*
             * Save as we progress so that dependent objects have ids when they are called upon. Some Calypso objects
             * don't keep a reference on other objects but only their id.
             */

            //
            // If it's a trade and it's a mirror we save the keyword
            // JMSReference
            if (translated instanceof Trade) {
                // change trade for Partial Termination.
                changePartialTerminationAction((Trade) translated);

                /*
                 * if (TradeUtil.isInternalDeal((Trade) translated)) { Log.debug( this,
                 * "SantanderDefaultImporterAdapter.importObject the trade is internal, so we must save the jmsReference."
                 * ); TradeUtil.setJMSReference((Trade) translated, this.getJMSReference()); } else {
                 */
                Log.debug(this,
                        "SantanderDefaultImporterAdapter.importObject the trade is not internal, so we dont'need save the jmsReference.");
                // }
            }
            this.importedObjects.add(translated);

            persistenceSession.save(translated);
        } catch (final PersistenceException e) {
            throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
        } catch (final SecurityException e) {
            throw new ErrorMessage(jaxbObject, MessageType.SECURITY_ERROR, e);
        }
    }

    /**
     * Change Trade action to New in order to perform Partial Termination
     *
     * @param trade
     */
    private void changePartialTerminationAction(final Trade trade) {
        // Change Trade Action for Partial Terminate
        if (Action.TERMINATE.equals(trade.getAction())
                && TerminationUtil.PARTIAL_TERMINATION.equals(trade.getKeywordValue(TerminationUtil.TERMINATION_TYPE))) {
            trade.setAction(Action.NEW);
            trade.setLongId(0);
            trade.setVersion(0);
            trade.getProduct().setId(0);
            trade.getProduct().setVersion(0);
            trade.setStatus(Status.S_NONE);
            // trade.getKeywords().remove("CODIFIER-Murex");
            // trade.getKeywords().remove("MxDealID");
            // trade.setExternalReference(null);
        }

    }

    public Object getImporterObject() {
        Object object = null;

        if ((this.importedObjects != null) && (this.importedObjects.size() != 0)) {
            object = this.importedObjects.firstElement();
            this.importedObjects.remove(0);
        }
        return object;
    }

    /**
     * Save the JMSCOrrelationID
     *
     * @param jmsReference
     */
    public void setJMSReference(final String jmsReference) {
        this.jmsReference = jmsReference;
    }

    /**
     * @return JMSCorrelationID
     */
    public String getJMSReference() {
        return this.jmsReference;
    }

    /**
     * @return the tasks
     */
    public List<Task> getTasks() {
        return this.tasks;
    }

    /**
     * @param tasks the tasks to set
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
