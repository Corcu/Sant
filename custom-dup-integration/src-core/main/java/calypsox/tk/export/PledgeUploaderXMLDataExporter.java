package calypsox.tk.export;

import calypsox.tk.bo.document.DataExporterTopicSender;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.*;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.services.GatewayUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class PledgeUploaderXMLDataExporter extends com.calypso.tk.export.UploaderXMLDataExporter{


    public PledgeUploaderXMLDataExporter(DataExporterConfig exporterConfig) {
        super(exporterConfig);
    }


    @Override
    public void sendData() {
        if(!isIgnoreObject()) {
            if("Topic".equalsIgnoreCase(this.getExportType()) && Util.isEmpty(this.getErrors()) && !Util.isEmpty(this.getDataToSend())) {
                (new DataExporterTopicSender(this.getProperties(), this.getSourceName())).send(this.getDs(), this.getDataToSend(), this.getContextMap(), this.getErrors());
            }
            else {
                super.sendData();
            }
        }
    }
    /**
     * In beta status
     * @param message
     */
    @Override
    public void exportFromMessage(BOMessage message) {
        setBoMessage(message);
        long linkedMessageId = CalypsoIDAPIUtil.getLinkedId(getBoMessage());
        if (linkedMessageId==0L || isLinkedMsgAccepted(linkedMessageId)) {
            this.createDataFromBOMessage();
        } else {
            List<String> errors=getErrors();
            errors.add("Cannot process this message as linked message is not processed");
            setErrors(errors);
        }
    }

    @Override
    protected Map<String, String> getContextMap() {
        Map<String, String> contextMap = super.getContextMap();
        return Optional.ofNullable(getCalypsoObject())
                .filter(t->t instanceof CalypsoTrade)
                        .map(t->this.putRoutingAttributes(contextMap, (CalypsoTrade) t))
                                .orElse(contextMap);
    }

    @Override
    public void setCalypsoObject(CalypsoObject calypsoObject) {
        if(calypsoObject instanceof CalypsoTrade){
            setTradeActionFromBOMessage((CalypsoTrade) calypsoObject);
            setMessageIdKwd((CalypsoTrade) calypsoObject);
        }
        super.setCalypsoObject(calypsoObject);
    }

    @Override
    protected void updateBOMessage() {
        saveAdviceDocument(this.getBoMessage(),this.getDataToSend());
        super.updateBOMessage();

    }

    private Map<String, String> putRoutingAttributes(Map<String, String> contextMap, CalypsoTrade trade){
        String routingValue=Optional.ofNullable(trade).map(CalypsoTrade::getTradeKeywords)
                .map(TradeKeywords::getKeyword).map(this::getSecurityTypeKwd)
                .filter(secType-> Bond.class.getSimpleName().equals(secType))
                .map(p ->"PledgeEU")
                .orElse("PledgeEQ");
        contextMap.put("TOPIC","Madrid");
        contextMap.put("ProductType",routingValue);
        return contextMap;
    }

    private String getSecurityTypeKwd(List<Keyword> kwds) {
        String kwdValue = null;
        for (Keyword currentKwd : kwds) {
            if (currentKwd != null && "SecurityType".equals(currentKwd.getKeywordName())) {
                kwdValue = currentKwd.getKeywordValue();
                break;
            }
        }
        return kwdValue;
    }

    private boolean isLinkedMsgAccepted(long linkedMessageId){
        BOMessage linkedMessage = GatewayUtil.getMessage(linkedMessageId);
        return (linkedMessage != null &&(linkedMessage.getStatus().toString().equalsIgnoreCase("SENT")
                || linkedMessage.getStatus().toString().equalsIgnoreCase("CANCELED")
                || linkedMessage.getStatus().toString().equalsIgnoreCase("ACKED")));
    }

    /**
     * Murex only accepts NEW and AMEND actions over this trades.
     * As it's source msgs are using the same AdviceConfigId, the one without a linkedId is the first (NEW) one.
     * @param calypsoTrade
     */
    private void setTradeActionFromBOMessage(CalypsoTrade calypsoTrade){
        String finalTradeAction= Action.S_NEW;
        boolean isAmendAction=Optional.ofNullable(this.getBoMessage()).map(BOMessage::getLinkedLongId)
                .map(id->id!=0L).orElse(false);
        if(isAmendAction){
           finalTradeAction=Action.S_AMEND;
        }
        calypsoTrade.setAction(finalTradeAction);
    }


    private void setMessageIdKwd(CalypsoTrade calypsoTrade){
        String kwdName= "MESSAGE_ID";
        String kwdValue=Optional.ofNullable(this.getBoMessage()).map(BOMessage::getLongId)
                .map(Object::toString).orElse("0");
        TradeKeywords kwds=Optional.ofNullable(calypsoTrade.getTradeKeywords())
                .orElse(new TradeKeywords());
        Keyword kwd=new Keyword();
        kwd.setKeywordName(kwdName);
        kwd.setKeywordValue(kwdValue);
        kwds.getKeyword().add(kwd);
        calypsoTrade.setTradeKeywords(kwds);
    }

    protected void saveAdviceDocument(BOMessage boMessage, String dataToSend) {
        AdviceDocument aDocument;
        try {
            if (this.getAdviceDocument(boMessage) == null && !Util.isEmpty(this.getDataToSend())) {
                aDocument=GatewayUtil.createAdviceDocument(boMessage, dataToSend);
                aDocument.setSentDate(new JDatetime());
                aDocument.setSentB(true);
                long adviceDocId = DSConnection.getDefault().getRemoteBO().save(aDocument);
                boMessage.setAttribute("DataAdviceDocumentID", String.valueOf(adviceDocId));
            }
        } catch (Exception exc) {
            Log.error(this,exc.getCause());
        }
    }


}
