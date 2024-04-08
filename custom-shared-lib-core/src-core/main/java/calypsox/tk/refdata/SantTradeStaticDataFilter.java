/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.refdata;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

@SuppressWarnings("deprecation")
public class SantTradeStaticDataFilter implements StaticDataFilterInterface {

    public static final String HAS_POSITIVE_NPV = "HasPositiveNPV";
    public static final String HAS_NEGATIVE_NPV = "HasNegativeNPV";
    public static final String MATURITY_TYPE="Maturity Type";
    public static final String DUAL_CURRENCY = "Dual Currency";


    @Override
    public boolean fillTreeList(DSConnection con, TreeList tl) {
        Vector<String> nodes = new Vector<>();
        nodes.add("SantTrade");
        tl.add(nodes, HAS_POSITIVE_NPV);
        tl.add(nodes, HAS_NEGATIVE_NPV);
        tl.add(nodes, MATURITY_TYPE);
        tl.add(nodes, DUAL_CURRENCY);
        return false;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void getDomainValues(DSConnection con, Vector v) {
        v.add(HAS_POSITIVE_NPV);
        v.add(HAS_NEGATIVE_NPV);
        v.add(MATURITY_TYPE);
        v.add(DUAL_CURRENCY);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Vector getTypeDomain(String attributeName) {
        Vector<String> v = new Vector<String>();
        if (attributeName.equals(HAS_POSITIVE_NPV)) {
            v.addElement(StaticDataFilterElement.S_IS);
        } else if (attributeName.equals(HAS_NEGATIVE_NPV)) {
            v.addElement(StaticDataFilterElement.S_IS);
        }else if (attributeName.equals(MATURITY_TYPE)) {
            v.addElement(StaticDataFilterElement.S_LIKE);
        } else if (attributeName.equals(DUAL_CURRENCY)) {
            v.addElement(StaticDataFilterElement.S_IS);
        }
        return v;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Vector getDomain(DSConnection con, String attributeName) {
        return null;
    }

    @Override
    public Object getValue(Trade trade, LegalEntity le, String role,
                           Product product, BOTransfer transfer, BOMessage message,
                           TradeTransferRule rule, ReportRow reportRow, Task task,
                           Account glAccount, CashFlow cashflow,
                           HedgeRelationship relationship, String filterElement,
                           StaticDataFilterElement element) {

        if (trade == null) {
            if (reportRow == null) {
                return Boolean.FALSE;
            }
            trade = reportRow.getProperty(ReportRow.TRADE);
            if (trade == null) {
                return Boolean.FALSE;
            }
        }

        if(filterElement.equals(HAS_POSITIVE_NPV)||filterElement.equals(HAS_NEGATIVE_NPV)){
            return acceptNPVCriterion(trade, filterElement);
        }else if(filterElement.equals(MATURITY_TYPE)&&trade.getProduct() instanceof SecFinance){
            return ((SecFinance)trade.getProduct()).getMaturityType();
        } else if(filterElement.equals(DUAL_CURRENCY)){
            return !trade.getTradeCurrency().equalsIgnoreCase(trade.getSettleCurrency());
        }

        return Boolean.FALSE;
    }


    @Override
    public boolean isTradeNeeded(String attributeName) {
        return true;
    }

    private boolean acceptNPVCriterion(Trade trade,String filterElement){
        JDate mtmDate = CollateralUtilities.getMTMDateFromTradeKeyword(trade);
        if (mtmDate == null) {
            return Boolean.FALSE;
        }

        List<PLMark> plMarks = null;
        PLMark plMark;
        try {
            // MIGRATION V14.4 19/05/2015
            List<Long> idList = new ArrayList<>();
            idList.add(trade.getLongId());
            //AAP Mig 14.4
            plMarks = new ArrayList<>(CollateralUtilities.retrievePLMarkBothTypes(idList, "DirtyPrice", mtmDate));
        } catch (PersistenceException e) {
            Log.error(this,
                    "Error getting PLMark for trade id = " + trade.getLongId(), e);
        }
        if (plMarks == null || plMarks.size() != 1) {
            return Boolean.FALSE;
        }
        plMark = plMarks.iterator().next();
        PLMarkValue plMarkValue = CollateralUtilities.retrievePLMarkValue(
                plMark, SantPricerMeasure.S_NPV);
        if (plMarkValue == null) {
            return Boolean.FALSE;
        }

        double value = plMarkValue.getMarkValue();

        // In case value = 0.0000012 - we check only up to 2 digits
        String valueStr = Util.numberToString(value, 2, Locale.ENGLISH, false);
        value = Double.parseDouble(valueStr);

        if ((value > 0) && filterElement.equals(HAS_POSITIVE_NPV)) {
            return Boolean.TRUE;
        }
        if ((value < 0) && filterElement.equals(HAS_NEGATIVE_NPV)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
