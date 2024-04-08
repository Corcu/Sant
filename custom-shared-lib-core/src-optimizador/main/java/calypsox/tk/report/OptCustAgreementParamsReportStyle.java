/* Actualizado por David Porras Mart?nez 23-11-11 */
/* Incidencias Optimizador 17/12/14 por GSM */

package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

public class OptCustAgreementParamsReportStyle extends CollateralConfigReportStyle {

	private static final long serialVersionUID = 432240759293827424L;
	// Colums used for general characteristics of counterpaties and contracts
	private static final String MARGINCALLCONTRACT = "Margin Call Contract";
	private static final String OWNER = "Owner";
	private static final String OWNERNAME = "Owner Name";
	private static final String COUNTERPARTY = "Counterparty";
	private static final String LONGNAMECOUNTERPARTY = "Long Name Counterparty";
	private static final String CONTRACTTYPE = "Contract Type";
	private static final String MASTERAGREEDESCRIPTION = "Master Agree Description";
	private static final String CONTRACTCCY = "Contract Ccy";
	private static final String MTACPTY = "MTA Cpty";
	private static final String MTAOWNER = "MTA Owner";
	private static final String THRESHOLDCPTY = "Threshold Cpty";
	private static final String THRESHOLDOWNER = "Threshold Owner";
	private static final String INITIALMARGIN = "Initial Margin";
	private static final String INITIALMARGINCCY = "Initial Margin Ccy";
	private static final String INITIALMARGIN2 = "Initial Margin2";
	private static final String CALCPERIOD = "Calc Period";
	private static final String FIRSTCALCDATE = "First Calc Date";
	private static final String ASSETTYPE = "Asset Type";
	private static final String ASSETTYPE2 = "Asset Type2";
	private static final String ONEWAY = "One Way";
	private static final String HEADCLONE = "Head Clone";
	private static final String CONTRACTLONGNAME = "Contract Long Name";
	private static final String MASTERAGREEMENTSHORTNAME = "Master Agreement Short Name";
	private static final String NOTUSED = "Not Used";
	private static final String MASTERSIGNEDDATE = "Master Signed Date";
	private static final String DELIVERYROUNDING = "Delivery Rounding";
	private static final String DELIVERYROUNDINGCPTY = "Delivery Rounding Cpty";
	private static final String DELIVERYROUNDINGOWNER = "Delivery Rounding Owner";
	private static final String TOLERANCEAMOUNT = "Tolerance Amount";
	private static final String TOLERANCEAMOUNTCCY = "Tolerance Amount Ccy";
	private static final String INDEPENDENTAMOUNTCPTY = "Independent Amount Cpty";
	private static final String INDEPENDENTAMOUNTOWNER = "Independent Amount Owner";
	private static final String CONCILIA = "Conciliation Field";

	// Colums used for Instruments
	private static final String INSTRUMENT = "Instrument";
	private static final String INSTRUMENTDESCRIPTION = "Instrument Description"; // added

	// Columns for Cash & Security Reports.
	private static final String HEADER = "Header";
	private static final String TRANSACTIONID = "Transaction ID";
	private static final String TRANSACTIONTYPE = "Transaction Type";
	private static final String ACTION = "Action";
	private static final String RECONCILIATIONTYPE = "Reconciliation Type";
	private static final String OFFICE = "Office"; // added
	private static final String TRANSACTIONDATE = "Transaction Date";
	private static final String MATURITYDATE = "Maturity Date";
	private static final String ISRECEIVED = "Received";
	private static final String LEADCURRENCY = "Currency";
	private static final String COLLATERALAMOUNT = "Collateral Obligation Amount";
	private static final String AGREEMENTTYPE = "Agreement Type"; // added
	private static final String AGREEMENTID = "Agreement Id"; // added
	// private static final String PRICE = "Price";
	// private static final String BONDID = "Bond ID";
	private static final String ISSUER = "Issuer";
	private static final String BONDMATURITYDATE = "Bond Maturity Date";
	private static final String ISIN = "Isin";
	private static final String COLLBONDNOMINAL = "Collateral Bond Nominal"; // added
	private static final String DIRTYPRICE = "Dirty Price";
	private static final String HAIRCUT = "Haircut";

