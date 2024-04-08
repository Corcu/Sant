/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.mapper;

import java.rmi.RemoteException;
import java.util.List;

import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportConstants;
import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportContext;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimCashAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimSecurityAllocationBean;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.collateral.impl.AllocationFactory;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * @author aela
 * 
 */
public class OptimizerAllocationMapper extends AbstractExternalAllocationMapper {
	protected MarginCallEntry entry = null;

	protected OptimAllocsImportContext context = null;

	protected PricingEnv pe = null;

	public OptimizerAllocationMapper(OptimAllocsImportContext context) {
		this.context = context;
		pe = context.getPricingEnv();
	}

	@Override
	protected CashAllocation mapCashAllocation(OptimCashAllocationBean bean,
			List<AllocImportErrorBean> messages) {

		OptimCashAllocationBean allocBean = (OptimCashAllocationBean) bean;

		CollateralConfig mcc = allocBean.getMcc();
		DSConnection dsCon = DSConnection.getDefault();
		MarginCall mc = new MarginCall();
		mc.setCurrencyCash(allocBean.getAssetCurrency());

		Trade mcTrade = new Trade();
		mcTrade.setProduct(mc);
		mcTrade.setQuantity(allocBean.getNominal());
		mc.setPrincipal(allocBean.getNominal());
		mcTrade.setTraderName(dsCon.getUser());
		mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
		mcTrade.setTradeCurrency(allocBean.getAssetCurrency());
		mcTrade.setSettleCurrency(allocBean.getAssetCurrency());
		
		//mcTrade.setBook(mcc.getBook());
		Book book = BOCache.getBook(DSConnection.getDefault(), allocBean.getCollateralBook());
		mcTrade.setBook(book);
		// set trade and product dates
		mcTrade.setSettleDate(JDate.valueOf(allocBean.getSettlementDate()));
		mcTrade.setTradeDate(this.entry.getProcessDatetime());

		CashAllocation cashAlloc = null;
		try {
			cashAlloc = getAllocationFactory().createCashMargin(mcTrade);
			if (cashAlloc != null) {
				//MIG_V14
				//cashAlloc.setDirection(getDirection(mcTrade));
				if (allocBean.getSettlementDate() != null) {
					cashAlloc.setSettlementDate(JDate.valueOf(allocBean
							.getSettlementDate()));
				}
			}
		}
		catch (MarketDataException e) {
			messages.add(new AllocImportErrorBean(e.getMessage()));
			Log.error(this, e);
		}
		catch (PricerException e) {
			messages.add(new AllocImportErrorBean(e.getMessage()));
			Log.error(this, e);
		}
		cashAlloc
				.setType("S".equals(allocBean.getCollateralType()) ? "Substitution"
						: "Margin");
		addOptimizationAttributes(cashAlloc, allocBean);
		cashAlloc.setSign(cashAlloc.getSign(), true);
		
		if (cashAlloc.getContractValue() != allocBean.getContractValue()) {
			cashAlloc.addAttribute("contractValueDiscrepancies", "true");
		}
		return cashAlloc;
	}

	void addOptimizationAttributes(MarginCallAllocation alloc,OptimAllocationBean allocBean) {
		alloc.addAttribute("isOptimizerAllocation", "true");
		alloc.addAttribute("origin", "Optimizer");
		alloc.addAttribute("collateralCost",allocBean.getCollateralCost());
		alloc.addAttribute("ASSET_RANKING",allocBean.getAssetRanking());
		alloc.addAttribute("treatmentID",context.getExecutionId());
	}

