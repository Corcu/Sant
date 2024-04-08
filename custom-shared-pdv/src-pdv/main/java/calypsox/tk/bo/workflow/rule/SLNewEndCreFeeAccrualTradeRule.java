package calypsox.tk.bo.workflow.rule;

import calypsox.tk.pricer.PricerSecLending;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class SLNewEndCreFeeAccrualTradeRule implements WfTradeRule {

    SLReverseCreFeeAccrualTradeRule slReverseCreFeeAccrualTradeRule = new SLReverseCreFeeAccrualTradeRule();

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "NEW FEE_ACCRUAL Cre in case of SecLending EndDate is today";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        if (trade == null) {
            return true;
        }

        SecLending secLending = (SecLending) trade.getProduct();
        JDate endDate = null;
        if (!secLending.getMaturityType().equalsIgnoreCase("OPEN")) {
            endDate = secLending.getEndDate();
        } else if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            endDate = bond.getEndDate();
        }

        if (null != endDate && endDate == JDate.getNow()) {
            CreArray cresNEW = null;

            try {
                String whereClause = slReverseCreFeeAccrualTradeRule.buildWhereClause(trade, BOCre.NEW);
                cresNEW = DSConnection.getDefault().getRemoteBO().getBOCres(null, whereClause, null);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not retrieve CREs from Trade: " + e.toString());
                return false;
            }

            if (cresNEW == null || cresNEW.isEmpty()) {
                return true;
            }

            BOCre cre = Arrays.stream(cresNEW.getCres()).max(Comparator.comparing(s -> s.getEffectiveDate().getJDatetime())).orElse(null);
            BOCre creNew;
            CreArray cres = new CreArray();

            try {
                if (null != cre && cre.getEffectiveDate().getMonth() == endDate.getMonth() && (cre.getEffectiveDate() != (endDate))) {
                    creNew = (BOCre) cre.clone();
                    JDate date = cre.getEffectiveDate().addBusinessDays(1, Util.string2Vector("SYSTEM"));
                    creNew.setId(0L);
                    creNew.setEffectiveDate(date);
                    creNew.setCreationDate(new JDatetime());
                    creNew.setCreType(BOCre.NEW);
                    creNew.setSentDate(null);
                    creNew.setStatus(BOCre.NEW);
                    creNew.setSentStatus(null);
                    creNew.setBookingDate(date);
                    if (isPreviousDayHoliday(trade, endDate)) {
                        double creAmount = priceSecLending(trade);
                        if (creAmount != 0.0) {
                            creNew.setAmount(0, creAmount);
                        }
                    }
                    cres.add(creNew);
                    DSConnection.getDefault().getRemoteBO().saveCres(cres);
                }

            } catch (CloneNotSupportedException e) {
                Log.error(this, "Could not clone CRE: " + e.toString());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not save New CRE: " + e.toString());
            }
            //cres contains only a CRE
            if (!cres.isEmpty()) {
                publishEvents(cres.get(0));
            }
        }
        return true;
    }


    /**
     * Publish event for CreOnlineSenderEngine
     *
     * @param cre
     */
    private void publishEvents(BOCre cre) {
        PSEventCre creEvent = new PSEventCre();
        creEvent.setBoCre(cre);
        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error publish the event: " + e);
        }
    }

    private boolean isPreviousDayHoliday(Trade trade, JDate endDate) {
        JDate previousDay = endDate.addDays(-1);
        if (((SecLending) trade.getProduct()).getMaturityType().equalsIgnoreCase("TERM")) {
            return !CollateralUtilities.isBusinessDay(previousDay, Util.string2Vector("SYSTEM"));
        }
        return false;
    }

    private double priceSecLending(Trade trade) {
        PricerSecLending pricerSecLending = new PricerSecLending();
        PricerMeasure pm = null;
        try {
            List<String> kws = removeAndKeepKeywords(trade);
            pm = pricerSecLending.price(trade, new JDatetime(), PricingEnv.loadPE("OFFICIAL_ACCOUNTING", new JDatetime()), 35);
            addKeywords(trade, kws);
        } catch (PricerException e) {
            Log.error("Can not do price on trade: " + trade.getLongId(), e);
        }
        return Optional.ofNullable(pm.getValue()).orElse(0.0);
    }

    private List<String> removeAndKeepKeywords(Trade trade) {
        List<String> saveValues = Arrays.asList(trade.getKeywordValue("TerminationDate"), trade.getKeywordValue("TerminationTradeDate"), trade.getKeywordValue("TerminationPayIntFlow"));
        trade.removeKeyword("TerminationDate");
        trade.removeKeyword("TerminationTradeDate");
        trade.removeKeyword("TerminationPayIntFlow");
        return saveValues;
    }

    private void addKeywords(Trade trade, List<String> savedValues) {
        trade.addKeyword("TerminationDate", savedValues.get(0));
        trade.addKeyword("TerminationTradeDate", savedValues.get(1));
        trade.addKeyword("TerminationPayIntFlow", savedValues.get(2));
    }
}
