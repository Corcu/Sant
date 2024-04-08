/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.gestorstp;

import com.calypso.tk.service.DSConnection;

/**
 * This class implements GestorSTPIncomingMessageEngine for processing MT568,
 * MT569, MT900/MT910 for UK
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 19/10/2016
 * 
 */
public class UKGestorSTPIncomingMessageEngine extends GestorSTPIncomingMessageEngine {
	
	public static String ENGINE_NAME_UK = "SANT_UK_GestorSTPIncomingMessageEngine";

	/**
	 * 
	 * @param dsCon
	 * @param hostName
	 * @param port
	 */
	public UKGestorSTPIncomingMessageEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
	}
	
	/**
	 * Name of the engine that offers this service
	 */
	@Override
	public String getEngineName() {
		return ENGINE_NAME_UK;
	}

}
