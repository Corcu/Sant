/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.calypso.engine.configuration.EngineDescription;
import com.calypso.engine.configuration.EngineNotConfiguredException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteEngineManagerService;
import com.calypso.tk.util.ScheduledTask;

/**
 * This schedule task allows to start, stop, restart engines
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 28/07/2016
 * 
 */

public class ScheduledTaskEngineManagement extends ScheduledTask {

	/**
	 * CONSTANTS DEFINITION 
	 */
	/*----------------------------------------------------------------------------------------------*/
	/**
	 * Enum containing the domain attributes constants.
	 */
	private enum DOMAIN_ATTRIBUTES {

		ACTION("Action:"), //1 
		WAIT_TIME("sleep Time in ms:"), //2 
		SPECIFIC_ENGINE("Select engines:"); //3

		private final String desc;

		// add description
		private DOMAIN_ATTRIBUTES(String d) {
			this.desc = d;
		}

		// return the description
		public String getDesc() {
			return this.desc;
		}

	} // end ENUM DOMAIN_ATTRIBUTES
	/*----------------------------------------------------------------------------------------------*/

	/**
	 * Enum to ACTIONS
	 */
	private enum ENUM_ACTIONS {

		START, // 1
		STOP, // 2
		RESTART; //

		public static List<String> getNames() {
			ArrayList<String> n = new ArrayList<String>(ENUM_ACTIONS.values().length);
			for (ENUM_ACTIONS e : ENUM_ACTIONS.values())
				n.add(e.toString().toUpperCase());
			return n;
		}
	}
	

	/*----------------------------------------------------------------------------------------------*/
	/*
	 * Name of this Schedule task
	 */
	private final static String TASK_INFORMATION = "Allows management of the engines";

	/*-----------------------------------------------------------------------------------------------*/
	
	private final static String ALL = "ALL";
	/*
	 * Variables
	 */
	private RemoteEngineManagerService service = null;
	

	/**
	 * Builds the custom attributes and values
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {

		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.ACTION.getDesc()).domain(ENUM_ACTIONS.getNames()));
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.WAIT_TIME.getDesc()).integer());
		
		final List<String> options = new ArrayList<String>();
		options.add(ALL);
		try {
			options.addAll(getEnginesNames());
		} catch (CalypsoServiceException | EngineNotConfiguredException e) {
			Log.error(this, e);
		}
		attributeList.add(attribute(DOMAIN_ATTRIBUTES.SPECIFIC_ENGINE.getDesc()).domain(options));
		
		return attributeList;
	}
	// //////////////////////////////////////////////
	// //////// OVERRIDE METHODS ///////////////////
	// ////////////////////////////////////////////

	/**
	 * Main method to be executed in this Scheduled task
	 * 
	 * @param connection
	 *            to DS
	 * @param connection
	 *            to PS
	 * @return result of the process
	 */
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {
		
		// read attributes
		final String action =super.getAttribute(DOMAIN_ATTRIBUTES.ACTION.getDesc());
		final Integer sleepTime = Integer.parseInt(super.getAttribute(DOMAIN_ATTRIBUTES.WAIT_TIME.getDesc()));
		try {
			
			final Collection<String> enginesNames = getEnginesNames();
			
			// restart all engines
			if (ENUM_ACTIONS.START.toString().equals(action)) {
				
				startAllEngines(enginesNames,sleepTime);
				
				//stop all engines
			} else if (ENUM_ACTIONS.STOP.toString().equals(action)) {
				
				stopAllEngines(enginesNames,sleepTime);
				
				//stop&start all engines
			} else {
				
				stopAllEngines(enginesNames,sleepTime);
				startAllEngines(enginesNames,sleepTime);
			}
				
		} catch (CalypsoServiceException | EngineNotConfiguredException | InterruptedException e) {
			
			Log.error(this, e);		
			return false;
		}
		
		return true;
	}

	
	/*-----------------------------------------------------------------------------------------------*/
	/**
	 * @return this task information, gathered from the constant
	 *         TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/*-----------------------------------------------------------------------------------------------*/
	/**
	 * Ensures that the attributes have a value introduced by who has setup the
	 * schedule task
	 * 
	 * @return if the attributes are ok
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

		boolean retVal = super.isValidInput(messages);

		for (DOMAIN_ATTRIBUTES attribute : DOMAIN_ATTRIBUTES.values()) {

			final String value = super.getAttribute(attribute.getDesc());

			if (Util.isEmpty(value)) {

				messages.addElement(attribute.getDesc() + " attribute is not specified.");
				retVal = false;
			}
		}

		return retVal;
	}

	/*---------------------------------------------------------------------------------------------------------------*/

