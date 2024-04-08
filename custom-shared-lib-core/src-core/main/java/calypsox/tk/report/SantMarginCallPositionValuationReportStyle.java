/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.util.SantReportingUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.CashPositionFacade;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.collateral.SecurityPositionFacade;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.MarginCallPositionValuationReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.*;

/**
 * Custom Position valuations reports, requirement for MMOO project
 *
 * @author aela & Guillermo Solano
 * @version 2.0, added collateral config columns and custom columns (new filters: isin, agreement owner, agreements &
 * process date)
 * @date 27/03/2015
 */
public class SantMarginCallPositionValuationReportStyle extends MarginCallPositionValuationReportStyle {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 1L;

    // Custom Column Names
    public static final String PROCESS_DATE = "Process Date";
    public static final String AGREEMENT_TYPE = "Agreement Type";
    public static final String OWNER_SHORT_NAME = "Owner short name";
    public static final String CPTY_SHORT_NAME = "Cpty short name";
    public static final String AGREEMENT_NAME = "Agreement name";
    public static final String PRODUCT_FAMILLY = "Family";
    public static final String SANT_PRICE = "Sant Price";
    public static final String HAIRCUT = "Haircut";

    // BAU 17/03/15 - GSM
    private CollateralConfigReportStyle collateralConfigReportStyle = null;
    private final static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";
    private final static String CUSTOM_COLLATERAL_CONFIG_REPORT_STYLE_NAME = "calypsox.tk.report.CollateralConfigReportStyle";
    // new columns
    private static final String SANT_DIRTY_PRICE = "Dirty Price";
    private static final String SANT_CONTRACT_VALUE = "Santander Contract Value";

    public static final String[] DEFAULTS_COLUMNS = {PROCESS_DATE, AGREEMENT_TYPE, OWNER_SHORT_NAME, CPTY_SHORT_NAME,
            AGREEMENT_NAME, PRODUCT_FAMILLY, SANT_PRICE, SANT_DIRTY_PRICE, SANT_CONTRACT_VALUE};

    /**
     * Securities Positions colums Values
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, SecurityPositionFacade position, String columnName, Vector errors)
            throws InvalidParameterException {

        MarginCallPosition pos = (MarginCallPosition) row.getProperty(DEFAULT);

        CollateralConfig cc = pos.getCollateralConfig();

        JDate valDate = (JDate) row.getProperty(VAL_DATE);

        Object value = getContractColumnValue(row, (MarginCallPosition) position, columnName, errors);
        if (value != null) {
            return value;

            // BAU 10/03/2015. Added Dirty Price
        } else if (SANT_DIRTY_PRICE.equals(columnName)) {
            return row.getProperty(DIRTY_PRICE_QUOTE);

        } else if (SANT_CONTRACT_VALUE.equals(columnName)) {
            return row.getProperty(CONTRACT_VALUE_YESTERDAY_QUOTES);

        } else if (HAIRCUT.equals(columnName)) {
            // 13/11/2015
            // product
            final Product security = ((SecurityPosition) pos).getProduct();

            Map<Integer, CollateralConfig> agreementMap = loadAgreementsByIds(Util.string2IntVector(Integer.toString(cc.getId())));
            CollateralConfig agreement = agreementMap.get(Integer.parseInt(Integer.toString(cc.getId())));

            //JRL 20/04/2016 Migration 14.4
            double haircut = Math.abs(getProductHaircut(agreement, security, valDate, pos));
            //AAP 16/05/2016 MIG 14.4 Amount format added, 2 decimals needed
            return new Amount(haircut, 2);
        } else {
            return super.getColumnValue(row, position, columnName, errors);
        }
    }

    /**
     * Cash Positions colums Values
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, CashPositionFacade position, String columnName, Vector errors)
            throws InvalidParameterException {

        Object value = getContractColumnValue(row, (MarginCallPosition) position, columnName, errors);

        if (value != null) {

            return value;

        } else if (NOMINAL.equals(columnName)) {
            return ""; // GSM: 16/10/2014. Nominal for cash must be blank
        } else if (PRODUCT_FAMILLY.equals(columnName)) {
            return "Cash";

        } else if (SANT_CONTRACT_VALUE.equals(columnName)) {
            return position.getValue();

        } else {
            return super.getColumnValue(row, position, columnName, errors);
        }
    }

    /**
     * Generic columns values for Securities & Cash
     *
     * @param row
     * @param position
     * @param columnName
     * @param errors
     * @return column value
     */
    public Object getContractColumnValue(ReportRow row, MarginCallPosition position, String columnName,
                                         @SuppressWarnings("rawtypes") Vector errors) {

        MarginCallPosition pos = (MarginCallPosition) row.getProperty(DEFAULT);

        CollateralConfig cc = pos.getCollateralConfig();

        if (PROCESS_DATE.equals(columnName)) {
            JDatetime valDateTime = (JDatetime) row.getProperty(VALUATION_DATE);
            return valDateTime;
        } else if (AGREEMENT_TYPE.equals(columnName)) {
            return cc.getContractType();
        } else if (OWNER_SHORT_NAME.equals(columnName)) {
            return cc.getProcessingOrg().getAuthName();
        } else if (CPTY_SHORT_NAME.equals(columnName)) {
            return cc.getLegalEntity().getAuthName();
        } else if (AGREEMENT_NAME.equals(columnName)) {
            return cc.getName();
        }

        // BAU 10/03/2015. Check is a CollateralConfig Column
        final String realColumnName = getMarginCallConfigReportStyle().getRealColumnName(MARGIN_CALL_CONFIG_PREFIX,
                columnName);
        if (!Util.isEmpty(realColumnName)) {
            // add collateral configmo as column
            row.setProperty(MARGIN_CALL_CONFIG, cc);
            return getMarginCallConfigReportStyle().getColumnValue(row, realColumnName, errors);
        }
        // none
        return null;
    }

