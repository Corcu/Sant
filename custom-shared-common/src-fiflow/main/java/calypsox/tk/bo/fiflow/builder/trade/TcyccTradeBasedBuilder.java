package calypsox.tk.bo.fiflow.builder.trade;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTradeSecurityHandler;
import calypsox.tk.bo.fiflow.staticdata.FIFlowStaticData;
import com.calypso.tk.core.AccountingBook;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.SecFinance;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class TcyccTradeBasedBuilder {

    Trade trade;
    JDate valDate;

    private final FIFlowTradeSecurityHandler securityWrapper=new FIFlowTradeSecurityHandler();
    private final String RigCodeKwdName = "RIG_CODE";

    public TcyccTradeBasedBuilder(Trade trade, JDate valDate){
        this.trade=trade;
        this.valDate=valDate;
        if(trade!=null) {
            this.securityWrapper.initRelatedSecutityData(trade.getProduct());
            this.securityWrapper.initRelatedSecPricesData(getPreviousBusinessDay());
        }
    }

    /*
     * Identifiers
     */
    public String buildIdEmprField() {
        String value = FIFlowStaticData.MADRID_ENTITY_CODE;
        if (isLondonBranch(trade)) {
            value = FIFlowStaticData.SLB_ENTITY_CODE;
        }
        return value;
    }

    public String buildIdCentField() {
        String value = FIFlowStaticData.MADRID_CENTER_CODE;
        if (isLondonBranch(trade)) {
            value = FIFlowStaticData.SLB_CENTER_CODE;
        }
        return value;
    }

    private boolean isLondonBranch(Trade trade){
        String poShortName= Optional.ofNullable(trade).map(Trade::getBook)
                .map(Book::getLegalEntity).map(LegalEntity::getCode).orElse("");
       return FIFlowStaticData.LONDON_BRANCH_LE_CODE.equals(poShortName);
    }


    //Persona
    public String buildTipersonField() {
        return getLEExternalRef(trade.getCounterParty())
                .map(s -> s.substring(0, 1)).orElse("");
    }

    public Integer buildCdPersonField() {
        String jAgentStr=getLEExternalRef(trade.getCounterParty())
                .map(s -> s.substring(1)).orElse("0");
        return Integer.parseInt(jAgentStr);
    }

    /**
     * @param legalEntity
     * @return
     */
    protected Optional<String> getLEExternalRef(LegalEntity legalEntity) {
        return Optional.ofNullable(legalEntity).map(LegalEntity::getExternalRef);
    }

    /*
     * Dates
     */
    public JDate buildFVtoOperField() {
        return Optional.ofNullable(this.trade).map(Trade::getSettleDate).orElse(null);
    }

    public JDate buildFConOperField() {
        return Optional.ofNullable(this.trade).map(Trade::getSettleDate).orElse(null);
    }

    /*
     * Trade Details
     */

    public String buildIdSentOp() {
        boolean isNegativeNominal=isNegative(buildIPrinOpe());
        int idSendFlj;
        if(isNegativeNominal){
            idSendFlj=2;
        }else {
           idSendFlj=1;
        }
        return String.valueOf(idSendFlj);
    }

    private boolean isNegative(double d) {
        return Double.compare(d, 0.0) < 0;
    }
    public String buildCdstrOpeField() {
        String accBookName = Optional.ofNullable(this.trade).map(Trade::getBook)
                .map(Book::getAccountingBook).map(AccountingBook::getName).orElse("");
        return FIFlowStaticData.PortfolioType.valueOf(FIFlowStaticData.PortfolioType.formatPortfolioType(accBookName)).getMappedValue();
    }

    public String buildCcReferREFInterna() {
        String refInternaCode = "REF_INTERNA";
        return securityWrapper.getSecCodeFromBond(refInternaCode);
    }

    public String buildCdPortfo() {
        return Optional.ofNullable(this.trade).map(Trade::getBook)
                .map(Book::getName).orElse("");
    }

    public String buildCodDivisa() {
        return buildCodDiviLiq();
    }

    public String buildCodDiviLiq() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
    }

    /*
     * PRICING RELATED METHODS
     */
    /**
     * @return Clean price
     */
    public Double buildImprLimp() {
        return getQuoteValue(this.securityWrapper.getCleanPrice());
    }

    /**
     * @return Dirty price
     */
    public Double buildImprSuci() {
        return getQuoteValue(this.securityWrapper.getDirtyPrice());
    }

    public Double buildIPrinOpe() {
        Product product= Optional.ofNullable(this.trade).map(Trade::getProduct)
                .orElse(null);
        Double nominal;
        if(product instanceof SecFinance){
            nominal=Optional.ofNullable(((SecFinance) product).getCollaterals()).map(collaterals -> collaterals.get(0))
                    .map(collateral -> collateral.computeNominal(trade)).orElse(0.0D);
        }else{
            nominal=Optional.ofNullable(product).map(p->p.computeNominal(trade))
                    .orElse(0.0D);
        }
        return nominal;
    }

    public Double buildImcpCorr() {
        return Optional.ofNullable(this.trade).map(Trade::getProduct).map(p -> p.computeNominal(this.trade,valDate))
                .orElse(0.0D);

    }

    public Double buildImpEfeOpe() {
        Double cleanPrice = buildImprLimp();
        return buildIPrinOpe() * (cleanPrice / 100);
    }

    private Double getQuoteValue(QuoteValue quoteValue) {
        return Optional.ofNullable(quoteValue).map(QuoteValue::getClose).map((close -> close * 100)).orElse(0.0D);

    }

    /**
     * For quote retrieval
     *
     * @return
     */
    private JDate getPreviousBusinessDay() {
        Vector<String> holidays = Util.string2Vector("SYSTEM");
        return this.valDate.addBusinessDays(-1, holidays);
    }

    /**
     * Get RigCode
     * @return
     */
    public String buildCdRig() {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue(RigCodeKwdName)).orElse("");
    }
}
