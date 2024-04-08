/* Actualizado por David Porras Mart?nez 23-11-11 */

package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.MarginCallReportStyle;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class KGR_Collateral_MarginCallReportStyle extends MarginCallReportStyle {
    // Colums used for general characteristics of counterpaties and contracts
    private static final String MARGINCALLCONTRACT = "Margin Call Contract";
    private static final String OWNER = "Owner";
    private static final String OWNER_SENTINEL = "OwnerSentinel";
    private static final String OWNERNAME = "Owner Name";
    private static final String COUNTERPARTY = "Counterparty";
    private static final String COUNTERPARTY_SENTINEL = "CounterpartySentinel";
    private static final String LONGNAMECOUNTERPARTY = "Long Name Counterparty";
    private static final String CONTRACTTYPE = "Contract Type";
    private static final String OUTSTANDINGDISPUTE = "Outstanding Disputes";
    private static final String COLLATERALMARGINTYPE = "Collateral Margin Type";
    private static final String MARKETVALUE = "Market Value";
    private static final String UNDERLYINGDURATION = "Underlying Duration";
    private static final String CCP = "CCP";
    private static final String MASTERAGREEDESCRIPTION = "Master Agree Description";
    private static final String CONTRACTCCY = "Contract Ccy";
    private static final String MTACPTY = "MTA Cpty";
    private static final String MTAOWNER = "MTA Owner";
    private static final String THRESHOLDCPTY = "Threshold Cpty";
    private static final String THRESHOLDOWNER = "Threshold Owner";
    private static final String INITIALMARGIN = "Initial Margin";
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
    private static final String COLLEQUITYNOMINAL = "Collateral Equity Nominal";
    private static final String DIRTYPRICE = "Dirty Price";
    private static final String DIRTYPRICEWITHHAIRCUT = "Dirty Price with Haircut";
    private static final String HAIRCUT = "Haircut";
    private static final String SEGREGATEDCOLLATERAL = "SegregatedCollateral";

    // added
    private static final String INDEPENDENTAMOUNT = "Independent Amount";
    private static final String SOURCE = "Source";
    private static final String STATUS = "Conctract Status";
    private static final String ELIGIBLE_CURRENCY = "Eligible Currencies";
    private static final String SENTINEL_BLOCK_STATUS = "Is blocked for Sentinel";

    // Default columns.
    public static final String[] DEFAULTS_COLUMNS = {MARGINCALLCONTRACT, OWNER, OWNERNAME, COUNTERPARTY,
            LONGNAMECOUNTERPARTY, CONTRACTTYPE, OUTSTANDINGDISPUTE, COLLATERALMARGINTYPE, MARKETVALUE,
            UNDERLYINGDURATION, CCP, MASTERAGREEDESCRIPTION, CONTRACTCCY, MTACPTY, MTAOWNER, THRESHOLDCPTY,
            THRESHOLDOWNER, INITIALMARGIN, CALCPERIOD, FIRSTCALCDATE, ASSETTYPE, ONEWAY, CONTRACTLONGNAME,
            MASTERAGREEMENTSHORTNAME, TRANSACTIONID, TRANSACTIONTYPE, ACTION, RECONCILIATIONTYPE, TRANSACTIONDATE,
            MATURITYDATE, LEADCURRENCY, COLLATERALAMOUNT, ISRECEIVED, ISSUER, BONDMATURITYDATE, HEADCLONE, NOTUSED,
            MASTERSIGNEDDATE, DELIVERYROUNDING, DELIVERYROUNDINGCPTY, DELIVERYROUNDINGOWNER, TOLERANCEAMOUNT,
            ASSETTYPE2, INDEPENDENTAMOUNTCPTY, INDEPENDENTAMOUNTOWNER, INITIALMARGIN2, INSTRUMENT, HEADER, ISIN,
            DIRTYPRICE, HAIRCUT, INSTRUMENTDESCRIPTION, OFFICE, AGREEMENTTYPE, AGREEMENTID, COLLBONDNOMINAL,
            INDEPENDENTAMOUNT, SOURCE, CONCILIA, STATUS, ELIGIBLE_CURRENCY};
    private static final long serialVersionUID = 8426999599834314465L;

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(MARGINCALLCONTRACT);
        treeList.add(OWNER);
        treeList.add(OWNERNAME);
        treeList.add(COUNTERPARTY);
        treeList.add(LONGNAMECOUNTERPARTY);
        treeList.add(CONTRACTTYPE);
        treeList.add(OUTSTANDINGDISPUTE);
        treeList.add(COLLATERALMARGINTYPE);
        treeList.add(MARKETVALUE);
        treeList.add(UNDERLYINGDURATION);
        treeList.add(CCP);
        treeList.add(MASTERAGREEDESCRIPTION);
        treeList.add(CONTRACTCCY);
        treeList.add(MTACPTY);
        treeList.add(MTAOWNER);
        treeList.add(THRESHOLDCPTY);
        treeList.add(THRESHOLDOWNER);
        treeList.add(INITIALMARGIN);
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
        treeList.add(ASSETTYPE2);
        treeList.add(INDEPENDENTAMOUNTCPTY);
        treeList.add(INDEPENDENTAMOUNTOWNER);
        treeList.add(INITIALMARGIN2);
        treeList.add(INSTRUMENT);
        treeList.add(HEADER);
        treeList.add(ISIN);
        treeList.add(DIRTYPRICE);
        treeList.add(DIRTYPRICEWITHHAIRCUT);
        treeList.add(HAIRCUT);
        treeList.add(INSTRUMENTDESCRIPTION);
        treeList.add(OFFICE);
        treeList.add(AGREEMENTTYPE);
        treeList.add(AGREEMENTID);
        treeList.add(COLLBONDNOMINAL);
        treeList.add(INDEPENDENTAMOUNT);
        treeList.add(SOURCE);
        treeList.add(CONCILIA);
        treeList.add(SEGREGATEDCOLLATERAL);
        treeList.add(COLLEQUITYNOMINAL);
        treeList.add(STATUS);
        treeList.add(ELIGIBLE_CURRENCY);
        treeList.add(SENTINEL_BLOCK_STATUS);
        treeList.add(OWNER_SENTINEL);
        treeList.add(COUNTERPARTY_SENTINEL);

        return treeList;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
                                final Vector errors) throws InvalidParameterException {
        final KGR_Collateral_MarginCallItem item = row
                .getProperty(KGR_Collateral_MarginCallItem.KGRCOL_MARGINCALL_ITEM);

        if (columnName.compareTo(MARGINCALLCONTRACT) == 0) {
            return item.getMarginCallContract();
        } else if (columnName.compareTo(OWNER) == 0 || columnName.compareTo(OWNER_SENTINEL) == 0) {
            return item.getOwner();
        } else if (columnName.compareTo(OWNERNAME) == 0) {
            return item.getOwnerName();
        } else if (columnName.compareTo(COUNTERPARTY) == 0 || columnName.compareTo(COUNTERPARTY_SENTINEL) == 0) {
            return item.getCounterparty();
        } else if (columnName.compareTo(CONTRACTTYPE) == 0) {
            return item.getContractType();
        } else if (columnName.compareTo(OUTSTANDINGDISPUTE) == 0) {
            return item.getOutstandingDisputes();
        } else if (columnName.compareTo(COLLATERALMARGINTYPE) == 0) {
            return item.getCollateralMarginType();
        } else if (columnName.compareTo(MARKETVALUE) == 0) {
            return item.getMarketValue();
        } else if (columnName.compareTo(UNDERLYINGDURATION) == 0) {
            return item.getUnderlyingDuration();
        } else if (columnName.compareTo(CCP) == 0) {
            return item.getCcp();
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
        } else if (columnName.compareTo(DIRTYPRICEWITHHAIRCUT) == 0) {
            return item.getDirtyPriceWithHaircut();
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
        } else if (columnName.compareTo(SEGREGATEDCOLLATERAL) == 0) {
            return item.getSegregatedCollateral();
        } else if (columnName.compareTo(COLLEQUITYNOMINAL) == 0) {
            return item.getCollateralAmount();
        } else if (columnName.compareTo(STATUS) == 0) {
            return item.getStatus();
        } else if (columnName.compareTo(ELIGIBLE_CURRENCY) == 0) {
            return item.getEligible_currency();
        }else if (columnName.compareTo(SENTINEL_BLOCK_STATUS) == 0) {
            return item.getIsSentinelBlocked();
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }
}