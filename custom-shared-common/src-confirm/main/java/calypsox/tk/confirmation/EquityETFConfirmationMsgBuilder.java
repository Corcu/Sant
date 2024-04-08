package calypsox.tk.confirmation;


import calypsox.tk.confirmation.CalConfirmFieldNames.*;
import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.equity.EquityETFConfirmCounterpartyBuilder;
import calypsox.tk.confirmation.builder.equity.EquityETFConfirmationEventDataBuilder;
import calypsox.tk.confirmation.builder.equity.EquityETFConfirmationFinancialDataBuilder;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;


public class EquityETFConfirmationMsgBuilder extends CalConfirmMsgBuilder {


    /**
     * Concrete builders
     */
    CalConfirmationProcessingOrgBuilder branchDataBuilder;
    CalConfirmationCommonVarsBuilder commonVarsBuilder;
    EquityETFConfirmCounterpartyBuilder cptyDataBuilder;
    EquityETFConfirmationEventDataBuilder eventDataBuilder;
    EquityETFConfirmationFinancialDataBuilder regCanBuilder;


    public EquityETFConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        branchDataBuilder = new CalConfirmationProcessingOrgBuilder(boMessage, boTransfer, trade);
        cptyDataBuilder = new EquityETFConfirmCounterpartyBuilder(boMessage, boTransfer, trade);
        commonVarsBuilder = new CalConfirmationCommonVarsBuilder(boMessage, boTransfer, trade);
        eventDataBuilder = new EquityETFConfirmationEventDataBuilder(boMessage, boTransfer, trade);
        regCanBuilder = new EquityETFConfirmationFinancialDataBuilder(boMessage, boTransfer, trade);
    }


    @Override
    public CalypsoConfirmationMsgBean build() {
        setCommonVars();
        setEventData();
        setBranchData();
        setCounterPartyData();
        setRegCanData();
        return messageBean;
    }


    private void setCommonVars() {
        addFieldToMsgBean(CommonVars.REG_TRADEDATE,commonVarsBuilder.buildRegTradeDate());
        addFieldToMsgBean(CommonVars.REG_VALUEDATE,commonVarsBuilder.buildRegValueDate());
        addFieldToMsgBean(CommonVars.REG_MATURITYDATE,commonVarsBuilder.buildRegValueDate());
        addFieldToMsgBean(CommonVars.CONFIRM_MODE,commonVarsBuilder.buildRegConfirmation());
        addFieldToMsgBean(CommonVars.MIFID_TRADEDATE,commonVarsBuilder.buildMifidTradingTime());
        addFieldToMsgBean(EquityETFFields.MIFID_AMOUNT,commonVarsBuilder.buildMifidCostsExpensesAmount());
        addFieldToMsgBean(EquityETFFields.MIFID_CURRENCY,commonVarsBuilder.buildMifidCostsExpensesCurr());
        addFieldToMsgBean(CommonVars.SYSTEM_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(EventData.OPERATIONAL_SOURCE,commonVarsBuilder.buildSystemSource());
    }


    private void setEventData() {
        addFieldToMsgBean(EventData.ACTION,eventDataBuilder.buildAction());
        addFieldToMsgBean(EventData.OPER_ID,eventDataBuilder.buildOperId());
        addFieldToMsgBean(EventData.STRUCT_IND,eventDataBuilder.buildStructInd());
        addFieldToMsgBean(EventData.EVENT_ID,eventDataBuilder.buildEventId());
        addFieldToMsgBean(EventData.INSTRUMENT_TYPE,eventDataBuilder.buildInstrumentType());
        addFieldToMsgBean(EventData.EVENT_TYPE,eventDataBuilder.buildEventType());
        addFieldToMsgBean(EventData.EVENT_DATE,eventDataBuilder.buildEventDate());
        addFieldToMsgBean(EventData.STP_IND,eventDataBuilder.buildSTPInd());
        addFieldToMsgBean(EventData.MUST_BE_SIGNED,eventDataBuilder.buildMustBeSigned());
        addFieldToMsgBean(EventData.INSTR_TYPE,eventDataBuilder.buildInstrumentSubType());
        addFieldToMsgBean(EventData.UTI_ID,eventDataBuilder.buildUTI());
        addFieldToMsgBean(EventData.EXTERNAL_ID,eventDataBuilder.buildExternalId());
        addFieldToMsgBean(EventData.SEND_TO_COMPLETED,eventDataBuilder.buildSendToCompleted());
        addFieldToMsgBean(EventData.VALID_BO_DATE, eventDataBuilder.getValidBODate());
    }


    private void setBranchData() {
        addFieldToMsgBean(BranchData.BR_NAME,branchDataBuilder.buildBrEntName());
        addFieldToMsgBean(BranchData.BR_CODE,branchDataBuilder.buildBrEntCode());
    }


    private void setCounterPartyData() {
        addFieldToMsgBean(CounterPartyData.CPTY_NAME,cptyDataBuilder.buildCptyName());
        addFieldToMsgBean(CounterPartyData.CPTY_CODE,cptyDataBuilder.buildCptyCode());
        addFieldToMsgBean(CounterPartyData.CPTY_ADDRESS,cptyDataBuilder.buildCptyAddress());
        addFieldToMsgBean(CounterPartyData.CPTY_CITY,cptyDataBuilder.buildCptyCity());
        addFieldToMsgBean(CounterPartyData.CPTY_FAX,cptyDataBuilder.buildCptyFax());
        addFieldToMsgBean(CounterPartyData.CPTY_EMAIL,cptyDataBuilder.buildCptyEmail());
        addFieldToMsgBean(CounterPartyData.CPTY_EMAIL_CHASE,cptyDataBuilder.buildCptyEmailChase());
        addFieldToMsgBean(CounterPartyData.CPTY_LANGUAGE,cptyDataBuilder.buildLanguage());
        addFieldToMsgBean(CounterPartyData.CPTY_CONTRACTDATE,cptyDataBuilder.buildCptyContractDate());
        addFieldToMsgBean(CounterPartyData.CPTY_CONTRACTTYPE,cptyDataBuilder.buildCptyContractType());
        addFieldToMsgBean(CounterPartyData.CUSTOMER,cptyDataBuilder.buildCustomer());
    }


    private void setRegCanData() {
        addFieldToMsgBean(EquityETFFields.OPERATION_DATE,regCanBuilder.buildOperationDate());
        addFieldToMsgBean(EquityETFFields.ENTRY_DATE,regCanBuilder.buildEntryDate());
        addFieldToMsgBean(EquityETFFields.PORTFOLIO,regCanBuilder.buildPortfolio());
        addFieldToMsgBean(EquityETFFields.DIRECTION,regCanBuilder.buildDirection());
        addFieldToMsgBean(EquityETFFields.SETTLEMENT_DATE,regCanBuilder.buildSettlementDate());
        addFieldToMsgBean(EquityETFFields.NOMINAL_VALUE_AMOUNT,regCanBuilder.buildNominalValueAmount());
        addFieldToMsgBean(EquityETFFields.TRADE_CURRENCY,regCanBuilder.buildTradeCurrency());
        addFieldToMsgBean(EquityETFFields.SETTLEMENT_CURRENCY,regCanBuilder.buildSettlementCurrency());
        addFieldToMsgBean(RegCancelRestructure.RETURN_STATUS,regCanBuilder.buildReturnStatus());
        addFieldToMsgBean(EquityETFFields.COMPILANCE_PERIOD,regCanBuilder.buildCompliancePeriod());
        addFieldToMsgBean(EquityETFFields.ALLOWANCE_NUMBER,regCanBuilder.buildAllowanceNumber());
        addFieldToMsgBean(EquityETFFields.ALLOWANCE_PURCH_PRICE,regCanBuilder.buildAllowancePurchPrice());
        addFieldToMsgBean(EquityETFFields.ALLOWANCE_PURCH_CURR,regCanBuilder.buildAllowancePurchCurr());
        addFieldToMsgBean(EquityETFFields.TOTAL_PURCH_PRICE,regCanBuilder.buildTotalPurchPrice());
        addFieldToMsgBean(EquityETFFields.TOTAL_PURCH_CURR,regCanBuilder.buildTotalPurchCurr());
        addFieldToMsgBean(EquityETFFields.BUSINESS_DAYS,regCanBuilder.buildBusinessDays());
        addFieldToMsgBean(EquityETFFields.BUY_VAT_JURISDICTION,regCanBuilder.buildBuyVatJurisdiction());
        addFieldToMsgBean(EquityETFFields.SELL_VAT_JURISDICTION,regCanBuilder.buildSellVatJurisdiction());
        addFieldToMsgBean(EquityETFFields.PAYMENT_DATE,regCanBuilder.buildPaymentDate());
        addFieldToMsgBean(EquityETFFields.DELIVERY_DATE,regCanBuilder.buildDeliveryDate());
        addFieldToMsgBean(EquityETFFields.CPTY_DELIVERY_BUSS_DAY_LOC,regCanBuilder.buildCptyDeliveryBussDayLoc());
        addFieldToMsgBean(EquityETFFields.BR_ACCOUNT,regCanBuilder.buildBrAccount());
        addFieldToMsgBean(EquityETFFields.BR_HOLDING_ACCOUNT,regCanBuilder.buildBrHoldingAccount());
        addFieldToMsgBean(EquityETFFields.CPTY_ACCOUNT,regCanBuilder.buildCptyAccount());
        addFieldToMsgBean(EquityETFFields.CPTY_HOLDING_ACCOUNT,regCanBuilder.buildCptyHoldingAccount());

        addFieldToMsgBean(EquityETFFields.CORPORATE,regCanBuilder.buildCorporate());
        addFieldToMsgBean(EquityETFFields.ISIN,regCanBuilder.buildIsin());
        addFieldToMsgBean(EquityETFFields.BROKER,regCanBuilder.buildBroker());
        addFieldToMsgBean(EquityETFFields.EXCHANGE,regCanBuilder.buildExchange());
        addFieldToMsgBean(EquityETFFields.GROSS_AMOUNT,regCanBuilder.buildGrossAmount());
        addFieldToMsgBean(EquityETFFields.BRK_FEE,regCanBuilder.buildFeeAmount("BRK_FEE"));
        addFieldToMsgBean(EquityETFFields.EXCHANGE_FEE ,regCanBuilder.buildFeeAmount("EXCHANGE_FEE"));
        addFieldToMsgBean(EquityETFFields.NET_AMOUNT  ,regCanBuilder.buildNetAmount());
        addFieldToMsgBean(EquityETFFields.REG_TRADETIME, regCanBuilder.buildRegTradetime());
        addFieldToMsgBean(EquityETFFields.EXECUTION_CENTER, regCanBuilder.buildExecutionCenter());
        addFieldToMsgBean(EquityETFFields.CPTY_COUNTRY, regCanBuilder.buildCptyCountry());
    }


}
