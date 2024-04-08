package calypsox.tk.util;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ScheduledTaskEOD_TRADE_VALUATION;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.ValuationUtil;

import java.rmi.RemoteException;
import java.util.*;

public class ScheduledTaskMEX_EOD_TRADE_VALUATION_PLMARK extends ScheduledTaskEOD_TRADE_VALUATION {
    public static final String PRICING_ENV_PLMARK_PARAM = "PricingEnv for PLMarks";
    private final String COLLATERAL_EXCLUDE_KWD="CollateralExclude";
    private final String PM_FOR_VALUATION = PricerMeasure.S_MARGIN_CALL;

    @Override
    public String getTaskInformation() {
        return "Generate only PLMarks for selected trades.";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(PRICING_ENV_PLMARK_PARAM));

        return attributeList;
    }
    @Override
    public Vector getDomainAttributes() {
        Vector v = new Vector();
        v.addElement(PRICING_ENV_PLMARK_PARAM);
        v.addAll(super.getDomainAttributes());
        return v;
    }

    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);

        if (Util.isEmpty(getAttribute(PRICING_ENV_PLMARK_PARAM))) {
            messages.addElement("Must select " + PRICING_ENV_PLMARK_PARAM);
            ret = false;
        }

        return ret;
    }

    @Override
    protected boolean publishTradeEvents(ValuationUtil valuationUtil, DSConnection ds, PSConnection ps, Task task,
                                         TaskArray tasks, JDatetime valDateTime, JDatetime undoDatetime) {
        //boolean ret = this.createAndPublishEvents(valuationUtil, ds, ps, task, tasks, valDateTime, undoDatetime);
        boolean ret = true;
        JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault());

        List<PLMark> plMarks = new ArrayList<>();
        String pricingEnv = getAttribute(PRICING_ENV_PLMARK_PARAM);
        if (Util.isEmpty(pricingEnv)) {
            pricingEnv = "DirtyPrice";
        }
        HashMap<String, Double> fxRates = new HashMap<String, Double>();
        Vector<Trade> trades = valuationUtil.getTrades();
        for (Trade trade : trades) {
            if (!isInternal(trade)&&isCollateralizable(trade)) {
                PricerMeasure[] pms = valuationUtil.getTradeMeasures(trade);
                if (pms == null) {
                    Log.warn(this, "No Pricer Measure for Trade, ignoring Trade " + trade.getLongId());
                    continue;
                }
                if (pms != null && pms.length != 1) {
                    Log.warn(this, "More than one Pricer Measure, will only use " + PM_FOR_VALUATION + " to create PL Marks.");
                }
                boolean wantedPMFound = false;
                for (PricerMeasure pm : pms) {
                    if (pm.getName().equals(PM_FOR_VALUATION)) {
                        wantedPMFound = true;
                    }
                }
                if (!wantedPMFound) {
                    Log.warn(this, PM_FOR_VALUATION + " Pricer Measure not found, ignoring Trade. " + trade.getLongId());
                    continue;
                }

                PLMark plMark = null;
                try {
                    plMark = CollateralUtilities.createPLMarkIfNotExists(
                            trade, DSConnection.getDefault(),
                            pricingEnv, valueDate);
                } catch (RemoteException e) {
                    Log.error(this, "Error retrieving PLMark : " + e.toString());
                    ret = false;
                    continue;
                }

                if (plMark == null) {
                    Log.error(this, "Could not retrieve/create PLMark for Trade " + trade.getLongId());
                    ret = false;
                    continue;
                }

                ArrayList<String> errorMsgs = new ArrayList<String>();
                CollateralConfig marginCallConfig = null;
                try {
                    int mccId = trade.getKeywordAsInt("MARGIN_CALL_CONFIG_ID");
                    if (mccId > 0) {
                        marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
                    } else {
                        marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
                    }
                } catch (RemoteException e) {
                    Log.error(this, "Could not retrieve Margin Call Config for Trade " + trade.getLongId() + " - " + e.toString());
                }

                if (marginCallConfig == null) {
                    Log.error(this, "No Margin Call Config found. Ignoring Trade " + trade.getLongId());
                    continue;
                }

                PLMarkValue plMarkValue = null;
                for (PricerMeasure pm : pms) {
                    if (!pm.getName().equals(PM_FOR_VALUATION)) {
                        continue;
                    }

                    // NPV PL Mark is a copy of Pricer Measure calculated
                    plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, pm.getCurrency(), pm.getValue(), "");
                    plMark.addPLMarkValue(plMarkValue);

                    // MARGIN_CALL is calculated via PM
                    plMarkValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_MARGIN_CALL, pm.getCurrency(), pm.getValue(), "");
                    plMark.addPLMarkValue(plMarkValue);

                    // Conversion for NPV_BASE
                    double baseMtmValue = pm.getValue();
                    String baseMtMCcy = pm.getCurrency();
                    if (marginCallConfig != null && !marginCallConfig.getCurrency().equals(baseMtMCcy)) {
                        JDate fxdate = valueDate;

                        String fxRateKey = fxdate.toString() + baseMtMCcy + marginCallConfig.getCurrency();
                        Double fxRate = 0.0;
                        if (!fxRates.containsKey(fxRateKey)) {
                            fxRate = CollateralUtilities.getFXRate(fxdate, baseMtMCcy, marginCallConfig.getCurrency());
                            fxRates.put(fxRateKey, fxRate);
                        } else {
                            fxRate = fxRates.get(fxRateKey);
                        }

                        if (fxRate > 0.0) {
                            baseMtMCcy = marginCallConfig.getCurrency();
                            baseMtmValue = baseMtmValue * fxRate;
                        }
                    }
                    plMarkValue = CollateralUtilities.buildPLMarkValue(SantPricerMeasure.S_NPV_BASE, baseMtMCcy, baseMtmValue, "");
                    plMark.addPLMarkValue(plMarkValue);
                }

                plMarks.add(plMark);
            }
        }
        if (plMarks.size() > 0) {
            try {
                CollateralUtilities.savePLMarks(plMarks);
            } catch (InterruptedException e) {
                Log.error(this, "Error : " + e.toString());
                ret = false;
            }
        }
        return ret;
    }

    private boolean isInternal(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }

    private boolean isCollateralizable(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue(COLLATERAL_EXCLUDE_KWD))
                .map(value->!Boolean.parseBoolean(value)).orElse(true);
    }

}
