package calypsox.tk.refdata;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Vector;

public class SantIsTripartyStaticDataFilter
        implements StaticDataFilterInterface {

    private static final String ATTRIBUTE = "SantIsTriparty";

    @Override
    public boolean fillTreeList(DSConnection paramDSConnection,
                                TreeList paramTreeList) {
        return false;
    }

    @Override
    public void getDomainValues(DSConnection paramDSConnection,
                                Vector paramVector) {
        paramVector.addElement(ATTRIBUTE);
    }

    @Override
    public Vector getTypeDomain(String paramString) {
        final Vector<String> v = new Vector<String>();
        if (paramString.equals(ATTRIBUTE)) {
            v.addElement(StaticDataFilterElement.S_IS);
            return v;
        }
        return v;
    }

    @Override
    public Vector getDomain(DSConnection paramDSConnection,
                            String paramString) {
        return null;
    }

    @Override
    public Object getValue(Trade trade, LegalEntity le, String role,
                           Product product, BOTransfer transfer, BOMessage message,
                           TradeTransferRule rule, ReportRow reportRow, Task task,
                           Account glAccount, CashFlow cashflow,
                           HedgeRelationship relationship, String filterElement,
                           StaticDataFilterElement element) {
        Boolean returnValue = null;

        if (ATTRIBUTE.equals(filterElement)) {
            if (transfer != null) {
                long tradeId = transfer.getTradeLongId();
                try {
                    Trade tradeFromTransfer = DSConnection.getDefault()
                            .getRemoteTrade().getTrade(tradeId);
                    Product rawProduct = tradeFromTransfer.getProduct();
                    if (rawProduct instanceof MarginCall) {
                        MarginCall marginCall = (MarginCall) rawProduct;
                        int contractId = marginCall.getMarginCallId();
                        CollateralConfig colConf = CacheCollateralClient
                                .getCollateralConfig(DSConnection.getDefault(),
                                        contractId);
                        if (colConf != null) {
                            boolean isTriparty = colConf.isTriParty();

                            returnValue = Boolean.valueOf(isTriparty);
                        } else {
                            String errorMessage = String.format(
                                    "Could not retrieve Collateral Config %d from Margin Call %d, trade %d, transfer %d",
                                    contractId, marginCall.getId(),
                                    trade.getLongId(), transfer.getLongId());
                            Log.error(this, errorMessage);
                        }
                    } else {
                        String errorMessage = String.format(
                                "Product %d from trade %d, transfer %d is not of type MarginCall",
                                rawProduct.getId(), tradeFromTransfer.getLongId(),
                                transfer.getLongId());
                        Log.error(this, errorMessage);
                    }
                } catch (CalypsoServiceException e) {
                    String errorMessage = String.format(
                            "Could not retrieve trade %d from transfer %d",
                            tradeId, transfer.getLongId());
                    Log.error(this, errorMessage, e);
                }
            } else {
                String errorMessage = "Transfer is null. This filter attribute is only applicable for transfers.";
                Log.error(this, errorMessage);
            }
        }

        return returnValue;
    }

    @Override
    public boolean isTradeNeeded(String paramString) {
        return false;
    }

}
