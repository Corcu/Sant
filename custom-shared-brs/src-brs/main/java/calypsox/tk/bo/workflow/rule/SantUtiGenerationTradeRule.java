package calypsox.tk.bo.workflow.rule;


import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import java.rmi.RemoteException;
import java.util.*;


public class SantUtiGenerationTradeRule implements WfTradeRule {


    private static final String LE_ATTR_US_COUNTERPARTY_DOMICILE = "US_COUNTERPARTY_DOMICILE";
    private static final String LE_ATTR_BRANCH_US_SWAP_DEALER = "BRANCH_US_SWAP_DEALER";
    private static final String LE_ATTR_AFFILIATE_US_GUARANTEED = "AFFILIATE_US_GUARANTEED";
    private static final String LE_ATTR_CONDUIT_US_PERSON = "CONDUIT_US_PERSON";
    private static final String LE_ATTR_SWAP_DEALER = "SWAP_DEALER";
    private static final String LE_ATTR_LEI = "LEI";
    private static final String LE_ATTR_EMIR_FULL_DELEG = "EMIR_FULL_DELEG";
    private static final String LE_ATTR_UTI_GENERATION_PARTY = "UTI_GENERATION_PARTY";
    private static final String SANTANDER = "Santander";
    private static final String COUNTERPARTY = "Counterparty";
    private static final String COUNTERPARTY_ALWAYS = "Counterparty_Always";
    private static final String TRADE_KWRD_UTI_REFERENCE = "UTI_REFERENCE";
    private static final String TRADE_KWRD_PRIOR_UTI_PREFIX = "PriorUTIPrefix";
    private static final String TRADE_KWRD_PRIOR_UTI_VALUE = "PriorUTIValue";
    private static final String TRADE_KWRD_MX_LAST_EVENT = "MxLastEvent";
    private static final String TRADE_KWRD_MX_TRADE_ID = "MurexTradeID";
    private static final String TRADE_KWRD_MX_TRANFER_FROM = "MurexTransferFrom";
    private static final String TRADE_KWRD_MX_TRANFER_TO = "MurexTransferTo";
    private static final String EVENT_NOVATION = "mxContractEventICOUNTERPART_AMENDMENT";
    private static final String EVENT_CANCEL_REISSUE = "mxContractEventICANCEL_REISSUE";
    private static final String EVENT_PORTFOLIO_ASSIGMENT = "mxContractEventIPORTFOLIO_ASSIGNMENT";
    private static final String ACTION_NONE = "NONE";
    private static final String COUNTRY_MEXICO = "MEXICO";
    private static final String COUNTRY_CANADA = "CANADA";
    private static final String LEI_SCALE = "ZYXWVUTSRQPONMLKJIHGFEDCBA9876543210";
    private static final String CALSTC = "CALSTC";
    private static final int UTI_LENGTH = 52;

    private LegalEntity po = null;
    private LegalEntity cp = null;
    private Collection<LegalEntityAttribute> poAttrs = null;
    private Collection<LegalEntityAttribute> cpAttrs = null;

    private boolean applyRegulations = true;
    private boolean utiGenerationBySantander = false;
    private boolean bondPayFloating = false;
    private boolean bondRecFloating = false;
    private boolean bondPayFixed = false;
    private boolean bondRecFixed = false;
    private boolean swapRecFixed = false;
    private boolean swapPayFixed = false;
    private boolean swapRecFloating = false;
    private boolean swapPayFloating = false;



    @Override
	public boolean check(final TaskWorkflowConfig taskworkflowconfig, final Trade newTrade, final Trade oldrade, final Vector vector,
						final DSConnection dsconnection, final Vector vector1, final Task task, final Object obj, final Vector vector2) {
		boolean rst = false;
	    po = newTrade.getBook().getLegalEntity();
        cp = newTrade.getCounterParty();
	    poAttrs = BOCache.getLegalEntityAttributes(dsconnection, po.getId());
	    cpAttrs = BOCache.getLegalEntityAttributes(dsconnection, cp.getId());
	    if (cpAttrs!=null &&  poAttrs!=null) {
	      rst = true;
	    }
	    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method check(): " + rst);
	    return rst;
	}


