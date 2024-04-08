package calypsox.tk.util.emir;

import java.util.ArrayList;
import java.util.List;

public enum EmirSnapshotColumn {

    /** INDEPENDENT **/

    PARTYREPOBLIGATION1(EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY2COUNTRYOTHERCPTY (EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY1CORPSECTOR(EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY1NATREPORTINGCPTY(EmirSnapshotReportType.INDEPENDENT),
    BENEFICIARYIDPARTY1PREFIX(EmirSnapshotReportType.INDEPENDENT),
    BENEFICIARYIDPARTY1VALUE(EmirSnapshotReportType.INDEPENDENT),
    TRADINGCAPACITY(EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY1COUNTERPARTYSIDE(EmirSnapshotReportType.INDEPENDENT),
    COLLATERALIZED(EmirSnapshotReportType.INDEPENDENT),
    COLLATERALPORTFOLIOCODEPARTY1(EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY1COLLATPORTFOLIO(EmirSnapshotReportType.INDEPENDENT),
   // COLLATERALPORTFOLIOCODEPARTY1(EmirSnapshotReportType.INDEPENDENT),
    POSTTRADETRANSEFFDATE(EmirSnapshotReportType.INDEPENDENT),
    CLEARINGTIMESTAMP(EmirSnapshotReportType.INDEPENDENT),
    CLEARINGDCOPREFIX(EmirSnapshotReportType.INDEPENDENT),
    CLEARINGDCOVALUE(EmirSnapshotReportType.INDEPENDENT),
    ACTIONTYPE1(EmirSnapshotReportType.INDEPENDENT),
    TRADEPARTY1FINENTJURISDICTION(EmirSnapshotReportType.INDEPENDENT),

    /** BOTH **/

    ACTION(EmirSnapshotReportType.BOTH),
    MESSAGETYPE(EmirSnapshotReportType.BOTH),
    SUBMITTEDVALUE(EmirSnapshotReportType.BOTH),
    SUBMITTEDFORPREFIX(EmirSnapshotReportType.BOTH),
    TRADEDATE(EmirSnapshotReportType.BOTH),
    PRODUCTIDPREFIX(EmirSnapshotReportType.BOTH),
    PRODUCTVALUE(EmirSnapshotReportType.BOTH),
    EXECUTIONAGENTPARTY2PREFIX(EmirSnapshotReportType.BOTH),
    EXECUTIONAGENTPARTYVALUE2(EmirSnapshotReportType.BOTH),
    LIFECYCLEEVENT(EmirSnapshotReportType.BOTH),
    ASOFDATETIME(EmirSnapshotReportType.BOTH),
    TRANSTYPE(EmirSnapshotReportType.BOTH),
    TRADEPARTY1BRANCHLOCATION(EmirSnapshotReportType.BOTH),
    TRADEPARTYTRANSACTIONID1(EmirSnapshotReportType.BOTH),
    TRADEPARTYVAL1(EmirSnapshotReportType.BOTH),
    TRADEPARTYPREF2(EmirSnapshotReportType.BOTH),
    TRADEPARTYPREF1(EmirSnapshotReportType.BOTH),
    TRADEPARTYVAL2(EmirSnapshotReportType.BOTH),
    LEIPREFIX(EmirSnapshotReportType.BOTH),
    LEIVALUE(EmirSnapshotReportType.BOTH),
    CONTRACTTYPE(EmirSnapshotReportType.BOTH),
    ASSETCLASS(EmirSnapshotReportType.BOTH),
    PRODUCTCLASSIFICATIONTYPE(EmirSnapshotReportType.BOTH),
    PRODUCTCLASSIFICATION(EmirSnapshotReportType.BOTH),
    PRODUCTIDENTIFICATIONTYPE(EmirSnapshotReportType.BOTH),
    PRODUCTIDENTIFICATION(EmirSnapshotReportType.BOTH),
    UNDERLYINGIDENTIFICATIONTYPE(EmirSnapshotReportType.BOTH),
    UNDERLYINGIDENTIFICATION(EmirSnapshotReportType.BOTH),
    NOTIONALCURRENCY(EmirSnapshotReportType.BOTH),
    UTI(EmirSnapshotReportType.BOTH),
    EXECUTIONVENUEMICCODE(EmirSnapshotReportType.BOTH),
    EXECUTIONVENUEPREFIX(EmirSnapshotReportType.BOTH),
    AMOUNT2(EmirSnapshotReportType.BOTH),
    CURRENCYOFPRICE(EmirSnapshotReportType.BOTH),
    NOTIONALAMOUNT1(EmirSnapshotReportType.BOTH),
    PRICEMULTIPLIER(EmirSnapshotReportType.BOTH),
    PRICENOTPRICETYPE1(EmirSnapshotReportType.BOTH),
    QUANTITY(EmirSnapshotReportType.BOTH),
    UPFRONTPAYMENT(EmirSnapshotReportType.BOTH),
    ORIGINALEXECTIME(EmirSnapshotReportType.BOTH),
    UNADJUSTEDDATE1(EmirSnapshotReportType.BOTH),
    UNADJUSTEDDATE2(EmirSnapshotReportType.BOTH),
    MASTERAGREEMENTTYPE(EmirSnapshotReportType.BOTH),
    MASTERAGREEMENTVERSION(EmirSnapshotReportType.BOTH),
    VERSION(EmirSnapshotReportType.BOTH),
    CONFIRMDATETIME(EmirSnapshotReportType.BOTH),
    CONFIRMTYPE(EmirSnapshotReportType.BOTH),
    CLEARINGIND(EmirSnapshotReportType.BOTH),
    CLEARINGSTATUS(EmirSnapshotReportType.BOTH),
    INTRAGROUP(EmirSnapshotReportType.BOTH),
    CASHSETTLEONLY(EmirSnapshotReportType.BOTH),
    UNDERLYINGASSETIDTYPE(EmirSnapshotReportType.BOTH),
    UNDERLASSET(EmirSnapshotReportType.BOTH),
    CALCULATIONBASIS(EmirSnapshotReportType.BOTH),
    SETTLEMENTDATE(EmirSnapshotReportType.BOTH),
    SENDTO(EmirSnapshotReportType.BOTH),
    SENIORITY(EmirSnapshotReportType.BOTH),
    TRANCHE(EmirSnapshotReportType.BOTH),
    PRIORUTI(EmirSnapshotReportType.BOTH),
    PRIORUTIPREFIX(EmirSnapshotReportType.BOTH),
    EXOTICPRODDAYCOUNTFRACTION(EmirSnapshotReportType.BOTH),
    ENTITYID(EmirSnapshotReportType.BOTH),
    LEVEL(EmirSnapshotReportType.BOTH),
    RESERVEDPARTICIPANTUSE1(EmirSnapshotReportType.BOTH),
    COUNTERPARTYREGION(EmirSnapshotReportType.BOTH),
    EXECUTIONTIME(EmirSnapshotReportType.BOTH),
    EXECUTIONVENUE(EmirSnapshotReportType.BOTH),
    MASTERAGREEMENTDATE(EmirSnapshotReportType.BOTH),
    MASTERCONFIRMATIONTYPE(EmirSnapshotReportType.BOTH),
    TRADEPARTY1NAME(EmirSnapshotReportType.BOTH),
    TRADEPARTY2NAME(EmirSnapshotReportType.BOTH),
    PARTYREGION(EmirSnapshotReportType.BOTH),
    REPORTINGDELEGATIONMODEL(EmirSnapshotReportType.BOTH),
    FIXEDRATE(EmirSnapshotReportType.BOTH),
    FLOATINGRATE(EmirSnapshotReportType.BOTH),
    INIPAYMENTAMOUNT(EmirSnapshotReportType.BOTH),
    PAYFREQMULTLEG1(EmirSnapshotReportType.BOTH),
    PAYFREQPERIODLEG1(EmirSnapshotReportType.BOTH),
    INDEXFACTOR(EmirSnapshotReportType.BOTH),
    SINGINIPAYAMOUNTPAYER(EmirSnapshotReportType.BOTH),
    SINGINIPAYAMOUNTRECEIVER(EmirSnapshotReportType.BOTH),
    INIPAYMENTCURRENCY(EmirSnapshotReportType.BOTH),
    //SINGPAYDATE(EmirSnapshotReportType.BOTH),
    COMPRESSEDTRADE(EmirSnapshotReportType.BOTH),
    MATRIXTYPE(EmirSnapshotReportType.BOTH),
    BUYERLEYPREFIX(EmirSnapshotReportType.BOTH),
    BUYERLEYVALUE(EmirSnapshotReportType.BOTH),
    INSURER(EmirSnapshotReportType.BOTH),
    CALCULATIONAGENTPARTYREFERENCE(EmirSnapshotReportType.BOTH),
    REPORTINGJURISDICTION(EmirSnapshotReportType.BOTH),
    /** DELEGATE **/

    ACTIONTYPE2(EmirSnapshotReportType.DELEGATE),
    BENEFICIARYIDPARTY2PREFIX(EmirSnapshotReportType.DELEGATE),
    BENEFICIARYIDPARTY2VALUE(EmirSnapshotReportType.DELEGATE),
    CLEARINGTHRESHOLDPARTY2(EmirSnapshotReportType.DELEGATE),
    COLLATERALIZEDPARTY2(EmirSnapshotReportType.DELEGATE),
    COLLATERALPORTFOLIOCODEPARTY2(EmirSnapshotReportType.DELEGATE),
    DIRLINKTOCOMMACTTREAFINPARTY2(EmirSnapshotReportType.DELEGATE),
    PARTYREPOBLIGATION2(EmirSnapshotReportType.DELEGATE),
    TRADEPAR2NONFINENTJURISDICTION(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY1COUNTRYOTHERCPTY(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2COLLATPORTFOLIO(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2CORPSECTOR(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2COUNTERPARTYSIDE(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2FINENTJURISDICTION(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2NATREPORTINGCPTY(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2THIPARTYVIEWID(EmirSnapshotReportType.DELEGATE),
    TRADEPARTY2THIPARTYVIEWIDTYPE(EmirSnapshotReportType.DELEGATE),
    TRADINGCAPACITYPARTY2(EmirSnapshotReportType.DELEGATE),
    MANDATORYDELEGREPORT2(EmirSnapshotReportType.DELEGATE),
    MASTERCONFIRMATIONDATE(EmirSnapshotReportType.BOTH),


 ;


    private String typeName;

    EmirSnapshotColumn(EmirSnapshotReportType type) {
        this.typeName = type.name();
    }

    private String getTypeName() {
        return typeName;
    }

    public static List<EmirSnapshotColumn> getIndependentColumns() {
        return getColumns(EmirSnapshotReportType.INDEPENDENT.name());
    }

    public static List<EmirSnapshotColumn> getDelegateColumns() {
        return getColumns(EmirSnapshotReportType.DELEGATE.name());
    }

    public static List<EmirSnapshotColumn> getBothColumns() {
        return getColumns(EmirSnapshotReportType.BOTH.name());
    }

    private static boolean isBoth(final String type) {
        return (EmirSnapshotReportType.BOTH.name().equals(type));
    }

    /**
     * Get columns method for EmirSnapshotColumn.
     *
     * @return columns
     */
    private static List<EmirSnapshotColumn> getColumns(final String typeName) {
        final EmirSnapshotColumn[] columns = EmirSnapshotColumn.values();
        final List<EmirSnapshotColumn> result = new ArrayList<EmirSnapshotColumn>();
        for (EmirSnapshotColumn column : columns) {
            if (typeName.equals(column.getTypeName())
                    || isBoth(column.getTypeName())
                    || typeName.equals(EmirSnapshotReportType.BOTH.name())) {
                result.add(column);
            }
        }
        return result;
    }
}
