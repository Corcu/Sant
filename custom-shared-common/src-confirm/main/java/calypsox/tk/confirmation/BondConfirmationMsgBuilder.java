package calypsox.tk.confirmation;

import calypsox.tk.confirmation.CalConfirmFieldNames.*;
import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationSDIBuilder;
import calypsox.tk.confirmation.builder.bond.*;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author dmenendd
 */
public class BondConfirmationMsgBuilder extends CalConfirmMsgBuilder{

    /**
     * Concrete builders
     */
    CalConfirmationProcessingOrgBuilder branchDataBuilder;
    CalConfirmationCommonVarsBuilder commonVarsBuilder;
    BondConfirmCounterpartyBuilder cptyDataBuilder;
    BondConfirmEventDataBuilder eventDataBuilder;
    BondConfirmFinantialDataBuilder finantialDataBuilder;
    CalConfirmationSDIBuilder sdiBuilder;

    public BondConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        branchDataBuilder = new CalConfirmationProcessingOrgBuilder(boMessage, boTransfer, trade);
        cptyDataBuilder = new BondConfirmCounterpartyBuilder(boMessage, boTransfer, trade);
        commonVarsBuilder = new BondConfirmCommonVarsBuilder(boMessage,boTransfer,trade);
        eventDataBuilder = new BondConfirmEventDataBuilder(boMessage,boTransfer,trade);
        finantialDataBuilder = new BondConfirmFinantialDataBuilder(boMessage,boTransfer,trade);
        sdiBuilder=new CalConfirmationSDIBuilder(boMessage,boTransfer,trade);
    }

    @Override
    public CalypsoConfirmationMsgBean build() {
        setCommonVars();
        setEventData();
        setBranchData();
        setCounterPartyData();
        setFinantialData();
        setSDIData();
        return messageBean;
    }

     void setBranchData(){
        addFieldToMsgBean(BranchData.BR_NAME,branchDataBuilder.buildBrName());
        addFieldToMsgBean(BranchData.BR_CODE,branchDataBuilder.buildBrEntCode());
    }

     void setSDIData(){
        addFieldToMsgBean(BranchData.BR_PAISNUCUST,sdiBuilder.getOurAgentCountry());
        addFieldToMsgBean(BranchData.BR_NUCIUDAD,sdiBuilder.getOurAgentCity());
        addFieldToMsgBean(CounterPartyData.CPTY_SUCIUDAD,sdiBuilder.getTheirAgentCity());
        addFieldToMsgBean(CounterPartyData.CPTY_PAISSUCUST,sdiBuilder.getTheirAgentCountry());
    }

     void setCounterPartyData() {
        addFieldToMsgBean(CalConfirmFieldNames.CommonVars.CONFIRM_MODE, cptyDataBuilder.buildConfirmMode());
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
        addFieldToMsgBean(EventData.INSTR_TYPE,cptyDataBuilder.buildInstrumentSubType());
    }

     void setCommonVars(){
        addFieldToMsgBean(CommonVars.REG_TRADEDATE,commonVarsBuilder.buildRegTradeDate());
        addFieldToMsgBean(CommonVars.REG_VALUEDATE,commonVarsBuilder.buildRegValueDate());
        addFieldToMsgBean(CommonVars.REG_MATURITYDATE,commonVarsBuilder.buildRegMaturity());
        addFieldToMsgBean(CommonVars.MIFID_TRADEDATE,commonVarsBuilder.buildMifidTradingTime());
        addFieldToMsgBean(CommonVars.SYSTEM_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(EventData.OPERATIONAL_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(EventData.SUSI_ID,commonVarsBuilder.buildTradeId());
    }

     void setEventData(){
        addFieldToMsgBean(EventData.ACTION,eventDataBuilder.buildAction());
        addFieldToMsgBean(EventData.OPER_ID,eventDataBuilder.buildOperId());
        addFieldToMsgBean(EventData.STRUCT_IND,eventDataBuilder.buildStructInd());
        addFieldToMsgBean(EventData.EVENT_ID,eventDataBuilder.buildEventId());
        addFieldToMsgBean(EventData.INSTRUMENT_TYPE,eventDataBuilder.buildInstrumentType());
        addFieldToMsgBean(EventData.EVENT_TYPE,eventDataBuilder.buildEventType());
        addFieldToMsgBean(EventData.EVENT_DATE,eventDataBuilder.buildEventDate());
        addFieldToMsgBean(EventData.STP_IND,eventDataBuilder.buildSTPInd());
        addFieldToMsgBean(EventData.MUST_BE_SIGNED,eventDataBuilder.buildMustBeSigned());
        addFieldToMsgBean(EventData.EXTERNAL_ID,eventDataBuilder.buildExternalId());
        addFieldToMsgBean(EventData.SEND_TO_COMPLETED,String.valueOf(1));
        addFieldToMsgBean(EventData.VALID_BO_DATE, eventDataBuilder.getValidBODate());
    }

     void setFinantialData(){
        addFieldToMsgBean(RegCancelRestructure.RETURN_STATUS,finantialDataBuilder.buildReturnStatus());
        addFieldToMsgBean(RegCancelRestructure.OPERATION_DATE,finantialDataBuilder.buildOperationDate());
        addFieldToMsgBean(RegCancelRestructure.ENTRY_DATE,finantialDataBuilder.buildEntryDate());
        addFieldToMsgBean(RegCancelRestructure.PORTFOLIO,finantialDataBuilder.buildPortfolio());
        addFieldToMsgBean(RegCancelRestructure.DIRECTION,finantialDataBuilder.buildDirection());
        addFieldToMsgBean(RegCancelRestructure.SETTLEMENT_DATE,finantialDataBuilder.buildSettlementDate());
        addFieldToMsgBean(RegCancelRestructure.QUANTITY,finantialDataBuilder.buildQuantity());
        addFieldToMsgBean(RegCancelRestructure.NOMINAL_VALUE_AMOUNT,finantialDataBuilder.buildNominalValueAmount());
        addFieldToMsgBean(RegCancelRestructure.CLEAN_PRICE,finantialDataBuilder.buildCleanPrice());
        addFieldToMsgBean(RegCancelRestructure.DIRTY_PRICE,finantialDataBuilder.buildDirtyPrice());
        addFieldToMsgBean(RegCancelRestructure.TRADE_CURRENCY,finantialDataBuilder.buildTradeCurrency());
        addFieldToMsgBean(RegCancelRestructure.SETTLEMENT_CURRENCY,finantialDataBuilder.buildSettlementCurrency());

        addFieldToMsgBean(RegCancelRestructure.DESCRIPTION_OF_THE_SECURITY_NAME,finantialDataBuilder.buildBondIssuer());
        addFieldToMsgBean(RegCancelRestructure.LOT_SIZE,finantialDataBuilder.buildLotSize());
        addFieldToMsgBean(RegCancelRestructure.DESCRIPTION_OF_THE_SECURITY_DATE,finantialDataBuilder.buildBondMaturityDate());
        addFieldToMsgBean(RegCancelRestructure.DESCRIPTION_OF_THE_SECURITY_COUPON,finantialDataBuilder.buildDescriptionOfTheSecurityCoupon());
        addFieldToMsgBean(RegCancelRestructure.INTERNAL_REFERENCE_ISIN,finantialDataBuilder.buildInternalReferenceIsin());
        addFieldToMsgBean(RegCancelRestructure.SBSD_PRODUCT,finantialDataBuilder.buildSBSDProduct());
        addFieldToMsgBean(RegCancelRestructure.UTI,finantialDataBuilder.buildSUTI());
    }
}
