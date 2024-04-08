package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotColumn;
import calypsox.tk.util.emir.EmirSnapshotReportType;

import java.util.HashMap;
import java.util.Map;

public class EmirFieldBuilderFactory {
  private Map<String, EmirFieldBuilder> fieldBuilderMap = null;

  private static EmirFieldBuilderFactory instance = null;

  private EmirFieldBuilderFactory() {
    fillMap();
  }

  public static synchronized EmirFieldBuilderFactory getInstance() {
    if (instance == null) {
      instance = new EmirFieldBuilderFactory();
    }
    return instance;
  }

  public EmirFieldBuilder getFieldBuilder(EmirSnapshotColumn column, EmirSnapshotReportType reportType) {
    String key = getKey(column, reportType);
    EmirFieldBuilder builder = fieldBuilderMap.get(key);
    if (builder == null) {
      key = getKey(column, EmirSnapshotReportType.BOTH);
      builder = fieldBuilderMap.get(key);
    }

    return builder;
  }

  private String getKey(EmirSnapshotColumn column, EmirSnapshotReportType reportType) {
    return column.name() + "-" + reportType.name();
  }

    private void fillMap() {
        fieldBuilderMap = new HashMap<String, EmirFieldBuilder>();
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ACTION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderAction());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ACTIONTYPE1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderActionType());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ACTIONTYPE2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderActionType());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.AMOUNT2, EmirSnapshotReportType.BOTH), new EmirFieldBuilderAmount2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ASOFDATETIME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderAsOfDateTime());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ASSETCLASS, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderAssetClass());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BENEFICIARYIDPARTY1PREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBENEFICIARYIDPARTY1PREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BENEFICIARYIDPARTY1VALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBENEFICIARYIDPARTY1VALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BENEFICIARYIDPARTY2PREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBENEFICIARYIDPARTY2PREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BENEFICIARYIDPARTY2VALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBENEFICIARYIDPARTY2VALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGIND, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGIND());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGSTATUS, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGSTATUS());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CASHSETTLEONLY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCASHSETTLEONLY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.VERSION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderVERSION());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGTHRESHOLDPARTY2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGTHRESHOLDPARTY2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGDCOPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGDCOPREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BUYERLEYPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBUYERLEYPREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.BUYERLEYVALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderBUYERLEYVALUE());


        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGDCOVALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGDCOVALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CLEARINGTIMESTAMP, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCLEARINGTIMESTAMP());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COLLATERALIZED, EmirSnapshotReportType.INDEPENDENT),
                new EmirFieldBuilderCOLLATERALIZED());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COLLATERALPORTFOLIOCODEPARTY1, EmirSnapshotReportType.INDEPENDENT),
                new EmirFieldBuilderCOLLATERALPORTFOLIOCODEPARTY1());

        //fieldBuilderMap.put(getKey(EmirSnapshotColumn.COLLATERALPORTFOLIOCODEPARTY1, EmirSnapshotReportType.BOTH),
        //       new EmirFieldBuilderCOLLATERALPORTFOLIOCODEPARTY1());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COLLATERALPORTFOLIOCODEPARTY2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCOLLATERALPORTFOLIOCODEPARTY2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CONFIRMDATETIME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCONFIRMDATETIME());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CONFIRMTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCONFIRMTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CONTRACTTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCONTRACTTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COUNTERPARTYREGION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCOUNTERPARTYREGION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CURRENCYOFPRICE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCURRENCYOFPRICE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.DIRLINKTOCOMMACTTREAFINPARTY2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderDIRLINKTOCOMMACTTREAFINPARTY2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONAGENTPARTY2PREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONAGENTPARTY2PREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONAGENTPARTYVALUE2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONAGENTPARTYVALUE2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONTIME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONTIME());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONVENUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONVENUE());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INIPAYMENTAMOUNT, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderINIPAYMENTAMOUNT());


        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONVENUEMICCODE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONVENUEMICCODE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXECUTIONVENUEPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderEXECUTIONVENUEPREFIX());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INTRAGROUP, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderINTRAGROUP());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.LEIPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderLEIPREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.LEIVALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderLEIVALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.LEVEL, EmirSnapshotReportType.BOTH), new EmirFieldBuilderLEVEL());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.LIFECYCLEEVENT, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderLIFECYCLEEVENT());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MASTERAGREEMENTDATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMASTERAGREEMENTDATE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MASTERAGREEMENTTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMASTERAGREEMENTTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MASTERAGREEMENTVERSION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMASTERAGREEMENTVERSION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MASTERCONFIRMATIONDATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMASTERCONFIRMATIONDATE());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MATRIXTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMATRIXTYPE());


        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MASTERCONFIRMATIONTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMASTERCONFIRMATIONTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MESSAGETYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderMESSAGETYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ORIGINALEXECTIME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderORIGINALEXECTIME());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PARTYREGION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPARTYREGION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PARTYREPOBLIGATION1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPARTYREPOBLIGATION1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PARTYREPOBLIGATION2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPARTYREPOBLIGATION2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.POSTTRADETRANSEFFDATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPOSTTRADETRANSEFFDATE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRICEMULTIPLIER, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRICEMULTIPLIER());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRICENOTPRICETYPE1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRICENOTPRICETYPE1());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTCLASSIFICATION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTCLASSIFICATION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTCLASSIFICATIONTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTCLASSIFICATIONTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTIDENTIFICATION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTIDENTIFICATION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTIDENTIFICATIONTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTIDENTIFICATIONTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTIDPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTIDPREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRODUCTVALUE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPRODUCTVALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.QUANTITY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderQUANTITY());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.FIXEDRATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderFIXEDRATE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INSURER, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderINSURER());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.REPORTINGJURISDICTION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderREPORTINGJURISDICTION());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.FLOATINGRATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderFLOATINGRATE());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PAYFREQMULTLEG1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPAYFREQMULTLEG1());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PAYFREQPERIODLEG1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderPAYFREQPERIODLEG1());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SINGINIPAYAMOUNTPAYER, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderSINGINIPAYAMOUNTPAYER());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SINGINIPAYAMOUNTRECEIVER, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderSINGINIPAYAMOUNTRECEIVER());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INIPAYMENTCURRENCY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderINIPAYMENTCURRENCY());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COMPRESSEDTRADE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCOMPRESSEDTRADE());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INDEXFACTOR, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderINDEXFACTOR());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.REPORTINGDELEGATIONMODEL, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderREPORTINGDELEGATIONMODEL());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CALCULATIONAGENTPARTYREFERENCE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderCALCULATIONAGENTPARTYREFERENCE());



        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SUBMITTEDVALUE, EmirSnapshotReportType.INDEPENDENT),
                new EmirFieldBuilderSUBMITTEDVALUE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SUBMITTEDVALUE, EmirSnapshotReportType.DELEGATE),
                new EmirFieldBuilderSUBMITTEDVALUEDelegate());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.MANDATORYDELEGREPORT2, EmirSnapshotReportType.DELEGATE),
                new EmirFieldBuilderMANDATORYDELEGREPORT2());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SUBMITTEDFORPREFIX, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderSUBMITTEDFORPREFIX());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEDATE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEDATE());


        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPAR2NONFINENTJURISDICTION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPAR2NONFINENTJURISDICTION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1COLLATPORTFOLIO, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1COLLATPORTFOLIO());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1CORPSECTOR, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1CORPSECTOR());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1COUNTERPARTYSIDE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1COUNTERPARTYSIDE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1COUNTRYOTHERCPTY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1COUNTRYOTHERCPTY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1FINENTJURISDICTION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1FINENTJURISDICTION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1NAME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1NAME());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1NATREPORTINGCPTY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY1NATREPORTINGCPTY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2COLLATPORTFOLIO, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2COLLATPORTFOLIO());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2CORPSECTOR, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2CORPSECTOR());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2COUNTERPARTYSIDE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2COUNTERPARTYSIDE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2COUNTRYOTHERCPTY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2COUNTRYOTHERCPTY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2FINENTJURISDICTION, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2FINENTJURISDICTION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2NAME, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2NAME());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2NATREPORTINGCPTY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTY2NATREPORTINGCPTY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTYPREF2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTYPREF2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTYPREF1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTYPREF1());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTYTRANSACTIONID1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTYTRANSACTIONID1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTYVAL1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTYVAL1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTYVAL2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADEPARTYVAL2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADINGCAPACITY, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADINGCAPACITY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADINGCAPACITYPARTY2, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRADINGCAPACITYPARTY2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRANSTYPE, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderTRANSTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNADJUSTEDDATE1, EmirSnapshotReportType.BOTH),
                new EmirFieldBuilderUNADJUSTEDDATE1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UTI, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUTI());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.ENTITYID, EmirSnapshotReportType.BOTH), new EmirFieldBuilderENTITYID());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.EXOTICPRODDAYCOUNTFRACTION, EmirSnapshotReportType.BOTH), new EmirFieldBuilderEXOTICPRODDAYCOUNTFRACTION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.NOTIONALAMOUNT1, EmirSnapshotReportType.BOTH), new EmirFieldBuilderNOTIONALAMOUNT1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.NOTIONALCURRENCY, EmirSnapshotReportType.BOTH), new EmirFieldBuilderNOTIONALCURRENCY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.RESERVEDPARTICIPANTUSE1, EmirSnapshotReportType.BOTH), new EmirFieldBuilderRESERVEDPARTICIPANTUSE1());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SENDTO, EmirSnapshotReportType.BOTH), new EmirFieldBuilderSENDTO());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SENIORITY, EmirSnapshotReportType.BOTH), new EmirFieldBuilderSENIORITY());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRANCHE, EmirSnapshotReportType.BOTH), new EmirFieldBuilderTRANCHE());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRIORUTI, EmirSnapshotReportType.BOTH), new EmirFieldBuilderPRIORUTI());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.PRIORUTIPREFIX, EmirSnapshotReportType.BOTH), new EmirFieldBuilderPRIORUTIPREFIX());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNDERLYINGASSETIDTYPE, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUNDERLYINGASSETIDTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNDERLASSET, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUNDERLASSET());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.CALCULATIONBASIS, EmirSnapshotReportType.BOTH), new EmirFieldBuilderCALCULATIONBASIS());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.SETTLEMENTDATE, EmirSnapshotReportType.BOTH), new EmirFieldBuilderSETTLEMENTDATE());


        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY1BRANCHLOCATION, EmirSnapshotReportType.BOTH), new EmirFieldBuilderTRADEPARTY1BRANCHLOCATION());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNADJUSTEDDATE2, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUNADJUSTEDDATE2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNDERLYINGIDENTIFICATION, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUNDERLYINGIDENTIFICATION());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UNDERLYINGIDENTIFICATIONTYPE, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUNDERLYINGIDENTIFICATIONTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.UPFRONTPAYMENT, EmirSnapshotReportType.BOTH), new EmirFieldBuilderUPFRONTPAYMENT());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.INIPAYMENTCURRENCY, EmirSnapshotReportType.BOTH), new EmirFieldBuilderINIPAYMENTCURRENCY());

        fieldBuilderMap.put(getKey(EmirSnapshotColumn.COLLATERALIZEDPARTY2, EmirSnapshotReportType.DELEGATE), new EmirFieldBuilderCOLLATERALIZEDPARTY2());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2THIPARTYVIEWIDTYPE, EmirSnapshotReportType.DELEGATE), new EmirFieldBuilderTRADEPARTY2THIPARTYVIEWIDTYPE());
        fieldBuilderMap.put(getKey(EmirSnapshotColumn.TRADEPARTY2THIPARTYVIEWID, EmirSnapshotReportType.DELEGATE), new EmirFieldBuilderTRADEPARTY2THIPARTYVIEWID());

    }
}
