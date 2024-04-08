package calypsox.tk.report;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.portbreakdown.PortBreakdownTradeWrapper;
import calypsox.tk.report.portbreakdown.PortfolioBreakdownMTMWrapper;
import calypsox.tk.report.portbreakdown.RepoMxPortBreakdownWrapper;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

public class SantPortfolioBreakdownReportStyle extends ReportStyle {

    private static final long serialVersionUID = 1158582952707689874L;

	// For Valuation Date
	public static final String CLOSE_OF_BUSINESS = "Close of Business";

	// For MTM Wrapper
	public static final String INDEPENDENT_AMOUNT_BASE = "Indep. amount Base";
	public static final String INDEPENDENT_CCY_BASE = "Indep. amount Base CCY";
	public static final String INDEPENDENT_AMOUNT = "Indep. amount";
	public static final String INDEPENDENT_AMOUNT_PORTUGAL = "Indep. amount Portugal";
	public static final String INDEPENDENT_AMOUNT_BASE_PORTUGAL = "Indep. amount Base Portugal";
	public static final String INDEPENDENT_CCY = "Indep. amount CCY";
	public static final String MTM_BASE_VALUE = "MTM Base Value";
	public static final String MTM_BASE_CCY = "MTM Base CCY";
	public static final String MTM_CCY = "MTM CCY";
	public static final String MTM_VALUE = "MTM Value";

	// For Trade Wrapper
	public static final String PRINCIPAL_CCY = "Principal CCY";
	public static final String PRINCIPAL = "Principal";
	public static final String PRINCIPAL_2_CCY = "Principal 2 CCY";
	public static final String PRINCIPAL_2 = "Principal 2";
	public static final String FX_RATE_1 = "FX_Rate_1";
	public static final String FX_RATE_2 = "FX_Rate_2";
	public static final String UNDERLYING_1 = "Underlying_1";
	public static final String UNDERLYING_2 = "Underlying_2";
	public static final String INSTRUMENT = "Instrument";

    // for repo enhacements
    public static final String REPO_ACCRUED_INTEREST = "Repo Accrued Interest";

	public static final String REPO_ACCRUED_BO_INTEREST = "Repo Accrued BO Interest";
    public static final String REPO_ACCRUED_INTEREST_PREVIOUS = "Repo Accrued Interest Previous";
    public static final String BOND_ACCRUED_INTEREST = "Bond Accrued Interest";
    public static final String CLEAN_PRICE = "Clean Price";
    public static final String CAPITAL_FACTOR = "Capital Factor";
    public static final String POOL_FACTOR = "Pool Factor";

	// TO CHECK
	public static final String INITIAL_COLLATERAL = "Initial Collateral";
	public static final String CLOSING_PRICE = "Closing Price";
	public static final String SANT_EXPOSURE = "Sant Exposure";

	public static final String HAIRCUT = "Haircut";

	public static final String BO_SYSTEM = "BO_SYSTEM";
	public static final String FO_SYSTEM = "FO_SYSTEM";
	public static final String BO_REFERENCE = "BO_REFERENCE";
	public static final String UTI = "UTI";
	public static final String USI = "USI";
	public static final String STM_REFERENCE = "STM_REFERENCE";

	//CREAM
	public static final String CREAM_ID = "CREAM_ID";
	public static final String FRONT_ID = "FRONT_ID";
	public static final String SBSD_MSBSD = "SBSD_MSBSD";
	public static final String SBS_PRODUCT = "SBS_product";
	public static final String DAY_COUNT_CONVENTION = "Day_Count_Convention";
	public static final String SWAP_AGENT_ID = "Swap_Agent_Id";
	public static final String SWAP_AGENT = "Swap_Agent";
	public static final String START_DATE="Start Date";

	public static final String HAIRCUT_FORMULA = "Haircut_Formula";
	
    public static final String DM_IA_BASE = "DM IA Base";
    public static final String REPO_INTEREST_RATE = "Repo Interest Rate";
	public static final String CLOSING_DATE = "Closing Date";

