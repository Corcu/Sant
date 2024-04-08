package calypsox.tk.report;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import org.apache.camel.model.OtherwiseDefinition;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Basket;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import calypsox.tk.report.emir.field.EmirFieldBuilderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.util.collateral.CollateralUtilities;


public class MaterialTermsReportLogic {


	private static final String VALUE_PO_BSTE = "BSTE";
	private static final String VALUE_PO_BDSD = "BDSD";
	private static final String VALUE_EMPTY = "";
	private static final String VALUE_TRUE = "TRUE";
	private static final String VALUE_YES = "Yes";
	private static final String VALUE_Y = "Y";
	private static final String VALUE_NO = "NO";
	private static final String VALUE_N = "N";
	private static final String VALUE_BRS = "BRS";
	private static final String VALUE_CREDIT_TOTALRETURNSWAP = "Credit:TotalReturnSwap";
	private static final String VALUE_CREDIT = "Credit";
	private static final String VALUE_PERFORMANCESWAP = "PerformanceSwap";
	private static final String VALUE_MUREX_FXFI = "MUREX FXFI";
	private static final String VALUE_CALYPSO_STC = "Calypso STC";
	private static final String VALUE_MATCHED = "Matched";
	private static final String VALUE_MADRID = "Madrid";
	private static final String VALUE_PO = "PO";
	private static final String VALUE_CP = "CP";
	private static final String VALUE_ESMA = "ESMA";
	private static final String VALUE_CFTC_ESMA = "CFTC,ESMA";
	private static final String VALUE_OFF_FACILITY = "Off-Facility";
	private static final String VALUE_SD = "SD";
	private static final String VALUE_US_COUNTERPARTY_DOMICILE = "US Counterparty Domicile";
	private static final String VALUE_BRANCH_US_SWAP_DEALER = "Non-US Branch of US Swap Dealer";
	private static final String VALUE_AFFILIATED_US_GUARANTEED = "Non-US Affiliate of a US person guaranteed by a US Person";
	private static final String VALUE_CONDUIT_US_PERSON = "Non-US Affiliate Conduit";
	private static final String VALUE_MSP = "MSP";
	private static final String VALUE_NONSD_MP = "NONSD_MP";
	private static final String TRADE_KWRD_USI_REFERENCE = "USI_REFERENCE";
	private static final String TRADE_KWRD_UTI_REFERENCE = "UTI_REFERENCE";
	private static final String TRADE_KWRD_UTI_TEMP = "TempUTITradeId";
	private static final String TRADE_KWRD_MATCHING_STATUS = "MatchingStatus";
	private static final String TRADE_KWRD_REPORTING_PARTY = "ReportingParty";
	private static final String TRADE_KWRD_PLATFORM = "Plarform";
	private static final String TRADE_KWRD_MUREX_TRANSFER_FROM = "MurexTransferFrom";
	private static final String TRADE_KWRD_MUREX_TRADE_ID = "MurexTradeID";
	private static final String TRADE_KWRD_MX_LAST_EVENT = "MxLastEvent";
	private static final String TRADE_EVENT_ISHARES_MODIFICATION = "mxContractEventRatesISHARES_MODIFICATION";
	private static final String LE_ATTR_SWAP_DEALER = "SWAP_DEALER";
	private static final String LE_ATTR_EMIR_FULL_DELEG = "EMIR_FULL_DELEG";
	private static final String LE_ATTR_LEI = "LEI";
	private static final String LE_ATTR_US_COUNTERPARTY_DOMICILE = "US_CONTERPARTY_DOMICILE";
	private static final String LE_ATTR_BRANCH_US_SWAP_DEALER = "BRANCH_US_SWAP_DEALER";
	private static final String LE_ATTR_AFFILIATED_US_GUARANTEED = "AFFILIATED_US_GUARANTEED";
	private static final String LE_ATTR_CONDUIT_US_PERSON = "CONDUIT_US_PERSON";
	private static final String LE_ATTR_MSP = "MSP";
	private static final String LE_ATTR_RISK_SECTOR = "RISK_SECTOR";
	private static final String REPORT_FILE_NAME = "Santander_Madrid_Calypso_Material_Terms_STC_";
	private static final String FILE_EXTENSION = "csv";
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	private static final String CALENDAR_SYSTEM = "SYSTEM";

	private Trade trade;
	private PerformanceSwap product;
	private PerformanceSwapLeg performanceSwapLeg;
	private SwapLeg swapLeg; 
	private JDatetime valDatetime;
	private LegalEntity po;
	private LegalEntity cpty;
	private final Map<String, String> poAttrs = new HashMap<String, String>();
	private final Map<String, String> cptyAttrs = new HashMap<String, String>();


