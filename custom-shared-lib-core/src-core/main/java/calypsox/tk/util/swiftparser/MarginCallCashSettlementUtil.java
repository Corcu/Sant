package calypsox.tk.util.swiftparser;

import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.sql.CollateralConfigFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.swiftparser.cashstatement.CashSettlementConfirmationHandler;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

public class MarginCallCashSettlementUtil {

    protected CashSettlementConfirmationHandler handler;
    protected BOMessage message;
    protected SwiftMessage swiftMessage;

    public MarginCallCashSettlementUtil(CashSettlementConfirmationHandler handler, BOMessage message,
                                        SwiftMessage swiftMessage) {

        this.handler = handler;
        this.message = message;
        this.swiftMessage = swiftMessage;
    }

    /**
     * @param handler
     * @param message
     * @param swiftMessage
     * @return MarginCall cash trade to be saved after processing the MT900
     */
    public Trade buildMarginCallCashTrade() {

        final Trade trade = handler.getExternalPositionUpdateTrade(message, swiftMessage);
        SimpleTransfer simpleTransfer = (SimpleTransfer) trade.getProduct();

        final MarginCall marginCall = new MarginCall();
        CollateralConfig contract = getCollateralConfig(trade); //TBD

        marginCall.setMarginCallId(contract.getId());
        marginCall.setLinkedLongId(contract.getId());

        marginCall.setPrincipal(simpleTransfer.getPrincipal());
        marginCall.setCurrencyCash(simpleTransfer.getCurrencyCash());

        marginCall.setOrdererLeId(trade.getBook().getLegalEntity().getId());
        marginCall.setOrdererRole("Client"); //("ProcessingOrg");

        marginCall.setMaturityDate(trade.getMaturityDate());
        marginCall.setFlowType("COLLATERAL");

        trade.setProduct(marginCall);

        return trade;
    }

    //test
    private CollateralConfig getCollateralConfig(final Trade trade) {

        DSConnection ds = DSConnection.getDefault();

        ArrayList<Integer> eligibleMarginCallConfigs;
        try {
            eligibleMarginCallConfigs = SantReportingUtil.getSantReportingService(ds)
                    .getMarginCallConfigsFromTrade(trade);

            if (!Util.isEmpty(eligibleMarginCallConfigs)) {
                for (Integer mccID : eligibleMarginCallConfigs) {
                    CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(ds, mccID);
                    return mcc;
                }
            }

        } catch (RemoteException e) {
            Log.error(this, e); //sonar
        }

        return null;
    }

    //test
    private CollateralConfig getMarginCallConfig3(CashSettlementConfirmationHandler handler, SwiftMessage swiftMessage) throws RemoteException {

        Account ac = handler.getAccount(swiftMessage);
        int poID = ac.getLegalEntityId();

        CollateralConfigFilter filter = new CollateralConfigFilter();
        filter.addPoId(poID);

        LegalEntityAttribute lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poID, poID, "ProcessingOrg", "DEFAULT_BOOK");
        //filter.addLeId(entityId);
        return null;
    }


    //test
    private CollateralConfig getMarginCallConfig2(int leid) throws RemoteException {

        CollateralConfig contract = null;
        CollateralServiceRegistry collateralService = ServiceRegistry.getDefault();

        //Margin Call Contract.
        Vector<CollateralConfig> marginCallVector = new Vector<CollateralConfig>();
        marginCallVector.addAll(collateralService.getCollateralDataServer().getAllMarginCallConfig(0, leid));

        for (int numMarginCall = 0; numMarginCall < marginCallVector.size(); numMarginCall++) {
            CollateralConfig mrgCallConfig = marginCallVector.get(numMarginCall);
            //We check if the value passed in the Excel file is equals to the name for the contract.
            if (null != mrgCallConfig && mrgCallConfig.getName().equals(contract)) {
                contract = mrgCallConfig;
                break;
            }
        }

        return contract;
    }


}
