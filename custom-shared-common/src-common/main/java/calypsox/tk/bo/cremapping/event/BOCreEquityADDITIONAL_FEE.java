package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreEquityADDITIONAL_FEE extends SantBOCre{

    private Product security;

    public BOCreEquityADDITIONAL_FEE(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromEquity(this.trade);
    }

    @Override
    protected void fillValues() {
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.ownIssuance = BOCreUtils.getInstance().isOwnIssuance(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = BOCreUtils.getInstance().loadIssuerName(this.trade);
        this.accountingRule = BOCreUtils.getInstance().loadAccountingRule(this.boCre);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
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

        if(listevents.contains(originalEventType) && null!=this.trade && trade.getStatus().toString().equalsIgnoreCase("CANCELED")){
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
    protected String loadProductType() {
        return getInstance().getProductTypeEquity();
    }

}