	public MaterialTermsReportLogic(final Trade trade, final JDatetime valDatetime) {
		this.trade = trade;
		this.valDatetime = valDatetime;
		final Book book = this.trade.getBook();
	    if (book != null) {
	    	po = book.getLegalEntity();
	    	cpty = trade.getCounterParty();
	    	fillLegalEntityAttributes(poAttrs, po);
	    	fillLegalEntityAttributes(cptyAttrs, cpty);
	    }
        this.product = (PerformanceSwap) this.trade.getProduct();
        this.performanceSwapLeg = (PerformanceSwapLeg) product.getPrimaryLeg();
	    this.swapLeg = (SwapLeg) product.getSecondaryLeg();
	}


	public void fillItem(final MaterialTermsReportItem item, final Vector<String> errors) {
		item.setColumnValue(MaterialTermsColumns.USI.toString(), getLogicUsi());
	    item.setColumnValue(MaterialTermsColumns.TRADEPARTYVAL_1.toString(), getLogicTradePartyVal1());
	    item.setColumnValue(MaterialTermsColumns.CFTC_CATEGORY_1.toString(), getLogicCftcCategory1());
	    item.setColumnValue(MaterialTermsColumns.US_PERSON_1.toString(), getLogicUsPerson1());
	    item.setColumnValue(MaterialTermsColumns.TRADEPARTYVAL_2.toString(), getLogicTradePartyVal2());
	    item.setColumnValue(MaterialTermsColumns.CFTC_CATEGORY_2.toString(), getLogicCftcCategory2());
	    item.setColumnValue(MaterialTermsColumns.US_PERSON_2.toString(), getLogicUsPerson2());
	    item.setColumnValue(MaterialTermsColumns.UPI.toString(), getLogicUpi());
	    item.setColumnValue(MaterialTermsColumns.MULTI_ASSET.toString(), getLogicMultiAsset());
	    item.setColumnValue(MaterialTermsColumns.ASSETCLASS.toString(), getLogicAssetClass());
	    item.setColumnValue(MaterialTermsColumns.SECONDARY_ASSET_CLASS.toString(), getLogicSecondaryAssetClass());
	    item.setColumnValue(MaterialTermsColumns.REGULATED_BY.toString(), getLogicRegulatedBy());
	    item.setColumnValue(MaterialTermsColumns.ADDITIONAL_REPOSITORY.toString(), getLogicAdditionalRepository());
	    item.setColumnValue(MaterialTermsColumns.EXECUTION_VENUE_PRF.toString(), getLogicExecutionVenuePrf());
	    item.setColumnValue(MaterialTermsColumns.EXECUTION_VENUE.toString(), getLogicExecutionVenue());
	    item.setColumnValue(MaterialTermsColumns.BUYER_LEI.toString(), getLogicBuyerLei());
	    item.setColumnValue(MaterialTermsColumns.SELLER_LEI.toString(), getLogicSellerLei());
	    item.setColumnValue(MaterialTermsColumns.REFERENCE_ENTITY.toString(), getLogicReferenceEntity());
	    item.setColumnValue(MaterialTermsColumns.UNDERL_ASSET.toString(), getLogicUnderlAsset());
	    item.setColumnValue(MaterialTermsColumns.START_DATE.toString(), getLogicStartDate());
	    item.setColumnValue(MaterialTermsColumns.END_DATE.toString(), getLogicEndDate());
	    item.setColumnValue(MaterialTermsColumns.PRICE.toString(), getLogicPrice());
	    item.setColumnValue(MaterialTermsColumns.NOTIONAL_AMOUNT.toString(), getLogicNotionalAmount());
	    item.setColumnValue(MaterialTermsColumns.NOTIONAL_CURRENCY.toString(), getLogicNotionalCurrency());
	    item.setColumnValue(MaterialTermsColumns.PAY_FREQ_PERIOD.toString(), getLogicPayFreqPeriod());
	    item.setColumnValue(MaterialTermsColumns.PAY_FREQ_PERIOD_MULT.toString(), getLogicPayFreqPeriorMult());
	    item.setColumnValue(MaterialTermsColumns.UPFRONT_FEE.toString(), getLogicUpfrontFee());
	    item.setColumnValue(MaterialTermsColumns.UPFRONT_CURRENCY.toString(), getLogicUpfrontCurrency());
	    item.setColumnValue(MaterialTermsColumns.COLLATERALIZED.toString(), getLogicCollateralized());
	    item.setColumnValue(MaterialTermsColumns.BACK_OFFICE_ID.toString(), getLogicBackOfficeId());
	    item.setColumnValue(MaterialTermsColumns.BACK_OFFICE_SYSTEM.toString(), getLogicBackOfficeSystem());
	    item.setColumnValue(MaterialTermsColumns.FRONT_OFFICE_ID.toString(), getLogicFrontOfficeId());
	    item.setColumnValue(MaterialTermsColumns.REC_DATE.toString(), getLogicRecDate());
	    item.setColumnValue(MaterialTermsColumns.BRANCH.toString(), getLogicBranch());
	    item.setColumnValue(MaterialTermsColumns.FEED.toString(), getLogicFeed());
	    item.setColumnValue(MaterialTermsColumns.STRUCTURE_ID.toString(), getLogicStructureId());
	    item.setColumnValue(MaterialTermsColumns.STRUCTURE_TYPE.toString(), getLogicStructureType());
	    item.setColumnValue(MaterialTermsColumns.INSTRUMENT.toString(), getLogicInstrument());
	    item.setColumnValue(MaterialTermsColumns.SOURCE_SYSTEM_FO.toString(), getLogicSourceSystemFo());
	    item.setColumnValue(MaterialTermsColumns.SHORT_NAME.toString(), getLogicShortName());
	    item.setColumnValue(MaterialTermsColumns.PORTFOLIO.toString(), getLogicPortfolio());
	    item.setColumnValue(MaterialTermsColumns.TRADE_DATE.toString(), getLogicTradeDate());
	    item.setColumnValue(MaterialTermsColumns.INDIVIDUAL_INDICATOR.toString(), getLogicIndividualIndicator());
	    item.setColumnValue(MaterialTermsColumns.RISK_SECTOR_CTY.toString(), getLogicRiskSectorCty());
	    item.setColumnValue(MaterialTermsColumns.UTI.toString(), getLogicUti());
	    item.setColumnValue(MaterialTermsColumns.INTERNAL_CODE.toString(), getLogicInternalCode());
	    item.setColumnValue(MaterialTermsColumns.MARKIT_ID.toString(), getLogicMarkitId());
	    item.setColumnValue(MaterialTermsColumns.NOMINAL_CURR.toString(), getLogicNominalCurr());
	    item.setColumnValue(MaterialTermsColumns.LIVE_NOMINAL.toString(), getLogicLiveNominal());
	    item.setColumnValue(MaterialTermsColumns.ORIG_NOMINAL.toString(), getLogicOrigNominal());
	    item.setColumnValue(MaterialTermsColumns.CONFIRM_SYSTEM.toString(), getLogicConfirmSystem());
	    item.setColumnValue(MaterialTermsColumns.UNADJUSTED_MATDATE.toString(), getLogicUnadjustedMatDate());
	    item.setColumnValue(MaterialTermsColumns.COUNTERPARTY_NAME.toString(), getLogicCounterpartyName());
	    item.setColumnValue(MaterialTermsColumns.DFA_REPORTING.toString(), getLogicDfaReporting());
	    item.setColumnValue(MaterialTermsColumns.EMIR_REPORTING.toString(), getLogicEmirReporting());
	    item.setColumnValue(MaterialTermsColumns.SWAP_DEALER_ID.toString(), getLogicSwapDealerId());
	    item.setColumnValue(MaterialTermsColumns.CRD_EMIR_CLEARING_CATEGORY.toString(), getLogicCrdEmirClearingCategory());
	    item.setColumnValue(MaterialTermsColumns.EMIR_FULL_DELEGATION.toString(), getLogicEmirFullDelegation());
	    item.setColumnValue(MaterialTermsColumns.TRADE_STATUS.toString(), getLogicTradeStatus());
	    item.setColumnValue(MaterialTermsColumns.INSTRUMENT_TYPE.toString(), getLogicInstrumentType());
	    item.setColumnValue(MaterialTermsColumns.RESIDENCE_COUNTRY.toString(), getLogicResidenceCountry());	    
	    item.setColumnValue(MaterialTermsColumns.TRADER.toString(), getLogicTrader());
	    item.setColumnValue(MaterialTermsColumns.SELLER.toString(), getLogicSeller());
	    item.setColumnValue(MaterialTermsColumns.ORIGINAL_COUNTERPARTY.toString(), getLogicOriginalCounterparty());
	    item.setColumnValue(MaterialTermsColumns.ORIG_CPTY_DFA_OWNER_TYPE.toString(), getLogicOrigCptyDfaOwnerType());
	    item.setColumnValue(MaterialTermsColumns.MTM.toString(), getLogicMtm());
	    item.setColumnValue(MaterialTermsColumns.MTM_CURRENCY.toString(), getLogicMtmCurrency());	    
	}


