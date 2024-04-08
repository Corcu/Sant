package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.Fee;
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
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import static com.calypso.tk.core.PricerMeasure.S_INDEPENDENT_AMOUNT;

/*
 * Checks if Trade has a Fee of type IND_AMOUNT.
 * Then checks if there is an INDEPENDENT_AMOUNT PLMark for the processDate(usually a day before except on the TradeDate).
 * If PLMark is not found, it takes the Fee amount and save it as INDEPENDENT_AMOUNT PLMark.
 * If INDEPENDENT_AMOUNT PLMark is found nothing does nothing.
 * If the trade is CollateralExposure and underlying type is CONTRACT_IA we always create INDEPENDENT_AMOUNT
 * with amount=CollateralExposure trade principal
 */
public class SantSaveTradeIndAmountFeeASMarkTradeRule implements WfTradeRule {

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "if Trade has no INDEPENDENT_AMOUNT PLMark and Trade has a fee of type . \n"
                + "IND_AMOUNT then it takes the Amount from Fee and saves it as \n" + "INDEPENDENT_AMOUNT PLMark. ";
        return desc;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        Log.debug("SantSaveTradeIndAmountFeeASMarkTradeRule", "check - Start");

        final JDate processDate = CollateralUtilities.getMTMProcessDate(trade);

        PLMark currentPLMark = null;

        try {

            if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                    || trade.getProductType().equals(CollateralStaticAttributes.REPO)
                    || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {

                Fee intAmountFee = CollateralUtilities.getTradeFee(trade,
                        CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT);
                Fee intAmountPOFee = CollateralUtilities.getTradeFee(trade,
                        CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT_PO);

                if ((intAmountFee != null) && (intAmountPOFee != null)) {
                    messages.add("Both " + CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT + " and "
                            + CollateralStaticAttributes.FEE_TYPE_IND_AMOUNT_PO
                            + " are present for the Trade.[SantSaveTradeIndAmountFeeASMarkTradeRule]");
                    return false;
                }

                if (intAmountPOFee != null) {
                    intAmountFee = intAmountPOFee;
                }

                if ((intAmountFee == null) && (!CollateralUtilities.isIAExposureTrade(trade))) {
                    return true;
                }

                ArrayList<String> errorMsgs = new ArrayList<String>();
                CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
                if (marginCallConfig == null) {
                    messages.add("No marginCall Contract found for the trade.[SantSaveTradeIndAmountFeeASMarkTradeRule]");
                    return false;
                }

                // we have IND_AMOUNT Fee attached to the trade or is an IA
                // Exposure Trade. Check if there is IND_AMOUNT plmarkValue
                PLMarkValue indAmountMarkValue = null;
                if (trade.getLongId() == 0) {
                    // When you save the first time you won't have trade id. In
                    // case of IA Exposure save INDEPENDENT_AMOUNT with
                    // alloocated seed
                    final Trade tempTrade = trade.clone();
                    tempTrade.setLongId(trade.getAllocatedLongSeed());
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(tempTrade,
                            marginCallConfig.getPricingEnvName(), processDate);
                } else {
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(trade,
                            marginCallConfig.getPricingEnvName(), processDate);
                }

                final PLMarkValue existingIndAmount = CollateralUtilities.retrievePLMarkValue(currentPLMark,
                        S_INDEPENDENT_AMOUNT);

                if ((existingIndAmount == null) || CollateralUtilities.isIAExposureTrade(trade)) {

                    double indAmountDbl = 0.0; // intAmountFee.getAmount();
                    String indAmountCcy = ""; // intAmountFee.getCurrency();
                    if (CollateralUtilities.isIAExposureTrade(trade)) {
                        final CollateralExposure product = (CollateralExposure) trade.getProduct();
                        indAmountDbl = product.getPrincipal() * product.getBuySell(trade);
                        indAmountCcy = trade.getSettleCurrency();
                    } else {
                        indAmountDbl = intAmountFee.getAmount();
                        indAmountCcy = intAmountFee.getCurrency();
                    }

                    // Load PE
                    PricingEnv pricingEnv = CollateralUtilities.getPricingEnvFromTempCache(dsCon,
                            marginCallConfig.getPricingEnvName(), new JDatetime(processDate, TimeZone.getDefault()));

                    if (pricingEnv == null) {
                        pricingEnv = PricingEnvSQL
                                .get(marginCallConfig.getPricingEnvName(), new JDatetime(processDate, TimeZone.getDefault()));
                    }

                    // If the Fee is not in Trade currency.... conversion required
                    if (!trade.getSettleCurrency().equals(indAmountCcy)) {
                        indAmountDbl = CollateralUtilities.convertCurrency(indAmountCcy, indAmountDbl,
                                trade.getSettleCurrency(), processDate, pricingEnv);
                        indAmountCcy = trade.getSettleCurrency();
                    }

                    indAmountMarkValue = CollateralUtilities.buildPLMarkValue(S_INDEPENDENT_AMOUNT, indAmountCcy,
                            indAmountDbl, CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

                    if (errorMsgs.size() == 0) {
                        final PLMarkValue indAmountBaseMarkValue = CollateralUtilities.convertPLMarkValueToBase(
                                indAmountMarkValue, pricingEnv, marginCallConfig.getCurrency(), processDate, errorMsgs);

                        currentPLMark.addPLMarkValue(indAmountMarkValue);
                        currentPLMark.addPLMarkValue(indAmountBaseMarkValue);
                    }

                    if (errorMsgs.size() > 0) {
                        // get error messages and raise Tasks.
                        for (final String error : errorMsgs) {
                            messages.add(error + ".[SantSaveTradeIndAmountFeeASMarkTradeRule]");
                        }
                        return false;
                    }

                    // 2. Save INDEPENDENT_AMOUNT PLMarkvalue
                    PLMarkSQL.save(currentPLMark, (Connection) dbCon);

                } else {
                    // If it is already there then it must have been coming from
                    // Feed or the blotter.
                    // Nothing to do in which case.
                    return true;
                }
            } else {
                String msg = "Invalid Trade Type. Only Repo/SecLending/CollateralExposure are allowed.";
                msg = msg + ".[SantSaveTradeIndAmountFeeASMarkTradeRule]";
                messages.add(msg);
                return false;
            }

        } catch (final Exception e) {
            String msg = "Error saving INDEPENDENT_AMOUNT for TradeId=" + trade.getLongId() + ". Error = " + e.getMessage();
            msg = msg + ".[SantSaveTradeIndAmountFeeASMarkTradeRule]";
            messages.add(msg);
            Log.error(this, e);//Sonar
            return false;
        }

        Log.debug("SantSaveTradeIndAmountFeeASMarkTradeRule", "Update - End");

        return true;
    }
}
