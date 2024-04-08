package calypsox.tk.report;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallAllocationEntry;
import calypsox.tk.report.style.SantMarginCallAllocationReportStyleHelper;
import calypsox.tk.report.style.SantMarginCallConfigReportStyleHelper;
import calypsox.tk.report.style.SantMarginCallEntryReportStyleHelper;
import calypsox.tk.report.style.SantTradeReportStyleHelper;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.collateral.SecurityAllocationFacade;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.security.InvalidParameterException;
import java.util.*;

public class SantResultOfMarginCallCalculationReportStyle extends ReportStyle {

    // Contract
    private final SantMarginCallConfigReportStyleHelper mccReportStyleHelper;

    // Allocation
    private final SantMarginCallAllocationReportStyleHelper allocReportStyleHelper;

    // Entry
    private final SantMarginCallEntryReportStyleHelper entryReportStyleHelper;

    // Trade
    private final SantTradeReportStyleHelper tradeReportStyleHelper;

    public static final String EVENT = "Event";
    public static final String DEALS = "Deals";
    public static final String DISPUTE = "Dispute";
    public static final String DISPUTE_EXPOSURE = "Dispute Exposure";
    public static final String DISPUTE_DATE = "Dispute Date";
    public static final String DISPUTE_TYPE = "Dispute Type";
    public static final String EXPOSURE_COUNTERPARTY = "Exposure Counterparty";
    public static final String VALUEDATE_DEFINE_CASH = "Valuedate Define Cash";
    public static final String VALUEDATE_DEFINE_BOND = "Valuedate Define Bond";
    public static final String FACE_AMOUNT_BOND = "Face Amount Bond";
    public static final String MARGIN_CALL_CALCULATION = "Margin Call Calculation";
    public static final String COLLATERAL_IN_TRANSIT_CASH = "Collateral in transit Cash";
    public static final String COLLATERAL_IN_TRANSIT_BOND = "Collateral in transit Bond";
    public static final String EFFECTIVE_CURRENCY_1 = "Effective Currency1";
    public static final String EFFECTIVE_CURRENCY_2 = "Effective Currency2";
    public static final String EFFECTIVE_CURRENCY_3 = "Effective Currency3";
    public static final String PRICE_BOND = "Price Bond";
    public static final String HAIRCUT_BOND = "Haircut Bond";

    private static List<String> MARGIN_CALL_NOT_CALCULTAED_STATUS = new ArrayList<String>();
    private static List<String> MARGIN_CALL_DISPUTE_STATUS = Arrays.asList("DISPUTE_PENDING", "DISPUTED(D. SEN)",
            "DISPUTED(NOCALL)", "DISPUTED(PAY)", "DISPUTED(RECV)");
    private static final long serialVersionUID = 1L;

    public SantResultOfMarginCallCalculationReportStyle() {
        this.mccReportStyleHelper = new SantMarginCallConfigReportStyleHelper();
        this.allocReportStyleHelper = new SantMarginCallAllocationReportStyleHelper();
        this.entryReportStyleHelper = new SantMarginCallEntryReportStyleHelper();
        this.tradeReportStyleHelper = new SantTradeReportStyleHelper();
    }

