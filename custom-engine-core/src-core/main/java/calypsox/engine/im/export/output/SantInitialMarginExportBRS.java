package calypsox.engine.im.export.output;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;

import java.util.Locale;
import java.util.TimeZone;

public class SantInitialMarginExportBRS extends SantInitialMarginExportOutput {

    protected final String MUREX_ROOT_CONTRACT = "MurexRootContract";
    PerformanceSwap brs;


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
    public SantInitialMarginExportBRS(StaticDataFilter sdf, Trade trade, MarginCallDetailEntryDTO entry, CollateralConfig contractCSA, CollateralConfig contractCSDPO, CollateralConfig contractCSDCPTY, String pricingEnvName) {
        super(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        this.brs = (PerformanceSwap) trade.getProduct();
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

        if (this.brs != null) {
            JDate endDate = this.brs.getMaturityDate();

            if (endDate != null) {
                result = sdf_ddMMyyyy.format(endDate.getDate(TimeZone.getDefault()));
            }
        }
        return result;
    }

    private boolean isTwoLegsPerformanceSwap(PerformanceSwap brs) {
        if (null != brs.getPrimaryLeg() && null != brs.getSecondaryLeg()) return true;
        return false;
    }

    @Override
    protected String getLogicNotional() {
        String result = "0";

        if (this.brs != null) {
            String subType = this.brs.getSubType();

            if (!Util.isEmpty(subType)) {
                if (isTwoLegsPerformanceSwap(this.brs)) {
                    double nominal1 = this.brs.getPrimaryLegNotional(JDate.getNow());
                    if (nominal1 != 0.0) {
                        String nominal1Value = Util.numberToString(nominal1);
                        if (!Util.isEmpty(nominal1Value)) {
                            result = nominal1Value;
                        }
                    }
                } else {
                    result = Util.numberToString(this.brs.getNotional(JDate.getNow()));
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotionalCcy() {
        String result = "";

        if (this.brs != null) {
            String subType = this.brs.getSubType();

            if (!Util.isEmpty(subType)) {
                if (isTwoLegsPerformanceSwap(this.brs)) {
                    String ccy1 = this.brs.getPrimaryLegCurrency();

                    if (!Util.isEmpty(ccy1)) {
                        return ccy1;
                    }
                } else {
                    return this.brs.getCurrency();
                }
            }
        }
        return result.toUpperCase(Locale.getDefault());
    }

    @Override
    protected String getLogicNotional2() {
        String result = "0";

        if (this.brs != null) {
            String subType = this.brs.getSubType();

            if (!Util.isEmpty(subType)) {
                if (isTwoLegsPerformanceSwap(this.brs)) {
                    double nominal2 = this.brs.getSecondaryLegNotional(JDate.getNow());
                    if (nominal2 != 0.0) {
                        String nominal2Value = Util.numberToString(nominal2);
                        if (!Util.isEmpty(nominal2Value)) {
                            result = nominal2Value;
                        }
                    }
                } else {
                    result = Util.numberToString(this.brs.getNotional(JDate.getNow()));
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotional2Ccy() {
        String result = "";

        if (this.brs != null) {
            String subType = this.brs.getSubType();

            if (!Util.isEmpty(subType)) {
                if (isTwoLegsPerformanceSwap(this.brs)) {
                    String ccy2 = this.brs.getSecondaryLegCurrency();

                    if (!Util.isEmpty(ccy2)) {
                        return ccy2;
                    }
                } else {
                    return this.brs.getCurrency();
                }
            }
        }
        return result.toUpperCase(Locale.getDefault());
    }

    @Override
    protected String getLogicMtm() {
        String result = "";
        if (this.brs != null && this.plMark != null) {
            for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV_LEG1)) {
                    result = Util.numberToString(markValue.getMarkValue());
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
        if (this.brs != null && this.plMark != null) {
            for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV_LEG1)) {
                    String ccy = markValue.getCurrency();

                    if (!Util.isEmpty(ccy)) {
                        result = ccy.toUpperCase();
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicMtm2() {
        String result = "";
        if (this.brs != null && this.plMark != null) {
            for (PLMarkValue markValue : this.plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV_LEG2)) {
                    result = Util.numberToString(markValue.getMarkValue());
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicMtm2Ccy() {
        String result = "";

        if (this.brs != null && this.plMark != null) {
            for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV_LEG2)) {
                    String ccy = markValue.getCurrency();
                    if (!Util.isEmpty(ccy)) {
                        result = ccy.toUpperCase();
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicFrontId() {
        String frontId = this.trade.getKeywordValue(MUREX_ROOT_CONTRACT);
        if (!Util.isEmpty(frontId)) return frontId;
        return null;
    }

    @Override
    protected String getLogicBoReferenceKw() {
        return String.valueOf(this.trade.getLongId());
    }

    @Override
    protected String getLogicProductType() {
        String result = "";

        if (this.trade != null) {
            result = trade.getProductType().toUpperCase();
        }
        return result;
    }

}
