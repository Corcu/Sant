package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;

public class BOCreSecLendingMTM_FULL extends SantBOCre {

    private static final String SEC_CODE = "ISIN";
    private Product security;

    public BOCreSecLendingMTM_FULL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurityFromSecLending(this.trade);
    }

    @Override
    protected void fillValues() {
        this.isin = BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.maturityDate = BOCreUtils.getInstance().isOpen(this.trade) ? JDate.valueOf("01/01/3000") : this.trade.getMaturityDate();
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        return null;
    }

    @Override
    protected CollateralConfig getContract() {
        if (null != this.trade && this.trade.getProduct() instanceof SecLending) {
            Integer contractId = ((SecLending) this.trade.getProduct()).getMarginCallContractId(this.trade);
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }

    @Override
    protected Double loadCreAmount() {
        Double amount = 0.0;
        if (this.boCre != null && this.trade != null) {
            if (((SecLending) this.trade.getProduct()).getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(this.trade.getBook().getAccountingBook().getName())) {
                amount = ((SecLending) this.trade.getProduct()).computeNominal(trade, effectiveDate);
            } else {
                amount = this.boCre.getAmount(0);
            }

        }

        return amount;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "";
    }

    @Override
    protected String loadSettlementMethod() {
        return "";
    }


}
