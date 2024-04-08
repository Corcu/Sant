package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreSecLendingPRINCIPAL extends BOCreMarginCallCST {

    public BOCreSecLendingPRINCIPAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        this.creDescription = "PRINCIPAL";
        this.settlementMethod = getInstance().getSettleMethod(this.creBoTransfer);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.transferAccount = getInstance().getTransferAccount(this.settlementMethod,this.creBoTransfer);
    }

    public CollateralConfig getContract() {
        if(null!=this.trade && this.trade.getProduct() instanceof SecLending){
            Integer contractId = ((SecLending) this.trade.getProduct()).getMarginCallContractId(this.trade);
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }

    @Override
    protected String loadAccountCurrency() {
        return null!=this.account ? this.account.getCurrency() : "";
    }

    @Override
    public JDate getCancelationDate() {
        if(("CANCELED_SEC_PAYMENT".equalsIgnoreCase(this.boCre.getOriginalEventType()) ||
                "CANCELED_SEC_RECEIPT".equalsIgnoreCase(this.boCre.getOriginalEventType()))
                && getInstance().isCanceledEvent(this.trade)){
            return getInstance().getActualDate();
        }
        return null;
    }

    @Override
    public Double getCashPosition() {
        final BOCre cRfromTrade = getInstance().getCRfromTrade(this.trade);
        return getInstance().getAccountBalancefromCre(cRfromTrade);
    }

    @Override
    protected String loadProductType() {
        return "SecLending";
    }

    @Override
    protected String getSubType(){
      return this.creBoTransfer.getTransferType();
    }

    @Override
    protected String getDebitCredit(double value) {
        return BOCreUtils.getInstance().getDebitCredit(value);
    }

}
