/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.refdata;

import calypsox.tk.core.SantanderUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

@SuppressWarnings("deprecation")
public class SantTransferStaticDataFilter implements StaticDataFilterInterface {


    public static final String AMOUNT_IN_RANGE_OF_EUR = "AmountInRangeOfEUR";

    public static final String CLAIM_ID = "ClaimId";


    @Override
    public boolean fillTreeList(DSConnection con, TreeList tl) {
        Vector<String> nodes = new Vector<>();
        nodes.add("SantTransfer");
        tl.add(nodes, AMOUNT_IN_RANGE_OF_EUR);
        tl.add(nodes, CLAIM_ID);
        return false;
    }


    @SuppressWarnings({"unchecked"})
    @Override
    public void getDomainValues(DSConnection con, Vector v) {
        v.add(AMOUNT_IN_RANGE_OF_EUR);
        v.add(CLAIM_ID);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Vector getTypeDomain(String attributeName) {
        Vector<String> v = new Vector<>();
        if (attributeName.equals(AMOUNT_IN_RANGE_OF_EUR)) {
            v.addElement(StaticDataFilterElement.S_FLOAT_RANGE);
        }
        if(attributeName.equals(CLAIM_ID)){
            v.addElement(StaticDataFilterElement.S_IS_NULL);
            v.addElement(StaticDataFilterElement.S_IS_NOT_NULL);
        }
        return v;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Vector getDomain(DSConnection con, String attributeName) {
        return null;
    }


    @Override
    public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer, BOMessage message,
                           TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount, CashFlow cashflow,
                           HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

        if (transfer == null) {
            if (reportRow == null) {
                return Boolean.FALSE;
            }
            transfer = reportRow.getProperty(ReportRow.TRANSFER);
            if (transfer == null) {
                return Boolean.FALSE;
            }
        }

        if (filterElement.equals(AMOUNT_IN_RANGE_OF_EUR)) {
            return acceptAmountRange(transfer);
        }

        if(filterElement.equals(CLAIM_ID)){
            return acceptClaimId(transfer);
        }

        return Boolean.FALSE;
    }


    @Override
    public boolean isTradeNeeded(String attributeName) {
        return false;
    }


    private Double acceptAmountRange(BOTransfer transfer) {
        Double amount = transfer.getSettlementAmount();
        if (!"EUR".equalsIgnoreCase(transfer.getSettlementCurrency())) {
            try {
                PricingEnv pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("default");
                amount = SantanderUtil.getInstance().convertToEUR(transfer.getSettlementAmount(), transfer.getSettlementCurrency(), JDate.getNow(), pricingEnv);
            } catch(MarketDataException e1){
                Log.error(this.getClass().toString(), "Error : " + e1.toString());
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().toString(), "Error : " + e.toString());
            }
        }
        return amount;
    }

    private String acceptClaimId(BOTransfer transfer){
        return transfer.getAttribute(CLAIM_ID);
    }


}
