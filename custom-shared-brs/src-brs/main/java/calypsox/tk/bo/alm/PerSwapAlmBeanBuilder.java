package calypsox.tk.bo.alm;

import com.calypso.tk.core.*;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.flow.CashFlowPrincipal;
import com.calypso.tk.product.flow.CashFlowSimple;
import com.calypso.tk.refdata.RateIndex;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PerSwapAlmBeanBuilder {
    Trade trade;
    JDate proccesDate;


    public PerSwapAlmBeanBuilder(Trade trade,JDate proccesDate) {
        this.trade = trade;
        this.proccesDate = proccesDate;
    }

    public List<PerSwapAlmBean> build(){
        List<PerSwapAlmBean> beans = new ArrayList<>();
        PerSwapAlmBean bean = new PerSwapAlmBean();
        fillStaticData(bean);
        fillOperData(bean,trade);
        fillSecurityData(bean,trade);
        fillBlankValues(bean);

        bean.setInd_red_tesoreria(""); //TODO
        bean.setInd_grupo(""); //TODO
        bean.setClave_colateral(""); //TODO
        bean.setInd_tipo_liquidacion(""); //TODO
        bean.setPeriodica(""); //TODO


        //TODO create clones for CashFlows
        try {
            final CashFlowSet flows = ((PerformanceSwap) trade.getProduct()).getFlows(proccesDate);
            if(null!=flows && !Util.isEmpty(flows.getFlows())){
                beans.addAll(Arrays.stream(flows.getFlows()).map(flow -> cloneAndFillFlowData(flow, bean))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }

        } catch (FlowGenerationException e) {
           Log.error(this,"Error loading cashFlow for trade: " + trade.getLongId() + " " + e.getCause());
        }

        return beans;
    }

    private PerSwapAlmBean cloneAndFillFlowData(CashFlow flow, PerSwapAlmBean bean){
        try {
            PerSwapAlmBean row = (PerSwapAlmBean) BeanUtils.cloneBean(bean);
            //--------------
            //TODO
            row.setFx_inicio_cf(buildField(flow.getStartDate()));
            row.setFx_fin_cf(buildField(flow.getEndDate()));
            row.setFx_payment_cf(buildField(flow.getDate()));


            if(flow instanceof CashFlowSimple){
                final CashFlowSimple simpleFlow = (CashFlowSimple) flow;
                final JDate fxResetDate = simpleFlow.getFXResetDate();
                row.setFx_fixing_cf("");
                row.setInd_direccion_cf("");
                row.setInd_montante_cf("");
                row.setInd_tipo_compraventa(simpleFlow.isFixedRate() ? "F" : "V");
                row.setMoneda_compraventa("");
                row.setConvencion_compraventa("");
                row.setPer_rate(buildField(simpleFlow.getRate()));
                row.setPer_spread(buildField(simpleFlow.getSpread()));
                row.setIndex(buildField(simpleFlow.getIndexFactor()));
            }


            return row;
            //--------------
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                | NoSuchMethodException e) {
            Log.error("Cannot create the second direction of the position", e);
        }
        return null;
    }

    private void fillStaticData(PerSwapAlmBean bean){
        bean.setId_layout("010");
        bean.setFx_extraccion(buildField(proccesDate));
        bean.setFx_datos(buildField(proccesDate));
        bean.setId_fuente("015");
        bean.setId_entidad("1");
        bean.setId_tipo_producto("52");

        bean.setCod_entidad("0049");
    }

    private void fillOperData(PerSwapAlmBean bean, Trade trade){
        bean.setClave_operacion(buildField(trade.getLongId()));
        bean.setCod_cliente(trade.getCounterParty().getCode());
        bean.setDesc_cliente(trade.getCounterParty().getName());
        bean.setFx_contratacion(buildField(trade.getTradeDate()));
        bean.setFx_valor(buildField(trade.getSettleDate()));
        bean.setFx_vencimiento(buildField(trade.getMaturityDate()));
        bean.setImp_montante_nominal(buildField(trade.getTradePrice())); //TODO

        bean.setPortfolio_bs(trade.getBook().getName());
        bean.setPortfolio(trade.getBook().getName());
    }

    private void fillSecurityData(PerSwapAlmBean bean, Trade trade){
        Product secProdcut = ((PerformanceSwap) trade.getProduct()).getReferenceProduct();
        bean.setIsin(secProdcut.getSecCode("ISIN"));
        bean.setDesc_nombre_subyacente(secProdcut.getDescription());
        bean.setDesc_nombre_emisor(buildField(secProdcut.getIssuerIds().get(0))); //TODO

        final RateIndex rateIndex = secProdcut.getRateIndex();
        if(null!=rateIndex){

        }

        bean.setCod_pais(""); //TODO Necesitamos mapeo bond country vs valor a reportar
        bean.setNum_cantidad("");
        bean.setInd_liquido("");
        bean.setInd_cotizado("");

        bean.setRating_sp(""); //TODO
        bean.setRating_md(""); //TODO
        bean.setRating_ft(""); //TODO

        bean.setPrecio_mercado("");
        bean.setValor_mercado("");
        bean.setInd_marca_autonomia("");

        bean.setMoneda_valor_mercado(secProdcut.getCurrency());
    }


    private void fillBlankValues(PerSwapAlmBean bean){
        //BLANK
        bean.setMoneda_vendidaed("");
        bean.setMoneda_compradaed("");
        bean.setImp_montante_moneda_vendidaed("");
        bean.setImp_montante_moneda_compradaed("");
        bean.setIntercambio("");
        bean.setNpv_moneda_comprada("");
        bean.setNpv_moneda_vendida("");
    }

    private String buildField(Object t){
        return String.valueOf(t);
    }
}
