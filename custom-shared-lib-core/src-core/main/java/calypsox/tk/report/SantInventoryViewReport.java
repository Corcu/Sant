/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.refdata.SantCreditRatingStaticDataFilter;
import calypsox.tk.report.inventoryview.property.CreditRatingHelper;
import calypsox.tk.report.inventoryview.property.CreditRatingMethod;
import calypsox.tk.report.inventoryview.property.HaircutData;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.SC;

public class SantInventoryViewReport extends BOSecurityPositionReport {

    private static final long serialVersionUID = -9073174165367091030L;

    protected CreditRatingMethod snpEQV = null;
    protected CreditRatingMethod moodyEQV = null;
    protected CreditRatingMethod fitchEQV = null;
    protected CreditRatingMethod strictlySnp = null;
    protected CreditRatingMethod strictlyMoody = null;
    protected CreditRatingMethod strictlyFitch = null;
    protected CreditRatingMethod strictlySC = null;

    protected List<StaticDataFilter> filtersToExecute = new ArrayList<StaticDataFilter>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector errorMsgs) {

        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");
        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TEMPLATE_NAME, "");
        this.filtersToExecute.clear();

        if (getPricingEnv() != null) {
            PricingEnv relloadedPE = AppUtil.loadPE(getPricingEnv().getName(), getValuationDatetime());
            setPricingEnv(relloadedPE);
        }
        initCreditRatingMethods();
        validateRatings(errorMsgs);
        if (!Util.isEmpty(errorMsgs)) {
            return new DefaultReportOutput(this);
        }

        DefaultReportOutput reportOutput = filterByRatings((DefaultReportOutput) super.load(errorMsgs));

        ReportRow[] rows = reportOutput.getRows();

        // START OA I118 31/10/2013
        if (rows != null) {
            for (int i = 0; i < rows.length; i++) {

                ReportRow row = rows[i];
                Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
                if (inventory instanceof InventorySecurityPosition) {
                    int securityId = ((InventorySecurityPosition) inventory).getSecurityId();
                    Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
                    if (product == null) {
                        continue;
                    }

                    if (getPricingEnv() != null) {
                        QuoteSet quoteSet = getPricingEnv().getQuoteSet();
                        QuoteValue productQuote = quoteSet.getProductQuote(product, getValDate(), getPricingEnv()
                                .getName());

                        // if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) { old
                        if ((productQuote != null)) {

                            Double closePrice = null;
                            Double lastPrice = null;

                            if (!Double.isNaN(productQuote.getClose())) {
                                closePrice = productQuote.getClose();
                                // START OA I117 31/10/2013
                                Double cleanPrice;
                                if (product instanceof Equity) {
                                    // For equities, no need to multiply by 100
                                    cleanPrice = closePrice;
                                } else {
                                    cleanPrice = closePrice * 100;
                                }
                                row.setProperty("CleanPrice", cleanPrice);
                                // END OA I117 31/10/2013
                            }

                            // GSM: 23/08/13. Add last price too - to see the last price from Bloomberg
                            if (!Double.isNaN(productQuote.getLast())) {
                                lastPrice = productQuote.getLast();
                                // GSM: 04/09/13. F.Poza expects to not be multiple by 100
                                row.setProperty("LastPrice", lastPrice); // * 100);
                            }

                            Hashtable positions = (Hashtable) row.getProperty(BOPositionReport.POSITIONS);
                            String s = Util.dateToMString(getValDate());
                            Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions
                                    .get(s);

                            if ((datedPositions == null) || (datedPositions.size() == 0)) {
                                continue;
                            }
                            double total = InventorySecurityPosition.getTotalSecurity(datedPositions, "Balance");

                            // double total = inventory.getTotal();
                            // START OA I117 31/10/2013
                            if ((closePrice != null) && !Double.isNaN(total)) {
                                if (product instanceof Bond) {

                                    // This is used in the report Style to calculate Balance_HC
                                    row.setProperty("Bodn_Closing_Price", closePrice);

                                    Bond bond = (Bond) product;
                                    double cleanPriceValue = total * closePrice * bond.getFaceValue();
                                    row.setProperty("CleanPrice_Value", cleanPriceValue);
                                }
                                if (product instanceof Equity) {
                                    double cleanPriceValue = total * closePrice;
                                    row.setProperty("CleanPrice_Value", cleanPriceValue);
                                }
                            }
                            // END OA I117 31/10/2013

                            // GSM: 23/08/13. Add last price too - to see the last price from Bloomberg
                            if ((lastPrice != null) && !Double.isNaN(total) && (product instanceof Bond)) {

                                // This is used in the report Style to calculate Balance_HC
                                row.setProperty("Bond_Last_Price", lastPrice);

                                final Bond bond = (Bond) product;
                                final double lastPriceValue = total * (lastPrice / 100) * bond.getFaceValue();
                                row.setProperty("LastPrice_Value", lastPriceValue);
                            }

                        }
                    }
                }
            }
        }
        // END OA I118 31/10/2013

        return reportOutput;
    }

    public void validateRatings(Vector<String> errorMsgs) {
        try {
            if ((this.snpEQV != null) && !this.snpEQV.isEmpty()) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(this.snpEQV.getRatingAgency(),
                        this.snpEQV.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.snpEQV.getRatingAgency(), this.snpEQV.getCreditRating()));
                }
            }

            if ((this.moodyEQV != null) && !this.moodyEQV.isEmpty()) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(
                        this.moodyEQV.getRatingAgency(), this.moodyEQV.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.moodyEQV.getRatingAgency(), this.moodyEQV.getCreditRating()));
                }
            }

            if ((this.fitchEQV != null) && !this.fitchEQV.isEmpty()) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(
                        this.fitchEQV.getRatingAgency(), this.fitchEQV.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.fitchEQV.getRatingAgency(), this.fitchEQV.getCreditRating()));
                }
            }

            if ((this.strictlySnp != null) && !this.strictlySnp.isEmpty() && !this.strictlySnp.getSign().equals("=")
                    && !this.strictlySnp.getSign().equals(CreditRatingHelper.EXISTS)) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(
                        this.strictlySnp.getRatingAgency(), this.strictlySnp.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.strictlySnp.getRatingAgency(), this.strictlySnp.getCreditRating()));
                }
            }

            if ((this.strictlyMoody != null) && !this.strictlyMoody.isEmpty()
                    && !this.strictlyMoody.getSign().equals("=")
                    && !this.strictlyMoody.getSign().equals(CreditRatingHelper.EXISTS)) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(
                        this.strictlyMoody.getRatingAgency(), this.strictlyMoody.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.strictlyMoody.getRatingAgency(), this.strictlyMoody.getCreditRating()));
                }
            }

            if ((this.strictlyFitch != null) && !this.strictlyFitch.isEmpty()
                    && !this.strictlyFitch.getSign().equals("=")
                    && !this.strictlyFitch.getSign().equals(CreditRatingHelper.EXISTS)) {
                int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(
                        this.strictlyFitch.getRatingAgency(), this.strictlyFitch.getCreditRating());
                if (globalPriority == -1) {
                    errorMsgs.add(String.format("%s Rating value %s is not configured in Global Rating Matrix",
                            this.strictlyFitch.getRatingAgency(), this.strictlyFitch.getCreditRating()));
                }
            }

        } catch (Exception e) {
            errorMsgs.add(e.getMessage());
            Log.error(this, e); //sonar
        }

    }

    private void initCreditRatingMethods() {
        String ratingMethodStr = "";
        ratingMethodStr = (String) getReportTemplate().get(SantInventoryViewReportTemplate.RATING_METHOD_SNP_EQV);
        this.snpEQV = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate().get(SantInventoryViewReportTemplate.RATING_METHOD_MOODY_EQV);
        this.moodyEQV = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate().get(SantInventoryViewReportTemplate.RATING_METHOD_FITCH_EQV);
        this.fitchEQV = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate().get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SNP);
        this.strictlySnp = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate()
                .get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_MOODY);
        this.strictlyMoody = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate()
                .get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_FITCH);
        this.strictlyFitch = CreditRatingMethod.valueOf(ratingMethodStr);

        ratingMethodStr = (String) getReportTemplate().get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SC);
        this.strictlySC = CreditRatingMethod.valueOf(ratingMethodStr);
    }

    public DefaultReportOutput filterByRatings(DefaultReportOutput reportOutput) {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        if (reportOutput == null) {
            return output;
        }

        ReportRow[] rows = reportOutput.getRows();
        final ArrayList<ReportRow> filteredRows = new ArrayList<ReportRow>();

        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
            if (inventory instanceof InventorySecurityPosition) {

                int securityId = ((InventorySecurityPosition) inventory).getSecurityId();
                Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);
                if ((product == null) || !(product instanceof Security)) {
                    continue;
                }

                // get StockLendigRate data from product and add to row as properties
                // saveOnRowProductStockLendingData(product, row);

                try {
                    boolean accepted = true;

                    if (accepted && (this.snpEQV != null) && !this.snpEQV.isEmpty() && !accept(product, this.snpEQV)) {
                        accepted = false;
                    }
                    if (accepted && (this.moodyEQV != null) && !this.moodyEQV.isEmpty()
                            && !accept(product, this.moodyEQV)) {
                        accepted = false;
                    }
                    if (accepted && (this.fitchEQV != null) && !this.fitchEQV.isEmpty()
                            && !accept(product, this.fitchEQV)) {
                        accepted = false;
                    }

                    if (accepted && (this.strictlySnp != null) && !this.strictlySnp.isEmpty()
                            && !accept(product, this.strictlySnp)) {
                        accepted = false;
                    }
                    if (accepted && (this.strictlyMoody != null) && !this.strictlyMoody.isEmpty()
                            && !accept(product, this.strictlyMoody)) {
                        accepted = false;
                    }
                    if (accepted && (this.strictlyFitch != null) && !this.strictlyFitch.isEmpty()
                            && !accept(product, this.strictlyFitch)) {
                        accepted = false;
                    }
                    if (accepted && (this.strictlySC != null) && !this.strictlySC.isEmpty()
                            && !accept(product, this.strictlySC)) {
                        accepted = false;
                    }

                    if (accepted) {
                        filteredRows.add(row);
                    }

                } catch (Exception e) {
                    Log.error(this, e);
                    // e.printStackTrace();
                }

            }
        }

        output.setRows(filteredRows.toArray(new ReportRow[filteredRows.size()]));
        return output;
    }

    public boolean accept(Product security, List<CreditRatingMethod> ratingMethodList) throws Exception {
        boolean accepted = true;
        for (CreditRatingMethod ratingMethod : ratingMethodList) {
            if (!ratingMethod.isEmpty() && !accept(security, ratingMethod)) {
                return false;
            }
        }
        return accepted;
    }

    public boolean accept(Product security, CreditRatingMethod ratingMethod) throws Exception {

        HashSet<Integer> priorityListToLookFor = new HashSet<>();
        int productId = security.getId();
        int issuerId = ((Security) security).getIssuerId();

        int priorityToLookFor = -1;

        if (ratingMethod.getHighestOrLowest().equals("HIGHEST")) {
            if (ratingMethod.getIssueOrIssuer().equals("Issue")) {
                priorityToLookFor = SantCreditRatingStaticDataFilter.getHighestProductRatingPriority(productId);
            } else {
                priorityToLookFor = SantCreditRatingStaticDataFilter.getHighestIssuerRatingPriority(issuerId);
            }
            if (priorityToLookFor == -1) {
                return false;
            }
            priorityListToLookFor.add(priorityToLookFor);
        } else if (ratingMethod.getHighestOrLowest().equals("LOWEST")) {
            if (ratingMethod.getIssueOrIssuer().equals("Issue")) {
                priorityToLookFor = SantCreditRatingStaticDataFilter.getLowestProductRatingPriority(productId);
            } else {
                priorityToLookFor = SantCreditRatingStaticDataFilter.getLowestIssuerRatingPriority(issuerId);
            }
            if (priorityToLookFor == -1) {
                return false;
            }
            priorityListToLookFor.add(priorityToLookFor);
        } else {
            // When no Lowest/Highest selected, we have to check all ratings
            if (ratingMethod.getIssueOrIssuer().equals("Issue")) {
                Vector<ProductCreditRating> productRatings = SantCreditRatingStaticDataFilter
                        .getProductRatings(productId);
                if (CreditRatingHelper.EXISTS.equals(ratingMethod.getSign())) {
                    String ratingValue = SantCreditRatingStaticDataFilter.getProductCreditRatingValue(productRatings,
                            ratingMethod.getRatingAgency());
                    if (!Util.isEmpty(ratingValue)) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (ratingMethod.isStrict()) { // Strictly a particular Agency
                    String ratingValue = SantCreditRatingStaticDataFilter.getProductCreditRatingValue(productRatings,
                            ratingMethod.getRatingAgency());

                    // When the sign is '=' we can compare the value directly.
                    if ("=".equals(ratingMethod.getSign()) && ratingMethod.getCreditRating().equals(ratingValue)) {
                        return true;
                    }

                    // In case of SC we compare the values directly. If it is not a number then we reject.
                    if (ratingMethod.getRatingAgency().equals(SC)) {
                        try {
                            double currentRating = Double.parseDouble(ratingValue);
                            double screenValue = Double.parseDouble(ratingMethod.getCreditRating());
                            if (ratingMethod.getSign().equals(">=") && (currentRating >= screenValue)) {
                                return true;
                            } else if (ratingMethod.getSign().equals("<=") && (currentRating <= screenValue)) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception exc) {
                            Log.warn(this, exc); //sonar
                            return false;
                        }
                    }

                    priorityToLookFor = SantCreditRatingStaticDataFilter.getGlobalPriority(
                            ratingMethod.getRatingAgency(), ratingValue);
                    if (priorityToLookFor == -1) {
                        return false;
                    }
                    priorityListToLookFor.add(priorityToLookFor);
                } else {
                    for (ProductCreditRating rating : productRatings) {
                        int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(rating.getAgencyName(),
                                rating.getRatingValue());
                        if (globalPriority != -1) {
                            priorityListToLookFor.add(globalPriority);
                        }
                    }
                }
            } else { // Issuer
                Vector<CreditRating> issuerRatings = SantCreditRatingStaticDataFilter.getIssuerRatings(issuerId);

                if (CreditRatingHelper.EXISTS.equals(ratingMethod.getSign())) {
                    String ratingValue = SantCreditRatingStaticDataFilter.getIssuerRatingValue(issuerRatings,
                            ratingMethod.getRatingAgency());
                    if (!Util.isEmpty(ratingValue)) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (ratingMethod.isStrict()) { // Strictly a particular Agency
                    String ratingValue = SantCreditRatingStaticDataFilter.getIssuerRatingValue(issuerRatings,
                            ratingMethod.getRatingAgency());
                    // When the sign is '=' we can compare the value directly.
                    if ("=".equals(ratingMethod.getSign()) && ratingMethod.getCreditRating().equals(ratingValue)) {
                        return true;
                    }

                    // In case of SC we compare the values directly. If it is not a number then we reject.
                    if (ratingMethod.getRatingAgency().equals(SC)) {
                        try {
                            double currentRating = Double.parseDouble(ratingValue);
                            double screenValue = Double.parseDouble(ratingMethod.getCreditRating());
                            if (ratingMethod.getSign().equals(">=") && (currentRating >= screenValue)) {
                                return true;
                            } else if (ratingMethod.getSign().equals("<=") && (currentRating <= screenValue)) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception exc) {
                            Log.warn(this, exc); //sonar
                            return false;
                        }
                    }

                    priorityToLookFor = SantCreditRatingStaticDataFilter.getGlobalPriority(
                            ratingMethod.getRatingAgency(), ratingValue);
                    if (priorityToLookFor == -1) {
                        return false;
                    }
                    priorityListToLookFor.add(priorityToLookFor);
                } else {
                    for (CreditRating rating : issuerRatings) {
                        int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(rating.getAgencyName(),
                                rating.getRatingValue());
                        if (globalPriority != -1) {
                            priorityListToLookFor.add(globalPriority);
                        }
                    }
                }
            }

        }

        int globalPriority = SantCreditRatingStaticDataFilter.getGlobalPriority(ratingMethod.getRatingAgency(),
                ratingMethod.getCreditRating());
        for (Integer priority : priorityListToLookFor) {
            if (ratingMethod.getSign().equals("<=")) {
                if (priority >= globalPriority) {
                    return true;
                }
            } else if (ratingMethod.getSign().equals(">=")) {
                if (priority <= globalPriority) {
                    return true;
                }
            } else if (ratingMethod.getSign().equals("=")) {
                if (priority == globalPriority) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected boolean buildWhere(StringBuffer where, StringBuffer from, String inventoryTable, Set<Integer> booksId,Set<Integer> configIds, List<CalypsoBindVariable> bindVariables) throws Exception {

        List<Integer> contracts = (List<Integer>) getReportTemplate().get(SantInventoryViewReportTemplate.CONTRACTS);

        List<HaircutData> haircuts = (List<HaircutData>) getReportTemplate().get(
                SantInventoryViewReportTemplate.HAIRCUTS);

        List<String> bondIssuers = new ArrayList<>();
        if (!Util.isEmpty(contracts)) {
            List<String> sfds = new ArrayList<>();

            List<StaticDataFilter> realSDFs = new ArrayList<>();
            for (Integer mccID : contracts) {
                CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccID);
                sfds.addAll(mcc.getEligibilityFilterNames());
            }

            if (Util.isEmpty(sfds)) {
                return false;
            }

            for (String sdf : sfds) {
                StaticDataFilter realSDF = BOCache.getStaticDataFilter(DSConnection.getDefault(), sdf);
                realSDFs.add(realSDF);
                Vector<String> bondIssuer = getBondIssuer(realSDF);
                if (Util.isEmpty(bondIssuer)) {
                    this.filtersToExecute.add(realSDF);
                } else {
                    bondIssuers.addAll(bondIssuer);
                }
            }

            if (Util.isEmpty(this.filtersToExecute) && Util.isEmpty(bondIssuers)) {
                bondIssuers.add("dummy");
            } else if (!Util.isEmpty(this.filtersToExecute)) {
                bondIssuers.clear();
                this.filtersToExecute.addAll(realSDFs);
            }
        }

        String currencies = (String) getReportTemplate().get(SantInventoryViewReportTemplate.CURRENCIES);
        String accBooks = (String) getReportTemplate().get(SantInventoryViewReportTemplate.ACC_BOOK_LIST);

        if (Util.isEmpty(bondIssuers)) {

            boolean isSec = super.buildWhere(where, from, inventoryTable,null, null, null);
            buildHaircutQuery(where, from, haircuts);
            if (!Util.isEmpty(currencies)) {
                buildCurrenciesQuery(where, from, currencies);
            }
            if (!Util.isEmpty(accBooks)) {
                buildAccBookQuery(where, from, accBooks);
            }

            return isSec;
            // from.append(", ");
            // from.append("product_bond, legal_entity ");
            // if (where.length() > 0) {
            // where.append(" AND ");
            // }
            // where.append(" product_bond.product_id= inv_secposition.security_id ");
            // where.append(" AND ");
            // where.append(" product_bond.issuer_le_id= legal_entity.legal_entity_id ");
            // where.append(" AND ");
            // where.append(" legal_entity.short_name in(" + Util.collectionToString(bondIssuers) + ")");
        }
        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "Bond");
        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TEMPLATE_NAME, "Ctr_Eligible_Secs");
        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TEMPLATE_PRIVATE, "true");
        getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TEMPLATE_USER, getReportTemplate().getUser());

        // build a temporary security template and set it to the current BOSecurityPosition template
        // This template will be removed after the load since it can not be amended before products load.
        ReportTemplateName ctrEligibleSecs = new ReportTemplateName("Ctr_Eligible_Secs");
        ctrEligibleSecs.setIsPrivate(false);
        ReportTemplate ctrEligibleSecsTempalte = BOCache.getReportTemplate(DSConnection.getDefault(), "Bond",
                ctrEligibleSecs);

        if (ctrEligibleSecsTempalte == null) {
            ctrEligibleSecsTempalte = new BondReportTemplate();
            ctrEligibleSecsTempalte.setTemplateName("Ctr_Eligible_Secs");
        }
        // modify the
        if (ctrEligibleSecsTempalte != null) {

            BondReportTemplate clonedCtrEligibleSecsTempalte = (BondReportTemplate) ctrEligibleSecsTempalte.clone();
            clonedCtrEligibleSecsTempalte.setId(0);
            clonedCtrEligibleSecsTempalte.setIsPrivate(true);
            clonedCtrEligibleSecsTempalte.setUser(getReportTemplate().getUser());
            // populate the template depending on the report criteria
            clonedCtrEligibleSecsTempalte.put(BondReportTemplate.ISSUER, Util.collectionToString(bondIssuers));
            // delete the old private template since there might be a cache pb
            ReportTemplateName ctrEligibleSecsPrivate = new ReportTemplateName("Ctr_Eligible_Secs");
            ctrEligibleSecsPrivate.setIsPrivate(true);
            ReportTemplate ctrEligibleSecsTempaltePrivate = BOCache.getReportTemplate(DSConnection.getDefault(),
                    "Bond", ctrEligibleSecsPrivate);
            if (ctrEligibleSecsTempaltePrivate != null) {
                ReportTemplate t = BOCache.getReportTemplate(this.getDSConnection(),
                        ctrEligibleSecsTempaltePrivate.getId());
                if (t != null) {
                    DSConnection.getDefault().getRemoteReferenceData().removeReportTemplate(t);
                }
            }

            DSConnection.getDefault().getRemoteReferenceData().save(clonedCtrEligibleSecsTempalte);
            getReportTemplate().put(SecurityTemplateHelper.SECURITY_REPORT_TEMPLATE_NAME,
                    clonedCtrEligibleSecsTempalte.getTemplateName());
        }

        boolean isSec = super.buildWhere(where, from, inventoryTable, null,null, null);
        buildHaircutQuery(where, from, haircuts);
        if (!Util.isEmpty(currencies)) {
            buildCurrenciesQuery(where, from, currencies);
        }
        if (!Util.isEmpty(accBooks)) {
            buildAccBookQuery(where, from, accBooks);
        }
        return isSec;

    }

    /**
     * This is to improve performance. If it doesn't find correct criteria it won't use this method and runs normally.
     * Criteria required in SD Filter are... Product Type=Bond & Security Issuer exists as an SD Filter element
     * <p>
     * This method will return the list of issuers used to define the eligible securities for a contract (used in SDF).
     * For the moment, all SDF are using PRODUCT_TYPE=Bond and ISSUER=XXXX. This will list of issuers will be used to
     * retrieve a list of securities before loading the position. If the SDF eligible securities definition changes,
     * either this method have to be adapted or, this optimization will be useless and the position will be loaded first
     * then the SDF will be applied on each position's securiy.
     *
     * @param realSDF
     * @return
     */
    @SuppressWarnings("unchecked")
    private Vector<String> getBondIssuer(StaticDataFilter realSDF) {

        Vector<StaticDataFilterElement> elements = realSDF.getElements();
        boolean isOnlyBond = false;
        Vector<String> issuers = null;
        if (!Util.isEmpty(elements) && (elements.size() == 2)) {
            for (StaticDataFilterElement sdfElement : elements) {
                if (StaticDataFilterElement.PRODUCT_TYPE.equals(sdfElement.getName())) {
                    if (!Util.isEmpty(sdfElement.getValues()) && (sdfElement.getValues().size() == 1)
                            && sdfElement.getValues().contains("Bond")) {
                        isOnlyBond = true;
                    }
                } else if (StaticDataFilterElement.SEC_ISSUER.equals(sdfElement.getName())) {
                    issuers = sdfElement.getValues();
                }
            }
            if (isOnlyBond) {
                return issuers;
            }

        }
        return null;
    }

    @Override
    protected boolean filterPosition(Inventory position) {
        boolean accecptedPos = super.filterPosition(position);
        boolean filterAccepted = true;
        // START OA I118 31/10/2013
        if (position instanceof InventorySecurityPosition) {
            Product product = ((InventorySecurityPosition) position).getProduct();
            if (product instanceof Bond) {
                JDate maturityDate = ((Bond) product).getMaturityDate();
                if (maturityDate != null) {
                    if (!maturityDate.gte(JDate.getNow())) {
                        return false;
                    }
                }
            }
            if (!Util.isEmpty(this.filtersToExecute)) {
                filterAccepted = false;
                for (StaticDataFilter sdf : this.filtersToExecute) {
                    if ((product != null) && sdf.accept(null, product)) {
                        filterAccepted = true;
                        break;
                    }
                }
            }
        }
        // END OA I118 31/10/2013
        return accecptedPos && filterAccepted;
    }

    private void buildCurrenciesQuery(StringBuffer where, StringBuffer from, String currencies) {

        if (!Util.isEmpty(currencies)) {
            if (from.length() > 0) {
                from.append(", ");
            }

            from.append(" product_desc ");

            Vector<String> ccyVect = Util.string2Vector(currencies);
            if (where.length() > 0) {
                where.append(" and ");
            }

            where.append(" product_desc.product_id=inv_secposition.security_id and product_desc.currency in "
                    + Util.collectionToSQLString(ccyVect));

        }
    }

    private void buildAccBookQuery(StringBuffer where, StringBuffer from, String accBookNames) {

        if (!Util.isEmpty(accBookNames)) {
            if (from.length() > 0) {
                from.append(", ");
            }

            if (from.indexOf("book") == -1) {
                from.append(" book, ");
                if (where.length() > 0) {
                    where.append(" and ");
                }
                where.append(" book.book_id=inv_secposition.book_id ");
            }

            from.append(" acc_book ");

            Vector<String> accBookNamesVect = Util.string2Vector(accBookNames);
            if (where.length() > 0) {
                where.append(" and ");
            }

            where.append(" acc_book.acc_book_id=book.acc_book_id and acc_book.acc_book_name in "
                    + Util.collectionToSQLString(accBookNamesVect));

        }
    }

    private void buildHaircutQuery(StringBuffer where, StringBuffer from, List<HaircutData> haircuts) {

        if (!Util.isEmpty(haircuts)) {
            if (from.length() > 0) {
                from.append(", bond_custom_data ");
            } else {
                from.append(" bond_custom_data ");
            }

            StringBuilder haircutQuery = new StringBuilder(" bond_custom_data.product_id=inv_secposition.security_id ");
            for (HaircutData h : haircuts) {
                if ("ECB".equals(h.getEntity())) {
                    haircutQuery.append(" AND  haircut_ecb ");
                    if (h.getMin() != null) {
                        haircutQuery.append(" >= " + Util.numberToString(h.getMin(), Locale.UK));
                        if (h.getMax() != null) {
                            haircutQuery.append(" AND ");
                        }
                    }
                    if (h.getMax() != null) {
                        haircutQuery.append(" haircut_ecb <= " + Util.numberToString(h.getMax(), Locale.UK));
                    }

                } else if ("Swiss".equals(h.getEntity())) {
                    haircutQuery.append(" AND  haircut_swiss ");
                    if (h.getMin() != null) {
                        haircutQuery.append(" >= " + Util.numberToString(h.getMin(), Locale.UK));
                        if (h.getMax() != null) {
                            haircutQuery.append(" AND ");
                        }
                    }
                    if (h.getMax() != null) {
                        haircutQuery.append(" haircut_swiss <= " + Util.numberToString(h.getMax(), Locale.UK));
                    }

                } else if ("FED".equals(h.getEntity())) {
                    haircutQuery.append(" AND  haircut_fed ");
                    if (h.getMin() != null) {
                        haircutQuery.append(" >= " + Util.numberToString(h.getMin(), Locale.UK));
                        if (h.getMax() != null) {
                            haircutQuery.append(" AND ");
                        }

                    }
                    if (h.getMax() != null) {
                        haircutQuery.append(" haircut_fed <= " + Util.numberToString(h.getMax(), Locale.UK));
                    }

                } else if ("BOE".equals(h.getEntity())) {
                    haircutQuery.append(" AND  haircut_boe ");
                    if (h.getMin() != null) {
                        haircutQuery.append(" >= " + Util.numberToString(h.getMin(), Locale.UK));
                        if (h.getMax() != null) {
                            haircutQuery.append(" AND ");
                        }

                    }
                    if (h.getMax() != null) {
                        haircutQuery.append(" haircut_boe <= " + Util.numberToString(h.getMax(), Locale.UK));
                    }

                }

                if (where.length() > 0) {
                    where.append(" AND ");
                }
                where.append(haircutQuery.toString());

            }

        }
    }

    // public void saveOnRowProductStockLendingData(Product product, ReportRow row) {
    // if (product instanceof Bond) {
    // BondCustomData bcd = (BondCustomData) product.getCustomData();
    // if (bcd != null) {
    // row.setProperty(STOCK_LENDING_RATE, bcd.getFee());
    // row.setProperty(EXP_DATE_TYPE, bcd.getExpired_date_type());
    // row.setProperty(EXP_DATE, bcd.getExpired_date());
    // row.setProperty(ACTIVE_AVAILABLE_QTY, bcd.getActive_available_qty());
    // row.setProperty(QTY_ON_LOAN, bcd.getQty_on_loan());
    // }
    // }
    // if (product instanceof Equity) {
    // EquityCustomData ecd = (EquityCustomData) product.getCustomData();
    // if (ecd != null) {
    // row.setProperty(STOCK_LENDING_RATE, ecd.getFee());
    // row.setProperty(EXP_DATE_TYPE, ecd.getExpired_date_type());
    // row.setProperty(EXP_DATE, ecd.getExpired_date());
    // row.setProperty(ACTIVE_AVAILABLE_QTY, ecd.getActive_available_qty());
    // row.setProperty(QTY_ON_LOAN, ecd.getQty_on_loan());
    // }
    // }
    // }

    @Override
    public boolean getAllowPricingEnv() {
        return true;
    }

