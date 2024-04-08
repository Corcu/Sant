package calypsox.tk.upload.validator;


import calypsox.tk.upload.validator.ccp.ClearingTradeUploadValidator;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventUpload;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.services.ErrorExceptionUtils;
import com.calypso.tk.upload.util.UploaderTradeUtil;

import java.util.*;


public class ValidateCalypsoTradeBond extends com.calypso.tk.upload.validator.ValidateCalypsoTradeBond implements ClearingTradeUploadValidator, ReprocessKwdValidator {


    /**
     * validate : unique external reference per product.
     */
    public void validate(CalypsoObject object, Vector<BOException> errors) {
        this.calypsoTrade = (CalypsoTrade) object;

        _trade = getExistingTrade(this.calypsoTrade, isUndoTerminate(), errors);

        if ((calypsoTrade.getTradeId() != null || _trade != null) && "NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
            errors.add(ErrorExceptionUtils.createException("21001", "Trade Action", "00011", UploaderTradeUtil.getID(this.calypsoTrade), _trade != null ? _trade.getLongId() : calypsoTrade.getTradeId()));
            return;
        }
        if ((calypsoTrade.getTradeId() == null && _trade == null) && !"NEW".equalsIgnoreCase(this.calypsoTrade.getAction())) {
            String productType = Util.isEmpty(this.calypsoTrade.getExternalReference()) ? "TradeId" : "External Reference";
            errors.add(ErrorExceptionUtils.createException("21002", productType, "00002", UploaderTradeUtil.getID(this.calypsoTrade), 0L));
            return;
        }

        if (_trade != null) {
            calypsoTrade.setTradeId(_trade.getLongId());
        }

        validateSettleDateIfActivated((CalypsoTrade) object, errors);
        //TradeFee fee = calypsoTrade.getTradeFee();
        //calypsoTrade.setTradeFee(null);
        if(!isReprocess(calypsoTrade)) {
            validateTerminateClearingTrade(calypsoTrade, errors);
        }

        String externalReference = calypsoTrade.getExternalReference();
        calypsoTrade.setExternalReference(null);
        super.validate(object, errors);
        calypsoTrade.setExternalReference(externalReference);

        //Equity equity = calypsoTrade.getProduct().getEquity();

        //if(errors!=null && errors.size()>0){
        //	generateErrorMessage(errors);
        //}
    }