	// added
	private static final String INDEPENDENTAMOUNT = "Independent Amount";
	private static final String SOURCE = "Source";

	// GSM: 19/02/2014. New Optimization fields
	private static final String HAIRCUT_RULE_NAME = "Haircut Rule Name";
	private static final String IS_REHYPOTHETICABLE = "Is Rehypotheticable";
	private static final String USES_SP_LT = "Uses S&P Long Term";
	private static final String USES_FITCH_LT = "Uses Fitch Long Term";
	private static final String USES_MOODY_LT = "Uses Moody Long Term";

	private static final String CPTY_MTA_TYPE = "Counterparty MTA Type";
	private static final String OWNER_MTA_TYPE = "Owner MTA Type";
	private static final String CPTY_MTA_CCY = "Counterparty MTA Ccy";
	private static final String OWNER_MTA_CCY = "Owner MTA Ccy";
	private static final String CPTY_THRESHOLD_TYPE = "Counterparty Threshold Type";
	private static final String OWNER_THRESHOLD_TYPE = "Owner Threshold Type";
	private static final String ELIGIBLE_AGENCIES = "Eligible Agencies";
	private static final String CPTY_CASH_OFFSET = "Counterparty Cash Offset";
	private static final String OWNER_CASH_OFFSET = "Owner Cash Offset";
	private static final String CPTY_SECURITY_OFFSET = "Counterparty Security Offset";
	private static final String OWNER_SECURITY_OFFSET = "Owner Security Offset";
	private static final String EXCLUDE_FROM_OPTIMIZER = "Exclude from optimizer";
	private static final String DEFAULT_BOOK = "Default Book";
	private static final String EOD_PRICING_ENV = "End of day Pricing Environment";
	private static final String INTRADAY_PRICING_ENV = "Intraday Pricing Environment";
	// GSM, end 19/02/2014
	// GSM: 16/12/2014 - Optimizer incidence, expects MCC info
	private static final String MCC_PO_THRESH_TYPE = "MCC PO Threshold type";
	private static final String MCC_PO_THRESH_AMOUNT = "MCC PO Threshold amount";
	private static final String MCC_PO_THRESH_CCY = "MCC PO Threshold ccy";
	private static final String MCC_PO_MTA_TYPE = "MCC PO MTA type";
	private static final String MCC_PO_MTA_AMOUNT = "MCC PO MTA amount";
	private static final String MCC_PO_MTA_CCY = "MCC PO MTA ccy";
	private static final String MCC_LE_THRESH_TYPE = "MCC LE Threshold type";
	private static final String MCC_LE_THRESH_AMOUNT = "MCC LE Threshold amount";
	private static final String MCC_LE_THRESH_CCY = "MCC LE Threshold ccy";
	private static final String MCC_LE_MTA_TYPE = "MCC LE MTA type";
	private static final String MCC_LE_MTA_AMOUNT = "MCC LE MTA amount";
	private static final String MCC_LE_MTA_CCY = "MCC LE MTA ccy";

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { MARGINCALLCONTRACT, OWNER, OWNERNAME, COUNTERPARTY, CONTRACTTYPE,
			MASTERAGREEDESCRIPTION, CONTRACTCCY, MTACPTY, CPTY_MTA_TYPE, CPTY_MTA_CCY, MTAOWNER, OWNER_MTA_CCY,
			OWNER_MTA_TYPE, THRESHOLDCPTY, CPTY_THRESHOLD_TYPE, THRESHOLDOWNER, OWNER_THRESHOLD_TYPE, INITIALMARGIN,
			INITIALMARGINCCY, CALCPERIOD, FIRSTCALCDATE, ASSETTYPE, ONEWAY, HEADCLONE, CONTRACTLONGNAME,
			MASTERAGREEMENTSHORTNAME, DELIVERYROUNDINGCPTY, DELIVERYROUNDINGOWNER, TOLERANCEAMOUNT, TOLERANCEAMOUNTCCY,
			INDEPENDENTAMOUNTCPTY, INDEPENDENTAMOUNTOWNER, HAIRCUT_RULE_NAME, IS_REHYPOTHETICABLE, USES_SP_LT,
			USES_FITCH_LT, USES_MOODY_LT, ELIGIBLE_AGENCIES, CPTY_CASH_OFFSET, CPTY_SECURITY_OFFSET, OWNER_CASH_OFFSET,
			OWNER_SECURITY_OFFSET, EXCLUDE_FROM_OPTIMIZER, DEFAULT_BOOK, EOD_PRICING_ENV, INTRADAY_PRICING_ENV };

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(MARGINCALLCONTRACT);
		treeList.add(OWNER);
		treeList.add(OWNERNAME);
		treeList.add(COUNTERPARTY);
		treeList.add(LONGNAMECOUNTERPARTY);
		treeList.add(CONTRACTTYPE);
		treeList.add(MASTERAGREEDESCRIPTION);
		treeList.add(CONTRACTCCY);
		treeList.add(MTACPTY);
		treeList.add(MTAOWNER);
		treeList.add(THRESHOLDCPTY);
		treeList.add(THRESHOLDOWNER);
		treeList.add(INITIALMARGIN);
		treeList.add(INITIALMARGINCCY);
		treeList.add(CALCPERIOD);
		treeList.add(FIRSTCALCDATE);
		treeList.add(ASSETTYPE);
		treeList.add(ONEWAY);
		treeList.add(CONTRACTLONGNAME);
		treeList.add(MASTERAGREEMENTSHORTNAME);
		treeList.add(TRANSACTIONID);
		treeList.add(TRANSACTIONTYPE);
		treeList.add(ACTION);
		treeList.add(RECONCILIATIONTYPE);
		treeList.add(TRANSACTIONDATE);
		treeList.add(MATURITYDATE);
		treeList.add(LEADCURRENCY);
		treeList.add(COLLATERALAMOUNT);
		treeList.add(ISRECEIVED);
		treeList.add(ISSUER);
		treeList.add(BONDMATURITYDATE);
		treeList.add(HEADCLONE);
		treeList.add(NOTUSED);
		treeList.add(MASTERSIGNEDDATE);
		treeList.add(DELIVERYROUNDING);
		treeList.add(DELIVERYROUNDINGCPTY);
		treeList.add(DELIVERYROUNDINGOWNER);
		treeList.add(TOLERANCEAMOUNT);
		treeList.add(TOLERANCEAMOUNTCCY);
		treeList.add(ASSETTYPE2);
		treeList.add(INDEPENDENTAMOUNTCPTY);
		treeList.add(INDEPENDENTAMOUNTOWNER);
		treeList.add(INITIALMARGIN2);
		treeList.add(INSTRUMENT);
		treeList.add(HEADER);
		treeList.add(ISIN);
		treeList.add(DIRTYPRICE);
		treeList.add(HAIRCUT);
		treeList.add(INSTRUMENTDESCRIPTION);
		treeList.add(OFFICE);
		treeList.add(AGREEMENTTYPE);
		treeList.add(AGREEMENTID);
		treeList.add(COLLBONDNOMINAL);
		treeList.add(INDEPENDENTAMOUNT);
		treeList.add(SOURCE);
		treeList.add(CONCILIA);

