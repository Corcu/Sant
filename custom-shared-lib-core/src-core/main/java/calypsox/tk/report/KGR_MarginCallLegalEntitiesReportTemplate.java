/**
 *
 */
package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.jaxb.xml.collateral.CollateralExposure;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.RemoteCollateralServer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.io.Serializable;
import java.util.*;

/**
 * @author aalonsop
 *
 */
public class KGR_MarginCallLegalEntitiesReportTemplate extends ReportTemplate {
    /**
     *
     */
    private static final long serialVersionUID = -462876310186440204L;
    protected KGRTemplate templateType;

    protected static final String CONTRACT = "MarginCallConfig";
    protected static final String POSITION = "Position";
    protected static final String HAIRCUT_PROP = "Haircut";
    protected static final String HEAD_CLONE = "HEAD_CLONE";
    /*
     * Template type Strings
     *
     */
    private static final String CPY_String = "CPY";
    private static final String INST_String = "INST";
    private static final String CASH_String = "CASH";
    private static final String SEC_String = "SEC";
    private static final String EQUITY_String = "EQUIT";

    @Override
    public void setDefaults() {

        Vector<String> columns = new Vector<String>();
        columns.addElement("Name");
        // templateType.setColumns(columns);
        columns.addElement("ADDITIONAL_FIELD.HEAD_CLONE");
        setColumns((String[]) (String[]) columns.toArray(new String[columns.size()]));
    }

    /**
     * TemplateType creator depending on the template name, must be called after
     * superclass _templateName attribute is initialized
     *
     * @return
     */
    protected void createTemplateType() {
        String templateName = getTemplateName();

        try {
            // If template name doesn't fit the required format a POTemplate is
            // created by default
            this.templateType = new POTemplate();
            if (templateName.contains(CPY_String))
                this.templateType = new CPYTemplate();
            else if (templateName.contains(INST_String))
                this.templateType = new InstrumentTemplate();
            else if (templateName.contains(CASH_String))
                this.templateType = new CashPositionsTemplate();
            else if (templateName.contains(SEC_String) || templateName.contains(EQUITY_String))
                this.templateType = new SecurityPositionsTemplate();
        } catch (NullPointerException e) {
            this.templateType = new POTemplate();
            Log.error(this, e); //sonar
        }
    }

    /**
     * Haircut calculation
     */
    private double getHaircut(final CollateralConfig marginCall, Product p) {
        double value = 0.0;
        // get HaircutProxy for contract
        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(marginCall.getPoHaircutName());

        if (p != null) {
            // get haircut value for security
            //JRL 20/04/2016 Migration 14.4
            value = 1 - Math.abs((haircutProxy.getHaircut(marginCall.getCurrency(), new CollateralCandidate(p), JDate.getNow(),
                    false, marginCall, marginCall.getContractDirection())));
            // percentage
            // and set
            // 100-%
        }
        return value * 100;
    }

    /**
     * Head Clone
     */
    private String getHeadClone(final CollateralConfig marginCall) {
        return CollateralUtilities.converseHeadCloneKGRContracts(marginCall.getAdditionalField(HEAD_CLONE));
    }

    // Generic types to be added later
    private List<LegalEntity> deleteDuplicates(List<LegalEntity> list) {
        Set<LegalEntity> hs = new LinkedHashSet<>();
        hs.addAll(list);
        List<LegalEntity> filteredList = new ArrayList<>();
        filteredList.addAll(hs);
        return filteredList;
    }

    /**
     * Class structure definition to avoid if/else template checks
     *
     * @author aalonsop
     *
     */
    interface KGRTemplate extends Serializable {
        public void setColumns(Vector<String> columns);

        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException;
    }

    private class POTemplate implements KGRTemplate {

        /**
         *
         */
        private static final long serialVersionUID = -2933594773284962070L;

        @Override
        public void setColumns(Vector<String> columns) {
            columns.add("Name");
            columns.add("Processing Org.Short Name");
            columns.add("Processing Org.Full Name");
            columns.add("Head Clone");
        }

