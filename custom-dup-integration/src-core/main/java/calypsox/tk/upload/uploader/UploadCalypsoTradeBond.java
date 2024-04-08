package calypsox.tk.upload.uploader;

import calypsox.ctm.CTMBlockTradeEnricher;
import calypsox.repoccp.ReconCCPConstants;
import calypsox.tk.bo.BondForwardFilterAdapter;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.ccp.ClearingTradeFilterAdapter;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.upload.pricingenv.UploaderPricingEnvHolderHandler;
import calypsox.tk.upload.validator.ReprocessKwdValidator;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.util.CreArray;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Vector;

import static calypsox.tk.upload.uploader.util.UploadBondMultiCCyUtil.beforeSaveUtil;

public class UploadCalypsoTradeBond extends com.calypso.tk.upload.uploader.UploadCalypsoTradeBond implements ClearingTradeFilterAdapter, BondForwardFilterAdapter, ReprocessKwdValidator {

    private static final String TRADE_KEYWORD_BONDFORWARD = "BondForward";
    private static final String TRADE_KEYWORD_BONDFORWARDTYPE = "BondForwardType";
    private static final String TRADE_KEYWORD_BFFIXINGVALUE = "BF_FixingValue";
    private static final String TRADE_KEYWORD_BFFIXINGDATE = "BF_FixingDate";
    private static final String TRADE_KEYWORD_MXINITIALCLEANPRICE = "MxInitialCleanPrice";
    private static final String TRADE_KEYWORD_MXROOTCONTRACT = "MurexRootContract";
    private static final String TRADE_KEYWORD_UPI_REFERENCE = "UPI_REFERENCE";
    private static final String UPI_VALUE = "InterestRate:Forward:Debt";

    private static final String FWD_CASH_FIXING = "FWD_CASH_FIXING";
    private static final String NOM_FWD = "NOM_FWD";

    private boolean deliveryToCash = false;
    
    // List of Processing Org used in Mexico
    private static final String DN_MX_POs = "MX_POs"; 


    @Override
    public Product uploadProduct(com.calypso.tk.upload.jaxb.Product product, Vector<BOException> errors, Connection
            dbConnection) {
    	if (!isMexicanTrade(trade)) {
            //As message reprocessing is done by MessageRules run in the DS, no peHolder is initialized by default.
            //So "Uploader" PEHolder config initialization is forced here
            UploaderPricingEnvHolderHandler.initOficcialAccPricingEnvHolder();
    	}
        return super.uploadProduct(product, errors, dbConnection);
    }

    @Override
    public void upload(CalypsoObject object, Vector<BOException> errors, Object dbCon, boolean saveToDB1) {
        super.upload(object,errors,dbCon,saveToDB1);
        setTradeAction(this.trade,this.calypsoTrade);
    }

    @Override
    protected void beforeSave(CalypsoObject calypsoObject, Vector<BOException> errors) {
        beforeSaveUtil(calypsoObject, errors, this.trade, "Bond");
        super.beforeSave(calypsoObject, errors);
    }


    @Override
    public void setTradeFields(CalypsoTrade jaxbCalypsoTrade, Bond bond, com.calypso.tk.upload.jaxb.Bond
    		jaxbBond, Vector<BOException> errors) {
    	super.setTradeFields(jaxbCalypsoTrade, bond, jaxbBond, errors);
    	if (!isMexicanTrade(trade)) {
    		validateDeliveryToCash();
    		addBondKeywords();
            CTMBlockTradeEnricher.enrichCTMBlockTrade(trade);
    		addBondForwardFixing(jaxbCalypsoTrade.getBuySell());
    		removeFwdCashFixing();
    		try {
    			removeCreNomFWD();
    		} catch (CalypsoServiceException e) {
    			Log.error(this, "Could not remove Cre NOM_FWD for trade " + trade.getLongId());
    		}
    		assignMarginCallContract();
    		setExternalReferenceFromKwd(trade);
    	}
    }

