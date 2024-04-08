package calypsox.tk.bo.boi;

import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @author dmenendez fb
 */
public class BOIDiarioMsgBuilderBRS extends BOIDiarioMsgBuilder{

    public BOIDiarioMsgBuilderBRS(Trade trade, JDate processDate) {
        super(trade, processDate);
    }

    @Override
    public List<BOIDiarioBean> build(PricingEnv env) {
        List<BOIDiarioBean> beans = new ArrayList<>();

        if(Optional.ofNullable(this.trade).isPresent() && this.trade.getProduct() instanceof PerformanceSwap) {
            BOIDiarioBean primaryLeg = new BOIDiarioBean();

            final PerformanceSwap product = (PerformanceSwap) this.trade.getProduct();
            final PLMark plMarkValue = getPLMarkValue(env, trade, this.processDate);

            primaryLeg.setFecProceso(this.formatter.checkValue(this.formatter.formatDate(this.processDate)));
            primaryLeg.setRefIntragrupo(this.formatter.checkLength(this.formatter.checkValue(trade.getKeywordValue("RIG_CODE"))));
            primaryLeg.setSistemaOrigen(BOIStaticData.APP_ORI);
            primaryLeg.setCodProducto(this.formatter.checkLength(this.formatter.checkValue("SWP")));
            primaryLeg.setCodInstrumento(this.formatter.checkLength(this.formatter.checkValue("SWAP")));

            primaryLeg.setCodPortfolio(this.formatter.checkLength(trade.getBook().getName()));
            primaryLeg.setCodOperacion(this.formatter.checkLength(this.formatter.checkValue(this.formatter.buildField(trade.getLongId()))));

            //EMPTY
            primaryLeg.setcodEstructura("");
            primaryLeg.setTipoEstructura("");

            primaryLeg.setCodGLCS(this.formatter.checkLength(this.formatter.checkValue(trade.getCounterParty().getCode())));
            primaryLeg.setCodOperacionNego(this.formatter.checkLength(trade.getKeywordValue("MurexRootContract")));

            primaryLeg.setCodDivisa(this.formatter.checkLength(this.formatter.checkValue(product.getPrimaryLegCurrency())));

            String directionPrimayLeg = loadPrimaryLegDirection(product);

            primaryLeg.setCodDireccion(directionPrimayLeg);
            primaryLeg.setCodEstrategia(BOIStaticData.TRADING);

            primaryLeg.setfecCaptura(this.formatter.checkValue(this.formatter.formatDate(trade.getEnteredDate().getJDate(TimeZone.getDefault()))));
            primaryLeg.setfecContratacion(this.formatter.checkValue(this.formatter.formatDate(JDate.valueOf(trade.getTradeDate()))));
            primaryLeg.setfecValor(this.formatter.checkValue(this.formatter.formatDate(trade.getSettleDate())));
            primaryLeg.setfecVencimiento(this.formatter.checkValue(this.formatter.formatDate(trade.getMaturityDate())));

            primaryLeg.setPrincipalOpe(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.getPrimaryLeg().getPrincipal()))));
            primaryLeg.setPrincipalVigor(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.getPrimaryLeg().getPrincipal()))));

            primaryLeg.setReferencia(this.formatter.checkLength(this.formatter.checkValue(getReferenceType(product.getPrimaryLeg()))));
            primaryLeg.setSpread(this.formatter.formatDecimal2(getCurrentSpread(product.getPrimaryLeg(), this.processDate)));
            primaryLeg.setTasaInteres(this.formatter.formatDecimal2(getCurrentRate(product.getPrimaryLeg(), this.processDate)));
            primaryLeg.setBaseCalculo(this.formatter.buildField(getDayCount(product.getPrimaryLeg())));

            primaryLeg.setFecInicio(this.formatter.formatDate(getStartDate(product.getPrimaryLeg(), this.processDate)));
            primaryLeg.setFecFin(this.formatter.formatDate(getEndDate(product.getPrimaryLeg(), this.processDate)));

            //EMPTY
            primaryLeg.setIndCallPut("");
            primaryLeg.settipOpcion1("");
            primaryLeg.settipOpcion2("");
            primaryLeg.setPrima("");
            primaryLeg.setDivisaPrima("");
            primaryLeg.setStrike("");

            // SALDOS OPE
            Double saldoCajasOpe = getPLCajas(product.getPrimaryLeg(), this.processDate, trade, directionPrimayLeg, true);
            Double saldoDevengosOpe = 0.0;
            Double saldMercadoOpe = getPLMark(plMarkValue,"NPV_LEG1");

            //SALDOS 33
            primaryLeg.setSaldoCajasOpe(this.formatter.formatDecimal(saldoCajasOpe));
            primaryLeg.setSaldoDevengosOpe(this.formatter.formatDecimal(saldoDevengosOpe));
            primaryLeg.setSaldoMercadoOpe(this.formatter.formatDecimal(saldMercadoOpe));
            primaryLeg.setSaldoContableOpe(this.formatter.formatDecimal(
                    saldoCajasOpe + saldoDevengosOpe + saldMercadoOpe));

            try {
                // SALDOS OPE EUR
                PricingEnv env2 = (PricingEnv) env.clone();
                env2.setName("OFFICIAL");

                Double saldoCajasLocal = convertToEUR(saldoCajasOpe, product.getPrimaryLegCurrency(),this.processDate, env2);
                Double saldoDevengosLocal = convertToEUR(saldoDevengosOpe, product.getPrimaryLegCurrency(),this.processDate, env2);
                Double saldMercadoLocal = convertToEUR(saldMercadoOpe, product.getPrimaryLegCurrency(),this.processDate, env2);

                //SALDOS 37
                primaryLeg.setSaldoCajasLocal(this.formatter.formatDecimal(saldoCajasLocal));
                primaryLeg.setSaldoDevengosLocal(this.formatter.formatDecimal(saldoDevengosLocal));
                primaryLeg.setSaldoMercadoLocal(this.formatter.formatDecimal(saldMercadoLocal));
                primaryLeg.setSaldoContableLocal(this.formatter.formatDecimal(
                        saldoCajasLocal + saldoDevengosLocal + saldMercadoLocal));

            }catch (MarketDataException | CloneNotSupportedException e) {
                Log.info(this.getClass().getSimpleName(),e.getCause());
            }

            beans.add(primaryLeg);

            //Always create the second leg
            BOIDiarioBean secondaryLeg;
            try {
                secondaryLeg = (BOIDiarioBean) BeanUtils.cloneBean(primaryLeg);
                if(null!=secondaryLeg){
                    fillSecondLeg(product,secondaryLeg,plMarkValue, env);
                    beans.add(secondaryLeg);
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                    | NoSuchMethodException e) {
                Log.error("Cannot create the second direction of the position", e);

            }

        }

        return beans;
    }

    private void fillSecondLeg(PerformanceSwap product,BOIDiarioBean secondaryLeg, PLMark plMark, PricingEnv env){

        secondaryLeg.setCodDivisa(this.formatter.checkLength(this.formatter.checkValue(product.getPrimaryLegCurrency())));

        String directionSecondaryLeg =loadSecondaryLegDirection(secondaryLeg.getCodDireccion());
        secondaryLeg.setCodDireccion(directionSecondaryLeg);

        secondaryLeg.setPrincipalOpe(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.getSecondaryLeg().getPrincipal()))));
        secondaryLeg.setPrincipalVigor(this.formatter.checkValue(this.formatter.formatDecimal(Math.abs(product.getPrincipal()))));

        secondaryLeg.setReferencia(this.formatter.checkLength(this.formatter.checkValue(getReferenceType(product.getSecondaryLeg()))));
        secondaryLeg.setSpread(this.formatter.formatDecimal2(((Bond) product.getReferenceProduct()).getRateIndexSpread()));
        secondaryLeg.setTasaInteres(this.formatter.formatDecimal2(getCurrentRate(product.getSecondaryLeg(), this.processDate)));
        secondaryLeg.setBaseCalculo(this.formatter.buildField(getDayCount(product.getSecondaryLeg())));

        secondaryLeg.setFecInicio(this.formatter.formatDate(getStartDate(product.getSecondaryLeg(), this.processDate)));
        secondaryLeg.setFecFin(this.formatter.formatDate(getEndDate(product.getSecondaryLeg(), this.processDate)));

        Double saldoCajasOpe = getPLCajas(product.getSecondaryLeg(), this.processDate, trade, directionSecondaryLeg, false);
        Double saldoDevengosOpe = 0.0;
        Double saldMercadoOpe = getPLMark(plMark,"NPV_LEG2");

        //SALDOS 33
        secondaryLeg.setSaldoCajasOpe(this.formatter.formatDecimal(saldoCajasOpe));
        secondaryLeg.setSaldoDevengosOpe(this.formatter.formatDecimal(saldoDevengosOpe));
        secondaryLeg.setSaldoMercadoOpe(this.formatter.formatDecimal(getPLMark(plMark,"NPV_LEG2")));
        secondaryLeg.setSaldoContableOpe(this.formatter.formatDecimal(
                saldoCajasOpe + saldoDevengosOpe + saldMercadoOpe));

        try {
            // SALDOS OPE EUR
            PricingEnv env2 = (PricingEnv) env.clone();
            env2.setName("OFFICIAL");

            Double saldoCajasLocal = convertToEUR(saldoCajasOpe, product.getPrimaryLegCurrency(),this.processDate, env2);
            Double saldoDevengosLocal = convertToEUR(saldoDevengosOpe, product.getPrimaryLegCurrency(),this.processDate, env2);
            Double saldMercadoLocal = convertToEUR(saldMercadoOpe, product.getPrimaryLegCurrency(),this.processDate, env2);

            secondaryLeg.setSaldoCajasLocal(this.formatter.formatDecimal(saldoCajasLocal));
            secondaryLeg.setSaldoDevengosLocal(this.formatter.formatDecimal(saldoDevengosLocal));
            secondaryLeg.setSaldoMercadoLocal(this.formatter.formatDecimal(saldMercadoLocal));
            secondaryLeg.setSaldoContableLocal(this.formatter.formatDecimal(
                    saldoCajasLocal + saldoDevengosLocal + saldMercadoLocal));

        } catch (MarketDataException | CloneNotSupportedException e) {
            Log.info(this.getClass().getSimpleName(),e.getCause());
        }

    }

    public String loadPrimaryLegDirection(PerformanceSwap product){
        String primayLegDesc = "";

        if(null!=trade && null!=product){
            PerformanceSwappableLeg primaryLeg = product.getPrimaryLeg();
            PerformanceSwapLeg primLeg = null;
            boolean perfLeg = false;

            if (primaryLeg instanceof PerformanceSwapLeg) {
                perfLeg = true;
                primLeg = (PerformanceSwapLeg)primaryLeg;
            }
            if (perfLeg) {
                if (primLeg.getNotional() > 0.0D) {
                    primayLegDesc = "B";
                } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() > 0.0D) {
                    primayLegDesc = "B";
                } else {
                    primayLegDesc = "S";
                }
            }
        }
        return primayLegDesc;
    }

    private String loadSecondaryLegDirection(String primaryDirection){
        if("B".equalsIgnoreCase(primaryDirection)){
            return "S";
        }else if("S".equalsIgnoreCase(primaryDirection)){
            return "B";
        }
        return "";
    }

    protected double getCurrentRate(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            CashFlowInterest cfi = (CashFlowInterest) cashFlow;
            if (cfi != null){
                return cfi.getRate();
            }else{
                return 0.0;
            }
        }
        return 0.0;
    }

    protected String getReferenceType(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            RateIndex index = getRateIndex(leg);
            if (index == null)
                return null;
            else
                return index.getName() + "-" + index.getTenor();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) leg;
            Security sec = perfSwapLeg.getReferenceAsset();
            if (sec instanceof Bond) {
                Bond bond = (Bond) sec;
                return bond.getSecCode(SecCode.ISIN);
            }
        }
        return null;
    }

    protected RateIndex getRateIndex(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            RateIndex index = ((SwapLeg) leg).getRateIndex();
            return index;
        }
        return null;
    }

    protected JDate getStartDate(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            if(cashFlow != null){
                return cashFlow.getCashFlowDefinition().getStartDate();
            } else{
                return null;
            }
        }
        return null;
    }

    protected JDate getEndDate(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            if(cashFlow != null){
                return cashFlow.getCashFlowDefinition().getEndDate();
            } else{
                return null;
            }
        }
        return null;
    }

    protected double getCurrentSpread(PerformanceSwappableLeg leg, JDate valDate) {

        CashFlowSet flows = leg.getFlows();
        if (flows != null && !flows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) flows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            return getSpread(cashFlow);

        }
        return 0.0;
    }

    protected double getSpread(CashFlow cf) {
        double out = 0.0;
        if (cf instanceof CashFlowInterest) {
            CashFlowInterest cfi = (CashFlowInterest) cf;
            return cfi.getSpread();
        }

        return out;
    }

    protected DayCount getDayCount(PerformanceSwappableLeg leg) {
        if (leg instanceof SwapLeg) {
            SwapLeg swapLeg = (SwapLeg) leg;
            return swapLeg.getDayCount();
        } else if (leg instanceof PerformanceSwapLeg) {
            PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) leg;
            return performanceSwapLeg.getDayCount();
        }
        return null;
    }

    protected double getPLCajas(PerformanceSwappableLeg leg, JDate valDate, Trade trade, String direction, Boolean isPrimaryLeg) {
        double amount = 0.0;
        if (trade != null) {
            try {
                final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                        .getTransfers(null, "trade_id = " + trade.getLongId(), null);

                for (int i = 0; i < transfers.size(); i++) {
                    String payRec = getPayRecive(transfers.get(i).getPayReceive());

                    if (transfers.get(i).getTransferType().equalsIgnoreCase("INTEREST") || (isPrimaryLeg && (transfers.get(i).getTransferType().equalsIgnoreCase("PRICE_CHANGE")))){
                        if (transfers.get(i).getStatus().toString().equalsIgnoreCase("SETTLED")
                                && (transfers.get(i).getValueDate().lte(valDate) || transfers.get(i).getValueDate().equals(valDate))
                                && (direction.equalsIgnoreCase(payRec) || transfers.get(i).getTransferType().equalsIgnoreCase("PRICE_CHANGE"))){
                            amount = amount + transfers.get(i).getSettlementAmount();
                        }
                    }
                }
                return amount;
            } catch (CalypsoServiceException exc) {
               Log.error(this,exc.getCause());
            }
        }
        return amount;
    }

    /**
     * Metodo encargado de devolver segundo sentido
     * @param payReceive
     * @return
     */
    private String getPayRecive(final String payReceive) {
        if("RECEIVE".equalsIgnoreCase(payReceive)){
            return "B";
        }else if("PAY".equalsIgnoreCase(payReceive)){
            return "S";
        }
        return "";
    }
}