//	@Override
//	public InventoryPositionArray filterPositions(InventoryPositionArray positions) {
//
//		positions = super.filterPositions(positions);
//
//		Rate rate = (Rate) getReportTemplate().get(SantThirdPartyInventoryViewReportTemplate.RATE);
//
//		if ((positions != null) && !positions.isEmpty()) {
//			if ((rate != null) && (!Util.isEmpty(rate.toString()))) {
//
//				InventoryPositionArray toReturn = new InventorySecurityPositionArray();
//
//				for (Object pos : positions.toVector()) {
//					InventorySecurityPosition secPos = (InventorySecurityPosition) pos;
//
//					int securityId = secPos.getSecurityId();
//					InventorySecurityPositionArray secPositions = new InventorySecurityPositionArray();
//					secPositions.add(secPos);
//
//					Product product = getProduct(securityId);
//
//					// START OA 29/01/14
//					// Some positions can exist in the system, even if the concerned product has been deleted.
//					if (product == null) {
//						continue;
//					}
//					// END OA 29/01/14
//
//					if ((rate != null) && (!Util.isEmpty(rate.toString()))) {
//						// when the user filters on rates, if custom data are not defined, the position is excluded
//						if (product.getCustomData() == null) {
//							continue;
//						}
//						if (product instanceof Bond) {
//							BondCustomData customData = ((BondCustomData) product.getCustomData());
//							if (rate.getSign().equals("<=")) {
//								if (customData.getFee() > rate.getRate().doubleValue()) {
//									continue;
//								}
//							}
//							if (rate.getSign().equals(">=")) {
//								if (customData.getFee() < rate.getRate().doubleValue()) {
//									continue;
//								}
//							}
//						}
//						if (product instanceof Equity) {
//							EquityCustomData customData = ((EquityCustomData) product.getCustomData());
//							if (rate.getSign().equals("<=")) {
//								if (customData.getFee() > rate.getRate().doubleValue()) {
//									continue;
//								}
//							}
//							if (rate.getSign().equals(">=")) {
//								if (customData.getFee() < rate.getRate().doubleValue()) {
//									continue;
//								}
//							}
//						}
//					}
//					toReturn.addElement(secPos);
//					// END OA 18/11/2013
//				}
//
//				return toReturn;
//			} else {
//				return positions;
//			}
//		}
//
//		else {
//			return positions;
//		}
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public ReportOutput buildOutput(DefaultReportOutput output) {
//		InventoryPositionArray positions = getPositions();
//
//		String ss = (String) this._reportTemplate.get(BOPositionReportTemplate.MOVE);
//		String moveType = ss;
//		Vector moveTypeList = Util.string2Vector(ss);
//
//		boolean hasBalance = false;
//		for (int k = 0; k < moveTypeList.size(); k++) {
//			String typeM = (String) moveTypeList.elementAt(k);
//			if (typeM.indexOf("Balance") >= 0) {
//				hasBalance = true;
//			}
//			if (typeM.equals("Both") || typeM.equals("Balance & Movements")) {
//				if (moveTypeList.indexOf("Balance") < 0) {
//					moveTypeList.addElement("Balance");
//				}
//				if (moveTypeList.indexOf("Movements") < 0) {
//					moveTypeList.addElement("Movements");
//				}
//				moveTypeList.removeElementAt(k);
//				k--;
//				continue;
//			}
//		}
//
//		@SuppressWarnings("unused")
//		Hashtable parentBalances = new Hashtable();
//		String bb = (String) this._reportTemplate.get(BOPositionReportTemplate.EXPLODE_POSITION);
//		boolean explodePosition = Util.isTrue(bb, false);
//
//		Vector sortedMoves = new Vector();
//		Vector sortedBalances = new Vector();
//		for (int k = 0; k < moveTypeList.size(); k++) {
//			String typeM = (String) moveTypeList.elementAt(k);
//
//			if (explodePosition && InventorySecurityPosition.isComposite(typeM)) {
//				Vector moveTypes = InventorySecurityPosition.getUnderBalanceTypes(typeM);
//				if (Util.isEmpty(moveTypes)) {
//					sortedMoves.addElement(typeM);
//				} else {
//					for (int kk = 0; kk < moveTypes.size(); kk++) {
//						String ss1 = (String) moveTypes.elementAt(kk);
//						sortedMoves.addElement(InventorySecurityPosition.getCleanName(ss1));
//					}
//
//				}
//			} else {
//				sortedMoves.addElement(typeM);
//			}
//			sortedBalances.addElement(typeM);
//		}
//
//		positions = filterPositions(positions);
//		Hashtable aggregationMap = buildAggregation(positions);
//
//		String s = (String) this._reportTemplate.get(BOPositionReportTemplate.FILTER_ZERO);
//		boolean filterZeroPosition = Util.isTrue(s, false);
//
//		s = (String) this._reportTemplate.get(BOPositionReportTemplate.SHORTS);
//		boolean showShorts = Util.isTrue(s, false);
//
//		s = (String) this._reportTemplate.get(BOPositionReportTemplate.OFFSETTING_POSITION);
//		boolean showOffsettingPosition = Util.isTrue(s, false);
//
//		JDate startDate = getPositionStartDate().addDays(1);
//
//		s = (String) this._reportTemplate.get(BOPositionReportTemplate.POSITION_TYPE);
//		Vector vvv = null;
//		if (!Util.isEmpty(s)) {
//			vvv = Util.string2Vector(s);
//			for (int k = 0; k < vvv.size(); k++) {
//				s = (String) vvv.elementAt(k);
//				s = s.toUpperCase();
//				vvv.setElementAt(s, k);
//			}
//		}
//
//		Hashtable longPositions = new Hashtable();
//		Hashtable shortPositions = new Hashtable();
//		if (showOffsettingPosition) {
//			Iterator iter = aggregationMap.keySet().iterator();
//			while (iter.hasNext()) {
//				Hashtable h = (Hashtable) aggregationMap.get(iter.next());
//				if ((h == null) || (h.size() == 0)) {
//					continue;
//				}
//
//				// If the position is long, put it
//				// in the long position queue
//				if (isLongPositions(h, moveType, startDate,ROUNDING_DIGITS)) {
//					Inventory position = getInventoryPosition(h);
//					Vector vv = (Vector) longPositions.get(position.getUnderlyerKey());
//					if (vv == null) {
//						vv = new Vector();
//						longPositions.put(position.getUnderlyerKey(), vv);
//					}
//					vv.addElement(position);
//
//				}
//				if (isShortPositions(h, moveType, startDate,ROUNDING_DIGITS)) {
//					// The position is short at some point put in the long position
//					Inventory position = getInventoryPosition(h);
//					Vector vv = (Vector) shortPositions.get(position.getUnderlyerKey());
//					if (vv == null) {
//						vv = new Vector();
//						shortPositions.put(position.getUnderlyerKey(), vv);
//					}
//					vv.addElement(position);
//
//				}
//
//			}
//		}
//
//		Vector rowVector = new Vector();
//		Iterator iter = aggregationMap.keySet().iterator();
//		while (iter.hasNext()) {
//			Hashtable h = (Hashtable) aggregationMap.get(iter.next());
//			if ((h == null) || (h.size() == 0)) {
//				continue;
//			}
//
//			/*
//			 * if (filterZeroPosition) { if (filterZeroPositions(h,moveType)) continue; }
//			 */
//			if (showOffsettingPosition) {
//				Inventory positionm = getInventoryPosition(h);
//				// If the Position is short, we need to display it
//				// only if there is a corresponding long position
//				if (isShortPositions(h, moveType, startDate,ROUNDING_DIGITS)) {
//					if (longPositions.get(positionm.getUnderlyerKey()) == null) {
//						continue;
//					}
//					Vector ttv = (Vector) shortPositions.get(positionm.getUnderlyerKey());
//					if (ttv.size() == 1) {
//						Vector vv = (Vector) longPositions.get(positionm.getUnderlyerKey());
//						if (vv.size() == 1) {
//							Inventory tt = (Inventory) vv.elementAt(0);
//							if (tt.getUniqueKey(false).equals(positionm.getUniqueKey(false))) {
//								continue;
//							}
//						}
//					}
//
//				}
//				// If the position is long
//				// we should only display it if there is a position which is shirt
//				else if (isLongPositions(h, moveType, startDate,ROUNDING_DIGITS)) {
//					if (shortPositions.get(positionm.getUnderlyerKey()) == null) {
//						continue;
//					}
//					Vector ttv = (Vector) longPositions.get(positionm.getUnderlyerKey());
//					if (ttv.size() == 1) {
//						Vector vv = (Vector) shortPositions.get(positionm.getUnderlyerKey());
//						if (vv.size() == 1) {
//							Inventory tt = (Inventory) vv.elementAt(0);
//							if (tt.getUniqueKey(false).equals(positionm.getUniqueKey(false))) {
//								continue;
//							}
//						}
//					}
//				}
//			} else if (showShorts) {
//				if (filterShortPositions(h, moveType, startDate,ROUNDING_DIGITS)) {
//					continue;
//				}
//			}
//
//			Inventory position = getInventoryPosition(h);
//
//			for (int k = 0; k < moveTypeList.size(); k++) {
//				String parentType = (String) moveTypeList.elementAt(k);
//				Vector moveTypes = new Vector();
//
//				if (position instanceof InventorySecurityPosition) {
//
//					if (explodePosition && InventorySecurityPosition.isComposite(parentType)) {
//						moveTypes = InventorySecurityPosition.getUnderBalanceTypes(parentType);
//						if (Util.isEmpty(moveTypes)) {
//							moveTypes.addElement(parentType);
//						}
//					} else {
//						moveTypes.addElement(parentType);
//					}
//
//				} else {
//					moveTypes.addElement(parentType);
//				}
//				for (int l = 0; l < moveTypes.size(); l++) {
//
//					String mType = (String) moveTypes.elementAt(l);
//
//					if (position instanceof InventoryCashPosition) {
//						if (!validCashType(mType)) {
//							continue;
//						}
//					} else {
//						if (explodePosition && !InventorySecurityPosition.isUnderPosition(mType)) {
//							continue;
//						}
//					}
//
//					if (filterZeroPosition) {
//						if (filterZeroPositions(h, InventorySecurityPosition.getCleanName(mType), startDate,ROUNDING_DIGITS)) {
//							continue;
//						}
//					}
//
//					if (!postFilter(h, position.getPositionDate())) {
//						continue;
//					}
//
//					ReportRow row = new ReportRow(position, ReportRow.INVENTORY);
//					row.setProperty(BOPositionReportTemplate.MOVE, InventorySecurityPosition.getCleanName(mType));
//					row.setProperty(POSITIONS, h);
//
//					row.setProperty(BOPositionReportStyle.PARENT_BALANCE, parentType);
//
//					if (hasBalance && (mType.indexOf("Movements") >= 0)) {
//						row.setProperty(ReportRow.IGNORE_TOTALS, "true");
//					}
//
//					// if (!InventorySecurityPosition.isInclusiveBalance(parentType, mType))
//					// row.setProperty(BOPositionReportStyle.BALANCE_SIGN,NEG_SIGN);
//
//					row.setProperty(BOPositionReportTemplate.POSITION_VALUE,
//							this._reportTemplate.get(BOPositionReportTemplate.POSITION_VALUE));
//					row.setProperty(BOPositionReportTemplate.START_DATE, getStartDate());
//					row.setProperty(BOPositionReportTemplate.END_DATE, getEndDate());
//					row.setProperty(BOPositionReportTemplate.AGGREGATION,
//							this._reportTemplate.get(BOPositionReportTemplate.AGGREGATION));
//					row.setProperty(BOPositionReportTemplate.CLOSING_BALANCE,
//							this._reportTemplate.get(BOPositionReportTemplate.CLOSING_BALANCE));
//					row.setProperty(BOPositionReportTemplate.MOVE2,
//							this._reportTemplate.get(BOPositionReportTemplate.MOVE));
//					row.setProperty(BOPositionReportTemplate.POSITION_TYPE, vvv);
//					row.setProperty(BOPositionReportTemplate.MOVE_LIST, sortedMoves);
//					row.setProperty(BOPositionReportTemplate.BALANCE_LIST, sortedBalances);
//					rowVector.addElement(row);
//				}
//			}
//		}
//		ReportRow[] rows = new ReportRow[rowVector.size()];
//		for (int i = 0; i < rows.length; i++) {
//			rows[i] = (ReportRow) rowVector.elementAt(i);
//		}
//		output.setRows(rows);
//
//		return output;
//	}
//
//	public boolean postFilter(Hashtable<JDate, Vector<InventorySecurityPosition>> positions, JDate positionDate) {
//		if ((positions == null) || positions.isEmpty()) {
//			return false;
//		}
//		if (positionDate == null) {
//			return false;
//		}
//		Value value = (Value) getReportTemplate().get(SantThirdPartyInventoryViewReportTemplate.VALUE);
//		if (((value != null) && (!Util.isEmpty(value.toString())))) {
//			JDate valueDate = Holiday.getCurrent().addBusinessDays(this._startDate,
//					DSConnection.getDefault().getUserDefaults().getHolidays(), value.getDays());
//			String sValueDate = Util.dateToMString(valueDate);
//			Vector<InventorySecurityPosition> datedPositions = positions.get(sValueDate);
//			if ((datedPositions == null) || (datedPositions.size() == 0)) {
//				return false;
//			}
//			InventorySecurityPosition invSecPos = datedPositions.get(0);
//			Product product = getProduct(invSecPos.getSecurityId());
//
//			// First case : nominal checking
//			if (value.getName().equals("Nominal")) {
//				double nominal = 0;
//				// START OA 29/01/14
//				if (product != null) {
//					double principal = product.getPrincipal(valueDate);
//					// END OA 29/01/14
//					nominal = InventorySecurityPosition.getTotalSecurity(datedPositions,
//							InventorySecurityPosition.BALANCE);
//
//					if (value.getSign().equals("<=")) {
//						if ((nominal * principal) > value.getValue().doubleValue()) {
//							return false;
//						}
//					}
//					if (value.getSign().equals(">=")) {
//						if ((nominal * principal) < value.getValue().doubleValue()) {
//							return false;
//						}
//					}
//				}
//			}
//			// Second case : quantity checking
//			if (value.getName().equals("Quantity")) {
//				if (value.getSign().equals("<=")) {
//					if (InventorySecurityPosition.getTotalSecurity(datedPositions, InventorySecurityPosition.BALANCE) > value
//							.getValue().doubleValue()) {
//						return false;
//					}
//				}
//				if (value.getSign().equals(">=")) {
//					if (InventorySecurityPosition.getTotalSecurity(datedPositions, InventorySecurityPosition.BALANCE) < value
//							.getValue().doubleValue()) {
//						return false;
//					}
//				}
//			}
//		}
//
//		return true;
//	}
//
//	// START OA 06/05/2014
//	@Override
//	public void initDates() {
//		super.initDates();
//		Value value = (Value) getReportTemplate().get(SantThirdPartyInventoryViewReportTemplate.VALUE);
//		if (((value != null) && (!Util.isEmpty(value.toString())))) {
//			JDate valueDate = this._startDate.addBusinessDays(value.getDays(), LocalCache.getCurrentHoliday()
//					.getHolidayCodeList());
//			if (valueDate.gte(this._endDate)) {
//				this._endDate = valueDate;
//			}
//		}
//	}
//	// END OA 06/05/2014
}