	@Override
	protected SecurityAllocation mapSecurityAllocation(
			OptimSecurityAllocationBean bean,
			List<AllocImportErrorBean> messages) {

		OptimSecurityAllocationBean allocBean = (OptimSecurityAllocationBean) bean;

		// get the allocation security
		CollateralConfig mcc = allocBean.getMcc();
		DSConnection dsCon = DSConnection.getDefault();
		MarginCall mc = new MarginCall();
		mc.setSecurity(allocBean.getSecurity());

		Trade mcTrade = new Trade();
		mcTrade.setProduct(mc);
		mcTrade.setQuantity(allocBean.getNominal()
				/ allocBean.getSecurity().getPrincipal(entry.getProcessDate()));
		mcTrade.setTraderName(dsCon.getUser());
		mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
		mcTrade.setTradeCurrency(mcc.getCurrency());
		mcTrade.setSettleCurrency(mcc.getCurrency());
		//mcTrade.setBook(mcc.getBook());
		Book book = BOCache.getBook(DSConnection.getDefault(), allocBean.getCollateralBook());
		mcTrade.setBook(book);
		// mcTrade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
		// mcc.getId());
		// mcTrade.addKeyword(TradeInterfaceUtils.TRANS_TRADE_KWD_MTM_DATE,
		// Util.dateToString(this.entry.getValueDate()));
		// set trade and product dates
		mcTrade.setSettleDate(JDate.valueOf(allocBean.getSettlementDate()));
		mcTrade.setTradeDate(this.entry.getProcessDatetime());

		SecurityAllocation allocSec = null;
		try {
			allocSec = getAllocationFactory().createSecurityMargin(mcTrade);
			if (allocSec != null) {
				//MIG_V14
				//allocSec.setDirection(getDirection(mcTrade));
				// this will be retrieved by calypso
				// if (allocBean.getAssetHaircut() != null
				// && allocBean.getAssetHaircut() != 0.0) {
				// allocSec.setHaircut(allocBean.getAssetHaircut());
				// }
				//if (allocBean.getAssetPrice() != null
				//		&& allocBean.getAssetPrice() != 0.0) {
				//	allocSec.setCleanPrice(allocBean.getAssetPrice());
				//}

				if (allocBean.getSettlementDate() != null) {
					allocSec.setSettlementDate(JDate.valueOf(allocBean
							.getSettlementDate()));
				}
			}
		}
		catch (MarketDataException e) {
			messages.add(new AllocImportErrorBean(e.getMessage()));
			Log.error(this, e);
		}
		catch (PricerException e) {
			messages.add(new AllocImportErrorBean(e.getMessage()));
			Log.error(this, e);
		}
		// set allcation type
		allocSec.setType("S".equals(allocBean.getCollateralType()) ? "Substitution"
				: "Margin");
		// compute the allocation values
		allocSec.setSign(allocSec.getSign(), true);

		// add allocation attributes
		allocSec.addAttribute("optimizerHaircut", allocSec.getHaircut());
		allocSec.addAttribute("optimizerContratValue",
				allocBean.getContractValue());

		if (allocSec.getHaircut() != allocBean.getAssetHaircut()) {
			allocSec.addAttribute("haircutDiscrepancies", "true");
		}
		
		if (allocSec.getCleanPrice() != allocBean.getAssetPrice()) {
			allocSec.addAttribute("priceDiscrepancies", "true");
		}
		
		if (allocSec.getContractValue() != allocBean.getContractValue()) {
			allocSec.addAttribute("contractValueDiscrepancies", "true");
		}
		
		addOptimizationAttributes(allocSec,allocBean);
		return allocSec;
	}

