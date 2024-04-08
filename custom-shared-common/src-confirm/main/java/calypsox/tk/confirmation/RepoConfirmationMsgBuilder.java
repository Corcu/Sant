package calypsox.tk.confirmation;

import calypsox.tk.confirmation.CalConfirmFieldNames.*;
import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationSDIBuilder;
import calypsox.tk.confirmation.builder.repo.RepoConfirmCommonVarsBuilder;
import calypsox.tk.confirmation.builder.repo.RepoConfirmCounterpartyBuilder;
import calypsox.tk.confirmation.builder.repo.RepoConfirmEventDataBuilder;
import calypsox.tk.confirmation.builder.repo.RepoConfirmFinantialDataBuilder;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

import java.util.AbstractMap;

/**
 * @author aalonsop
 */
public class RepoConfirmationMsgBuilder extends CalConfirmMsgBuilder{

    /**
     * Concrete builders
     */
    CalConfirmationProcessingOrgBuilder branchDataBuilder;
    CalConfirmationCommonVarsBuilder commonVarsBuilder;
    RepoConfirmCounterpartyBuilder cptyDataBuilder;
    RepoConfirmEventDataBuilder eventDataBuilder;
    RepoConfirmFinantialDataBuilder regCancBuilder;
    CalConfirmationSDIBuilder sdiBuilder;

    public RepoConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        branchDataBuilder = new CalConfirmationProcessingOrgBuilder(boMessage, boTransfer, trade);
        cptyDataBuilder = new RepoConfirmCounterpartyBuilder(boMessage, boTransfer, trade);
        commonVarsBuilder = new RepoConfirmCommonVarsBuilder(boMessage,boTransfer,trade);
        eventDataBuilder = new RepoConfirmEventDataBuilder(boMessage,boTransfer,trade);
        regCancBuilder = new RepoConfirmFinantialDataBuilder(boMessage,boTransfer,trade);
        sdiBuilder=new CalConfirmationSDIBuilder(boMessage,boTransfer,trade);
    }

    @Override
    public CalypsoConfirmationMsgBean build() {
        setCommonVars();
        setEventData();
        setBranchData();
        setCounterPartyData();
        setRegCancData();
        setSDIData();
        return messageBean;
    }

    private void setBranchData(){
        addFieldToMsgBean(BranchData.BR_NAME,branchDataBuilder.buildBrName());
        addFieldToMsgBean(BranchData.BR_CODE,branchDataBuilder.buildBrEntCode());
    }

    private void setSDIData(){
        addFieldToMsgBean(BranchData.BR_PAISNUCUST,sdiBuilder.getOurAgentCountry());
        addFieldToMsgBean(BranchData.BR_NUCIUDAD,sdiBuilder.getOurAgentCity());
        addFieldToMsgBean(CounterPartyData.CPTY_SUCIUDAD,sdiBuilder.getTheirAgentCity());
        addFieldToMsgBean(CounterPartyData.CPTY_PAISSUCUST,sdiBuilder.getTheirAgentCountry());
        addFieldToMsgBean("SEC_INSTR_CPTY_ACCWITH[1]",sdiBuilder.getTheirAgentName());
        addFieldToMsgBean("SEC_INSTR_BR_ACCWITH[1]",sdiBuilder.getOurAgentName());
        addFieldToMsgBean("SEC_INSTR_BR_ACCWITHNU[1]",sdiBuilder.getOurAgentAccAndName());
        addFieldToMsgBean("SEC_INSTR_CPTY_ACCWITHNU[1]",sdiBuilder.getTheirAgentAccAndName());
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
        addFieldToMsgBean(EventData.INSTR_TYPE,cptyDataBuilder.buildInstrumentSubType());
    }

    private void setCommonVars(){
        addFieldToMsgBean(CommonVars.REG_TRADEDATE,commonVarsBuilder.buildRegTradeDate());
        addFieldToMsgBean(CommonVars.REG_VALUEDATE,commonVarsBuilder.buildRegValueDate());
        addFieldToMsgBean(CommonVars.REG_MATURITYDATE,commonVarsBuilder.buildRegMaturity());
        addFieldToMsgBean(CommonVars.CONFIRM_MODE,commonVarsBuilder.buildRegConfirmation());
        addFieldToMsgBean(CommonVars.MIFID_TRADEDATE,commonVarsBuilder.buildMifidTradingTime());

        addFieldToMsgBean(CommonVars.SYSTEM_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(EventData.OPERATIONAL_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(EventData.SUSI_ID,commonVarsBuilder.buildTradeId());
    }

    private void setEventData(){
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

    private void setRegCancData(){
        addFieldToMsgBean("FIXED_IND",regCancBuilder.buildFixedRateInd());
        addFieldToMsgBean("IR_PERIOD_RATE[1]",regCancBuilder.buildFixedIRPeriodRate());
        addFieldToMsgBean("IR_PERIOD_RATE_FLOATING[1]",regCancBuilder.buildFloatingIRPeriodRate());
        addFieldToMsgBean("REPO_UNDRLYING_SEC_CURRENCY[1]",regCancBuilder.buildUnderlyingSecCurrency());
        addFieldToMsgBean("REPO_UNDRLYING_NOMINAL_AMOUNT[1]",regCancBuilder.buildRepoUnderlyingNominalAmount());
        addFieldToMsgBean(RegCancelRestructure.REPO_INITIAL_SETTLE_AMOUNT,regCancBuilder.buildRepoInitialSettleAmount());
        addFieldToMsgBean(RegCancelRestructure.REPO_FINAL_SETTLE_AMOUNT,regCancBuilder.buildRepoFinalSettleAmount());
        addFieldToMsgBean(RegCancelRestructure.REPO_EMISION1,regCancBuilder.buildBondName());
        addFieldToMsgBean("REPO_UNDRLYING_SEC_CODE[1]",regCancBuilder.buildInternalReferenceIsin());
        addFieldToMsgBean("REPO_UNDRLYING_DIRTY_PRICE[1]",regCancBuilder.buildRepoCleanPrice());
        addFieldToMsgBean(RegCancelRestructure.REPO_DIRECTION,regCancBuilder.buildDirection());
        addFieldToMsgBean(RegCancelRestructure.FIX_INCOME_DIRECTION,regCancBuilder.buildDirection());
        addFieldToMsgBean(RegCancelRestructure.REPO_DUAL_CURRENCY,regCancBuilder.buildDualCurrency());
        addFieldToMsgBean(RegCancelRestructure.REPO_DUAL_AMOUNT,regCancBuilder.buildDualAmount());
        addFieldToMsgBean(RegCancelRestructure.RETURN_STATUS,regCancBuilder.buildReturnStatus());
    }

    void addFieldToMsgBean(String fieldName, String fieldValue){
        AbstractMap.SimpleEntry<String, String> field = new AbstractMap.SimpleEntry<>(fieldName, fieldValue);
        messageBean.addElement(field);
    }
}
