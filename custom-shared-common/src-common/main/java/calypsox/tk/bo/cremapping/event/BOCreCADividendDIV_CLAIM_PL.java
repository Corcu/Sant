package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCADividendDIV_CLAIM_PL extends BOCreCADividendCST{

    private Equity security;

    public BOCreCADividendDIV_CLAIM_PL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = (Equity)BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    public void fillValues() {
        this.creDescription = this.boCre.getDescription();
        this.counterparty = null!=trade ? this.trade.getCounterParty().getExternalRef() : "";
        this.mcContractID = BOCreUtils.getInstance().loadContractID(this.trade);
        if (getContract() != null){
            this.mcContractType = getContract().getContractType();
        }
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.partenonId = BOCreUtils.getInstance().getPartenonCA(this.trade);
        this.ownIssuance = isOwnIssuance();
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestadoCA(this.trade);
        this.productID = BOCreUtils.getInstance().loadProductID(this.security);
        this.issuerName = this.security.getIssuer().getExternalRef();
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.claimProductType = BOCreUtils.getInstance().loadClaimProductType(this.trade);
        this.role = this.trade.getRole();
        this.issuerShortName = this.security.getIssuer().getCode();
        this.caReference = BOCreUtils.getInstance().getCAReference(this.trade);

        //DIVIDENDOS
        if(("CounterParty").equals(this.role)){
            this.negoPercentage = this.trade.getKeywordValue("ContractDivRate") != null ? Double.valueOf(this.trade.getKeywordValue("ContractDivRate")) : 1;
            this.negoAmount = this.trade.getQuantity() * this.trade.getTradePrice() * this.negoPercentage;
            this.diffGrossNegoAmount = this.negoAmount - (this.trade.getNegociatedPrice() * this.trade.getQuantity());
            if(this.diffGrossNegoAmount<0){
                this.diffGrossNegoDirection = "-";
            }else{
                this.diffGrossNegoDirection = "+";

            }
        }
    }

    @Override
    public CollateralConfig getContract() {
        final int contractId = this.trade != null ? trade.getKeywordAsInt("CASource") : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
    }

    @Override
    protected JDate getCancelationDate() {
        if(null != this.boCre &&
                (BOCreConstantes.CANCELED_TRADE_EVENT.equalsIgnoreCase(this.boCre.getOriginalEventType()))){
            return getInstance().getActualDate();
        } else return null;
    }

    @Override
    protected Double loadCreAmount(){
        return null!=this.boCre ? this.boCre.getAmount(0) : 0.0D;
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadProductType() {
        return null!=this.trade ? this.trade.getProductType() : "";
    }

    @Override
    protected String getSubType() {
        return null!=security ? this.security.getSecCode("EQUITY_TYPE") : "";
    }

    @Override
    protected String loadProccesingOrg(){ return null!=trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";    }

    @Override
    protected String loadEndOfMonth() {
        return "";
    }

    @Override
    protected String loadCounterParty() {
        return null!=trade ? this.trade.getCounterParty().getExternalRef() : "";
    }

    public String isOwnIssuance() {
        String issuerName = "";
        if (this.security.getIssuer() != null) {
            issuerName = this.security.getIssuer().getCode() != null ? this.security.getIssuer().getCode() : "";
        }
        return BOCreConstantes.BSTE.equals(issuerName) ? "SI" : "NO";
    }

}
