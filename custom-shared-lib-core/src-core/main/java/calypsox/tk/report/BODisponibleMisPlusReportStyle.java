package calypsox.tk.report;

import calypsox.tk.util.bean.BODisponiblePartenonBean;
import calypsox.util.FormatUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BODisponibleMisPlusReportStyle extends BODisponibleSecurityPositionReportStyle{
    public static final String ORIGIN = "ORIGIN";
    public static final String PROCESSDATE = "PROCESSDATE";
    public static final String ENTITY = "ENTITY";
    public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
    public static final String BRANCH = "BRANCH";
    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String DEAL_ID = "DEAL_ID";
    public static final String INTERNAL_ID = "INTERNAL_ID";
    public static final String ISIN = "ISIN";
    public static final String STATUS = "STATUS";
    public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String COUNTERPARTY_ID = "COUNTERPARTY_ID";
    public static final String GLSCOUNTERPARTY = "GLSCOUNTERPARTY";
    public static final String COUNTERPARTYDESC = "COUNTERPARTYDESC";
    public static final String NIFCOUNTERPARTY = "NIFCOUNTERPARTY";
    public static final String COUNTERPARTYSECTOR = "COUNTERPARTYSECTOR";
    public static final String COUNTERPARTYCOUNTRY = "COUNTERPARTYCOUNTRY";
    public static final String ISSUERID = "ISSUERID";
    public static final String ISSUERGLS = "ISSUERGLS";
    public static final String ISSUERDESC = "ISSUERDESC";
    public static final String EMISORNAME = "EMISORNAME";
    public static final String ISSUERNIF = "ISSUERNIF";
    public static final String ISSUERSECTOR = "ISSUERSECTOR";
    public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
    public static final String INPUTDATE = "INPUTDATE";
    public static final String TRADEDATE = "TRADEDATE";
    public static final String VALUEDATE = "VALUEDATE";
    public static final String MATURITYDATE = "MATURITYDATE";
    public static final String STRATEGYHEDGE = "STRATEGYHEDGE";
    public static final String STRATEGY_ID = "STRATEGY_ID";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String CURRENCY = "CURRENCY";
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String PRINCIPALCUR  = "PRINCIPALCUR";
    public static final String ACCOUNTCUSTODIO = "ACCOUNTCUSTODIO";
    public static final String ISINDESC = "ISINDESC";
    public static final String INTERNALREF = "INTERNALREF";
    public static final String TITLENOMINAL = "TITLENOMINAL";
    public static final String TITLES = "TITLES";
    public static final String PRINCIPALACCAV = "PRINCIPALACCAV";
    public static final String PRINCIPALAMORTAV = "PRINCIPALAMORTAV";
    public static final String DIRTYPRICE = "DIRTYPRICE";
    public static final String FACTOR  = "FACTOR";
    public static final String BASE  = "BASE";
    public static final String CLEANPRICE = "CLEANPRICE";
    public static final String CUSTODIONAME = "CUSTODIONAME";
    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String FORMAT_NUMBER_15 = "##############0.00#############";

    private static final String FORMAT_NUMBER_0 = "0.#";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {


        LegalEntity bondIssuer = Optional.ofNullable(row.getProperty(BODisponibleSecurityPositionReportStyle.ISSUER)).filter(LegalEntity.class::isInstance).map(LegalEntity.class::cast).orElse(new LegalEntity());
        InventorySecurityPosition inventory = Optional.ofNullable(row.getProperty("Inventory")).filter(InventorySecurityPosition.class::isInstance).map(InventorySecurityPosition.class::cast).orElse(new InventorySecurityPosition());
        Book positionBook = Optional.ofNullable(inventory.getBook()).orElse(new Book());
        LegalEntity positionPO = Optional.ofNullable(positionBook).map(Book::getLegalEntity).orElse(new LegalEntity());
        LegalEntity agentCounterParty = Optional.ofNullable(inventory.getAgent()).orElse(new LegalEntity());

        if (ORIGIN.equals(columnId)) {
            return "DISPONIBLE";
        } else if (PROCESSDATE.equals(columnId)) {
            return getValDate(row);
        } else if (ENTITY.equals(columnId)) {
            return FormatUtil.splitString(getAttFromLE(positionBook.getLegalEntity(),"ALIAS_ENTITY_GER"),4);
        } else if (ACCOUNTING_CENTER.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getCentro()).orElse("");
        } else if (BRANCH.equals(columnId)) {
            return null!=positionPO ? positionPO.getCode() : "";
        } else if (BRANCH_ID.equals(columnId)) {
            return null!=positionPO ? positionPO.getId() : "";
        } else if (DEAL_ID.equals(columnId)) {
            return getDealId(row);
        } else if (INTERNAL_ID.equals(columnId)) {
            return getDealId(row);
        } else if (ISIN.equals(columnId)) {
            return FormatUtil.splitString(super.getColumnValue(row,"PRODUCT_CODE.ISIN",errors),12);
        } else if (STATUS.equals(columnId)) {
            return "LIVE";
        } else if (INSTRUMENT_ID.equals(columnId)) {
            String product = (String) Optional.ofNullable(row.getProperty(PRODUCT)).orElse("");
            String productSubType = (String) Optional.ofNullable(row.getProperty(PRODUCT_SUBTYPE)).orElse("");
            return product + "/" + productSubType;
        } else if (INSTRUMENT.equals(columnId)) {
            return getRowInstrument(row);
        } else if (INSTRTYPE.equals(columnId)) {
           return getRowInstrumentType(row);
        } else if (COUNTERPARTY_ID.equals(columnId)) {
            return FormatUtil.splitString(agentCounterParty.getCode(),10);
        } else if (GLSCOUNTERPARTY.equals(columnId)) {
            return FormatUtil.splitString(agentCounterParty.getCode(),6);
        } else if (COUNTERPARTYDESC.equals(columnId)) {
            return FormatUtil.splitString(agentCounterParty.getName(),35);
        } else if (NIFCOUNTERPARTY.equals(columnId)) {
            return FormatUtil.splitString(agentCounterParty.getExternalRef(),10);
        } else if (COUNTERPARTYSECTOR.equals(columnId)) {
            return FormatUtil.splitString(getAttFromLE(agentCounterParty,"RISK_SECTOR"),15);
        } else if (COUNTERPARTYCOUNTRY.equals(columnId)) {
            return FormatUtil.splitString(getISOCode(agentCounterParty.getCountry()),3);
        } else if (ISSUERID.equals(columnId)) {
            return FormatUtil.splitString(bondIssuer.getCode(),10);
        } else if (ISSUERGLS.equals(columnId)) {
            return FormatUtil.splitString(bondIssuer.getCode(),6);
        } else if (ISSUERDESC.equals(columnId)) {
            return FormatUtil.splitString(bondIssuer.getName(),35);
        } else if (EMISORNAME.equals(columnId)) {
            return FormatUtil.splitString(bondIssuer.getName(),200);
        } else if (ISSUERNIF.equals(columnId)) {
            return FormatUtil.splitString(bondIssuer.getExternalRef(),10);
        } else if (ISSUERSECTOR.equals(columnId)) {
            return FormatUtil.splitString(getAttFromLE(bondIssuer,"RISK_SECTOR"),15);
        } else if (ISSUERCOUNTRY.equals(columnId)) {
            return FormatUtil.splitString(getISOCode(bondIssuer.getCountry()),3);
        } else if (INPUTDATE.equals(columnId)) {
            return getValDate(row);
        } else if (TRADEDATE.equals(columnId)) {
            return "";
        } else if (VALUEDATE.equals(columnId)) {
            return getValDate(row);
        } else if (MATURITYDATE.equals(columnId)) {
            return null!=inventory.getProduct() ? inventory.getProduct().getMaturityDate() : null;
        } else if (STRATEGYHEDGE.equals(columnId)) {
            return FormatUtil.splitString(null!=positionBook.getAccountingBook() ? positionBook.getAccountingBook().getName() : "",20);
        } else if (STRATEGY_ID.equals(columnId)) {
            return getStragyId(positionBook);
        } else if (PORTFOLIO.equals(columnId)) {
            return FormatUtil.splitString(positionBook.getName(),15);
        } else if (CURRENCY.equals(columnId)) {
            return FormatUtil.splitString(inventory.getSettleCurrency(),3);
        } else if (PRINCIPAL.equals(columnId)) {
            return formatDecimal(super.getColumnValue(row, POSITION_PRINCIPAL, errors),FORMAT_NUMBER_15);
        } else if (PRINCIPALCUR.equals(columnId)) {
            Amount amount = Optional.ofNullable(super.getColumnValue(row, POSITION_PRINCIPAL, errors))
                    .map(Amount.class::cast).orElse(new Amount());
            String settleCurrency = inventory.getSettleCurrency();
            return formatDecimal(new Amount(amountToEur(amount.get(),settleCurrency,getValDate(row),null)),FORMAT_NUMBER_15);
        } else if (ACCOUNTCUSTODIO.equals(columnId)) {
            return FormatUtil.splitString(null!=inventory.getAccount() ? inventory.getAccount().getName() : "",35);
        } else if (ISINDESC.equals(columnId)) {
            return FormatUtil.splitString(null!=inventory.getProduct() ? inventory.getProduct().getDescription() : "",20);
        } else if (INTERNALREF.equals(columnId)) {
            return FormatUtil.splitString(Optional.ofNullable(inventory.getProduct()).map(sec->sec.getSecCode("REF_INTERNA")).orElse(""),20);
        } else if (TITLENOMINAL.equals(columnId)) {
            return FormatUtil.splitString(getTitleNominal(row),1);
        } else if (TITLES.equals(columnId)) {
            JDate valDate = getValDate(row);
             return formatDecimal(getPositionValueForaDate(valDate,columnId,row,row.getProperty(POSITION_VALUE)),FORMAT_NUMBER_0);
        } else if (PRINCIPALACCAV.equals(columnId)) {
            Amount amount = Optional.ofNullable(super.getColumnValue(row, POSITION_PRINCIPAL, errors))
                    .filter(Amount.class::isInstance)
                    .map(Amount.class::cast).orElse(new Amount());
            ConstantDisplayValue constantDisplayValue = Optional.ofNullable(super.getColumnValue(row, "Market Quote.DirtyPrice", errors))
                    .filter(ConstantDisplayValue.class::isInstance)
                    .map(ConstantDisplayValue.class::cast).orElse(null);
            double dirtyPrice = null != constantDisplayValue && !Double.isNaN(constantDisplayValue.get()) ? constantDisplayValue.get()*100 : 1.0;
            return formatDecimal(new Amount(amount.get()*dirtyPrice),FORMAT_NUMBER_15);
        } else if (PRINCIPALAMORTAV.equals(columnId)) {
            Amount amount = Optional.ofNullable(super.getColumnValue(row, POSITION_PRINCIPAL, errors))
                    .filter(Amount.class::isInstance)
                    .map(Amount.class::cast).orElse(new Amount());
            Amount poolFactor = new Amount(null != inventory.getProduct() ? ((Bond) inventory.getProduct()).getCurrentFactor(getValDate(row)) : 0.0);
            return formatDecimal(amount.get() * poolFactor.get(),FORMAT_NUMBER_15);
        } else if (DIRTYPRICE.equals(columnId)) {
            return formatDecimal(super.getColumnValue(row, "Market Quote.DirtyPrice", errors),FORMAT_NUMBER_15);
        } else if (FACTOR.equals(columnId)) {
            return formatDecimal(new Amount(null!=inventory.getProduct() ? ((Bond)inventory.getProduct()).getPoolFactor(getValDate(row)) : 0.0),FORMAT_NUMBER_15);
        } else if (BASE.equals(columnId)) {
            return new Amount(100);
        } else if (CLEANPRICE.equals(columnId)) {
            return formatDecimal(super.getColumnValue(row, "Market Quote.CleanPrice", errors),FORMAT_NUMBER_15);
        } else if (CUSTODIONAME.equals(columnId)) {
            return FormatUtil.splitString(agentCounterParty.getName(),200);
        } else if (FILLER1.equals(columnId)) {
            return "";
        } else if (FILLER2.equals(columnId)) {
            return "";
        } else if (FILLER3.equals(columnId)) {
            return "";
        } else if (FILLER4.equals(columnId)) {
            return "";
        } else if (FILLER5.equals(columnId)) {
            return "";
        } else {
            return super.getColumnValue(row, columnId, errors);
        }
    }

    private JDate getValDate(ReportRow row){
        return row.getProperty("ValuationDatetime")!=null ? ((JDatetime)row.getProperty("ValuationDatetime")).getJDate(TimeZone.getDefault()) : null;
    }

    private String getISOCode(String countryName){

        try {
            final Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
            if(null!=country){
                return country.getISOCode();
            }
        } catch (Exception e) {
            Log.error(this, "Error Extractin ISO Country from " + countryName + ": ", e);
        }
        return "";
    }

    private String getAttFromLE(LegalEntity entity, String atttributeName) {
        if (null != entity && !Util.isEmpty(atttributeName)) {
            Collection<LegalEntityAttribute> attributes = entity.getLegalEntityAttributes();
            if (!Util.isEmpty(attributes)) {
                for (LegalEntityAttribute att : attributes) {
                    if (att.getAttributeType().equalsIgnoreCase(atttributeName)) {
                        return att.getAttributeValue();
                    }
                }
            }
        }
        return "";
    }

    private String getTitleNominal(ReportRow row){
        String property = Optional.ofNullable(row.getProperty(POSITION_VALUE)).map(String::valueOf).orElse("");
        switch (property){
            case "Nominal":
                return "N";
            case "Quantity":
                return "T";
            default:
                return "";
        }
    }

    public static Double amountToEur(Double value, String origCurrency, JDate valDate, PricingEnv env) {
        if ("EUR".equalsIgnoreCase(origCurrency)) {
            return value;
        }
        Double valueEUR = 0.0;
        try {
            double fxRate = CollateralUtilities.getFXRatebyQuoteSet(valDate, origCurrency, "EUR", env);
            valueEUR = value * fxRate;
            if(valueEUR == 0.0){
                return value;
            }
        } catch (Exception e)  {
            Log.error(Log.CALYPSOX,"Error getting FXQuotes.", e);
        }
        return valueEUR;
    }

    public String formatDecimal(Object object, String formatSize) {
        Double value = 0.0;
        DecimalFormat decimalFormat = new DecimalFormat(formatSize);
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.forLanguageTag("US")));
        decimalFormat.setDecimalSeparatorAlwaysShown(false);

        String valueToReturn = "0.0";
        if(object instanceof Amount){
            value = ((Amount) object).get();
            valueToReturn = decimalFormat.format(value);
        }else if(object instanceof Double){
            valueToReturn = decimalFormat.format(object);
        }else if(object instanceof ConstantDisplayValue){
            double v = ((ConstantDisplayValue) object).get();
            if(!Double.isNaN(v)){
                valueToReturn = decimalFormat.format(v);
            }
        }else if(object instanceof DisplayValue){
            double v = ((DisplayValue) object).get();
            if(!Double.isNaN(v)){
                valueToReturn = decimalFormat.format(v);
            }
        }
        return valueToReturn;
    }

    private String getRowInstrument(ReportRow row){
        String property = (String) row.getProperty(ISSUE_TYPE);
        switch (property){
            case "RFCUSTCD": return "CEDULA";
            case "RFCUSTLT": return "LETRA";
            case "RFCUSTBO": return "BONO";
            case "RFCUSTPG": return "PAGARE";
            case "RFCUSTACBO": return "AUTOBONO";
            case "RFCUSTACCD": return "AUTOCEDULA";

            case "RFCUSTGRBO": return "BLOQBONO";
            case "RFCUSTGRLT": return "BLOQLETRA";
            case "RFCUSTGRPG": return "BLOQPAGARE";
            case "RFCUSTGRCD": return "BLOQCEDULA";
            case "RFCUSTGRACBO": return "BLOQAUTOBONO";
            case "RFCUSTGRACCD": return "BLOQAUTOCEDULA";

            case "RFCUSTPGBO": return "PIGBONO";
            case "RFCUSTPGLT": return "PIGLETRA";
            case "RFCUSTPGPG": return "PIGPAGARE";
            case "RFCUSTPGCD": return "PIGCEDULA";
            case "RFCUSTPGACBO": return "PIGAUTOBONO";
            case "RFCUSTPGACCD": return "PIGAUTOCEDULA";
            default: return "";
        }
    }

    private String getRowInstrumentType(ReportRow row){
        String property = (String) row.getProperty(ISSUE_TYPE);
        switch (property){
            case "RFCUSTCD":
            case "RFCUSTLT":
            case "RFCUSTBO":
            case "RFCUSTPG":
            case "RFCUSTACBO":
            case "RFCUSTACCD":
                return "CUSTODIO";
            case "RFCUSTGRBO":
            case "RFCUSTGRLT":
            case "RFCUSTGRPG":
            case "RFCUSTGRCD":
            case "RFCUSTGRACBO":
            case "RFCUSTGRACCD":
                return "BLOQUEO";
            case "RFCUSTPGBO":
            case "RFCUSTPGLT":
            case "RFCUSTPGPG":
            case "RFCUSTPGCD":
            case "RFCUSTPGACBO":
            case "RFCUSTPGACCD":
                return "PIGNORACION";
            default: return "";
        }
    }

    private String getStragyId(Book positionBook){
        if(positionBook!=null) {
            AccountingBook acctBook = positionBook.getAccountingBook();
            if(acctBook != null) {
                String acctBookName = acctBook.getName();
                if(acctBookName.equals("Negociacion")) {
                    return "NE";
                }
                else if(acctBookName.equals("Inversion crediticia")) {
                    return "IC";
                }
                else if(acctBookName.equals("Inversion a vencimiento")) {
                    return "IV";
                }
                else if(acctBookName.equals("Disponible para la venta")) {
                    return "DV";
                }
                else if(acctBookName.equals("Designados a valor razonable")) {
                    return "DVR";
                }
            }
        }
        return "NEG";
    }

    private String getDealId(ReportRow row){
        String empresa = Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean) p).getEmpresa()).orElse("");
        String centro = Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean) p).getCentro()).orElse("");
        String productType = Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean) p).getTipoProducto()).orElse("");
        String contractNumber = Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean) p).getNumeroDeContrato()).orElse("");
        return empresa + centro + productType + contractNumber;
    }
}