	public static final String CURRENT_RESET_DATE = "Current Reset Date";
	private final String tradeKwdPrefix = "TRADE_KEYWORD.";

	// Buy Sell
	// Override Calypso behaviour
	public static final String SANT_BUY_SELL = "Buy/Sell";

	public static String[] DEFAULTS_COLUMNS = { CLOSE_OF_BUSINESS, INSTRUMENT, HAIRCUT };

	private final TradeReportStyle tradeReportStyle = getReportStyle(ReportRow.TRADE);

	private final SantMarginCallConfigReportStyleHelper mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final Trade trade = row.getProperty(ReportRow.TRADE);
		final JDate valDate = row.getProperty(SantTradeBrowserReportTemplate.VAL_DATE);

        final PortBreakdownTradeWrapper tradeWrapper = row
                .getProperty(SantPortfolioBreakdownReportTemplate.TRADE_WRAPPER);
        final PortfolioBreakdownMTMWrapper mtmWrapper = row
                .getProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_VAL_DATE);
        final PortfolioBreakdownMTMWrapper mtmWrapperPreviousDay =row
                .getProperty(SantPortfolioBreakdownReportTemplate.MTM_WRAPPER_PREVIOUS_DAY);

		// Report Process Date
		if (columnName.equals(CLOSE_OF_BUSINESS)) {
			return valDate;
		}

        // MTM Wrapper
		else if (columnName.equals(INDEPENDENT_AMOUNT)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				return getIndependentAmount(row, trade, valDate, SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM);
			} else {
				return String.format(Locale.US,"%.2f", mtmWrapperPreviousDay.getIndepAmount().get());
			}
		} else if (columnName.equals(INDEPENDENT_CCY)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
				if(config != null) {
					PLMark mark;
					JDate prevValdate = Holiday.getCurrent().previousBusinessDay(valDate, new Vector<>());
					try {
						mark = DSConnection.getDefault().getRemoteMark().
								getMark("PL", trade.getLongId(), null, config.getPricingEnvName(), prevValdate);
					} catch (PersistenceException e) {
						Log.error(this, e);
						return null;
					}
					if (mark != null) {
						PLMarkValue markValue= mark.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
						if (markValue != null) {
							return markValue.getCurrency();
						}
					}
				}
				return "";
			} else {
				return mtmWrapperPreviousDay.getIndepAmountCcy();
			}
		} else if (columnName.equals(INDEPENDENT_AMOUNT_BASE)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				return getIndependentAmount(row, trade, valDate, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
			} else {
				return String.format(Locale.US,"%.2f", mtmWrapperPreviousDay.getIndepAmountBase().get());
			}
		} else if (columnName.equals(INDEPENDENT_AMOUNT_BASE_PORTUGAL)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				return getIndependentAmountComma(row, trade, valDate, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
			} else {
				return mtmWrapper.getIndepAmountBase();
			}
		} else if (columnName.equals(INDEPENDENT_AMOUNT_PORTUGAL)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				return getIndependentAmountComma(row, trade, valDate, SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM);
			} else {
				return mtmWrapper.getIndepAmount();
			}
		}else if (columnName.equals(INDEPENDENT_CCY_BASE)) {
			if (isEquitySwapOne(trade) && valDate != null) {
				CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
				if(config != null) {
					PLMark mark;
					JDate prevValdate = Holiday.getCurrent().previousBusinessDay(valDate, new Vector<>());
					try {
						mark = DSConnection.getDefault().getRemoteMark().
								getMark("PL", trade.getLongId(), null, config.getPricingEnvName(), prevValdate);
					} catch (PersistenceException e) {
						Log.error(this, e);
						return null;
					}
					if (mark != null) {
						PLMarkValue markValue= mark.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
						if (markValue != null) {
							return markValue.getCurrency();
						}
					}
				}
				return "";
			} else {
				return mtmWrapperPreviousDay.getIndepAmountBaseCcy();
			}
        } else if (columnName.equals(MTM_VALUE)) {
            if (Product.SEC_LENDING.equals(trade.getProductType())) {
                return mtmWrapper.getMarginCall();
            }
            return mtmWrapper.getNpv();
        } else if (columnName.equals(MTM_CCY)) {
            return mtmWrapper.getNpvCcy();
        } else if (columnName.equals(MTM_BASE_VALUE)) {
            return mtmWrapper.getNpvBase();
        } else if (columnName.equals(MTM_BASE_CCY)) {
            return mtmWrapper.getNpvBaseCcy();
        } else if (columnName.equals(CLOSING_PRICE)) {
            if (Product.SEC_LENDING.equals(trade.getProductType())) {
                return getOSLAClosingPrice(tradeWrapper.getPrincipal(), mtmWrapper.getNpv());
            }

			return mtmWrapper.getClosingPrice();

			// repo enhacements
		} else if (columnName.equals(REPO_ACCRUED_INTEREST)) {
			return mtmWrapper.getRepoAccruedInterest();
		} else if (columnName.equals(REPO_ACCRUED_INTEREST_PREVIOUS)) {
			return mtmWrapperPreviousDay.getRepoAccruedInterest();
		}  else if (columnName.equals(BOND_ACCRUED_INTEREST)) {
			if (mtmWrapper instanceof RepoMxPortBreakdownWrapper) {
				return new Amount(mtmWrapper.getBondAccruedInterestRaw(), 4);
			} else {
				return mtmWrapper.getBondAccruedInterest();
			}
		} else if (columnName.equals(CLEAN_PRICE)) {
			return mtmWrapper.getCleanPrice();
		} else if (columnName.equals(CAPITAL_FACTOR)) {
			return mtmWrapper.getCapitalFactor(trade, valDate);
		}

		// Trade Wrapper
		else if (columnName.equals(PRINCIPAL)) {
			return tradeWrapper.getPrincipal();
		} else if (columnName.equals(PRINCIPAL_CCY)) {
			return tradeWrapper.getPrincipalCcy();
		} else if (columnName.equals(PRINCIPAL_2)) {
			return tradeWrapper.getPrincipal2();
		} else if (columnName.equals(PRINCIPAL_2_CCY)) {
			return tradeWrapper.getPrincipal2Ccy();
		} else if (columnName.equals(FX_RATE_1)) {
			return tradeWrapper.getFXRate1();
		} else if (columnName.equals(FX_RATE_2)) {
			return tradeWrapper.getFXRate2();
		} else if (columnName.equals(UNDERLYING_1)) {
			return tradeWrapper.getUndelying1();
		} else if (columnName.equals(UNDERLYING_2)) {
			return tradeWrapper.getUndelying2();
		} else if (columnName.equals(INSTRUMENT)) {
			return tradeWrapper.getInstrument();
		} else if (columnName.equals(INITIAL_COLLATERAL)) {
			return tradeWrapper.getInitialCollateral();

			// repo enhacements
		} else if (columnName.equals(POOL_FACTOR)) {
			return tradeWrapper.getPoolFactor();
		}

		else if (columnName.equals(SANT_EXPOSURE)) {
			if (Product.SEC_LENDING.equals(trade.getProductType())) {
				if ((mtmWrapper.getMarginCall() != null) && (tradeWrapper.getInitialCollateral() != null)) {
					return new Amount(mtmWrapper.getMarginCall().get() - tradeWrapper.getInitialCollateral().get(), 2);
				}
			}
			return null;
		}

		else if (columnName.equals(SANT_BUY_SELL)) {

			String direction;

			if (tradeWrapper.getPrincipal() == null) {
				return null;
			}

			direction = tradeWrapper.getDirection();

			if ((direction == null) || direction.equals("")) {

				if (tradeWrapper.getPrincipal().get() < 0) {
					direction = "Loan";
				} else {
					direction = "Borrower";
				}
			}

			return direction;

			// if (tradeWrapper.getPrincipal().get() < 0) {
			// return "Loan";
			// }
			// return "Borrower";
		}

		else if (columnName.equals(TradeReportStyle.TRADE_DATE)) {
			return tradeWrapper.getTradeDate();
		}

		else if (this.mccReportStyleHelper.isColumnName(columnName)) {
			return this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
		} else if (HAIRCUT.equals(columnName)) {
			// 20/02/2016
			// A?adido Haircut definido para SUSI y OPERACIONES FI
			double haircut = 0;
			if (trade.getProductType().equals("Repo")) {
				Repo product = (Repo) trade.getProduct();
				 haircut=Optional.ofNullable(product.getCollaterals())
						.filter(coll->!Util.isEmpty(coll)).map(coll->coll.get(0)).map(Collateral::getHaircut).orElse(0.0d);
			} else if (trade.getProductType().equals("SecLending")) {
				SecLending product = (SecLending) trade.getProduct();
				haircut=Optional.ofNullable(product.getCollaterals())
						.filter(coll->!Util.isEmpty(coll)).map(coll->coll.get(0)).filter(collat->collat instanceof Collateral)
						.map(collat->((Collateral)collat).getHaircut()).orElse(0.0d);
			} else {
				return haircut;
			}

			if ("OPERACIONES FI".equals(trade.getKeywordValue("BO_SYSTEM"))) {
				return Math.abs(haircut);
			} else if ("SUSI".equals(trade.getKeywordValue("BO_SYSTEM"))) {
				return Math.abs(haircut) * 100;
			} else {
				return haircut;
			}

		} else if (BO_SYSTEM.equals(columnName)||tradeKwdPrefix.concat(BO_SYSTEM).equals(columnName)) {
			return mtmWrapper.getBoSystem();
		}else if (FO_SYSTEM.equals(columnName)||tradeKwdPrefix.concat(FO_SYSTEM).equals(columnName)) {
			Vector<String> domainValuesBondFOSystem = LocalCache.getDomainValues(DSConnection.getDefault(), "BondFOSystem");
			return isBond(trade) ? domainValuesBondFOSystem.get(0) : mtmWrapper.getFoSystem();
		} else if (BO_REFERENCE.equals(columnName)||tradeKwdPrefix.concat(BO_REFERENCE).equals(columnName)) {
			return mtmWrapper.getBoReference();
		} else if (UTI.equals(columnName)) {
			String uti = trade.getKeywordValue("UTI_REFERENCE");
			if(StringUtils.isBlank(uti) && Product.PERFORMANCESWAP.equalsIgnoreCase(trade.getProductType())){
				return trade.getKeywordValue("TempUTITradeId");
			} else{
				return uti;
			}

        } else if (USI.equals(columnName)) {
            return trade.getKeywordValue("USI_REFERENCE");
        } else if (STM_REFERENCE.equals(columnName)) {
            return !Util.isEmpty(trade.getKeywordValue("STM_REFERENCE")) ? trade.getKeywordValue("STM_REFERENCE") : "";
        } else if (CREAM_ID.equals(columnName)) {
            if (Util.isEmpty(trade.getKeywordValue("MurexRootContract"))) {
                return trade.getExternalReference();
            } else {
                return (trade.getProduct() instanceof SecLending) ? trade.getKeywordValue("Contract ID") : trade.getKeywordValue("MurexRootContract");
            }
		} else if (FRONT_ID.equals(columnName)) {
			return !Util.isEmpty(trade.getKeywordValue("Contract ID")) ? trade.getKeywordValue("Contract ID") : trade.getExternalReference();
		} else if (SBSD_MSBSD.equals(columnName)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(SBSD_MSBSD)).orElse("");
        } else if (SBS_PRODUCT.equals(columnName)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(SBS_PRODUCT)).orElse("");
        } else if (DAY_COUNT_CONVENTION.equals(columnName)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(DAY_COUNT_CONVENTION)).orElse("");
        } else if (SWAP_AGENT_ID.equals(columnName)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(SWAP_AGENT_ID)).orElse("");
        } else if (SWAP_AGENT.equals(columnName)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(SWAP_AGENT)).orElse("");
        } else if (START_DATE.equals(columnName) && isBond(trade)) {
            return this.tradeReportStyle.getColumnValue(row, TradeReportStyle.SETTLE_DATE, errors);
        } else if (DM_IA_BASE.equals(columnName)) {
            CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
            DisplayValue amt = getMainPLMarkValueAmount(config, trade, valDate, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
			return amt != null ? String.format(Locale.US,"%.2f",amt) :
					String.format(Locale.US,"%.2f",new Amount(0.0, 2).get());
        } else if (HAIRCUT_FORMULA.equals(columnName)) {
        	String hcFormulaKwd = trade.getKeywordValue("HAIRCUT_FORMULA");
        	return hcFormulaKwd != null && !hcFormulaKwd.isEmpty() ? hcFormulaKwd : "1 / (1 +/- X)";
        } else if (REPO_INTEREST_RATE.equals(columnName)) {
            double spread = 0.0;
            double sfor = 0.0;
			double rate = 0D;

            if (((Repo) trade.getProduct()).getCash()!=null)
				spread = ((Repo) trade.getProduct()).getCash().getSpread() * 100;

            if ((((Repo) trade.getProduct()).getCash().getRateIndex() != null)) {

                String quoteName = "MM." + ((Repo) trade.getProduct()).getCash().getRateIndex().getCurrency() + "." + ((Repo) trade.getProduct()).getCash().getRateIndex().getName() + "." + ((Repo) trade.getProduct()).getCash().getRateIndex().getTenor().getName() + "." + ((Repo) trade.getProduct()).getCash().getRateIndex().getSource();

                QuoteValue value = new QuoteValue();
                value.setQuoteSetName("OFFICIAL");
                value.setName(quoteName);
                value.setQuoteType("Yield");
                value.setDate(valDate);
                final QuoteValue quoteValue;
                try {
                    quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);
                    sfor = quoteValue.getClose() * 100;
                } catch (CalypsoServiceException e) {
                    throw new RuntimeException(e);
                }
            } else {
				rate = ((Repo) trade.getProduct()).getCash().getFixedRate() * 100;
				return new Amount(rate + sfor, 2);
			}

            return new Amount(spread + sfor, 2);

		} else if (CLOSING_DATE.equals(columnName)) {
			if (trade.getProduct() instanceof SecFinance) {
				SecFinance secFinance = (SecFinance) trade.getProduct();
				if (secFinance.getMaturityType().equalsIgnoreCase("OPEM"))
					return "";
				else secFinance.getEndDate();
			}
			return trade.getMaturityDate();

		} else if (columnName.equals(REPO_ACCRUED_BO_INTEREST)) {
			return mtmWrapper.getRepoAccruedBOInterest();
		} else if (columnName.equals((CURRENT_RESET_DATE))) {
			return getResetDateFromInterestFlow(trade, valDate);
		} else {
            Object retVal = this.tradeReportStyle.getColumnValue(row, columnName, errors);
            if (retVal == null) {
                retVal = this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
            }
            return retVal;
        }

    }

    /**
     * Check if is equity swap one product type
     *
     * @param trade the current trade
     * @return true if is equity swap one
     */
    private boolean isEquitySwapOne(Trade trade) {
        if (trade != null && trade.getProduct() != null && trade.getProduct() instanceof CollateralExposure &&
                !Util.isEmpty(trade.getKeywordValue("BO_SYSTEM")) &&
                "SWAP_ONE".equals(trade.getKeywordValue("BO_SYSTEM"))) {
            CollateralExposure coe = (CollateralExposure) trade.getProduct();
            return !Util.isEmpty(coe.getUnderlyingType()) && "EQUITY_SWAP".equals(coe.getUnderlyingType());
        }
        return false;
    }

    private boolean isBond(Trade trade) {
        return Optional.ofNullable(trade).map(Trade::getProduct)
                .map(p -> p instanceof Bond).orElse(false);
    }

	private Object getOSLAClosingPrice(DisplayValue quantity, DisplayValue mtmValue) {
		if ((quantity == null) || (mtmValue == null)) {
			return null;
		}
		double qty = Math.abs(quantity.get());
		double mtm = Math.abs(mtmValue.get());
		if (Math.abs(qty) == 0) {
			return null; // cannot divide by 0
		}
		BigDecimal bigQuantity = new BigDecimal(qty);
		BigDecimal bigMtm = new BigDecimal(mtm);
		return bigMtm.divide(bigQuantity, 4, RoundingMode.HALF_EVEN);
		// return null;
	}

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(this.tradeReportStyle.getNonInheritedTreeList());
		treeList.add(this.mccReportStyleHelper.getTreeList());
		treeList.add(this.tradeReportStyle.getTreeList());
		treeList.add(HAIRCUT);
		treeList.add(BO_SYSTEM);
		treeList.add(FO_SYSTEM);
		treeList.add(BO_REFERENCE);
		treeList.add(UTI);
		treeList.add(USI);
		treeList.add(CREAM_ID);
		treeList.add(FRONT_ID);
		treeList.add(STM_REFERENCE);
		treeList.add(SBSD_MSBSD);
		treeList.add(SBS_PRODUCT);
		treeList.add(DAY_COUNT_CONVENTION);
		treeList.add(SWAP_AGENT_ID);
		treeList.add(SWAP_AGENT);
		return treeList;
	}

    /**
     * Get the main PLMark value by trade id, pricing env, and value date
     *
     * @param config  the CollateralConfig
     * @param trade   the Trade
     * @param valDate the valuation date
     * @param name    the PLMark name
     * @return the PLMark amount
     */
    private DisplayValue getMainPLMarkValueAmount(CollateralConfig config, Trade trade, JDate valDate, String name) {
        if (config != null && trade != null && valDate != null && !Util.isEmpty(name) && !Util.isEmpty(config.getPricingEnvName())) {
            PLMark mark;
            try {
                mark = DSConnection.getDefault().getRemoteMark().
                        getMark("PL", trade.getLongId(), null, config.getPricingEnvName(), valDate);
            } catch (PersistenceException e) {
                Log.error(this, e);
                return null;
            }
            return mark != null && mark.getPLMarkValueByName(name) != null ?
                    CollateralUtilities.formatAmount(mark.getPLMarkValueByName(name).getMarkValue(),
                            mark.getPLMarkValueByName(name).getCurrency()) : null;
        }
        return null;
    }

	private String getIndependentAmount(ReportRow row, Trade trade, JDate valDate, String plMarkName){
		CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
		if(config != null){
			JDate prevValdate = Holiday.getCurrent().previousBusinessDay(valDate, new Vector<>());
			DisplayValue amt = getMainPLMarkValueAmount(config, trade, prevValdate, plMarkName);
			return amt != null ? String.format(Locale.US,"%.2f",amt.get()) :
					String.format(Locale.US,"%.2f",new Amount(0.0, 2).get());
		}
		return String.format(Locale.US,"%.2f",new Amount(0.0, 2).get());
	}

	private DisplayValue getIndependentAmountComma(ReportRow row, Trade trade, JDate valDate, String plMarkName){
		CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
		if(config != null){
			DisplayValue amt = getMainPLMarkValueAmount(config, trade, valDate, plMarkName);
			return amt != null ? amt : new Amount(0.0, 2);
		}
		return new Amount(0.0, 2);
	}

	private JDate getResetDateFromInterestFlow(Trade trade, JDate valDate) {
		JDate resetDate = null;
		if (trade.getProduct() instanceof Repo) {
			if (((Repo) trade.getProduct()).getFlows() != null) {
				CashFlow currentInterestFlow = ((Repo) trade.getProduct()).getFlows().findEnclosingCashFlow(valDate, CashFlow.INTEREST);
				if (currentInterestFlow != null)
					resetDate = currentInterestFlow.getStartDate();
			}
		}
		return resetDate != null ? resetDate : valDate;
	}
}