    public void validateSettleDateIfActivated(CalypsoTrade object, Vector<BOException> errors) {
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "DEACTIVATE_BOND_DUP_SETTLEDATE_CHECK");
        if (!Boolean.parseBoolean(activationFlag)&&!isReprocess(calypsoTrade)) {
            validateSettleDate(object, errors);
        }
    }

    private void validateSettleDate(CalypsoTrade object, Vector<BOException> errors) {
        this.calypsoTrade = object;
        JDate currentJDate = JDate.getNow();
        JDate settleDate = JDate.valueOf(object.getTradeSettleDate().toGregorianCalendar().getTime());
        if(!Action.S_NEW.equalsIgnoreCase(calypsoTrade.getAction())) {
            if (settleDate.lte(currentJDate)){
                errors.add(ErrorExceptionUtils.createException("21001", "Mod/Canc where Settle Date is before/equal current date " + currentJDate, "00149", settleDate.toString(), 0L));
            }
        } else {
            if (settleDate.before(currentJDate)) {
                errors.add(ErrorExceptionUtils.createException("21001", "Settle Date is before current date " + currentJDate, "00009", settleDate.toString(), 0L));
            }
        }
    }

    public boolean isUndoTerminate() {
        return calypsoTrade.getAction().equals("UNDO_TERMINATE");
    }


    private void generateErrorMessage(Vector<BOException> errors) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><CalypsoUploadDocument xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SantanderCalypsoProducts_v1.3.xsd\"><CalypsoTrade tradeType=\"Bond\"><ExternalReference>41EXT_BNMMDCP_CP_1</ExternalReference><InternalReference/><Action>NEW</Action><TradeCounterParty>DEUKA</TradeCounterParty><CounterPartyRole>CounterParty</CounterPartyRole><TradeBook>COL_MAD_DV</TradeBook><TradeCurrency>USD</TradeCurrency><TradeSettlementCurrency>USD</TradeSettlementCurrency><TradeNotional>1000000.0</TradeNotional><BuySell>BUY</BuySell><TradeDateTime>2016-01-25T09:30:47.0Z</TradeDateTime><TradeSettleDate>2018-01-26</TradeSettleDate><StartDate>2010-07-07</StartDate><MaturityDate>2015-07-06</MaturityDate><TraderName>NONE</TraderName><SalesPerson>NONE</SalesPerson><ProductType>Bond</ProductType><ProductSubType/><Product><Bond><ProductCodeType>ISIN</ProductCodeType><ProductCodeValue>IT0005253817</ProductCodeValue><NegotiatedPriceType>DirtyPrice</NegotiatedPriceType><NegotiatedPrice>102</NegotiatedPrice><Quantity>1000000.0</Quantity><FxRate>1.5</FxRate><SettlementAmount>10000000</SettlementAmount><SecondaryFXRate>21</SecondaryFXRate></Bond></Product><Termination><TerminationReason/><TerminationAmount>0.0</TerminationAmount><RemainingNotional>10000</RemainingNotional><TerminationPercent>0</TerminationPercent><FFCPOption>false</FFCPOption></Termination><Novation><NovationType/><NovationAmount>0.0</NovationAmount><RemainingNotional>1000000.0</RemainingNotional><NovationPercent>0</NovationPercent><NovationCounterParty/><NovationCounterPartyRole/><FFCPOption>false</FFCPOption></Novation><TradeKeywords><Keyword><KeywordName>MurexStatus</KeywordName><KeywordValue>1</KeywordValue></Keyword><Keyword><KeywordName>UniqueID</KeywordName><KeywordValue>1234567890</KeywordValue></Keyword><Keyword><KeywordName>FlowControlEventType</KeywordName><KeywordValue>Insert</KeywordValue></Keyword><Keyword><KeywordName>MurexRootContract</KeywordName><KeywordValue>012345</KeywordValue></Keyword></TradeKeywords></CalypsoTrade></CalypsoUploadDocument>";
        Map<String, String> attr = new HashMap<String, String>();
        attr.put("JMS_TIMESTAMP", "1649084978686");
        attr.put("JMS_MESSAGE_ID", "ID:EMS-DES-FWEST.D92622976F734484F:21");
        attr.put("JMSXDeliveryCount", "1");
        attr.put("ProductType", "BOND MCY");

        PSEventUpload psEventUpload = new PSEventUpload();
        psEventUpload.setMessage(xml);
        psEventUpload.setAttributes(attr);
        psEventUpload.setMessageSource("Murex");
        psEventUpload.setGateway("Uploader");
        psEventUpload.setMessageFormat("UploaderXML");
        psEventUpload.setSequence(0);
        psEventUpload.setEventStatsUpdated(false);

        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(psEventUpload);
        } catch (Exception e) {
            Log.error(ValidateCalypsoTradeBond.class.getName() + ": failed to publish PSEventUpload event", e);
        }


        DSConnection ds = DSConnection.getDefault();
        String xmlBook = this.calypsoTrade.getTradeBook();
        Book book = BOCache.getBook(DSConnection.getDefault(), xmlBook);

        StringBuilder errorType = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            errorType.append(errors.get(i).getMessage());
            if (i != errors.size() - 1) {
                errorType.append("  ---  ");
            }
        }

        BOMessage message = new BOMessage();
        message.setMessageType("FLOWCONTROL_EXPORT");
        message.setStatus(Status.S_NONE);
        message.setAction(Action.NEW);
        message.setSubAction(Action.NONE);
        message.setLongId(0);
        message.setMessageClass(0);
        message.setProductType("Bond");
        message.setProductFamily("Bond");
        message.setEventType("ERROR_IMPORT");
        message.setLanguage("English");
        message.setCreationDate(new JDatetime());
        message.setTradeUpdateDatetime(new JDatetime());
        message.setMatchingB(false);
        message.setAction(Action.NEW);
        message.setFormatType(null);
        message.setExternalB(true);
        message.setGateway("P37");
        message.setAddressMethod("CALYPSOML");
        message.setFormatType("TEXT");
        message.setTemplateName("FlowControl.txt");
        message.setAttribute("FlowControlErrorException", errorType.toString());

        // Message Attributes
        if (book != null) {
            message.setAttribute("FlowControlErrorEntity", book.getName());
        }
        if (this.calypsoTrade.getMirrorBook() != null) {
            message.setAttribute("FlowControlErrorType", "internal");
        } else {
            message.setAttribute("FlowControlErrorType", "external");
        }
        if (this.calypsoTrade.getTradeKeywords() != null) {
            for (Keyword keyword : this.calypsoTrade.getTradeKeywords().getKeyword()) {
                if (keyword.getKeywordName().equals("UniqueID") && !Util.isEmpty(keyword.getKeywordValue())) {
                    message.setAttribute("FlowControlErrorUniqueID", keyword.getKeywordValue());
                } else if (keyword.getKeywordName().equals("FlowControlEventType") && !Util.isEmpty(keyword.getKeywordValue())) {
                    message.setAttribute("FlowControlErrorEventType", keyword.getKeywordValue());
                } else if (keyword.getKeywordName().equals("MurexRootContract") && !Util.isEmpty(keyword.getKeywordValue())) {
                    message.setAttribute("FlowControlErrorMurexRootContract", keyword.getKeywordValue());
                }
            }
        }

        try {
            final long messageId = ds.getRemoteBO().save(message, 0, "UploadImportMessageEngine");
            Log.info(this, "Message saved with id: " + messageId);
        } catch (final CalypsoServiceException e) {
            final String errorMessage = String.format("Could not save %s message", "FLOWCONTROL_EXPORT");
            Log.error(this, errorMessage, e);
        }
    }

    /**
     *
     * @param calypsoTrade
     * @param isUndoTerminate
     * @param errors
     * @return Existing trade in Calypso with the same ExtRef and productType LIKE 'Bond%'.
     */
    public final Trade getExistingTrade(CalypsoTrade calypsoTrade, boolean isUndoTerminate,Vector<BOException> errors) {
        //To keep core's logic, calypsotrade is temporally modified so 'Bond%' will be set inside query's bindVariable
        String rawProductType=calypsoTrade.getProductType();
        calypsoTrade.setProductType(rawProductType+"%");
        Trade trade=ValidatorUtil.getExistingTrade(calypsoTrade, isUndoTerminate,getCustomValidatorUtilQuery(isUndoTerminate), errors);
        calypsoTrade.setProductType(rawProductType);
        return trade;
    }

    /**
     * productType = ? is replaced with productType LIKE ?
     * @return Modified query
     */
    private String getCustomValidatorUtilQuery(boolean isUndoTerminate){
        if (isUndoTerminate) {
            return ValidatorUtil.EXISTING_TRADE_WHERE_CLAUSE_TERMINATED.replace("product_desc.product_type = ?", "product_desc.product_type LIKE ?");
        }else {
            return ValidatorUtil.EXISTING_TRADE_WHERE_CLAUSE.replace("product_desc.product_type = ?", "product_desc.product_type LIKE ?");
        }
    }

}
