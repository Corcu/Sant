package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.service.refdata.RemoteCollateralDataServer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventorySecurityPositionArray;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Juan Angel Torija
 */
public class SantMCGCPoolingReport extends Report {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -6089330498152017144L;

    public static final String TYPE = "SantMCGCPooling";
    public static final String CONTRATO = "Contratos";

    public static final String OWNER_AGR = "Owner agreement";
    private int idContract;

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

        String contratos = (String) getReportTemplate().get(CONTRATO);
        List<String> contratosLista = new ArrayList<>();
        if (StringUtils.isNotBlank(contratos)) {
            contratosLista = Arrays.asList(contratos.split(";"));
        }

        List<Integer> idContracts = new ArrayList<Integer>();

        Map<String, String> params = new HashMap<>();
        params.put("MC_GC_POOLING", "SI");
        params.put("MC_TRIPARTY", "SI");

        Map<Integer, String> idContractCesta = getFilteredCollateralContractsIds(params, idContracts, contratosLista);

        // Collateral value by isinAlloc and isinFather
        Map<CestaIsinKey, Double> cestaIsinCollatValue = new HashMap<CestaIsinKey, Double>();
        // Nominal by isinAlloc and isinFather
        Map<CestaIsinKey, Double> cestaIsinNominal = new HashMap<CestaIsinKey, Double>();
        // collat value by isinFather
        Map<String, Double> cestaCollatValue = new HashMap<String, Double>();

        // aggregates calculated values
        getValuesAggregated(idContractCesta, cestaIsinCollatValue, cestaIsinNominal, cestaCollatValue);

        for (CestaIsinKey key : cestaIsinCollatValue.keySet()) {

//            if (isins.contains(key.getCesta())) {
            double nom = Math.abs(cestaIsinNominal.get(key));
            String direction;
            if (nom >= 0.01) {
                if (cestaIsinNominal.get(key) < 0) {
                    direction = "P";
                } else {
                    direction = "C";
                }
                SantMCGCPoolingItem santMCGCPoolingItem = new SantMCGCPoolingItem();
                santMCGCPoolingItem.setCurrency(key.getCcy());
                santMCGCPoolingItem.setDirection(direction);
                santMCGCPoolingItem.setIsinAlloc(key.getIsin());
                santMCGCPoolingItem.setIsinCesta(key.getCesta());
                santMCGCPoolingItem.setCollateralValue(Math.abs(cestaIsinCollatValue.get(key)));
                santMCGCPoolingItem.setCollateralCestaAlloc(cestaCollatValue.get(key.getCesta()));
                santMCGCPoolingItem.setNominal(nom);

                ReportRow row = new ReportRow(santMCGCPoolingItem, TYPE);
                rowsList.add(row);
            }
        }
//        }

