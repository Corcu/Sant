package calypsox.tk.report;

import calypsox.tk.report.concentration.AcceptedPositions;
import calypsox.tk.report.concentration.ConcentrationReportItem;
import calypsox.util.collateral.CollateralUtilities;
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
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryPositionArray;

import java.util.*;

public class SantConcentrationReport extends com.calypso.tk.report.BOSecurityPositionReport {

    public final static String MOVEMENT_BALANCE = "Balance";
    public final static String MOVEMENT_DIRTY_VALUE = "Balance_DirtyValue";
    public final static String MOVEMENT_CLEAN_VALUE = "Balance_CleanValue";
    public final static String GLOBAL_POSITION = "Global Position";
    public final static String TOTAL_ISSUED = "Total Issued";

    protected double globalPosition;
    protected double globalTotalIssued;
    protected Map<String, AcceptedPositions> acceptedPosMap;

    private static final long serialVersionUID = -5905298162259020812L;


    @SuppressWarnings({"rawtypes", "unchecked"})
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

    @SuppressWarnings("unused")
    private ReportOutput load() throws Exception {

        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");

        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_DATE, "Settle");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Internal");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_TYPE, "Theoretical");
        getReportTemplate().put(BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
        getReportTemplate().put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
        getReportTemplate().put(BOSecurityPositionReportTemplate.AGGREGATION, "Book");
        getReportTemplate().put(BOPositionReportTemplate.FILTER_ZERO, "false");

        Vector<String> errorMsgs = new Vector<String>();
        initDates(errorMsgs);

        buildFiltersAndPercentages(errorMsgs);
        if (this.acceptedPosMap.size() == 0) {
            throw new Exception("No Filter criteria specified");
        }
        if (!Util.isEmpty(errorMsgs)) {
            throw new Exception(Util.collectionToString(errorMsgs, "\n"));
        }

        if (getPricingEnv() != null) {
            PricingEnv relloadedPE = AppUtil.loadPE(getPricingEnv().getName(), getValuationDatetime());
            setPricingEnv(relloadedPE);
        }

        calculateGlobalPosition(errorMsgs);
        if (!Util.isEmpty(errorMsgs)) {
            throw new Exception("Exception encountered:" + errorMsgs);
        }

        List<ReportRow> rowList = new ArrayList<ReportRow>();
        String movement = (String) getReportTemplate().get(BOSecurityPositionReportTemplate.MOVE);
        String books = (String) getReportTemplate().get(BOSecurityPositionReportTemplate.BOOK_LIST);

        for (String filter : this.acceptedPosMap.keySet()) {
            AcceptedPositions acceptedPositions = this.acceptedPosMap.get(filter);

            ConcentrationReportItem item = new ConcentrationReportItem();
            item.setBookList(books);
            item.setDate(this._startDate);
            item.setFilterName(acceptedPositions.getFilter().getName());
            item.setMovementType(movement);
            item.setPercentage(acceptedPositions.getPercentage());
            item.setCriteria(acceptedPositions.getCriteria());

            if (acceptedPositions.getPosOrTotalIssued().equals(GLOBAL_POSITION)) {
                double limitedPosValue = acceptedPositions.getLimitedPosValue();
                double calculatedPerc = (limitedPosValue * 100) / this.globalPosition;
                if (calculatedPerc >= acceptedPositions.getPercentage()) {
                    item.setLimitedPosValue(limitedPosValue);
                    item.setGlobalPosValue(this.globalPosition);
                    item.setCalculatedPercentage(calculatedPerc);
                    ReportRow row = new ReportRow(item, SantConcentrationReportTemplate.CONCENTRATION_REPORT_ITEM);
                    rowList.add(row);
                }
            } else if (acceptedPositions.getPosOrTotalIssued()
                    .equals(TOTAL_ISSUED)) {
                double limitedTotalIssued = acceptedPositions.getTotalIssued();
                double calculatedPerc = (limitedTotalIssued * 100) / this.globalTotalIssued;
                if (calculatedPerc >= acceptedPositions.getPercentage()) {
                    item.setGlobalPosValue(this.globalTotalIssued);
                    item.setLimitedPosValue(limitedTotalIssued);
                    item.setCalculatedPercentage(calculatedPerc);
                    ReportRow row = new ReportRow(item, SantConcentrationReportTemplate.CONCENTRATION_REPORT_ITEM);
                    rowList.add(row);
                }
            }
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

      /*  StringBuffer where = new StringBuffer();
        StringBuffer from = new StringBuffer();
        boolean productsSelected = buildWhere(where, from, null, null, null);
        if (!productsSelected) {
            // No products are selected, hence no position should be displayed.
            return null;
        }

        positions = load(where.toString(), from.toString(), errorMsgs, null);*/
        //Positions are stored in this._positions
        ReportOutput reportOutput = super.load(errorMsgs);
       /* //MIG_V14
        this._positions = trimPositions(positions);*/
        //for (int i = 0; i < positions.size(); i++) {
        //	this._positions.put(positions.getElementAt(i), positions.getElementAt(i));
        //}

        ReportRow[] rows = new ReportRow[0];
        if (reportOutput instanceof DefaultReportOutput) {
            rows = ((DefaultReportOutput) reportOutput).getRows();
        }
        Vector<InventorySecurityPosition> positionsVect = new Vector<>();

        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
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

    private void calculateGlobalPosition(Vector<String> errorMsgs) throws Exception {
        this.globalPosition = 0.0;
        this.globalTotalIssued = 0.0;

        String movement = (String) getReportTemplate().get(BOSecurityPositionReportTemplate.MOVE);

        // StringBuffer where = new StringBuffer();
        // StringBuffer from = new StringBuffer();
        // buildWhere(where, from, null);
        // Vector<InventorySecurityPosition> positions = getDSConnection().getRemoteBackOffice()
        // .getLastInventorySecurityPositions(where.toString());

        Vector<InventorySecurityPosition> positions = loadPositions(errorMsgs);
        if (!Util.isEmpty(errorMsgs)) {
            return;
        }

        for (InventorySecurityPosition pos : positions) {
            String ccy = pos.getSettleCurrency();

            double faceValue = 0;
            double totalIssued = 0;
            double positionValue = 0;
            String securityCcy = "";

            if (pos.getProduct() instanceof Bond) {
                Bond bond = (Bond) pos.getProduct();
                faceValue = bond.getFaceValue();
                totalIssued = bond.getTotalIssued();
                securityCcy = bond.getCurrency();
            } else if (pos.getProduct() instanceof Equity) {
                Equity equity = (Equity) pos.getProduct();
                securityCcy = equity.getCurrency();
                totalIssued = equity.getTotalIssued();
                faceValue = equity.getPrincipal();
            }

            if (movement.equals(MOVEMENT_BALANCE)) {
                positionValue = pos.getTotalSecurity() * faceValue;
            } else if (movement.equals(MOVEMENT_DIRTY_VALUE)
                    || movement.equals(MOVEMENT_CLEAN_VALUE)) {
                QuoteValue productQuote = getPricingEnv().getQuoteSet().getProductQuote(pos.getProduct(),
                        this._startDate, getPricingEnv().getName());
                if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                    Double closePrice = productQuote.getClose();
                    positionValue = pos.getTotalSecurity() * faceValue * closePrice;
                } else {
                    throw new Exception("Quote not found for the product id=" + pos.getProduct().getId() + ", ISIN="
                            + pos.getProduct().getSecCode("ISIN"));
                }
            }

            if (!ccy.equals("EUR")) {
                try {
                    positionValue = CollateralUtilities.convertCurrency(ccy, positionValue, "EUR", this._startDate,
                            getPricingEnv());
                } catch (Exception exc) {
                    Log.error(this, exc); //sonar
                    errorMsgs.add(exc.getMessage());
                    return;
                }
            }

            this.globalPosition += positionValue;

            List<String> keys = filterSecPosition(pos);
            if (!Util.isEmpty(keys)) {
                // Total Issued in EUR
                if (!securityCcy.equals("EUR") && (pos.getTotalSecurity() != 0)) {
                    try {
                        totalIssued = CollateralUtilities.convertCurrency(securityCcy, totalIssued, "EUR",
                                this._startDate, getPricingEnv());
                    } catch (Exception exc) {
                        errorMsgs.add(exc.getMessage());
                        Log.error(this, exc); //sonar
                        return;
                    }
                }
                this.globalTotalIssued += totalIssued;
                for (String key : keys) {
                    // String filter = getFilterNameFromKey(key);
                    AcceptedPositions acceptedPositions = this.acceptedPosMap.get(key);
                    acceptedPositions.addPosition(pos);
                    acceptedPositions.addLimitedPosValue(positionValue);
                    acceptedPositions.addTotalIssued(positionValue);
                }
            }
        }

    }

    protected List<String> filterSecPosition(Inventory position) {
        List<String> keys = new ArrayList<String>();
        boolean accecptedPos = super.filterPosition(position);
        if (accecptedPos) {
            if ((position instanceof InventorySecurityPosition) && !Util.isEmpty(this.acceptedPosMap.keySet())) {
                for (String key : this.acceptedPosMap.keySet()) {
                    AcceptedPositions acceptedPositions = this.acceptedPosMap.get(key);
                    StaticDataFilter sdf = acceptedPositions.getFilter();
                    Product product = ((InventorySecurityPosition) position).getProduct();
                    if ((product != null) && sdf.accept(null, product)) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    private void buildFiltersAndPercentages(Vector<String> errorMsgs) {
        this.acceptedPosMap = new HashMap<String, AcceptedPositions>();

        addFilterAndPercentage(this.acceptedPosMap,
                (String) getReportTemplate().get(SantConcentrationReportTemplate.FILTER1), (String) getReportTemplate()
                        .get(SantConcentrationReportTemplate.PERCENTAGE1),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED1),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.CRITERIA1), errorMsgs);
        addFilterAndPercentage(this.acceptedPosMap,
                (String) getReportTemplate().get(SantConcentrationReportTemplate.FILTER2), (String) getReportTemplate()
                        .get(SantConcentrationReportTemplate.PERCENTAGE2),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED2),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.CRITERIA2), errorMsgs);
        addFilterAndPercentage(this.acceptedPosMap,
                (String) getReportTemplate().get(SantConcentrationReportTemplate.FILTER3), (String) getReportTemplate()
                        .get(SantConcentrationReportTemplate.PERCENTAGE3),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED3),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.CRITERIA3), errorMsgs);
        addFilterAndPercentage(this.acceptedPosMap,
                (String) getReportTemplate().get(SantConcentrationReportTemplate.FILTER4), (String) getReportTemplate()
                        .get(SantConcentrationReportTemplate.PERCENTAGE4),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED4),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.CRITERIA4), errorMsgs);
        addFilterAndPercentage(this.acceptedPosMap,
                (String) getReportTemplate().get(SantConcentrationReportTemplate.FILTER5), (String) getReportTemplate()
                        .get(SantConcentrationReportTemplate.PERCENTAGE5),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED5),
                (String) getReportTemplate().get(SantConcentrationReportTemplate.CRITERIA5), errorMsgs);

    }

    private void addFilterAndPercentage(Map<String, AcceptedPositions> filtersMap, String filterName,
                                        String percentage, String posOrTotalIssued, String criteria, Vector<String> errorMsgs) {
        if (Util.isEmpty(filterName) || Util.isEmpty(percentage)) {
            return;
        }
        Double perc = Double.valueOf(percentage);
        StaticDataFilter staticDataFilter = BOCache.getStaticDataFilter(getDSConnection(), filterName);
        if ((staticDataFilter != null) && (perc != null) && !Util.isEmpty(posOrTotalIssued)) {
            if (!hasProductTypeElement(staticDataFilter)) {
                errorMsgs.add("SDFilter " + staticDataFilter.getName()
                        + " is missing a mandatory attribute ProductType.");
            }
            if (Util.isEmpty(criteria)) {
                errorMsgs.add("Criteria is missing for SDFilter " + staticDataFilter.getName() + ".");
            }
            AcceptedPositions accPosition = new AcceptedPositions(staticDataFilter, perc, posOrTotalIssued, criteria);
            filtersMap.put(getKey(filterName, criteria), accPosition);
        }

    }

    private String getKey(String filterName, String criteria) {
        return filterName + "-" + criteria;
    }

    @SuppressWarnings("unused")
    private String getFilterNameFromKey(String key) {
        if (key.indexOf("-") != -1) {
            return key.substring(0, key.indexOf("-"));
        }
        return null;
    }


    private boolean hasProductTypeElement(StaticDataFilter staticDataFilter) {
        Vector<StaticDataFilterElement> elements = staticDataFilter.getElements();
        for (StaticDataFilterElement element : elements) {
            if (element.getName().equals("Product Type")) {
                return true;
            }
        }
        return false;
    }
}