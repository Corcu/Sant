/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.mapper;

import java.rmi.RemoteException;
import java.util.List;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.bean.SecurityExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.allocation.validator.SecurityExternalAllocationBeanValidator;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.collateral.impl.AllocationFactory;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 * 
 */
public class SecurityAllocationMapper extends AbstractExternalAllocationMapper {

	/**
	 * @param context
	 */
	public SecurityAllocationMapper(ExternalAllocationImportContext context) {
		super(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.allocation.mapper.AbstractExternalAllocationMapper
	 * #mapActualAllocation(calypsox.tk.collateral.allocation.bean.
	 * ExternalAllocationBean, java.util.List)
	 */
	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.mapper.AbstractExternalAllocationMapper#mapActualAllocation(calypsox.tk.collateral.allocation.bean.ExternalAllocationBean, java.util.List)
	 */
	@Override
	public SecurityAllocation mapActualAllocation(ExternalAllocationBean bean,
			List<AllocImportErrorBean> messages) {

		SecurityExternalAllocationBean allocBean = (SecurityExternalAllocationBean) bean;
		CollateralConfig mcc = allocBean.getCollateralConfig();
		MarginCallEntry mce = allocBean.getEntry();
				
		// get the allocation security
		DSConnection dsCon = DSConnection.getDefault();
		MarginCall mc = new MarginCall();
		Product sec = null;
		try {
			sec = DSConnection.getDefault().getRemoteProduct()
					.getProductByCode("ISIN", allocBean.getAssetISIN());
		}
		catch (RemoteException e) {
			Log.error(this, e);
		}

		mc.setSecurity(sec);

		Trade mcTrade = new Trade();
		mcTrade.setProduct(mc);
		mcTrade.setQuantity(allocBean.getAssetAmount());		
		
		if(allocBean.getAllocationDirection() !=0 ) {
			mcTrade.setQuantity(mcTrade.getQuantity()*allocBean.getAllocationDirection() );
		}
		
		mcTrade.setTraderName(dsCon.getUser());
		mcTrade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
		mcTrade.setTradeCurrency(mcc.getCurrency());
		mcTrade.setSettleCurrency(mcc.getCurrency());

		Book book = BOCache.getBook(DSConnection.getDefault(),
				allocBean.getCollateralBook());
		mcTrade.setBook(book);
		// set trade and product dates
		mcTrade.setSettleDate(JDate.valueOf(allocBean.getSettlementDate()));
		mcTrade.setTradeDate(mce.getProcessDatetime());

		SecurityAllocation allocSec = null;
		try {
			allocSec = getAllocationFactory(mce).createSecurityMargin(mcTrade);

			// compute the allocation values
			allocSec.setSign(allocSec.getSign(), true);

			if (allocSec != null) {
				//MIG_V14
				//allocSec.setDirection(getDirection(mcTrade));

				if (allocBean.getSettlementDate() != null) {
					allocSec.setSettlementDate(JDate.valueOf(allocBean
							.getSettlementDate()));
				}
			}
			// set allcation type
			allocSec.setType("S".equals(allocBean.getCollateralType()) ? "Substitution"
					: "Margin");
		}
		catch (MarketDataException e) {
			messages.add(new AllocImportErrorBean(e.getMessage(), allocBean));
			Log.error(this, e);
		}
		catch (PricerException e) {
			messages.add(new AllocImportErrorBean(e.getMessage(), allocBean));
			Log.error(this, e);
		}
		return allocSec;
	}

	/**
	 * @return
	 */
	private AllocationFactory getAllocationFactory(MarginCallEntry entry) {
		return AllocationFactory.getInstance(null, entry);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see calypsox.tk.collateral.allocation.mapper.ExternalAllocationMapper#
	 * isValidAllocation
	 * (calypsox.tk.collateral.allocation.bean.ExternalAllocationBean,
	 * com.calypso.tk.collateral.MarginCallEntry, java.util.List)
	 */
	@Override
	public boolean isValidAllocation(ExternalAllocationBean allocBean,
			MarginCallEntry entry, List<AllocImportErrorBean> messages)
			throws Exception {
		return new SecurityExternalAllocationBeanValidator(entry, context)
				.validate(allocBean, messages);
	}
}
