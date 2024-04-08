package calypsox.tk.confirmation;


import calypsox.tk.confirmation.CalConfirmFieldNames.*;
import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.equity.EquityCO2ConfirmCounterpartyBuilder;
import calypsox.tk.confirmation.builder.equity.EquityCO2ConfirmationEventDataBuilder;
import calypsox.tk.confirmation.builder.equity.EquityCO2ConfirmationFinancialDataBuilder;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;


public class EquityCO2ConfirmationMsgBuilder extends CalConfirmMsgBuilder {


    /**
     * Concrete builders
     */
    CalConfirmationProcessingOrgBuilder branchDataBuilder;
    CalConfirmationCommonVarsBuilder commonVarsBuilder;
    EquityCO2ConfirmCounterpartyBuilder cptyDataBuilder;
    EquityCO2ConfirmationEventDataBuilder eventDataBuilder;
    EquityCO2ConfirmationFinancialDataBuilder regCanBuilder;


    public EquityCO2ConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        branchDataBuilder = new CalConfirmationProcessingOrgBuilder(boMessage, boTransfer, trade);
        cptyDataBuilder = new EquityCO2ConfirmCounterpartyBuilder(boMessage, boTransfer, trade);
        commonVarsBuilder = new CalConfirmationCommonVarsBuilder(boMessage, boTransfer, trade);
        eventDataBuilder = new EquityCO2ConfirmationEventDataBuilder(boMessage, boTransfer, trade);
        regCanBuilder = new EquityCO2ConfirmationFinancialDataBuilder(boMessage, boTransfer, trade);
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
        addFieldToMsgBean(EquitySpotFwdFields.MIFID_AMOUNT,commonVarsBuilder.buildMifidCostsExpensesAmount());
        addFieldToMsgBean(EquitySpotFwdFields.MIFID_CURRENCY,commonVarsBuilder.buildMifidCostsExpensesCurr());
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

        addFieldToMsgBean(EquitySpotFwdFields.OPERATION_DATE,regCanBuilder.buildOperationDate());
        addFieldToMsgBean(EquitySpotFwdFields.ENTRY_DATE,regCanBuilder.buildEntryDate());
        addFieldToMsgBean(EquitySpotFwdFields.PORTFOLIO,regCanBuilder.buildPortfolio());
        addFieldToMsgBean(EquitySpotFwdFields.DIRECTION,regCanBuilder.buildDirection());
        addFieldToMsgBean(EquitySpotFwdFields.SETTLEMENT_DATE,regCanBuilder.buildSettlementDate());
        addFieldToMsgBean(EquitySpotFwdFields.NOMINAL_VALUE_AMOUNT,regCanBuilder.buildNominalValueAmount());
        addFieldToMsgBean(EquitySpotFwdFields.TRADE_CURRENCY,regCanBuilder.buildTradeCurrency());
        addFieldToMsgBean(EquitySpotFwdFields.SETTLEMENT_CURRENCY,regCanBuilder.buildSettlementCurrency());
        addFieldToMsgBean(RegCancelRestructure.RETURN_STATUS,regCanBuilder.buildReturnStatus());
        addFieldToMsgBean(EquitySpotFwdFields.ALLOWANCE_TYPE,regCanBuilder.buildAllowanceType());
        addFieldToMsgBean(EquitySpotFwdFields.COMPILANCE_PERIOD,regCanBuilder.buildCompliancePeriod());
        addFieldToMsgBean(EquitySpotFwdFields.ALLOWANCE_NUMBER,regCanBuilder.buildAllowanceNumber());
        addFieldToMsgBean(EquitySpotFwdFields.ALLOWANCE_PURCH_PRICE,regCanBuilder.buildAllowancePurchPrice());
        addFieldToMsgBean(EquitySpotFwdFields.ALLOWANCE_PURCH_CURR,regCanBuilder.buildAllowancePurchCurr());
        addFieldToMsgBean(EquitySpotFwdFields.TOTAL_PURCH_PRICE,regCanBuilder.buildTotalPurchPrice());
        addFieldToMsgBean(EquitySpotFwdFields.TOTAL_PURCH_CURR,regCanBuilder.buildTotalPurchCurr());
        addFieldToMsgBean(EquitySpotFwdFields.BUSINESS_DAYS,regCanBuilder.buildBusinessDays());
        addFieldToMsgBean(EquitySpotFwdFields.BUY_VAT_JURISDICTION,regCanBuilder.buildBuyVatJurisdiction());
        addFieldToMsgBean(EquitySpotFwdFields.SELL_VAT_JURISDICTION,regCanBuilder.buildSellVatJurisdiction());
        addFieldToMsgBean(EquitySpotFwdFields.PAYMENT_DATE,regCanBuilder.buildPaymentDate());
        addFieldToMsgBean(EquitySpotFwdFields.DELIVERY_DATE,regCanBuilder.buildDeliveryDate());
        addFieldToMsgBean(EquitySpotFwdFields.CPTY_DELIVERY_BUSS_DAY_LOC,regCanBuilder.buildCptyDeliveryBussDayLoc());
        addFieldToMsgBean(EquitySpotFwdFields.BR_ACCOUNT,regCanBuilder.buildBrAccount());
        addFieldToMsgBean(EquitySpotFwdFields.BR_HOLDING_ACCOUNT,regCanBuilder.buildBrHoldingAccount());
        addFieldToMsgBean(EquitySpotFwdFields.CPTY_ACCOUNT,regCanBuilder.buildCptyAccount());
        addFieldToMsgBean(EquitySpotFwdFields.CPTY_HOLDING_ACCOUNT,regCanBuilder.buildCptyHoldingAccount());
    }


}