	private String getLogicUsi() {
		return trade.getKeywordValue(TRADE_KWRD_USI_REFERENCE);
	}


	private String getLogicTradePartyVal1() {
		String attrValue = poAttrs.get(LE_ATTR_LEI);
		if(attrValue == null) {
			attrValue = "";
		}
		return "LEI - " + attrValue;
	}


	private String getLogicCftcCategory1() {
		String po = trade.getBook().getLegalEntity().getCode();
		if(!Util.isEmpty(po) && (VALUE_PO_BSTE.equalsIgnoreCase(po) || VALUE_PO_BDSD.equalsIgnoreCase(po))) {
			return VALUE_SD;
		}
		else {
			return VALUE_EMPTY;
		}
	}


	private String getLogicUsPerson1() {
		return VALUE_NO;
	}


	private String getLogicTradePartyVal2() {
		String attrValue = cptyAttrs.get(LE_ATTR_LEI);
		if(attrValue == null) {
			attrValue = "";
		}
		return "LEI - " + attrValue;
	}


	private String getLogicCftcCategory2() {
		String attrSwapDealer = cptyAttrs.get(LE_ATTR_SWAP_DEALER);
		if(!Util.isEmpty(attrSwapDealer) && VALUE_Y.equalsIgnoreCase(attrSwapDealer)) {
			return VALUE_SD;
		}
		else {
			String attrMsp = cptyAttrs.get(LE_ATTR_MSP);
			if(!Util.isEmpty(attrMsp) && VALUE_Y.equalsIgnoreCase(attrMsp)) {
				return VALUE_MSP;
			}
		}
		return VALUE_NONSD_MP;
	}


