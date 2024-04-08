package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Vector;

/*
 * This Trade workflow rule is only applicable for Exposure Trades with Sub Type SecLending.
 * Creates MarginCall Trades for all the collaterals attached to the trade and fills the trade_id field
 * with the ID of the marginCall trade.
 */
public class SantCreateMCTradesTradeRule implements WfTradeRule {

    public static final String COLLATERAL_IN_CASH = "COLLATERAL_IN_CASH";
    public static final String COLLATERAL_CASH_CCY = "COLLATERAL_CASH_CCY";
    public static final String COLLATERAL_CASH_AMOUNT = "COLLATERAL_CASH_AMOUNT";
    public static final String COLLATERAL_CASH_TRADE_ID = "COLLATERAL_CASH_TRADE_ID";

    public static final String COLLATERAL_IN_SECURITY = "COLLATERAL_IN_SECURITY";
    public static final String COLLATERAL_SEC_CCY = "COLLATERAL_SEC_CCY";
    public static final String COLLATERAL_SEC_AMOUNT = "COLLATERAL_SEC_AMOUNT";
    public static final String COLLATERAL_SEC_ISIN = "COLLATERAL_SEC_ISIN";
    public static final String COLLATERAL_SEC_TRADE_ID = "COLLATERAL_SEC_TRADE_ID";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        if (trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)
                && ((CollateralExposure) trade.getProduct()).getSubType().equals("SECURITY_LENDING")) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        final String desc = "This Trade workflow rule is only applicable for Exposure Trades with Sub Type SecLending. "
                + "Creates MarginCall Trades for all the collaterals attached to the trade";
        return desc;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection ds, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        Log.debug("SantCreateMCTradesTradeRule", "Update - Start");

        CollateralExposure product = (CollateralExposure) trade.getProduct();
        Vector<String> tmpMsgs = new Vector<String>();

        if (!hasPendingCollateral(trade)) {
            return true;
        }

        validateCollateralInfo(trade, tmpMsgs);
        if (tmpMsgs.size() > 0) {
            messages.addAll(tmpMsgs);
            return false;
        }

        Trade securityMarginCallTrade = null;
        Trade cashMarginCallTrade = null;

        if ((Boolean) product.getAttribute(COLLATERAL_IN_SECURITY)) {
            securityMarginCallTrade = buildSecurityMarginCallTrade(trade, tmpMsgs);
            if (tmpMsgs.size() > 0) {
                messages.addAll(tmpMsgs);
                return false;
            }
        }

        if ((Boolean) product.getAttribute(COLLATERAL_IN_CASH)) {
            cashMarginCallTrade = buildCashMarginCallTrade(trade, tmpMsgs);
            if (tmpMsgs.size() > 0) {
                messages.addAll(tmpMsgs);
                return false;
            }
        }

        try {
            if (cashMarginCallTrade != null) {
                long cashMarginCallTradeId = ds.getRemoteTrade().save(cashMarginCallTrade);
                product.addAttribute(COLLATERAL_CASH_TRADE_ID, cashMarginCallTradeId + "");
            }

            if (securityMarginCallTrade != null) {
                long securityMarginCallTradeId = ds.getRemoteTrade().save(securityMarginCallTrade);
                product.addAttribute(COLLATERAL_SEC_TRADE_ID, securityMarginCallTradeId + "");
            }

        } catch (RemoteException e) {
            Log.error(this, e); //sonar
        }

        Log.debug("SantCreateMCTradesTradeRule", "Update - End");

        return true;
    }

    private boolean hasPendingCollateral(Trade expTrade) {
        CollateralExposure expProduct = (CollateralExposure) expTrade.getProduct();
        if (((Boolean) expProduct.getAttribute(COLLATERAL_IN_CASH) && Util.isEmpty((String) expProduct
                .getAttribute(COLLATERAL_CASH_TRADE_ID)))
                || ((Boolean) expProduct.getAttribute(COLLATERAL_IN_SECURITY) && Util.isEmpty((String) expProduct
                .getAttribute(COLLATERAL_SEC_TRADE_ID)))) {
            return true;
        }

        return false;
    }

    private void validateCollateralInfo(Trade expTrade, Vector<String> messages) {
        if (Util.isEmpty(expTrade.getInternalReference())) {
            messages.add("Trade has not contract linked.");
        } else {
            try {
                Integer.parseInt(expTrade.getInternalReference());
            } catch (Exception exc) {
                Log.error(this, exc); //sonar
                messages.add("Trade has invalid contract number " + expTrade.getInternalReference() + " linked to it.");
            }
        }

        CollateralExposure expProduct = (CollateralExposure) expTrade.getProduct();

        if ((Boolean) expProduct.getAttribute(COLLATERAL_IN_CASH)) {
            if ((expProduct.getAttribute(COLLATERAL_CASH_AMOUNT) == null)
                    || !(expProduct.getAttribute(COLLATERAL_CASH_AMOUNT) instanceof Amount)) {
                messages.add("COLLATERAL_CASH_AMOUNT is invalid");
            }

            if (Util.isEmpty((String) expProduct.getAttribute(COLLATERAL_CASH_CCY))) {
                messages.add("COLLATERAL_CASH_CCY is mandatory");
            }
        }

        if ((Boolean) expProduct.getAttribute(COLLATERAL_IN_SECURITY)) {
            // ISIN
            if (Util.isEmpty((String) expProduct.getAttribute(COLLATERAL_SEC_ISIN))) {
                messages.add("ISIN is mandatory");
            } else {

                String isin = (String) expProduct.getAttribute(COLLATERAL_SEC_ISIN);
                try {
                    Product productByCode = DSConnection.getDefault().getRemoteProduct().getProductByCode("ISIN", isin);
                    if (productByCode == null) {
                        messages.add("No security found with ISIN " + isin);
                    }
                } catch (RemoteException e) {
                    Log.error(this, e); //sonar
                    messages.add(e.getMessage());
                }
            }

            if ((expProduct.getAttribute(COLLATERAL_SEC_AMOUNT) == null)
                    || !(expProduct.getAttribute(COLLATERAL_SEC_AMOUNT) instanceof Amount)) {
                messages.add("COLLATERAL_SEC_AMOUNT is invalid");
            }

            if (Util.isEmpty((String) expProduct.getAttribute(COLLATERAL_SEC_CCY))) {
                messages.add("COLLATERAL_SEC_CCY is mandatory");
            }

        }

    }

    public Trade buildCashMarginCallTrade(Trade expTrade, Vector<String> messages) {

        CollateralExposure expProduct = (CollateralExposure) expTrade.getProduct();

        Trade marginCallTrade = new Trade();
        marginCallTrade.setAction(Action.NEW);
        marginCallTrade.setTraderName("NONE");
        marginCallTrade.setSalesPerson("NONE");
        marginCallTrade.setBook(getContractBook(Integer.parseInt(expTrade.getInternalReference())));
        marginCallTrade.setCounterParty(expTrade.getCounterParty());
        marginCallTrade.setTradeDate(expTrade.getTradeDate());
        marginCallTrade.setSettleDate(expProduct.getStartDate());

        MarginCall marginCall = new MarginCall();
        marginCall.setFlowType("COLLATERAL");
        marginCall.setSecurity(null);
        marginCallTrade.setProduct(marginCall);
        marginCall.setMarginCallId(Integer.parseInt(expTrade.getInternalReference()));
        double principal = ((Amount) expProduct.getAttribute(COLLATERAL_CASH_AMOUNT)).get();
        if (principal < 0) {
            marginCallTrade.setQuantity(-1.);
        } else {
            marginCallTrade.setQuantity(1.);
        }
        marginCall.setPrincipal(principal);

        marginCall.setCurrencyCash((String) expProduct.getAttribute(COLLATERAL_CASH_CCY));

        return marginCallTrade;
    }

    private Trade buildSecurityMarginCallTrade(Trade expTrade, Vector<String> messages) {
        CollateralExposure expProduct = (CollateralExposure) expTrade.getProduct();

        String isin = (String) expProduct.getAttribute(COLLATERAL_SEC_ISIN);
        Product productByCode;
        try {
            productByCode = DSConnection.getDefault().getRemoteProduct().getProductByCode("ISIN", isin);
        } catch (RemoteException e) {
            messages.add(e.getMessage());
            Log.error(this, e);//Sonar
            return null;
        }

        if (productByCode == null) {
            messages.add("Security not found with ISIN=" + isin);
            return null;
        }

        Trade marginCallTrade = new Trade();
        marginCallTrade.setAction(Action.NEW);
        marginCallTrade.setTraderName("NONE");
        marginCallTrade.setSalesPerson("NONE");
        marginCallTrade.setBook(getContractBook(Integer.parseInt(expTrade.getInternalReference())));
        marginCallTrade.setCounterParty(expTrade.getCounterParty());
        marginCallTrade.setTradeDate(expTrade.getTradeDate());
        marginCallTrade.setSettleDate(expProduct.getStartDate());

        MarginCall marginCall = new MarginCall();
        marginCall.setFlowType("SECURITY");
        marginCallTrade.setProduct(marginCall);
        marginCall.setMarginCallId(Integer.parseInt(expTrade.getInternalReference()));
        marginCall.setSecurity(productByCode);

        double secAmount = ((Amount) expProduct.getAttribute(COLLATERAL_SEC_AMOUNT)).get();

        marginCall.setCurrencyCash((String) expProduct.getAttribute(COLLATERAL_SEC_CCY));
        if (productByCode instanceof Bond) {
            Bond bond = (Bond) productByCode;
            marginCallTrade.setQuantity(secAmount / bond.getFaceValue());
            marginCall.setPrincipal(bond.getFaceValue());
        } else if (productByCode instanceof Equity) {
            Equity equity = (Equity) productByCode;
            marginCallTrade.setQuantity(secAmount / equity.getPrincipal());
            marginCall.setPrincipal(equity.getPrincipal());
        }

        return marginCallTrade;
    }

    private Book getContractBook(int contractId) {
        CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                contractId);
        return collateralConfig.getBook();

    }
}
