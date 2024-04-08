package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
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
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Juan Angel Torija
 */
public class SantMCTripartyReport extends Report {

    public static final String TYPE = "SantMCTriparty";
    public static final String CHECK_BO_SYSTEM = "OPERACIONES FI";
    // v14 GSM 05/03/16 - package visibility
    public static final String OWNER_AGR = "Owner agreement";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector arg0) {

        final StandardReportOutput output = new StandardReportOutput(this);

        Vector v = new Vector();
        v.add("TARGET");
        getReportTemplate().setHolidays(v);
        List<ReportRow> rowsList = new ArrayList<>();

        // Get Collateral Contracts Ids with attribute MC_Triparty = SI
        List<Integer> ids = getFilteredCollateralContractsIds("MC_TRIPARTY", "SI");

        // Get all Allocations
        List<MarginCallAllocationDTO> allocs = getAllocations(ids);

        Map<IsinFatherIdKey, Double> isinFatherIdCollatValue = new HashMap<>();
        Map<IsinFatherIdKey, Double> isinFatherIdNominal = new HashMap<>();
        Map<String, Double> fatherIdCollatValue = new HashMap<>();

        Map<String, Trade> tradeFather = new HashMap<>();
        Map<String, Product> isinProduct = new HashMap<>();

        // aggregates calculated values
        getValuesAggregated(tradeFather, isinProduct, allocs, isinFatherIdCollatValue, isinFatherIdNominal,
                fatherIdCollatValue);

        for (IsinFatherIdKey key : isinFatherIdCollatValue.keySet()) {
            double nom = Math.abs(isinFatherIdNominal.get(key));
            String direction;
            if (nom >= 0.01) {
                if (isinFatherIdNominal.get(key) < 0) {
                    direction = "P";
                } else {
                    direction = "C";
                }
                SantMCTripartyItem santMCTripartyItem = new SantMCTripartyItem();
                santMCTripartyItem.setDirection(direction);
                santMCTripartyItem.setCurrency(key.getCcy());
                santMCTripartyItem.setIsin(key.getIsin());
                santMCTripartyItem.setId(key.getFatherID());
                santMCTripartyItem.setCollateralFatherAlloc(fatherIdCollatValue.get(key.getFatherID()));
                santMCTripartyItem.setCollateralValue(Math.abs(isinFatherIdCollatValue.get(key)));
                santMCTripartyItem.setNominal(nom);

                ReportRow row = new ReportRow(santMCTripartyItem, TYPE);
                rowsList.add(row);
            }
        }

        output.setRows(rowsList.toArray(new ReportRow[0]));
        return output;
    }

    private void getValuesAggregated(Map<String, Trade> tradeFather, Map<String, Product> isinProduct,
                                     List<MarginCallAllocationDTO> allocs, Map<IsinFatherIdKey, Double> isinFatherIdCollatValue,
                                     Map<IsinFatherIdKey, Double> isinFatherIdNominal, Map<String, Double> fatherIdCollatValue) {
        List<String> wrongFather = new ArrayList<String>();
        if (allocs != null) {
            for (MarginCallAllocationDTO marginCallAllocationDTO : allocs) {
                if (marginCallAllocationDTO instanceof SecurityAllocationDTO) {
                    SecurityAllocationDTO secDto = (SecurityAllocationDTO) marginCallAllocationDTO;
                    String isin = secDto.getProduct().getSecCode("ISIN");
                    String fatherId = String.valueOf(marginCallAllocationDTO.getAttribute("Father ID"));
                    TradeArray tradeArray = new TradeArray();
                    Trade trade = new Trade();
                    Product p;
                    if (tradeFather.containsKey(fatherId)) {
                        trade = tradeFather.get(fatherId);
                        if (!trade.getKeywordValue("BO_SYSTEM").equals(CHECK_BO_SYSTEM)) {
                            wrongFather.add(fatherId);
                        }
                    } else {
                        if (!wrongFather.contains(fatherId)) {
                            try {
                                tradeArray = DSConnection.getDefault().getRemoteTrade()
                                        .getTradesByExternalRef(fatherId);
                            } catch (RemoteException e) {
                                Log.error(this, e); //sonar
                                wrongFather.add(fatherId);
                                trade = null;
                            }
                            if (tradeArray != null) {
                                if (tradeArray.size() > 0) {
                                    trade = tradeArray.get(0);
                                    tradeFather.put(fatherId, trade);
                                    if (!trade.getKeywordValue("BO_SYSTEM").equals(CHECK_BO_SYSTEM)) {
                                        wrongFather.add(fatherId);
                                    }

                                } else {
                                    wrongFather.add(fatherId);
                                    trade = null;
                                }
                            }
                        }
                    }

                    if (!wrongFather.contains(fatherId)) {
                        if (isinProduct.containsKey(isin)) {
                            p = isinProduct.get(isin);
                        } else {
                            p = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(), "ISIN",
                                    isin.toString());
                            if (p != null) {
                                isinProduct.put(isin, p);
                            }
                        }

                        if ((trade != null) && (p != null)) {

                            JDate maturityDate = JDate.getNow();//new JDate();
                            try {
                                maturityDate = trade.getMaturityDate();

                                if (maturityDate.after(getValDate().addBusinessDays(-1,
                                        getReportTemplate().getHolidays()))) {

                                    String ccy = marginCallAllocationDTO.getBaseCurrency();
                                    double poolFactor = 1;
                                    double faceValue = 0;
                                    double quantity = 0;
                                    double haircut = 0;
                                    double dirtyPrice = 1;
                                    CollateralConfig contract = new CollateralConfig();

                                    if (p.getMaturityDate().after(
                                            getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()))) {
                                        dirtyPrice = getDirtyPrice(p,
                                                getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()));

                                        try {
                                            contract = ServiceRegistry
                                                    .getDefault()
                                                    .getCollateralDataServer()
                                                    .getMarginCallConfig(
                                                            marginCallAllocationDTO.getCollateralConfigId());

                                            CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
                                            HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
                                            HaircutProxy haircutProxy = fact.getProxy(contract.getPoHaircutName());

                                            //JRL 20/04/2016 Migration 14.4
                                            haircut = Math.abs(haircutProxy.getHaircut(contract.getCurrency(),
                                                    new CollateralCandidate(p),
                                                    getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()),
                                                    true, contract, contract.getContractDirection())) * 100;
                                            if (haircut == 0) {
                                                haircut = 1;
                                            }
                                        } catch (RemoteException e1) {
                                            Log.error(this, "Error getting contract & haircut id " + marginCallAllocationDTO.getCollateralConfigId());
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

                                        IsinFatherIdKey key = new IsinFatherIdKey(isin, fatherId, ccy);

                                        if (isinFatherIdCollatValue.containsKey(key)) {
                                            isinFatherIdCollatValue
                                                    .put(key,
                                                            isinFatherIdCollatValue.get(key)
                                                                    + (quantity * faceValue * poolFactor * dirtyPrice * (1 - (haircut / 100))));
                                        } else {
                                            isinFatherIdCollatValue.put(key, (quantity * faceValue * poolFactor
                                                    * dirtyPrice * (1 - (haircut / 100))));
                                        }

                                        if (isinFatherIdNominal.containsKey(key)) {
                                            isinFatherIdNominal.put(key, isinFatherIdNominal.get(key)
                                                    + (quantity * faceValue * poolFactor));
                                        } else {
                                            isinFatherIdNominal.put(key, quantity * faceValue * poolFactor);
                                        }
                                    }
                                } else {
                                    wrongFather.add(fatherId);
                                }
                            } catch (Exception ex) {
                                Log.error(this, ex); //sonar
                            }
                        }
                    }
                }
            }
        }

        for (Entry<IsinFatherIdKey, Double> entry : isinFatherIdCollatValue.entrySet()) {
            if (fatherIdCollatValue.containsKey(entry.getKey().getFatherID())) {
                fatherIdCollatValue.put(entry.getKey().getFatherID(),
                        fatherIdCollatValue.get(entry.getKey().getFatherID()) + Math.abs(entry.getValue()));
            } else {
                fatherIdCollatValue.put(entry.getKey().getFatherID(), Math.abs(entry.getValue()));
            }
        }

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
        str.append(" AND trade_keyword.trade_id    =trade.trade_id ");
        str.append(" AND trade_keyword.keyword_name='Father ID' ");
        str.append(" AND trade.trade_status <> 'CANCELED' ");
        str.append(" AND product_sec_code.product_id =margin_call_allocation.product_id ");
        str.append(" AND product_sec_code.sec_code ='ISIN' ");

        List<String> from = Arrays.asList("trade", "product_sec_code", "trade_keyword", "margin_call_allocation");

        List<MarginCallAllocationDTO> allocs = null;

        try {
            allocs = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallAllocations(str.toString(), from);
        } catch (RemoteException e1) {
            Log.error(this, "Error while getting allocations." + "\n" + e1); //sonar
        }
        return allocs;
    }

    /**
     * @param attribCode
     * @param attribValue
     * @return
     */
    private List<Integer> getFilteredCollateralContractsIds(String attribCode, String attribValue) {

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        List<CollateralConfig> listContracts = null;
        // retrieve the contract
        try {

            listContracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

        } catch (RemoteException e) {
            // DB error, should not happen
            Log.error(this, e.getLocalizedMessage() + "\n" + e); //sonar
            return null;
        }

        // GSM 21/07/15. SBNA Multi-PO filter
        // Agreement owners
        // GSM 04/08/15. SBNA Multi-PO filter, adapted filter by ST.
        String owners = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
        if (Util.isEmpty(owners)) {
            owners = (String) getReportTemplate().get(OWNER_AGR);
        }
        // final String owners = (String) getReportTemplate().get(SantMCGCPoolingReportTemplatePanel.OWNER_AGR);
        HashSet<Integer> allowedPOsIDs = new HashSet<Integer>();

        if (!Util.isEmpty(owners)) {
            for (String poID : Util.string2Vector(owners)) {
                allowedPOsIDs.add(Integer.parseInt(poID));
            }
        }

        List<Integer> ids = new ArrayList<Integer>();
        for (CollateralConfig collateralConfig : listContracts) {

            // GSM 21/07/15. SBNA Multi-PO filter
            if (!allowedPOsIDs.isEmpty()) {
                if (!allowedPOsIDs.contains(collateralConfig.getProcessingOrg().getLegalEntityId())) {
                    continue;
                }
            }

            final String field = collateralConfig.getAdditionalField(attribCode);
            if (attribValue.equals(field)) {
                ids.add(collateralConfig.getId());
            }
        }
        return ids;
    }

    private class IsinFatherIdKey {

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
            result = (prime * result) + ((this.fatherID == null) ? 0 : this.fatherID.hashCode());
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
            IsinFatherIdKey other = (IsinFatherIdKey) obj;
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
            if (this.fatherID == null) {
                if (other.fatherID != null) {
                    return false;
                }
            } else if (!this.fatherID.equals(other.fatherID)) {
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

        public IsinFatherIdKey(String isin, String fatherID, String ccy) {
            super();
            this.isin = isin;
            this.fatherID = fatherID;
            this.ccy = ccy;
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
         * @return the fatherID
         */
        public String getFatherID() {
            return this.fatherID;
        }

        /**
         * @param fatherID the fatherID to set
         */
        @SuppressWarnings("unused")
        public void setFatherID(String fatherID) {
            this.fatherID = fatherID;
        }

        private String isin;
        private String fatherID;
        private String ccy;

        private SantMCTripartyReport getOuterType() {
            return SantMCTripartyReport.this;
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
}
