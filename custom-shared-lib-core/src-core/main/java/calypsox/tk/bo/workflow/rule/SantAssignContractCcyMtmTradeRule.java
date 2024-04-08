package calypsox.tk.bo.workflow.rule;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.sql.PLMarkSQL;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.MarketDataServerImpl;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import static calypsox.util.TradeInterfaceUtils.*;

/**
 * This rule will create a MTM with informations passed in the trade transient
 * keywords.
 *
 * @author aela
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SantAssignContractCcyMtmTradeRule implements WfTradeRule {

    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    // chach for the pricing env
    private static HashMap<String, PricingEnv> pricingEnvsCache = new HashMap<String, PricingEnv>();
    protected CollateralServiceRegistry collateralServiceRegistry = ServiceRegistry.getDefault();

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector exception, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "create a MTM with informations passed in the trade transient keywords";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection ds,
                          Vector exception, Task task, Object dbCon, Vector events) {
        PricingEnv pricingEnv = null;
        CollateralConfig marginCallConfig = null;

        // For the moment, this rule applies only for CollateralExposure deals
        if ((trade.getProduct() == null) || !(trade.getProduct() instanceof CollateralExposure)) {
            return true;
        }

        CollateralExposure product = (CollateralExposure) trade.getProduct();
        try {
            Trade tradeToGetPLMark = (Trade) trade.clone();
            if (trade.getLongId() == 0) {
                tradeToGetPLMark.setLongId(trade.getAllocatedLongSeed());
            }

            // check if the contract have bean already assigned
            int mccId = 0;
            try {
                mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

            } catch (Exception e) {
                Log.error(this, e); // sonar
                mccId = 0;
            }

            if (mccId == 0) {
                marginCallConfig = this.collateralServiceRegistry.getCollateralDataServer().getMarginCallConfig(trade);
            } else {
                marginCallConfig = CacheCollateralClient.getCollateralConfig(ds, mccId);
            }

            double mtmAmount = Double.parseDouble(trade.getKeywordValue(TRANS_TRADE_KWD_MTM));
            JDate mtmDate = JDate.valueOf(dateFormat.parse(trade.getKeywordValue(TRANS_TRADE_KWD_MTM_DATE)));
            String mtmCcy = trade.getKeywordValue(TRANS_TRADE_KWD_MTM_CCY);

            if (marginCallConfig != null) {
                if (pricingEnvsCache.get(marginCallConfig.getPricingEnvName()) == null) {
                    pricingEnv = new MarketDataServerImpl().getPricingEnv(marginCallConfig.getPricingEnvName(),
                            trade.getTradeDate());
                    pricingEnvsCache.put(marginCallConfig.getPricingEnvName(), pricingEnv);

                } else {
                    pricingEnv = (PricingEnv) pricingEnvsCache.get(marginCallConfig.getPricingEnvName());
                }

                mtmCcy = marginCallConfig.getCurrency();
                // then convert the mtm into the contract
                // currency

                if (CollateralUtilities.isTwoLegsProductType(product.getSubType())) {

                    double mtmAmount1 = (Double) product.getAttribute(COL_CTX_PROP_MTM_1);
                    String mtmCcy1 = (String) product.getAttribute(COL_CTX_PROP_MTM_CCY_1);
                    double mtmAmount2 = (Double) product.getAttribute(COL_CTX_PROP_MTM_2);
                    String mtmCcy2 = (String) product.getAttribute(COL_CTX_PROP_MTM_CCY_2);

                    mtmAmount = 0;
                    if (!Util.isEmpty(mtmCcy1) && !Util.isEmpty(mtmCcy2) && !mtmCcy1.equals(mtmCcy2)) {
                        trade.setTradeCurrency(marginCallConfig.getCurrency());
                        trade.setSettleCurrency(marginCallConfig.getCurrency());
                        product.setCurrency(marginCallConfig.getCurrency());

                        mtmAmount += CollateralUtilities.convertCurrency(mtmCcy1, mtmAmount1,
                                marginCallConfig.getCurrency(), mtmDate, pricingEnv);
                        mtmAmount += CollateralUtilities.convertCurrency(mtmCcy2, mtmAmount2,
                                marginCallConfig.getCurrency(), mtmDate, pricingEnv);
                    } else {
                        mtmAmount = mtmAmount1 + mtmAmount2;
                    }

                } else {
                    mtmAmount = CollateralUtilities.convertCurrency(mtmCcy, mtmAmount, marginCallConfig.getCurrency(),
                            mtmDate, pricingEnv);
                }

                // List of plMarks to save
                List<PLMarkValue> markValues = new ArrayList<PLMarkValue>();
                // MARGIN_CALL
                markValues.add(createPLMarkValue(mtmAmount, mtmCcy, mtmDate, PricerMeasure.S_MARGIN_CALL));
                // NPV
                markValues.add(createPLMarkValue(mtmAmount, mtmCcy, mtmDate, PricerMeasure.S_NPV));
                // INDEPENDENT_AMOUNT
                if (!Util.isEmpty(trade.getKeywordValue(TRANS_TRADE_KWD_MTM_IA))) {
                    double mtmIAAmount = Double.parseDouble(trade.getKeywordValue(TRANS_TRADE_KWD_MTM_IA));
                    JDate mtmIADate = JDate
                            .valueOf(dateFormat.parse(trade.getKeywordValue(TRANS_TRADE_KWD_MTM_IA_DATE)));
                    String mtmIACcy = trade.getKeywordValue(TRANS_TRADE_KWD_MTM_IA_CCY);

                    markValues.add(
                            createPLMarkValue(mtmIAAmount, mtmIACcy, mtmIADate, PricerMeasure.S_INDEPENDENT_AMOUNT));
                }

                // create and save the plMark
                PLMark plMark = createPLMarks(tradeToGetPLMark, trade.getBookId(), mtmDate,
                        marginCallConfig.getPricingEnvName(), markValues);
                PLMarkSQL.save(plMark, (Connection) dbCon);
            }
        } catch (Exception e1) {
            Log.error(this, "Error while adding the mtm amount on the trade " + trade.getLongId(), e1);
            messages.add("Error while adding the mtm amount " + e1.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Method for create PLMarks
     *
     * @param tradeId   id for the trade
     * @param bookID
     * @param repoBean2 Array of strings for get the data, the currency and the mark
     *                  value
     * @return
     */
    private PLMark createPLMarks(Trade trade, int bookID, JDate plMarkDate, String pricingEnvName,
                                 List<PLMarkValue> plmarkValues) {
        PLMark plMark = CollateralUtilities.createPLMarkIfNotExists(trade, pricingEnvName, plMarkDate);
        // Fill the PLMark.
        plMark.setTradeLongId(trade.getLongId());
        plMark.setBookId(bookID);
        plMark.setPricingEnvName(pricingEnvName);
        plMark.setValDate(plMarkDate);
        // add the plMarkValues

        for (PLMarkValue value : plmarkValues) {
            plMark.addPLMarkValue(value);
        }

        return plMark;
    }

    /**
     * @param mtmValue
     * @param mtmCurrency
     * @param mtmDate
     * @param plMarkValueName
     * @return a PLMarkValue with the given values
     */
    private PLMarkValue createPLMarkValue(Double mtmValue, String mtmCurrency, JDate mtmDate, String plMarkValueName) {
        PLMarkValue plMarkValue = new PLMarkValue();
        // Fill the PLMarketValue.
        plMarkValue.setMarkValue(mtmValue);
        plMarkValue.setCurrency(mtmCurrency);
        // Migration V14 - 04012016
        plMarkValue.setOriginalCurrency(mtmCurrency);
        plMarkValue.setMarkName(plMarkValueName);

        return plMarkValue;

    }
}
