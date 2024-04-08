package calypsox.tk.bo.boi;

import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerBond;
import com.calypso.tk.pricer.PricerBondInput;
import com.calypso.tk.pricer.PricerRepo;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowDefinitionImpl;
import com.calypso.tk.util.PricerMeasureUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @author dmenendd
 */
public class BOIDiarioBuilderBond extends BOIDiarioMsgBuilder{

    public BOIDiarioBuilderBond(Trade trade, JDate processDate) {
        super(trade, processDate);
    }

    @Override
    public List<BOIDiarioBean> build(PricingEnv env) {
        List<BOIDiarioBean> beans = new ArrayList<>();

        if(Optional.ofNullable(this.trade).map(Trade::getProduct).map(prd->prd instanceof Bond).orElse(false)) {
            BOIDiarioBean bondBean = new BOIDiarioBean();

            final Bond product = (Bond) this.trade.getProduct();

            //igual
            bondBean.setFecProceso(this.formatter.checkValue(this.formatter.formatDate(this.processDate)));
            //igual
            bondBean.setRefIntragrupo(this.formatter.checkLength(this.formatter.checkValue(trade.getKeywordValue("RIG_CODE"))));
            bondBean.setSistemaOrigen("CALSTC");

            bondBean.setCodProducto(this.formatter.checkLength(this.formatter.checkValue(validateForward(trade))));
            bondBean.setCodInstrumento(this.formatter.checkLength(this.formatter.checkValue(validateForward(trade))));

            bondBean.setCodPortfolio(this.formatter.checkLength(trade.getBook().getName()));
            bondBean.setCodOperacion(buildCodOperacion());

            //EMPTY // igual
            bondBean.setcodEstructura("");
            bondBean.setTipoEstructura("");

            //igual
            bondBean.setCodGLCS(this.formatter.checkLength(this.formatter.checkValue(trade.getCounterParty().getCode())));
            bondBean.setCodOperacionNego(this.formatter.checkLength(trade.getKeywordValue("MurexRootContract")));

            bondBean.setCodDivisa(this.formatter.checkLength(this.formatter.checkValue(trade.getTradeCurrency())));

            bondBean.setCodDireccion(buildBondDirection(product));
            bondBean.setCodEstrategia(BOIStaticData.TRADING);

            //Igual
            bondBean.setfecCaptura(this.formatter.checkValue(this.formatter.formatDate(trade.getEnteredDate().getJDate(TimeZone.getDefault()))));
            bondBean.setfecContratacion(this.formatter.checkValue(this.formatter.formatDate(JDate.valueOf(trade.getTradeDate()))));
            bondBean.setfecValor(this.formatter.checkValue(this.formatter.formatDate(trade.getSettleDate())));
            bondBean.setfecVencimiento(this.formatter.checkValue(this.formatter.formatDate(trade.getSettleDate())));

            //Nominal
            bondBean.setPrincipalOpe(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.computeNominal(trade)))));
            //TODO mark inflaction. Current Nominal
            bondBean.setPrincipalVigor(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.computeNominal(trade, this.processDate)))));

            bondBean.setReferencia(this.formatter.checkLength
                    (this.formatter.checkValue(
                            Optional.ofNullable(product.getSecurity()).map(sec->sec.getSecCode("ISIN")).orElse(""))));
            bondBean.setSpread(getCurrentSpread(product, this.processDate));
            bondBean.setTasaInteres(this.formatter.formatDecimal2(getCurrentRate(product, this.processDate)));
            bondBean.setBaseCalculo(this.formatter.buildField(product.getDaycount()));

            bondBean.setFecInicio(this.formatter.formatDate(getStartDate(product, this.processDate)));
            bondBean.setFecFin(this.formatter.formatDate(getEndDate(product, this.processDate)));

            //EMPTY//igual
            bondBean.setIndCallPut("");
            bondBean.settipOpcion1("");
            bondBean.settipOpcion2("");
            bondBean.setPrima("");
            bondBean.setDivisaPrima("");
            bondBean.setStrike("");

            setPLData(bondBean,product,env);


            beans.add(bondBean);
        }

        return beans;
    }


    private void setPLData(BOIDiarioBean bondBean, Bond product,PricingEnv env){

        PricingEnv envFx = PricingEnv.loadPE(env.getName(),this.processDate.getJDatetime());
        // SALDOS OPE
        Double saldoCajasOpe = 0.0;
        Double saldoDevengosOpe = 0.0;

        Double saldMercadoOpe = getPLMark( getPLMarkValueOFFICIAL_ACCOUNTING(env, trade, this.processDate),"MTM_NET_MUREX");

        //SALDOS 33
        bondBean.setSaldoCajasOpe(this.formatter.formatDecimal(saldoCajasOpe));
        bondBean.setSaldoDevengosOpe(this.formatter.formatDecimal(saldoDevengosOpe));
        bondBean.setSaldoMercadoOpe(this.formatter.formatDecimal(saldMercadoOpe));
        bondBean.setSaldoContableOpe(this.formatter.formatDecimal(
                saldoCajasOpe + saldoDevengosOpe + saldMercadoOpe));

        try {
            // SALDOS OPE EUR
            Double saldoCajasLocal = convertToEUR(saldoCajasOpe, product.getCurrency(),this.processDate, envFx);
            Double saldoDevengosLocal = convertToEUR(saldoDevengosOpe, product.getCurrency(),this.processDate, envFx);
            Double saldMercadoLocal = convertToEUR(saldMercadoOpe, product.getCurrency(),this.processDate, envFx);

            //SALDOS 37
            bondBean.setSaldoCajasLocal(this.formatter.formatDecimal(saldoCajasLocal));
            bondBean.setSaldoDevengosLocal(this.formatter.formatDecimal(saldoDevengosLocal));
            bondBean.setSaldoMercadoLocal(this.formatter.formatDecimal(saldMercadoLocal));
            bondBean.setSaldoContableLocal(this.formatter.formatDecimal(
                    saldoCajasLocal + saldoDevengosLocal + saldMercadoLocal));

        } catch (MarketDataException exc) {
            Log.info(this.getClass().getSimpleName(),exc.getMessage(),exc.getCause());
        }
    }

    public String buildCodOperacion(){
        TradePartenonBuilder partenonBuilder=new TradePartenonBuilder(this.trade);
        String codOperacion=partenonBuilder.buildCdoPerboField();
        if(Util.isEmpty(codOperacion)){
            codOperacion=trade.getKeywordValue("MurexRootContract");
        }
        return this.formatter.checkLength(this.formatter.checkValue(this.formatter.buildField(codOperacion)));
    }
    public String buildBondDirection(Bond product){
        return product != null ? product.getBuySell(trade) == 1 ? "B" : "S" : "";
    }

    protected JDate getStartDate(Bond bond, JDate valDate) {
        return Optional.ofNullable(getCurrentInterestFlow(bond,valDate))
                    .map(CashFlow::getCashFlowDefinition).map(FdnCashFlowDefinitionImpl::getStartDate)
                    .orElse(bond.getStartDate());
    }

    protected JDate getEndDate(Bond bond, JDate valDate) {
        return Optional.ofNullable(getCurrentInterestFlow(bond,valDate))
                .map(CashFlow::getCashFlowDefinition).map(FdnCashFlowDefinitionImpl::getEndDate)
                .orElse(Optional.ofNullable(bond.getEndDate()).orElse(JDate.valueOf(9999,12,31)));
    }

    protected String getCurrentSpread(Bond bond, JDate valDate) {
        return String.valueOf(getSpread(getCurrentInterestFlow(bond,valDate)));
    }

    protected double getCurrentRate(Bond bond, JDate valDate) {
        CashFlow flow=getCurrentInterestFlow(bond,valDate);
        double rate=0.0D;
        if(flow instanceof CashFlowInterest){
            rate=((CashFlowInterest) flow).getRate();
        }
        return rate;
    }

    private CashFlow getCurrentInterestFlow(Bond bond, JDate valDate){
        CashFlow flow=null;
        try {
            CashFlowSet flows = bond.getFlows(trade, valDate, true, -1, true);
            if (flows != null && !flows.isEmpty()) {
                flow= flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
                }
            } catch (FlowGenerationException exc) {
                Log.error(this,exc.getCause());
        }
        return flow;
    }



    protected double getSpread(CashFlow cf) {
        double out = 0.0;
        if (cf instanceof CashFlowInterest) {
            CashFlowInterest cfi = (CashFlowInterest) cf;
            return cfi.getSpread();
        }

        return out;
    }
    protected double getPLCajas(Bond bond, JDate valDate,PricingEnv env) {
        double currentYearSettledInterest = 0.0D;
        CashFlowSet flows = bond.getFlows();
        if (flows != null && !flows.isEmpty()) {
            try {
                flows.calculate(env.getQuoteSet(), this.processDate);
                for (CashFlow flow : flows.getFlows()) {
                    if (flow instanceof CashFlowInterest && isCurrentYearAndPastFlow(flow, valDate)) {
                        currentYearSettledInterest += flow.getAmount();
                    }
                }
            }catch(FlowGenerationException exc){
            Log.warn(this, exc.getCause());
        }
    }
        return currentYearSettledInterest;
    }

    private boolean isCurrentYearAndPastFlow(CashFlow flow,JDate valDate){
        return flow.getDate().getYear()==valDate.getYear()&&flow.getDate().lte(valDate);
    }

    protected Double calculatePricerMeasure(String pmName, Trade trade, PricingEnv env){
        double pricerMeasureAmt=0.0D;
        Pricer pricer=new PricerBond() {
            @Override
            protected double modifiedDuration(Trade trade, JDatetime valDatetime, JDate settleDate, double dprice, double yield, PricingEnv env) throws PricerException {
                return 0;
            }

            @Override
            public double discountedMargin(Trade trade, JDatetime valDatetime, JDatetime spotDatetime, double dprice, PricingEnv env, PricerBondInput input) throws PricerException {
                return 0;
            }

            @Override
            public double dpriceFromDiscountMargin(Trade trade, JDatetime valDatetime, double discountMargin, PricingEnv env, PricerBondInput input) throws PricerException {
                return 0;
            }

            @Override
            public double dpriceFromDiscountMargin(Trade trade, JDatetime valDatetime, JDate settleDate, double discountMargin, PricingEnv env, PricerBondInput input) throws PricerException {
                return 0;
            }

            @Override
            public double accrual(Bond bond, JDate date, PricingEnv env, boolean doFirst) throws PricerException {
                return 0;
            }

            @Override
            public double bondEquivalentYieldSemiAnnual(Bond bond, JDate date, double dprice, PricingEnv env) throws PricerException {
                return 0;
            }

            @Override
            public double dprice(Bond bond, JDate date, double yield, PricingEnv env) throws PricerException {
                return 0;
            }

            @Override
            public double dprice(Trade trade, Bond bond, JDate date, double yield, PricingEnv env) throws PricerException {
                return 0;
            }
        };
        PricerMeasure measure = PricerMeasureUtility.makeMeasure(pmName);
        try {
            pricer.price(trade, this.processDate.getJDatetime(), env, new PricerMeasure[]{measure});
            pricerMeasureAmt= Optional.ofNullable(measure).map(PricerMeasure::getValue)
                    .orElse(0.0D);
        } catch (PricerException exc) {
            Log.warn(this,exc.getCause());
        }
        return pricerMeasureAmt;
    }
    /**
     *
     * @param trade
     * @return
     */
    private String validateForward(Trade trade){
        if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
            return BOIStaticData.BNDFWD;
        }
        else
            return BOIStaticData.BND;
    }
}
