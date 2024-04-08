package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreBondMTM_NET extends SantBOCre {

    private Product security;

    public BOCreBondMTM_NET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromBond(this.trade);
    }

    public void fillValues() {
        this.aliasIdentifier = "RF_MTM_NET";
        if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
            this.productType = "BondForward";
        }
        if (BOCreUtils.getInstance().isBFWDelivery(this.trade)){
            this.forwardDate = BOCreUtils.getInstance().calculateforwardDate(this.trade, this.security);
        }
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade, this.boCre);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.deliveryType = BOCreUtils.getInstance().loadDeliveryType(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.productCurrency = BOCreUtils.getInstance().loadProductBondCurrency(this.trade);
        this.buySell = BOCreUtils.getInstance().loadBuySell(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    public CollateralConfig getContract() {
        final int contractId = this.trade != null ? trade.getKeywordAsInt("CASource") : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
    }

    @Override
    protected String getSubType(){
        if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
            return this.productSubType = trade.getKeywordValue("BondForwardType");
        } else if(null!=this.trade){
            return this.trade.getProductSubType();
        }
        return "";
    }

    @Override
    protected JDate getCancelationDate() {
        if(null != this.boCre &&
                (BOCreConstantes.CANCEL_PAYMENT_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType())
                        ||(BOCreConstantes.CANCELED_RECEIPT_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType())))){
            return getInstance().getActualDate();
        } else return null;
    }

    protected Account getAccount() { return null; }

    @Override
    protected Double getPosition(){
        return 0.0;
    }

    @Override
    protected String loadProductType() {
        return getInstance().getProductTypeBond();
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "EOD";
    }

}