	private AllocationFactory getAllocationFactory() {
		return AllocationFactory.getInstance(null, entry);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.optimizer.importer.mapper.AbstractExternalAllocationMapper#isValidCashAllocation(calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimCashAllocationBean, java.util.List)
	 */
	@Override
	protected boolean isValidCashAllocation(OptimCashAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		boolean isValid = true;
		// check mandatory fields
		if(Util.isEmpty(allocBean.getCollateralType())) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_EMPTY_MANDATORY_FIELD,
					"The mandatory field Collateral Type is not set"));
			isValid = false;
		}
		
		if(allocBean.getNbCtrAllocs()<=0) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_EMPTY_MANDATORY_FIELD,
					"The mandatory field Number of Allocations is not valid"));
			isValid = false;
		}
		// check the CollateralConfig
		isValid = checkMarginCallConfig(allocBean, messages);

		// check the PO
		isValid = isValid && checkProcessingOrg(allocBean, messages);
		
		// check the book
		isValid = isValid && checkBook(allocBean, messages);
				
		// check the pricing Env
		isValid = isValid && checkPricingEnv(allocBean, messages);

		// check currency
		isValid = isValid && checkCurrency(allocBean, messages);

		// check currency
		isValid = isValid && checkEligibleCurrency(allocBean, messages);

		allocBean.getErrorsList().addAll(messages);

		return isValid;
	}

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private boolean checkEligibleCurrency(OptimCashAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		boolean isEligibleCurrency = false;
		List<CollateralConfigCurrency> currencies = allocBean.getMcc()
				.getEligibleCurrencies();
		if (!Util.isEmpty(currencies)) {
			for (CollateralConfigCurrency ccy : currencies) {
				if (ccy.getCurrency().equals(allocBean.getAssetCurrency())) {
					isEligibleCurrency = true;
					break;
				}
			}
		}
		if (!isEligibleCurrency) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_CCY_NOT_ELIGIBLE,
					"The currency " + allocBean.getAssetCurrency()
							+ " is not eligible to be used with the contract "
							+ allocBean.getMcc().getName()));
		}
		return isEligibleCurrency;
	}

	private boolean checkCurrency(OptimCashAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		CurrencyDefault cd = LocalCache.getCurrencyDefault(allocBean
				.getAssetCurrency());
		if (cd == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_CCY_NOT_FOUND, "row "
							+ allocBean.getRowNumber() + " : Currency "
							+ allocBean.getAssetCurrency()
							+ " is not valid or not setup in the system"));
			return false;
		}
		return true;
	}

	private boolean checkAssetPrice(OptimSecurityAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		if (allocBean.getAssetPrice() == null
				|| allocBean.getAssetPrice().doubleValue() <= 0.0) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_PRICE_NOT_VAIDE,
					"Invalid security price : "
							+ (allocBean.getAssetPrice() == null ? "null"
									: allocBean.getAssetPrice().doubleValue())));
			return false;
		}
		return true;
	}

	@Override
	protected boolean isValidSecurityAllocation(
			OptimSecurityAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		boolean isValid = true;
		// check mandatory fields
		if(Util.isEmpty(allocBean.getCollateralType())) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_EMPTY_MANDATORY_FIELD,
					"The mandatory field Collateral Type is not set"));
			isValid = false;
		}
		
		if(allocBean.getNbCtrAllocs()<=0) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_EMPTY_MANDATORY_FIELD,
					"The mandatory field Number of Allocations is not valid"));
			isValid = false;
		}
		// check the CollateralConfig
		 isValid = checkMarginCallConfig(allocBean, messages);

		// check the PO
		isValid = isValid && checkProcessingOrg(allocBean, messages);
		
		// check the book
		isValid = isValid && checkBook(allocBean, messages);

		// check the pricing Env
		isValid = isValid && checkPricingEnv(allocBean, messages);

		// check the ISIN
		isValid = isValid && checkISIN(allocBean, messages);

		// check that security is accepted by the contract
		isValid = isValid && checkEligibleSecurity(allocBean, messages);

		// check the security price
		isValid = isValid && checkAssetPrice(allocBean, messages);

		allocBean.getErrorsList().addAll(messages);

		return isValid;
	}

	private boolean checkEligibleSecurity(
			OptimSecurityAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		List<String> sdfs = allocBean.getMcc().getEligibilityFilterNames();
		boolean isEligibleSecurity = false;
		if (!Util.isEmpty(sdfs)) {
			for (String sdf : sdfs) {
				StaticDataFilter realSDF = BOCache.getStaticDataFilter(
						DSConnection.getDefault(), sdf);
				if ((realSDF != null)
						&& realSDF.accept(null, allocBean.getSecurity())) {
					isEligibleSecurity = true;
					break;
				}
			}
		}

		if (!isEligibleSecurity) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_NOT_ELIGIBLE,
					"The security with ISIN code "
							+ (Util.isEmpty(allocBean.getAssetISIN()) ? ""
									: allocBean.getAssetISIN())
							+ " is not eligible to be used with the contract "
							+ allocBean.getMcc().getName()));

		}
		return isEligibleSecurity;
	}

	private boolean checkISIN(OptimSecurityAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		Product sec = null;
		try {
			sec = DSConnection.getDefault().getRemoteProduct()
					.getProductByCode("ISIN", allocBean.getAssetISIN());
		}
		catch (RemoteException e) {
			Log.error(this, e);
		}

		if (sec == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_ISIN_NOT_FOUND,
					"Unable to find a security with the ISIN code "
							+ (Util.isEmpty(allocBean.getAssetISIN()) ? ""
									: allocBean.getAssetISIN())));
			return false;
		}
		allocBean.setSecurity(sec);
		return true;
	}

	private boolean checkPricingEnv(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		if (pe == null) {
			messages.add(new AllocImportErrorBean(
					"No pricing environment found for the contract  "
							+ allocBean.getCtrShortName()));
			return false;
		}
		return true;
	}

	private boolean checkProcessingOrg(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(),
				allocBean.getPoShortName());
		if (po == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_PROCESSING_ORG_NOT_VALID,
					"Invalid ProcessingOrganisation "
							+ (Util.isEmpty(allocBean.getPoShortName()) ? ""
									: allocBean.getPoShortName())));
			return false;
		}
		return true;
	}
	
	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	private boolean checkBook(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		
		Book book = BOCache.getBook(DSConnection.getDefault(), allocBean.getCollateralBook());
		
		if (book == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_BOOK_NOT_VALID,
					"Invalid Book "
							+ (Util.isEmpty(allocBean.getCollateralBook()) ? ""
									: allocBean.getCollateralBook())));
			return false;
		}
		
		/*
		if(allocBean.getMcc() != null) {
			allocBean.getMcc().is
			//allocBean.getMcc().getEligibleBooks(arg0)
		}
		*/
		return true;
	}

	private boolean checkMarginCallConfig(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {
		String contractName = allocBean.getContractName();
		if (Util.isEmpty(contractName)) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_MARGIN_CALL_NOT_FOUND,
					"No margin call contract specified "));
			return false;
		}

		Double contractId = context.getContractsNameForId().get(contractName);
		int mccId = (contractId == null ? 0 : contractId.intValue());
		CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(
				DSConnection.getDefault(), mccId);

		allocBean.setMcc(mcc);

		if (mcc == null) {
			messages.add(new AllocImportErrorBean(
					OptimAllocsImportConstants.ERR_MARGIN_CALL_NOT_FOUND,
					"No margin call contract found with the name "
							+ contractName));
			return false;
		}

		return true;
	}

	/**
	 * @param allocBean
	 * @param errors
	 * @return
	 */
	public MarginCallEntry mapEntry(OptimContractAllocsBean allocBean,
			List<String> errors) {
		int mccId = 0;
		this.entry = null;
		if (allocBean.getMcc() == null) {
			Double contractId = context.getContractsNameForId().get(
					allocBean.getContractName());
			mccId = (contractId == null ? 0 : contractId.intValue());
			CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(
					DSConnection.getDefault(), mccId);

			allocBean.setMcc(mcc);
		}
		else {
			mccId = allocBean.getMcc().getId();
		}

		try {
			Log.error(this,Thread.currentThread().getName()+" mccId = "+mccId +" mccName = "+(allocBean.getContractName()==null?"null":allocBean.getContractName()));
			this.entry = CollateralManagerUtil.loadEntry(mccId,
					context.getExecutionContext(), errors);
		}
		catch (RemoteException e) {
			errors.add("Unable to load the margin call entry for the contract "
					+ allocBean);
			Log.error(this, e);
		}
		return this.entry;
	}
}