    @Override
    public TreeList getTreeList() {
        if (this._treeList != null) {
            return this._treeList;
        }

        final TreeList treeList = new TreeList();
        treeList.add("SantCollateral", this.entryReportStyleHelper.getTreeList());
        treeList.add("SantCollateral", this.mccReportStyleHelper.getTreeList());
        treeList.add("SantCollateral", this.allocReportStyleHelper.getTreeList());
        treeList.add("SantCollateral", this.tradeReportStyleHelper.getTreeList());
        treeList.add("SantCollateral", EVENT);
        treeList.add("SantCollateral", DEALS);
        treeList.add("SantCollateral", DISPUTE);
        treeList.add("SantCollateral", DISPUTE_EXPOSURE);
        treeList.add("SantCollateral", DISPUTE_DATE);
        treeList.add("SantCollateral", DISPUTE_TYPE);
        treeList.add("SantCollateral", EXPOSURE_COUNTERPARTY);
        treeList.add("SantCollateral", VALUEDATE_DEFINE_BOND);
        treeList.add("SantCollateral", VALUEDATE_DEFINE_CASH);
        treeList.add("SantCollateral", FACE_AMOUNT_BOND);
        treeList.add("SantCollateral", MARGIN_CALL_CALCULATION);
        treeList.add("SantCollateral", COLLATERAL_IN_TRANSIT_CASH);
        treeList.add("SantCollateral", COLLATERAL_IN_TRANSIT_BOND);
        treeList.add("SantCollateral", EFFECTIVE_CURRENCY_1);
        treeList.add("SantCollateral", EFFECTIVE_CURRENCY_2);
        treeList.add("SantCollateral", EFFECTIVE_CURRENCY_3);
        treeList.add("SantCollateral", PRICE_BOND);
        treeList.add("SantCollateral", HAIRCUT_BOND);
        return treeList;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        if (row == null) {
            return null;
        }
        SantMarginCallAllocationEntry santAlloc = (SantMarginCallAllocationEntry) row
                .getProperty("SantMarginCallAllocationEntry");

        if (santAlloc == null) {
            return null;
        }

        CollateralConfig mcc = santAlloc.getMarginCallConfig();

        if (mcc == null) {
            return null;
        }

        initNotCalculatedStatus();

        if (EVENT.equals(columnName)) {
            return "Margin Call";
        } else if (DEALS.equals(columnName)) {
            return santAlloc.getSantEntry().getNbTrades();
        } else if (MARGIN_CALL_CALCULATION.equals(columnName)) {
            return (MARGIN_CALL_NOT_CALCULTAED_STATUS.contains(santAlloc.getSantEntry().getEntry().getStatus()) ? "NO"
                    : "YES");
        } else if (EFFECTIVE_CURRENCY_1.equals(columnName)) {
            return (santAlloc.getSantEntry().getAllocationsCurrencies().contains("EUR") ? "EUR" : "");
        } else if (EFFECTIVE_CURRENCY_2.equals(columnName)) {
            return (santAlloc.getSantEntry().getAllocationsCurrencies().contains("USD") ? "USD" : "");
        } else if (EFFECTIVE_CURRENCY_3.equals(columnName)) {
            return (santAlloc.getSantEntry().getAllocationsCurrencies().contains("GBP") ? "GBP" : "");
        } else if (DISPUTE.equals(columnName)) {
            if (isMarginDisputed(santAlloc)) {
                Double dispAmount = getDisputeAmount(santAlloc);
                if (dispAmount != null) {
                    return new Amount(getDisputeAmount(santAlloc), 2);
                }
            }
            return null;
        } else if (DISPUTE_DATE.equals(columnName)) {
            if (isMarginDisputed(santAlloc)) {
                return santAlloc.getSantEntry().getEntry().getProcessDatetime().getJDate(TimeZone.getDefault());
            }
            return null;
        } else if (DISPUTE_EXPOSURE.equals(columnName)) {
            if (isMarginDisputed(santAlloc)) {
                Double dispAmount = getDisputeAmount(santAlloc);
                if ((dispAmount != null) && (santAlloc.getSantEntry().getEntry().getNetExposure() != 0)) {
                    return new Amount((dispAmount * 100) / santAlloc.getSantEntry().getEntry().getNetExposure(), 2);
                }
            }
            return null;
        } else if (DISPUTE_TYPE.equals(columnName)) {
            if (isMarginDisputed(santAlloc)) {
                Double dispAmount = getDisputeAmount(santAlloc);
                if (dispAmount != null) {
                    return (dispAmount > 0 ? "Deficit" : "exceso");
                }

            }
            return null;
        } else if (EXPOSURE_COUNTERPARTY.equals(columnName)) {
            Double cptyExpAmount = getCptyExposureAmount(santAlloc);
            if (cptyExpAmount != null) {
                return new Amount(cptyExpAmount);
            }
            return null;
        } else if (VALUEDATE_DEFINE_BOND.equals(columnName)) {
            if ((!santAlloc.isDummy())
                    && SecurityAllocation.UNDERLYING_TYPE.equals(santAlloc.getAllocation().getUnderlyingType())) {
                return santAlloc.getAllocation().getSettlementDate();
            }
            return null;
        } else if (VALUEDATE_DEFINE_CASH.equals(columnName)) {
            if ((!santAlloc.isDummy())
                    && CashAllocation.UNDERLYING_TYPE.equals(santAlloc.getAllocation().getUnderlyingType())) {
                return santAlloc.getAllocation().getSettlementDate();
            }
            return null;
        } else if (COLLATERAL_IN_TRANSIT_BOND.equals(columnName)) {
            return new Amount(santAlloc.getSantEntry().getSecurityInTransitBase(), 2);
        } else if (COLLATERAL_IN_TRANSIT_CASH.equals(columnName)) {
            return new Amount(santAlloc.getSantEntry().getCashInTransitBase(), 2);
        } else if (FACE_AMOUNT_BOND.equals(columnName)) {
            if ((!santAlloc.isDummy())
                    && SecurityAllocation.UNDERLYING_TYPE.equals(santAlloc.getAllocation().getUnderlyingType())) {
                return new Amount(((SecurityAllocationFacade) santAlloc.getAllocation()).getNominal(), 2);
            }
            return null;
        } else if (PRICE_BOND.equals(columnName)) {
            if ((!santAlloc.isDummy())
                    && SecurityAllocation.UNDERLYING_TYPE.equals(santAlloc.getAllocation().getUnderlyingType())) {
                MarginCallEntry entry;
                entry = SantMarginCallUtil.getMarginCallEntry(santAlloc.getSantEntry().getEntry(), mcc, false);
                SecurityAllocation secAlloc = new SecurityAllocation(entry,
                        ((SecurityAllocationDTO) santAlloc.getAllocation()));
                DisplayValue displayValue = secAlloc.getPriceDisplayValue();
                displayValue.set(secAlloc.getDirtyPrice());
                return displayValue;
            }
            return null;
        } else if (columnName.startsWith(SantMarginCallConfigReportStyleHelper.MCC_LE_RATING_PREFIX)) {
            int leId = mcc.getLegalEntity().getId();
            String agency = columnName.substring(SantMarginCallConfigReportStyleHelper.MCC_LE_RATING_PREFIX.length());
            return getCreditRatingValue(leId, agency, santAlloc.getSantEntry().getEntry().getProcessDatetime()
                    .getJDate(TimeZone.getDefault()));
        } else if (columnName.startsWith(SantMarginCallConfigReportStyleHelper.MCC_PO_RATING_PREFIX)) {
            int poId = mcc.getProcessingOrg().getId();
            String agency = columnName.substring(SantMarginCallConfigReportStyleHelper.MCC_PO_RATING_PREFIX.length());
            return getCreditRatingValue(poId, agency, santAlloc.getSantEntry().getEntry().getProcessDatetime()
                    .getJDate(TimeZone.getDefault()));
        } else if (HAIRCUT_BOND.equals(columnName)) {
            if (santAlloc.isDummy()) {
                return null;
            }
            //JRL 20/04/2016 Migration 14.4
            return new Amount(100 - (Math.abs(santAlloc.getAllocation().getHaircut()) * 100), 2);
        } else if ("MCC.LE MTA".equals(columnName)) {
            // The column says LE MTA but it is actually a value from the Entry
            return formatNumber(santAlloc.getSantEntry().getEntry().getMTAAmount());
        } else if ("MCC.LE Threshold Amount".equals(columnName)) {
            // The column says LE threshold but it is actually a value from the Entry
            return formatNumber(santAlloc.getSantEntry().getEntry().getThresholdAmount());
        }

        // Allocation
        if (this.allocReportStyleHelper.isColumnName(columnName) && (!santAlloc.isDummy())) {
            row.setProperty(ReportRow.DEFAULT, santAlloc.getAllocation());
            return this.allocReportStyleHelper.getColumnValue(row, columnName, errors);
        }

        // Entry
        else if (this.entryReportStyleHelper.isColumnName(columnName) && (santAlloc.getSantEntry().getEntry() != null)) {
            row.setProperty(ReportRow.DEFAULT, santAlloc.getSantEntry().getEntry());
            return this.entryReportStyleHelper.getColumnValue(row, columnName, errors);
        }
        // Contract
        else if (this.mccReportStyleHelper.isColumnName(columnName) && (santAlloc.getMarginCallConfig() != null)) {
            row.setProperty(ReportRow.MARGIN_CALL_CONFIG, santAlloc.getMarginCallConfig());
            return this.mccReportStyleHelper.getColumnValue(row, columnName, errors);
        }
        // Trade
        else if (this.tradeReportStyleHelper.isColumnName(columnName) && (santAlloc.getTrade() != null)) {
            row.setProperty(ReportRow.TRADE, santAlloc.getTrade());
            return this.tradeReportStyleHelper.getColumnValue(row, columnName, errors);
        }

        return null;
    }

