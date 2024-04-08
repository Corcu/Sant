package calypsox.tk.upload.mapper;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.*;
import com.calypso.tk.upload.mapper.DefaultMapper;
import com.calypso.tk.upload.util.UploaderTradeUtil;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Vector;

public class MurexRepoMapper extends DefaultMapper {

	public static final String LOG_CATEGORY = "MurexRepoMapper";

	private String source = null;
	public static final String COUPON_FREQUENCY = "CouponFrequency";

	public static final String EXTENDABLE_NOTICE_KEYWORD = "ExtendableNotice";
	public static final String MUREX_VERSION_NUMBER_KEYWORD = "MurexVersionNumber";
	public static final String HAIRCUT_FORMULA_KEYWORD = "HAIRCUT_FORMULA";
	public static final String WKF_SUBTYPE_KEYWORD = "WorkflowSubType";
	public static final String WKF_SUBTYPE_KEYWORD_REPO_MUREX = "RepoMurex";
	public static final String WKF_MXLASTEVENT_KEYWORD_REPO_MUREX = "MxLastEvent";
	public static final String TRADE_LENGTH_KEYWORD = "TradeLength";
	
	public static final String MX_LAST_EVENT_PORTFOLIO_ASSIGNMENT_VALUE = "PORTFOLIO_ASSIGNMENT";

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	CalypsoTrade calypsoTrade;

	@Override
  public void reMap(CalypsoObject calypObject, String source, Vector<BOException> errors) {
		setSource(source);
		if (calypObject instanceof CalypsoTrade) {
			calypsoTrade = (CalypsoTrade) calypObject;
			mapKeywords(calypsoTrade.getTradeKeywords());
			mapTrade(calypsoTrade);
			mapTermination(calypsoTrade);
			if(calypsoTrade.getProduct()!=null)
				mapProduct(calypsoTrade.getProduct().getRepo());
		}
	}

	public void mapTermination(CalypsoTrade calypsoTrade){
		if("TERMINATE".equalsIgnoreCase(calypsoTrade.getAction())){
			if(Optional.ofNullable(calypsoTrade.getTermination()).isPresent()){
				Termination termination = calypsoTrade.getTermination();
				termination.setSettleInterestAmount(null); //Null o 0.0 ??
				calypsoTrade.setTermination(termination);
			}
		}
	}

	public void mapKeywords(TradeKeywords keywords) {
		if(keywords==null) {
			keywords=new TradeKeywords();
			calypsoTrade.setTradeKeywords(keywords);
		}
		String workflowSubType = getKeyWordValue(WKF_SUBTYPE_KEYWORD);
		if(Util.isEmpty(workflowSubType)) {
			Keyword newKeyword = new Keyword();
			newKeyword.setKeywordName(WKF_SUBTYPE_KEYWORD);
			newKeyword.setKeywordValue(WKF_SUBTYPE_KEYWORD_REPO_MUREX);
			keywords.getKeyword().add(newKeyword);
		}
	}
	
	public void mapTrade(CalypsoTrade trade) {
		if(trade!=null) {
			mapTradeFee(trade.getTradeFee());
		}
	}

	public void mapTradeFee(TradeFee tradeFee) {
		if(tradeFee!=null) {
			for(final Fee fee : tradeFee.getFee()) {
				mapFee(fee);
			}
		}
	}

	public void mapFee(Fee fee) {
		if(fee !=null) {
			if(Util.isEmpty(fee.getFeeCounterParty())) {
				if(calypsoTrade.getTradeCounterParty() != null) {
					fee.setFeeCounterParty(calypsoTrade.getTradeCounterParty());
				}
				else {
					final Book book = BOCache.getBook(DSConnection.getDefault(), calypsoTrade.getTradeBook());
					final LegalEntity bookPO = BOCache.getLegalEntity(DSConnection.getDefault(), book.getProcessingOrgBasedId());
					fee.setFeeCounterParty(bookPO.getCode());
				}
			}
		}
	}

	public void mapProduct(Repo repo) {
		if (repo != null) {
			if (isOpenTermExtendable(repo)) {
				final XMLGregorianCalendar callableDate = getCalculatedCallableDate(repo);
				if (callableDate != null) {
					repo.setCallableDate(callableDate);
				}else {
					repo.setCallableDate(calypsoTrade.getMaturityDate());
				}
			}else {
				repo.setCallableDate(null);
			}
			updateMaturityDate(repo);
			mapFundingDetails(repo.getFundingDetails());
			mapHaircut(repo);

		}
	}

	public void mapHaircut(Repo repo){
		String haircutFormula = getKeyWordValue(HAIRCUT_FORMULA_KEYWORD);
		if("Haircut X".equalsIgnoreCase(haircutFormula)){
			String direction = repo.getDirection();
			if(validHaircut(repo)){
				HaircutDetails haircutDetails = repo.getSecurityDetails().getBondDetails().getBondDetail().get(0).getHaircutDetails();
				double hairCutPercentage = 100-haircutDetails.getHairCutPercentage();
				haircutDetails.setHairCutPercentage(Math.abs(hairCutPercentage));

				if("Repo".equalsIgnoreCase(direction) || "Sell".equalsIgnoreCase(direction)){
					if(hairCutPercentage<0.0){
						haircutDetails.setHairCutDirection("Receive");
					}else if(hairCutPercentage>=0.0){
						haircutDetails.setHairCutDirection("Give");
					}
				}else if("Reverse".equalsIgnoreCase(direction) || "Buy".equalsIgnoreCase(direction)){
					if(hairCutPercentage<0.0){
						haircutDetails.setHairCutDirection("Give");
					}else if(hairCutPercentage>=0.0){
						haircutDetails.setHairCutDirection("Receive");
					}
				}
				repo.getSecurityDetails().getBondDetails().getBondDetail().get(0).setHaircutDetails(haircutDetails);
			}
		}
	}

