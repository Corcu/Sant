/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.style;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import javassist.Modifier;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class SantMarginCallConfigReportStyleHelper extends CollateralConfigReportStyle {

    private static final long serialVersionUID = -5549336234462155685L;

    static final public String MCC_LEGAL_ENTITY_PREFIX = "MCC.LegalEntity.";
    static final public String MCC_PO_PREFIX = "MCC.Processing Org.";
    static final public String MCC_PREFIX = "MCC.";
    static final public String MCC_PO_RATING_PREFIX = "MCC.Processing Org.CreditRating.";
    static final public String MCC_LE_RATING_PREFIX = "MCC.LegalEntity.CreditRating.";

    // Custom column
    static final public String MCC_VALUATION_AGENT = "MCC.ValuationAgent";

    static final public String MCC_START_DATE = "MCC.Start Date";

    static final protected HashSet<String> legalEntities = new HashSet<String>();

    static {
        legalEntities.add(MCC_LEGAL_ENTITY_PREFIX);
        legalEntities.add(MCC_PO_PREFIX);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TreeList getTreeList() {
        final TreeList treeList = new TreeList("SantMarginCallConfig");
        loadMCCTreeList(treeList);
        final Vector<String> parentNode = new Vector<String>();

        @SuppressWarnings("deprecation") final TreeList legalEntityTreeList = getLegalEntityStyle().getTreeList();
        for (final String lePrefix : legalEntities) {
            addSubTreeList(treeList, parentNode, lePrefix, legalEntityTreeList);

        }
        final Iterator<String> iter = _idsToKeywords.keySet().iterator();
        final TreeList additionalFieldsTreeList = new TreeList("ADDITIONAL_FIELD");
        while (iter.hasNext()) {
            additionalFieldsTreeList.add(iter.next());

        }
        treeList.add(additionalFieldsTreeList);

        treeList.add(getRatingTreeList());

        return treeList;
    }

    private TreeList getRatingTreeList() {
        final Vector<String> ratingAgencies = LocalCache.getDomainValues(DSConnection.getDefault(), "ratingAgency");

        final TreeList mccTreeList = new TreeList("MCC");
        final TreeList poTreeList = new TreeList("Processing Org");
        final TreeList leTreeList = new TreeList("LegalEntity");
        final TreeList poRatingTreeList = new TreeList("CreditRating");
        final TreeList leRatingTreeList = new TreeList("CreditRating");

        for (final String agency : ratingAgencies) {
            poRatingTreeList.add(MCC_PO_RATING_PREFIX + agency);
            leRatingTreeList.add(MCC_LE_RATING_PREFIX + agency);
        }

        poTreeList.add(poRatingTreeList);
        leTreeList.add(leRatingTreeList);
        mccTreeList.add(poTreeList);
        mccTreeList.add(leTreeList);

        return mccTreeList;
    }

    private void loadMCCTreeList(final TreeList treeList) {
        try {
            @SuppressWarnings("rawtypes") final Class cl = Class.forName("com.calypso.tk.report.CollateralConfigReportStyle");
            final Field[] fields = cl.getDeclaredFields();

            for (int i = 0; i < fields.length; i++) {
                final Field f = fields[i];
                if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
                    final String simpleName = (String) f.get(null);
                    if (simpleName.endsWith(".")) {
                        continue;
                    }
                    final String prefixColumnName = MCC_PREFIX + simpleName;
                    treeList.add(prefixColumnName);
                }
            }
            // Custom Column
            treeList.add(MCC_VALUATION_AGENT);

        } catch (final Exception e) {
            Log.error(this, e);
        }

    }

    public boolean isColumnName(final String columnName) {
        if (Util.isEmpty(columnName)) {
            return false;
        }

        if (columnName.startsWith("MCC.") || columnName.startsWith(ADDITIONAL_FIELD_PREFIX)) {
            return true;
        }
        return false;
    }

    private String getRealColumnName(String columnName) {
        if (columnName.startsWith("MCC.")) {
            columnName = columnName.substring(4);
            // Fix to avoid to redefine all report templates - prefix LegalEntity should have a space in between
            if (columnName.contains("LegalEntity")) {
                columnName = columnName.replace("LegalEntity", "Legal Entity");
            }

        }
        return columnName;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        if (row == null) {
            return null;
        }

        final CollateralConfig config = row.getProperty(ReportRow.MARGIN_CALL_CONFIG);

        if (config == null) {
            return null;
        }

        String value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(config, columnName, this);
        if (!Util.isEmpty(value)) {
            return value;
        }
        if (columnName.startsWith(MCC_LE_RATING_PREFIX)) {
            final int leId = config.getLegalEntity().getId();
            final String agency = columnName.substring(MCC_LE_RATING_PREFIX.length());
            return getCreditRatingValue(leId, agency);
        } else if (columnName.startsWith(MCC_PO_RATING_PREFIX)) {
            final int poId = config.getProcessingOrg().getId();
            final String agency = columnName.substring(MCC_PO_RATING_PREFIX.length());
            return getCreditRatingValue(poId, agency);
        } else if (columnName.equals(MCC_VALUATION_AGENT)) {
            return getValuationAgent(config);
        }
//		///JRL 07/04/2016 Migration Calypso 14.4
//		else if(columnName.equals(MCC_START_DATE)){
//			return config.getStartingDate().getJDate(TimeZone.getDefault());
//		}

        return super.getColumnValue(row, getRealColumnName(columnName), errors);
    }

    public static String getCreditRatingValue(final int leId, final String agency) {
        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setDebtSeniority("SENIOR_UNSECURED");
        cr.setAgencyName(agency);
        cr.setRatingType(CreditRating.CURRENT);
        cr.setAsOfDate(JDate.getNow());
        cr = BOCache.getLatestRating(DSConnection.getDefault(), cr);
        if (cr != null) {
            return cr.getRatingValue();
        }
        return null;
    }

    public String getValuationAgent(final CollateralConfig config) {
        final String valuationType = config.getValuationAgentType();
        if (Util.isEmpty(valuationType) || CollateralConfig.NONE.equals(valuationType)) {
            return null;
        }

        if (CollateralConfig.PARTY_A.equals(valuationType)) {
            return config.getProcessingOrg().getCode();
        }

        if (CollateralConfig.PARTY_B.equals(valuationType)) {
            return config.getLegalEntity().getCode();
        }

        if (CollateralConfig.BOTH.equals(valuationType)) {
            return new StringBuilder(config.getProcessingOrg().getCode()).append(" ")
                    .append(config.getLegalEntity().getCode()).toString();
        }

        if (CollateralConfig.THIRD_PARTY.equals(valuationType)) {
            final int leId = config.getValuationAgentId();
            if (leId != 0) {
                return BOCache.getLegalEntity(DSConnection.getDefault(), leId).getName();
            }
        }

        return null;
    }
}