    private void assignMarginCallContract() {
        if (isBondForwardTrade(this.trade)) {
            CollateralConfig cc = getCollateralConfig(this.trade);
            setCollateralInfo(trade, cc);
        }
    }

    private void validateDeliveryToCash() {
        String murexBondType = trade.getKeywordValue("Mx PRCOMFX");
        String oldBondType = trade.getKeywordValue(TRADE_KEYWORD_BONDFORWARDTYPE);
        if (!"Bond Fwd Delivery".equals(murexBondType) && "Delivery".equals(oldBondType)) {
            deliveryToCash = true;
        }
    }

    private void addBondKeywords() {
        String murexBondType = trade.getKeywordValue("Mx PRCOMFX");
        String mercadoPrimario = trade.getBook().getAttribute("mercadoPrimario");
        if (Util.isEmpty(mercadoPrimario) || "false".equalsIgnoreCase(mercadoPrimario)) {
            if ("Bond Fwd Cash".equals(murexBondType)) {
                trade.addKeyword(TRADE_KEYWORD_BONDFORWARD, "true");
                trade.addKeyword(TRADE_KEYWORD_BONDFORWARDTYPE, "Cash");
                trade.addKeyword(TRADE_KEYWORD_UPI_REFERENCE, UPI_VALUE);
            } else if ("Bond Fwd Delivery".equals(murexBondType)) {
                trade.addKeyword(TRADE_KEYWORD_BONDFORWARD, "true");
                trade.addKeyword(TRADE_KEYWORD_BONDFORWARDTYPE, "Delivery");
                trade.addKeyword(TRADE_KEYWORD_UPI_REFERENCE, UPI_VALUE);
            }
            //TODO si pasa de FORWARD a SPOT quitar keywords TRADE_KEYWORD_BONDFORWARD y TRADE_KEYWORD_BONDFORWARDTYPE
        }
    }

    private void addBondForwardFixing(String direction) {
        String fixingValue = trade.getKeywordValue(TRADE_KEYWORD_BFFIXINGVALUE);
        boolean exist = false;
        if (!Util.isEmpty(fixingValue)) {
            Vector<Fee> feeList = trade.getFeesList();
            if (feeList != null && feeList.size() > 0) {
                for (Fee fee : trade.getFeesList()) {
                    if (fee != null && FWD_CASH_FIXING.equalsIgnoreCase(fee.getType())) {
                        exist = true;
                        buildFee(fee, fixingValue, direction);
                    }
                }
            }
            if (!exist) {
                Fee fee = new Fee();
                fee = buildFee(fee, fixingValue, direction);

                trade.addFee(fee);
            }
        }
    }

    private void removeFwdCashFixing() {
        String bondForwardType = trade.getKeywordValue(TRADE_KEYWORD_BONDFORWARDTYPE);
        if (!Util.isEmpty(bondForwardType) && !"Cash".equalsIgnoreCase(bondForwardType)) {
            Vector<Fee> feeList = trade.getFeesList();
            Vector<Fee> feeListOut = new Vector<>();
            if (feeList != null && feeList.size() > 0) {
                for (Fee fee : trade.getFeesList()) {
                    if (fee != null && !FWD_CASH_FIXING.equalsIgnoreCase(fee.getType())) {
                        feeListOut.add(fee);
                    }
                }
                trade.setFees(feeListOut);
            }
        }
    }

    private void removeCreNomFWD() throws CalypsoServiceException {
        if (deliveryToCash && JDate.getNow().after(BOCreUtils.calculateforwardDate(trade, trade.getProduct()))) {
            CreArray creArray = DSConnection.getDefault().getRemoteBO().getBOCres(trade.getLongId());
            CreArray cresOut = new CreArray();
            if (creArray != null && creArray.size() > 0) {
                for (BOCre boCre : creArray.getCres()) {
                    if (boCre != null && NOM_FWD.equalsIgnoreCase(boCre.getEventType())) {
                        try {
                            BOCre creReversal = (BOCre) boCre.clone();
                            creReversal.setId(0L);
                            creReversal.setEffectiveDate(JDate.getNow());
                            creReversal.setLinkedId(boCre.getId());
                            creReversal.setCreationDate(new JDatetime());
                            creReversal.setCreType("REVERSAL");
                            creReversal.setSentDate(null);
                            creReversal.setStatus(BOCre.NEW);
                            creReversal.setSentStatus(null);
                            creReversal.setSentStatus("");
                            cresOut.add(creReversal);
                        } catch (CloneNotSupportedException e) {
                            Log.error(this, "Could not save the fee for trade " + trade.getLongId());
                        }
                        //cres contains only a CRE
                        if (!cresOut.isEmpty()) {
                            publishEvents(cresOut.get(0));
                        }
                    }
                }
            }
        }
    }

