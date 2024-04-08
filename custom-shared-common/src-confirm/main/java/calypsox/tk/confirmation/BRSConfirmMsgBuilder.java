package calypsox.tk.confirmation;

import calypsox.tk.confirmation.CalConfirmFieldNames.*;
import calypsox.tk.confirmation.builder.CalConfirmationCounterpartyBuilder;
import calypsox.tk.confirmation.builder.CalConfirmationProcessingOrgBuilder;
import calypsox.tk.confirmation.builder.brs.BRSConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.builder.brs.BRSConfirmationEventDataBuilder;
import calypsox.tk.confirmation.builder.brs.BRSConfirmationFinantialDataBuilder;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

/**
 * @author aalonsop
 */
public class BRSConfirmMsgBuilder extends CalConfirmMsgBuilder {

    /**
     * Concrete builders
     */
    CalConfirmationProcessingOrgBuilder branchDataBuilder;
    BRSConfirmationCommonVarsBuilder commonVarsBuilder;
    CalConfirmationCounterpartyBuilder cptyDataBuilder;
    BRSConfirmationEventDataBuilder eventDataBuilder;
    BRSConfirmationFinantialDataBuilder regCancReestructBuilder;

    public BRSConfirmMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        branchDataBuilder = new CalConfirmationProcessingOrgBuilder(boMessage, boTransfer, trade);
        cptyDataBuilder = new CalConfirmationCounterpartyBuilder(boMessage, boTransfer, trade);
        commonVarsBuilder = new BRSConfirmationCommonVarsBuilder(boMessage, boTransfer, trade);
        eventDataBuilder = new BRSConfirmationEventDataBuilder(boMessage, boTransfer, trade);
        regCancReestructBuilder = new BRSConfirmationFinantialDataBuilder(boMessage, boTransfer, trade);
    }

    @Override
    public CalypsoConfirmationMsgBean build() {
        setCommonVars();
        setEventData();
        setBranchData();
        setCounterPartyData();
        setRegCancelRestructure();
        return messageBean;
    }

    private void setCommonVars() {
        addFieldToMsgBean(CommonVars.REG_TRADEDATE,commonVarsBuilder.buildRegTradeDate());
        addFieldToMsgBean(CommonVars.REG_VALUEDATE,commonVarsBuilder.buildRegValueDate());
        addFieldToMsgBean(CommonVars.CONFIRM_MODE,commonVarsBuilder.buildRegConfirmation());
        addFieldToMsgBean(CommonVars.TRS_MATURITY,commonVarsBuilder.buildRegMaturity());
        addFieldToMsgBean(CommonVars.MIFID_TRADING_TIME,commonVarsBuilder.buildMifidTradingTime());
        addFieldToMsgBean(CommonVars.MIFID_COSTS_EXPENSES_AMOUNT,commonVarsBuilder.buildMifidCostsExpensesAmount());
        addFieldToMsgBean(CommonVars.MIFID_COSTS_EXPENSES_CURR,commonVarsBuilder.buildMifidCostsExpensesCurr());
        addFieldToMsgBean(CommonVars.MIFID_COSTS_EXPENSES_AMOUNT_MODIF,commonVarsBuilder.buildMifidCostsExpensesAmountModif());
        addFieldToMsgBean(CommonVars.MIFID_COSTS_EXPENSES_CURR_MODIF,commonVarsBuilder.buildMifidCostsExpensesCurrModif());
        addFieldToMsgBean(CommonVars.MIFID_TRADING_TIME_MODIF,commonVarsBuilder.buildMifidTradingTimeModif());
        addFieldToMsgBean(CommonVars.SYSTEM_SOURCE,commonVarsBuilder.buildSystemSource());
        addFieldToMsgBean(CommonVars.BUSINESS_DAYS,commonVarsBuilder.buildBusinessDays());
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
        addFieldToMsgBean(EventData.WAIT_CONFIRM,eventDataBuilder.buildWaitConfirm());
        addFieldToMsgBean(EventData.PARENTEVENT_ID,eventDataBuilder.buildParentEventId());
        addFieldToMsgBean(EventData.ORIGINAL_OPER_ID,eventDataBuilder.buildOriginalOperId());
        addFieldToMsgBean(EventData.MANUAL_CHASING,eventDataBuilder.buildManualChasing());
        addFieldToMsgBean(EventData.MUST_BE_SIGNED,eventDataBuilder.buildMustBeSigned());
        addFieldToMsgBean(EventData.INSTR_TYPE,eventDataBuilder.buildInstrumentSubType());
        addFieldToMsgBean(EventData.USI_ID,eventDataBuilder.buildUSI());
        addFieldToMsgBean(EventData.UTI_ID,eventDataBuilder.buildUTI());
        addFieldToMsgBean(EventData.EXTERNAL_ID,eventDataBuilder.buildExternalId());
    }

    private void setBranchData() {
        addFieldToMsgBean(BranchData.BR_NAME,branchDataBuilder.buildBrName());
        addFieldToMsgBean(BranchData.BR_ENTNAME,branchDataBuilder.buildBrEntName());
        addFieldToMsgBean(BranchData.BR_CODE,branchDataBuilder.buildBrEntCode());
        addFieldToMsgBean(BranchData.BR_ADDRESS,branchDataBuilder.buildBrAddress());
        addFieldToMsgBean(BranchData.BR_CITY,branchDataBuilder.buildBrCity());
        addFieldToMsgBean(BranchData.BR_PC,branchDataBuilder.buildBrPC());
        addFieldToMsgBean(BranchData.BR_COUNTRY,branchDataBuilder.buildBrCountry());
        addFieldToMsgBean(BranchData.BR_FAX,branchDataBuilder.buildBrFax());
        addFieldToMsgBean(BranchData.BR_EMAIL,branchDataBuilder.buildBrEmail());
    }

    private void setCounterPartyData() {
        addFieldToMsgBean(CounterPartyData.CPTY_NAME,cptyDataBuilder.buildCptyName());
        addFieldToMsgBean(CounterPartyData.CPTY_CODE,cptyDataBuilder.buildCptyCode());
        addFieldToMsgBean(CounterPartyData.CPTY_ADDRESS,cptyDataBuilder.buildCptyAddress());
        addFieldToMsgBean(CounterPartyData.CPTY_CITY,cptyDataBuilder.buildCptyCity());
        addFieldToMsgBean(CounterPartyData.CPTY_PC,cptyDataBuilder.buildCptyPostalCode());
        addFieldToMsgBean(CounterPartyData.CPTY_COUNTRY,cptyDataBuilder.buildCptyCountry());
        addFieldToMsgBean(CounterPartyData.CPTY_FAX,cptyDataBuilder.buildCptyFax());
        addFieldToMsgBean(CounterPartyData.CPTY_EMAIL,cptyDataBuilder.buildCptyEmail());
        addFieldToMsgBean(CounterPartyData.CPTY_EMAIL_CHASE,cptyDataBuilder.buildCptyEmailChase());
        addFieldToMsgBean(CounterPartyData.CPTY_CONTACT,cptyDataBuilder.buildCptyContact());
        addFieldToMsgBean(CounterPartyData.CORPORATE_IND,cptyDataBuilder.buildCorporateIndicator());
        addFieldToMsgBean(CounterPartyData.CPTY_LANGUAGE,cptyDataBuilder.buildLanguage());
        addFieldToMsgBean(CounterPartyData.CPTY_CONTRACTDATE,cptyDataBuilder.buildCptyContractDate());
        addFieldToMsgBean(CounterPartyData.CPTY_CONTRACTTYPE,cptyDataBuilder.buildCptyContractType());
        addFieldToMsgBean(CounterPartyData.CUSTOMER,cptyDataBuilder.buildCustomer());
    }

    private void setRegCancelRestructure(){
        addFieldToMsgBean(RegCancelRestructure.SETTLEMENT_CURRENCY,regCancReestructBuilder.buildSettlementCurrency());
        addFieldToMsgBean(RegCancelRestructure.RELEVANT_JURISDICTION,regCancReestructBuilder.buildRelevantJurisdiction());
        addFieldToMsgBean(RegCancelRestructure.BOND_TRADE_ID,regCancReestructBuilder.buildBondTradeId());
        addFieldToMsgBean(RegCancelRestructure.INTERNAL_REFERENCE_ISIN,regCancReestructBuilder.buildInternalReferenceIsin());
        addFieldToMsgBean(RegCancelRestructure.BOND_ISSUER,regCancReestructBuilder.buildBondIssuer());
        addFieldToMsgBean(RegCancelRestructure.FACE_AMOUNT,regCancReestructBuilder.buildFaceAmount());
        addFieldToMsgBean(RegCancelRestructure.CLEAN_IND,regCancReestructBuilder.buildPriceFixingIndicator());
        addFieldToMsgBean(RegCancelRestructure.INITIAL_PRICE,regCancReestructBuilder.buildInitialPrice());
        addFieldToMsgBean(RegCancelRestructure.FINAL_VALUE_DATE,regCancReestructBuilder.buildFinalValuationDate());
        addFieldToMsgBean(RegCancelRestructure.TR_PERIOD_PAYMENTDATE,regCancReestructBuilder.buildTRPeriodPaymentDate());
        addFieldToMsgBean(RegCancelRestructure.COUPON_TYPE,regCancReestructBuilder.buildCouponType());
        addFieldToMsgBean(RegCancelRestructure.SPREAD_ADD,regCancReestructBuilder.buildSpreadAdd());
        addFieldToMsgBean(RegCancelRestructure.FLOATING_RATE_OPTION,regCancReestructBuilder.buildFloatingRateOption());
        addFieldToMsgBean(RegCancelRestructure.SWAP_RATEINDEX_TENOR,regCancReestructBuilder.buildSwapRateIndexTenor());
        addFieldToMsgBean(RegCancelRestructure.AMOUNT_PAYER,regCancReestructBuilder.buildAmountPayer());
        addFieldToMsgBean(RegCancelRestructure.CALCULATION_AGENT,regCancReestructBuilder.buildCalculationAgent());
        addFieldToMsgBean(RegCancelRestructure.FINAL_PRICE_DIRECTION,regCancReestructBuilder.buildFinalPriceDirection());
        addFieldToMsgBean(RegCancelRestructure.FIX_PERIOD_PAYMENTDATE_COMPLETE,regCancReestructBuilder.buildFixPeriodPaymentDateComplete());
        addFieldToMsgBean(RegCancelRestructure.FIX_PERIOD_PAYMENTDATE_DAY,regCancReestructBuilder.buildFixPeriodPaymentDay());
        addFieldToMsgBean(RegCancelRestructure.RATE_DAY_COUNT_FRACTION,regCancReestructBuilder.buildRateDayCountFraction());
        addFieldToMsgBean(RegCancelRestructure.RATE_PAYER,regCancReestructBuilder.buildRatePayer());
        addFieldToMsgBean(RegCancelRestructure.ROLL_CONVENTION,regCancReestructBuilder.buildRollConvention());
        addFieldToMsgBean(RegCancelRestructure.ROLL_CONVENTION_MATURITY,regCancReestructBuilder.buildRollConventionMaturity());
        addFieldToMsgBean(RegCancelRestructure.PREMIUM_TYPE,regCancReestructBuilder.buildPremiumType());
        addFieldToMsgBean(RegCancelRestructure.PRIMARY_INCOME_PAYMENT_TYPE,regCancReestructBuilder.buildPrimaryIncomePaymentType());
        addFieldToMsgBean(RegCancelRestructure.PRIMARY_LEG_CONFIG,regCancReestructBuilder.buildPrimaryLegConfig());
        addFieldToMsgBean(RegCancelRestructure.PRIMARY_RETURN_PAYMENT_TYPE,regCancReestructBuilder.buildPrimaryReturnPaymentType());
        addFieldToMsgBean(RegCancelRestructure.RETURN_STATUS,regCancReestructBuilder.buildReturnStatus());
        addFieldToMsgBean(RegCancelRestructure.SECONDARY_LEG_CONFIG,regCancReestructBuilder.buildSecondaryLegConfig());
        addFieldToMsgBean(RegCancelRestructure.BOND_MATURITY_DATE,regCancReestructBuilder.buildBondMaturityDate());
        addFieldToMsgBean(RegCancelRestructure.EARLY_AMOUNT,regCancReestructBuilder.buildEarlyAmount());
        addFieldToMsgBean(RegCancelRestructure.EARLY_CURRENCY,regCancReestructBuilder.buildEarlyCurrency());
        addFieldToMsgBean(RegCancelRestructure.EARLY_INTSETTDATE,regCancReestructBuilder.buildEarlySettleDate());
        addFieldToMsgBean(RegCancelRestructure.TERMINATION_FEE_DATE,regCancReestructBuilder.buildTerminationFeeDate());
        addFieldToMsgBean(RegCancelRestructure.SWAP_COUPON_FRECUENCY,regCancReestructBuilder.buildSwapCouponFreqCode());
        addFieldToMsgBean(RegCancelRestructure.SWAP_FIRST_INTEREST_PAYMENT_DATE,regCancReestructBuilder.buildSwapFirstIntPaymentDate());
    }
}
