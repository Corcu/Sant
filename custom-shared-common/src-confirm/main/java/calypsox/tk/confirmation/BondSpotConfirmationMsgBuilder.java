package calypsox.tk.confirmation;

import calypsox.tk.confirmation.builder.bond.BondConfirmProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.bond.BondSpotConfirmEventDataBuilder;
import calypsox.tk.confirmation.builder.bond.BondSpotConfirmFinantialDataBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BondSpotConfirmationMsgBuilder extends BondConfirmationMsgBuilder{


    public BondSpotConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.eventDataBuilder=new BondSpotConfirmEventDataBuilder(boMessage,boTransfer,trade);
        this.branchDataBuilder=new BondConfirmProcessingOrgBuilder(boMessage,boTransfer,trade);
        this.finantialDataBuilder=new BondSpotConfirmFinantialDataBuilder(boMessage, boTransfer, trade);
    }

    @Override
    void setEventData(){
        super.setEventData();
        BondSpotConfirmEventDataBuilder builder= (BondSpotConfirmEventDataBuilder) this.eventDataBuilder;
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ACTION_FLOW,builder.buildActionFlow());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ENTITY_ID,builder.buildEntityId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ACCOUNTING_CENTER_ID,builder.buildAccountingCenterId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.PRODUCT_ID,builder.buildProductId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.SUBPRODUCT_ID,builder.buildSubProductId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.TRADE_ID,builder.buildPartenonTradeId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.EVENT_ID_FLOW,builder.buildEventIdFlow());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.EVENT_TYPE_ID,builder.buildEventTypeId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CPTY_TYPE,this.cptyDataBuilder.buildCptyType());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CPTY_CODE_FLOW,this.cptyDataBuilder.buildCptyCodeFlow());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CPTY_CONTRACT_DATE,this.finantialDataBuilder.buildCptyContractDate());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.TRADING_PLATFORM,builder.buildElectPlat());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.FO_CODE,builder.buildFOCode());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.REFERENCE_ID,this.finantialDataBuilder.buildReferenceId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ACCRUED_INTEREST,this.finantialDataBuilder.buildAccruedInterest());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ACCRUAL,this.finantialDataBuilder.buildAccrual());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.EX_COUPON_DATE,this.finantialDataBuilder.buildExCouponDate());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.YIELD,this.finantialDataBuilder.buildYield());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.BLOCK_IDENTIFIER,builder.buildBlockIdentifier());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.BLOCK_OPERATION_NUMBER,builder.buildBlockOperationNumber());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CORPORATE_EVENT_IND,builder.buildCorporateEventId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ALLOCATION_INDICATOR,builder.buildAllocationIndicator());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.BLOCK_ALLOCATION_IND,builder.buildBlockAllocationInd());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CASH_AMOUNT,this.finantialDataBuilder.buildTotalAmount());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.METHOD_OF_PAYMENT,this.finantialDataBuilder.buildMethodOfPayment());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CONTRACT_ID,builder.buildContractId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.GISTP_GLOBAL_ID,builder.buildGistGlobalId());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.GISTP_ID_CREATOR,builder.buildGistIdCreator());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.RETAIL_IND,builder.buildRetailIndicator());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.MIFID_TRADING_DATE,this.commonVarsBuilder.buildRegTradeDate());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.MIFID_TRADING_TIME,this.commonVarsBuilder.buildMifidTradingTime());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.MIFID_COSTS_EXPENSES_AMOUNT,this.finantialDataBuilder.buildMifidCostsExpensesAmount());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.MIFID_COSTS_EXPENSES_CURR,this.finantialDataBuilder.buildMifidCostsExpensesCurr());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.PRINCIPAL,this.finantialDataBuilder.buildPrincipal());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.DUAL_TOTAL_SETTLEMENT_AMOUNT,this.finantialDataBuilder.buildDualTotalSettlementAmt());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.DUAL_SETTLEMENT_CURRENCY,this.finantialDataBuilder.buildDualTotalSettlementCurrency());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.ALERT_SETTLEMENT_MODEL_NAME,((BondConfirmProcessingOrgBuilder)this.branchDataBuilder).buildAlertSettlementModelName());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.SETTLEMENT_HOUSE_CODE,(this.cptyDataBuilder).buildSettlementHouseCode());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.S_T_C_NAME,this.sdiBuilder.getOurSDIBICS());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.S_T_C_CITY,this.sdiBuilder.getOurSDICity());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.S_T_C_AN,this.sdiBuilder.getOurSDIAccount());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.C_T_C_NAME,this.sdiBuilder.getTheirSDIBICS());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.C_T_C_CITY,this.sdiBuilder.getTheirSDICity());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.C_T_C_AN,this.sdiBuilder.getTheirSDIAccount());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.NUMBER_OF_DAYS_ACCRUED,((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getNumberOfDaysAccrued());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.REFERENCE_SUBTYPE, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getReferenceSubtype());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.DATED_DATE, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getDatedDate());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.DESCRIPTION_OF_THE_SECURITY, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getDescriptionOfTheSecurity());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.INTEREST_RATE_EMISION_CLASS, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getInterestRateEmisionClass());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.BASIS, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getBasis());
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.QUANTITY_TYPE, ((BondSpotConfirmFinantialDataBuilder)this.finantialDataBuilder).getQuantityType());
    }

    @Override
    void setCounterPartyData() {
        super.setCounterPartyData();
        addFieldToMsgBean(CalConfirmFieldNames.BondSpotFields.CPTY_EMAIL_ALTERNATIVO,this.cptyDataBuilder.buildAlternEmail());
    }

}
