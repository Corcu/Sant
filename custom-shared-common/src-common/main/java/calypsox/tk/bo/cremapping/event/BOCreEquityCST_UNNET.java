package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreEquityCST_UNNET extends SantBOCre{

    private Product security;

    public BOCreEquityCST_UNNET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromEquity(this.trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Cash Settlement Unnet";
        this.transferAccount = getInstance().getTransferAccountEquity(this.settlementMethod,this.creBoTransfer);
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : "";
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettedTransferLongId() : 0;
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.ownIssuance = BOCreUtils.getInstance().isOwnIssuance(this.trade);
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.productCurrency = BOCreUtils.getInstance().loadProductCurrency(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        BOTransfer creBoTransferNet = getTransferNet(this.boCre);
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(creBoTransferNet).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && null!=originalEventType && originalEventType.contains("SETTLED")){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_SEC_PAYMENT,CANCELED_SEC_RECEIPT,CANCELED_RECEIPT,CANCELED_PAYMENT");

        if(listevents.contains(originalEventType) && null!=this.trade){
            return JDate.getNow();
        }

        return null;
    }

    public CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    protected String getSubType(){
        if(null!=this.trade){
            final Equity product = (Equity) this.trade.getProduct();
            return product.getSecCode("EQUITY_TYPE");
        }
        return "";
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod(){
        return getInstance().getSettleMethod(this.creBoTransfer);
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeEquity();
    }

    public BOTransfer getTransferNet(BOCre boCre) {
        if (null != boCre && boCre.getNettedTransferLongId() > 0) {
            try {
                return DSConnection.getDefault().getRemoteBO().getBOTransfer(boCre.getNettedTransferLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading transfer for CRE: " + boCre.getId());
            }
        }
        return null;
    }

}
