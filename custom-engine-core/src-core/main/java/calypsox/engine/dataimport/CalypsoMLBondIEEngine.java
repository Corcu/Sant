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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.calypso.apps.importer.adapter.DefaultImporterAdapter;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import calypsox.apps.importer.adapter.SantDefaultBondImporterAdapter;
import calypsox.engine.importer.ImporterUtil;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.util.BondJMSQueueAnswer;
import calypsox.tk.util.JMSQueueAnswer;
import calypsox.tools.santfilesaver.SantFileSaver;
import calypsox.tools.santfilesaver.SantFileSaverException;

//Project: Bloomberg tagging

/**
 * Import bonds into Calypso
 *
 * @author aela
 */
public class CalypsoMLBondIEEngine extends CalypsoMLIEEngine {

    /**
     * Constructor
     *
     * @param configName config name use to create the IEAdapter
     * @param dsCon      DataServer connection
     * @param hostName   hostname of the EventServer
     * @param esPort     port of the EventServer
     */
    public CalypsoMLBondIEEngine(DSConnection dsCon, String hostName,
                                 int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Generate a message to send back to the MiddleWare
     *
     * @param object          LegalEntity imported
     * @param exception       exception exception if some
     * @param externalMessage original message
     * @return The response of the bond into calypso
     */
    @Override
    protected JMSQueueAnswer generateAnswer(final Object object,
                                            final Exception exception, final ExternalMessage message) {
        final JMSQueueAnswer answer = new BondJMSQueueAnswer(message);
        
        if (message.getText().contains("RDFlowXML") || message.getText().contains("RDFlowTransaction")) {
        	answer.setTransactionKey(ImporterUtil.getInstance().getXMLTagValue(message, "transactionKey"));
        	
        	if (exception == null) {
        		answer.setCode(JMSQueueAnswer.OK);
        		answer.setDescription("Successfully imported in Calypso.");
        	}
        	else {
        		answer.setException(exception);        		
        	}
        }
        else {
        	if (object instanceof Bond) {
        		Bond bond = (Bond) object;

        		answer.setCode(JMSQueueAnswer.OK);
        		answer.setDescription("Bond successfully imported in Calypso.");
        		answer.setDestinationKey(String.valueOf(bond.getId()));

        		String isin = bond.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

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
        	
        	String bondsEngineName = CalypsoMLBondIEEngine.class.getSimpleName().toLowerCase();
        	try {
        		String refInterna = CalypsoMLBondIEEngine.getRefInternaFromXML(message.getText());
    			SantFileSaver.saveFile(bondsEngineName, "out", "xml", "bond_out_" + refInterna, 0L, answer.toString());
    		} catch (SantFileSaverException e1) {
    			Log.error(bondsEngineName, "Error saving message in a file.");
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
        return SantDefaultBondImporterAdapter.class;
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
     * @return task
     */
    @Override
    protected Task buildTask(final String message,
                             final JMSQueueAnswer answer) {


        Task task;
        if (answer != null) {


            task = buildTask(message, 0, "EX_BOND_IMPORT",
                    Task.EXCEPTION_EVENT_CLASS);

            if (answer.getDestinationKey() != null) {

                task.setObjectLongId(Long.parseLong(answer.getDestinationKey()));
                task.setObjectClassName(Bond.class.getName());
            }
        } else {

            task = new Task();
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
    
    public static String getRefInternaFromXML(String xml) {
    	String patternString = "REF_INTERNA.*?value.*?>([A-Z][0-b9].[A-Z]{2}.[A-Z]{3}.[0-9]+)<";
		Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(xml);
		
		while(matcher.find()) {
			return matcher.group(1);
		}
    	
    	return "";
    }
}
