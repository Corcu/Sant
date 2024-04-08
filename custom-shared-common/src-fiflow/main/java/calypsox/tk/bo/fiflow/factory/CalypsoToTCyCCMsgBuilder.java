package calypsox.tk.bo.fiflow.factory;

import calypsox.tk.bo.fiflow.builder.trade.TcyccTradeBasedBuilder;
import calypsox.tk.bo.fiflow.builder.trade.TradePartenonBuilder;
import calypsox.tk.bo.fiflow.model.CalypsoToTCyCCBean;
import calypsox.tk.bo.fiflow.staticdata.FIFlowStaticData;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class CalypsoToTCyCCMsgBuilder extends FIFlowMsgBuilder<CalypsoToTCyCCBean> {

    TcyccTradeBasedBuilder tradeBuilder;
    TradePartenonBuilder partenonBuilder;

    public CalypsoToTCyCCMsgBuilder(Trade trade, JDate valDate) {
        this.tradeBuilder=new TcyccTradeBasedBuilder(trade,valDate);
        this.partenonBuilder=new TradePartenonBuilder(trade);
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
        //Same as settleSettle
        messageBean.setfLiqOpe(tradeBuilder.buildFVtoOperField());
        messageBean.setfConOper(tradeBuilder.buildFConOperField());
        messageBean.sethConOper("000000");
    }

    private void fillOldTradeDetails() {
        messageBean.setCdstrOpe(tradeBuilder.buildCdstrOpeField());
        messageBean.setCcreFer(tradeBuilder.buildCcReferREFInterna());
        messageBean.setCdPortfo(tradeBuilder.buildCdPortfo());
        messageBean.setCoDivisa(tradeBuilder.buildCodDivisa());
        messageBean.setCodDivlq(tradeBuilder.buildCodDiviLiq());
        messageBean.setiPrinOpe(tradeBuilder.buildIPrinOpe());
        messageBean.setInOpinex("1");
        messageBean.setCestoPbo("2");
        messageBean.setCdProdux("2");
        messageBean.setMarToMar(0);
        //OJO CON ESTE
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
        messageBean.setCdIroi(FIFlowStaticData.EMPTY_STRING);
        messageBean.setCdRig(tradeBuilder.buildCdRig());
        messageBean.setIdStrip(FIFlowStaticData.EMPTY_STRING);
    }
}
