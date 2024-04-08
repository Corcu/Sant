package calypsox.tk.report;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryPositionArray;

import java.util.*;

public class SantInventoryMissingSecurityQuotesReport extends com.calypso.tk.report.BOSecurityPositionReport {

    private static final long serialVersionUID = -8517842518921351033L;
    public final static String MOVEMENT_BALANCE = "Balance";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(Vector errorMsgs) {
        try {

            return load();

        } catch (Exception e) {
            Log.error(this, e);
            errorMsgs.add(e.getMessage());
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    private ReportOutput load() throws Exception {

        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");

        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_DATE, "Settle");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Internal");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_TYPE, "Theoretical");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
        getReportTemplate().put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
        getReportTemplate().put(BOSecurityPositionReportTemplate.AGGREGATION, "Book");
        // getReportTemplate().put(BOSecurityPositionReportTemplate.AGGREGATION_TYPE,
        // "Book");
        getReportTemplate().put(BOPositionReportTemplate.FILTER_ZERO, "false");
        getReportTemplate().put(BOPositionReportTemplate.MOVE, MOVEMENT_BALANCE);

        // BOSecurityPositionReportTemplate
        // getReportTemplate().put(BOPositionReportTemplate.BOOK_LIST, "false");

        // V14 Migration AAP
        ((SantInventoryMissingSecurityQuotesReportTemplate) this._reportTemplate)
                .setBOContext(createReportTemplateContext(getReportTemplate()));

        Vector<String> errorMsgs = new Vector<String>();
        initDates(errorMsgs);

        if (getPricingEnv() != null) {
            PricingEnv relloadedPE = AppUtil.loadPE(getPricingEnv().getName(), getValuationDatetime());
            setPricingEnv(relloadedPE);
        }

        List<InventorySecurityPosition> missingQuotesItems = getMissingQuotesItems(errorMsgs);
        if (!Util.isEmpty(errorMsgs)) {
            throw new Exception("Exception encountered:" + errorMsgs);
        }

        Collections.sort(missingQuotesItems, new SecurityPositionIsinComparator());

        List<ReportRow> rowList = new ArrayList<ReportRow>();
        for (InventorySecurityPosition pos : missingQuotesItems) {
            ReportRow row = new ReportRow(pos,
                    SantInventoryMissingSecurityQuotesReportTemplate.INVENTORY_MISSING_SECURITY);
            row.setProperty(SantInventoryMissingSecurityQuotesReportTemplate.PRICING_ENV, getPricingEnv().getName());
            row.setProperty(SantInventoryMissingSecurityQuotesReportStyle.VALUE_DATE, this._startDate);
            rowList.add(row);
        }

        ReportRow[] rows = rowList.toArray(new ReportRow[rowList.size()]);
        DefaultReportOutput output = new DefaultReportOutput(this);
        output.setRows(rows);
        return output;
    }

    public void initDates(Vector<String> errorMsgs) {
        JDate processEndDate = null;
        processEndDate = getDate(getReportTemplate(), getValuationDatetime().getJDate(TimeZone.getDefault()), TradeReportTemplate.END_DATE,
                TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);

        if (processEndDate == null) {
            errorMsgs.add("Date cannot be empty.");
        }

        this._startDate = processEndDate;
        this._endDate = processEndDate;
    }

    @SuppressWarnings({"rawtypes"})
    private Vector<InventorySecurityPosition> loadPositions(Vector errorMsgs) throws Exception {
        InventoryPositionArray positions = null;

        DefaultReportOutput output = new DefaultReportOutput(this);

        StringBuffer where = new StringBuffer();
        StringBuffer from = new StringBuffer();
        boolean productsSelected = buildWhere(where, from, "",null, null, null);
        if (!productsSelected) {
            // No products are selected, hence no position should be displayed.
            return null;
        }

        positions = load(where.toString(), from.toString(), errorMsgs, null);

        this._positions = trimPositions(positions);
        // for (int i = 0; i < positions.size(); i++) {
        // this._positions.put(positions.getElementAt(i),
        // positions.getElementAt(i));
        // }

        DefaultReportOutput reportOutput = (DefaultReportOutput) buildOutput(output);

        ReportRow[] rows = reportOutput.getRows();
        Vector<InventorySecurityPosition> positionsVect = new Vector<InventorySecurityPosition>();

        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
            if (inventory instanceof InventorySecurityPosition) {
                int securityId = ((InventorySecurityPosition) inventory).getSecurityId();
                Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
                if (product == null) {
                    continue;
                }
                positionsVect.add((InventorySecurityPosition) inventory);
            }
        }

        return positionsVect;
    }

    /**
     * Calculates Global position and also total issued of all securities.
     *
     * @param errorMsgs
     * @throws Exception
     */
    private List<InventorySecurityPosition> getMissingQuotesItems(Vector<String> errorMsgs) throws Exception {
        List<InventorySecurityPosition> finalPositions = new ArrayList<InventorySecurityPosition>();

        Vector<InventorySecurityPosition> positions = loadPositions(errorMsgs);
        if (Util.isEmpty(errorMsgs)) {
            for (InventorySecurityPosition pos : positions) {
                if (acceptedPosition(pos)) {
                    QuoteValue productQuote = getPricingEnv().getQuoteSet().getProductQuote(pos.getProduct(),
                            this._startDate, getPricingEnv().getName());
                    if ((productQuote == null) || (Double.isNaN(productQuote.getClose()))) {
                        finalPositions.add(pos);
                    }
                }
            }
        }

        return finalPositions;

    }

    protected boolean acceptedPosition(Inventory position) {
        if ((position instanceof InventorySecurityPosition) && (position.getTotal() > 0.0)) {
            return true;
        }

        return false;
    }

}