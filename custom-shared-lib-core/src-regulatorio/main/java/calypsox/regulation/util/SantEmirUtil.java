/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.regulation.util;

import static calypsox.tk.report.CollateralizedTradesReport.NO_VALUEDATE_ERROR_MESSAGE;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.tk.report.CollateralizedTradesReportLogic;
import calypsox.util.collateral.CollateralUtilities;

/**
 * SantEmirUtil contains utils for the generation of CVM and CLM messages for
 * EMIR report.
 *  
 */
public class SantEmirUtil {

	/**
	 * Accepted trade status.
	 */
	private static final String ACCEPTED_STATUS = "VERIFIED MATURED CANCELED TERMINATED";

	/**
	 * Class Path
	 */
	private static final String PATH = "calypsox.regulation.util.SantEmirUtil.java";

	private static final String DEFAULT_POS_NAMES = "('BSTE','BDSD','BSHK','BSNY','BECM','WK15') ";

	/**
	 * Default constructor.
	 */
	protected SantEmirUtil() {
		// nothing to do
	}

	/**
	 * Return Legal Entity attribute from a given type.
	 * 
	 * @param trade
	 *            Trade
	 * @param attributeType
	 *            String
	 * @param isCptyAttr
	 *            boolean
	 * @return String Attribute value from a given type.
	 */
	public static String getLegalEntityAttribute(final Trade trade, final String attributeType,
			final boolean isCptyAttr) {
		int leId = 0;
		final int poId = trade.getBook().getLegalEntity().getId();
		if (isCptyAttr) {
			leId = trade.getCounterParty().getEntityId();
		} else {
			leId = trade.getBook().getLegalEntity().getId();
		}

		String attrValue = null;

		LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poId, leId, "ALL",
				attributeType);
		if (attr == null) {
			attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, leId, "ALL", attributeType);
		}
		if (attr != null) {
			attrValue = attr.getAttributeValue();
		}
		return attrValue;
	}

	/**
	 * Return Legal Entity attribute from a given type.
	 * 
	 * @param po
	 *            LegalEntity
	 * @param le
	 *            LegalEntity
	 * @param attributeType
	 *            String
	 * @param isCptyAttr
	 *            boolean
	 * @return String Attribute value from a given type.
	 */
	public static String getLegalEntityAttribute(final LegalEntity po, final LegalEntity cpty,
			final String attributeType, final boolean isCptyAttr) {

		int leId = 0;
		final int poId = po.getId();
		if (isCptyAttr) {
			leId = cpty.getEntityId();
		} else {
			leId = poId;
		}

		String attrValue = null;

		LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poId, leId, "ALL",
				attributeType);
		if (attr == null) {
			attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, leId, "ALL", attributeType);
		}
		if (attr != null) {
			attrValue = attr.getAttributeValue();
		}
		return attrValue;
	}

	/**
	 * Check if trade is internal or not checking the attribute mirror tradeId.
	 * 
	 * @param trade
	 *            Trade
	 * @return true or false if trade is internal or not
	 */
	public final static boolean isInternal(final Trade trade) {

		boolean rst = false;

		DSConnection ds = DSConnection.getDefault();

		final LegalEntity cp = trade.getCounterParty();
		final Collection<LegalEntityAttribute> cpAtts = BOCache.getLegalEntityAttributes(ds, cp.getId());

		final LegalEntity po = trade.getBook().getLegalEntity();
		final Collection<LegalEntityAttribute> poAtts = BOCache.getLegalEntityAttributes(ds, po.getId());

		final Iterator<LegalEntityAttribute> poIter = poAtts.iterator();
		LegalEntityAttribute currentAtt = null;
		String poLei = "poLei";

		while (poIter.hasNext()) {
			currentAtt = poIter.next();
			// BAU 1.2.0 . Comprueba que el valor del atributo no es nulo antes
			// de asignar ese valor a poLei
			if ("LEI".contains(currentAtt.getAttributeType()) && (!Util.isEmpty(currentAtt.getAttributeValue()))) {
				poLei = currentAtt.getAttributeValue();
			}
		}
		// JRL Migration V14.4
		if (cpAtts != null) {
			final Iterator<LegalEntityAttribute> cpIter = cpAtts.iterator();
			String cpLei = "cpLei";

			while (cpIter.hasNext()) {
				currentAtt = cpIter.next();
				// BAU 1.2.0 . Comprueba que el valor del atributo no es nulo
				// antes de asignar ese valor cpLei
				if ("LEI".contains(currentAtt.getAttributeType()) && (!Util.isEmpty(currentAtt.getAttributeValue()))) {
					cpLei = currentAtt.getAttributeValue();
				}
			}

			if ((trade.getMirrorTradeId() != 0) || poLei.equalsIgnoreCase(cpLei)) {
				rst = true;
			}
		}
		Log.debug(PATH + "isInternal: ", "Is Internal rst:" + rst);
		return rst;
	}

	/**
	 * Check if trade has an accepted status (VERIFIED, CANCEL).
	 * 
	 * @param trade
	 *            Trade
	 * @return true or false if the trade has an accepted status or not
	 */
	public final static boolean isStatusAccepted(final Trade trade) {

		boolean rst = false;

		if (!Util.isEmpty(trade.getStatus().toString()) && ACCEPTED_STATUS.contains(trade.getStatus().toString())) {
			rst = true;
		}

		return rst;
	}

	/**
	 * Check if collateral contract is opened.
	 * 
	 * @param mcd
	 *            MarginCallEntryDTO
	 * @return true or false if trade is internal or not
	 */
	public final static boolean isCollateralContractOpened(final MarginCallEntryDTO mce) {

		boolean rst = true;
		int mcId = 0;
		MarginCallConfig mc_conf = null;

		if (mce.getCollateralConfigId() != 0) {
			mcId = mce.getCollateralConfigId();
			mc_conf = BOCache.getMarginCallConfig(DSConnection.getDefault(), mcId);
		}

		if (mc_conf != null &&!Util.isEmpty(mc_conf.getAgreementStatus())) {
			if (mc_conf.getAgreementStatus().equals("CLOSED")) {
				rst = false;
			}
		}
		return rst;
	}

	/**
	 * Check if the trade has UTI or BO_REFERENCE.
	 * 
	 * @param mcde
	 *            MarginCallDetailEntryDTO
	 * @return true or false depending on if has UTI or BO_REFERENCE.
	 * @throws RemoteException
	 */
	public final static boolean hasUtiOrBoReference(final Trade trade) throws RemoteException {

		boolean rst = true;

		/**
		 * Uti Keyword reference.
		 */
		String UtiKW = "UTI_REFERENCE";

		/**
		 * bo_reference Keyword reference.
		 */
		String boReferenceKW = "BO_REFERENCE";

		if ((Util.isEmpty(trade.getKeywordValue(UtiKW))) && (Util.isEmpty(trade.getKeywordValue(boReferenceKW)))) {
			rst = false;
		}
		return rst;
	}

	/**
	 * Retrives all the MarginCallEntryDTO of a given date (therefore, all
	 * Collaterals trade extracted from this method are collateralized). This is
	 * used in the Batch mode process
	 * 
	 * @param processDate
	 * @param reportTemplate
	 * 
	 * @return a list with all the MCDEntries that belong to the current system
	 *         day when the call is done
	 */
	public static List<MarginCallEntryDTO> retriveMCAliveEntryList(final List<String> errors, JDate processDate,
			ReportTemplate reportTemplate) {

		List<MarginCallEntryDTO> finalMCEList = null;

		Log.debug(PATH + "retriveMCAliveEntryList: ", "1. retriveMCAliveEntryList -> Start call");

		if (processDate == null) {

			Log.error(PATH + "retriveMCAliveEntryList: ", NO_VALUEDATE_ERROR_MESSAGE);

		}

		// retrieve only products required for EMIR
		String where = "mrgcall_config.mrg_call_def = margin_call_entries.mcc_id"
				+ " and mrgcall_config.agreement_status = 'OPEN' and mrgcall_config.contract_type in "
				+ getEmirContractTypesFromDV() + "and (mrgcall_config.product_list like "
				+ getEmirProductTypesFromDV2MCCTable() + ") and trunc(margin_call_entries.process_date) = "
				+ Util.date2SQLString(processDate)
				+ " and mrgcall_config.process_org_id in (select LEGAL_ENTITY_ID from legal_entity where short_name in "
				// GSM 31/07/15. SBNA Multi-PO filter
				+ getEmirReportablePOsFromDV(reportTemplate) + ")";
		// JRL MIG 14.4
		// where += " order by margin_call_entries.process_date desc";

		// get margin call detail entries for trades
		try {
			finalMCEList = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallEntries(where,
					Arrays.asList("mrgcall_config", "margin_call_entries"));

		} catch (RemoteException e) {
			errors.add("SantEmirUtil.java: NOT POSSIBLE to generate the List of MarginCallEntryDTO");
			Log.error(PATH + "retriveMCAliveEntryList: ", e);

		}

		Log.debug(PATH + "retriveMCAliveEntryList: ", "2. retriveMCAliveEntryList -> Return list MCDE. End");

		return finalMCEList;
	}

	/**
	 * Retrives the margin call entries of the head collateral contract for a
	 * given clone contract.
	 * 
	 * @param errors
	 * 
	 * @param processDate
	 * 
	 * @return a list with all the head MCDEntries that belong to a given mcc
	 *         head name
	 */
	public static List<MarginCallEntryDTO> retriveHeadMCE(final List<String> errors, JDate processDate,
			String MCC_HEAD) {

		List<MarginCallEntryDTO> finalMCEList = null;

		Log.debug(PATH + "retriveHeadMCE: ", "1. retriveHeadMCE -> Start call");

		if (processDate == null) {

			Log.error(CollateralizedTradesReportLogic.class, NO_VALUEDATE_ERROR_MESSAGE);

		}

		// retrieve only products required for EMIR
		String where = "mrgcall_config.mrg_call_def = margin_call_entries.mcc_id "
				+ "and mrgcall_config.agreement_status = 'OPEN' and mrgcall_config.contract_type in "
				+ getEmirContractTypesFromDV() + "and (mrgcall_config.product_list like "
				+ getEmirProductTypesFromDV2MCCTable() + ") and trunc(margin_call_entries.process_date) = "
				+ Util.date2SQLString(processDate) + "and description= '" + MCC_HEAD + "'";

		// get margin call detail entries for trades
		try {
			finalMCEList = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallEntries(where,
					Arrays.asList("mrgcall_config", "margin_call_entries"));

		} catch (RemoteException e) {
			errors.add("SantEmirUtil.java: NOT POSSIBLE to generate the List of MarginCallEntryDTO");
			Log.error(PATH + "retriveHeadMCE: ", e);

		}

		Log.debug(PATH + "retriveHeadMCE: ", "2. retriveHeadMCE -> Return list MCDE. End");

		return finalMCEList;
	}

	/**
	 * Get value of the collateral clone.
	 * 
	 * @return value of the collateral clone
	 */
	public static double getLogicValueOfTheCollateral(MarginCallEntryDTO clone) {

		double value = 0.0;

		if ((clone != null) && !Util.isEmpty(String.valueOf(clone.getPreviousTotalMargin()))) {
			value = clone.getPreviousTotalMargin();
		}

		return value;
	}

	/**
	 * Construct the condition for the query pointing to mrgcall_config table.
	 * 
	 * @return String
	 */
	public static String getEmirProductTypesFromDV2MCCTable() {

		Vector<String> prodTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirProductTypes");
		String prodLike = " ";

		if (!Util.isEmpty(prodTypes)) {
			Iterator<String> iter = prodTypes.iterator();
			while (iter.hasNext()) {
				prodLike += "'%" + iter.next().replace(' ', '%') + "%'";
				if (iter.hasNext()) {
					prodLike += " or mrgcall_config.product_list like ";
				}
			}
			prodLike += " ";
		} else {
			prodLike = "'%CollateralExposure%' ";
		}
		return prodLike;
	}

	/**
	 * Construct the condition for the query pointing to PRODUCT_DESC table.
	 * 
	 * @return String
	 */
	public static String getEmirProductTypesFromDV2PDTable() {

		Vector<String> prodTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirProductTypes");
		String prodLike = "";

		if (!Util.isEmpty(prodTypes)) {
			Iterator<String> iter = prodTypes.iterator();
			while (iter.hasNext()) {
				prodLike += "'%" + iter.next().replace(' ', '%') + "%'";
				if (iter.hasNext()) {
					prodLike += " or PRODUCT_DESC.PRODUCT_TYPE like ";
				}
			}
			prodLike += "";
		} else {
			prodLike = "'%CollateralExposure%' ";
		}
		return prodLike;
	}

	public static String getEmirContractTypesFromDV() {

		Vector<String> contractTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirContractTypes");
		String prodIn = "('";

		if (!Util.isEmpty(contractTypes)) {
			Iterator<String> iter = contractTypes.iterator();
			while (iter.hasNext()) {
				prodIn += iter.next();
				if (iter.hasNext()) {
					prodIn += "','";
				}
			}
			prodIn += "') ";
		} else {
			prodIn = "('CSA') ";
		}
		return prodIn;
	}

	// GSM 31/07/15. SBNA Multi-PO filter
	public static String getEmirReportablePOsFromDV(ReportTemplate template) {

		Vector<String> poAgrIds = null;
		final String poAgrStr = CollateralUtilities.filterPoNamesByTemplate(template);

		if (!Util.isEmpty(poAgrStr)) {
			poAgrIds = Util.string2Vector(poAgrStr);
			if (!Util.isEmpty(poAgrIds)) {
				return Util.collectionToSQLString(poAgrIds);
			}
		} else {
			Attributes pos = template.getAttributes();
			String list = (String) pos.getAttributes().get("PO List for export LE");
			Vector<String> poList =  Util.string2Vector(list);
			return Util.collectionToSQLString(poList);
		}

		final Vector<String> poTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirReportablePOs");
		if (!Util.isEmpty(poTypes)) {
			return Util.collectionToSQLString(poTypes);
		}
		return DEFAULT_POS_NAMES;
	}

	public static String getEmirReportablePOsFromDV() {

		final String defaultPOs = DEFAULT_POS_NAMES;// "('BSTE','BDSD','BSHK','BSNY','BECM','WK15')
													// ";
		final Vector<String> poTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirReportablePOs");
		String prodIn = "('";

		if (!Util.isEmpty(poTypes)) {
			Iterator<String> iter = poTypes.iterator();
			while (iter.hasNext()) {
				prodIn += iter.next();
				if (iter.hasNext()) {
					prodIn += "','";
				}
			}
			prodIn += "') ";
			// default
		} else {
			prodIn = defaultPOs;
		}
		return prodIn;
	}

	public static String getEmirSubProductTypesFromDV() {

		Vector<String> prodTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirSubProductTypes");
		String prodIn = "('";

		if (!Util.isEmpty(prodTypes)) {
			Iterator<String> iter = prodTypes.iterator();
			while (iter.hasNext()) {
				prodIn += iter.next();
				if (iter.hasNext()) {
					prodIn += "','";
				}
			}
			prodIn += "') ";
		} else {
			prodIn = "('BOND_OPTION','CAP_AND_FLOOR','CASH_FLOW_MATCHING','COMMODITY_OPTION','CREDIT_DERIVATIVES',"
					+ "'CURRENCY_SWAP','EQUITY_FORWARD','EQUITY_OPTION','EQUITY_SWAP','FORWARD_RATE_AGREEMENT',"
					+ "'FX_DELIVERABLE_FORWARD','FX_NON_DELIVERABLE_FORWARD','FX_OPTION','FX_SWAP_DELIVERABLE',"
					+ "'FX_SWAP_NON_DELIVERABLE','INDEX_OPTION','INTEREST_RATE_OPTION','INTEREST_RATE_SWAP','SWAP_OPTION') ";
		}
		return prodIn;
	}

	public static String getEmirAcceptedStatusFromDV() {

		Vector<String> prodTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirAcceptedStatusTypes");
		String prodIn = "('";

		if (!Util.isEmpty(prodTypes)) {
			Iterator<String> iter = prodTypes.iterator();
			while (iter.hasNext()) {
				prodIn += iter.next();
				if (iter.hasNext()) {
					prodIn += "','";
				}
			}
			prodIn += "') ";
		} else {
			prodIn = "('CANCELED','TERMINATED','MATURED','CHECKED','VERIFIED') ";
		}
		return prodIn;
	}
}