	private String getLogicUsPerson2() {
		String attrUsCptyDomicile = cptyAttrs.get(LE_ATTR_US_COUNTERPARTY_DOMICILE); 		
		if(Util.isEmpty(attrUsCptyDomicile) || (!Util.isEmpty(attrUsCptyDomicile) && VALUE_N.equalsIgnoreCase(attrUsCptyDomicile))) {
			String attrBranchUsSwapDealer = cptyAttrs.get(LE_ATTR_BRANCH_US_SWAP_DEALER);
			String attrAffiliatedUsGuaranteed = cptyAttrs.get(LE_ATTR_AFFILIATED_US_GUARANTEED);
			String attrConduitUsPerson = cptyAttrs.get(LE_ATTR_CONDUIT_US_PERSON);
			if (((attrBranchUsSwapDealer == null) || 
				 attrBranchUsSwapDealer != null && VALUE_N.equalsIgnoreCase(attrBranchUsSwapDealer))
				&&
				 ((attrAffiliatedUsGuaranteed == null) || 
				 attrAffiliatedUsGuaranteed != null && VALUE_N.equalsIgnoreCase(attrAffiliatedUsGuaranteed))
				&&
				 ((attrConduitUsPerson == null) || 
				 attrConduitUsPerson != null && VALUE_N.equalsIgnoreCase(attrConduitUsPerson)))
			{
			return VALUE_NO;
			}
		}
		return VALUE_YES;
	}


	private String getLogicUpi() {
		return VALUE_CREDIT_TOTALRETURNSWAP;
	}


	private String getLogicMultiAsset() {
		return VALUE_EMPTY;
	}


	private String getLogicAssetClass() {
		return VALUE_CREDIT;
	}


	private String getLogicSecondaryAssetClass() {
		return VALUE_EMPTY;
	}


	private String getLogicRegulatedBy() {
		String tradeKwrd = trade.getKeywordValue(TRADE_KWRD_REPORTING_PARTY);
		if(!Util.isEmpty(tradeKwrd) && (VALUE_PO.equalsIgnoreCase(tradeKwrd) || VALUE_CP.equalsIgnoreCase(tradeKwrd))) {
			return VALUE_CFTC_ESMA;
		}
		else {
			return VALUE_ESMA;	
		}
	}


	private String getLogicAdditionalRepository() {
		return VALUE_EMPTY;
	}


	private String getLogicExecutionVenuePrf() {
		return VALUE_EMPTY;
	}


	private String getLogicExecutionVenue() {
		String tradeKwrd = trade.getKeywordValue(TRADE_KWRD_PLATFORM);
		if(!Util.isEmpty(tradeKwrd)) {
			return tradeKwrd;
		}
		else {
			return VALUE_OFF_FACILITY;	
		}
	}


