package calypsox.tk.report;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.DisplayDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.PLMarkReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

public class SantMTMAuditReportStyle extends ReportStyle {

    private static final long serialVersionUID = 1274465876998289361L;
    public static final String PO = "PO";
    public static final String COUNTERPARTY = "Counterpary";
    public static final String CONTRACT_ID = "Collateral Agreement ID";
    public static final String CONTRACT_NAME = "Collateral Agreement Name";
    public static final String TRADE_ID = "Trade id";
    public static final String TRADE_DATE = "Trade date";
    public static final String SETTLE_DATE = "Settle date";
    public static final String MATURITY_DATE = "Maturity date";
    public static final String PRODUCT_TYPE = "Product Type";
    public static final String PORTFOLIO = "Portfolio";
    public static final String CHANGE_REASON = "Change Reason";
    public static final String CURRENCY = "CURRENCY";
    public static final String MEASURE_NAME = "Measure Name";
    public static final String MTM_VALUE_DATE = "MTM Value date";
    public static final String OLD_VALUE = "MTM Old Value";
    public static final String NEW_VALUE = "MTM New Value";
    public static final String MODIF_DATE = "MODIF_DATE";

    public static final String[] DEFAULTS_COLUMNS = {PO, COUNTERPARTY, CONTRACT_ID, CONTRACT_NAME, TRADE_ID,
            TRADE_DATE, SETTLE_DATE, MATURITY_DATE, PRODUCT_TYPE, PORTFOLIO, CHANGE_REASON, MTM_VALUE_DATE, CURRENCY,
            MEASURE_NAME, OLD_VALUE, NEW_VALUE, MODIF_DATE};

    private TradeReportStyle tradeReportStyle;
    private PLMarkReportStyle plMarkReportStyle;

    @Override
    public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
            throws InvalidParameterException {
        SantMTMAuditItem mtmAuditItem = (SantMTMAuditItem) row.getProperty(SantMTMAuditItem.SANT_MTM_AUDIT_ITEM);
        CollateralConfig marginCallConfig = (CollateralConfig) row.getProperty(ReportRow.MARGIN_CALL_CONFIG);

        if (columnName.equals(PO)) {
            return mtmAuditItem.getTrade().getBook().getLegalEntity().getCode();

        } else if (columnName.equals(COUNTERPARTY)) {
            return mtmAuditItem.getTrade().getCounterParty().getCode();

        } else if (columnName.equals(CONTRACT_ID)) {
            return mtmAuditItem.getTrade().getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER);

        } else if (columnName.equals(CONTRACT_NAME)) {
            if (marginCallConfig != null) {
                return marginCallConfig.getName();
            } else {
                return "";
            }

        } else if (columnName.equals(TRADE_ID)) {
            return mtmAuditItem.getTrade().getLongId();

        } else if (columnName.equals(TRADE_DATE)) {
            return mtmAuditItem.getTradeDate();

        } else if (columnName.equals(SETTLE_DATE)) {
            return mtmAuditItem.getTrade().getSettleDate();

        } else if (columnName.equals(MATURITY_DATE)) {
            return mtmAuditItem.getTrade().getMaturityDate();

        } else if (columnName.equals(PRODUCT_TYPE)) {
            return mtmAuditItem.getTrade().getProductType();

        } else if (columnName.equals(PORTFOLIO)) {
            return mtmAuditItem.getTrade().getBook().getName();

        } else if (columnName.equals(CHANGE_REASON)) {
            return mtmAuditItem.getChangeReason();

        } else if (columnName.equals(MTM_VALUE_DATE)) {
            return mtmAuditItem.getMtmValDate();

        } else if (columnName.equals(CURRENCY)) {
            return mtmAuditItem.getMarkCcy();

        } else if (columnName.equals(MEASURE_NAME)) {
            return mtmAuditItem.getMarkName();

        } else if (columnName.equals(OLD_VALUE)) {
            return mtmAuditItem.getOldValue();

        } else if (columnName.equals(NEW_VALUE)) {
            return mtmAuditItem.getNewValue();

        } else if (columnName.equals(MODIF_DATE)) {
            return mtmAuditItem.getModifDate();
        } else {
            Object retVal = null;
            final Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
            final PLMark plMark = (PLMark) row.getProperty(ReportRow.PL_MARK);

            if (mtmAuditItem.getTrade() != null) {
                // Specific to trade date to remove timestamp
                if (TradeReportStyle.TRADE_DATE.equals(columnName)) {
                    if (trade.getTradeDate() == null) {
                        return null;
                    }
                    retVal = new DisplayDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
                } else {
                    retVal = getTradeReportStyle().getColumnValue(row, columnName, errors);
                }
            }

            if ((retVal == null) && (plMark != null)) {
                retVal = getPLMarkReportStyle().getColumnValue(row, columnName, errors);
            }

            return retVal;
        }

    }

    @Override
    public TreeList getTreeList() {
        if (this._treeList != null) {
            return this._treeList;
        }
        @SuppressWarnings("deprecation") final TreeList treeList = super.getTreeList();

        if (this.tradeReportStyle == null) {
            this.tradeReportStyle = getTradeReportStyle();
        }

        if (this.tradeReportStyle != null) {
            treeList.add(this.tradeReportStyle.getNonInheritedTreeList());
        }
        if (this.plMarkReportStyle == null) {
            this.plMarkReportStyle = getPLMarkReportStyle();
        }
        if (this.plMarkReportStyle != null) {
            treeList.add(this.plMarkReportStyle.getNonInheritedTreeList());
        }

        return treeList;
    }

    protected TradeReportStyle getTradeReportStyle() {
        if (this.tradeReportStyle == null) {
            this.tradeReportStyle = (TradeReportStyle) getReportStyle(ReportRow.TRADE);
        }
        return this.tradeReportStyle;
    }

    protected PLMarkReportStyle getPLMarkReportStyle() {
        if (this.plMarkReportStyle == null) {
            this.plMarkReportStyle = (PLMarkReportStyle) getReportStyle(ReportRow.PL_MARK);
        }
        return this.plMarkReportStyle;
    }

}
