/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.util.interfaceImporter;

import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_CLOSING_PRICE;
import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_CLOSING_PRICE_AT_START;
import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_CUPON_CORRIDO;
import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_HAIRCUT;
import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_HAIRCUT_DIRECTION;
import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_REPO_RATE;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


import calypsox.tk.util.bean.InterfaceTradeBean;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;

/**
 * Disruptive code to: add the specific CollateralExposure.SECURITY_LENDING
 * keywords and to do the new MtM calculation.
 * 
 * @version 2.1
 * @author Guillermo Solano & Gerardo Saiz
 * @since 07/15
 * 
 */
public class InterfaceSecLendingTradeMapper {

	/**
	 * Constant: negative the Mtm
	 */
	private static final String BORROW = "Borrower";

	/**
	 * CollateralExposure.SECURITY_LENDING trade
	 */
	private final Trade trade;

	/**
	 * Mapped data for the trade
	 */
	private final InterfaceTradeBean tradeBean;

	/**
	 * Product info
	 */
	private final CollateralExposure product;

	/**
	 * SimpleDateFormat
	 */
	protected final SimpleDateFormat dateFormat = new SimpleDateFormat(
	        "dd/MM/yyyy");

	/**
	 * TradeImportTracker
	 */
	private final TradeImportTracker tradeImportTracker;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            of the CollateralExposure importer
	 */
	public InterfaceSecLendingTradeMapper(final Trade trade,
	        final InterfaceTradeBean tradeBean,
	        TradeImportTracker tradeImportTracker) {

		this.trade = trade;
		this.tradeBean = tradeBean;
		this.product = (CollateralExposure) trade.getProduct();
		this.tradeImportTracker = tradeImportTracker;
	}

	/**
	 * Transformations required to the Collateral Exposure Product Security
	 * lending: 1? replace the closing price, 2? calculate the new mtm & 3? add
	 * missing keywords
	 */
	public void buildSecLending() {
		Boolean ret = true;
		// replace closing price
		ret = replaceClosingPrice();

		// calculate specific MtM
		calculateSecLendingMtM(ret);

		// add keywords
		buildSecLendingKeywords();

	}

