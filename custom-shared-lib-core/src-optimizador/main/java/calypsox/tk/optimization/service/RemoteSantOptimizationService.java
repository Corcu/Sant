package calypsox.tk.optimization.service;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.JDate;

/**
 * Custom remote services for the optimization module
 * 
 * @author Guillermo Solano
 * 
 */

public interface RemoteSantOptimizationService {

	public static final String SERVER_NAME = "SantOptimizationService";

	/**
	 * Get haircut close_quote from products received as ids
	 * 
	 */
	Map<Integer, Double> getProductHaircutQuoteMap(Vector<Integer> productIds, String quoteSetName, JDate valDate)
			throws RemoteException;
	
	/**
	 * @return the list of all contracts names with their ids
	 * @throws RemoteException
	 */
	Map<String, Double> getAllContractNamesForIds()
			throws RemoteException;
	
	/**
	 * @return unique id for a given seedName
	 * @throws RemoteException
	 */
	Integer nextSeed(String seedName)
			throws RemoteException;

	Long getOptimImportExecutionID(long defaultExecId) throws RemoteException;
}
