package calypsox.tk.upload.mapper;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.jaxb.BondDetail;
import com.calypso.tk.upload.jaxb.BondDetails;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.CollateralDetails;
import com.calypso.tk.upload.jaxb.CollateralPoolDetails;
import com.calypso.tk.upload.jaxb.CollateralRateDetail;
import com.calypso.tk.upload.jaxb.EquityDetail;
import com.calypso.tk.upload.jaxb.EquityDetails;
import com.calypso.tk.upload.jaxb.GeneralDetails;
import com.calypso.tk.upload.jaxb.HaircutDetails;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.jaxb.SecLending;
import com.calypso.tk.upload.jaxb.SecurityDetails;
import com.calypso.tk.upload.jaxb.SubstitutionDetail;
import com.calypso.tk.upload.mapper.DefaultMapper;

public class MurexSecLendingMapper extends DefaultMapper {
	
	public static final String REPROCESS_KW = "Reprocess";
	public static final String NEWSPREAD_KW = "NewSpread";
	public static final String RATE_AMENDMENT_KW = "HAS_RATE_AMENDMENT";
	public static final String TRUE = "true";
	public static final String FEE_RERATE_ACTION = "FEE_RERATE";
	public static final String CANCEL_ACTION = "CANCEL_ACTION";
	
	CalypsoTrade calypsoTrade;
	
	boolean reMap = false;
	
	public boolean isTermination() {
		return calypsoTrade.getAction().equals(Action.S_TERMINATE);
	}
	
	public void ReMap(CalypsoObject calypObject, String source, Vector<BOException> errors, Object dbConn) {
		reMap = true;
		this.reMap(calypObject, source, errors);
	}
	
	public void reMap(CalypsoObject calypObject, String source, Vector<BOException> errors) {
		if (calypObject instanceof CalypsoTrade) {
			calypsoTrade = (CalypsoTrade) calypObject;
			handleReprocess();
			mapProduct(calypsoTrade.getProduct().getSecLending());
			mapPartialReturn(calypsoTrade.getPartialReturn());
			if(hasEmptyReRate() && hasNewSpreadKeword() &&  (isCancelAction() || isFeeRerate())) {
				if(isCancelAction()) {
					removeKeyword(NEWSPREAD_KW);
				}
				calypsoTrade.setAction(Action.S_AMEND);
			}
		}
	}

	public void addKeyword(String name, String value) {
		Keyword newKeyword = new Keyword();
		newKeyword.setKeywordName(name);
		newKeyword.setKeywordValue(value);
		calypsoTrade.getTradeKeywords().getKeyword().add(newKeyword);
	}
	
	public void handleReprocess() {
		if(reMap) {
			addKeyword(REPROCESS_KW, TRUE);
		}
	}
	

	public void mapPartialReturn(SubstitutionDetail substitutionDetail) {
		if(substitutionDetail!=null) {
			mapBondDetails(substitutionDetail.getBondDetails());
			mapEquityDetails(substitutionDetail.getEquityDetails());
		}
		
	}
	
	public void mapProduct(SecLending secLending) {
		if(secLending!=null) {
			mapGeneralDetails(secLending.getGeneralDetails());
			mapSecurityDetails(secLending.getSecurityDetails());
			mapCollateralDetails(secLending.getCollateralDetails());
		}
	}
	
	public void mapGeneralDetails(GeneralDetails generalDetails) {
		if(generalDetails!=null) {
			if(isTermination())
				generalDetails.setTerminationType("TERM");
		}
		
	}
	
	public void mapCollateralDetails(CollateralDetails collateralDetails) {
		if(collateralDetails!=null) {
			boolean removeInitialMarginFXRate = false;
			if(collateralDetails.getCollateralCurrency()!=null && calypsoTrade.getTradeCurrency()!=null && calypsoTrade.getTradeCurrency().equals(collateralDetails.getCollateralCurrency())) {
				removeInitialMarginFXRate=true;
			}
			mapCollateralPoolDetails(collateralDetails.getCollateralPoolDetails(), removeInitialMarginFXRate);
		}
	}
	
