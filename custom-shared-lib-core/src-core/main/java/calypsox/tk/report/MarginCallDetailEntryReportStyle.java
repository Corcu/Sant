/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MarginCallDetailEntryReportStyle extends
        com.calypso.tk.report.MarginCallDetailEntryReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 1295439630145254554L;

    private Trade trade;

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    // Columns
    private static final String TRADE_ID = "bo_reference"; // kw BO_REFERENCE
    private static final String FRONT_ID = "front_Id"; // External Reference
    private static final String CLOSE_OF_BUSINESS = "close_of_business"; // ValueDate
    // private static final String TRADE_DATE = "Trade Date";
    private static final String MATURITY_DATE = "maturity_date";
    private static final String PORTFOLIO = "portfolio"; // Book
    private static final String DEAL_OWNER = "deal_owner"; // Cpty
    private static final String INSTRUMENT = "instrument"; // Subtype
    private static final String UNDERLYING_1 = "underlying"; //
    private static final String UNDERLYING_2 = "underlying_2"; //
    private static final String PRINCIPAL_CCY = "principal_ccy";
    private static final String PRINCIPAL_CCY_2 = "principal_2_ccy";
    private static final String FO_SYSTEM = "trade_keyword_fo_system";
    private static final String UPI_REFERENCE = "trade_keyword_upi_reference";

    // Constants

    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String BO_REFERENCE_KW = "BO_REFERENCE";
    private static final String FO_SYSTEM_KW = "FO_SYSTEM";
    private static final String UPI_REFERENCE_KW = "UPI_REFERENCE";
    private static final String S_PRINCIPALCCY = "principalCcy";
    private static final String S_PRINCIPAL2CCY = "principal2Ccy";

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(TRADE_ID);
        treeList.add(FRONT_ID);
        treeList.add(CLOSE_OF_BUSINESS);
        treeList.add(MATURITY_DATE);
        treeList.add(PORTFOLIO);
        treeList.add(DEAL_OWNER);
        treeList.add(INSTRUMENT);
        treeList.add(UNDERLYING_1);
        treeList.add(UNDERLYING_2);
        treeList.add(PRINCIPAL_CCY);
        treeList.add(PRINCIPAL_CCY_2);
        treeList.add(FO_SYSTEM);
        treeList.add(UPI_REFERENCE);
        return treeList;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors)
            throws InvalidParameterException {

        MarginCallDetailEntryDTO entry = (MarginCallDetailEntryDTO) row
                .getProperty("Default");

        JDatetime valDatetime = (JDatetime) row
                .getProperty("ValuationDatetime");

        if (columnName.compareTo(TRADE_ID) == 0) {
            return getTradeId(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(FRONT_ID) == 0) {
            return getFrontId(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(CLOSE_OF_BUSINESS) == 0) {
            return getCloseOfBusiness(valDatetime);
        } else if (columnName.compareTo(MATURITY_DATE) == 0) {
            return getMaturityDate(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(PORTFOLIO) == 0) {
            return getPortfolio(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(DEAL_OWNER) == 0) {
            return getDealOwner(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(INSTRUMENT) == 0) {
            return getInstrument(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(UNDERLYING_1) == 0) {
            return getUnderlying(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(UNDERLYING_2) == 0) {
            return getUnderlying2(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(PRINCIPAL_CCY) == 0) {
            return getPrincipalCCY(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(PRINCIPAL_CCY_2) == 0) {
            return getPrincipalCCY2(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(FO_SYSTEM) == 0) {
            return getLogicFoSystemKw(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(UPI_REFERENCE) == 0) {
            return getLogicUpiReference(getTrade(entry.getTradeId()));
        } else if (columnName.compareTo(TRADE_DATE) == 0) {
            // CR - 06/07/2018
            JDatetime tradeDate = (JDatetime) super.getColumnValue(row,
                    columnName, errors);
            return getFormattedTradeDate(tradeDate);
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }

    private String getTradeId(final Trade trade) {
        String result = "";
        if (trade != null) {
            result = trade.getKeywordValue(BO_REFERENCE_KW);
        }

        return result;
    }

    private String getFrontId(final Trade trade) {
        if (trade != null && trade.getExternalReference() != null) {
            return trade.getExternalReference();
        }
        return "";
    }

    private String getCloseOfBusiness(final JDatetime valDatetime) {
        // Format 23/08/2018
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        final Date valDate = valDatetime.getJDate(TimeZone.getDefault())
                .getDate(TimeZone.getDefault());
        final String closeOfBusiness = sdf.format(valDate);
        return closeOfBusiness;
    }

    private String getMaturityDate(final Trade trade) {
        if (trade != null && trade.getMaturityDate() != null) {
            // Format 23/08/2018
            final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            final Date matDate = trade.getMaturityDate().getDate(
                    TimeZone.getDefault());
            final String maturityDate = sdf.format(matDate);
            return maturityDate;
        }

        return "";

    }

    private String getPortfolio(final Trade trade) {
        if (trade != null && trade.getBook() != null) {
            return trade.getBook().getName();
        }
        return "";
    }

    private String getDealOwner(final Trade trade) {
        if (trade != null && trade.getCounterParty() != null) {
            return trade.getCounterParty().getName();
        }
        return "";
    }

    private String getInstrument(final Trade trade) {
        if (trade != null) {
            if ("CollateralExposure".equals(trade.getProductType())) {
                return trade.getProductSubType();
            } else {
                return trade.getProductType();
            }
        }
        return "";
    }

    private String getUnderlying(final Trade trade) {

        if (trade != null) {
            final Product product = trade.getProduct();
            if ((product instanceof CollateralExposure)) {
                CollateralExposure collatExpo = (CollateralExposure) product;
                if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
                    return (String) collatExpo.getAttribute("UNDERLYING_1");
                } else {
                    return (String) collatExpo.getAttribute("UNDERLYING");
                }
            }
        }

        return "";
    }

    private boolean is2Legs(String underlyingType) {

        final String[] _twoLegs = {"CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP",
                "FX_SWAP_NON_DELIVERABLE", "FX_SWAP_DELIVERABLE",
                "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT",
                "FX_DELIVERABLE_FORWARD", "EQUITY_SWAP", "CURRENCY_SWAP",
                "BASIS_SWAP"};

        return Arrays.asList(_twoLegs).contains(underlyingType);
    }

    private String getUnderlying2(final Trade trade) {
        if (trade != null) {
            final Product product = trade.getProduct();
            if ((product instanceof CollateralExposure)) {
                CollateralExposure collatExpo = (CollateralExposure) product;
                return (String) collatExpo.getAttribute("UNDERLYING_2");
            }
        }
        return "";
    }

    private String getPrincipalCCY(final Trade trade) {
        HashMap<String, String> map = buildPrincipals(trade);

        if (map != null && !map.isEmpty() && map.get(S_PRINCIPALCCY) != null) {
            return (String) map.get(S_PRINCIPALCCY);
        }

        return "";
    }

    private String getPrincipalCCY2(final Trade trade) {
        HashMap<String, String> map = buildPrincipals(trade);

        if (map != null && !map.isEmpty() && map.get(S_PRINCIPAL2CCY) != null) {
            return (String) map.get(S_PRINCIPAL2CCY);
        }

        return "";
    }

    private String getLogicFoSystemKw(final Trade trade) {
        String result = "";
        if (trade != null) {
            result = trade.getKeywordValue(FO_SYSTEM_KW);
        }

        return result;
    }

    private String getLogicUpiReference(final Trade trade) {
        String result = "";

        if (trade != null) {
            result = trade.getKeywordValue(UPI_REFERENCE_KW);
            if (Util.isEmpty(result)) {
                result = "NotAvailable";
                Log.warn(this, "The trade " + trade.getLongId()
                        + " don't have the keyword UPI_REFERENCE.");
            }
        }

        return result;
    }

    private HashMap<String, String> buildPrincipals(Trade input) {

        HashMap<String, String> map = new HashMap<String, String>();

        if ((input == null)) {
            Log.error(this, "Cannot retrieve the trade.");
        } else {
            Product product = input.getProduct();

            if (product == null) {
                Log.error(this,
                        "Cannot retrieve product of the trade int. ref: "
                                + input.getInternalReference());
            } else {
                map.put(S_PRINCIPALCCY, product.getCurrency());
                map.put(S_PRINCIPAL2CCY, product.getCurrency());

                // derivative
                if ((product instanceof CollateralExposure)) {
                    // take the type
                    map = buildTwoLegs(input, map);
                }
            }
        }

        return map;
    }

    /**
     * Builds the two legs of the trade, putting the data of the principal leg
     * (nominal and ccy) on the left side and the secondary data on the right.
     *
     * @param tradeBeanLeg1
     * @param tradeBeanLeg2
     * @return
     */
    private HashMap<String, String> buildTwoLegs(Trade trade,
                                                 HashMap<String, String> map) {

        String principalCcy = "";
        String principal2Ccy = "";

        CollateralExposure collatExpo;
        String tradeDirection = "";

        if ((trade == null) || (trade.getProduct() == null)) {
            return map;
        }

        if (!(trade.getProduct() instanceof CollateralExposure)) {
            return map;
        }

        collatExpo = (CollateralExposure) trade.getProduct();

        String leg1Direction = (String) collatExpo.getAttribute("DIRECTION_1");
        String leg2Direction = (String) collatExpo.getAttribute("DIRECTION_2");

        if ((leg1Direction == null) || (leg2Direction == null)
                || leg1Direction.isEmpty() || leg2Direction.isEmpty()) {
            return map;
        }

        // leg one with loan is principal
        if ("Buy".equalsIgnoreCase(tradeDirection)
                || "Loan".equalsIgnoreCase(tradeDirection)) {

            if (leg1Direction.equalsIgnoreCase("Loan")
                    || leg1Direction.equalsIgnoreCase("Buy")) { // principal

                principalCcy = (String) collatExpo.getAttribute("CCY_1");
                principal2Ccy = (String) collatExpo.getAttribute("CCY_2");

            } else { // sell, leg1 is borrower
                principal2Ccy = (String) collatExpo.getAttribute("CCY_1");
                principalCcy = (String) collatExpo.getAttribute("CCY_2");
            }

        } else { // leg2 is principal

            if (leg2Direction.equalsIgnoreCase("Borrower")
                    || leg2Direction.equalsIgnoreCase("Sell")) { // principal

                principal2Ccy = (String) collatExpo.getAttribute("CCY_1");
                principalCcy = (String) collatExpo.getAttribute("CCY_2");

            } else { // sell, leg1 is borrower
                principalCcy = (String) collatExpo.getAttribute("CCY_1");
                principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
            }
        }
        map.put(S_PRINCIPALCCY, principalCcy);
        map.put(S_PRINCIPAL2CCY, principal2Ccy);

        return map;
    }

    private Trade getTrade(long tradeId) {

        Trade aTrade = null;

        if (getTrade() == null || getTrade().getLongId() != tradeId) {
            try {
                aTrade = DSConnection.getDefault().getRemoteTrade()
                        .getTrade(tradeId);
                setTrade(aTrade);
            } catch (CalypsoServiceException e) {
                Log.error(this,
                        "Error getting trade with trade id: " + tradeId, e);
            }
        }

        return getTrade();
    }

    // CR - 06/07/2018

    /**
     * Format Trade Date
     *
     * @param tradeDate
     * @return
     */
    private String getFormattedTradeDate(final JDatetime tradeDate) {
        // Format 23/08/2018
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        final Date tDate = tradeDate.getJDate(TimeZone.getDefault()).getDate(
                TimeZone.getDefault());
        final String sTradeDateFormatted = sdf.format(tDate);
        return sTradeDateFormatted;
    }

}