	/**
	 * 
	 */
	private Boolean replaceClosingPrice() {

		// Closing Price = Dirty Price from QuoteValue (PdV)
		Map<String, Double> map = null;
		try {

			map = getQuoteValuesFromISIN(this.tradeBean
			        .getUnderlaying(), JDate.valueOf(this.dateFormat
			                .parse(this.tradeBean.getMtmDate())));
		} catch (Exception e) {
			TradeImportStatus error = new TradeImportStatus(this.tradeBean, 55,
			        "Error while getting Clean and Dirty Quote for Isin "
			                + this.product.getSecCode("ISIN") + " on "
			                + this.tradeBean.getValueDate(),
			        TradeImportStatus.ERROR);
			this.tradeImportTracker.addError(error);
			Log.error(this, e);//sonar
		}

		if (map == null) {
			TradeImportStatus error = new TradeImportStatus(this.tradeBean, 55,
			        "Error while getting Clean and Dirty Quote for Isin "
			                + this.product.getSecCode("ISIN") + " on "
			                + this.tradeBean.getValueDate(),
			        TradeImportStatus.ERROR);
			this.tradeImportTracker.addError(error);
			return false;
		}

		// save as data & keyword
		Double newClosingPrice = map.get("DirtyPrice");

		if (newClosingPrice != null) {
			newClosingPrice = newClosingPrice * 100;

			this.tradeBean.setClosingPriceDaily(newClosingPrice.toString());
			final Amount closingAmount = new Amount(newClosingPrice);
			this.product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE,
			        closingAmount);
			final Amount closingAmountFormatted = new Amount(newClosingPrice, 2);
			this.trade.addKeyword(TRD_IMP_FIELD_CLOSING_PRICE,
			        closingAmountFormatted.toString());

		} else {
			this.tradeBean.setClosingPriceDaily(null);
			this.trade.addKeyword(TRD_IMP_FIELD_CLOSING_PRICE,
			        "");
			this.product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE,
			        "");
			TradeImportStatus error = new TradeImportStatus(this.tradeBean, 56,
			        "Not possible to calculate MTM. There is no Dirty Price for Isin "
			                + this.tradeBean.getUnderlaying() + " on "
			                + this.tradeBean.getMtmDate(),
			        TradeImportStatus.ERROR);
			this.tradeImportTracker.addError(error);
			return false;
		}

		Double cleanPrice = map.get("CleanPrice");
		if (cleanPrice != null) {
			cleanPrice = cleanPrice * 100;

			final Amount cuponCorrido = new Amount(
			        newClosingPrice - cleanPrice, 2);
			this.trade.addKeyword(TRD_IMP_FIELD_CUPON_CORRIDO,
			        cuponCorrido.toString());
		} else {
			TradeImportStatus error = new TradeImportStatus(this.tradeBean, 57,
			        "CUPON_CORRIDO not calculated. There is no Clean Price for Isin "
			                + this.tradeBean.getUnderlaying() + " on "
			                + this.tradeBean.getMtmDate(),
			        TradeImportStatus.WARNING);
			this.tradeImportTracker.addError(error);
		}

		return true;

	}

	/**
	 * @param isin
	 *            the product isin for which to get the quouteName,
	 * @param quoteDate
	 *            date at which the product should be alive (not matured)
	 * @return the quote name for this product
	 * @throws RemoteException
	 *             if a problem occurs when getting quotes names from the DataServer
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Double> getQuoteValuesFromISIN(final String isin, final JDate quoteDate)
	        throws RemoteException {

		String quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, quoteDate);

		String sql = "quote_name = '" + quoteName + "' and quote_date=" + Util.date2SQLString(quoteDate);

		Vector<QuoteValue> quoteValues = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(sql);

		Map<String, Double> map = new HashMap<String, Double>();

		for (QuoteValue value : quoteValues) {
			map.put(value.getQuoteSetName(), value.getClose());
		}

		return map;
	}

	/**
	 * Calcuates de Mtm with and without haircut for the
	 * CollateralExposure.SECURITY_LENDING product. Adaption done only for this
	 * product and in importation from the BO Murex.
	 * 
	 */
	private void calculateSecLendingMtM(Boolean withMTM) {

		// original value
		final Double closingPrice = CollateralUtilities.parseStringAmountToDouble(this.tradeBean.getClosingPriceDaily());
		final Double lotSize = CollateralUtilities.parseStringAmountToDouble(this.tradeBean.getLotSize());
		final Double quantity = CollateralUtilities.parseStringAmountToDouble(this.tradeBean.getNominal());

		if (closingPrice != null) {
			// calculate Mtm
			Double newMtM = (closingPrice * quantity * lotSize) / 100;

			// calculate the sign
			final String direction = this.tradeBean.getDirection();
			if (direction.contains(BORROW)) {
				newMtM *= -1;
			}

			// save Mtm NPV (without haircut)
			this.tradeBean.setMtmNpv(newMtM.toString());

			// haircut will be calculated later by satisfying the keyword above
			this.tradeBean.setMtm(newMtM.toString());
		}

		// calculate mtm applying haircut
		final String hair = this.tradeBean.getHaircut();
		final String hairDirection = this.tradeBean.getHaircutDirection();
		Double haircut = 0.0d;

		// Double factoredMtm = newMtM;
		if (!Util.isEmpty(hair) && !hair.trim().equals("0")
		        && !Util.isEmpty(hairDirection)) {

			haircut = Math.abs(CollateralUtilities.parseStringAmountToDouble(this.tradeBean.getHaircut()));
			if (hairDirection.contains("REC")) {
				haircut += 1.0d;
			} else {
				haircut -= 1.0d;
			}
			// Haircut has to be multiplied by 100, to be coherent with the
			// CollateralUtilities.calculateMARGIN_CALL
			// method
			haircut = Math.abs(haircut) * 100;
		}

		if (haircut != 0.0d) {

			this.trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT,
			        haircut.toString());
		}

		// put nominal as = quantity * lot_size
		if ((quantity != 0.0d) && (lotSize != 0.0d)) {

			// Quantity is always 1 or -1 for CollateralExposure trades
			// this.trade.setQuantity(quantity);

			final Double nominal = quantity * lotSize;
			// this.tradeBean.setNominal(round(nominal));
			if (nominal != null) {
				this.product.setPrincipal(nominal);
			}

		}
	}

	/**
	 * Just to format the decimal of the haircut + %
	 * 
	 * @param value
	 * @return
	 */
	public String round(Double value) {

		DecimalFormat df = new DecimalFormat("0.##");
		return df.format(value) + "%";
	}

	/**
	 * Adds the information for Haircut, Haircut Direction, Repo Rate & Closing
	 * Price at Start
	 * 
	 * @param trade
	 * @param product
	 * @param tradeBean
	 */
	private void buildSecLendingKeywords() {

		// Haircut
		Double hairCutValue = 0.0d;
		if (!Util.isEmpty(this.tradeBean.getHaircut())) {
			hairCutValue = CollateralUtilities.parseStringAmountToDouble(this.tradeBean.getHaircut());

			if ((hairCutValue != null) && (hairCutValue != 0.0d)) {
				hairCutValue *= 100;
			}
		}
		this.product.addAttribute(TRD_IMP_FIELD_HAIRCUT, round(hairCutValue));

		// haircut direction
		if (!Util.isEmpty(this.tradeBean.getHaircutDirection())) {
			this.product.addAttribute(TRD_IMP_FIELD_HAIRCUT_DIRECTION,
			        this.tradeBean.getHaircutDirection());
		}

		this.trade.addKeyword(TRD_IMP_FIELD_HAIRCUT_DIRECTION,
		        this.tradeBean.getHaircutDirection());

		// Closing price at start
		if (!Util.isEmpty(this.tradeBean.getClosingPriceStart())) {
			this.product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE_AT_START,
			        getValueAsAmount(this.tradeBean.getClosingPriceStart()));
		}

		final Amount closingFormatted = new Amount(
		        this.tradeBean.getClosingPriceStart(), 2);
		this.trade.addKeyword(TRD_IMP_FIELD_CLOSING_PRICE_AT_START,
		        closingFormatted.toString());

		// Repo Rate
		if (!Util.isEmpty(this.tradeBean.getRepoRate())) {

			this.product.addAttribute(TRD_IMP_FIELD_REPO_RATE,
			        getValueAsAmount(this.tradeBean.getRepoRate()));
			this.trade.addKeyword(TRD_IMP_FIELD_REPO_RATE,
			        this.tradeBean.getRepoRate());
		}
	}

	/**
	 * @param closingPriceDaily
	 * @return the double value corresponding to the given string
	 */
	private static Amount getValueAsAmount(String closingPriceDaily) {
		Double doubleValue = CollateralUtilities.parseStringAmountToDouble(closingPriceDaily);
		if (doubleValue != null) {
			return new Amount(doubleValue);
		}
		return null;
	}

	// JRL Migratio 14.4
	// /**
	// * @param closingPriceDaily
	// * @return the double value corresponding to the given string
	// */
	// private static Double getDoubleValue(String closingPriceDaily) {
	// try {
	// return new Double(closingPriceDaily);
	// }
	// catch (Exception e) {
	// Log.error(InterfaceSecLendingTradeMapper.class, e);
	// return null;
	// }
	// }

}