		treeList.add(HAIRCUT_RULE_NAME);
		treeList.add(IS_REHYPOTHETICABLE);
		treeList.add(USES_SP_LT);
		treeList.add(USES_FITCH_LT);
		treeList.add(USES_MOODY_LT);
		treeList.add(CPTY_MTA_TYPE);
		treeList.add(CPTY_MTA_CCY);
		treeList.add(OWNER_MTA_TYPE);
		treeList.add(OWNER_MTA_CCY);
		treeList.add(CPTY_THRESHOLD_TYPE);
		treeList.add(OWNER_THRESHOLD_TYPE);
		treeList.add(ELIGIBLE_AGENCIES);
		treeList.add(CPTY_CASH_OFFSET);
		treeList.add(CPTY_SECURITY_OFFSET);
		treeList.add(OWNER_CASH_OFFSET);
		treeList.add(OWNER_SECURITY_OFFSET);
		treeList.add(EXCLUDE_FROM_OPTIMIZER);
		// GSM: 16/12/2014
		treeList.add(MCC_PO_THRESH_TYPE);
		treeList.add(MCC_PO_THRESH_AMOUNT);
		treeList.add(MCC_PO_THRESH_CCY);
		treeList.add(MCC_PO_MTA_TYPE);
		treeList.add(MCC_PO_MTA_AMOUNT);
		treeList.add(MCC_PO_MTA_CCY);
		treeList.add(MCC_LE_THRESH_TYPE);
		treeList.add(MCC_LE_THRESH_AMOUNT);
		treeList.add(MCC_LE_THRESH_CCY);
		treeList.add(MCC_LE_MTA_TYPE);
		treeList.add(MCC_LE_MTA_AMOUNT);
		treeList.add(MCC_LE_MTA_CCY);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final OptCustAgreementParamsItem item = (OptCustAgreementParamsItem) row
				.getProperty(OptCustAgreementParamsItem.OPT_MARGINCALL_ITEM);