	private String getLogicBuyerLei() {
		String value = "";
		PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) product.getPrimaryLeg();
		if(performanceSwapLeg !=null) {
			  if(performanceSwapLeg.getBuySell(trade)==1){
				  value = poAttrs.get(LE_ATTR_LEI);
				  if(Util.isEmpty(value)) {
					  value = po.getCode();
				  }
			  }
			  else {
				  value = cptyAttrs.get(LE_ATTR_LEI);
				  if(Util.isEmpty(value)) {
					  value = cpty.getCode();
				  }
				  
			  }
		}
		return value;
	}


	private String getLogicSellerLei() {
		String value = "";
		PerformanceSwapLeg performanceSwapLeg = (PerformanceSwapLeg) product.getPrimaryLeg();
		if(performanceSwapLeg !=null) {
			  if(performanceSwapLeg.getBuySell(trade)==1){
				  value = cptyAttrs.get(LE_ATTR_LEI);
				  if(Util.isEmpty(value)) {
					  value = cpty.getCode();
				  }
			  }
			  else {
				  value = poAttrs.get(LE_ATTR_LEI);
				  if(Util.isEmpty(value)) {
					  value = po.getCode();
				  }
			  }
		}
		return value;
	}


	private String getLogicReferenceEntity() {
        String rst = VALUE_EMPTY;
        if (trade.getProduct() instanceof PerformanceSwap) {
            PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);
            if (pLeg != null) {
                if (pLeg.getReferenceProduct() instanceof Bond) {
                    Bond bond = (Bond) pLeg.getReferenceProduct();
                    int leId = bond.getIssuerId();
                    LegalEntity le = null;
                    if(leId!=0) {
                    	le = BOCache.getLegalEntity(DSConnection.getDefault(), leId);
                    	if(le!=null) {
                    		rst = le.getName();
                    	}
                    }
                    
                }
            }
        }
        if (Util.isEmpty(rst)) {
            rst = VALUE_EMPTY;
        }
        return rst;
	}


	private String getLogicUnderlAsset() {
        String rst = VALUE_EMPTY;
        String uType = EmirFieldBuilderUtil.getInstance().getLogicUNDERLYNGASSETTYPE(trade);
        if (EmirSnapshotReduxConstants.ISIN.equalsIgnoreCase(uType)) {
            PerformanceSwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getBondSecurityLeg(trade);

            if (pLeg.getLegConfig().equalsIgnoreCase(EmirSnapshotReduxConstants.LEG_SINGLE_ASSET)) {
                rst = pLeg.getReferenceProduct().getSecCode(EmirSnapshotReduxConstants.ISIN);

            } else if (pLeg.getReferenceProduct() instanceof Basket)  {
                Basket basket = (Basket) pLeg.getReferenceProduct();
                rst = getBasketISINS(basket);
            }
        }

        if (Util.isEmpty(rst)) {
            rst = VALUE_EMPTY;
        }

        return rst;
	}


	private String getLogicStartDate() { 
		String startDate = "";
		JDatetime date = new JDatetime(swapLeg.getStartDate(), TimeZone.getDefault());
		if (date != null) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			startDate = dateFormat.format(date);
		}
		return startDate;
	}


	private String getLogicEndDate() {
		String endDate = "";
		JDatetime date = new JDatetime(swapLeg.getEndDate(), TimeZone.getDefault());
		if (date != null) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			endDate = dateFormat.format(date);
		}
		return endDate;
	}


	private String getLogicPrice() {
		return EmirFieldBuilderUtil.getInstance().getlogicFIXRATE(trade);
	}


	private String getLogicNotionalAmount() {
        String rst = VALUE_EMPTY;
        JDate eventDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            if ("Bullet".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                Double value = pLeg.getPrincipal();
                if (Double.compare(value, 0.0D) != 0)  {
                    rst = new BigDecimal(Math.abs(value)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                }
            } else if ("Schedule".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                SwapLeg swapLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
                if (swapLeg != null) {
                    if (!swapLeg.getFlows().isEmpty()) {
                        for (CashFlow cf : swapLeg.getFlows()) {
                            if (cf.getDate().gte(eventDate)) {
                                if (cf instanceof CashFlowInterest) {
                                    Double amount = ((CashFlowInterest) cf).getNotional();
                                    rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rst;
	}


	private String getLogicNotionalCurrency() {
        String rst = VALUE_EMPTY;
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            rst = pLeg.getCurrency();
        }
        return rst;
	}


	private String getLogicPayFreqPeriod() {
        String rst = VALUE_EMPTY;
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            rst = EmirFieldBuilderUtil.getInstance().getMappedValueCouponFrequency(pLeg.getCouponFrequency().toString(), 1);
        }
        return rst;
	}


	private String getLogicPayFreqPeriorMult() {
	    String rst = VALUE_EMPTY;
	    SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
	    if (pLeg != null) {
	      rst = EmirFieldBuilderUtil.getInstance().getMappedValueCouponFrequency(pLeg.getCouponFrequency().toString(), 0);
	    }
	    return rst;
	}


	private String getLogicUpfrontFee() {
		return EmirFieldBuilderUtil.getInstance().getLogicINIPAYMENTAMOUNT(trade);
	}


	private String getLogicUpfrontCurrency() {
        Fee upFront = EmirFieldBuilderUtil.getInstance().getPremiumFee(trade);
        if (upFront != null) {
            return upFront.getCurrency();
        }
        return VALUE_EMPTY;
	}


	private String getLogicCollateralized() {
		return EmirFieldBuilderUtil.getInstance().getLogicCollateralized(trade);
	}


	private String getLogicBackOfficeId() {
		return String.valueOf(trade.getLongId());
	}


	private String getLogicBackOfficeSystem() {
		return VALUE_CALYPSO_STC;
	}


	private String getLogicFrontOfficeId() {
		String tradeKwrd = trade.getKeywordValue(TRADE_KWRD_MUREX_TRADE_ID);
		if(!Util.isEmpty(tradeKwrd)) {
			return tradeKwrd;
		}
		else {
			return VALUE_EMPTY;	
		}
	}


	private String getLogicRecDate() {
		String recDate = ""; 
		if (valDatetime != null) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			recDate = dateFormat.format(valDatetime);
		}
		return recDate;
	}
  

	private String getLogicBranch() {
		return VALUE_MADRID;
	}


	private String getLogicFeed() {
		final StringBuffer rst = new StringBuffer(REPORT_FILE_NAME);
		final SimpleDateFormat sdfFeed = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
		final String date = sdfFeed.format(valDatetime.getJDate(TimeZone.getDefault()).getJDatetime(TimeZone.getDefault()));
		rst.append(date);
		rst.append(".");
		rst.append(FILE_EXTENSION);
		return rst.toString();
	}


	private String getLogicStructureId() {
		return VALUE_EMPTY;
	}


	private String getLogicStructureType() {
		return VALUE_EMPTY;
	}


	private String getLogicInstrument() {
		return VALUE_PERFORMANCESWAP;
	}


	private String getLogicSourceSystemFo() {
		return VALUE_MUREX_FXFI;
	}


	private String getLogicShortName() {
		return trade.getCounterParty().getCode();
	}


	private String getLogicPortfolio() {
		return trade.getBook().getName();
	}


	private String getLogicTradeDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		return sdf.format(trade.getTradeDate());
	}


	private String getLogicIndividualIndicator() {
		return VALUE_NO;
	}


	private String getLogicRiskSectorCty() {
		String attrValue = cptyAttrs.get(LE_ATTR_RISK_SECTOR);
		if(!Util.isEmpty(attrValue)) {
			return attrValue;
		}
		else {
			return VALUE_EMPTY;	
		}
	}


	private String getLogicUti() {
		String uti = trade.getKeywordValue(TRADE_KWRD_UTI_REFERENCE); 
		if(Util.isEmpty(uti)) {
			uti = trade.getKeywordValue(TRADE_KWRD_UTI_TEMP);
		}
		return uti;
	}


	private String getLogicInternalCode() {
		return VALUE_EMPTY;
	}


	private String getLogicMarkitId() {
		return VALUE_EMPTY;
	}


	private String getLogicNominalCurr() {
        String rst = VALUE_EMPTY;
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            rst = pLeg.getCurrency();
        }
        return rst;
	}


	private String getLogicLiveNominal() {
		String rst = VALUE_EMPTY;
        JDate eventDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());
        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            if ("Bullet".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                Double value = pLeg.getPrincipal();
                if (Double.compare(value, 0.0D) != 0)  {
                    rst = new BigDecimal(Math.abs(value)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                }
            } else if ("Schedule".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                SwapLeg swapLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
                if (swapLeg != null) {
                    if (!swapLeg.getFlows().isEmpty()) {
                        for (CashFlow cf : swapLeg.getFlows()) {
                            if (cf.getDate().gte(eventDate)) {
                                if (cf instanceof CashFlowInterest) {
                                    Double amount = ((CashFlowInterest) cf).getNotional();
                                    rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
//        String mxLastEvent = trade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
//		if(TRADE_EVENT_ISHARES_MODIFICATION.equalsIgnoreCase(mxLastEvent)) {
//			Double amount = swapLeg.getPrincipal();
//			if(amount != null) {
//				rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString(); 
//			}
//		}
		
        return rst;
	}


	private String getLogicOrigNominal() {
		String rst = VALUE_EMPTY;

		Double amount = swapLeg.getPrincipal();
		if(amount != null) {
			rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString(); 
		}
		
//		String mxLastEvent = trade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT);
//		if(TRADE_EVENT_ISHARES_MODIFICATION.equalsIgnoreCase(mxLastEvent)) {
//	        JDate eventDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());
//	        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
//	        if (pLeg != null) {
//	            if ("Bullet".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
//	                Double value = pLeg.getPrincipal();
//	                if (Double.compare(value, 0.0D) != 0)  {
//	                    rst = new BigDecimal(Math.abs(value)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
//	                }
//	            } else if ("Schedule".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
//	                SwapLeg swapLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
//	                if (swapLeg != null) {
//	                    if (!swapLeg.getFlows().isEmpty()) {
//	                        for (CashFlow cf : swapLeg.getFlows()) {
//	                            if (cf.getDate().gte(eventDate)) {
//	                                if (cf instanceof CashFlowInterest) {
//	                                    amount = ((CashFlowInterest) cf).getNotional();
//	                                    rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
//	                                    break;
//	                                }
//	                            }
//	                        }
//	                    }
//	                }
//	            }
//	        }
//		}
		
		return rst;
	}


	private String getLogicConfirmSystem() {
		String tradeKwrd = trade.getKeywordValue(TRADE_KWRD_MATCHING_STATUS);
		if(!Util.isEmpty(tradeKwrd) && VALUE_MATCHED.equalsIgnoreCase(tradeKwrd)) {
			return VALUE_CALYPSO_STC;
		}
		else {
			return VALUE_NO;	
		}
	}


	private String getLogicUnadjustedMatDate() {
		String startDate = "";
		JDatetime date = new JDatetime(swapLeg.getStartDate(), TimeZone.getDefault());
		if (date != null) {
			final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			startDate = dateFormat.format(date);
		}
		return startDate;
	}


	private String getLogicCounterpartyName() {
		return trade.getCounterParty().getName();
	}


	private String getLogicDfaReporting() {
		String tradeKwrd = trade.getKeywordValue(TRADE_KWRD_REPORTING_PARTY);
		if(!Util.isEmpty(tradeKwrd) && VALUE_PO.equalsIgnoreCase(tradeKwrd)) {
			return VALUE_YES;
		}
		else {
			return VALUE_NO;	
		}
	}


	private String getLogicEmirReporting() {
		return VALUE_YES;
	}


	private String getLogicSwapDealerId() {
		String attrValue = cptyAttrs.get(LE_ATTR_SWAP_DEALER);
		if(!Util.isEmpty(attrValue) && VALUE_Y.equalsIgnoreCase(attrValue)) {
			return VALUE_YES;
		}
		else {
			return VALUE_NO;	
		}
	}


	private String getLogicCrdEmirClearingCategory() {
		return VALUE_EMPTY;
	}


	private String getLogicEmirFullDelegation() {
		String attrValue = cptyAttrs.get(LE_ATTR_EMIR_FULL_DELEG);
		if(!Util.isEmpty(attrValue) && VALUE_TRUE.equalsIgnoreCase(attrValue)) {
			return VALUE_YES;
		}
		else {
			return VALUE_NO;	
		}
	}


	private String getLogicTradeStatus() {
		return trade.getStatus().getStatus();
	}


	private String getLogicInstrumentType() {
		return VALUE_BRS;
	}


	private String getLogicResidenceCountry() {
		return trade.getCounterParty().getCountry();
	}


	private String getLogicTrader() {
	    return trade.getTraderName();
	}


	private String getLogicSeller() {
	    return trade.getSalesPerson();
	}


	private String getLogicOriginalCounterparty() {
		String value = VALUE_EMPTY;
		Trade originalTrade = null;
		String strOriginalTradeId = trade.getKeywordValue(TRADE_KWRD_MUREX_TRANSFER_FROM);
		if(!Util.isEmpty(strOriginalTradeId)) {
	        TradeArray tradeSet = getTradeByKeyword(TRADE_KWRD_MUREX_TRADE_ID, strOriginalTradeId);
	        if ((tradeSet != null) && (tradeSet.size() != 0)) {
	        	originalTrade = tradeSet.firstElement();
	        }
			if (originalTrade != null && !trade.getCounterParty().getCode().equalsIgnoreCase(originalTrade.getCounterParty().getCode())) {
				value = originalTrade.getCounterParty().getName();
			}
		}
		return value;
	}


	private String getLogicOrigCptyDfaOwnerType() {
		String value = VALUE_EMPTY;
		Trade originalTrade = null;
		String strOriginalTradeId = trade.getKeywordValue(TRADE_KWRD_MUREX_TRANSFER_FROM);
		if(!Util.isEmpty(strOriginalTradeId)) {
	        TradeArray tradeSet = getTradeByKeyword(TRADE_KWRD_MUREX_TRADE_ID, strOriginalTradeId);
	        if ((tradeSet != null) && (tradeSet.size() != 0)) {
	        	originalTrade = tradeSet.firstElement();
	        }
			if (originalTrade != null) {
				Map<String, String> origCptyAttrs = new HashMap<String, String>();
				fillLegalEntityAttributes(origCptyAttrs, originalTrade.getCounterParty());
				
				String attrUsCptyDomicile = origCptyAttrs.get(LE_ATTR_US_COUNTERPARTY_DOMICILE); 
				String attrBranchUsSwapDealer = origCptyAttrs.get(LE_ATTR_BRANCH_US_SWAP_DEALER);
				String attrAffiliatedUsGuaranteed = origCptyAttrs.get(LE_ATTR_AFFILIATED_US_GUARANTEED);
				String attrConduitUsPerson = origCptyAttrs.get(LE_ATTR_CONDUIT_US_PERSON);		
				if(!Util.isEmpty(attrUsCptyDomicile) && VALUE_Y.equalsIgnoreCase(attrUsCptyDomicile)) {
					value = VALUE_US_COUNTERPARTY_DOMICILE;
				}
				else if (!Util.isEmpty(attrBranchUsSwapDealer) && VALUE_Y.equalsIgnoreCase(attrBranchUsSwapDealer)) {
					value = VALUE_BRANCH_US_SWAP_DEALER;
				}
				else if (!Util.isEmpty(attrAffiliatedUsGuaranteed) && VALUE_Y.equalsIgnoreCase(attrAffiliatedUsGuaranteed)) {
					value = VALUE_AFFILIATED_US_GUARANTEED;
				}
				else if (!Util.isEmpty(attrConduitUsPerson) && VALUE_Y.equalsIgnoreCase(attrConduitUsPerson)) {
					value = VALUE_CONDUIT_US_PERSON;
					}
			}
		}
		return value;
	}


	private String getLogicMtm() {
		if (isEnteredToday()) {
			return VALUE_EMPTY;
		}
        String mtm = VALUE_EMPTY;
        PLMarkValue plMarkValue = getPLMarkValue();
        Double mtmValue = 0.0d;
        if (plMarkValue != null) {
            mtmValue = plMarkValue.getMarkValue();
        }
        mtm = new BigDecimal(mtmValue.doubleValue()).setScale(2 , RoundingMode.HALF_EVEN).toPlainString();
        mtm = mtm.replaceAll(",", ".");
		return EmirFieldBuilderUtil.getInstance().roundAmountByLength(mtm, 20, 4);
	}


	private String getLogicMtmCurrency() {
		if (isEnteredToday()) {
			return VALUE_EMPTY;
		}
        String rst = "EUR";
        if (trade != null) {
            PLMarkValue mtmValue = getPLMarkValue();
            if (mtmValue != null && (!Util.isEmpty(mtmValue.getCurrency()))) {
                rst = mtmValue.getCurrency();
            }
        }
        return rst;
	}


	//  ----- o ----- \\


	private void fillLegalEntityAttributes(final Map<String, String> legalEntityAttributes, final LegalEntity legalEntity) {
		final Collection<?> leAttributes = legalEntity.getLegalEntityAttributes();
		if (!Util.isEmpty(leAttributes)) {
			final Iterator<?> iAttribute = leAttributes.iterator();
			while (iAttribute.hasNext()) {
				final Object attributeObject = iAttribute.next();
				if (attributeObject instanceof LegalEntityAttribute) {
					final LegalEntityAttribute attribute = (LegalEntityAttribute) attributeObject;
					legalEntityAttributes.put(attribute.getAttributeType(), attribute.getAttributeValue());
				}
			}
		}
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


    protected PLMarkValue getPLMarkValue() {
        PricingEnv pricingEnv =  PricingEnv.loadPE("DirtyPrice", valDatetime);
        PLMarkValue result = null;
        final Vector<String> calendar = new Vector<String>();
        calendar.add(CALENDAR_SYSTEM);
        JDate currentDate = valDatetime.getJDate(TimeZone.getDefault()).addBusinessDays(-1, calendar);
        try {
            PLMark plMark  =  CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(), currentDate);
            if (plMark!= null) {
                result  = plMark.getPLMarkValueByName("MIS_NPV");
                 if (result !=  null
                        && result.getMarkValue() != 0.0) {
                    return result;
                }
                result = plMark.getPLMarkValueByName("NPV");
                if (result !=  null
                        && result.getMarkValue() != 0.0) {
                    return result;
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return null;
    }
    
    
    private String getBasketISINS(Basket basket) {
        StringBuilder rst = new StringBuilder();
        if (basket != null && !Util.isEmpty(basket.getBasketComponents())) {
            for (Object obj : basket.getBasketComponents()) {
                if (obj instanceof  Product) {
                    Product component = (Product) obj;
                    if (!Util.isEmpty(component.getSecCode(EmirSnapshotReduxConstants.ISIN))) {
                        rst.append(component.getSecCode(EmirSnapshotReduxConstants.ISIN));
                        rst.append(";");
                    }
                }
            }
        }
        return  rst.toString();
    }


    private boolean isEnteredToday() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    	JDate enteredDate = trade.getEnteredDate().getJDate(TimeZone.getDefault());
    	JDate valueDate = valDatetime.getJDate(TimeZone.getDefault());
    	String sdfEnteredDate = sdf.format(enteredDate.getDate(TimeZone.getDefault()));
    	String sdfValueDate = sdf.format(valueDate.getDate(TimeZone.getDefault()));
    	if(sdfEnteredDate.equalsIgnoreCase(sdfValueDate)) {
    		return true;
    	}
    	return false;
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


}
