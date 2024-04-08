/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice.response;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

/**
 * handler to do the JAXB validations of the responses received from EFS. This class allows to match the XML schema and,
 * in case of error, indicate what error has occurred
 * 
 * @author Guillermo Solano
 * @version 0.1
 * 
 */
public class EFSResponseSchemaValidator implements ValidationEventHandler {

	/**
	 * Checks the XML message respects the schema.
	 * 
	 * @see javax.xml.bind.ValidationEventHandler#handleEvent(javax.xml.bind.ValidationEvent)
	 * @return true if the message is valid
	 */
	// GSM: used now for tests, what would be nice to use this data to inform the log
	// In case filtering is required in a future, this is the starting point
	@Override
	public boolean handleEvent(ValidationEvent event) {

		System.out.println("\nEVENT");
		System.out.println("SEVERITY:  " + event.getSeverity());
		System.out.println("MESSAGE:  " + event.getMessage());
		System.out.println("LINKED EXCEPTION:  " + event.getLinkedException());
		System.out.println("LOCATOR");
		System.out.println("    LINE NUMBER:  " + event.getLocator().getLineNumber());
		System.out.println("    COLUMN NUMBER:  " + event.getLocator().getColumnNumber());
		System.out.println("    OFFSET:  " + event.getLocator().getOffset());
		System.out.println("    OBJECT:  " + event.getLocator().getObject());
		System.out.println("    NODE:  " + event.getLocator().getNode());
		System.out.println("    URL:  " + event.getLocator().getURL());

		return true;
	}

}
