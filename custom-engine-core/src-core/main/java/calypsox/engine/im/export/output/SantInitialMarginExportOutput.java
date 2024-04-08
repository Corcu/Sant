/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.im.export.output;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

public abstract class SantInitialMarginExportOutput {

    protected static final String NPV = "NPV";
    protected static final String NPV_LEG1 = "NPV_LEG1";
    protected static final String NPV_LEG2 = "NPV_LEG2";
    private static final String UPI_REFERENCE_KW = "UPI_REFERENCE";
    private static final String BO_SYSTEM_KW = "BO_SYSTEM";
    private static final String FO_SYSTEM_KW = "FO_SYSTEM";
    private static final String IM_TERMINATION_CCY = "IM_TERMINATION_CCY";
    private static final String IM_REPORTING_CCY = "IM_REPORTING_CCY";
    private static final String END_LINE = "\n";
    private final static String SEPARATOR = "|";
    private final static String IM_CALC_METHOD = "IM_CALCULATION_METHOD";
    private final static String IM_CALCULATION_DEFAULT = "IM_CALCULATION_METHOD_DEFAULT";
    private final static String UPI = "UPI_REFERENCE";
    private final static String BLANK = "";

    // private static final String IM_QEF_EXPORT_FILTER = "IM_QEF_EXPORT_FILTER";
    // private static final String CALC_METHOD_FIELD = "ACCRUAL_INCLUDE_CALC";
    // private static final String NPV_BASE = "NPV_BASE";

    protected static SimpleDateFormat sdf_ddMMyyyy = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    CollateralConfig contractCSA;
    CollateralConfig contractCSDPO;
    CollateralConfig contractCSDCPTY;

    String pricingEnv;

    MarginCallDetailEntryDTO mcDetailEntry;
    Trade trade;
    PLMark plMark;

    private boolean filterApproved;
    private String processDate;
    private String valueDate;
    private String frontId;
    private String contractName;
    private String contractCcy;
    private String calculationMethod;
    private String po;
    private String cpty;
    private String maturityDate;
    private String maturityDate2;
    private String noticional;
    private String noticionalCcy;
    private String noticional2;
    private String noticional2Ccy;
    private String mtm;
    private String mtmCcy;
    private String mtm2;
    private String mtm2Ccy;
    private String foSystemKw;
    private String boSystemKw;
    private String boReferenceKw;
    private String upiReference;
    private String productType;
    private String terminationCcy;
    private String csdIMPOReportingCcy;
    private String csdIMCPTYContractCcy;
    private String csdIMCPTYTerminationCcy;
    private String csdIMCPTYReportingCcy;


    private SantInitialMarginExportOutput() {
    }

    /**
     * @param sdf            StaticDataFilter IM_QEF_EXPORT_FILTER
     * @param plMark         Extracted PLMark
     * @param trade          Extracted Trade
     * @param entry          MarginCallDetailEntryDTO
     * @param contractCSA    CollateralConfig CSA
     * @param contractCSD    CollateralConfig CSD
     * @param pricingEnvName Princing Enviroment
     * @author epalaobe
     * <p>
     * New constructor to not extract the trade and the static data filter all times.
     */
    protected SantInitialMarginExportOutput(StaticDataFilter sdf, Trade trade, MarginCallDetailEntryDTO entry, CollateralConfig contractCSA, CollateralConfig contractCSDPO, CollateralConfig contractCSDCPTY,
                                            String pricingEnvName) {
        this.filterApproved = true;
        this.contractCSA = contractCSA;
        this.contractCSDPO = contractCSDPO;
        this.contractCSDCPTY = contractCSDCPTY;
        this.mcDetailEntry = entry;
        this.pricingEnv = pricingEnvName;

        if (this.mcDetailEntry != null) {
            this.trade = trade;

            if (sdf != null && !sdf.accept(this.trade)) {
                this.filterApproved = false;
            }
        }

        JDate processDate = null;
        if (mcDetailEntry.getProcessDatetime() != null) {
            processDate = JDate.valueOf(mcDetailEntry.getProcessDatetime());
        } else {
            processDate = JDate.getNow();
        }

        this.plMark = null;

        try {
            this.plMark = CollateralUtilities.retrievePLMark(trade.getLongId(), DSConnection.getDefault(), pricingEnv, processDate.addBusinessDays(-1, Util.string2Vector("SYSTEM")));
        } catch (RemoteException e) {
            Log.error(SantInitialMarginExportOutput.class, "PLMark not found");
        }
    }

    public Product getProduct() {
        return trade.getProduct();
    }

