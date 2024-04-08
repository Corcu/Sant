package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Vector;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.generic.SantGenericTradeReportStyle;
import calypsox.tk.report.generic.columns.SantPricerMeasureValueColumn;
import calypsox.tk.util.SantTradeKeywordUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.report.ReportRow;

public class KGR_Export_MtMReportStyle extends SantGenericTradeReportStyle {

	/** Serial UID */
	private static final long serialVersionUID = -148745329263159692L;
	// Constants used for the column names.
	private static final String SOURCE_SYSTEM = "SOURCE_SYSTEM";
	private static final String TRANSACTION_ID = "TRANSACTION_ID";
	private static final String OWNER = "OWNER";
	private static final String COUNTERPARTY = "COUNTERPARTY";
	private static final String AGREEMENT_ID = "AGREEMENT_ID";
	private static final String MTM_BASE = "MTM_BASE";
	private static final String MTM_CURRENCY_BASE = "MTM_CURRENCY_BASE";
	public static final String MTM_BASE_VALUE_OLD = "MTM Base Old";
	public static final String MTM_BASE_VALUE_NEW = "MTM Base New";
	public static final String MTM_DIFF = "MTM Diff";
	public static final String REVISED_MTM_VALUE = "Revised MTM Value";
	public static final String CALCULATED_MTM_VALUE = "Calculated MTM Value";
	private static final String OPERACIONES_FI = "Operaciones FI";
	private static final String YES = "Si";

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { SOURCE_SYSTEM, TRANSACTION_ID, OWNER, COUNTERPARTY, AGREEMENT_ID,
			MTM_BASE, MTM_CURRENCY_BASE };

	@Override
	public TreeList getTreeList() {

		TreeList treeList = super.getTreeList();
		treeList.add(SOURCE_SYSTEM);
		treeList.add(TRANSACTION_ID);
		treeList.add(OWNER);
		treeList.add(COUNTERPARTY);
		treeList.add(AGREEMENT_ID);
		treeList.add(MTM_BASE);
		treeList.add(MTM_CURRENCY_BASE);

		return treeList;
	}

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {

		final SantMTMAuditItem item = (SantMTMAuditItem) row.getProperty(SantMTMAuditItem.SANT_MTM_AUDIT_ITEM);
		final Trade trade = row.getProperty(ReportRow.TRADE);
		final PLMark plMark = (PLMark) row.getProperty(ReportRow.PL_MARK);
		final JDate valDate = (JDate) row.getProperty(SantTradeBrowserReportTemplate.VAL_DATE);
		final PricingEnv pricingEnv = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);

		if (columnName.equals(MTM_VALUE)) {
			return CollateralUtilities.formatNumber(getOriginalMtmValue(item, plMark));
		} else if (columnName.equals(REVISED_MTM_VALUE)) {
			return CollateralUtilities.formatNumber(getRevisedMtmValue(trade, plMark, item, valDate, pricingEnv));
		} else if (columnName.equals(CALCULATED_MTM_VALUE)) {
			return CollateralUtilities.formatNumber(getCalculatedMtmValue(trade, plMark, item, valDate, pricingEnv));
		}
		