    /**
     * @param santAlloc
     * @return
     */
    private Double getDisputeAmount(SantMarginCallAllocationEntry santAlloc) {
        return getAmountFromEntryAttibute(santAlloc, "MtM difference");
    }

    private boolean isMarginDisputed(SantMarginCallAllocationEntry santAlloc) {
        if (MARGIN_CALL_DISPUTE_STATUS.contains(santAlloc.getSantEntry().getEntry().getStatus())
                || (getDisputeAmount(santAlloc) != null)) {
            return true;
        }
        return false;
    }

    private static String formatNumber(final double number) {

        return Util.numberToString(number, 2, Locale.getDefault(), true);

    }

    /**
     * @param santAlloc
     * @return
     */
    private Double getCptyExposureAmount(SantMarginCallAllocationEntry santAlloc) {
        return getAmountFromEntryAttibute(santAlloc, "Cpty MtM");
    }

    /**
     * @param santAlloc
     * @param attributeName
     * @return
     */
    private Double getAmountFromEntryAttibute(SantMarginCallAllocationEntry santAlloc, String attributeName) {
        Double disputeAmount = null;
        try {
            disputeAmount = (Double) santAlloc.getSantEntry().getEntry().getAttribute(attributeName);
        } catch (Exception e) {
            disputeAmount = null;
            Log.error(this, e);//Sonar
        }
        return disputeAmount;
    }

