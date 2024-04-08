package calypsox.tk.bo.boi;

import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowDefinitionImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @author aalonsop
 */
public class BOIDiarioBuilderRepo extends BOIDiarioMsgBuilder{

    public BOIDiarioBuilderRepo(Trade trade, JDate processDate) {
        super(trade, processDate);
    }

    @Override
    public List<BOIDiarioBean> build(PricingEnv env) {
        String repoString="REP";
        List<BOIDiarioBean> beans = new ArrayList<>();

        if(Optional.ofNullable(this.trade).map(Trade::getProduct).map(prd->prd instanceof Repo).orElse(false)) {
            BOIDiarioBean repoBean = new BOIDiarioBean();

            final Repo product = (Repo) this.trade.getProduct();

            //igual
            repoBean.setFecProceso(this.formatter.checkValue(this.formatter.formatDate(this.processDate)));
            //igual
            repoBean.setRefIntragrupo(this.formatter.checkLength(this.formatter.checkValue(trade.getKeywordValue("RIG_CODE"))));
            repoBean.setSistemaOrigen("CALSTC");

            repoBean.setCodProducto(this.formatter.checkLength(this.formatter.checkValue(repoString)));
            repoBean.setCodInstrumento(this.formatter.checkLength(this.formatter.checkValue(repoString)));

            repoBean.setCodPortfolio(this.formatter.checkLength(trade.getBook().getName()));
            repoBean.setCodOperacion(buildCodOperacion());

            //EMPTY // igual
            repoBean.setcodEstructura("");
            repoBean.setTipoEstructura("");

            //igual
            repoBean.setCodGLCS(this.formatter.checkLength(this.formatter.checkValue(trade.getCounterParty().getCode())));
            repoBean.setCodOperacionNego(this.formatter.checkLength(trade.getKeywordValue("MurexRootContract")));

            repoBean.setCodDivisa(this.formatter.checkLength(this.formatter.checkValue(product.getCurrency())));

            repoBean.setCodDireccion(buildRepoDirection(product));
            repoBean.setCodEstrategia(BOIStaticData.TRADING);

            //Igual
            repoBean.setfecCaptura(this.formatter.checkValue(this.formatter.formatDate(trade.getEnteredDate().getJDate(TimeZone.getDefault()))));
            repoBean.setfecContratacion(this.formatter.checkValue(this.formatter.formatDate(JDate.valueOf(trade.getTradeDate()))));
            repoBean.setfecValor(this.formatter.checkValue(this.formatter.formatDate(trade.getSettleDate())));
            repoBean.setfecVencimiento(this.formatter.checkValue(this.formatter.formatDate(getFechaVencimiento(product))));

            repoBean.setPrincipalOpe(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(
                    Optional.ofNullable(product.getCollaterals())
                            .filter(coll-> !Util.isEmpty(coll))
                            .map(coll->coll.get(0))
                            .map(Collateral::getNominal).orElse(0.0d)))));
            repoBean.setPrincipalVigor(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.getPrincipal()))));

            repoBean.setReferencia(this.formatter.checkLength
                    (this.formatter.checkValue(
                            Optional.ofNullable(product.getSecurity()).map(sec->sec.getSecCode("REF_INTERNA")).orElse(""))));
            repoBean.setSpread(getCurrentSpread(product, this.processDate));
            repoBean.setTasaInteres(this.formatter.formatDecimal2(getCurrentRate(product, this.processDate)));
            repoBean.setBaseCalculo(this.formatter.buildField(product.getDayCount()));

            repoBean.setFecInicio(this.formatter.formatDate(getStartDate(product, this.processDate)));
            repoBean.setFecFin(this.formatter.formatDate(getEndDate(product, this.processDate)));

            //EMPTY//igual
            repoBean.setIndCallPut("");
            repoBean.settipOpcion1("");
            repoBean.settipOpcion2("");
            repoBean.setPrima("");
            repoBean.setDivisaPrima("");
            repoBean.setStrike("");

            setPLData(repoBean,product,env);


            beans.add(repoBean);
        }

        return beans;
    }


    private JDate getFechaVencimiento(Repo repo){
        JDate endDate=JDate.valueOf(9999,12,31);
        if(!repo.getMaturityTypeFacet().isOpen()){
            endDate=repo.getEndDate();
        }
        return endDate;
    }


    private void setPLData(BOIDiarioBean repoBean, Repo product,PricingEnv env){


        PricingEnv envFx = PricingEnv.loadPE("OFFICIAL",this.processDate.getJDatetime());
        // SALDOS OPE
        Double saldoCajasOpe = getPLCajas(product, this.processDate,envFx);
        Double saldoDevengosOpe =calculatePricerMeasure("ACCRUAL_FIRST",trade,env);

        Double saldMercadoOpe = getPLMark( getPLMarkValue(env, trade, this.processDate),"NPV");

        //SALDOS 33
        repoBean.setSaldoCajasOpe(this.formatter.formatDecimal(saldoCajasOpe));
        repoBean.setSaldoDevengosOpe(this.formatter.formatDecimal(saldoDevengosOpe));
        repoBean.setSaldoMercadoOpe(this.formatter.formatDecimal(saldMercadoOpe));
        repoBean.setSaldoContableOpe(this.formatter.formatDecimal(
                saldoCajasOpe + saldoDevengosOpe + saldMercadoOpe));

        try {
            // SALDOS OPE EUR
            Double saldoCajasLocal = convertToEUR(saldoCajasOpe, product.getCurrency(),this.processDate, envFx);
            Double saldoDevengosLocal = convertToEUR(saldoDevengosOpe, product.getCurrency(),this.processDate, envFx);
            Double saldMercadoLocal = convertToEUR(saldMercadoOpe, product.getCurrency(),this.processDate, envFx);

            //SALDOS 37
            repoBean.setSaldoCajasLocal(this.formatter.formatDecimal(saldoCajasLocal));
            repoBean.setSaldoDevengosLocal(this.formatter.formatDecimal(saldoDevengosLocal));
            repoBean.setSaldoMercadoLocal(this.formatter.formatDecimal(saldMercadoLocal));
            repoBean.setSaldoContableLocal(this.formatter.formatDecimal(
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
    public String buildRepoDirection(Repo product){
       String direction=product.getDirection();
       String res="S";
       if(Repo.DIRECTION_REVERSEREPO.equals(direction)){
            res="B";
       }
       return res;
    }

    protected JDate getStartDate(Repo repo, JDate valDate) {
        return Optional.ofNullable(getCurrentInterestFlow(repo,valDate))
                    .map(CashFlow::getCashFlowDefinition).map(FdnCashFlowDefinitionImpl::getStartDate)
                    .orElse(repo.getStartDate());
    }

    protected JDate getEndDate(Repo repo, JDate valDate) {
        return Optional.ofNullable(getCurrentInterestFlow(repo,valDate))
                .map(CashFlow::getCashFlowDefinition).map(FdnCashFlowDefinitionImpl::getEndDate)
                .orElse(Optional.ofNullable(repo.getEndDate()).orElse(JDate.valueOf(9999,12,31)));
    }

    protected String getCurrentSpread(Repo repo, JDate valDate) {
        return String.valueOf(getSpread(getCurrentInterestFlow(repo,valDate)));
    }

    protected double getCurrentRate(Repo repo, JDate valDate) {
        CashFlow flow=getCurrentInterestFlow(repo,valDate);
        double rate=0.0D;
        if(flow instanceof CashFlowInterest){
            rate=((CashFlowInterest) flow).getRate();
        }
        return rate;
    }

    private CashFlow getCurrentInterestFlow(Repo repo, JDate valDate){
        CashFlow flow=null;
        try {
            CashFlowSet flows = repo.getFlows(trade, valDate, true, -1, true);
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
    protected double getPLCajas(Repo repo, JDate valDate,PricingEnv env) {
        double currentYearSettledInterest = 0.0D;
        CashFlowSet flows = repo.getFlows();
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

}