		if (columnName.compareTo(MARGINCALLCONTRACT) == 0) {
			return item.getMarginCallContract();
		} else if (columnName.compareTo(OWNER) == 0) {
			return item.getOwner();
		} else if (columnName.compareTo(OWNERNAME) == 0) {
			return item.getOwnerName();
		} else if (columnName.compareTo(COUNTERPARTY) == 0) {
			return item.getCounterparty();
		} else if (columnName.compareTo(CONTRACTTYPE) == 0) {
			return item.getContractType();
		} else if (columnName.compareTo(MASTERAGREEDESCRIPTION) == 0) {
			return item.getMasterAgreeDescription();
		} else if (columnName.compareTo(CONTRACTCCY) == 0) {
			return item.getContractCcy();
		} else if (columnName.compareTo(MTACPTY) == 0) {
			return item.getMtaCpty();
		} else if (columnName.compareTo(MTAOWNER) == 0) {
			return item.getMtaOwner();
		} else if (columnName.compareTo(THRESHOLDCPTY) == 0) {
			return item.getThresholdCpty();
		} else if (columnName.compareTo(THRESHOLDOWNER) == 0) {
			return item.getThresholdOwner();
		} else if (columnName.compareTo(INITIALMARGIN) == 0) {
			return item.getInitialMargin();
		} else if (columnName.compareTo(INITIALMARGINCCY) == 0) {
			return item.getInitialMarginCcy();
		} else if (columnName.compareTo(CALCPERIOD) == 0) {
			return item.getCalcPeriod();
		} else if (columnName.compareTo(FIRSTCALCDATE) == 0) {
			return item.getFirstCalcDate();
		} else if (columnName.compareTo(ASSETTYPE) == 0) {
			return item.getAssetType();
		} else if (columnName.compareTo(ONEWAY) == 0) {
			return item.getOneWay();
		} else if (columnName.compareTo(CONTRACTLONGNAME) == 0) {
			return item.getContractLongName();
		} else if (columnName.compareTo(MASTERAGREEMENTSHORTNAME) == 0) {
			return item.getMasterAgreementShortName();
		} else if (columnName.compareTo(LONGNAMECOUNTERPARTY) == 0) {
			return item.getContractLongName();
		} else if (columnName.compareTo(TRANSACTIONID) == 0) {
			return item.getTransactionId();
		} else if (columnName.compareTo(TRANSACTIONTYPE) == 0) {
			return item.getTransactionType();
		} else if (columnName.compareTo(ACTION) == 0) {
			return item.getAction();
		} else if (columnName.compareTo(RECONCILIATIONTYPE) == 0) {
			return item.getReconciliationType();
		} else if (columnName.compareTo(TRANSACTIONDATE) == 0) {
			return item.getTransactionDate();
		} else if (columnName.compareTo(MATURITYDATE) == 0) {
			return item.getMaturityDate();
		} else if (columnName.compareTo(LEADCURRENCY) == 0) {
			return item.getContractCcy();
		} else if (columnName.compareTo(COLLATERALAMOUNT) == 0) {
			return item.getCollateralAmount();
		} else if (columnName.compareTo(ISRECEIVED) == 0) {
			return item.getIsReceived();
			// } else if(columnName.compareTo(PRICE) == 0){
			// return item.getPrice();
			// } else if(columnName.compareTo(BONDID) == 0){
			// return item.getBondId();
		} else if (columnName.compareTo(ISSUER) == 0) {
			return item.getIssuer();
		} else if (columnName.compareTo(BONDMATURITYDATE) == 0) {
			return item.getBondMaturityDate();
		} else if (columnName.compareTo(HEADCLONE) == 0) {
			return item.getHeadClone();
		} else if (columnName.compareTo(NOTUSED) == 0) {
			return item.getNotUsed();
		} else if (columnName.compareTo(MASTERSIGNEDDATE) == 0) {
			return item.getMasterSignedDate();
		} else if (columnName.compareTo(DELIVERYROUNDING) == 0) {
			return item.getDeliveryRounding();
		} else if (columnName.compareTo(DELIVERYROUNDINGCPTY) == 0) {
			return item.getDeliveryRoundingCpty();
		} else if (columnName.compareTo(DELIVERYROUNDINGOWNER) == 0) {
			return item.getDeliveryRoundingOwner();
		} else if (columnName.compareTo(TOLERANCEAMOUNT) == 0) {
			return item.getToleranceAmount();
		} else if (columnName.compareTo(TOLERANCEAMOUNTCCY) == 0) {
			return item.getToleranceAmountCcy();
		} else if (columnName.compareTo(ASSETTYPE2) == 0) {
			return item.getAssetType();
		} else if (columnName.compareTo(INDEPENDENTAMOUNTCPTY) == 0) {
			return item.getIndependentAmountCpty();
		} else if (columnName.compareTo(INDEPENDENTAMOUNTOWNER) == 0) {
			return item.getIndependentAmountOwner();
		} else if (columnName.compareTo(INITIALMARGIN2) == 0) {
			return item.getInitialMargin();
		} else if (columnName.compareTo(INSTRUMENT) == 0) {
			return item.getInstrument();
		} else if (columnName.compareTo(HEADER) == 0) {
			return item.getHeader();
		} else if (columnName.compareTo(ISIN) == 0) {
			return item.getIsin();
		} else if (columnName.compareTo(DIRTYPRICE) == 0) {
			return item.getDirtyPrice();
		} else if (columnName.compareTo(HAIRCUT) == 0) {
			return item.getHaircut();
		} else if (columnName.compareTo(INSTRUMENTDESCRIPTION) == 0) { // added
			// , , ,
			// ,
			return item.getInstrument();
		} else if (columnName.compareTo(OFFICE) == 0) { // added
			return item.getOwner();
		} else if (columnName.compareTo(AGREEMENTTYPE) == 0) { // added
			return item.getContractType();
		} else if (columnName.compareTo(AGREEMENTID) == 0) { // added
			return item.getMarginCallContract();
		} else if (columnName.compareTo(COLLBONDNOMINAL) == 0) { // added
			return item.getCollateralAmount();
		} else if (columnName.compareTo(INDEPENDENTAMOUNT) == 0) { // added
			return item.getIndependentAmount();
		} else if (columnName.compareTo(SOURCE) == 0) { // added
			return item.getSource();
		} else if (columnName.compareTo(CONCILIA) == 0) { // added
			return item.getConcilia();
			// GSM: 19/02/2014. New Optimization fields
		} else if (columnName.equals(HAIRCUT_RULE_NAME)) {
			return item.getHaircutRuleName();
		} else if (columnName.equals(IS_REHYPOTHETICABLE)) {
			return item.isRehypothecable();
		} else if (columnName.equals(USES_SP_LT)) {
			return item.isLongTerm_SP();
		} else if (columnName.equals(USES_FITCH_LT)) {
			return item.isLongTerm_Fitch();
		} else if (columnName.equals(USES_MOODY_LT)) {
			return item.isLongTerm_Moody();
		} else if (columnName.equals(CPTY_MTA_TYPE)) {
			return item.getCtpyMtaType();
		} else if (columnName.equals(OWNER_MTA_TYPE)) {
			return item.getOwnerMtaType();
		} else if (columnName.equals(CPTY_MTA_CCY)) {
			return item.getCtpyMtaCcy();
		} else if (columnName.equals(OWNER_MTA_CCY)) {
			return item.getOwnerMtaCcy();
		} else if (columnName.equals(CPTY_THRESHOLD_TYPE)) {
			return item.getCtpyThresholdType();
		} else if (columnName.equals(OWNER_THRESHOLD_TYPE)) {
			return item.getOwnerThresholdType();
		} else if (columnName.equals(ELIGIBLE_AGENCIES)) {
			return item.getEligibleAgencies();

			// GSM: 19/02/2014. New Optimization fields END
		} else if (columnName.equals(CPTY_CASH_OFFSET)) {
			return item.getCtpyCashOffset();
		} else if (columnName.equals(OWNER_CASH_OFFSET)) {
			return item.getOwnerCashOffset();
		} else if (columnName.equals(CPTY_SECURITY_OFFSET)) {
			return item.getCtpySecurityOffset();
		} else if (columnName.equals(OWNER_SECURITY_OFFSET)) {
			return item.getOwnerSecurityOffset();
		} else if (columnName.equals(EXCLUDE_FROM_OPTIMIZER)) {
			return item.isExcludeFromOptimizer();
		} else if (columnName.equals(DEFAULT_BOOK)) {
			return item.getBook();
		} else if (columnName.equals(EOD_PRICING_ENV)) {
			return item.getEODPricingEnvironment();
		} else if (columnName.equals(INTRADAY_PRICING_ENV)) {
			return item.getIntradayPricingEnvironment();
		}
		// GSM: 16/12/2014 - Optimizer incidence, expects MCC info
		final CollateralConfig contract = (CollateralConfig) row
				.getProperty(OptCustAgreementParamsItem.OPT_MARGIN_CALL_CONFIG);
		if (contract == null) {
			return "";
		}