	@Override
	public String getDescription() {
		final StringBuffer str = new StringBuffer();
	    str.append("Fills the trade keyword ");
	    str.append('\"');
	    str.append(TRADE_KWRD_UTI_REFERENCE);
	    str.append('\"');
	    str.append('.');
	    return str.toString();
	}


    @Override
    public boolean update(TaskWorkflowConfig wc, Trade newTrade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Starts method update(). Trade: " + newTrade.getLongId());

        // Check if apply the product
        if(!isTradeReportable(newTrade)){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method update(). Trade " + newTrade.getLongId() + " not processed.");
            return true;
        }

        // Check if the uti comes from Murex
        String tradeUtiReference = newTrade.getKeywordValue(TRADE_KWRD_UTI_REFERENCE);
        if(!Util.isEmpty(tradeUtiReference)){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method update(). UTI_REFERENCE '" + tradeUtiReference + "' comes from Front.");
            return true;
        }

        // Regulations
        if(!regulationDFA(newTrade)) {
            if(!regulationCanada(newTrade)) {
                regulationMexico(newTrade);
            }
        }
        if(!applyRegulations) {
            if (isCptyFinancial(newTrade)) {
                if (!existBilateralAgreement(newTrade)) {
                    tieBreak(newTrade);
                }
            }
        }

        // Evento NEW
        else if(isEventNew(newTrade, oldTrade)){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event NEW.");
            if (utiGenerationBySantander){
                generateUti(newTrade);
            }
            return true;
        }

        // Evento Novation
        if (isEventNovation(newTrade)) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event NOVATION.");
            String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
            Trade novatedTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
            if (novatedTrade != null) {
                String novatedUti = novatedTrade.getKeywordValue(TRADE_KWRD_UTI_REFERENCE);
                if (!Util.isEmpty(novatedUti)) {
                    if (novatedUti.length() >= 10) {
                        newTrade.addKeyword(TRADE_KWRD_PRIOR_UTI_PREFIX, novatedUti.substring(0, 10));
                    }
                    if (novatedUti.length() > 10) {
                        newTrade.addKeyword(TRADE_KWRD_PRIOR_UTI_VALUE, novatedUti.substring(10));
                    }
                }
            }
            if (utiGenerationBySantander){
                generateUti(newTrade);
            }
            return true;
        }