    private JDate formatJDate(String date) {
        JDate out = new JDate();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            out = JDate.valueOf(format.parse(date));
        } catch (ParseException e) {
            Log.error(this, "Could not save the fee for trade " + trade.getLongId());
        }
        return out;
    }

    private Fee buildFee(Fee fee, String fixingValue, String direction) {
        Bond bond = (Bond) trade.getProduct();
        String fixingDate = trade.getKeywordValue(TRADE_KEYWORD_BFFIXINGDATE);
        String cleanPrice = trade.getKeywordValue(TRADE_KEYWORD_MXINITIALCLEANPRICE);

        fee.setType(FWD_CASH_FIXING);
        fee.setDate(formatJDate(fixingDate));
        fee.setFeeDate(formatJDate(fixingDate));
        fee.setStartDate(formatJDate(fixingDate).addBusinessDays(-1, bond.getHolidays()));
        fee.setEndDate(trade.getSettleDate());
        fee.setKnownDate(formatJDate(fixingDate));

        fee.setLegalEntityId(trade.getCounterParty().getLegalEntityId());
        fee.setCurrency(trade.getSettleCurrency());

        //Calculate Nominal
        double quantity = Optional.ofNullable(this.trade).map(Trade::getQuantity).orElse(0.0);
        double faceValue = Optional.ofNullable(bond).map(Bond::getFaceValue).orElse(0.0);
        double nominal = Math.abs(quantity * faceValue);
        double amount = 0.0;

        amount = direction != null ? "BUY".equalsIgnoreCase(direction) ?
                nominal * ((Double.parseDouble(fixingValue) - Double.parseDouble(cleanPrice)) / 100) :
                nominal * ((Double.parseDouble(cleanPrice) - Double.parseDouble(fixingValue)) / 100) : 0;

        fee.setAmount((double) Math.round(amount * 100) / 100);

        return fee;
    }


    private CollateralConfig getCollateralConfig(Trade trade) {
        CollateralConfig marginCallConfig = null;
        int mccId = 0;
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (mccId == 0) {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            try {
                marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
            } catch (RemoteException e) {
                Log.error(this, "Could not find MarginCall Config : " + e);
            }
        } else {
            marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        }
        return marginCallConfig;
    }

    /**
     * @param trade
     * @param cc
     */
    private void setCollateralInfo(Trade trade, CollateralConfig cc) {
        if (cc != null) {
            trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, cc.getId());
            trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE, cc.getContractType());
            trade.addKeyword("FO_SYSTEM", "Mx3");
            trade.addKeyword("BO_SYSTEM", "CALYPSO_STC");
            trade.setInternalReference(String.valueOf(cc.getId()));
        }
    }

    /**
     * Method's entry point is not correct, it works... but luckily.
     * This externalReference modification MUST be done before calypsox.tk.upload.validator.ValidateCalypsoTradeBondCTM call
     * @param trade
     */
    protected void setExternalReferenceFromKwd(Trade trade) {
        if (trade != null) {
            String mxRootContract = Optional.ofNullable(trade.getKeywordValue(TRADE_KEYWORD_MXROOTCONTRACT))
                    .orElse("");
            trade.setExternalReference(mxRootContract);
        }
    }

    /**
     * Clearing Trade's amendment uses a different action for workflow filtering purposes.
     * If any financial change is done, the trade cannot be auto amended. AMEND_RECON action has an AuditFilter
     * set to check this.
     * @param trade
     * @param calypsoTrade
     */
    protected void setTradeAction(Trade trade, CalypsoTrade calypsoTrade){
        if(isClearedTrade(trade)){
            setAmendTradeAction(trade,calypsoTrade);
        }
    }

    protected void setAmendTradeAction(Trade trade, CalypsoTrade calypsoTrade){
        if(Action.AMEND.equals(trade.getAction())&&
                !isReprocess(calypsoTrade)){
            trade.setAction(Action.valueOf(ReconCCPConstants.WF_AMEND_RECON));
        }
    }

    /**
     * Publish event for CreOnlineSenderEngine
     *
     * @param cre
     */
    private void publishEvents(BOCre cre) {
        PSEventCre creEvent = new PSEventCre();
        creEvent.setBoCre(cre);
        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(creEvent);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error publish the event: " + e);
        }
    }
    
	/**
	 * Returns true or false if the trade has a mexican PO
	 * @param trade
	 * @return result
	 */
	protected boolean isMexicanTrade(final Trade trade) {
		boolean result = false;
		
		String processingOrgName  = getProcessingOrgName(trade);
		boolean validMxPO = isAValidMexicanProcessingOrg(processingOrgName);
		
		if (validMxPO) {
	    	result = true;
		}
		
	    return result;
	}
	
	/**
	 * return the name of processing org
	 * @param trade
	 * @return
	 */
	private String getProcessingOrgName(final Trade trade) {
		String processingOrgName = "";
		
		if(trade.getBook() != null) {
			Book book = trade.getBook();
			int idProcessingOrg = book.getProcessingOrgBasedId();
			LegalEntity legalEntity = getRemoteLegalEntity(idProcessingOrg);
			
			if(legalEntity != null) {
				//shortname of the legal entity
				processingOrgName = legalEntity.getCode();
			}
		}
				
		return processingOrgName;
	}
	
	/**
	 * retrieve a valid processing org from mexico
	 * @param processingOrgParam
	 * @return
	 */
	private boolean isAValidMexicanProcessingOrg(final String processingOrgParam) {
        boolean result = false;
        
		if (!Util.isEmpty(processingOrgParam)) {
		    Vector<String> vectorMexicanProcessingOrg = getDomainValuesOfDomainName(DN_MX_POs);
		    
		    if ( vectorMexicanProcessingOrg.contains(processingOrgParam)) {
		    	result = true;
		    }
		}
		
		return result;
	}
	
	/**
	 * Retrieve the legal entity
	 */
	protected LegalEntity getRemoteLegalEntity(final int idProcessingOrg) {
		LegalEntity legalEntity   = null;
		
		try {
			legalEntity = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(idProcessingOrg);	
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this,
					"Error retrieving the legal entity with the PO: " + idProcessingOrg);

		}
		
		return legalEntity;
	}

	/**
	 * Retrieve the Domain Values of a Domain Name
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesOfDomainName(final String domainName) {
		Vector<String> result = new Vector<String>();

		Vector<String> auxiliar = this.getDomainValuesWithCache(domainName);
		if (Util.isEmpty(auxiliar)) {
			auxiliar = this.getDomainValuesWithoutCache(domainName);
		}
		if (!Util.isEmpty(auxiliar)) {
			result = auxiliar;
		}

		return result;
	}
	
	
	/**
	 * Retrieve the Domain Values of a Domain Name using cache
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesWithCache(final String domainName) {
		return LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
	}

	/**
	 * Retrieve the Domain Values of a Domain Name without using cache
	 * 
	 * @param domainName
	 * @return
	 */
	private Vector<String> getDomainValuesWithoutCache(final String domainName) {
		Vector<String> domainValues = null;

		try {
			domainValues = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(domainName);
		} catch (CalypsoServiceException calypsoServiceException) {
			Log.error(this,
					"Error retrieving the Domain Values of " + domainName + " : " + calypsoServiceException.toString());
		}

		return domainValues;
	}

}
