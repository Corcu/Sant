package calypsox.tk.report;

import com.calypso.tk.core.JDate;
import com.calypso.tk.report.MarginCallPositionBaseReportStyle;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.TradeReportTemplate;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;


public class SACCRInventoryPositionReportTemplate extends SantCollateralBOPositionReportTemplate {

    public static final String COLLATERAL_CONFIG_TYPE = "Contract type";
    public static final String COLLATERAL_MOVEMENT_TYPE = "Movement type";
    public static final String COLLATERAL_PROCESS_DATE = "Collateral Process Date";
    public static final String COLLATERAL_VALUE_DATE = "Collateral Value Date";
    public static final String COLLATERAL_MATURITY_DATE = "Collateral Maturity Date";
    public static final String COLLATERAL_IN_TRANSIT = "Collateral In Transit";
    public static final String CCP_PLATFORM = "CCP Platform";
    public static final String SEGREGATED_COLLATERAL = "Segregated Collateral";
    public static final String MARGIN_CALL_ENTRY_DIRECTION = "MCEntry Direction custom";
    public static final String MARGIN_TYPE = "Margin Type";
    public static final String MARGIN_CALL_ENTRY = "MarginCallEntry";
    public final static String ACTUAL = "ACTUAL";
    public final static String THEORETICAL = "THEORETICAL";
    public final static String CASH = "Cash";
    public final static String SECURITY = "Security";
    public final static String IM_AMOUNT = "IM Amount";
    public static final String SOURCE_SYSTEM = "MarginCallConfig.Processing Org.Attribute.SOURCE_SYSTEM_IRIS";
    
    public static final String DEAL_ID_LAGO = "Deal ID";
    /*
     * Default Columns constants
     */
    public static String[] DEFAULT_COLUMNS = {SOURCE_SYSTEM, "MarginCallEntry.MarginCallConfig.ADDITIONAL_FIELD.SEGREGATED_COLLATERAL", "MarginCallEntry.ADDITIONAL_FIELD.SEGREGATED_COLLATERAL",
            COLLATERAL_CONFIG_TYPE, COLLATERAL_MOVEMENT_TYPE, MarginCallPositionBaseReportStyle.CURRENCY, "Nominal", MarginCallPositionBaseReportStyle.FX_RATE,
            MarginCallPositionBaseReportStyle.CONTRACT_VALUE, COLLATERAL_IN_TRANSIT, COLLATERAL_PROCESS_DATE, COLLATERAL_VALUE_DATE, COLLATERAL_MATURITY_DATE, "Description", "Product.Security Id",
            MARGIN_CALL_ENTRY_DIRECTION, MarginCallPositionBaseReportStyle.HAIRCUT, "Type", MarginCallPositionReportStyle.SANT_DIRTY_PRICE, MarginCallPositionBaseReportStyle.CLEAN_PRICE,
            CCP_PLATFORM, SEGREGATED_COLLATERAL, "Long (Nominal)", "Long (Quantity)", "Short (Nominal)", "Short (Quantity)", "Quote Price", "Quantity", "Value", "Next Coupon Record Date",
            "Next Dividend Date", "Next Dividend Record Date", MARGIN_TYPE, DEAL_ID_LAGO, IM_AMOUNT};

    /**
     * Default columns, define in order based on DDR
     */
    @Override
    public void setDefaults() {
        super.setDefaults();
        this.resetColumns();
        Set<String> columns = new LinkedHashSet<String>(
                Arrays.asList(DEFAULT_COLUMNS));
        columns.addAll(Arrays.asList(getColumns()));
        removeDateColumns(toVector(getColumns()));
        removeParentColumns(columns);
        setColumns(columns.toArray(new String[0]));
    }

    private void removeParentColumns(Set<String> columns) {
        columns.removeIf(s -> Arrays.asList(SantCollateralBOPositionReportStyle.DEFAULT_COLUMN_NAMES).contains(s));
    }

    /**
     * Call before loading the report (done by the report before the load).
     */
    @Override
    public void callBeforeLoad() {
        JDate valDate = this._valDate;
        if (valDate == null) {
            valDate = JDate.getNow();
        }

        this.startDate = com.calypso.tk.report.Report.getDate(this, valDate, TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        this.endDate = Report.getDate(this, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
                TradeReportTemplate.END_TENOR);
        if (this.endDate == null) {
            this.endDate = this.startDate.addDays(5);
        }
    }
}
