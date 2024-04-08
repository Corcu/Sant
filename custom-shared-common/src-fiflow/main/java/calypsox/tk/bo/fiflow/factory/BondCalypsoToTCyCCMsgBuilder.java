package calypsox.tk.bo.fiflow.factory;

import calypsox.tk.bo.fiflow.builder.trade.BondTcyccTradeBasedBuilder;
import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import calypsox.tk.bo.fiflow.model.CalypsoToTCyCCBean;
import calypsox.tk.bo.fiflow.staticdata.FIFlowStaticData;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;

/**
 * @author dmenendd
 */
public class BondCalypsoToTCyCCMsgBuilder extends FIFlowMsgBuilder<CalypsoToTCyCCBean> {

    BondTcyccTradeBasedBuilder tradeBuilder;
    TradePartenonBuilder partenonBuilder;

    PricingEnv pricingEnv;

    public BondCalypsoToTCyCCMsgBuilder(Trade trade, JDate valDate, PricingEnv pricingEnv) {
        this.tradeBuilder=new BondTcyccTradeBasedBuilder(trade,valDate,pricingEnv);
        this.partenonBuilder=new TradePartenonBuilder(trade);
        this.pricingEnv=pricingEnv;
        messageBean = new CalypsoToTCyCCBean();
    }


    @Override
    public CalypsoToTCyCCBean build() {
        fillIdentifiers();
        fillTradeDates();
        fillOldTradeDetails();
        fillPriceDetails();
        fillCoverDetails();
        fillBondFields();
        return messageBean;
    }

    private void fillIdentifiers() {
        messageBean.setIdEmpr(partenonBuilder.buildCodEmprField());
        messageBean.setIdCent(partenonBuilder.buildCodCentField());
        messageBean.setCodsProd(partenonBuilder.buildCodsProdField());
        messageBean.setCodProd(partenonBuilder.buildCodProdField());
        messageBean.setCdoPerbo(partenonBuilder.buildCdoPerboField());
        messageBean.setTiPerson(tradeBuilder.buildTipersonField());
        messageBean.setCdPerson(tradeBuilder.buildCdPersonField());
    }

    private void fillTradeDates() {
        messageBean.setfLiqOpe(tradeBuilder.buildFVtoOperField());
        messageBean.setfConOper(JDate.valueOf(tradeBuilder.buildFConOperField()));
        messageBean.sethConOper(tradeBuilder.buildHConOperField());
    }

    private void fillOldTradeDetails() {
        messageBean.setCdstrOpe(tradeBuilder.buildCdstrOpeField());
        messageBean.setCcreFer(tradeBuilder.buildCcReferREFInterna());
        messageBean.setCdPortfo(tradeBuilder.buildCdPortfo());
        messageBean.setCoDivisa(tradeBuilder.buildCodDivisa());
        messageBean.setCodDivlq(tradeBuilder.buildCodDiviLiq());
        messageBean.setnTtituloo(tradeBuilder.buildnTituloo());
        messageBean.setiPrinOpe(tradeBuilder.buildIPrinOpe());
        messageBean.setInOpinex(tradeBuilder.buildInOpinex());
        messageBean.setCestoPbo(tradeBuilder.buildCestoPbo());
        messageBean.setCdProdux("0");
        messageBean.setMarToMar(0);
        messageBean.setIdSentOp(tradeBuilder.buildIdSentOp());
    }

    private void fillPriceDetails() {
        messageBean.setImprLimp(tradeBuilder.buildImprLimp());
        messageBean.setImprSuci(tradeBuilder.buildImprSuci());
        messageBean.setImcpCorr(tradeBuilder.buildImcpCorr());
        messageBean.setImEfeOpe(tradeBuilder.buildImpEfeOpe());
    }

    private void fillCoverDetails() {
        messageBean.setIndCober(String.valueOf(0));
        messageBean.setCdCobert(FIFlowStaticData.EMPTY_STRING);
        messageBean.setClCobert(FIFlowStaticData.EMPTY_STRING);
        messageBean.setIdCobert(FIFlowStaticData.EMPTY_STRING);
    }

    private void fillBondFields() {
        messageBean.setCdIroi(tradeBuilder.buildCdIroiField());
        messageBean.setCdRig(tradeBuilder.buildCdRigField());
        messageBean.setIdStrip(tradeBuilder.buildIdStripField());
    }
}
