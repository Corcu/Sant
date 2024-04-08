package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;
import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

/**
 * Use for generate CustomerTransferCST message
 */
public class BOCreCustomerTransferCST extends SantBOCre{

    public BOCreCustomerTransferCST(BOCre cre, Trade trade) {
        super(cre,trade);
    }

    public void fillValues() {
        this.creDescription = "Cash Settlement";
        this.settlementMethod = getInstance().getSettleMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccount(this.settlementMethod,this.creBoTransfer);
        this.tradeId = this.trade.getKeywordAsLongId("INTEREST_TRANSFER_FROM");
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : "";
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getLongId() : 0;
    }

    /**
     * Need Override, Load collateralConfig from Account
     */
    @Override
    protected void init() {
        this.book = BOCache.getBook(DSConnection.getDefault(),this.boCre.getBookId());
        this.clientBoTransfer = getClientBoTransfer();
        this.creBoTransfer = getCreBoTransfer();
        this.account = getAccount();
        this.collateralConfig = getContract();
    }

    @Override
    public JDate getCancelationDate() {
        return getInstance().isCanceledTransfer(this.boCre)
                && getInstance().isCanceledEvent(this.trade) ? getInstance().getActualDate() : null;
    }

    @Override
    protected Double getPosition() {
        return getCashPosition();
    }

    @Override
    protected String getSubType() {
        if(this.clientBoTransfer !=null){
            return this.clientBoTransfer.getTransferType();
        }
        return "";
    }

    public Double getCashPosition() {
        JDate valDate = null;
        final Trade ibFromCT = getInstance().getIBFromCT(this.trade);
        if(null!=ibFromCT && ibFromCT.getProduct() instanceof InterestBearing ){
            valDate = ((InterestBearing)ibFromCT.getProduct()).getEndDate();
            return getInstance().getInvLastCashPosition(this.collateralConfig, this.trade, BOCreConstantes.DATE_TYPE_SETTLE,BOCreConstantes.THEORETICAL,valDate);
        }else{
            Log.error("Error loading InterestBearing from CustomerTransfer: " + this.trade.getLongId());
        }
        return 0.0;
    }

    public CollateralConfig getContract() {
        return getInstance().getContract(this.account);
    }

    protected Account getAccount() {
        return getInstance().getAccount(this.clientBoTransfer);
    }

}
