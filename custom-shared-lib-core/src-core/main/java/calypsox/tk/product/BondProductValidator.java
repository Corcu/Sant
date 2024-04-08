/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.product;

import java.rmi.RemoteException;
import java.util.Vector;

import com.calypso.apps.product.ShowProduct;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.AuthUtil;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PendingModification;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.DefaultProductValidator;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.event.PSEventBloombergUpdate;
import calypsox.tk.event.PSEventProduct;
import calypsox.util.collateral.CollateralUtilities;

//Project: Bloomberg tagging

/**
 * A quick control to avoid saving bonds with a wrong date roll frequency. It
 * has to be review when bonds will be imported via an interface (and not
 * fasttrack)
 * 
 * @author aela
 * 
 */
public class BondProductValidator extends DefaultProductValidator {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean isValidInput(Product product, ShowProduct window, Vector messages) {
		boolean res = false;
		
		if (product instanceof Bond) {
			Bond bond = (Bond) product;
			boolean isValidBond = super.isValidInput(product, window, messages);
			boolean isValidIsinCcyPair = isValidIsinCurrencyPair(product, messages);
			String isin = product.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

			if (isValidBond && isValidIsinCcyPair) {
				if (!CollateralUtilities.isValidISINValue(isin, messages)){
					return false;
				}
				
				try {
					createPsEventBloombergUpdate(isin);
				} catch (CalypsoServiceException cse) {
					Log.error(this, "Couldn't publish the MarginCallQef Events: " + cse.getMessage());
					Log.error(this, cse); // sonar purpose
				}
			}

			// Removed flow generation because it would replace custom flows
			
			res = isValidBond && isValidIsinCcyPair;
			
			if (res && checkIfRealModification(bond)) {
				try {
					PSEventProduct event = new PSEventProduct();
					event.setProduct(product);
		            DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
		        } catch (CalypsoServiceException exc) {
		            Log.error(this.getClass().getSimpleName(), "Couldn't publish event - " + exc.toString());
		        }
			}
		}
		
		
		
		return res;
	}
	
	private boolean checkIfRealModification(Bond bond) {
		Bond oldbond = null;
		if (bond.getId() > 0) {
		   try {
		      oldbond = (Bond)DSConnection.getDefault().getRemoteProduct().getProduct(bond.getId());
		   } catch (Exception e) {
		      Log.error(this, e);
		   }
		}
		
		if (oldbond == null) {
			return true;
		}
		
		Vector<PendingModification> diffs = new Vector<PendingModification>();
		AuthUtil.diff(bond, oldbond, diffs, DSConnection.getDefault().getUser(), new JDatetime());
		
		if (diffs.size() > 0) {
			Vector ignoredModifications = LocalCache.getDomainValues(DSConnection.getDefault(), "BondIgnoreChanges");
			
			if (ignoredModifications != null && ignoredModifications.size() > 0) {
				for (int i = diffs.size() - 1; i >= 0; i--) {
					PendingModification currentModif = (PendingModification)diffs.get(i);
					
					if (ignoredModifications.contains(currentModif.getEntityFieldName())) {
						diffs.remove(i);
					}
				}
			}
			
			if (diffs.size() > 0) {
				return true;
			}
		}
		
		return false;
	}

	// private boolean isUserAllowed(Bond bond) {
	// AccessUtil.isAuthorized("AddDividend");
	// return false;
	// }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean isValidIsinCurrencyPair(Product bond, Vector messages) {

		String isin = bond.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN);

		if (Util.isEmpty(isin)) {
			return true;
		}

		// get products matching by ISIN - BAU DPM 12/05/15 - Fix ISIN with
		// spaces
		Vector<Product> matchingProducts = getMatchingProductsByIsin(isin.trim());

		// if already exist products, check currency
		if (!Util.isEmpty(matchingProducts)) {
			for (Product product : matchingProducts) {
				if (!(product instanceof Bond)) {
					continue;
				}
				if (bond.getCurrency().equals(product.getCurrency())) {
					// alta nueva
					if (bond.getId() != product.getId()) {
						messages.add("Already exists in the system one bond with ISIN = " + isin + " and Currency = "
								+ bond.getCurrency());
						return false;
					}
				}
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private Vector<Product> getMatchingProductsByIsin(String isin) {

		Vector<Product> matchingProducts = null;

		try {
			matchingProducts = DSConnection.getDefault().getRemoteProduct()
					.getProductsByCode(CollateralStaticAttributes.BOND_SEC_CODE_ISIN, isin);
		} catch (RemoteException e) {
			Log.error(this, "Cannot get any bond related to ISIN = " + isin + ".\n", e);
		}

		return matchingProducts;

	}

	/**
	 * createPsEventBloombergUpdate.
	 * 
	 * @param tituloId
	 *            String
	 * @throws CalypsoServiceException
	 */
	private void createPsEventBloombergUpdate(String tituloId) throws CalypsoServiceException {
		PSEventBloombergUpdate event = new PSEventBloombergUpdate(tituloId, 0);

		DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
	}

}