        // Resto de eventos
        else {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND'");
            String oldUtiReference = "";
            if((isEventCancelReissue(newTrade) || isEventPortfolioAssignment(newTrade)) && (newTrade.getProduct() instanceof PerformanceSwap)) {
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Resto de Eventos - isCancelReissue Or PortfolioAssignment is true - MxLastEvent: " + newTrade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT));
                String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
                oldTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Resto de Eventos - isCancelReissue Or PortfolioAssignment is true - OldTradeId: '" + oldTrade.getLongId());
            }
            oldUtiReference = oldTrade.getKeywordValue(TRADE_KWRD_UTI_REFERENCE);
            // Si Santander genera ahora el UTI
            if(utiGenerationBySantander) {
                if(Util.isEmpty(oldUtiReference)){
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case i");
                    generateUti(newTrade);
                }
                else if(oldUtiReference.contains(CALSTC)) {
                    if (isSameLei(dsCon, newTrade, oldTrade)) {
                        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case ii");
                        newTrade.addKeyword(TRADE_KWRD_UTI_REFERENCE, oldUtiReference);
                    } else {
                        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case iii");
                        generateUti(newTrade);
                    }
                }
                else {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case iv");
                    generateUti(newTrade);
                }
            }
            // Si la Contrapartida genera ahora el UTI
            else {
                if(Util.isEmpty(oldUtiReference)){
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case v");
                    newTrade.addKeyword(TRADE_KWRD_UTI_REFERENCE, "");
                }
                else if(oldUtiReference.contains(CALSTC)) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case vi");
                    newTrade.addKeyword(TRADE_KWRD_UTI_REFERENCE, "");
                }
                else if(isSameLei(dsCon, newTrade, oldTrade)) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case vii");
                    newTrade.addKeyword(TRADE_KWRD_UTI_REFERENCE, oldUtiReference);
                }
                else{
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event 'AMEND' Case viii");
                    newTrade.addKeyword(TRADE_KWRD_UTI_REFERENCE, "");
                }
            }
            return true;
        }
    }


    private boolean regulationDFA(Trade trade) {
        String usCounterpartyDomicile = getLEAttributeValue(cpAttrs, LE_ATTR_US_COUNTERPARTY_DOMICILE, po);
        String branchUsSwapDealer = getLEAttributeValue(cpAttrs, LE_ATTR_BRANCH_US_SWAP_DEALER, po);
        String affiliateUsGuaranteed = getLEAttributeValue(cpAttrs, LE_ATTR_AFFILIATE_US_GUARANTEED, po);
        String conduitUsPerson = getLEAttributeValue(cpAttrs, LE_ATTR_CONDUIT_US_PERSON, po);
        String swapDealer = getLEAttributeValue(cpAttrs, LE_ATTR_SWAP_DEALER, po);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationDFA - " + LE_ATTR_US_COUNTERPARTY_DOMICILE + " " + usCounterpartyDomicile + " - " + LE_ATTR_BRANCH_US_SWAP_DEALER + ": " + branchUsSwapDealer +
                " - " + LE_ATTR_AFFILIATE_US_GUARANTEED + ": " + affiliateUsGuaranteed + " - " + LE_ATTR_CONDUIT_US_PERSON + ": " + conduitUsPerson + " - " + LE_ATTR_SWAP_DEALER + ": " +  swapDealer);
        boolean isDFA = ((!Util.isEmpty(usCounterpartyDomicile) && "Y".equalsIgnoreCase(usCounterpartyDomicile)) ||
                (!Util.isEmpty(branchUsSwapDealer) && "Y".equalsIgnoreCase(branchUsSwapDealer)) ||
                (!Util.isEmpty(affiliateUsGuaranteed) && "Y".equalsIgnoreCase(affiliateUsGuaranteed)) ||
                (!Util.isEmpty(conduitUsPerson) && "Y".equalsIgnoreCase(conduitUsPerson)));
        boolean isSwapDealer = !Util.isEmpty(swapDealer) && "Y".equalsIgnoreCase(swapDealer);
        if(isDFA && !isSwapDealer){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationDFA Case i");
            utiGenerationBySantander = true;
            return true;
        }
        else if(isDFA && isSwapDealer){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationDFA Case ii");
            applyRegulations = false;
            return true;
        }
        else if(!isDFA) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationDFA Case iii");
            return false;
        }
        return true;
    }


    private boolean regulationCanada(Trade trade){
        boolean isCptyCountryCanada = COUNTRY_CANADA.equalsIgnoreCase(cp.getCountry());
        String emirFullDeleg = getLEAttributeValue(cpAttrs, LE_ATTR_EMIR_FULL_DELEG, po);
        String utiGenerationParty = getLEAttributeValue(cpAttrs, LE_ATTR_UTI_GENERATION_PARTY, po);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada - " + cp.getCountry() + ": " + isCptyCountryCanada + " - " +
                LE_ATTR_EMIR_FULL_DELEG + ": " + emirFullDeleg + " - " + LE_ATTR_UTI_GENERATION_PARTY + ": " + utiGenerationParty);
        if(!isCptyCountryCanada){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada Case i");
            return false;
        }
        else if(!Util.isEmpty(emirFullDeleg) && "TRUE".equalsIgnoreCase(emirFullDeleg)){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada Case ii");
            utiGenerationBySantander = true;
        }
        else if((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && SANTANDER.equalsIgnoreCase(utiGenerationParty))){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada Case iii");
            utiGenerationBySantander = true;
        }
        else if ((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && (COUNTERPARTY.equalsIgnoreCase(utiGenerationParty) || COUNTERPARTY_ALWAYS.equalsIgnoreCase(utiGenerationParty)))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada Case iv");
            counterpartyGeneratesUti();
        }
        else if((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                Util.isEmpty(utiGenerationParty)) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationCanada Case v");
            counterpartyGeneratesUti();
        }
        return true;
    }


    private boolean regulationMexico(Trade trade){
        boolean isCptyCountryMexico = COUNTRY_MEXICO.equalsIgnoreCase(cp.getCountry());
        boolean isCptyFinancial = cp.getClassification();
        String emirFullDeleg = getLEAttributeValue(cpAttrs, LE_ATTR_EMIR_FULL_DELEG, po);
        String utiGenerationParty = getLEAttributeValue(cpAttrs, LE_ATTR_UTI_GENERATION_PARTY, po);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico - Counrty: " + cp.getCountry() + " - is Mexico:" + isCptyCountryMexico +
                " - Financial " + isCptyFinancial + " - " + LE_ATTR_EMIR_FULL_DELEG + ": " + emirFullDeleg + " - " + LE_ATTR_UTI_GENERATION_PARTY + ": " + utiGenerationParty);
        if(!isCptyCountryMexico || !isCptyFinancial){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico Case i");
           applyRegulations = false;
        }
        else if(isCptyCountryMexico && isCptyFinancial &&
                (!Util.isEmpty(emirFullDeleg) && "TRUE".equalsIgnoreCase(emirFullDeleg))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico Case ii");
            utiGenerationBySantander = true;
        }
        else if(isCptyCountryMexico && isCptyFinancial &&
                (Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && SANTANDER.equalsIgnoreCase(utiGenerationParty))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico Case iii");
            utiGenerationBySantander = true;
        }
        else if(isCptyCountryMexico && isCptyFinancial &&
                (Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && (COUNTERPARTY.equalsIgnoreCase(utiGenerationParty) || COUNTERPARTY_ALWAYS.equalsIgnoreCase(utiGenerationParty)))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico Case iv");
            counterpartyGeneratesUti();
        }
        else if(isCptyCountryMexico && isCptyFinancial &&
                (Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (Util.isEmpty(utiGenerationParty))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - RegulationMexico Case v");
            counterpartyGeneratesUti();
        }
        return true;
    }


    private final boolean isCptyFinancial(Trade trade) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isCptyFinancial");
        if(!cp.getClassification()) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - IsCptyFinancial Case i");
            utiGenerationBySantander = true;
            return false;
        }
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - IsCptyFinancial Case ii");
        return true;
    }


    private final boolean existBilateralAgreement(Trade trade) {
        String emirFullDeleg = getLEAttributeValue(cpAttrs, LE_ATTR_EMIR_FULL_DELEG, po);
        String utiGenerationParty = getLEAttributeValue(cpAttrs, LE_ATTR_UTI_GENERATION_PARTY, po);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - ExistBilateralAgreement - " + LE_ATTR_EMIR_FULL_DELEG + ": " + emirFullDeleg + " - " + LE_ATTR_UTI_GENERATION_PARTY + ": " + utiGenerationParty);
        if(!Util.isEmpty(emirFullDeleg) && "TRUE".equalsIgnoreCase(emirFullDeleg)) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - ExistBilateralAgreement Case i");
            utiGenerationBySantander = true;
            return true;
        }
        else if((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && SANTANDER.equalsIgnoreCase(utiGenerationParty))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - ExistBilateralAgreement Case ii");
            utiGenerationBySantander = true;
            return true;
        }
        else if((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (!Util.isEmpty(utiGenerationParty) && (COUNTERPARTY.equalsIgnoreCase(utiGenerationParty) || COUNTERPARTY_ALWAYS.equalsIgnoreCase(utiGenerationParty)))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - ExistBilateralAgreement Case iii");
            counterpartyGeneratesUti();
            return true;
        }
        else if((Util.isEmpty(emirFullDeleg) || (!Util.isEmpty(emirFullDeleg) && "FALSE".equalsIgnoreCase(emirFullDeleg))) &&
                (Util.isEmpty(utiGenerationParty))) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - ExistBilateralAgreement Case iv");
            return false;
        }
        return true;
    }


    private void tieBreak(Trade trade){
        if(isPerformanceSwap(trade, trade.getProductType())){
            tieBreakPerformanceSwap(trade, (PerformanceSwap) trade.getProduct());
        }
        else if(isBondForward(trade, trade.getProductType())){
            tieBreakBondForward(trade, (Bond) trade.getProduct());
        }
        else if(isEquityForwardCO2(trade, trade.getProductType())) {
            tieBreakEquityForwardCO2(trade, (Equity) trade.getProduct());
        }
        return;
    }


    private void tieBreakPerformanceSwap(Trade trade, PerformanceSwap brs) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method TieBreakPerformanceSwap");
        String primaryLegDirection = "";
        String primaryLegType = "";
        String secondaryLegDirection = "";
        String secondaryLegType = "";
        // Primary Leg
        PerformanceSwappableLeg primaryLeg = brs.getPrimaryLeg();
        PerformanceSwapLeg primLeg = null;
        boolean perfLeg = false;
        if (primaryLeg instanceof PerformanceSwapLeg) {
            perfLeg = true;
            primLeg = (PerformanceSwapLeg) primaryLeg;
        }
        if (perfLeg) {
            if (primLeg.getNotional() < 0.0D) {
                primaryLegDirection = "Pay";
            } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                primaryLegDirection = "Pay";
            } else {
                primaryLegDirection = "Rec";
            }
        }
        primaryLegType = loadProductIntType(brs.getReferenceProduct());
        if ("Rec".equalsIgnoreCase(primaryLegDirection)) {
            if ("Fixed".equalsIgnoreCase(primaryLegType)) {
                this.bondRecFixed = true;
            } else {
                this.bondRecFloating = true;
            }
        } else if ("Pay".equalsIgnoreCase(primaryLegDirection)) {
            if ("Fixed".equalsIgnoreCase(primaryLegType)) {
                this.bondPayFixed = true;
            } else {
                this.bondPayFloating = true;
            }
        }
        // Secondary Leg
        SwapLeg swap = (SwapLeg) brs.getSecondaryLeg();
        secondaryLegType = swap.getLegType();
        if ("Rec".equalsIgnoreCase(primaryLegDirection)) {
            secondaryLegDirection = "Pay";
        } else if ("Pay".equalsIgnoreCase(primaryLegDirection)) {
            secondaryLegDirection = "Rec";
        }
        if ("Rec".equalsIgnoreCase(secondaryLegDirection)) {
            if ("Fixed".equalsIgnoreCase(secondaryLegType)) {
                this.swapRecFixed = true;
            } else {
                this.swapRecFloating = true;
            }
        } else if ("Pay".equalsIgnoreCase(secondaryLegDirection)) {
            if ("Fixed".equalsIgnoreCase(secondaryLegType)) {
                this.swapPayFixed = true;
            } else {
                this.swapPayFloating = true;
            }
        }
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap - bondRecFloating: " + bondRecFloating + " - bondPayFloating: " + bondPayFloating + " - bondRecFixed: " + bondRecFixed  +
                " - bondPayFixed: " + bondPayFixed + " - swapPayFixed: " + swapPayFixed + " - swapRecFixed: " + swapRecFixed + " - swapPayFloating: " + swapPayFloating + " - swapRecFloating: " + swapRecFloating);
        // Case 1
        if(this.bondRecFloating && this.swapPayFixed){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case i");
            utiGenerationBySantander = true;
        }
        // Case 2
        else if(this.bondPayFloating && this.swapRecFixed){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case ii");
            counterpartyGeneratesUti();
        }
        // Case 3
        else if(this.bondRecFloating && this.swapPayFloating){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case iii");
            checkLei(trade);
        }
        // Case 4
        else if(this.bondPayFloating && this.swapRecFloating){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case iv");
            checkLei(trade);
        }
        // Case 5
        else if(this.bondRecFixed && this.swapPayFloating){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case v");
            counterpartyGeneratesUti();
        }
        // Case 6
        else if(this.bondPayFixed && this.swapRecFloating){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case vi");
            utiGenerationBySantander = true;
        }
        // Case 7
        else if(this.bondRecFixed && this.swapPayFixed){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case vii");
            checkLei(trade);
        }
        // Case 8
        else if(this.bondPayFixed && this.swapRecFixed){
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakPerformanceSwap Case viii");
            checkLei(trade);
        }
    }


    private void tieBreakBondForward(Trade trade, Bond bond) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakBondForward");
        checkLei(trade);
    }


    private void tieBreakEquityForwardCO2(Trade trade, Equity equity) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - TieBreakEquityForwardCO2");
        checkLei(trade);
    }


    private void checkLei(Trade trade) {
        if (Util.isEmpty(poAttrs) || Util.isEmpty(cpAttrs)) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei Case i");
            counterpartyGeneratesUti();
            return;
        }
        String poLei = getLEAttributeValue(poAttrs, LE_ATTR_LEI, trade.getBook().getLegalEntity());
        String cpLei = getLEAttributeValue(cpAttrs, LE_ATTR_LEI, trade.getBook().getLegalEntity());
        if (Util.isEmpty(poLei) || Util.isEmpty(cpLei)) {
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei Case ii");
            counterpartyGeneratesUti();
            return;
        }
        else {
            for (int i = 0; i < poLei.length(); i++) {
                int posPo = LEI_SCALE.indexOf(poLei.charAt(i));
                int posCp = LEI_SCALE.indexOf(cpLei.charAt(i));
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei PO LEI: " + poLei + " pos: " + posPo);
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei CP LEI: " + cpLei + " pos: " + posCp);
                if (posPo < posCp) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei Case iii");
                    utiGenerationBySantander = true;
                    return;
                } else if (posPo > posCp) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - CheckLei Case iv");
                    counterpartyGeneratesUti();
                    return;
                }
            }
        }
    }


    public boolean isTradeReportable(Trade trade) {
        return !isInternalDeal(trade) && isProductReportable(trade);
    }


    public boolean isTradeReportable(Trade trade, Collection<LegalEntityAttribute> poAttrs, Collection<LegalEntityAttribute> cpAttrs) {
        return (!isInternalDeal (trade, poAttrs, cpAttrs) && isProductReportable(trade));
    }


    public boolean isInternalDeal(final Trade trade, final Collection<LegalEntityAttribute> poAtts, final Collection<LegalEntityAttribute> cpAtts) {
        boolean rst = false;
        if (trade == null) {
            return rst;
        }
        if (poAtts == null | cpAtts == null) {
            return rst;
        }
        final Iterator<LegalEntityAttribute> poIter = poAtts.iterator();
        LegalEntityAttribute currentAtt = null;
        String poLei = "";
        while (poIter.hasNext()) {
            currentAtt = poIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                poLei = currentAtt.getAttributeValue();
            }
        }
        final Iterator<LegalEntityAttribute> cpIter = cpAtts.iterator();
        String cpLei = "";
        while (cpIter.hasNext()) {
            currentAtt = cpIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                cpLei = currentAtt.getAttributeValue();
            }
        }
        if ((trade.getMirrorTradeId() != 0) || ((poLei != null) && (cpLei != null) && poLei.equalsIgnoreCase(cpLei))) {
            rst = true;
        }
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Is Internal trade attr, rst: " + rst);
        return rst;
    }


    private final boolean isProductReportable(final Trade trade) {
        return isPerformanceSwap(trade, trade.getProductType()) || isBondForward(trade, trade.getProductType()) || isEquityForwardCO2(trade, trade.getProductType()) ? true : false;
    }


    private boolean isPerformanceSwap(Trade trade, String productType) {
        return !Util.isEmpty(productType) && "PerformanceSwap".equalsIgnoreCase(productType) && trade.getProduct() instanceof PerformanceSwap ? true : false;
    }


    private boolean isBondForward(Trade trade, String productType) {
        String bondForward = trade.getKeywordValue("BondForward");
        return "Bond".equalsIgnoreCase(productType) && !Util.isEmpty(bondForward) && trade.getProduct() instanceof Bond && "true".equalsIgnoreCase(bondForward) ? true : false;
    }


    private boolean isEquityForwardCO2(Trade trade, String productType) {
        if(trade.getProduct() instanceof Equity){
            Equity equity = (Equity) trade.getProduct();
            String equityType = equity.getSecCode("EQUITY_TYPE");
            String spotFwd= trade.getKeywordValue("Mx_Product_SubType");
            return "Equity".equalsIgnoreCase(productType) && !Util.isEmpty(equityType) &&
                    (("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))) && (!Util.isEmpty(spotFwd) && "FORWARD".equalsIgnoreCase(spotFwd)) ? true : false;
        }
        return false;
    }


    public boolean isInternalDeal(final Trade trade) {
        boolean rst = false;
        if (trade == null) {
            return rst;
        }
        if (poAttrs == null | cpAttrs == null) {
            return rst;
        }
        final Iterator<LegalEntityAttribute> poIter = poAttrs.iterator();
        LegalEntityAttribute currentAtt = null;
        String poLei = "";
        while (poIter.hasNext()) {
            currentAtt = poIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                poLei = currentAtt.getAttributeValue();
            }
        }
        final Iterator<LegalEntityAttribute> cpIter = cpAttrs.iterator();
        String cpLei = "";
        while (cpIter.hasNext()) {
            currentAtt = cpIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                cpLei = currentAtt.getAttributeValue();
            }
        }
        if ((trade.getMirrorTradeId() != 0) || ((poLei != null) && (cpLei != null) && poLei.equalsIgnoreCase(cpLei))) {
            rst = true;
        }
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Is Internal trade, rst: " + rst);
        return rst;
    }


    public void generateUti(Trade trade) {
        String uti_reference = "";
        String usiPrefix = getLEAttributeValue(poAttrs, LE_ATTR_LEI, po);
        String beforePad = CALSTC;
        String tradeIdStr = String.valueOf(trade.getLongId());
        if (trade.getLongId()==0 || trade.getLongId()==-1) {
            tradeIdStr = String.valueOf(trade.getAllocatedLongSeed());
        }
        int lengthData = usiPrefix.length() + beforePad.length() + tradeIdStr.length();
        String pad = String.format("%0" + (UTI_LENGTH - lengthData) + "d%s", 0, "");
        uti_reference = usiPrefix + beforePad + pad + tradeIdStr;
        trade.addKeyword(TRADE_KWRD_UTI_REFERENCE, uti_reference);
    }


    public void counterpartyGeneratesUti() {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Counterparty generates the UTI_REFERENCE");
    }


    private String getLEAttributeValue(final Collection<LegalEntityAttribute> attr, String attributeType, LegalEntity po) {
        if (attr == null) {
            return "";
        }
        for (final LegalEntityAttribute leAttr : attr) {
            if (leAttr.getAttributeType().equals(attributeType) && ((leAttr.getProcessingOrgId() == 0) || (leAttr.getProcessingOrgId() == po.getId()))) {
                return leAttr.getAttributeValue() != null ? leAttr.getAttributeValue() : "";
            }
        }
        return "";
    }


    private boolean isEventNew(Trade newTrade, Trade oldTrade) {
        String action = newTrade.getAction().toString();
        String mxLastEvent = newTrade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
        boolean isNew = ACTION_NONE.equalsIgnoreCase(newTrade.getAction().toString()) &&
                (Util.isEmpty(mxLastEvent) || (!Util.isEmpty(mxLastEvent) && !EVENT_NOVATION.equalsIgnoreCase(mxLastEvent) && !EVENT_CANCEL_REISSUE.equalsIgnoreCase(mxLastEvent) && !EVENT_PORTFOLIO_ASSIGMENT.equalsIgnoreCase(mxLastEvent)));
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventNew: '" + isNew + "' - newTradeAction: " + action +
                " - newTradeStatus: '" + newTrade.getStatus().getStatus() + "' - " + TRADE_KWRD_MX_LAST_EVENT + ": '" + mxLastEvent + "'");
        return isNew;
    }


    private boolean isEventNovation(Trade newTrade) {
        String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventNovation. mxTransferFrom: " + mxTransferFrom);
        if(!Util.isEmpty(mxTransferFrom)){
            Trade novatedTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
            if (novatedTrade != null) {
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventNovation. There is a trade with id: " + novatedTrade.getLongId());
                String mxLastEvent = novatedTrade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
                String mxTransferTo = novatedTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_TO);
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventNovation. Trade: " + novatedTrade.getLongId() + " - mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                if (EVENT_NOVATION.equalsIgnoreCase(mxLastEvent) && !Util.isEmpty(mxTransferTo) ) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventNovation is true - mxTransferFrom: " + mxTransferFrom + "mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isEventCancelReissue(Trade newTrade) {
        String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventCancelReissue. mxTransferFrom: " + mxTransferFrom);
        if(!Util.isEmpty(mxTransferFrom)){
            Trade canceledTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
            if (canceledTrade != null) {
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventCancelReissue. There is a trade with id: " + canceledTrade.getLongId());
                String mxLastEvent = canceledTrade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
                String mxTransferTo = canceledTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_TO);
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventCancelReissue. Trade: " + canceledTrade.getLongId() + " - mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                if (EVENT_CANCEL_REISSUE.equalsIgnoreCase(mxLastEvent) && !Util.isEmpty(mxTransferTo) ) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventCancelReissue is true - mxTransferFrom: " + mxTransferFrom + "mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isEventPortfolioAssignment(Trade newTrade) {
        String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventPortfolioAssignment. mxTransferFrom: " + mxTransferFrom);
        if(!Util.isEmpty(mxTransferFrom)){
            Trade terminatedTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
            if (terminatedTrade != null) {
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventPortfolioAssignment. There is a trade with id: " + terminatedTrade.getLongId());
                String mxLastEvent = terminatedTrade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
                String mxTransferTo = terminatedTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_TO);
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventPortfolioAssignment. Trade: " + terminatedTrade.getLongId() + " - mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                if (EVENT_PORTFOLIO_ASSIGMENT.equalsIgnoreCase(mxLastEvent) && !Util.isEmpty(mxTransferTo) ) {
                    Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isEventPortfolioAssignment is true - mxTransferFrom: " + mxTransferFrom + "mxTransferTo: " + mxTransferTo + " - mxLastEvent: " + mxLastEvent);
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isSameLei(DSConnection dsCon, Trade newTrade, Trade oldTrade) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei.");
        final LegalEntity newEntity = newTrade.getCounterParty();
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - newEntity: " + newEntity.getCode());
        final Collection<LegalEntityAttribute> newAttrs = BOCache.getLegalEntityAttributes(dsCon, newEntity.getId());
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - newEntityAttr: " + newAttrs.toString());
        final LegalEntity oldEntity = oldTrade.getCounterParty();
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - oldEntity: " + oldEntity.getCode());
        final Collection<LegalEntityAttribute> oldAttrs = BOCache.getLegalEntityAttributes(dsCon, oldEntity.getId());
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - oldEntityAttr: " + oldAttrs.toString());
        final Iterator<LegalEntityAttribute> newIter = newAttrs.iterator();
        LegalEntityAttribute currentAtt = null;
        String newLei = "";
        while (newIter.hasNext()) {
            currentAtt = newIter.next();
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - NewCurrentAttr: " + currentAtt);
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                newLei = currentAtt.getAttributeValue();
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - newLei: " + newLei);
            }
        }
        final Iterator<LegalEntityAttribute> oldIter = oldAttrs.iterator();
        String oldLei = "";
        while (oldIter.hasNext()) {
            currentAtt = oldIter.next();
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - OldCurrentAttr: " + currentAtt);
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                oldLei = currentAtt.getAttributeValue();
                Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method isSameLei - oldLei: " + oldLei);
            }
        }
        if (!Util.isEmpty(newLei) && !Util.isEmpty(oldLei) && newLei.equalsIgnoreCase(oldLei)) {
            return true;
        }
        return false;
    }


    public Trade getTradeByKeyword(String keywordName, String keywordValue) {
        Trade trade = null;
        try {
            TradeArray tradeArray = DSConnection
                    .getDefault()
                    .getRemoteTrade()
                    .getTrades(
                            "trade, trade_keyword kwd",
                            "trade.trade_id=kwd.trade_id and kwd.keyword_name='" + keywordName + "' and kwd.keyword_value='" + keywordValue + "'",
                            null, null);
            if((tradeArray != null) && (tradeArray.size() != 0)) {
                trade = tradeArray.firstElement();
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return trade;
    }


    private String loadProductIntType(Product product){
        if(null!=product){
            if( product instanceof Bond){
                if(((Bond)product).getFixedB()){
                    return "Fixed";
                }else{
                    return "Float";
                }
            }
        }
        return "";
    }


    private String getUtiMurexTransferFrom(Trade newTrade) {
        Log.info(Log.INFO, this.getClass().getSimpleName() + " - Event NOVATION.");
        String mxTransferFrom = newTrade.getKeywordValue(TRADE_KWRD_MX_TRANFER_FROM);
        Trade oldTrade = getTradeByKeyword(TRADE_KWRD_MX_TRADE_ID, mxTransferFrom);
        if(oldTrade != null) {
            String utiOldTrade = oldTrade.getKeywordValue(TRADE_KWRD_UTI_REFERENCE);
            Log.info(Log.INFO, this.getClass().getSimpleName() + " - Method getUtiMurexTransferFrom - mxTransferFrom: " + mxTransferFrom +
                    " - oldTrade: '" + oldTrade.getLongId() + "' - " + "' - UTI_OLD_TRADE: '" + utiOldTrade + "'");
            return utiOldTrade;
        }
        return "";
    }


}