        @Override
        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException {

            for (LegalEntity le : deleteDuplicates(marginCall.getAdditionalPO())) {
                CollateralConfig clonedMC = (CollateralConfig) marginCall.clone();
                clonedMC.setPoId(le.getId());
                ReportRow row = new ReportRow(clonedMC, CONTRACT);
                row.setProperty(HEAD_CLONE, getHeadClone(marginCall));
                rows.add(row);
            }
        }

    }

    private class CPYTemplate implements KGRTemplate {
        /**
         *
         */
        private static final long serialVersionUID = 3747836237889462300L;

        @Override
        public void setColumns(Vector<String> columns) {
            columns.add("Name");
            columns.add("Legal Entity.Short Name");
            columns.add("Legal Entity.Full Name");
            columns.add("Head Clone");
        }

        @Override
        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException {
            for (LegalEntity le : deleteDuplicates(marginCall.getAdditionalLE())) {
                CollateralConfig clonedMC = (CollateralConfig) marginCall.clone();
                clonedMC.setLeId(le.getId());
                ReportRow row = new ReportRow(clonedMC, CONTRACT);
                row.setProperty(HEAD_CLONE, getHeadClone(marginCall));
                rows.add(row);
            }
        }

    }

    private class InstrumentTemplate implements KGRTemplate {
        /**
         *
         */
        private static final long serialVersionUID = -3432711777259767456L;
        private static final String CUSTOMIZED = "Customized";

        @Override
        public void setColumns(Vector<String> columns) {
            columns.add("Name");
            columns.add(CollateralConfigReportStyle.PRODUCTS);
            columns.add("Head Clone");
        }

        /*
         * Need to implements a filter to delete duplicated exposure types or
         * products.
         */
        @Override
        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException {
            for (String productType : marginCall.getProductList()) {
                Vector<String> products = new Vector<String>();
                if (productType.equals(CollateralExposure.class.getSimpleName())) {
                    try {
                        products.addAll(getExposureTypes(marginCall));
                    } catch (NullPointerException e) {
                        products.add(productType);
                        Log.warn(this, e); //sonar
                    }
                }
                //Needs optimization
                for (String exposureType : products) {
                    CollateralConfig clonedMC = (CollateralConfig) marginCall.clone();
                    Vector<String> productL = new Vector<String>();
                    productL.add(exposureType);
                    clonedMC.setProductList(productL);
                    ReportRow row = new ReportRow(clonedMC, CONTRACT);
                    row.setProperty(HEAD_CLONE, getHeadClone(marginCall));
                    rows.add(row);
                }
            }
        }

        private List<String> getExposureTypes(CollateralConfig marginCall) throws NullPointerException {
            List<String> exposureTypeList = new ArrayList<>();
            Set<String> exposureTypes = new LinkedHashSet<>();
            for (String exposureType : marginCall.getExposureTypeList()) {
                // get mapped value
                String mappedExposureType = CollateralUtilities
                        .initMappingInstrumentValues(DSConnection.getDefault(), "GBO").get(exposureType);
                if (mappedExposureType != null) {
                    // filter
                    if (mappedExposureType.contains(CUSTOMIZED)) {
                        mappedExposureType = "Caps and Floors";
                    } else {
                        try {
                            mappedExposureType = mappedExposureType.substring(0, mappedExposureType.indexOf('-'));
                        } catch (StringIndexOutOfBoundsException e) {
                            Log.warn(this, "Exposure Type " + mappedExposureType + " doesnt contain - separator");
                            Log.warn(this, e); //sonar
                        }

                    }
                    exposureTypes.add(mappedExposureType);
                }
                // If this instrument cannot be found in the
                // domain values, use the key we are looking
                // for directly.
                else
                    exposureTypes.add(exposureType);
            }
            exposureTypeList.addAll(exposureTypes);
            return exposureTypeList;
        }
    }

    private class CashPositionsTemplate implements KGRTemplate {

        /**
         *
         */
        private static final long serialVersionUID = 7999985071793104840L;

