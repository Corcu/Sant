/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory.util;

import static calypsox.engine.inventory.SantPositionConstants.BLOQUEO;

import java.util.Vector;

import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.engine.inventory.SantPositionBean;

/**
 * Recovers the value of FI positions for actual, theoretical, pledge & unavailable
 * 
 * @author Patrice Guerrido & Guillermo Solano
 * @version 2.0
 * @date 20/01/2014
 */
public class FetchCurrentPositionHelper {

	// FO Position should have a dummy agent
	// BO Position should have a dummy Book.
	private static final String INV_DUMMY_AGENT = "INV_DUMMY_AGENT";
	private static final String INV_DUMMY_BOOK = "INV_DUMMY_BOOK";
	private static Vector<String> dummyBooks = null;
	private static Vector<String> dummyAgents = null;

	/**
	 * Get the position value for the Pos bean
	 * 
	 * @param posBean
	 * @return
	 * @throws Exception
	 */
	public double get(final SantPositionBean posBean) throws Exception {

		final boolean isBloqueo = posBean.getPositionType().equals(BLOQUEO);

		if (isBloqueo) {
			return getBloqueoPosition(posBean);
		}

		// DPM & GSM: 04/11/2014.
		/*
		 * MarginCall positions are already filtered by GD. For this reason, the call to getFOPosition is not required,
		 * as rests the MC position to the Internal Position.
		 */

		// final boolean isBOPosition = isBackOfficePosition(posBean);
		//
		// if (isBOPosition) {
		// return getBOPosition(posBean);
		// }
		return getBOPosition(posBean);
	}

	/**
	 * Return the value of the BO position
	 * 
	 * @param posBean
	 * @return
	 * @throws Exception
	 */
	private double getBOPosition(final SantPositionBean posBean) throws Exception {

		double totalInternalPosition = SecInventoryPositionLoader.fecthInternalPosition(posBean, false);
		return totalInternalPosition;
	}

	/**
	 * get bloqueos (pledge) positions
	 */
	private double getBloqueoPosition(SantPositionBean posBean) throws Exception {
		double totalInternalPledgeOutPosition = SecInventoryPositionLoader.fecthInternalPosition(posBean, true);
		return totalInternalPledgeOutPosition;
	}

	// NOT USED ANYMORE:

	/**
	 * If it's or not a BO position
	 * 
	 * @param posBean
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private boolean isBackOfficePosition(final SantPositionBean posBean) throws Exception {

		loadDummyDVs();

		if (dummyBooks.contains(posBean.getBook().getName())) {
			return true;
		}
		if (dummyAgents.contains(posBean.getAgent().getName())) {
			return false;
		}
		throw new Exception("Cannot determine if it is a back-office or a front-office position - check domainValues"
				+ INV_DUMMY_AGENT + ", " + INV_DUMMY_AGENT);
	}

	/*
	 * Caches de DV for the dummies books and agents (normally only one)
	 */
	private void loadDummyDVs() {

		if ((dummyBooks == null) || (dummyAgents == null) || dummyBooks.isEmpty() || dummyAgents.isEmpty()) {

			dummyBooks = LocalCache.getDomainValues(DSConnection.getDefault(), INV_DUMMY_BOOK);
			dummyAgents = LocalCache.getDomainValues(DSConnection.getDefault(), INV_DUMMY_AGENT);
		}
	}

	// /**
	// * Return the value of the FO position
	// *
	// * @param posBean
	// * @return
	// * @throws Exception
	// */
	// @SuppressWarnings("unused")
	// private double getFOPosition(SantPositionBean posBean) throws Exception {
	//
	// double totalInternalPosition = SecInventoryPositionLoader.fecthInternalPosition(posBean, false, false);
	// double totalMarginCallPosition = SecInventoryPositionLoader.fecthMarginCallPosition(posBean);
	//
	// return -totalMarginCallPosition + totalInternalPosition;
	// }

}
