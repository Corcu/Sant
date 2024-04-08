package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreBondFWD_CASH_FIXING extends SantBOCre {

    private Product security;
    public static final String FWD_CASH_FIXING = "FWD_CASH_FIXING";
    public static final String FWD_CASH_FIXING_REAL = "FWD_CASH_FIXING_REAL";

    public BOCreBondFWD_CASH_FIXING(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromBond(this.trade);
    }

    public void fillValues() {
        if (FWD_CASH_FIXING.equalsIgnoreCase(this.boCre.getEventType())){
            this.aliasIdentifier = "RF_FWD_CASH_FIXING";
        } else if (FWD_CASH_FIXING_REAL.equalsIgnoreCase(this.boCre.getEventType())){
            this.aliasIdentifier = "RF_FWD_CASH_FIXING_REAL";
        }

        if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
            this.productType = "BondForward";
        }
        this.fixingDate = formatJDate(BOCreUtils.getInstance().loadFixingDate(this.trade));
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
        return "INTRADAY";
    }

    private JDate formatJDate (String date){
        JDate out = new JDate();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            out = JDate.valueOf(format.parse(date));
        } catch (ParseException e) {
            Log.error(this, "Could not save the fee for trade " + trade.getLongId());
        }
        return out;
    }

}