		// PO threshold
		if (columnName.equals(MCC_PO_THRESH_TYPE)) {

			return contract.getPoNewThresholdType();

		} else if (columnName.equals(MCC_PO_THRESH_AMOUNT)) {

			return contract.getPoNewThresholdAmount();

		} else if (columnName.equals(MCC_PO_THRESH_CCY)) {

			return contract.getPoNewThresholdCurrency();
			// LE threshold
		} else if (columnName.equals(MCC_LE_THRESH_TYPE)) {

			return contract.getLeNewThresholdType();

		} else if (columnName.equals(MCC_LE_THRESH_AMOUNT)) {

			return contract.getLeNewThresholdAmount();

		} else if (columnName.equals(MCC_LE_THRESH_CCY)) {

			return contract.getLeNewThresholdCurrency();
			// PO MTA
		} else if (columnName.equals(MCC_PO_MTA_TYPE)) {

			return contract.getPoMTAType();

		} else if (columnName.equals(MCC_PO_MTA_AMOUNT)) {

			return contract.getPoMTAAmount();

		} else if (columnName.equals(MCC_PO_MTA_CCY)) {

			return contract.getPoMTACurrency();
			// LE MTA
		} else if (columnName.equals(MCC_LE_MTA_TYPE)) {

			return contract.getLeMTAType();

		} else if (columnName.equals(MCC_LE_MTA_AMOUNT)) {

			return contract.getLeMTAAmount();

		} else if (columnName.equals(MCC_LE_MTA_CCY)) {

			return contract.getLeMTACurrency();
		}

		// GSM: 26/02/2015. Error in OptCustAgreementParams, no columns in collateralConfig
		return super.getColumnValue(row, columnName, errors);

	}

}