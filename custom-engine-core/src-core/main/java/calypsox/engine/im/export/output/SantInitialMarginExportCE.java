package calypsox.engine.im.export.output;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;

import java.util.Locale;
import java.util.TimeZone;

public class SantInitialMarginExportCE extends SantInitialMarginExportOutput {

    protected static final String CCY_2_ATT = "CCY_2";
    protected static final String NOMINAL_2_ATT = "NOMINAL_2";
    protected static final String CCY_1_ATT = "CCY_1";
    protected static final String NOMINAL_1_ATT = "NOMINAL_1";
    protected static final String NUM_FRONT_ID_KW = "NUM_FRONT_ID";
    protected static final String BO_REFERENCE_KW = "BO_REFERENCE";

    CollateralExposure ce;


    /**
     * @param sdf             StaticDataFilter IM_QEF_EXPORT_FILTER
     * @param trade           Extracted Trade
     * @param entry           MarginCallDetailEntryDTO
     * @param contractCSA     CollateralConfig CSA
     * @param contractCSDPO
     * @param contractCSDCPTY
     * @param pricingEnvName  Princing Enviroment
     * @author epalaobe
     * <p>
     * New constructor to not extract the trade and the static data filter all times.
     */
    public SantInitialMarginExportCE(StaticDataFilter sdf, Trade trade, MarginCallDetailEntryDTO entry, CollateralConfig contractCSA, CollateralConfig contractCSDPO, CollateralConfig contractCSDCPTY, String pricingEnvName) {
        super(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        this.ce = (CollateralExposure) trade.getProduct();
    }

    @Override
    public void fillInfoByProduct() {

        super.fillInfo();

        setMaturityDate(getLogicMaturityDate());
        setNoticional(getLogicNotional());
        setNoticionalCcy(getLogicNotionalCcy());
        setNoticional2(getLogicNotional2());
        setNoticional2Ccy(getLogicNotional2Ccy());
        setMtm(getLogicMtm());
        setMtmCcy(getLogicMtmCcy());
        setMtm2(getLogicMtm2());
        setMtm2Ccy(getLogicMtm2Ccy());
        setFrontId(getLogicFrontId());
        setBoReferenceKw(getLogicBoReferenceKw());
        setProductType(getLogicProductType());
    }

    @Override
    protected String getLogicMaturityDate() {
        String result = "";

        if (this.ce != null) {
            JDate endDate = this.ce.getEndDate();

            if (endDate != null) {
                result = sdf_ddMMyyyy.format(endDate.getDate(TimeZone.getDefault()));
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotional() {
        String result = "0";

        if (this.ce != null) {
            String subType = this.ce.getSubType();

            if (!Util.isEmpty(subType)) {
                if (CollateralUtilities.isTwoLegsProductType(subType)) {
                    Object nominal1 = this.ce.getAttribute(NOMINAL_1_ATT);

                    if (nominal1 != null) {
                        String nominal1Value = nominal1.toString();
                        if (!Util.isEmpty(nominal1Value)) {
                            result = nominal1Value;
                        }
                    }
                } else {
                    result = Util.numberToString(this.ce.getPrincipal());
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotionalCcy() {
        String result = "";

        if (this.ce != null) {
            String subType = this.ce.getSubType();

            if (!Util.isEmpty(subType)) {
                if (CollateralUtilities.isTwoLegsProductType(subType)) {
                    Object ccy1 = this.ce.getAttribute(CCY_1_ATT);

                    if (ccy1 != null) {
                        result = ccy1.toString();
                    }
                } else {
                    result = this.ce.getCurrency();
                }
            }
        }
        return result.toUpperCase(Locale.getDefault());
    }

    @Override
    protected String getLogicNotional2() {
        String result = "0";

        if (this.ce != null) {
            String subType = this.ce.getSubType();

            if (!Util.isEmpty(subType)) {
                if (CollateralUtilities.isTwoLegsProductType(subType)) {
                    Object nominal2 = this.ce.getAttribute(NOMINAL_2_ATT);

                    if (nominal2 != null) {
                        String nominal2Value = nominal2.toString();
                        if (!Util.isEmpty(nominal2Value)) {
                            result = nominal2Value;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotional2Ccy() {
        String result = "";

        if (this.ce != null) {
            String subType = this.ce.getSubType();

            if (!Util.isEmpty(subType)) {
                if (CollateralUtilities.isTwoLegsProductType(subType)) {
                    Object ccy2 = this.ce.getAttribute(CCY_2_ATT);

                    if (ccy2 != null) {
                        result = ccy2.toString();
                    }
                }
            }
        }
        return result.toUpperCase(Locale.getDefault());
    }

    @Override
    protected String getLogicMtm() {
        String result = "";
        if (this.trade != null && this.plMark != null) {
            if (CollateralUtilities.isTwoLegsProductType(trade.getProductSubType())) {
                for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV_LEG1)) {
                        result = Util.numberToString(markValue.getMarkValue());
                    }
                }
            } else {

                for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV)) {
                        result = Util.numberToString(markValue.getMarkValue());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Retrieve the Mark-to-Market currency code.
     *
     * @return The currency code for the Mark-to-Market.
     */
    @Override
    protected String getLogicMtmCcy() {
        String result = "";
        if (this.trade != null && this.plMark != null) {
            if (CollateralUtilities.isTwoLegsProductType(trade.getProductSubType())) {
                for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV_LEG1)) {
                        String ccy = markValue.getCurrency();

                        if (!Util.isEmpty(ccy)) {
                            result = ccy.toUpperCase();
                        }
                    }
                }
            } else {
                for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV)) {
                        String ccy = markValue.getCurrency();

                        if (!Util.isEmpty(ccy)) {
                            result = ccy.toUpperCase();
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicMtm2() {
        String result = "";
        if (this.trade != null && this.plMark != null) {
            if (CollateralUtilities.isTwoLegsProductType(trade.getProductSubType())) {
                for (PLMarkValue markValue : this.plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV_LEG2)) {
                        result = Util.numberToString(markValue.getMarkValue());
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicMtm2Ccy() {
        String result = "";

        if (this.trade != null && this.plMark != null) {
            if (CollateralUtilities.isTwoLegsProductType(trade.getProductSubType())) {
                for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                    if (markValue.getMarkName().equals(NPV_LEG2)) {
                        String ccy = markValue.getCurrency();
                        if (!Util.isEmpty(ccy)) {
                            result = ccy.toUpperCase();
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicFrontId() {
        String result = "";

        if (this.trade != null) {
            result = this.trade.getKeywordValue(NUM_FRONT_ID_KW);
        }
        return result;
    }

    @Override
    protected String getLogicBoReferenceKw() {
        String result = "";

        if (this.trade != null) {
            result = this.trade.getKeywordValue(BO_REFERENCE_KW);
        }
        return result;
    }

    @Override
    protected String getLogicProductType() {
        String result = "";

        if (this.trade != null) {
            result = trade.getProductSubType();
        }
        return result;
    }

}