    private static void initNotCalculatedStatus() {
        if ((MARGIN_CALL_NOT_CALCULTAED_STATUS != null) && (MARGIN_CALL_NOT_CALCULTAED_STATUS.size() > 0)) {
            return;
        }
        @SuppressWarnings("rawtypes")
        Vector notCalculatedStatusDomain = LocalCache.getDomainValues(DSConnection.getDefault(),
                "MarginCallNotCalculatedStatus");
        if ((notCalculatedStatusDomain != null) && (notCalculatedStatusDomain.size() > 0)) {
            for (int i = 0; i < notCalculatedStatusDomain.size(); i++) {
                String domainValue = (String) notCalculatedStatusDomain.get(i);
                if (domainValue != null) {
                    MARGIN_CALL_NOT_CALCULTAED_STATUS.add(domainValue);
                }
            }
        }
    }

    public static String getCreditRatingValue(final int leId, final String agency, JDate processDate) {
        JDate asOfDate = processDate;
        if (processDate == null) {
            asOfDate = JDate.getNow();
        }
        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setDebtSeniority("SENIOR_UNSECURED");
        cr.setAgencyName(agency);
        cr.setRatingType(CreditRating.CURRENT);
        cr.setAsOfDate(asOfDate);
        cr = BOCache.getLatestRating(DSConnection.getDefault(), cr);
        if (cr != null) {
            return cr.getRatingValue();
        }
        return null;
    }

}
