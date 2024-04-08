package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.sql.PLMarkSQL;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.*;

/**
 * Try to automatically dispute the given entry
 *
 * @author aela
 */
public class SantDisputeContractCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Try to automatically dispute the given entry.";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        // margin call entry valuation date
        JDatetime valDateTime = paramMarginCallEntry.getLastUpdate();
        if (valDateTime == null) {
            valDateTime = paramMarginCallEntry.getValueDatetime();
        }
        Calendar cal = Calendar.getInstance(paramMarginCallEntry.getValuationTimeZone());
        cal.setTimeInMillis(valDateTime.getTime());
        cal.set(Calendar.AM_PM, Calendar.PM);
        cal.set(Calendar.HOUR, 11);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);

        JDatetime lastValDateTime = new JDatetime(cal.getTime());

        // get the contract
        CollateralConfig mcc = paramMarginCallEntry.getCollateralConfig();
        if (mcc == null) {
            mcc = CacheCollateralClient.getCollateralConfig(paramDSConnection,
                    paramMarginCallEntry.getCollateralConfigId());
            if (mcc == null) {
                paramList.add("Unable to get the margin call contract.");
                return false;
            }
        }

        // check that the contract is defined as to be disputed
        List<String> mccProductList = mcc.getProductList();
        List<String> mccExposureTypeList = mcc.getExposureTypeList();
        // START OA 27/11/2013 Enhancing the error message
        if (Util.isEmpty(mccProductList) || !mccProductList.contains(CollateralExposure.PRODUCT_TYPE)
                || Util.isEmpty(mccExposureTypeList) || !mccExposureTypeList.contains("DISPUTE_ADJUSTMENT")) {
            paramList
                    .add("The contract is not configured to be disputed (Need DISPUTE_ADJUSTMENT to be defined in the Exposure Types and "
                            + CollateralExposure.PRODUCT_TYPE + " to be contained in the Products)");
            return false;

        }
        // END OA 27/11/2013 Enhancing the error message
        // get the disputed trade
        Trade tradeDispute = null;
        try {
            TradeArray disputeTrades = new TradeArray();
            TradeSQL.getTrades(disputeTrades, "product_collateral_exposure",
                    "trade.product_id=product_collateral_exposure.product_id and product_collateral_exposure.mcc_id="
                            + mcc.getId()
                            + " and underlying_type='DISPUTE_ADJUSTMENT' and trade.trade_status<>'CANCELED' ",
                    "trade.trade_id", null);

            if (!Util.isEmpty(disputeTrades) && (disputeTrades.size() > 0)) {
                if (disputeTrades.size() > 1) {
                    paramList.add("More than one dispute adjustment trade found for the contract");
                    return false;
                }

                tradeDispute = disputeTrades.get(0);
            }

            // else {
            // paramList.add("Unable to get dispute adjustment trade from the contract "
            // + mcc.getId());
            // return false;
            // }
        } catch (Exception e) {
            Log.error(this, e);
            paramList.add("Unable to get dispute adjustment trade from the contract");
            return false;
        }

        // create a nex dispute trade for the current contract
        try {
            tradeDispute = createOrUpdateDipsuteExposureTrade(paramMarginCallEntry, mcc, tradeDispute,
                    paramDSConnection);
        } catch (Exception e) {
            Log.error(this, e);
            tradeDispute = null;
        }

        if (tradeDispute == null) {
            paramList.add("Unable to get dispute adjustment trade from the contract " + mcc.getId());
            return false;

        }

        // get the disputed trade plMark
        PLMark plMark = null;
        double dispTradeMrgCallValue = 0.0;
        // if (mcc.accept(tradeDispute,
        // paramMarginCallEntry.getValueDatetime(),
        // paramMarginCallEntry.getProcessDate(), paramDSConnection))

        // set the trade end date to
        CollateralExposure colExpProduct = (CollateralExposure) tradeDispute.getProduct();
        colExpProduct.setMaturityDate(paramMarginCallEntry.getValueDate());
        colExpProduct.setEndDate(paramMarginCallEntry.getValueDate());

        plMark = CollateralUtilities.createPLMarkIfNotExists(tradeDispute, mcc.getPricingEnvName(),
                paramMarginCallEntry.getValueDate());
        PLMarkValue mrgCallPlMark = CollateralUtilities.retrievePLMarkValue(plMark, PricerMeasure.S_MARGIN_CALL);
        if (mrgCallPlMark != null) {
            dispTradeMrgCallValue = mrgCallPlMark.getMarkValue();
        }

        // get the tolerance amount
        double toleranceAmount = mcc.getDisputeContractToleranceAmount();
        boolean isPercentage = mcc.isPercentageDisputeContractTolerance();
        Double cptyMtm = null;
        try {
            // get the cpty MtM
            cptyMtm = (Double) paramMarginCallEntry.getAttribute("Cpty MtM");
        } catch (Exception e) {
            paramList.add("Cannot get Cpty MtM value");
            Log.error(this, e);//Sonar
            return false;
        }

        if (cptyMtm == null) {
            paramList.add("Cpty MtM is not set.");
            return false;
        }
        double poMtM = paramMarginCallEntry.getNetBalance() - dispTradeMrgCallValue;
        // calculate the dispute amount
        double signedDisputeAmount = cptyMtm - poMtM;
        double calculatedDisputeAmount = Math.abs(signedDisputeAmount);

        paramMarginCallEntry.addAttribute("PO MtM", poMtM);
        paramMarginCallEntry.addAttribute("MtM difference", signedDisputeAmount);
        PLMarkValue npvValue = null;
        boolean toleranceExceeded;
        if (isPercentage) {
            toleranceExceeded = (calculatedDisputeAmount > (Math.abs(poMtM) * (toleranceAmount / 100)));
        } else {
            toleranceExceeded = (calculatedDisputeAmount > toleranceAmount);
        }
        if (toleranceExceeded) {
            paramMarginCallEntry.addAttribute("MtM Dispute Status", "Difference above Tolerence");
            // paramMarginCallEntry.addAttribute("MtM Agreed Amount", 0);
            npvValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, mcc.getCurrency(), 0, "Dispute init");
            plMark.addPLMarkValue(npvValue);
        } else {
            double mtmAgreedAmount = (poMtM + cptyMtm) / 2.0;
            paramMarginCallEntry.addAttribute("MtM Agreed Amount", mtmAgreedAmount);
            paramMarginCallEntry.addAttribute("MtM Dispute Status", "Dispute resolved automatically");
            npvValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, mcc.getCurrency(),
                    (cptyMtm - poMtM) / 2.0, "Dipsute adjustment");
            plMark.addPLMarkValue(npvValue);
        }
        // create the MARGIN_CALL PlMark from the NPV
        PLMarkValue mcPLMarkValue = CollateralUtilities.calculateMARGIN_CALL(paramDSConnection, tradeDispute, npvValue,
                mcc.getPricingEnvName(), paramMarginCallEntry.getValueDate(), null, new ArrayList<String>());
        if (mcPLMarkValue != null) {
            plMark.addPLMarkValue(mcPLMarkValue);
        }
        // save the pl mark and the trade
        try {
            if (plMark != null) {
                PLMarkSQL.save(plMark);
            }

            // Very bad but no choice at the moment
            // 1- Saving trade performing the workflow so that PLMarks are updated
            // 2- Unfortunately we cannot update update time as it is overriden by Calypso when performimg workflow
            // We have to retrieve the trade, update the update time and save it without performing the workflow

            // 1- perform workflow
            Trade tradeToAmend = (Trade) tradeDispute.clone();
            tradeToAmend.setAction(Action.AMEND);
            TradeServerImpl.saveTrade(tradeToAmend, (Connection) paramObject, new Vector());

            // 2- update time on Trade
            tradeDispute = TradeSQL.getTrade(tradeToAmend.getLongId());
            tradeToAmend = (Trade) tradeDispute.clone();
            tradeToAmend.setUpdatedTime(lastValDateTime);
            TradeSQL.save(tradeToAmend, (Connection) paramObject);

        } catch (Exception e) {
            Log.error(this, e);
            paramList.add("Unable to save pl mark on the dispute adjustment trade");
            return false;
        }
        return true;
    }

    /**
     * This method should be moved outside the DataServer, when it will be possible (create an engine or when it will be
     * possible to add custom code in the margin call manager)
     *
     * @param entry        entry for which a dispute trade will be created
     * @param mcc          Margin call contract to dispute
     * @param tradeDispute
     * @param dsCon
     * @return a CollateralExposure trade of type DISPUTE_ADJUSTMENT
     * @throws RemoteException
     */
    private Trade createOrUpdateDipsuteExposureTrade(MarginCallEntry entry, CollateralConfig mcc, Trade tradeDispute,
                                                     DSConnection dsCon) throws Exception {
        CollateralExposure product = null;
        Trade trade = null;
        if (tradeDispute == null) {
            trade = new Trade();
            product = new CollateralExposure();
            trade.setAction(Action.NEW);
        } else {
            trade = (Trade) tradeDispute.clone();
            trade.setAction(Action.AMEND);
            product = (CollateralExposure) tradeDispute.getProduct();
        }
        trade.setProduct(product);
        // set trade properties
        trade.setTraderName(dsCon.getUser());
        trade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
        trade.setTradeCurrency(mcc.getCurrency());
        trade.setSettleCurrency(mcc.getCurrency());
        trade.setBook(mcc.getBook());
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, mcc.getId());
        trade.addKeyword(TradeInterfaceUtils.TRANS_TRADE_KWD_MTM_DATE, Util.dateToString(entry.getValueDate()));
        // set trade and product dates
        trade.setSettleDate(entry.getValueDate());
        trade.setTradeDate(entry.getValueDatetime());
        product.setEnteredDatetime(new JDatetime());
        product.setStartDate(mcc.getStartingDate().getJDate(TimeZone.getDefault()));
        product.setMaturityDate(entry.getValueDate());
        // set the end date for the product
        product.setEndDate(entry.getValueDate());
        product.setDirection("Buy", trade);
        // set the product properties
        product.setSubType("DISPUTE_ADJUSTMENT");
        product.setUnderlyingType("DISPUTE_ADJUSTMENT");
        product.setCurrency(mcc.getCurrency());
        // link this trade to the entry contract
        product.setMccId(entry.getCollateralConfigId());
        product.addAttribute("CONTRACT_ID", "" + entry.getCollateralConfigId());

        long tradeId = trade.getLongId();
        if (tradeId == 0) {
            tradeId = dsCon.getRemoteTrade().save(trade);
            if (tradeId > 0) {
                trade = dsCon.getRemoteTrade().getTrade(tradeId);
            }
        }

        return trade;
    }
}
