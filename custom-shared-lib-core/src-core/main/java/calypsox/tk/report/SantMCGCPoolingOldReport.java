package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.RemoteCollateralServer;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.service.refdata.RemoteCollateralDataServer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Juan Angel Torija
 */
public class SantMCGCPoolingOldReport extends Report {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -6089330498152017144L;

    public static final String TYPE = "SantMCGCPooling";

    public static final String CESTAS = "Cesta1";

    public static final String OWNER_AGR = "Owner agreement";

    /**
     *
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector arg0) {

        final StandardReportOutput output = new StandardReportOutput(this);

        Vector v = new Vector();
        v.add("TARGET");
        getReportTemplate().setHolidays(v);

        List<ReportRow> rowsList = new ArrayList<ReportRow>();

        String cestas = (String) getReportTemplate().get(CESTAS);

        List<String> isins = Arrays.asList(cestas.split(";"));

        List<Integer> idContracts = new ArrayList<Integer>();
        // Get Collateral Contracts Ids with attribute MC_GC_POOLING = SI
        // GSM 21/07/15. SBNA Multi-PO filter
        Map<Integer, String> idContractCesta = getFilteredCollateralContractsIds("MC_GC_POOLING", "SI", idContracts);

        // Get all Allocations
        List<MarginCallAllocationDTO> allocs = getAllocations(idContracts);
        // Collateral value by isinAlloc and isinFather
        Map<CestaIsinKey, Double> cestaIsinCollatValue = new HashMap<CestaIsinKey, Double>();
        // Nominal by isinAlloc and isinFather
        Map<CestaIsinKey, Double> cestaIsinNominal = new HashMap<CestaIsinKey, Double>();
        // collat value by isinFather
        Map<String, Double> cestaCollatValue = new HashMap<String, Double>();

        // aggregates calculated values
        getValuesAggregated(idContractCesta, allocs, cestaIsinCollatValue, cestaIsinNominal, cestaCollatValue);

        for (CestaIsinKey key : cestaIsinCollatValue.keySet()) {

            if (isins.contains(key.getCesta())) {
                double nom = Math.abs(cestaIsinNominal.get(key));
                String direction;
                if (nom >= 0.01) {
                    if (cestaIsinNominal.get(key) < 0) {
                        direction = "P";
                    } else {
                        direction = "C";
                    }
                    SantMCGCPoolingOldItem santMCGCPoolingOldItem = new SantMCGCPoolingOldItem();
                    santMCGCPoolingOldItem.setCurrency(key.getCcy());
                    santMCGCPoolingOldItem.setDirection(direction);
                    santMCGCPoolingOldItem.setIsinAlloc(key.getIsin());
                    santMCGCPoolingOldItem.setIsinCesta(key.getCesta());
                    santMCGCPoolingOldItem.setCollateralValue(Math.abs(cestaIsinCollatValue.get(key)));
                    santMCGCPoolingOldItem.setCollateralCestaAlloc(cestaCollatValue.get(key.getCesta()));
                    santMCGCPoolingOldItem.setNominal(nom);

                    ReportRow row = new ReportRow(santMCGCPoolingOldItem, TYPE);
                    rowsList.add(row);
                }
            }
        }

        output.setRows(rowsList.toArray(new ReportRow[0]));
        return output;
    }

    /**
     * @param idContractCesta
     * @param allocs
     * @param cestaIsinCollatValue
     * @param cestaIsinNominal
     * @param cestaCollatValue
     */
    private void getValuesAggregated(Map<Integer, String> idContractCesta, List<MarginCallAllocationDTO> allocs,
                                     Map<CestaIsinKey, Double> cestaIsinCollatValue, Map<CestaIsinKey, Double> cestaIsinNominal,
                                     Map<String, Double> cestaCollatValue) {

        Map<String, Product> isinProduct = new HashMap<String, Product>();
        Map<String, List<String>> cestaIsins = new HashMap<String, List<String>>();
        for (Integer idContract : idContractCesta.keySet()) {
            CollateralConfig marginCall = new CollateralConfig();
            try {
                marginCall = DSConnection.getDefault()
                        .getRemoteService(RemoteCollateralDataServer.class).getMarginCallConfig(idContract);
            } catch (RemoteException e) {
                Log.error(this, e); //sonar
            }
            List<SecurityPositionDTO> securityPositions = getSecurities(marginCall, DSConnection.getDefault(),
                    getValDate());
            if (!Util.isEmpty(securityPositions)) {
                List<String> ISINs = new ArrayList<String>();
                for (SecurityPositionDTO securityPos : securityPositions) {
                    if (securityPos != null) {
                        ISINs.add(securityPos.getProduct().getSecCode("ISIN"));
                    }
                }
                cestaIsins.put(marginCall.getAdditionalField("CESTA_GC_POOLING"), ISINs);
            }
        }

        if (allocs != null) {
            for (MarginCallAllocationDTO marginCallAllocationDTO : allocs) {
                if (marginCallAllocationDTO instanceof SecurityAllocationDTO) {

                    SecurityAllocationDTO secDto = (SecurityAllocationDTO) marginCallAllocationDTO;
                    String isin = secDto.getProduct().getSecCode("ISIN");
                    String ccy = marginCallAllocationDTO.getBaseCurrency();
                    String cesta = idContractCesta.get(marginCallAllocationDTO.getCollateralConfigId());

                    if (!Util.isEmpty(cestaIsins) && (cestaIsins.get(cesta) != null)
                            && cestaIsins.get(cesta).contains(isin)) {
                        double poolFactor = 1;
                        double faceValue = 0;
                        double quantity = 0;
                        double haircut = 1;
                        double dirtyPrice = 1;
                        CollateralConfig contract = new CollateralConfig();

                        Product p;

                        if (isinProduct.containsKey(isin)) {
                            p = isinProduct.get(isin);
                        } else {
                            p = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(), "ISIN",
                                    isin.toString());
                            if (p != null) {
                                isinProduct.put(isin, p);
                            }
                        }

                        if (p.getMaturityDate()
                                .after(getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()))) {
                            dirtyPrice = getDirtyPrice(p,
                                    getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()));

                            try {
                                contract = DSConnection.getDefault()
                                        .getRemoteService(RemoteCollateralDataServer.class)
                                        .getMarginCallConfig(marginCallAllocationDTO.getCollateralConfigId());

                                CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
                                HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
                                HaircutProxy haircutProxy = fact.getProxy(contract.getPoHaircutName());
                                //JRL 20/04/2016 Migration 14.4
                                haircut = Math.abs(haircutProxy.getHaircut(contract.getCurrency(), new CollateralCandidate(p),
                                        getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()), true,
                                        contract, contract.getContractDirection())) * 100;
                                // V12
                                // haircut =
                                // haircutProxy.getHaircut(contract.getCurrency(),
                                // new CollateralCandidate(p),
                                // getValDate().addBusinessDays(-1,
                                // getReportTemplate().getHolidays()), true) *
                                // 100;
                                //
                                if (haircut == 0) {
                                    haircut = 1;
                                }
                            } catch (RemoteException e1) {
                                Log.error(this, e1); //sonar
                            }

                            try {

                                if (p instanceof BondAssetBacked) {
                                    BondAssetBacked abs = (BondAssetBacked) p;
                                    poolFactor = abs.getPoolFactor(getValDate());
                                    faceValue = abs.getFaceValue();
                                } else if (p instanceof Bond) {
                                    Bond bond = (Bond) p;
                                    poolFactor = 1;
                                    faceValue = bond.getFaceValue();

                                }
                            } catch (Exception e) {
                                Log.error(this, e); //sonar
                            }

                            quantity = ((SecurityAllocationDTO) marginCallAllocationDTO).getQuantity();

                            CestaIsinKey key = new CestaIsinKey(isin, cesta, ccy);

                            if (cestaIsinCollatValue.containsKey(key)) {
                                cestaIsinCollatValue.put(key, cestaIsinCollatValue.get(key)
                                        + (quantity * faceValue * poolFactor * dirtyPrice * (1 - (haircut / 100))));
                            } else {
                                cestaIsinCollatValue.put(key,
                                        (quantity * faceValue * poolFactor * dirtyPrice * (1 - (haircut / 100))));
                            }

                            if (cestaIsinNominal.containsKey(key)) {
                                cestaIsinNominal.put(key,
                                        cestaIsinNominal.get(key) + (quantity * faceValue * poolFactor));
                            } else {
                                cestaIsinNominal.put(key, quantity * faceValue * poolFactor);
                            }

                        }
                    }

                }
            }
            for (Entry<CestaIsinKey, Double> entry : cestaIsinCollatValue.entrySet()) {
                if (cestaCollatValue.containsKey(entry.getKey().getCesta())) {
                    cestaCollatValue.put(entry.getKey().getCesta(),
                            cestaCollatValue.get(entry.getKey().getCesta()) + Math.abs(entry.getValue()));
                } else {
                    cestaCollatValue.put(entry.getKey().getCesta(), Math.abs(entry.getValue()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public double getDirtyPrice(Product bond, JDate valDate) {

        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (bond != null) {

            String isin = bond.getSecCode("ISIN");
            String quoteName;
            try {
                quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate);
                if (!quoteName.equals("")) {
                    String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = to_date('"
                            + valDate + "', 'dd/mm/yy') AND quote_set_name = 'DirtyPrice'";
                    vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                    if ((vQuotes != null) && (vQuotes.size() > 0)) {
                        return vQuotes.get(0).getClose();
                    }
                }
            } catch (RemoteException e1) {
                Log.error(this, "Cannot retrieve dirty price", e1);

            }
        }

        return 0.00;
    }

    public List<SecurityPositionDTO> getSecurities(final CollateralConfig marginCall, final DSConnection dsConn,
                                                   JDate date) {
        final List<Integer> mccID = new ArrayList<Integer>();
        mccID.add(marginCall.getId());
        try {
            // get entry for date and contract
            final List<MarginCallEntryDTO> entries = DSConnection.getDefault()
                    .getRemoteService(RemoteCollateralServer.class)
                    .loadEntries(marginCall.getId(), JDate.getNow(), JDate.getNow(), TimeZone.getDefault(), 1);
            //V12
//			final List<MarginCallEntryDTO> entries = ServiceRegistry.getDefault(dsConn).getCollateralServer()
//					.loadEntries(mccID, new JDatetime(date), TimeZone.getDefault());
            if ((entries != null) && (entries.size() > 0)) {
                // get security positions
                if (entries.get(0).getPreviousSecurityPosition() != null) {
                    return entries.get(0).getPreviousSecurityPosition().getPositions();
                }

            }
        } catch (final RemoteException e) {
            Log.error(this, "Cannot get marginCallEntry for the contract" + "\n" + e); //sonar
        }
        return null;
    }

    /**
     * @param ids
     * @return
     */
    private List<MarginCallAllocationDTO> getAllocations(List<Integer> ids) {
        StringBuffer str = new StringBuffer();

        str.append("     margin_call_allocation.type ='Security' ");
        str.append(" AND margin_call_allocation.trade_id =trade.trade_id   ");
        str.append(" AND margin_call_allocation.mcc_id IN " + Util.collectionToSQLString(ids) + " ");
        str.append(" AND trade.trade_status <> 'CANCELED' ");
        str.append(" AND product_sec_code.product_id =margin_call_allocation.product_id ");
        str.append(" AND product_sec_code.sec_code ='ISIN' ");

        List<String> from = Arrays.asList("trade", "product_sec_code", "margin_call_allocation");

        List<MarginCallAllocationDTO> allocs = null;

        try {
            allocs = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallAllocations(str.toString(), from);
        } catch (RemoteException e1) {
            Log.error(this, "Error while getting allocations.");
            Log.error(this, e1); //sonar
        }
        return allocs;
    }

    /**
     * @param attribCode
     * @param attribValue
     * @return
     */
    private Map<Integer, String> getFilteredCollateralContractsIds(String attribCode, String attribValue,
                                                                   List<Integer> ids) {

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        List<CollateralConfig> listContracts = null;
        Map<Integer, String> idContractCesta = new HashMap<Integer, String>();
        // retrieve the contract
        try {

            listContracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

        } catch (RemoteException e) {
            // DB error, should not happen
            Log.error(this, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            return null;
        }

        // GSM 21/07/15. SBNA Multi-PO filter
        // Agreement owners
        // final String owners = (String)
        // getReportTemplate().get(SantMCGCPoolingReportTemplatePanel.OWNER_AGR);
        // GSM 04/08/15. SBNA Multi-PO filter
        String owners = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
        if (Util.isEmpty(owners)) {
            owners = (String) getReportTemplate().get(OWNER_AGR);
        }
        HashSet<Integer> allowedPOsIDs = new HashSet<Integer>();
        if (!Util.isEmpty(owners)) {
            for (String poID : Util.string2Vector(owners)) {
                allowedPOsIDs.add(Integer.parseInt(poID));
            }
        }

        for (CollateralConfig collateralConfig : listContracts) {

            // GSM 21/07/15. SBNA Multi-PO filter
            if (!allowedPOsIDs.isEmpty()) {
                if (!allowedPOsIDs.contains(collateralConfig.getProcessingOrg().getLegalEntityId())) {
                    continue;
                }
            }

            final String field = collateralConfig.getAdditionalField(attribCode);

            if (attribValue.equals(field)) {
                if (!Util.isEmpty(collateralConfig.getAdditionalField("CESTA_GC_POOLING"))) {
                    idContractCesta.put(collateralConfig.getId(),
                            collateralConfig.getAdditionalField("CESTA_GC_POOLING"));
                    ids.add(collateralConfig.getId());
                }
            }

        }
        return idContractCesta;
    }

    private class CestaIsinKey {

        private String isin;
        private String cesta;
        private String ccy;

        /**
         * @return the isin
         */
        public String getIsin() {
            return this.isin;
        }

        /**
         * @param isin the isin to set
         */
        @SuppressWarnings("unused")
        public void setIsin(String isin) {
            this.isin = isin;
        }

        /**
         * @return the ccy
         */
        public String getCcy() {
            return this.ccy;
        }

        /**
         * @param ccy the ccy to set
         */
        @SuppressWarnings("unused")
        public void setCcy(String ccy) {
            this.ccy = ccy;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = (prime * result) + getOuterType().hashCode();
            result = (prime * result) + ((this.ccy == null) ? 0 : this.ccy.hashCode());
            result = (prime * result) + ((this.cesta == null) ? 0 : this.cesta.hashCode());
            result = (prime * result) + ((this.isin == null) ? 0 : this.isin.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CestaIsinKey other = (CestaIsinKey) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (this.ccy == null) {
                if (other.ccy != null) {
                    return false;
                }
            } else if (!this.ccy.equals(other.ccy)) {
                return false;
            }
            if (this.cesta == null) {
                if (other.cesta != null) {
                    return false;
                }
            } else if (!this.cesta.equals(other.cesta)) {
                return false;
            }
            if (this.isin == null) {
                if (other.isin != null) {
                    return false;
                }
            } else if (!this.isin.equals(other.isin)) {
                return false;
            }
            return true;
        }

        /**
         * @return the cesta
         */
        public String getCesta() {
            return this.cesta;
        }

        /**
         * @param cesta
         */
        @SuppressWarnings("unused")
        public void setCesta(String cesta) {
            this.cesta = cesta;
        }

        private SantMCGCPoolingOldReport getOuterType() {
            return SantMCGCPoolingOldReport.this;
        }

        public CestaIsinKey(String isin, String cesta, String ccy) {
            super();
            this.isin = isin;
            this.cesta = cesta;
            this.ccy = ccy;
        }

    }

}
