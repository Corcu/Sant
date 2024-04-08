package calypsox.engine.im.export.output;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;

import java.util.Locale;
import java.util.TimeZone;

/**
 * @author aalonsop
 */
public class SantInitialMarginExportBond extends SantInitialMarginExportOutput {

    Bond bond;

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
    protected SantInitialMarginExportBond(StaticDataFilter sdf, Trade trade, MarginCallDetailEntryDTO entry, CollateralConfig contractCSA, CollateralConfig contractCSDPO, CollateralConfig contractCSDCPTY, String pricingEnvName) {
        super(sdf, trade, entry, contractCSA, contractCSDPO, contractCSDCPTY, pricingEnvName);
        this.bond = (Bond) trade.getProduct();
    }

    @Override
    protected String getLogicMaturityDate() {
        String result = "";
        if (this.bond != null) {
            JDate endDate = this.bond.getMaturityDate();
            if (endDate != null) {
                result = sdf_ddMMyyyy.format(endDate.getDate(TimeZone.getDefault()));
            }
        }
        return result;
    }

    @Override
    protected String getLogicNotional() {
        return Util.numberToString(this.trade.computeNominal());
    }

    @Override
    protected String getLogicNotionalCcy() {
        String result = this.bond.getCurrency();
        return result.toUpperCase(Locale.getDefault());
    }

    @Override
    protected String getLogicNotional2() {
        return "0";
    }

    @Override
    protected String getLogicNotional2Ccy() {
        return "";
    }

    @Override
    protected String getLogicMtm() {
        String result = "";
        if (this.trade != null && this.plMark != null) {
            for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV)) {
                    result = Util.numberToString(markValue.getMarkValue());
                }
            }
        }
        return result;
    }

    @Override
    protected String getLogicMtmCcy() {
        String result = "";
        if (this.trade != null && this.plMark != null) {
            for (PLMarkValue markValue : plMark.getMarkValuesAsList()) {
                if (markValue.getMarkName().equals(NPV)) {
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
        return "";
    }

    @Override
    protected String getLogicMtm2Ccy() {
        return "";
    }

    @Override
    protected String getLogicFrontId() {
        return this.trade.getKeywordValue("Contract ID") != null ? this.trade.getKeywordValue("Contract ID") : this.trade.getExternalReference();
    }

    @Override
    protected String getLogicBoReferenceKw() {
        return String.valueOf(this.trade.getLongId());
    }

    @Override
    protected String getLogicProductType() {
        return "BOND_FORWARD";
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
}
