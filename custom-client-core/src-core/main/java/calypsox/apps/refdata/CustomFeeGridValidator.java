package calypsox.apps.refdata;

import java.awt.Frame;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.refdata.SantMarginCallStaticDataFilter;
import calypsox.tk.refdata.SantTradeStaticDataFilter;

import com.calypso.apps.refdata.FeeGridValidator;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.FeeGrid;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

public class CustomFeeGridValidator implements FeeGridValidator {
	public static final String FEE_SDF_PREFIX = "IA_";
	public static final String FEEPO_SDF_PREFIX = "IA_PO_";
	public static final String SDF_GROUP_NAME = "FeeGrid";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(FeeGrid grid, Frame w, Vector messages) {
		// Ugly but no choice for the moment,
		// we have to add a static data filter on the feeGrid if it was created from the margin call config window.
		// to do so, we will check if the margin call attributes are present

		if (grid != null) {
			String mccId = grid.getAttribute("MARGIN_CALL_CONTRACT_ID");
			if (Util.isEmpty(mccId)) {
				return true;
			}
			MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), Integer.parseInt(mccId));
			if (mcc == null) {
				return true;
			}

			// check if a static data filter is used
			String sdfName = grid.getStaticDataFilter();

			StaticDataFilter sdf = null;
			if (!Util.isEmpty(sdfName)) {
				sdf = BOCache.getStaticDataFilter(DSConnection.getDefault(), sdfName);
			}

			if (sdf != null) {
				// update the existing sdf
				Vector<StaticDataFilterElement> elements = sdf.getElements();
				if (!Util.isEmpty(elements)) {
					for (StaticDataFilterElement sdfElement : elements) {
						if (("KEYWORD." + CollateralStaticAttributes.MC_CONTRACT_NUMBER).equals(sdfElement.getName())) {
							Vector<String> v = new Vector<String>();
							v.add("" + mcc.getId());
							sdfElement.setValues(v);
							try {
								DSConnection.getDefault().getRemoteReferenceData().save(sdf);
							} catch (RemoteException e) {
								Log.error(this, e);
								messages.add("Unable to update SDF " + sdfName);
								return false;
							}
						}
					}
				}
			} else {
				// create a new sdf
				try {
					StaticDataFilter newSDF = createFeeGridStaticDataFilter(mcc, grid);
					grid.setStaticDataFilter(newSDF.getName());
				} catch (RemoteException e) {
					Log.error(this, e);
					messages.add("Unable to create an SDF for this Fee Grid");
					return false;
				}
			}

		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private StaticDataFilter createFeeGridStaticDataFilter(MarginCallConfig mcc, FeeGrid grid) throws RemoteException {
		String feeGridSDFName = getFilterNameToSave(FEE_SDF_PREFIX + mcc.getName());

		if ("ProcessingOrg".equals(grid.getAttribute("MARGIN_CALL_PAYER_ROLE"))) {
			feeGridSDFName = getFilterNameToSave(FEEPO_SDF_PREFIX + mcc.getName());
		}
		// check if the sdf already exists
		StaticDataFilter existingSDF = BOCache.getStaticDataFilter(DSConnection.getDefault(), feeGridSDFName);

		if (existingSDF != null) {
			return existingSDF;
		}

		StaticDataFilter feeSDFilter = new StaticDataFilter(feeGridSDFName);

		Set<String> setGroup = new HashSet<String>();
		setGroup.add(SDF_GROUP_NAME);

		StaticDataFilterElement feeSDFElementIsNegPosNPV = null;
		StaticDataFilterElement isCollateralizableSDFElement = new StaticDataFilterElement(
				SantMarginCallStaticDataFilter.IS_COLLATERALIZABLE_TRADE);

		if ("ProcessingOrg".equals(grid.getAttribute("MARGIN_CALL_PAYER_ROLE"))) {
			feeSDFElementIsNegPosNPV = new StaticDataFilterElement(SantTradeStaticDataFilter.HAS_NEGATIVE_NPV);
		} else {
			feeSDFElementIsNegPosNPV = new StaticDataFilterElement(SantTradeStaticDataFilter.HAS_POSITIVE_NPV);
		}

		StaticDataFilterElement adjSDFElementMCC_ID = new StaticDataFilterElement("KEYWORD."
				+ CollateralStaticAttributes.MC_CONTRACT_NUMBER);

		// Values for mcc id element.
		Vector<String> values = new Vector<String>();
		// if it's a child independent amount contract, then use the parent id
		if (MarginCallConfig.SUBTYPE_INDEPENDENT_AMOUNT.equals(mcc.getSubtype())) {
			values.add("" + mcc.getParentId());
		} else {
			values.add("" + mcc.getId());
		}
		adjSDFElementMCC_ID.setValues(values);
		adjSDFElementMCC_ID.setType(StaticDataFilterElement.STRING_ENUMERATION);

		// Values for is negative.
		feeSDFElementIsNegPosNPV.setIsValue(true);
		feeSDFElementIsNegPosNPV.setType(StaticDataFilterElement.IS);

		// Values for is negative.
		isCollateralizableSDFElement.setIsValue(true);
		isCollateralizableSDFElement.setType(StaticDataFilterElement.IS);

		feeSDFilter.setComment("SDF Created automatically at contract validation for technical trades");
		feeSDFilter.setGroups(setGroup);
		Vector<StaticDataFilterElement> elements = new Vector<StaticDataFilterElement>();
		elements.add(isCollateralizableSDFElement);
		elements.add(feeSDFElementIsNegPosNPV);
		elements.add(adjSDFElementMCC_ID);
		feeSDFilter.setElements(elements);
		boolean sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(feeSDFilter);
		if (!sdfSaved) {
			throw new RemoteException("Static Data Filter " + feeSDFilter.getName() + " not saved.");
		}

		return feeSDFilter;
	}

	private String getFilterNameToSave(String filterName) {
		if (Util.isEmpty(filterName)) {
			return filterName;
		}
		if (filterName.length() > 32) {
			return filterName.substring(0, 32);

		} else {
			return filterName;
		}
	}
}