    public final void fillInfo() {
        // this method won't be called if filterApproved = false

        this.processDate = getLogicProcessDate();
        this.valueDate = getLogicValueDate();
        this.contractName = getLogicContractName();
        this.contractCcy = getLogicContractCcy();
        this.calculationMethod = getLogicCalculationMethod();
        this.po = getLogicPo();
        this.cpty = getLogicCpty();
        this.maturityDate2 = getLogicMaturityDate2();
        this.foSystemKw = getLogicFoSystemKw();
        this.boSystemKw = getLogicBoSystemKw();
        this.upiReference = getLogicUpiReference();
        this.terminationCcy = getLogicTerminationCcy();
        this.csdIMPOReportingCcy = getLogicCsdIMPOReportingCcy();
        this.csdIMCPTYContractCcy = getLogicCsdIMCPTYContractCcy();
        this.csdIMCPTYTerminationCcy = getLogicCsdIMCPTYTerminationCcy();
        this.csdIMCPTYReportingCcy = getLogicCsdIMCPTYReportingCcy();

    }

    protected String getLogicProcessDate() {
        String result = "";

        if (this.mcDetailEntry != null) {
            JDatetime processDate = this.mcDetailEntry.getProcessDatetime();

            if (processDate != null) {
                result = sdf_ddMMyyyy.format(processDate.getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault()));
            }
        }
        return result;
    }

    protected String getLogicValueDate() {
        String result = "";

        if (this.mcDetailEntry != null) {
            JDatetime valueDate = this.mcDetailEntry.getValueDatetime();

            if (valueDate != null) {
                result = sdf_ddMMyyyy.format(valueDate.getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault()));
            }
        }
        return result;
    }

    protected String getLogicMaturityDate2() {
        String result = "";
        // empty
        return result;
    }

    protected String getLogicCpty() {
        String result = "";

        if (this.trade != null) {
            LegalEntity cpty = this.trade.getCounterParty();

            if (cpty != null) {
                result = cpty.getCode();
            }
        }
        return result;
    }

    protected String getLogicContractName() {
        String result = "";

        if (this.contractCSDPO != null) {
            result = this.contractCSDPO.getName();

        }
        return result;
    }


    private String getLogicFoSystemKw() {
        String result = "";

        if (this.trade != null) {
            result = this.trade.getKeywordValue(FO_SYSTEM_KW);
        }
        return result;
    }

    private String getLogicBoSystemKw() {
        String result = "";

        if (this.trade != null) {
            result = this.trade.getKeywordValue(BO_SYSTEM_KW);
        }
        return result;
    }

    protected String getLogicCalculationMethod() {
        HashMap<String, String> IMCal = new HashMap<>();

        if (this.contractCSA != null && this.trade != null) {
            IMCal = getIMCalc(this.contractCSA.getAdditionalFields());
            String keyordUPI = this.trade.getKeywordValue(UPI);

            for (Entry<String, String> entry : IMCal.entrySet()) {
                if (!Util.isEmpty(entry.getValue()) && !entry.getValue().equals("null")) {
                    String[] values = String.valueOf(entry.getValue()).split(";");
                    for (String te : values) {

                        //Metodo anterior. Para recuperar el IM_CALCULATION_METHOD hasta que el usuario cambie todos. Borrar cuando haya terminado
                        if (keyordUPI != null && (keyordUPI.trim().startsWith(te.trim()))) {
                            String[] result1 = entry.getKey().toString().split("_");
                            return result1[result1.length - 1];
                        }

                        //Nuevo metodo de recuperacion de IM_CALCULATION_METHOD. Ahora directamente lo busca del SDF que tenga definido en el AF
                        if (keyordUPI != null) {
                            StaticDataFilter sdf = null;
                            try {
                                sdf = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(te);
                                if (sdf != null) {
                                    if (sdf.accept(this.trade)) {
                                        String[] result1 = entry.getKey().toString().split("_");
                                        return result1[result1.length - 1];
                                    }
                                }
                            } catch (CalypsoServiceException e) {
                                Log.error(this.getClass(), "the SDF" + te + "does not exist");
                            }
                        }
                    }
                }
            }
        }
        return IMCal.get(IM_CALCULATION_DEFAULT);
    }

    protected String getLogicContractCcy() {
        String result = "";

        if (this.contractCSDPO != null) {
            result = this.contractCSDPO.getCurrency();
        }
        return result;
    }

    private String getLogicUpiReference() {
        String result = "";

        if (this.trade != null) {
            if("BOND_FORWARD".equalsIgnoreCase(trade.getProductSubType())){
                result="InterestRate:Forward:Debt";
            }else {
                result = trade.getKeywordValue(UPI_REFERENCE_KW);
            }
            if (Util.isEmpty(result)) {
                result = "NotAvailable";
                Log.warn(this, "The trade " + trade.getLongId() + " don't have the keyword UPI_REFERENCE.");
            }
        }

        return result;
    }

    private String getLogicTerminationCcy() {
        String result = "";

        if (this.contractCSDPO != null) {
            result = this.contractCSDPO.getAdditionalField(IM_TERMINATION_CCY);
        }
        return result;
    }

    protected String getLogicPo() {
        String result = "";

        if (this.trade != null) {
            Book book = trade.getBook();

            if (book != null) {
                LegalEntity po = book.getLegalEntity();

                if (po != null) {
                    result = po.getCode();
                }
            }
        }
        return result;
    }

    public String formatAmount(String amount) {
        if (!Util.isEmpty(amount)) {
            return amount.replace('.', '*').replace(',', '.').replace('*', ',');
        }
        return amount;

    }

    private String getLogicCsdIMPOReportingCcy() {

        String result = "";
        if (this.contractCSDPO != null) {
            if (!Util.isEmpty(this.contractCSDPO.getAdditionalField(IM_REPORTING_CCY))) {
                result = this.contractCSDPO.getAdditionalField(IM_REPORTING_CCY);
            }
        }
        return result;
    }

    private String getLogicCsdIMCPTYContractCcy() {

        String result = "";
        if (this.contractCSDCPTY != null) {
            result = this.contractCSDCPTY.getCurrency();
        }
        return result;
    }

    private String getLogicCsdIMCPTYTerminationCcy() {

        String result = "";
        if (this.contractCSDCPTY != null) {
            if (!Util.isEmpty(this.contractCSDCPTY.getAdditionalField(IM_TERMINATION_CCY))) {
                result = this.contractCSDCPTY.getAdditionalField(IM_TERMINATION_CCY);
            }
        }
        return result;
    }

    private String getLogicCsdIMCPTYReportingCcy() {

        String result = "";
        if (this.contractCSDCPTY != null) {
            if (!Util.isEmpty(this.contractCSDCPTY.getAdditionalField(IM_REPORTING_CCY))) {
                result = this.contractCSDCPTY.getAdditionalField(IM_REPORTING_CCY);
            }
        }
        return result;
    }

    public boolean isFilterAccepted() {
        return filterApproved;
    }

    public String generateOutput() {
        StringBuilder infoFormatted = new StringBuilder();

        infoFormatted.append(this.processDate);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.valueDate);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.frontId);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.contractName);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.contractCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.calculationMethod);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.po);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.cpty);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.maturityDate);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.maturityDate2);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.noticional);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.noticionalCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.noticional2);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.noticional2Ccy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.mtm);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.mtmCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.mtm2);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.mtm2Ccy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.foSystemKw);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.boSystemKw);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.boReferenceKw);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.upiReference);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.productType);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.terminationCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.csdIMPOReportingCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.csdIMCPTYContractCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.csdIMCPTYTerminationCcy);
        infoFormatted.append(SEPARATOR);

        infoFormatted.append(this.csdIMCPTYReportingCcy);
        infoFormatted.append(END_LINE);

        Log.info(SantInitialMarginExportOutput.class.getName(), "Rellenamos info boleta --------------> " + infoFormatted.toString());

        return infoFormatted.toString();
    }

    private HashMap<String, String> getIMCalc(Map<String, String> map) {
        HashMap<String, String> methods = new HashMap<String, String>();

        for (Entry<String, String> entry : map.entrySet()) {
            if (String.valueOf(entry.getKey()).contains(IM_CALC_METHOD)) {
                methods.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return methods;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public void setMaturityDate(String maturityDate) {
        this.maturityDate = maturityDate;
    }

    public void setNoticional(String noticional) {
        this.noticional = noticional;
    }

    public void setNoticionalCcy(String noticionalCcy) {
        this.noticionalCcy = noticionalCcy;
    }

    public void setNoticional2(String noticional2) {
        this.noticional2 = noticional2;
    }

    public void setNoticional2Ccy(String noticional2Ccy) {
        this.noticional2Ccy = noticional2Ccy;
    }

    public void setMtm(String mtm) {
        this.mtm = mtm;
    }

    public void setMtmCcy(String mtmCcy) {
        this.mtmCcy = mtmCcy;
    }

    public void setMtm2(String mtm2) {
        this.mtm2 = mtm2;
    }

    public void setMtm2Ccy(String mtm2Ccy) {
        this.mtm2Ccy = mtm2Ccy;
    }

    public void setFrontId(String frontId) {
        this.frontId = frontId;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public void setBoReferenceKw(String boReferenceKw) {
        this.boReferenceKw = boReferenceKw;
    }

    protected abstract String getLogicMaturityDate();

    protected abstract String getLogicNotional();

    protected abstract String getLogicNotionalCcy();

    protected abstract String getLogicNotional2();

    protected abstract String getLogicNotional2Ccy();

    protected abstract String getLogicMtm();

    protected abstract String getLogicMtmCcy();

    protected abstract String getLogicMtm2();

    protected abstract String getLogicMtm2Ccy();

    protected abstract String getLogicFrontId();

    protected abstract String getLogicBoReferenceKw();

    protected abstract String getLogicProductType();

    public abstract void fillInfoByProduct();


}