        @Override
        public void setColumns(Vector<String> columns) {
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.HEADER);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTIONID);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTION_TYPE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.ACTION);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.RECONCILIATIONTYPE);
            // CPY
            columns.add("Legal Entity.Short Name");
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.OFFICE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTION_DATE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.MATURITY_DATE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.RECEIVED);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.CURRENCY);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.COLLATOBLIGATIONAMOUNT);
            //MIG V16 CONST
            //columns.add(MarginCallPositionBaseReportStyle.QUOTE_PRICE);
            columns.add("Quote Price");
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.AGREEMENTTYPE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.AGREEMENTID);
            columns.add(CollateralConfigReportStyle.ADDITIONAL_FIELD_PREFIX
                    + KGR_MarginCallLegalEntitiesReportStyle.CONTRACT_INDEPENDENT_AMOUNT);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.SOURCE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.CONCILIA);
        }

        @Override
        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException {
            try {
                List<MarginCallEntryDTO> entries = DSConnection.getDefault()
                        .getRemoteService(RemoteCollateralServer.class)
                        .loadEntries(marginCall.getId(), JDate.getNow(), JDate.getNow(), TimeZone.getDefault(), 1);
                if (entries != null) {
                    // posiciones de cash
                    if (entries.get(0).getPreviousCashPosition() != null) {
                        for (CashPositionDTO pos : entries.get(0).getPreviousCashPosition().getPositions()) {
                            ReportRow row = new ReportRow(pos, POSITION);
                            row.setProperty(CONTRACT, marginCall);
                            rows.add(row);
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Cannot get marginCallEntry for the contract", e);
            }

        }

    }

    private class SecurityPositionsTemplate implements KGRTemplate {

        private static final long serialVersionUID = 6836510907926581524L;

        @Override
        public void setColumns(Vector<String> columns) {
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.HEADER);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTIONID);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTION_TYPE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.ACTION);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.RECONCILIATIONTYPE);
            // CPY
            columns.add("Legal Entity.Short Name");
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.OFFICE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.ISSUER);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.TRANSACTION_DATE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.MATURITY_DATE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.BOND_MATURITY_DATE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.ISIN);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.RECEIVED);
            columns.add(CollateralConfigReportStyle.CURRENCY);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.COLLATBONDNOMINAL);
            //MIG V16 CONST
            //columns.add(MarginCallPositionBaseReportStyle.QUOTE_PRICE);
            columns.add("Quote Price");
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.HAIRCUT);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.AGREEMENTTYPE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.AGREEMENTID);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.SOURCE);
            columns.add(KGR_MarginCallLegalEntitiesReportStyle.CONCILIA_FIELD);
        }

        @Override
        public void getReportRows(CollateralConfig marginCall, List<ReportRow> rows) throws CloneNotSupportedException {
            try {
                List<MarginCallEntryDTO> entries = DSConnection.getDefault()
                        .getRemoteService(RemoteCollateralServer.class)
                        .loadEntries(marginCall.getId(), JDate.getNow(), JDate.getNow(), TimeZone.getDefault(), 1);
                if (entries.get(0).getPreviousSecurityPosition() != null) {
                    for (SecurityPositionDTO pos : entries.get(0).getPreviousSecurityPosition().getPositions()) {
                        if (checkProductType(pos)) {
                            ReportRow row = new ReportRow(pos, POSITION);
                            row.setProperty(CONTRACT, marginCall);
                            row.setProperty(HAIRCUT_PROP, getHaircut(marginCall, pos.getProduct()));
                            rows.add(row);
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Cannot get marginCallEntry for the contract", e);
            }
        }

        /*
         * Filter Bonds or Equity positions
         */
        private boolean checkProductType(SecurityPositionDTO position) {
            if (position.getProduct() instanceof Bond && getTemplateName().contains(SEC_String))
                return true;
            if (position.getProduct() instanceof Equity && getTemplateName().contains(EQUITY_String))
                return true;
            return false;
        }
    }
}
