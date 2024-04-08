package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.sql.PLMarkSQL;
import com.calypso.tk.marketdata.sql.PricingEnvSQL;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import static com.calypso.tk.core.PricerMeasure.S_MARGIN_CALL;
import static com.calypso.tk.core.PricerMeasure.S_NPV;

/*
 * Checks if there is an NPV PLMark for the current date.
 * If NPV is not found, stops the trade from STP and creates a task saying NPV is missing.
 * If NPV is found it calculates the MARGIN_CALL and saves it as PLMark.
 * It takes FO_HAIRCUT trade keyword in to account if exists otherwise uses OSLA_FACTOR of the MarginCall Contract.
 */
public class SantCalcMarginCallTradeRule implements WfTradeRule {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        Log.debug("SantCalcMarginCallTradeRule", "check - Start");

        if (trade == null) {
            return false;
        }

        // For Independent_Amount ExposureTrades we save it NPV as Zero in
        // update method
        if (CollateralUtilities.isIAExposureTrade(trade)) {
            return true;
        }

        final JDate processDate = CollateralUtilities.getMTMProcessDate(trade);

        // When we save the trade first time we wont have id so wont have NPV as
        // well.
        // In which case we need to stop the trade from STPing
        if (trade.getLongId() == 0) {
            final String msg = "NPV not found for trade for date " + processDate;
            messages.add(msg);
            return false;
        }

        try {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);

            if (CollateralUtilities.retrievePLMarkValue(trade, dsCon, S_NPV, marginCallConfig.getPricingEnvName(),
                    processDate) == null) {
                final String msg = "NPV not found for the date " + processDate + ", TradeId=" + trade.getLongId();
                // msg = msg + ".[SantCalcMarginCall]";
                messages.add(msg);
                return false;
            }
        } catch (final RemoteException e) {
            final String msg = "Error retreiving NPV for the date " + processDate + ", TradeId=" + trade.getLongId();
            // msg = msg + ".[SantCalcMarginCall]";
            messages.add(msg);
            Log.error(this, e); //sonar
            return false;
        }

        Log.debug("SantCalcMarginCallTradeRule", "check - End");

        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "Gets NPV PLMark for the current date. \n"
                + "If trade has FO_HAIRCUT trade keyword it uses it otherwise it uses MCContracts OslaFactor\n"
                + "to calculate MarginCall and saves it as PLMark ";
        return desc;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        Log.debug("SantCalcMarginCallTradeRule", "Update - Start");

        if (trade == null) {
            return false;
        }
        Log.debug("SantCalcMarginCallTradeRule", "Trade id = " + trade.getLongId());

        final JDate processDate = CollateralUtilities.getMTMProcessDate(trade);

        PLMark currentPLMark = null;

        try {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
            if (marginCallConfig == null) {
                messages.add("No marginCall Contract found for the trade.[SantCalcMarginCallTradeRule]");
                return false;
            }

            // For Independent_Amount ExposureTrades we save NPV it as Zero
            if (CollateralUtilities.isIAExposureTrade(trade)) {
                if (trade.getLongId() == 0) {
                    // When you save the first time you won't have trade id. In
                    // case of IA Exposure save NPV etc with
                    // alloocated seed
                    final Trade tempTrade = (Trade) trade.clone();
                    tempTrade.setLongId(trade.getAllocatedLongSeed());
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(tempTrade,
                            marginCallConfig.getPricingEnvName(), processDate);
                } else {
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(trade,
                            marginCallConfig.getPricingEnvName(), processDate);
                }
                if (CollateralUtilities.retrievePLMarkValue(currentPLMark, S_NPV) == null) {
                    final PLMarkValue npv = CollateralUtilities.buildPLMarkValue(S_NPV, trade.getSettleCurrency(), 0.0,
                            CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);
                    currentPLMark.addPLMarkValue(npv);
                }
            } else {
                // currentPLMark = CollateralUtilities.createPLMarkIfNotExists(trade,
                // marginCallConfig.getPricingEnvName(), processDate);
                currentPLMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                        marginCallConfig.getPricingEnvName(), processDate);
            }

            final PLMarkValue existingMarginCall = CollateralUtilities
                    .retrievePLMarkValue(currentPLMark, S_MARGIN_CALL);

            if (existingMarginCall == null) {
                final ArrayList<String> errorMessages = new ArrayList<String>();
                // 1. calculate MARGIN_CALL
                if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                        || trade.getProductType().equals(CollateralStaticAttributes.REPO)
                        || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)
                        || trade.getProductType().equals(PerformanceSwap.PERFORMANCESWAP)) {

                    PricingEnv pricingEnv = CollateralUtilities.getPricingEnvFromTempCache(dsCon,
                            marginCallConfig.getPricingEnvName(), new JDatetime(processDate, TimeZone.getDefault()));

                    if (pricingEnv == null) {
                        pricingEnv = PricingEnvSQL
                                .get(marginCallConfig.getPricingEnvName(), new JDatetime(processDate, TimeZone.getDefault()));
                    }

                    CollateralUtilities.calculateMCAndNpvBase(dsCon, trade, currentPLMark, pricingEnv, processDate,
                            null, errorMessages);
                } else {
                    String msg = "Invalid Trade Type. Only Repo/SecLending/CollateralExposure are allowed.";
                    msg = msg + ".[SantCalcMarginCall]";
                    messages.add(msg);
                    return false;
                }

                if (errorMessages.size() > 0) {
                    // get error messages and raise Tasks.
                    for (final String error : errorMessages) {
                        messages.add(error + ".[SantCalcMarginCall]");
                    }
                    return false;
                }

                PLMarkSQL.save(currentPLMark, (Connection) dbCon);

            }
        } catch (final Exception e) {
            String msg = "Error saving MARGIN_CALL for TradeId=" + trade.getLongId() + ". Error = " + e.getMessage();
            msg = msg + ".[SantCalcMarginCall]";
            messages.add(msg);
            Log.error(this, e); //sonar
            return false;
        }

        Log.debug("SantCalcMarginCallTradeRule", "Update - End");

        return true;
    }

}