		return super.getColumnValue(row, columnName, errors);
	}

	private double getOriginalMtmValue(final SantMTMAuditItem item, final PLMark plMark) {
		if (item.getMarginCallConfig() != null && item.getMarginCallConfig().getContractType().equals("OSLA")) {
			String valorString = getSantPricerMeasureValue(PricerMeasure.S_NPV, plMark).getAmount().toString();

			DecimalFormat df = new DecimalFormat();
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(',');
			symbols.setGroupingSeparator('.');
			valorString = valorString.replace("(", "-");
			valorString = valorString.replace(")", "");
			df.setDecimalFormatSymbols(symbols);
			try {
				double d = df.parse(valorString).doubleValue();
				return d;
			} catch (ParseException e) {
				Log.error(this, e);
			}

		} else {
			String valorString = getSantPricerMeasureValue(PricerMeasure.S_NPV, plMark).getAmount().toString();

			DecimalFormat df = new DecimalFormat();
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator(',');
			symbols.setGroupingSeparator('.');
			valorString = valorString.replace("(", "-");
			valorString = valorString.replace(")", "");
			df.setDecimalFormatSymbols(symbols);
			try {
				return df.parse(valorString).doubleValue();
			} catch (ParseException e) {
				Log.error(this, e);
			}
		}
		return 0.0;
	}

	private double getRevisedMtmValue(final Trade trade, final PLMark plMark, final SantMTMAuditItem item,
			final JDate valDate, final PricingEnv pricingEnv) {
		double mtmValue = 0.0D;
		if (trade != null) {
			Product product = trade.getProduct();
			if (product instanceof Repo) {
				Repo repo = (Repo) product;
				mtmValue = getMtmValue(trade, repo, plMark, valDate, pricingEnv);
			} else if (product instanceof SecLending || product instanceof CollateralExposure || product instanceof PerformanceSwap) {
				mtmValue = getOriginalMtmValue(item, plMark);
			}
		}
		return mtmValue;
	}
	
	private double getCalculatedMtmValue(final Trade trade, final PLMark plMark, final SantMTMAuditItem item,
			final JDate valDate, final PricingEnv pricingEnv) {
		double mtmValue = 0.0D;
		if (trade != null) {
			Product product = trade.getProduct();
			if (product instanceof Repo) {
				Repo repo = (Repo) product;
				mtmValue = getMtmValueSimplified(trade, repo, plMark, valDate, pricingEnv);
			} else if (product instanceof SecLending || product instanceof CollateralExposure || product instanceof PerformanceSwap) {
				mtmValue = getOriginalMtmValue(item, plMark);
			}
		}
		return mtmValue;
	}

	private double getMtmValue(final Trade trade, final Repo repo, final PLMark plMark, final JDate valDate, final PricingEnv pricingEnv) {
		double mtmValue = 0.0D;
		String repoType = repo.getSubType();
		double repoAccruedInterest = getAccruedInterest(plMark, SantPricerMeasure.S_REPO_ACCRUED_INTEREST);
		double principal = getValue(trade, SantTradeKeywordUtil.PRINCIPAL);
		double nominal = getValue(trade, SantTradeKeywordUtil.NOMINAL);
		double dirtyPrice = getValue(trade, SantTradeKeywordUtil.DIRTY_PRICE);
		double indexRatio = getValue(trade, SantTradeKeywordUtil.INDEX_RATIO);
		double secondPart = nominal * (dirtyPrice / 100) * indexRatio;
		double exchangeRate = 1;

		String directionStr = repo.getDirection();
		double direction = 0;
		if (Repo.SUBTYPE_STANDARD.equalsIgnoreCase(repoType)) {
			direction = Repo.DIRECTION_REVERSEREPO.equalsIgnoreCase(directionStr) ? 1 		// loan -> 1
						: Repo.DIRECTION_REPO.equalsIgnoreCase(directionStr) ? -1			// borrower -> -1
						: 0;
			mtmValue = getMtmForRepoStandard(trade, repo, principal, secondPart, repoAccruedInterest, valDate);
			if (isMultiCcy(trade))
				exchangeRate = getBasePrincipalExchangeRate(repo, valDate, trade, direction == 1);
		} else if (Repo.SUBTYPE_BSB.equalsIgnoreCase(repoType)) {
			direction = Repo.DIRECTION_BUY.equalsIgnoreCase(directionStr) ? 1 				// loan -> 1
						: Repo.DIRECTION_SELL.equalsIgnoreCase(directionStr) ? -1			// borrower -> -1
						: 0;
			mtmValue = getMtMforRepoBSB(trade, repo, principal, secondPart, plMark, repoAccruedInterest, valDate, pricingEnv);
		}
		double irisMtmValue = (Math.abs(principal) - (direction * mtmValue)) * exchangeRate;
		
		return irisMtmValue;
	}
	
	private double getMtmValueSimplified(final Trade trade, final Repo repo, final PLMark plMark, final JDate valDate, final PricingEnv pricingEnv) {
		double mtmValue = 0.0D;
		String repoType = repo.getSubType();
		double repoAccruedInterest = getAccruedInterest(plMark, SantPricerMeasure.S_REPO_ACCRUED_INTEREST);
		double principal = getValue(trade, SantTradeKeywordUtil.PRINCIPAL);
		double nominal = getValue(trade, SantTradeKeywordUtil.NOMINAL);
		double dirtyPrice = getValue(trade, SantTradeKeywordUtil.DIRTY_PRICE);
		double indexRatio = getValue(trade, SantTradeKeywordUtil.INDEX_RATIO);
		double secondPart = nominal * (dirtyPrice / 100) * indexRatio;

		if (Repo.SUBTYPE_STANDARD.equalsIgnoreCase(repoType)) {
			mtmValue = getMtmForRepoStandard(trade, repo, principal, secondPart, repoAccruedInterest, valDate);
		} else if (Repo.SUBTYPE_BSB.equalsIgnoreCase(repoType)) {
			mtmValue = getMtMforRepoBSB(trade, repo, principal, secondPart, plMark, repoAccruedInterest, valDate, pricingEnv);
		}
		
		return mtmValue;
	}

	private double getMtmForRepoStandard(Trade trade, Repo repo, double principal, double secondPart,
			double repoAccruedInterest, final JDate valDate) {
		double mtmValue = 0.0D;
		String direction = repo.getDirection();

		if (isMultiCcy(trade)) {
			secondPart *= getBondPrincipalExchangeRate(repo, valDate);
		}

		if (Repo.DIRECTION_REVERSEREPO.equalsIgnoreCase(direction)) {		// loan (repos from SUSI or Operaciones FI)
			mtmValue = -principal + repoAccruedInterest - secondPart;
		} else if (Repo.DIRECTION_REPO.equalsIgnoreCase(direction)) {		// borrower
			if (isFromOperacionesFi(trade))									// repos from Operaciones FI
				mtmValue = -(principal - repoAccruedInterest) - secondPart; // reviewed
			else															// repos from SUSI
				mtmValue = -(principal + repoAccruedInterest) - secondPart; // reviewed
		}

		return mtmValue;
	}

	private double getMtMforRepoBSB(Trade trade, Repo repo, double principal, double secondPart, final PLMark plMark,
			double repoAccruedInterest, final JDate valDate, final PricingEnv pricingEnv) {
		double mtmValue = 0.0D;
		double couponAmount = getCouponAmount(repo, valDate, pricingEnv);
		double couponAccruedInterest = getAccruedInterest(plMark, SantPricerMeasure.S_BOND_ACCRUED_INTEREST);
		String direction = repo.getDirection();

		if (Repo.DIRECTION_BUY.equalsIgnoreCase(direction)) {				// loan (repos from SUSI or Operaciones FI)
			mtmValue = -principal + repoAccruedInterest - couponAmount - couponAccruedInterest - secondPart;
		} else if (Repo.DIRECTION_SELL.equalsIgnoreCase(direction)) {		// borrower
			if (isFromOperacionesFi(trade))									// repos from Operaciones FI
				mtmValue = -(principal - repoAccruedInterest) - (couponAmount + couponAccruedInterest + secondPart);
			else															// repos from SUSI
				mtmValue = -(principal + repoAccruedInterest) - (couponAmount + couponAccruedInterest + secondPart);
		}
		return mtmValue;
	}

	private double getAccruedInterest(final PLMark plMark, final String pricerMeasure) {
		double accruedInterest = 0.0D;
		if (plMark != null) {
			PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, pricerMeasure);
			if (plValue != null) {
				accruedInterest = plValue.getMarkValue();
			}
		}
		return accruedInterest;
	}

	private double getCouponAmount(final Repo repo, final JDate valDate, final PricingEnv pricingEnv) {
		double couponAmount = 0.0D;
		if (repo instanceof CollateralBased) {
			CollateralBased collBased = (CollateralBased) repo;
			couponAmount = collBased.getCollateralCoupon(valDate, pricingEnv);
		}
		return couponAmount;
	}

	private double getValue(final Trade trade, final String keyword) {
		String value = trade.getKeywordValue(keyword);
		if (!Util.isEmpty(value)) {
			return Double.valueOf(value);
		}
		return 0.0D;
	}

	private boolean isFromOperacionesFi(final Trade trade) {
		String boSystem = trade.getKeywordValue(SantTradeKeywordUtil.BO_SYSTEM);
		return !Util.isEmpty(boSystem) && OPERACIONES_FI.equalsIgnoreCase(boSystem);
	}

	private boolean isMultiCcy(final Trade trade) {
		String multiCurrency = trade.getKeywordValue(SantTradeKeywordUtil.MULTICURRENCY);
		if(!Util.isEmpty(multiCurrency)){
			return YES.equalsIgnoreCase(multiCurrency);
		}
		return false;
	}

	private double getBondPrincipalExchangeRate(final Repo repo, final JDate valDate) {
		String repoCcy = repo.getCurrency();			// currency of repo (principal)
		Product security = repo.getSecurity();			// bond
		if (security != null) {
			String secCcy = security.getCurrency();		// currency of bond
			return CollateralUtilities.getFXRate(valDate, secCcy, repoCcy);		// exchange rate:  bond_ccy â?„ principal_ccy
		}
		return 0.0D;
	}

	private double getBasePrincipalExchangeRate(final Repo repo, final JDate valDate, final Trade trade, final boolean isLoan) {
		String princCcy = trade.getKeywordValue(SantTradeKeywordUtil.PRINCIPAL_CCY);
		String baseCcy = null;
		if (isLoan) {										// loan -> base from bond
			Product security = repo.getSecurity();
			if (security != null) 
				baseCcy = security.getCurrency();
		}
		else												// borrower -> base from principal (repo)
			baseCcy = repo.getCurrency();
				
		return (baseCcy == null)
			? 0.0D
			: CollateralUtilities.getFXRate(valDate, baseCcy, princCcy);	// exchange rate:  base_ccy â?„ principal_ccy
	}

	private SantPricerMeasureValueColumn getSantPricerMeasureValue(final String pricerMeasure, final PLMark plMark) {
		return new SantPricerMeasureValueColumn(plMark, pricerMeasure);
	}

}