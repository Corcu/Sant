/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.dataimport;

import calypsox.apps.importer.adapter.SantDefaultEquityImporterAdapter;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.util.EquityJMSQueueAnswer;
import calypsox.tk.util.JMSQueueAnswer;
import com.calypso.apps.importer.adapter.DefaultImporterAdapter;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

//Project: Bloomberg tagging

/**
 * Import bonds into Calypso
 *
 * @author Guillermo Solano
 */

public class CalypsoMLEquityIEEngine extends CalypsoMLIEEngine {

    /**
     * Constructor
     *
     * @param configName config name use to create the IEAdapter
     * @param dsCon      DataServer connection
     * @param hostName   hostname of the EventServer
     * @param esPort     port of the EventServer
     */
    public CalypsoMLEquityIEEngine(DSConnection dsCon, String hostName,
                                   int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          Equity imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return
     */
    @Override
    protected JMSQueueAnswer generateAnswer(final Object object,
                                            final Exception exception, final ExternalMessage message) {

        final JMSQueueAnswer answer = new EquityJMSQueueAnswer(message); // GSM:
        // check

        if (object instanceof Equity) {
            Equity equity = (Equity) object;

            answer.setCode(JMSQueueAnswer.OK);
            answer.setDescription("Equity successfully imported in Calypso.");
            answer.setDestinationKey(String.valueOf(equity.getId()));

            String isin = equity
                    .getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

            try {
                createPsEventBloombergUpdate(isin);
            } catch (CalypsoServiceException cse) {
                answer.setException(cse);
            }
        }
        if (exception != null) {
            answer.setException(exception);

            if (answer.getTransactionKey() == null) {
                answer.setTransactionKey(getDirtyReference(message));
            }
        }
        return answer;
    }

    /**
     * get specific CML importer adapter
     *
     * @return SantanderDefaultEquityImporterAdapter class name
     */
    @Override
    protected Class<? extends DefaultImporterAdapter> getImportAdapterClass() {
        return SantDefaultEquityImporterAdapter.class; // GSM: unfinish
        // EquityPreTranslator
    }

    /**
     * get the tag used for the dirty reference
     *
     * @return the tag value: externalReference
     */
    @Override
    protected String getDirtyReferenceTag() {
        return ":code";
    }

    /**
     * generate a task from an specific message
     *
     * @param exception source of the task
     * @param answer    anwser if possible
     */
    @Override
    protected Task buildTask(final String message,
                             final JMSQueueAnswer answer) {

        Task task = new Task();

        if (answer != null) {

            task = buildTask(message, 0, "EX_EQUITY_IMPORT",
                    Task.EXCEPTION_EVENT_CLASS);
            if (answer.getDestinationKey() != null) {

                task.setObjectLongId(Long.parseLong(answer.getDestinationKey()));
                task.setObjectClassName(Equity.class.getName());
            }
        }

        return task;
    }

    /**
     * createPsEventBloombergUpdate.
     *
     * @param tituloId String
     * @throws CalypsoServiceException
     */
    private void createPsEventBloombergUpdate(String tituloId)
            throws CalypsoServiceException {
        PSEventBloombergUpdate event = new PSEventBloombergUpdate(tituloId, 0);

        DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
    }
}