    /**
     * Sets report columns
     */
    @Override
    public TreeList getTreeList() {

        final TreeList treeList = super.getTreeList();
        treeList.add(PROCESS_DATE);
        treeList.add(AGREEMENT_TYPE);
        treeList.add(OWNER_SHORT_NAME);
        treeList.add(CPTY_SHORT_NAME);
        treeList.add(AGREEMENT_NAME);
        treeList.add(PRODUCT_FAMILLY);
        treeList.add(SANT_PRICE);
        treeList.add(SANT_DIRTY_PRICE);
        treeList.add(SANT_CONTRACT_VALUE);
        treeList.add(HAIRCUT);

        return treeList;
    }

    /**
     * Recover the custom collateral Style
     */
    @Override
    protected CollateralConfigReportStyle getMarginCallConfigReportStyle() {

        try {
            if (this.collateralConfigReportStyle == null) {

                this.collateralConfigReportStyle = (CollateralConfigReportStyle) InstantiateUtil.getInstance(
                        CUSTOM_COLLATERAL_CONFIG_REPORT_STYLE_NAME, true, true);

            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        if (this.collateralConfigReportStyle == null) {
            Log.error(this, "Cannot load custom class " + CUSTOM_COLLATERAL_CONFIG_REPORT_STYLE_NAME + ". Using CORE");
            return super.getMarginCallConfigReportStyle();
        }
        return this.collateralConfigReportStyle;
    }

    // 13/11/2015
    /* load agreements from agreement ids */
    private Map<Integer, CollateralConfig> loadAgreementsByIds(List<Integer> agreementIds) {
        Map<Integer, CollateralConfig> agreements = new HashMap<Integer, CollateralConfig>();

        try {

            agreements = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigByIds(
                    agreementIds);
        } catch (PersistenceException e) {
            System.out.println("Error getting contracts" + e);
        }

        return agreements;
    }


    // 13/11/2015
    // return Haircut
    private double getProductHaircut(CollateralConfig agreement, Product product, JDate valDate, MarginCallPosition pos) {

        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(agreement.getPoHaircutName());

        //v14 GSM - 12/04/2016 - Has changed
        //AAP MIG 14.4 16/05 Changed x10000 multiplier by x100
        return haircutProxy.getHaircut(agreement.getCurrency(), new CollateralCandidate(product), valDate, true, agreement, pos.getDirection()) * 100;
    }

} // end class
