package calypsox.tk.bo.workflow.rule;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;
import calypsox.util.collateral.CollateralUtilities;

public class SantUtiUsiGenerationTradeRule implements WfTradeRule {


	private static final String KEYWORD_UTI_TRADE_ID = "UTI_REFERENCE";
	private static final String KEYWORD_USI_TRADE_ID = "USI_REFERENCE";
	private static final String KEYWORD_TEMP_UTI_TRADE_ID = "TempUTITradeId";
	private static final String KEYWORD_PRIOR_UTI_PREFIX_TRADE_ID = "PriorUTIPrefix";
	private static final String KEYWORD_PRIOR_UTI_VALUE_TRADE_ID = "PriorUTIValue";
	private static final String KEYWORD_MX_LAST_EVENT = "MxLastEvent";
	private static final String KEYWORD_MX_TRADE_ID = "MurexTradeID";
	private static final String UTI_VALUE_PREFIX_BEFORE_PAD = "SANTANDER";
	private static final String UTI_VALUE_PREFIX_AFTER_PAD = "CALSTC";
	private static final String UTI_VALUE_TEMP_SUFFIX = "T";
	private static final int UTI_VALUE_LENGTH = 32;
	private static final int NEW_UTI_VALUE_LENGTH = 52;
	private static final String KEYWORD_MUREX_TRANFER_FROM = "MurexTransferFrom";
	private static final String KEYWORD_REPORTING_PARTY = "ReportingParty";
	private static final String LE_ATTR_LEI = "LEI";
	private static final String LE_ATTR_US_CONTERPARTY_DOMICILE = "US_CONTERPARTY_DOMICILE";
	private static final String LE_ATTR_BRANCH_US_SWAP_DEALER = "BRANCH_US_SWAP_DEALER";
	private static final String LE_ATTR_AFFILIATE_US_GUARANTEED = "AFFILIATE_US_GUARANTEED";
	private static final String LE_ATTR_CONDUIT_US_PERSON = "CONDUIT_US_PERSON";
	private static final String LE_ATTR_SWAP_DEALER = "SWAP_DEALER";
	private static final String LE_ATTR_UTI_GENERATION_PARTY = "UTI_GENERATION_PARTY";
	private static final String LE_ATTR_UTI_GENERATION_PARTY_VALUE_SANTANDER = "Santander";
    private static final String LE_ATTRIBUTE_USI_PREFIX = "USI_Prefix";
	private static final String VALUE_YES = "Y";
	private static final String VALUE_RP_PO = "PO";
	private static final String VALUE_RP_CP = "CP";
	private static final String EEA_COUNTRIES = "EEACountries";
	private static final String EVENT_CANCEL_REISSUE = "mxContractEventICANCEL_REISSUE";
	private static final String EVENT_RESTRUCTURE = "mxContractEventIRESTRUCTURE";
	private static final String EVENT_PORTFOLIO_ASSIGNMENT = "mxContractEventIPORTFOLIO_ASSIGNMENT";
	private static final String EVENT_COUNTERPART_AMENDMENT = "mxContractEventICOUNTERPART_AMENDMENT";

	boolean bondPayFloating = false;
	boolean bondRecFloating = false;
	boolean bondPayFixed = false;
	boolean bondRecFixed = false;
	boolean swapRecFixed = false;
	boolean swapPayFixed = false;
	boolean swapRecFloating = false;
	boolean swapPayFloating = false;



	@Override
	public boolean check(final TaskWorkflowConfig taskworkflowconfig, final Trade trade, final Trade trade1, final Vector vector,
						final DSConnection dsconnection, final Vector vector1, final Task task, final Object obj, final Vector vector2) {

		boolean rst = false;
	    final LegalEntity po = trade.getBook().getLegalEntity();
	    final Collection<LegalEntityAttribute> poAttrs = BOCache.getLegalEntityAttributes(dsconnection, po.getId());
	    final LegalEntity cp = trade.getCounterParty();
	    final Collection<LegalEntityAttribute> cpAttrs = BOCache.getLegalEntityAttributes(dsconnection, cp.getId());

	    if (cpAttrs!=null &&  poAttrs!=null) {
	      rst = true;
	    }
	    Log.info(this, "SantUtiUsiGenerationTradeRule check(): " + rst);

	    return rst;
	}



