package calypsox.tk.anacredit.util;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class AnacreditMapper {


    private static final String ANACREDIT_PAIS_NEGOCIO = "ANACREDIT.PaisNegocio";
    private static final String ANACREDIT_PROVINVCIA_NEGOCIO = "ANACREDIT.ProvinciaNegocio";
    protected static final String ANACREDIT_TIPO_CARTEA = "ANACREDIT.Tipo_Cartera_IFRS9";
    protected static final String ANACREDIT_FREQ = "ANACREDIT.Freq_revision_interes";
    protected static final String ANACREDIT_LEI = "ANACREDIT.Lei";
    protected static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
    protected static final String ANACREDIT_RATE_CODE = "ANACREDIT_RATE_CODE";
    protected static final String J_MIN_PADRE = "J_MIN_PADRE";
    protected static final String LEI = "LEI";
    protected static final String J_MINORISTA = "J_MINORISTA";
    private static final String BDSD = "BDSD";

    public static String getCarteraPrudencial(String tipo_cartera_ifrs9) {
        String value = "ZZZ";
        if("02_01".equalsIgnoreCase(tipo_cartera_ifrs9) || "04_01".equalsIgnoreCase(tipo_cartera_ifrs9)){
            value = "C01";
        }else if ("41_01".equalsIgnoreCase(tipo_cartera_ifrs9)
                || "08_01".equalsIgnoreCase(tipo_cartera_ifrs9)
                || "06_01".equalsIgnoreCase(tipo_cartera_ifrs9)){
            value = "C02";
        }
        return value;

    }

    public static String getEntidadDepositaria(EquityTypeIdentifier identifier) {
        if (identifier != null) {
            LegalEntity cptyOrAgent = identifier.getCounterPartyOrAgent();
            if (cptyOrAgent != null ) {
                if (identifier.isEQPLZ()) {
                    return "24";
                }
                else if (cptyOrAgent.getCode().equals("BSTE")
                        && getLEAttribute(cptyOrAgent, "TAXID").equals("A39000013")) {
                    return  "12";
                }
                else {
                    String countryISO = getCountryISO(cptyOrAgent);
                    if (Util.isEmpty(countryISO)) {
                        return "00";
                    }
                    else if ("ES".equals(getCountryISO(cptyOrAgent))) {
                        return "13";
                    }
                    else {
                        return "14";
                    }
                }
            }
        }
        return "00";
    }

    public static String getEntidadDepositaria(BOTransfer xfer) {
        if (xfer != null) {
            LegalEntity cptyOrAgent = null;

            cptyOrAgent = BOCache.getLegalEntity(DSConnection.getDefault(),xfer.getInternalAgentId());

            if(null!=cptyOrAgent){
                if (cptyOrAgent.getCode().equals("BSTE")
                        && getLEAttribute(cptyOrAgent, "TAXID").equals("A39000013")) {
                    return  "12";
                }
                else {
                    String countryISO = getCountryISO(cptyOrAgent);
                    if (Util.isEmpty(countryISO)) {
                        return "00";
                    }
                    else if ("ES".equals(getCountryISO(cptyOrAgent))) {
                        return "13";
                    }
                    else {
                        return "14";
                    }
                }
            }
        }
        return "00";
    }

    public AccountInterestConfig getAccInterest(Account account){
        if(null!=account){
            return BOCreUtils.getInstance().getAccountInterest(account);
        }
        return null;
    }

    public static String getAccountDir(AccountInterestConfig accInterest){
        String accountDir = "";
        if(null!=accInterest){
            accountDir= BOCreUtils.getInstance().getFixedFloating(accInterest);
        }
        return accountDir;
    }

    public static String getJMin(LegalEntity le) {
        String jmin = "";
        if ( null!=le)  {
            LegalEntityAttribute lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),le.getId(), le.getId(), "ALL",  J_MIN_PADRE);
            //LegalEntityAttribute lea = LegalEntityAttributesCache.getInstance().getAttribute(le.getId(), le.getId(), "ALL",  J_MIN_PADRE);
            if (null != lea && !Util.isEmpty(lea.getAttributeValue())) {
                jmin = lea.getAttributeValue();
            } else {
                //lea = LegalEntityAttributesCache.getInstance().getAttribute(le.getId(), le.getId(), "ALL",  J_MINORISTA);
                 lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),le.getId(), le.getId(), "ALL",  J_MINORISTA);
                if (null != lea) {
                    jmin = lea.getAttributeValue();
                }
            }
        }
        return jmin;
    }

    public static String  getLEAttribute(LegalEntity le, String attribute) {
        String result = "";
        if ( null!=le)  {
            //LegalEntityAttribute lea = LegalEntityAttributesCache.getInstance().getAttribute(le.getId(), le.getId(), "ALL",  attribute);
            LegalEntityAttribute lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), le.getId(), le.getId(), "ALL",  attribute);
            if (null != lea) {
                result = lea.getAttributeValue();
                if (Util.isEmpty(result)) {
                    result = "";
                }
            }
        }
        return result;
    }


    /**
     * @param currency
     * @return
     */
    public String getCurrencyMap(String currency, Vector<String> errors, String identifier) {
        try {
            String cod_dmn = LocalCache.getCurrencyDefault(currency).getAttribute("cod_DMN");
            if(Util.isEmpty(cod_dmn)){
                errors.add("Line warning: " + identifier + " - Currency: " + currency + " not mapped.");
                cod_dmn = "999"; //default value
            }
            return cod_dmn;
        } catch ( Exception ex) {
            errors.add("Line warning: " + identifier + " - Error getting Currency Defaults for Currency code: " + currency);
        }
        return "999";
    }

    /**
     * @param book
     * @return
     */
    public static String getTipoCartera(Book book){
        String cod_dmn = "";
        if(null!=book){
            String name = book.getAccountingBook().getName();
            cod_dmn = LocalCache.getDomainValueComment(DSConnection.getDefault(), ANACREDIT_TIPO_CARTEA, name);
            if(Util.isEmpty(cod_dmn)){
                cod_dmn = "06_01";
            }
        }
        return cod_dmn;
    }

    public String getPlazoResidual(int diff){
        if(diff<=90){
            return "P2";
        }else if(diff<=180){
            return "P3";
        }else if(diff<=365){
            return "P4";
        }else if(diff<=730){
            return "P5";
        }else if(diff<=1825){
            return "P6";
        }else if(diff<=3650){
            return "P7";
        }else if(diff<=7300){
            return "PE";
        }else if(diff>7300){
            return "PF";
        }else {
            return "";
        }
    }

    public String getPlazoOrigenM(int diff){
        if(diff<=7){
            return "05";
        }else if(diff<=30){
            return "10";
        }else if(diff<=90){
            return "20";
        }else if(diff<=180){
            return "30";
        }else if(diff<=365){
            return "40";
        }else if( diff<=540){
            return "50";
        }else if(diff<=730){
            return "60";
        }else if(diff<=1095){
            return "70";
        }else if(diff<=1825){
            return "80";
        }else if(diff>1825){
            return "90";
        }else {
            return "";
        }
    }

    /**
     * @param range
     * @return Attribute ANACREDIT_RATE_CODE from RateIndex of the account
     */
    public RateIndexDefaults getRateIndexDefault(AccountInterestConfigRange range) {
        if(null!=range){
            return LocalCache.getRateIndexDefaults(DSConnection.getDefault(), range.getRateIndex().getCurrency(), range.getRateIndex().getName());
        }
        return null;
    }

    public Double getRateIndexQuote(AccountInterestConfigRange range, JDate valdate) {
        if (range != null){
            return getRateIndexQuote(range.getRateIndex(), valdate);
        }
        return 0.0;
    }

    public Double getRateIndexQuote(RateIndex rateIndex, JDate valdate){
        String quoteName = rateIndex.getQuoteName();
        QuoteValue value = new QuoteValue();
        value.setQuoteSetName("OFFICIAL");
        value.setName(quoteName);
        value.setQuoteType("Yield");
        value.setDate(valdate);
        try {
            final QuoteValue quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);
            if(null!=quoteValue){
                return quoteValue.getClose();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading QuoteValue for: " + quoteName + ": " + e);
        }
        return 0.0;
    }

    public String getRateCode(RateIndexDefaults rateIndexDefaults){
        if(null!=rateIndexDefaults){
            return rateIndexDefaults.getAttribute(ANACREDIT_RATE_CODE);
        }
        return "";
    }

    public String getRateFrec(RateIndexDefaults rateIndexDefaults){
        if(null!=rateIndexDefaults){
            Frequency publishFrequency = rateIndexDefaults.getPublishFrequency();
            String s = publishFrequency.toString();
            return LocalCache.getDomainValueComment(DSConnection.getDefault(), ANACREDIT_FREQ, s);
        }
        return "";
    }

    public String getFreqRevTipoInteres(Frequency frequency) {
        if(null != frequency) {
            String s = frequency.toString();
            switch (s) {
                case "NON": return "0";
                case "DLY": return "16";
                case "MTH": return "8";
                case "QTR": return "18";
                case "SA": return "19";
                case "PA": return "1";
            }
        }
        return "12"; // default
    }


    public Double getSpread(AccountInterestConfig accInterests) {
        double spread = 0.0;
        AccountInterestConfigRange range = getRange(accInterests);
        if(null!=range){
            spread = range.getSpread();
        }
        return spread;
    }


    public AccountInterestConfigRange getRange(AccountInterestConfig accInterests){ //TODO comprobar rango de active from y active to
        Iterator<AccountInterestConfigRange> iterator = accInterests.getRanges().iterator();
        while (iterator.hasNext()) {
            final AccountInterestConfigRange range = iterator.next();
            if ((range == null) || range.isFixed()) {
                continue;
            }
            if (range.getRateIndex() == null) {
                continue;
            }
            return range;
        }
        return null;
    }

    public boolean validAgent(LegalEntity legalEntity){
        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), ANACREDIT_LEI);
        if(null!=legalEntity && !Util.isEmpty(domainValues)){
            String leiDefaultValue = domainValues.get(0);
            List<LegalEntityAttribute> legalEntityAttributes = (List<LegalEntityAttribute>) legalEntity.getLegalEntityAttributes();
            if(!Util.isEmpty(legalEntityAttributes)){
                String lei = legalEntityAttributes.stream()
                        .filter(att -> LEI.equalsIgnoreCase(att.getAttributeType()))
                        .map(LegalEntityAttribute::getAttributeValue)
                        .findFirst()
                        .orElse("");
                return leiDefaultValue.equalsIgnoreCase(lei);
            }
        }
        return false;
    }

    public Account loadAccount(String id, String currency){
        Account account = null;
        final List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), MARGIN_CALL_CONTRACT, id);
        account = accounts.stream().filter(acc -> acc.getCurrency().equalsIgnoreCase(currency)).findFirst().orElse(null);
        return account;
    }

    /**
     * Is SELL Trade
     * @param trade
     * @return
     */
    public boolean isSell(Trade trade) {
        return (null!= trade && trade.getQuantity()<0);
    }

    /**
     * Is SELL Trade
     * @param pos
     * @return
     */
    public boolean isSell(SecurityPositionDTO pos) {
        return (null!= pos && pos.getQuantity()<0);
    }

    /**
     * Is SELL Trade
     * @param pos
     * @return
     */
    public boolean isSell(InventorySecurityPosition pos) {
        return (null!= pos && pos.getTotal()<0);
    }

    public static String getPaisNegocio(LegalEntity le) {
        String result = LocalCache.getDomainValueComment(DSConnection.getDefault(), ANACREDIT_PAIS_NEGOCIO, "ALL");
        if (Util.isEmpty(result)) {
            result = "0724";
        }
        String specificCode = LocalCache.getDomainValueComment(DSConnection.getDefault(), ANACREDIT_PAIS_NEGOCIO, le.getCode());
        if (Util.isEmpty(specificCode) && BDSD.equals(le.getCode())) {
            specificCode = "0826";
        }
        if(!Util.isEmpty(specificCode)) {
            result = specificCode;
        }
        return result;
    }

    public String getProvinciaNegocio(LegalEntity le) {
     String result = LocalCache.getDomainValueComment(DSConnection.getDefault(), ANACREDIT_PROVINVCIA_NEGOCIO, le.getCode());
        if (Util.isEmpty(result)) {
            if (BDSD.equals(le.getCode())) {
                result = "90";
            } else if ("BSTE".equals(le.getCode())) {
                result = "28";
            }
        }
        return result;
    }

    public String getCotiza(String jerarquia) {
        String result = "M00";
        if (!Util.isEmpty(jerarquia))  {
            if ("1".equals(jerarquia)) {
                return "M00";
            } else if ("2".equals(jerarquia)) {
                return "M01";
            } else if ("3".equals(jerarquia)) {
                return "M03";
            }
        }
        return result;
    }

    public String getJerarquiaValorRazonable(String jerarquia ) {
        String result = "1";
        if (!Util.isEmpty(jerarquia))  {
            if ("1".equals(jerarquia)
                    ||  "2".equals(jerarquia)
                    || "3".equals(jerarquia) ) {
                result =  jerarquia;
            }
        }
        /*
        if ("99".equals(tipo_cartera_irfs9)
                || "11_01".equals(tipo_cartera_irfs9)
                || "19".equals(tipo_cartera_irfs9)) {
            result =  "0";
        }
         */
         return result;
    }

    public String getSecCodeJerarquia(Product product)  {
        String result = "1";
        try {
            if (product != null) {
                String attr = product.getSecCode(AnacreditConstants.ATTR_ANACREDIT_JERARQUIA);
                int i = Integer.valueOf(attr);
                if (!Util.isEmpty(attr)
                        && i > 0
                        && i < 4) {
                    return attr;
                }
            }
        } catch (Exception e) {} // Do nothing
        return result;
    }


    public static LegalEntity getProductIssuer(Product product) {
        if (product != null
                && product instanceof Security) {
            Security security = (Security) product;
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), security.getIssuerId());
            if (le != null)  {
                return le;
            }
        }
        return null;
    }

    public String getDerechosReenbolso (String producto_ac) {
        String derechos_reeenbolso = "2";
        if (AnacreditFormatter.PRODUCTOS_DERECHO_REEMBOLSO.contains(producto_ac)) {
            derechos_reeenbolso = "1";
        }
        return derechos_reeenbolso;
    }


    public String getTipoFuenteCarga(Product underlying, LegalEntity legalEntity) {
        String result = "1"; // Sin cargas por defecto
        if (underlying != null
                && !Util.isEmpty(underlying.getSecCode("BLOQUEO_DISPRF"))) {
            String sectorContable = getLEAttribute( legalEntity, "SECTORCONTABLE");

            if (Arrays.asList("01","1","110","111").contains(sectorContable)) {
                result ="5";
            }
            else  if ("RF152".equalsIgnoreCase(ActivoOpValores.instance().get(underlying)))   {
                result = "10";

            }
            else  if ("RF153".equalsIgnoreCase(ActivoOpValores.instance().get(underlying)))   {
                result ="11";
            } else   {
                result ="12";
            }
        }
        return result;
    }

    public String getTipoActivoRecGarantia(Product underlying) {
        if (underlying != null) {
            if (underlying instanceof Bond) {
                return "T28";
            } else if (underlying instanceof Equity) {
                return "T30";
            }
        }
        return "";
    }

    public String getCotizaModuloD(String jerarquia) {
        if (!Util.isEmpty(jerarquia)){
            if ("1".equals(jerarquia)) {
                return "O1";
            } else if ("2".equals(jerarquia)) {
                return "O2";
            } else if ("3".contains(jerarquia)) {
                return "O5";
            }
        }
        return "O1";

    }

    public static String getCountryISO(LegalEntity le) {
        if (le != null
                && !Util.isEmpty(le.getCountry())) {
            String countryName = le.getCountry();
            try {
                Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
                if (country != null) {
                    return country.getISOCode();
                }
            } catch (Exception e) {
                Log.error("AnacreditMapper", "Error Extractin ISO Country from " + countryName + ": ", e);
            }
        }
        return "";
    }
}
