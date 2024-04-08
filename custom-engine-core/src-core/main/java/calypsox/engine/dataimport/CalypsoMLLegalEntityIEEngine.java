/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */
package calypsox.engine.dataimport;

import calypsox.apps.importer.adapter.SantanderDefaultLegalEntityImporterAdapter;
import calypsox.tk.util.JMSQueueAnswer;
import com.calypso.apps.importer.adapter.DefaultImporterAdapter;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.service.DSConnection;

/**
 * The Class CalypsoMLLegalEntityIEEngine.
 * <p>
 * This engine handle the CML importing process, specific from LegalEntity
 *
 * @author Bruno P.
 * @version 1.0
 * @since 03/18/2011
 */
public class CalypsoMLLegalEntityIEEngine extends CalypsoMLIEEngine {

    /**
     * Constructor
     *
     * @param configName config name use to create the IEAdapter
     * @param dsCon      DataServer connection
     * @param hostName   hostname of the EventServer
     * @param esPort     port of the EventServer
     */
    public CalypsoMLLegalEntityIEEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          LegalEntity imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return
     */
    @Override
    protected JMSQueueAnswer generateAnswer(final Object object, final Exception exception,
                                            final ExternalMessage message) {
        final JMSQueueAnswer answer = new JMSQueueAnswer(message);
        if (object instanceof LegalEntity) {
            final LegalEntity le = (LegalEntity) object;

            answer.setCode(JMSQueueAnswer.OK);
            answer.setDescription("LegalEntity successfully imported in Calypso.");
            answer.setDestinationKey(String.valueOf(le.getId()));
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
     * @return SantanderDefaultLegalEntityImporterAdapter class name
     */
    @Override
    protected Class<? extends DefaultImporterAdapter> getImportAdapterClass() {
        return SantanderDefaultLegalEntityImporterAdapter.class;
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
    protected Task buildTask(final String message, final JMSQueueAnswer answer) {

        Task task;
        if (answer != null) {

            task = buildTask(message, 0, answer.getDescription(), Task.EXCEPTION_EVENT_CLASS);

            if (answer.getDestinationKey() != null) {
                task.setObjectLongId(Long.parseLong(answer.getDestinationKey()));
                task.setObjectClassName(LegalEntity.class.getName());
            }
        } else {

            task = new Task();
        }

        return task;
    }

}