	///////////////////////////////////////////////////////////////////////
	////////////////// PRIVATE METHODS ///////////////////////////////////
	/////////////////////////////////////////////////////////////////////

	/**
	 *
	 * @param enginesNames to start, ALL starts all engines
	 * @param sleepTime between each thread
	 * @throws InterruptedException
	 */
	private void startAllEngines(Collection<String> enginesNames, Integer sleepTime) throws InterruptedException {
		
		final String engineSelection =super.getAttribute(DOMAIN_ATTRIBUTES.SPECIFIC_ENGINE.getDesc());
		
		if (engineSelection.equals(ALL)){
			
			for (String engineName : enginesNames){
				
				final StartAllEngines thread = new StartAllEngines(engineName, service);
				thread.start();
				Thread.sleep(sleepTime.longValue());
				thread.join();
			}
			
		} else {
			
				final StartAllEngines thread = new StartAllEngines(engineSelection, service);
				thread.start();
				thread.join();
		}
		
	}

	/**
	 *
	 * @param enginesNames to stopo, ALL stops all engines
	 * @param sleepTime between each thread
	 * @throws InterruptedException
	 */
	private void stopAllEngines(Collection<String> enginesNames, Integer sleepTime) throws InterruptedException {
		
		final String engineSelection =super.getAttribute(DOMAIN_ATTRIBUTES.SPECIFIC_ENGINE.getDesc());
		
		if (engineSelection.equals(ALL)){
			
			for (String engineName : enginesNames){
				
				final StopAllEngines thread = new StopAllEngines(engineName, service);
				thread.start();
				Thread.sleep(sleepTime.longValue());
				thread.join();
			}
			
		} else {
				final StopAllEngines thread = new StopAllEngines(engineSelection, service);
				thread.start();
				thread.join();
		}		
	}
	
	/**
	 * 
	 * @return RemoteEngineManagerService
	 */
	private RemoteEngineManagerService getEngineServiceManager(){
		
		if (service==null)
			return DSConnection.getDefault().getService(RemoteEngineManagerService.class);
		
		return service;	
	}
	
	/**
	 * 
	 * @return collection with all the engines in the system
	 * @throws CalypsoServiceException
	 * @throws EngineNotConfiguredException
	 */
	private Collection<String> getEnginesNames() throws CalypsoServiceException, EngineNotConfiguredException{
		
		this.service = getEngineServiceManager();
		Vector<String> v = new Vector<String>();
		Set<EngineDescription> enginesDescription =	service.getAllEngineDescriptions();
		
		for(EngineDescription engine : enginesDescription){
			v.add(engine.getName());
		}
		
		return v;		
	}
	
	/*
	 * Inner class to manage the start of each engine
	 *
	 */
	public class StartAllEngines extends Thread {
		
		private String ENGINE_NAME;
		private RemoteEngineManagerService SERVICE;
		
		public StartAllEngines(String engineName, RemoteEngineManagerService service){
			ENGINE_NAME = engineName;
			SERVICE = service;
		}
		
	    public void run(){
	    	
	    	try {
	    		
	    	SERVICE.removeEngineMessage(ENGINE_NAME);
	    	SERVICE.startEngine(ENGINE_NAME);
	    	
	    	} catch (CalypsoServiceException | EngineNotConfiguredException e) {
	    		Log.error(this, e);
	    	}
	    }	
	}
	
	/*
	 * Inner class to manage the stop of each engine
	 *
	 */
	public class StopAllEngines extends Thread {
		
		private String ENGINE_NAME;
		private RemoteEngineManagerService SERVICE;
		
		public StopAllEngines(String engineName, RemoteEngineManagerService service){
			ENGINE_NAME = engineName;
			SERVICE = service;
		}
		
	    public void run(){
	    	
	    	try {
	    		
	    	SERVICE.removeEngineMessage(ENGINE_NAME);
	    	SERVICE.stopEngine(ENGINE_NAME);
	    	
	    	} catch (CalypsoServiceException | EngineNotConfiguredException e) {
	    		Log.error(this, e);
	    	}
	    }	
	}
}