	public void mapCollateralPoolDetails(CollateralPoolDetails collateralPoolDetails, boolean removeInitialMarginFXRate) {
		if(collateralPoolDetails!=null) {
			if(removeInitialMarginFXRate)
				collateralPoolDetails.setInitialMarginFXRate(null);
			mapCollateralRateDetail (collateralPoolDetails.getVMPoolRateDetails());
		}
	}
	
	public void mapCollateralRateDetail (CollateralRateDetail collateralRateDetail) {
		if(collateralRateDetail!=null) {
			if(hasEmptyReRate() && hasNewSpreadKeword() && isFeeRerate()) {
				Double newSpread = getKeyWordValueAsDouble(NEWSPREAD_KW);
				collateralRateDetail.setSpread(newSpread*100);
				this.addKeyword(RATE_AMENDMENT_KW, TRUE);
			}
		}
	}
	
	public void mapSecurityDetails(SecurityDetails securityDetails) {
		if(securityDetails!=null) {
			mapBondDetails(securityDetails.getBondDetails());
			mapEquityDetails(securityDetails.getEquityDetails());
		}
	}
	
	public void mapBondDetails(BondDetails bondDetails) {
		if(bondDetails!=null) {
			for(BondDetail bondDetail : bondDetails.getBondDetail()) {
				mapBondDetail(bondDetail);
			}
		}
	}
	
	public void mapEquityDetails(EquityDetails equityDetails) {
		if(equityDetails!=null) {
			for(EquityDetail equityDetail : equityDetails.getEquityDetail()) {
				mapEquityDetail(equityDetail);
			}
		}
	}
	
	public void mapEquityDetail(EquityDetail equityDetail) {
		if(equityDetail!=null) {
			equityDetail.setReturnMarginCallCash(null);
			if("Common".equals(equityDetail.getProductCodeType())) {
				if(!reMap && equityDetail.getFxRate()!=null && !equityDetail.getFxRate().equals(0.0d)) {
					equityDetail.setFxRate(1.0d/equityDetail.getFxRate());
				}
			}
			mapHaircutDetails(equityDetail.getHaircutDetails());
		}
	}
	
	public void mapBondDetail(BondDetail bondDetail) {
		if(bondDetail!=null) {
			bondDetail.setReturnMarginCallCash(null);
			mapHaircutDetails(bondDetail.getHaircutDetails());
		}
	}
	
	public void mapHaircutDetails(HaircutDetails haircutDetails) {
		if(haircutDetails!=null) {
			if(haircutDetails.getHairCutPercentage()>=100.0d) {
				haircutDetails.setHairCutPercentage(haircutDetails.getHairCutPercentage()-100.0d);
			}
		}
	}
	
	public String getKeyWordValue(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword.getKeywordValue();
				}
			}
		}
		return null;
	}
	
	public void removeKeyword(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			ListIterator<Keyword> keywordIt = calypsoTrade.getTradeKeywords().getKeyword().listIterator();
			
			while(keywordIt.hasNext()) {
				Keyword kw = keywordIt.next();
				if(kw.getKeywordName().equals(keywordName)) {
					kw.setKeywordValue(null);
				}
			}
		}

	}
	
	public boolean isCancelAction() {
		return calypsoTrade.getAction().equals(CANCEL_ACTION);
	}
	
	public boolean isFeeRerate() {
		return calypsoTrade.getAction().equals(FEE_RERATE_ACTION);
	}
	
	public boolean hasNewSpreadKeword() {
		if(getKeyWordValue(NEWSPREAD_KW)==null) {
			return false;
		}
		return true;
	}
	public Double getKeyWordValueAsDouble(String keywordName) {
		String keywordValue = getKeyWordValue(keywordName);
		if(Util.isEmpty(keywordValue))
			return 0.0d;
		return Double.parseDouble(keywordValue);
	}
	
	public boolean hasEmptyReRate() {
		
		if(calypsoTrade.getFeeReRate()!=null) {
			return calypsoTrade.getFeeReRate().getFeeReRate()==0.0d;
		}
		return true;

	}
	

}
