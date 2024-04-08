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

import calypsox.apps.importer.adapter.SantanderDefaultBookImporterAdapter;
import calypsox.tk.util.JMSQueueAnswer;
import com.calypso.apps.importer.adapter.DefaultImporterAdapter;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.BookAttribute;
import com.calypso.tk.service.DSConnection;

/**
 * The Class CalypsoMLBookIEEngine.
 * <p>
 * This engine handle the CML importing process, specific from Book
 *
 * @author Soma.
 */
public class CalypsoMLBookIEEngine extends CalypsoMLIEEngine {

    public CalypsoMLBookIEEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          Book imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return
     */
    @Override
    protected JMSQueueAnswer generateAnswer(final Object object, final Exception exception,
                                            final ExternalMessage message) {


        final JMSQueueAnswer answer = new JMSQueueAnswer(message);

        StringBuffer aliasGBO;
        StringBuffer aliasSUSI;
        StringBuffer aliasKONDOR;

        if (object instanceof Book) {
            aliasGBO = new StringBuffer();
            aliasSUSI = new StringBuffer();
            aliasKONDOR = new StringBuffer();
            final Book book = (Book) object;

            BookAttribute attribute;

            // BAU 5.6 - Idientifier code of Legal Entity will be the GLCS code instead of company code

            for (int i = 0; i < book.getAttributes().size(); i++) {

                attribute = (BookAttribute) book.getAttributes().get(i);


                if (!Util.isEmpty(attribute.getName()) && !Util.isEmpty(attribute.getValue())) {
                    if (attribute.getName().equals("ALIAS_BOOK_GBO")) {
                        aliasGBO.append("ALIAS_BOOK_GBO = ").append(attribute.getValue());
                    }
                    if (attribute.getName().equals("ALIAS_BOOK_SUSI")) {
                        aliasSUSI.append("ALIAS_BOOK_SUSI = ").append(attribute.getValue());
                    }
                    if (attribute.getName().equals("ALIAS_BOOK_KONDOR")) {
                        aliasKONDOR.append("ALIAS_BOOK_KONDOR = ").append(attribute.getValue());
                    }
                }
            }
            answer.setCode(JMSQueueAnswer.OK);

            answer.setDescription("Book successfully imported in Calypso. " + aliasGBO.toString() + ", " + aliasSUSI
                    + ", " + aliasKONDOR);
            answer.setDestinationKey(String.valueOf(book.getId()));
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
     * @return SantanderDefaultBookImporterAdapter class name
     */
    @Override
    protected Class<? extends DefaultImporterAdapter> getImportAdapterClass() {
        return SantanderDefaultBookImporterAdapter.class;
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
                task.setObjectClassName(Book.class.getName());
            }
        } else {
            task = new Task();
        }

        return task;
    }

}