	private boolean validHaircut(Repo repo){
		return null!=repo && null!=repo.getSecurityDetails() && null!=repo.getSecurityDetails().getBondDetails() && null!=repo.getSecurityDetails().getBondDetails().getBondDetail().get(0);
	}

	public void mapFundingDetails(RepoFunding repoFunding) {
		if(repoFunding!=null) {
			repoFunding.setDateRollConvention(getCalypsoValue(DATE_ROLL,repoFunding.getDateRollConvention()));
			if(repoFunding.getCompoundingFrequency()!=null
					&&repoFunding.getCompoundingFrequency().equals(repoFunding.getCouponFrequency())) {
				repoFunding.setCompoundingFrequency(null);
			}
		}
	}

	public String getCalypsoValue(String typeName, String interfaceValue) {
		return getCalypsoValue(getSource(),typeName,interfaceValue,null);
	}

	public String getKeyWordValue(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(final Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword.getKeywordValue();
				}
			}
		}
		return null;

	}

	/**
   * If OpenTerm = OPEN Then If ProcessDate < Start Date Then Notice Days = MaturityDate â€“ StartDate
   * MaturityDate = Empty Else Notice Days = MaturityDate â€“ ProcessDate MaturityDate = Empty
   *
   *
   * Example : Today is 2020-07-22 We receive <MaturityDate>2020-08-17</ MaturityDate> We integrate
   * <NoticeDays>26</NoticeDays> <MaturityDate></ MaturityDate>
   *
   *
   * If OpenTerm = EVERGREEN Then MaturityDate = StartDate + TradeKeyword.TradeLength
   *
   * Example : We receive <StartDate>2020-08-17</ StartDate> <MaturityDate>2023-08-17</ MaturityDate>
   * <Keyword> <KeywordName>TradeLength</KeywordName> <KeywordValue>3M</KeywordValue> </Keyword>
   *
   * We integrate <StartDate>2020-08-17</ StartDate> <MaturityDate>2020-11-17</ MaturityDate>
   *
   * @param repo
   */

	public void updateMaturityDate (Repo repo) {

		if(isOpenTermOpen(repo)) {
			repo.setNoticeDays(1);
        	calypsoTrade.setMaturityDate(null);
		}

		if(isOpenTermEverGreen(repo)) {
			final String tradeLength = getKeyWordValue(TRADE_LENGTH_KEYWORD);
			if (!Util.isEmpty(tradeLength) && calypsoTrade.getStartDate() != null) {
				final Tenor tenor = Tenor.valueOf(tradeLength);
				JDate date = JDate.valueOf(calypsoTrade.getStartDate().toGregorianCalendar());
				if (tradeLength.toUpperCase().contains("D")) {
					date = date.addDays(tenor.getCode());
				}
				else {
					date = date.addTenor(tenor);
				}
				calypsoTrade.setMaturityDate(dateToCalendar(date));
			}

		}


	}


	public XMLGregorianCalendar getCalculatedCallableDate(Repo repo) {
		XMLGregorianCalendar callableDate = null;
		final String extendableNotice = getKeyWordValue(EXTENDABLE_NOTICE_KEYWORD);
		if (Util.isEmpty(extendableNotice)) {
			return null;
		}
		
		
		if(null!=repo){
			final Tenor tenor = Tenor.valueOf(extendableNotice);
			JDate date = UploaderTradeUtil.getDate(calypsoTrade.getStartDate());
			date = date.addTenor(tenor);
			callableDate = dateToCalendar(date.getDate());
		}
		return callableDate;

	}

	public static XMLGregorianCalendar dateToCalendar(JDate date) {
		return dateToCalendar(date.getDate());
	}

    public static XMLGregorianCalendar dateToCalendar(Date date) {
        if (date != null) {
            final GregorianCalendar greg = new GregorianCalendar();
            greg.setTime(date);
            try {
				return DatatypeFactory.newInstance().newXMLGregorianCalendar(greg);
			} catch (final DatatypeConfigurationException e) {
				Log.error(LOG_CATEGORY, e);
			}
        }
        return null;
    }

    public boolean isOpenTermExtendable(Repo repo) {
    	return repo.getOpenTerm()!=null && repo.getOpenTerm().equals("EXTENDABLE");
    }

    public boolean isOpenTermOpen(Repo repo) {
    	return repo.getOpenTerm()!=null && repo.getOpenTerm().equals("OPEN");
    }

    public boolean isOpenTermEverGreen(Repo repo) {
    	return repo.getOpenTerm()!=null && repo.getOpenTerm().equals("EVERGREEN");
    }

}