	@Override
	public String getDescription() {
		final StringBuffer str = new StringBuffer();
	    str.append("Fills the trade keywords ");
	    str.append('\"');
	    str.append(KEYWORD_UTI_TRADE_ID);
	    str.append('\"');
	    str.append(" , ");
	    str.append('\"');
	    str.append("KEYWORD_USI_TRADE_ID");
	    str.append('\"');
	    str.append('.');
	    return str.toString();
	}



	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
						Vector excps, Task task, Object dbCon, Vector events) {

		Log.debug(Log.DEBUG, "Starts SantUtiUsiGenerationTradeRule update(). Trade: " + trade.getLongId());
		String utiFromFront = trade.getKeywordDisplayValue(KEYWORD_UTI_TRADE_ID);
		String usiFromFront = trade.getKeywordDisplayValue(KEYWORD_USI_TRADE_ID);

		final LegalEntity po = trade.getBook().getLegalEntity();
	    final Collection<LegalEntityAttribute> poAttrs = BOCache.getLegalEntityAttributes(dsCon, po.getId());
	    final LegalEntity cp = trade.getCounterParty();
	    final Collection<LegalEntityAttribute> cpAttrs = BOCache.getLegalEntityAttributes(dsCon, cp.getId());

	    if(isNotReportableTrade(trade, poAttrs, cpAttrs)) {
	    	Log.debug(Log.DEBUG, "SantUtiUsiGenerationTradeRule update(). Trade " + trade.getLongId() + " not processed.");
	    	return true;
	    }

	    // Manage evetns. If returns 'true' finish.
	    // if returns 'false' continue with the generation of the uti
	    if (manageEvent(dsCon, trade)) {
	    	return true;
	    }

	    // In case of generate new Uti
	    if (utiOrUsiSentFromFoSystem(utiFromFront, usiFromFront)) {
	    	if (utiFilledAndUsiFilled(utiFromFront, usiFromFront)) {
	    		return true;
	    	}
	    	else if (utiEmptyAndUsiFilled(utiFromFront, usiFromFront)) {
				trade.addKeyword(KEYWORD_UTI_TRADE_ID, usiFromFront);
				return true;
			}
	    }

		if(isTradeUnderDfaScope(cpAttrs, po)){
			if(isCounterpatyDefinedAsSwapDealer(cpAttrs, po)) {
				if(isSantanderUtiUsiGenerationParty_TieBreak(trade, poAttrs, cpAttrs)){
					String utiUsi = generateUti(trade, poAttrs);
					trade.addKeyword(KEYWORD_USI_TRADE_ID, utiUsi);
					trade.addKeyword(KEYWORD_UTI_TRADE_ID, utiUsi);
					return true;
				}
				else {
					trade.addKeyword(KEYWORD_REPORTING_PARTY, VALUE_RP_CP);
				}
			}
			else {
				String utiUsi = generateUti(trade, poAttrs);
				trade.addKeyword(KEYWORD_USI_TRADE_ID, utiUsi);
				if(Util.isEmpty(utiFromFront)) {
					trade.addKeyword(KEYWORD_UTI_TRADE_ID, utiUsi);
				}
				trade.addKeyword(KEYWORD_REPORTING_PARTY, VALUE_RP_PO);
				return true;
			}
		}

		if(!isAnEuropeanCounterparty(cp)){
			String uti = generateUti(trade, poAttrs);
			trade.addKeyword(KEYWORD_UTI_TRADE_ID, uti);
			return true;
		}

		if (cptyHasUtiGenerationPartyAttrFilled(cpAttrs)) {
			if(cptyHasUtiGenerationPartyAttrAsSantander(cpAttrs)) {
				String uti = generateUti(trade, poAttrs);
				trade.addKeyword(KEYWORD_UTI_TRADE_ID, uti);
				return true;
			}
			else {
				String utiTemp = generateUtiTemp(trade, poAttrs);
				trade.addKeyword(KEYWORD_TEMP_UTI_TRADE_ID, utiTemp);
				return true;
			}
		}
		else {
			if(isSantanderUtiUsiGenerationParty_TieBreak(trade, poAttrs, cpAttrs)){
				String uti = generateUti(trade, poAttrs);
				trade.addKeyword(KEYWORD_UTI_TRADE_ID, uti);
				return true;
			}
			else {
				String utiTemp = generateUtiTemp(trade, poAttrs);
				trade.addKeyword(KEYWORD_TEMP_UTI_TRADE_ID, utiTemp);
				return true;
			}
		}
	 }



	public final boolean checkProductType(final Trade trade) {
		boolean rst = false;

	    final String productType = trade.getProductType();
        if(!Util.isEmpty(productType) && (productType.equalsIgnoreCase("PerformanceSwap") || isBondForward(trade, productType) || isEquityCO2(trade, productType))) {
	        rst = true;
	    }
	      return rst;
	}



	public boolean isInternal(final Trade trade, final Collection<LegalEntityAttribute> poAtts, final Collection<LegalEntityAttribute> cpAtts) {
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
	    Log.debug(this, "Is Internal rst: " + rst);
	    return rst;
	}



	public String getLEAttributeValue(final Collection<LegalEntityAttribute> attr, String attributeType, LegalEntity po) {
		if (attr == null) {
	      return "";
	    }

	    for (final LegalEntityAttribute leAttr : attr) {
	      if (leAttr.getAttributeType().equals(attributeType)
	          && ((leAttr.getProcessingOrgId() == 0) || (leAttr.getProcessingOrgId() == po.getId()))) {
	        final String rst = leAttr.getAttributeValue();
	        if (rst != null) {
	          return rst;
	        }
	        return "";
	      }
	    }
	    return "";
	  }



	private boolean utiOrUsiSentFromFoSystem(String uti, String usi){
		return (!Util.isEmpty(uti) || !Util.isEmpty(usi)) ? true : false;
	}



	private boolean utiFilledAndUsiFilled(String uti, String usi){
	 	return (!Util.isEmpty(uti) && !Util.isEmpty(usi)) ? true : false;
	}



	private boolean utiEmptyAndUsiFilled(String uti, String usi){
		return (Util.isEmpty(uti) && !Util.isEmpty(usi)) ? true : false;
	}



	private boolean isTradeUnderDfaScope(Collection<LegalEntityAttribute> cpAtts, LegalEntity po){
		String usCptyDomicile = getLEAttributeValue(cpAtts, LE_ATTR_US_CONTERPARTY_DOMICILE, po);
		String branchUsSwapDealer = getLEAttributeValue(cpAtts, LE_ATTR_BRANCH_US_SWAP_DEALER, po);
		String affiliateUsGuaranteed = getLEAttributeValue(cpAtts, LE_ATTR_AFFILIATE_US_GUARANTEED, po);
		String conduitUsPerson = getLEAttributeValue(cpAtts, LE_ATTR_CONDUIT_US_PERSON, po);

		if((!Util.isEmpty(usCptyDomicile) && VALUE_YES.equalsIgnoreCase(usCptyDomicile)) ||
		   (!Util.isEmpty(branchUsSwapDealer) && VALUE_YES.equalsIgnoreCase(branchUsSwapDealer)) ||
		   (!Util.isEmpty(affiliateUsGuaranteed) && VALUE_YES.equalsIgnoreCase(affiliateUsGuaranteed)) ||
		   (!Util.isEmpty(conduitUsPerson) && VALUE_YES.equalsIgnoreCase(conduitUsPerson))){
			return true;
		}
		else {
			return false;
		}
	}



	private boolean isCounterpatyDefinedAsSwapDealer(Collection<LegalEntityAttribute> cpAtts, LegalEntity po){
		String cptySwapDealer = getLEAttributeValue(cpAtts, LE_ATTR_SWAP_DEALER, po);
		if (!Util.isEmpty(cptySwapDealer) && cptySwapDealer.equalsIgnoreCase(VALUE_YES)){
			return true;
		}
		else {
			return false;
		}
	}



	private boolean isAnEuropeanCounterparty(LegalEntity cpty){
		String cptyCountry = cpty.getCountry();
		Vector<String> eeaCountries = CollateralUtilities.getDomainValues(EEA_COUNTRIES);
		if(!Util.isEmpty(eeaCountries)) {
			for (String country : eeaCountries) {
				if(country.equalsIgnoreCase(cptyCountry)) {
					return true;
				}
			}
		}
		return false;
	}



	private boolean cptyHasUtiGenerationPartyAttrFilled(Collection<LegalEntityAttribute> attrs){
	    boolean rst = false;
	    String attrValue = "";
	    if (attrs == null) {
	    	return rst;
	    }

	    final Iterator<LegalEntityAttribute> iterator = attrs.iterator();
	    LegalEntityAttribute attr = null;
	    while (iterator.hasNext()) {
	    	attr = iterator.next();
	    	if (LE_ATTR_UTI_GENERATION_PARTY.contains(attr.getAttributeType())) {
	    		attrValue = attr.getAttributeValue();
	    	}
	    }

	    if (!Util.isEmpty(attrValue)){
	    	rst = true;
	    }
	    return rst;
	}



	private boolean cptyHasUtiGenerationPartyAttrAsSantander(Collection<LegalEntityAttribute> attrs){

		boolean rst = false;
	    String attrValue = "";
	    if (attrs == null) {
	    	return rst;
	    }

	    final Iterator<LegalEntityAttribute> iterator = attrs.iterator();
	    LegalEntityAttribute attr = null;
	    while (iterator.hasNext()) {
	    	attr = iterator.next();
	    	if (LE_ATTR_UTI_GENERATION_PARTY.contains(attr.getAttributeType())) {
	    		attrValue = attr.getAttributeValue();
	    	}
	    }

	    if (!Util.isEmpty(attrValue) && LE_ATTR_UTI_GENERATION_PARTY_VALUE_SANTANDER.equalsIgnoreCase(attrValue)){
	    	rst = true;
	    }

	    return rst;
	}



	private boolean manageEvent(DSConnection dsCon, Trade newTrade) {

		String mxLastEvent = "";
		String priorUtiPrefix = "";
		String priorUtiValue = "";
		Trade oldTrade = null;

		final String murexTranferFrom = newTrade.getKeywordValue(KEYWORD_MUREX_TRANFER_FROM);
		if(Util.isEmpty(murexTranferFrom)){
			return false;
		}

        TradeArray tradeSet = getTradeByKeyword(KEYWORD_MX_TRADE_ID, murexTranferFrom);
        if ((tradeSet != null) && (tradeSet.size() != 0)) {
        	oldTrade = tradeSet.firstElement();
        }

	    if(oldTrade==null) {
	    	Log.error(this, "Trade could not be retrieved");
	        return false;
	    }
	    Log.info(this, "Retrieved trade by external reference(" + murexTranferFrom + "): " + oldTrade);

	    boolean eventOnD = isEventOnD(newTrade.getEnteredDate(), oldTrade.getEnteredDate());

	    mxLastEvent = oldTrade.getKeywordValue(KEYWORD_MX_LAST_EVENT);
	    if(Util.isEmpty(mxLastEvent)) {
	    	Log.error(this, "Trade: " + oldTrade.getLongId() + " has the kwd '" + KEYWORD_MX_LAST_EVENT + "' empty.");
	    	return false;
	    }

	    if(EVENT_CANCEL_REISSUE.equalsIgnoreCase(mxLastEvent) || EVENT_RESTRUCTURE.equalsIgnoreCase(mxLastEvent)){
	    	if(eventOnD) {
	    		return false;
	    	}
	    	else {
	    		if(isSameCounterparty(newTrade, oldTrade)) {
	    			String oldUti = oldTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
	    			if (Util.isEmpty(oldUti)) {
	    				oldUti = oldTrade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
	    			}
	    			newTrade.addKeyword(KEYWORD_UTI_TRADE_ID, oldUti);
	    			return true;
	    		}
	    		else {
	    			if(isSameLei(dsCon, newTrade, oldTrade)) {
	    				String oldUti = oldTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
		    			if (Util.isEmpty(oldUti)) {
		    				oldUti = oldTrade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
		    			}
	    				newTrade.addKeyword(KEYWORD_UTI_TRADE_ID, oldUti);
	    				return true;
	    			}
	    			else {
	    				String oldUti = oldTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
		    			if (Util.isEmpty(oldUti)) {
		    				oldUti = oldTrade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
		    			}
	    				newTrade.addKeyword(KEYWORD_UTI_TRADE_ID, oldUti);
	    				return true;
	    			}
	    		}
	    	}
	    }
	    else if(EVENT_COUNTERPART_AMENDMENT.equalsIgnoreCase(mxLastEvent)){
	    	// Check the priorUti
	    	String oldUti = oldTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
	    	if (Util.isEmpty(oldUti)) {
				oldUti = oldTrade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
			}

		    if (!Util.isEmpty(oldUti)){
		    	if(oldUti.length()>=10) {
		    		priorUtiPrefix = oldUti.substring(0, 9);
		    	}
			    if(oldUti.length()>10) {
			    	priorUtiValue = oldUti.substring(10);
			    }
			    if(!Util.isEmpty(priorUtiPrefix)) {
			    	newTrade.addKeyword(KEYWORD_PRIOR_UTI_PREFIX_TRADE_ID, priorUtiPrefix);
			    }
			    if(!Util.isEmpty(priorUtiValue)) {
			    	newTrade.addKeyword(KEYWORD_PRIOR_UTI_VALUE_TRADE_ID, priorUtiValue);
			    }
		    }
		    // Check event logic
	    	if(isSameLei(dsCon, newTrade, oldTrade)) {
	    		final LegalEntity oldPo = oldTrade.getBook().getLegalEntity();
	    	    final Collection<LegalEntityAttribute> oldPoAttrs = BOCache.getLegalEntityAttributes(dsCon, oldPo.getId());
	    	    final LegalEntity oldCp = oldTrade.getCounterParty();
	    	    final Collection<LegalEntityAttribute> oldCpAttrs = BOCache.getLegalEntityAttributes(dsCon, oldCp.getId());
	    	    if(Util.isEmpty(oldPoAttrs) || Util.isEmpty(oldCpAttrs)) {
	    	    	Log.info(this, "Trade " + oldTrade.getLongId() + " has empty Po or Cpty attributes.");
	    	    	return true;
	    	    }
	    		if(isTradeReportable(oldTrade, oldPoAttrs, oldCpAttrs)) {
		    		newTrade.addKeyword(KEYWORD_UTI_TRADE_ID, oldUti);
		    		return true;
	    		}
	    	else if(!isSameLei(dsCon, newTrade, oldTrade)) {
	    			return false;
	    		}
	    	}
	    }
	    else if(EVENT_PORTFOLIO_ASSIGNMENT.equalsIgnoreCase(mxLastEvent)){
	    	if(eventOnD) {
	    		return false;
	    	}
	    	else {
	    		String oldUti = oldTrade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
		    	if (Util.isEmpty(oldUti)) {
					oldUti = oldTrade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
				}
	    		newTrade.addKeyword(KEYWORD_UTI_TRADE_ID, oldUti);
	    		return true;
	    	}
	    }

	    return false;
	}



	private boolean isSantanderUtiUsiGenerationParty_TieBreak(Trade trade,
			Collection<LegalEntityAttribute> poAttrs, Collection<LegalEntityAttribute> cpAttrs){

		loadByProduct(trade);

		// Case 1
		if(this.bondRecFloating && this.swapPayFixed){
			return false;
		}
		// Case 2
		else if(this.bondPayFloating && this.swapRecFixed){
			return true;
		}
		// Case 3
		else if(this.bondRecFloating && this.swapPayFloating){
			return checkLeiScale(trade, poAttrs, cpAttrs);
		}
		// Case 4
		else if(this.bondPayFloating && this.swapRecFloating){
			return checkLeiScale(trade, poAttrs, cpAttrs);
		}
		// Case 5
		else if(this.bondRecFixed && this.swapPayFloating){
			return true;
		}
		// Case 6
		else if(this.bondPayFixed && this.swapRecFloating){
			return false;
		}
		// Case 7
		else if(this.bondRecFixed && this.swapPayFixed){
			return checkLeiScale(trade, poAttrs, cpAttrs);
		}
		// Case 8
		else if(this.bondPayFixed && this.swapRecFixed){
			return checkLeiScale(trade, poAttrs, cpAttrs);
		}

        return false;
    }



    private boolean checkLeiScale(Trade trade, Collection<LegalEntityAttribute> poAttrs, Collection<LegalEntityAttribute> cpAttrs){

        if (Util.isEmpty(poAttrs) || Util.isEmpty(cpAttrs)) {
            return false;
        }

        String scale = "ZYXWVUTSRQPONMLKJIHGFEDCBA9876543210";
        String poLei = getLEAttributeValue(poAttrs, LE_ATTR_LEI, trade.getBook().getLegalEntity());
        String cpLei = getLEAttributeValue(cpAttrs, LE_ATTR_LEI, trade.getBook().getLegalEntity());

        if (Util.isEmpty(poLei) || Util.isEmpty(cpLei)) {
            return false;
        }

        for (int i=0; i<poLei.length(); i++) {
            int posPo = scale.indexOf(poLei.charAt(i));
            int posCp = scale.indexOf(cpLei.charAt(i));
            if(posPo<posCp) {return true;}
            else if(posPo>posCp) {return false;}
        }
        return true;
    }

	public boolean isNewCodeEnabled(){
		String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","ACTIVATE_NEW_UTI");
		return Boolean.parseBoolean(activationFlag);
	}

	public String generateUti(Trade trade, Collection<LegalEntityAttribute> poAttrs) {
		if (isNewCodeEnabled()) {
			return generateUti(trade,poAttrs,NEW_UTI_VALUE_LENGTH);
		}else{
			return generateUtiOld(trade,poAttrs);
		}
	}

	public String generateUti(Trade trade, Collection<LegalEntityAttribute> poAttrs,int utiLegth) {
		String usiPrefix = getLEAttributeValue(poAttrs, "LEI", trade.getBook().getLegalEntity());
		String beforePad = "CALSTC";
		String tradeIdStr = String.valueOf(trade.getLongId());
		if (trade.getLongId()==0 || trade.getLongId()==-1) {
			tradeIdStr = String.valueOf(trade.getAllocatedLongSeed());
		}
		int lengthData = usiPrefix.length() + beforePad.length() + tradeIdStr.length();
		String pad = String.format("%0" + (utiLegth - lengthData) + "d%s", 0, "");

		return usiPrefix + beforePad + pad + tradeIdStr;
	}

	public String generateUtiTemp(Trade trade, Collection<LegalEntityAttribute> poAttrs) {
		if (isNewCodeEnabled()) {
			String uti = generateUti(trade, poAttrs, NEW_UTI_VALUE_LENGTH - 1);
			return uti + UTI_VALUE_TEMP_SUFFIX;
		}else{
			return generateUtiTempOld(trade,poAttrs);
		}
	}

	public String generateUtiOld(Trade trade, Collection<LegalEntityAttribute> poAttrs) {
        String usiPrefix = getLEAttributeValue(poAttrs, LE_ATTRIBUTE_USI_PREFIX, trade.getBook().getLegalEntity());
        String beforePad = UTI_VALUE_PREFIX_BEFORE_PAD;
        String afterPad = UTI_VALUE_PREFIX_AFTER_PAD;
        String tradeIdStr = String.valueOf(trade.getLongId());
        if (trade.getLongId()==0 || trade.getLongId()==-1) {
            tradeIdStr = String.valueOf(trade.getAllocatedLongSeed());
        }
        int lengthData =  beforePad.length() + tradeIdStr.length() + afterPad.length();
        String pad = String.format("%0" + (UTI_VALUE_LENGTH - lengthData) + "d%s", 0, "");

        String uti = usiPrefix + beforePad + pad + afterPad + tradeIdStr;

        return uti;
    }

	public String generateUtiTempOld(Trade trade, Collection<LegalEntityAttribute> poAttrs) {
		String usiPrefix = getLEAttributeValue(poAttrs, LE_ATTRIBUTE_USI_PREFIX, trade.getBook().getLegalEntity());
		String beforePad = UTI_VALUE_PREFIX_BEFORE_PAD;
		String afterPad = UTI_VALUE_PREFIX_AFTER_PAD;
		String tempSuffix = UTI_VALUE_TEMP_SUFFIX;
		String tradeIdStr = String.valueOf(trade.getLongId());
		if (trade.getLongId()==0 || trade.getLongId()==-1) {
			tradeIdStr = String.valueOf(trade.getAllocatedLongSeed());
		}
		int lengthData = beforePad.length() + tradeIdStr.length() + afterPad.length() + tempSuffix.length();
		String pad = String.format("%0" + (UTI_VALUE_LENGTH - lengthData) + "d%s", 0, "");

		String uti = usiPrefix + beforePad + pad + afterPad + tradeIdStr + tempSuffix;

		return uti;
	}



    public void loadByProduct(Trade trade){

        if(null==trade){
            return;
        }
        Product product = trade.getProduct();
        if(null==product){
            return;
        }

        // Performance Swap
        if (product instanceof PerformanceSwap) {
            PerformanceSwap brs = (PerformanceSwap) product;
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
        }

        // Bond
        else if(product instanceof Bond){
            Bond bond = (Bond) product;
            String direction = "";

            if (bond.getNotional() < 0.0D) {
                direction = "Pay";
            } else if (bond.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                direction = "Pay";
            } else {
                direction = "Rec";
            }
            String type = loadProductIntType(bond);
            if ("Rec".equalsIgnoreCase(direction)) {
                if ("Fixed".equalsIgnoreCase(type)) {
                    this.bondRecFixed = true;
                } else {
                    this.bondRecFloating = true;
                }
            } else if ("Pay".equalsIgnoreCase(direction)) {
                if ("Fixed".equalsIgnoreCase(type)) {
                    this.bondPayFixed = true;
                } else {
                    this.bondPayFloating = true;
                }
            }
        }
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



    private boolean isEventOnD(JDatetime tradeEnteredDate, JDatetime oldTradeEnteredDate) {
        TimeZone tz = TimeZone.getDefault();
        Date enteredDate = tradeEnteredDate.getJDate(tz).getDate();
        Date oldEnteredDate =oldTradeEnteredDate.getJDate(tz).getDate();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(enteredDate).equals(fmt.format(oldEnteredDate));
    }



    private boolean isSameLei(DSConnection dsCon, Trade newTrade, Trade oldTrade) {
        final LegalEntity newEntity = newTrade.getCounterParty();
        final Collection<LegalEntityAttribute> newAttrs = BOCache.getLegalEntityAttributes(dsCon, newEntity.getId());
        final LegalEntity oldEntity = oldTrade.getCounterParty();
        final Collection<LegalEntityAttribute> oldAttrs = BOCache.getLegalEntityAttributes(dsCon, oldEntity.getId());
        final Iterator<LegalEntityAttribute> newIter = newAttrs.iterator();
        LegalEntityAttribute currentAtt = null;
        String newLei = "";

        while (newIter.hasNext()) {
            currentAtt = newIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                newLei = currentAtt.getAttributeValue();
            }
        }
        final Iterator<LegalEntityAttribute> oldIter = oldAttrs.iterator();
        String oldLei = "";
        while (oldIter.hasNext()) {
            currentAtt = oldIter.next();
            if (LE_ATTR_LEI.contains(currentAtt.getAttributeType())) {
                oldLei = currentAtt.getAttributeValue();
            }
        }
        if ((newLei!=null) && (oldLei!=null) && newLei.equalsIgnoreCase(oldLei)) {
            return true;
        }
        return false;
    }

	public boolean isNotReportableTrade(Trade trade, Collection<LegalEntityAttribute> poAttrs, Collection<LegalEntityAttribute> cpAttrs){
		return !isTradeReportable(trade,poAttrs,cpAttrs);
	}

    public boolean isTradeReportable(Trade trade, Collection<LegalEntityAttribute> poAttrs, Collection<LegalEntityAttribute> cpAttrs) {
        return (checkProductType(trade) && !isInternal(trade, poAttrs, cpAttrs));
    }



    private boolean isSameCounterparty(Trade newTrade, Trade oldTrade) {
        return newTrade.getCounterParty().getCode().equalsIgnoreCase(oldTrade.getCounterParty().getCode()) ? true : false;
    }



    public TradeArray getTradeByKeyword(String keywordName, String keywordValue) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection
                    .getDefault()
                    .getRemoteTrade()
                    .getTrades(
                            "trade, trade_keyword kwd",
                            "trade.trade_id=kwd.trade_id and kwd.keyword_name='" + keywordName + "' and kwd.keyword_value='" + keywordValue + "'",
                            null, null);
        } catch (RemoteException e) {
            Log.error(this, e);
            existingTrades = null;
        }
        return existingTrades;
    }


    private boolean isBondForward(Trade trade, String productType) {
        String bondForward = trade.getKeywordValue("BondForward");
        return (productType.equalsIgnoreCase("Bond") && (!Util.isEmpty(bondForward)) && ("true".equalsIgnoreCase(bondForward))) ? true : false;
    }


	private boolean isEquityCO2(Trade trade, String productType) {
		if(trade.getProduct() instanceof Equity){
			Equity equity = (Equity) trade.getProduct();
			String equityType = equity.getSecCode("EQUITY_TYPE");
			String spotFwd= trade.getKeywordValue("Mx_Product_SubType");
			return ((!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))) && (!Util.isEmpty(spotFwd) && "FORWARD".equalsIgnoreCase(spotFwd))) ? true : false;
		}
		return false;
	}


}