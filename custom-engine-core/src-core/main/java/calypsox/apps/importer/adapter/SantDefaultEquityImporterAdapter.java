/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.importer.adapter;

import calypsox.processing.EquityPreTranslator;
import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.infra.util.Util;
import com.calypso.processing.error.ErrorMessage;
import com.calypso.processing.error.MessageType;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.PersistenceSession;

import java.util.Vector;

public class SantDefaultEquityImporterAdapter extends SantanderDefaultImporterAdapter {

    /**
     * Generic constructor
     *
     * @throws AdapterException
     */
    public SantDefaultEquityImporterAdapter() throws AdapterException {
        super();
    }

    /**
     * This method will translate the jaxbObject and save the translation using the persistenceSession.
     *
     * @param jaxbObject         the object to import.
     * @param persistenceSession the PersistenceSession to use to save the translated object.
     * @throws ErrorMessage if a translation or persistence error occurs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void importObject(final com.calypso.jaxb.xml.Object jaxbObject,
                                final PersistenceSession persistenceSession) throws ErrorMessage {

        Equity equityTranslated = null;

        try {
            preTranslate(jaxbObject); // equity pretranslation
            final Object translated = translateObject(jaxbObject, persistenceSession);
            this.importedObjects.add(translated);

            if (translated instanceof Equity) {
                final Vector messages = new Vector();
                equityTranslated = (Equity) translated;
                if (!equityTranslated.isValidInput(messages)) {
                    // build a task for each error message
                    buildTasks(messages, equityTranslated.getId());
                    throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, messages.toString());
                }
            }
            // persist the translated object depending on the import action
            persistenceSession.save(translated);
        } catch (final PersistenceException e) {
            throw new ErrorMessage(jaxbObject, MessageType.PERSISTENCE_ERROR, e);
        } catch (final SecurityException e) {
            throw new ErrorMessage(jaxbObject, MessageType.SECURITY_ERROR, e);
        }
    }

    /**
     * Bond pre translation method.
     *
     * @param jaxbObject
     */
    @Override
    protected void preTranslate(final com.calypso.jaxb.xml.Object jaxbObject) throws ErrorMessage {
        // Do the conversion here, iso_code to country name
        // jaxbObject.get
        final EquityPreTranslator preProcessor = new EquityPreTranslator(); // GSM: undone
        preProcessor.process(jaxbObject);
        super.preTranslate(jaxbObject);

    }

    /**
     * Generate a Task
     *
     * @param comment    comment related to the task
     * @param tradeId    trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    protected Task buildTask(String comment, long objectId, String eventType, String eventClass) {
        Task task = new Task();
        task.setObjectLongId(objectId);
        task.setTradeLongId(0);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource("EquityImportEngine");
        task.setComment(comment);
        return task;
    }

    // ////////////////////////
    // // PRIVATE METHODS ////
    // //////////////////////

    /**
     * Tasks builder.
     *
     * @param messages to attach to the task
     * @param equityId
     */
    private void buildTasks(Vector<String> messages, int equityId) {
        if (!Util.isEmpty(messages)) {
            for (String message : messages) {
                getTasks().add(buildTask(message, equityId, "EX_EQUITY_IMPORT", Task.EXCEPTION_EVENT_CLASS));
            }
        }

    }

}