        output.setRows(rowsList.toArray(new ReportRow[0]));
        return output;
    }

    /**
     * @param idContractCesta
     * @param cestaIsinCollatValue
     * @param cestaIsinNominal
     * @param cestaCollatValue
     */
    private void getValuesAggregated(Map<Integer, String> idContractCesta,
                                     Map<CestaIsinKey, Double> cestaIsinCollatValue, Map<CestaIsinKey, Double> cestaIsinNominal,
                                     Map<String, Double> cestaCollatValue) {

        Map<String, Product> isinProduct = new HashMap<String, Product>();
        Map<String, List<String>> contractsIsins = new HashMap<String, List<String>>();
        for (Integer idContract : idContractCesta.keySet()) {
            CollateralConfig marginCall = new CollateralConfig();
            try {
                marginCall = DSConnection.getDefault()
                        .getRemoteService(RemoteCollateralDataServer.class).getMarginCallConfig(idContract);

                InventorySecurityPositionArray secPositions = getPositions(idContract);

                if (secPositions != null && secPositions.size() > 0) {
                    List<String> ISINs = new ArrayList<String>();
                    for (InventorySecurityPosition securityPos : secPositions) {
                        if (securityPos != null) {
                            String isin = securityPos.getProduct().getSecCode("ISIN");
                            String ccy = securityPos.getProduct().getCurrency();
                            Double nominal = getNominal(securityPos);
                            double faceValue = getFaceValue(securityPos);


                            nominal *= faceValue;
                            Double isinCollatValue = getCollateralValue(securityPos, nominal, marginCall.getCurrency());

                            if (isinCollatValue != 0) {

                                CestaIsinKey key = new CestaIsinKey(isin, idContractCesta.get(idContract), ccy);

                                if (cestaIsinCollatValue.containsKey(key)) {
                                    cestaIsinCollatValue.put(key, cestaIsinCollatValue.get(key)
                                            + isinCollatValue);
                                } else {
                                    cestaIsinCollatValue.put(key,
                                            isinCollatValue);
                                }

                                if (cestaIsinNominal.containsKey(key)) {
                                    cestaIsinNominal.put(key,
                                            cestaIsinNominal.get(key) + nominal);
                                } else {
                                    cestaIsinNominal.put(key, nominal);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(this, e); //sonar
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

    private double getFaceValue(InventorySecurityPosition securityPos) {
        double faceValue = 1;
        Product p = securityPos.getProduct();
        if (p instanceof BondAssetBacked) {
            BondAssetBacked abs = (BondAssetBacked) p;
            faceValue = abs.getFaceValue();
        } else if (p instanceof Bond) {
            Bond bond = (Bond) p;
            faceValue = bond.getFaceValue();
        }

        return faceValue;
    }

    private Double getNominal(InventorySecurityPosition securityPos) {
        Vector postions = new Vector();
        postions.add(securityPos);
        Double nominal = InventorySecurityPosition.getTotalSecurity(postions, "Balance");

        return nominal;
    }

    private InventorySecurityPositionArray getPositions(final Integer idContract) {
        InventorySecurityPositionArray secPositions = null;

        try {
            String where = "(inv_sec_balance.internal_external = 'MARGIN_CALL' AND inv_sec_balance.position_type = 'THEORETICAL' AND inv_sec_balance.date_type = 'TRADE') AND inv_sec_balance.mcc_id = " + idContract + " AND inv_sec_balance.config_id = 0 AND inv_sec_balance.security_id IN (SELECT product_id FROM product_desc WHERE (maturity_date is null or maturity_date >= " + Util.date2SQLString(getValDate()) + " ))";

            secPositions = this.getDSConnection().getRemoteInventory().getSecurityPositionsFromTo("", where, getValDate(), getValDate(), null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return secPositions;
    }

    private Double getCollateralValue(InventorySecurityPosition securityPos, double nominal, String marginCallCcy) {
        Double collatValue = Double.valueOf(0);
        try {

            // Nominal * (DirtyPrice) * (1 – (haircut/100)) / FX Rate

            double haircut = 0;
            double dirtyPrice = 1;
            double fxRate = 1;


            // haircut
            CollateralConfig contract;
            contract = DSConnection.getDefault()
                    .getRemoteService(RemoteCollateralDataServer.class)
                    .getMarginCallConfig(securityPos.getMarginCallConfigId());
            CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
            HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
            HaircutProxy haircutProxy = fact.getProxy(contract.getPoHaircutName());
            //JRL 20/04/2016 Migration 14.4
            haircut = Math.abs(haircutProxy.getHaircut(contract.getCurrency(), new CollateralCandidate(securityPos.getProduct()),
                    getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()), true,
                    contract, contract.getContractDirection())) * 100;

            // dirtyPrice
            dirtyPrice = CollateralUtilities.getDirtyPrice(securityPos.getProduct(), getValDate(), getPricingEnv(), getReportTemplate().getHolidays());

            // FX Rate
            fxRate = CollateralUtilities.getFXRate(getValDate().addBusinessDays(-1, getReportTemplate().getHolidays()), securityPos.getProduct().getCurrency(), marginCallCcy);

            // SecPosition Collat Value
            collatValue = nominal * (dirtyPrice / 100) * (1 - (haircut / 100)) * fxRate;
        } catch (Exception e) {
            Log.error(this, e);
        }

        return collatValue;
    }


    /**
     * @param params
     * @param ids
     * @return
     */
    private Map<Integer, String> getFilteredCollateralContractsIds(Map<String, String> params,
                                                                   List<Integer> ids, List<String> contratos) {

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        List<CollateralConfig> listContracts = null;
        Map<Integer, String> idContractAcuerdo = new HashMap<Integer, String>();
        // retrieve the contract
        try {

            listContracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

        } catch (RemoteException e) {
            // DB error, should not happen
            Log.error(this, e.getLocalizedMessage());
            Log.error(this, e); //sonar
            return null;
        }

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
            boolean attOK = false;

            if ((contratos.isEmpty() || contratos.contains(collateralConfig.getName())) && (allowedPOsIDs.isEmpty() || allowedPOsIDs.contains(collateralConfig.getProcessingOrg().getLegalEntityId()))) {


                for (String attribCode : params.keySet()) {
                    String attribValue = params.get(attribCode);

                    if (!attOK) { // It checks if the contract has already been added to the list

                        final String field = collateralConfig.getAdditionalField(attribCode);

                        if (attribValue.equals(field)) {
                            ids.add(collateralConfig.getId());
                            idContractAcuerdo.put(collateralConfig.getId(),
                                    collateralConfig.getName());

                            attOK = true;

                        }
                    }

                }

            }

        }
        return idContractAcuerdo;
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

        private SantMCGCPoolingReport getOuterType() {
            return SantMCGCPoolingReport.this;
        }

        public CestaIsinKey(String isin, String cesta, String ccy) {
            super();
            this.isin = isin;
            this.cesta = cesta;
            this.ccy = ccy;
        }

    }

}
