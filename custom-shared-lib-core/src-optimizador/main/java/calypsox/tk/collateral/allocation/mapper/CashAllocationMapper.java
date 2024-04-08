/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.mapper;

import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.validator.CashExternalAllocationBeanValidator;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.impl.AllocationFactory;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 * 
 */
public class CashAllocationMapper extends AbstractExternalAllocationMapper {

	
	/**
	 * @param context
	 */
	public CashAllocationMapper(ExternalAllocationImportContext context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.mapper.AbstractExternalAllocationMapper#mapActualAllocation(calypsox.tk.collateral.allocation.bean.ExternalAllocationBean, java.util.List)
	 */
	@Override
	public CashAllocation mapActualAllocation(
			ExternalAllocationBean allocBean,
			List<AllocImportErrorBean> messages) {

		CollateralConfig mcc = allocBean.getCollateralConfig();
		MarginCallEntry mce = allocBean.getEntry();
		
		DSConnection dsCon = DSConnection.getDefault();
		MarginCall mc = new MarginCall();
		mc.setCurrencyCash(allocBean.getAssetCurrency());

		Trade mcTrade = new Trade();
		mcTrade.setProduct(mc);
		mcTrade.setQuantity(allocBean.getAssetAmount());
		
		if(allocBean.getAllocationDirection() !=0 ) {
			mcTrade.setQuantity(mcTrade.getQuantity()*allocBean.getAllocationDirection() );
		}

		mc.setPrincipal(allocBean.getAssetAmount());
		mcTrade.setTraderName(dsCon.getUser());
		mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
		mcTrade.setTradeCurrency(allocBean.getAssetCurrency());
		mcTrade.setSettleCurrency(allocBean.getAssetCurrency());
		
		Book book = BOCache.getBook(DSConnection.getDefault(), allocBean.getCollateralBook());
		mcTrade.setBook(book);
		// set trade and product dates
		mcTrade.setSettleDate(JDate.valueOf(allocBean.getSettlementDate()));
		mcTrade.setTradeDate(mce.getProcessDatetime());

		CashAllocation cashAlloc = null;
		try {
			cashAlloc = getAllocationFactory(mce).createCashMargin(mcTrade);
			cashAlloc.setSign(cashAlloc.getSign(), true);
			
			if (cashAlloc != null) {
				//MIG_V14
				//cashAlloc.setDirection(getDirection(mcTrade));
				if (allocBean.getSettlementDate() != null) {
					cashAlloc.setSettlementDate(JDate.valueOf(allocBean
							.getSettlementDate()));
				}
			}
			cashAlloc
			.setType("S".equals(allocBean.getCollateralType()) ? "Substitution"
					: "Margin");
		}
		catch (MarketDataException e) {
			messages.add(new AllocImportErrorBean(e.getMessage(),allocBean));
			Log.error(this, e);
		}
		catch (PricerException e) {
			messages.add(new AllocImportErrorBean(e.getMessage(),allocBean));
			Log.error(this, e);
		}
		return cashAlloc;
	}
	
	
	/**
	 * @return
	 */
	private AllocationFactory getAllocationFactory(MarginCallEntry entry) {
		return AllocationFactory.getInstance(null, entry);
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.mapper.ExternalAllocationMapper#isValidAllocation(calypsox.tk.collateral.allocation.bean.ExternalAllocationBean, com.calypso.tk.collateral.MarginCallEntry, java.util.List)
	 */
	@Override
	public boolean isValidAllocation(ExternalAllocationBean allocBean, MarginCallEntry entry,
			List<AllocImportErrorBean> messages) throws Exception {
		return new CashExternalAllocationBeanValidator(entry,context).validate(allocBean, messages);
	}
}
