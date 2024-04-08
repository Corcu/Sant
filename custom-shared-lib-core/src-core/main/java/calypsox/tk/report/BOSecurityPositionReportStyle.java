/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.bo.CustomClientCacheImpl;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.inventory.SpecificInventoryPositionValues;
import com.calypso.tk.bo.inventory.SpecificInventorySecurityPositionValues;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.*;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.*;
import com.calypso.tk.refdata.Haircut.HaircutData;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.util.BondUtil;
import com.calypso.tk.util.InstantiateUtil;
import com.enterprisedt.net.j2ssh.util.Hash;
import org.jfree.util.Log;
import java.text.SimpleDateFormat;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static calypsox.tk.core.CollateralStaticAttributes.MCC_ADD_FIELD_MCC_COUPON_RIGHTS;
import static calypsox.tk.report.BOSecurityPositionReportTemplate.*;

public class BOSecurityPositionReportStyle
        extends com.calypso.tk.report.BOSecurityPositionReportStyle {

    private static final long serialVersionUID = 2219632537376778702L;

    public static final String AGENT_NAME = "Agent Name";
    public static final String CLEANPRICE_QUOTE = "CleanPrice_Quote";
    public static final String CLEANPRICE_VALUE = "CleanPrice_Value";
    // GSM: 23/08/13. Add last price too - to see the last price from Bloomberg
    public static final String LASTPRICE_QUOTE = "LastPrice_Quote";
    public static final String LASTPRICE_VALUE = "LastPrice_Value";

    public static final String COLLATERAL_HAIRCUT = "Collateral.Haircut";
    public static final String MCC_CONTRACT_NAME = "MCC Contract Name";
    public static final String MCC_CONTRACT_TYPE = "MCC Contract Type";
    public static final String MCC_REHYPOTHECABLE_COLLATERAL = "MCC Rehypothecable Collateral";
    public static final String MCC_HAIRCUT_TYPE = "MCC Haircut Type";

    public static final String SANT_EX_DATE_COUPON = "Sant.Sant_Ex_Date_Coupon";
    public static final String NEXT_COUPON_DATE = BondReportStyle.NEXT_COUPON_DATE;
    public static final String COUPON_FREQUENCY = BondReportStyle.COUPON_FREQUENCY;

    public static final String MCC_COUPON_RIGHTS = "Sant.MCC Coupon Rights";
    public static final String ACCOUNTING_BOOK = "Sant.Accounting Book";
    public static final String BOOK_ACTIVITY = "Sant.Book Activity";

    public static final String TOTAL_ISSUED = "Total Issued";
    public static final String ValDatePrice = "ValDate Price";

    public static final String CONCILIATION_PRICE_D = "Conciliation Price D";
    public static final String CONCILIATION_PRICE = "Conciliation Price D-1";
    public static final String CONCILIATION_PRICE_2 = "Conciliation Price D-2";
    public static final String INV_PRODUCT = "INV_PRODUCT";
    public static final String ROW_NUMBER = "ROW_NUMBER";
    public static final String HOLIDAYS = "HOLIDAYS";

    public static final String REPORT_DATE = "Report Date";
    public static final String NOMINAL_FMT = "Nominal Fmt";
    public static final String SANT_TIPO_POSICION = "Sant.Tipo Posicion";
    public static final String SANT_SECTOR_CPTY = "Sant.Sector CPTY";
    public static final String SANT_SECTOR_EMISOR = "Sant.Sector EMISOR";
    public static final String SANT_MC_TIPO_CONTRACTO = "Sant.MC Tipo Contracto";
    public static final String SANT_AGENTE = "Sant.Agente";

    public static final String INFORMES_INTERNOS_NOMINAL = "Informes Internos.Nominal";
    public static final String INFORMES_INTERNOS_MVALUE = "Informes Internos.Market Value";
    public static final String INFORMES_INTERNOS_MVALUE_EUR = "Informes Internos.Market Value EUR";
    public static final String INFORMES_INTERNOS_PRECIO = "Informes Internos.Precio";

    public static final String UNDERLYING_TYPE = "Underlying Type";

    // EQUITY - Template Disponible Recon
    public static final String EQUITY_FEED = "EQUITY_FEED";
    public static final String EQUITY_NOMINAL = "EQUITY_NOMINAL";
    public static final String EQUITY_SISTEMA_CALC  = "EQUITY_SISTEMA_CALC";
    public static final String EQUITY_CURRENCY  = "EQUITY_CURRENCY";
    public static final String EQUITY_NUMTITULOS  = "EQUITY_NUMTITULOS";
    public static final String EQUITY_DESCRIPCION  = "EQUITY_DESCRIPCION";
    public static final String EQUITY_MERCADO = "EQUITY_MERCADO";
    public static final String EQUITY_ANOTACION  = "EQUITY_ANOTACION";
    public static final String EQUITY_PRECIO_UD  = "EQUITY_PRECIO_UD";
    public static final String EQUITY_TIPOPRODUCTO  = "EQUITY_TIPOPRODUCTO";

    // Custom client cache implementation
    private static final CustomClientCacheImpl _customClientCache = (CustomClientCacheImpl) BOCache.getCustomClientCache();

    /*
     * EFECTIVO como precio*nominal (en divisa origen -currency de la equity-)
     * para los IM en titulos
     */
    public static final String MARKET_VALUE = "Market Value";
    /*
     * EFECTIVO_EUR como precio*nominal (en divisa origen) para los IM en
     * titulos con el fixing D-1 a EUR
     */
    public static final String MARKET_VALUE_EUR = "Market Value EUR Fix";
    /*
     * String as FX.EUR.CCY=xx.xx with the fixing of yesterday close
     */
    public static final String FX_FIXING = "EUR Fixing";
    /*
     * Direction, Pay for nominal < 0 , receive >= 0
     */
    public static final String DIRECTION = "Direction";

    /*
     * Security Price
     */
    public static final String PRICE = "Price D-1";

    /*
     * J Issuer
     */
    public static final String JISSUER = "J Issuer";

    /*
     * Row Numbre ID
     */
    public static final String ROWID = "Row Id";

    /*
     * Nominal
     */
    public static final String NOMINAL = calypsox.tk.report.BOSecurityPositionReportTemplate.NOMINAL_PROPERTY;
    private static final String BILATERAL = "BILATERAL";
    public static final String SECTORCONTABLE = "SECTORCONTABLE";

    /**
     * Collateral Config Style
     */
    private CollateralConfigReportStyle collateralConfigReportStyle = null;

    /**
     * Prefix to identify a MarginCallConfig column
     */
    private static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";

    // Is MarginCall trade eligible in its contract
    public static final String ELIGIBLE = "Is Position Eligible";
    public static final String POSITION_ELIGIBLE_TRUE = "YES";
    public static final String POSITION_ELIGIBLE_FALSE = "NO";
    // Is MarginCall trade eligible in its contract - End

    //CENTRO y EMPRECON (SLBE)
    public static final String CENTRO = "CENTRO";
    public static final String EMPRECON = "EMPRECON";

    //CA
    public static final String NEXTCA_RECORDDATE = "Next CA Record Date";
    public static final String NEXTCATYPE = "Next CA Type";
    public static final String NEXTCA_EXDATE = "Next CA Ex Date";
    private SantGenericQuotesLoader quotesLoader = null;

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {

        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        Product product = row.getProperty(INV_PRODUCT);
        JDate valDate = JDate.valueOf(ReportRow.getValuationDateTime(row));
        QuoteSet quoteSet = row.getProperty(QUOTE_SET_PROPERTY);
        Vector holidays = row.getProperty(BOSecurityPositionReportStyle.HOLIDAYS);
        PricingEnv pricingEnv = row.getProperty(PRICING_ENV_PROPERTY);
        CollateralConfig mcConfig = row.getProperty(COLLATERAL_CONFIG_PROPERTY);
        Integer rowNumber = row.getProperty(ROW_NUMBER);

        if (inventory == null) {
            throw new InvalidParameterException(
                    "Invalid row " + row + ". Cannot locate Inventory object");
        }
        
        String entity = null;
        if (inventory.getBook() != null) {
        	entity = BOCreUtils.getInstance().getEntity(inventory.getBook().getName());
        }

        if (SantProductCustomDataReportStyle
                .isProductCustoDataColumn(columnId)) {
            return getProductCustomDataReportStyle().getColumnValue(row,
                    columnId, errors);
        }

        if (REPORT_DATE.equals(columnId)) {
            return valDate;
        } else if (CENTRO.equals(columnId)) {
            if (product!=null){
                return BOCreUtils.getInstance().getCentroContable(product, entity, false);
            }
            return "";
        } else if (EMPRECON.equals(columnId)) {
            return BOCreUtils.getInstance().getEntityCod(entity, false);
        } else if (NOMINAL_FMT.equals(columnId)) {
            //+1000.00 or
            Double nominal = getNominal(row, inventory);
            if (nominal != null) {
                String valorString = new Amount(nominal, 2).toString();
                valorString = valorString.replace(".", "");
                valorString = valorString.replace(",", ".");
                return valorString;
            }
            return "";
        } else if (SANT_TIPO_POSICION.equals(columnId)) {
            return getTipoPosicion(row, mcConfig, inventory);
        } else if (SANT_SECTOR_CPTY.equals(columnId)
                || SANT_SECTOR_EMISOR.equals(columnId)) {
            return getSectorContable(columnId, mcConfig, inventory, product, row);

        } else if (INFORMES_INTERNOS_NOMINAL.equals(columnId)) {
            Double nominalInformes = getInformesInternosNominal(row);
            if (nominalInformes != null) {
                return new Amount(nominalInformes, 2);
            }
        } else if (INFORMES_INTERNOS_MVALUE.equals(columnId)) {
            Double mValueInformes = getInformesInternosMarketValue(row);
            if (mValueInformes != null) {
                return new Amount(mValueInformes, 2);
            }
        } else if (INFORMES_INTERNOS_MVALUE_EUR.equals(columnId)) {
            Double mValueInformesEUR = getInformesInternosMarketValueEURNoFixing(row);
            if (mValueInformesEUR != null) {
                return new Amount(mValueInformesEUR, 2);
            }
        } else if (INFORMES_INTERNOS_PRECIO.equals(columnId)) {
            Double precio = getInformesInternosPrecio(row);
            if (precio != null) {
                return new Amount(precio, 2);
            }

        } else if (SANT_MC_TIPO_CONTRACTO.equals(columnId)) {
            if (mcConfig != null) {
                if (!mcConfig.isTriParty()) {
                    return BILATERAL;
                }
                return mcConfig.getContractType();
            }
            return null;
        } else if (SANT_AGENTE.equals(columnId)) {
            if (mcConfig.isTriParty()) {
                return super.getColumnValue(row, MARGIN_CALL_CONFIG_PREFIX + "Triparty Agent", errors);
            } else {
                return mcConfig.getLegalEntity().getCode();
            }
        } else if (AGENT_NAME.equals(columnId)) {
            if (inventory.getAgent() == null) {
                return "NONE";
            }
            return inventory.getAgent().getName();
        } else if (CONCILIATION_PRICE.equals(columnId)) {
            return getPriceByDate(row, product, 1);
        } else if (CONCILIATION_PRICE_D.equals(columnId)) {
            return getPriceByDate(row, product, 2);
        } else if (CONCILIATION_PRICE_2.equals(columnId)) {
            return getPriceByDate(row, product, 0);
        } else if (CLEANPRICE_QUOTE.equals(columnId)) {
            return getCleanPrice(row, product, quoteSet, pricingEnv, valDate);
        } else if (CLEANPRICE_VALUE.equals(columnId)) {
            return getCleanPriceValue(row, product, quoteSet, pricingEnv, valDate);
            // GSM: 23/08/13. Add last price too - to see the last price from
            // Bloomberg
        } else if (LASTPRICE_QUOTE.equals(columnId)) {
            return row.getProperty("LastPrice");
        } else if (LASTPRICE_VALUE.equals(columnId)) {
            return row.getProperty("LastPrice_Value");

        } else if (COLLATERAL_HAIRCUT.equals(columnId)) {
            if (inventory.getInternalExternal()
                    .equals(InventorySecurityPosition.MARGIN_CLASS)) {
                return getCollateralHaircut(inventory, row, columnId, errors);
            } else {
                return null;
            }

        } else if (MCC_CONTRACT_NAME.equals(columnId)) {
            return (mcConfig == null) ? null : mcConfig.getName();
        } else if (MCC_CONTRACT_TYPE.equals(columnId)) {
            return (mcConfig == null) ? null : mcConfig.getContractType();
        } else if (MCC_REHYPOTHECABLE_COLLATERAL.equals(columnId)) {
            return (mcConfig == null) ? null : mcConfig.isRehypothecable() + "";
        } else if (MCC_HAIRCUT_TYPE.equals(columnId)) {
            return (mcConfig == null) ? null : mcConfig.getHaircutType();
        } else if (NEXTCA_RECORDDATE.equals(columnId)){
            JDate nextRecordDate = null;
            if ((product != null) && (product instanceof Bond)) {
                nextRecordDate = (JDate) super.getColumnValue(row,
                        BondReportStyle.NEXT_COUPON_RECORD_DATE, errors);
            }

            if ((product != null) && (product instanceof Equity)) {
                nextRecordDate = (JDate) super.getColumnValue(row,
                        EquityReportStyle.NEXT_DIVIDEND_RECORD_DATE, errors);
            }
            return nextRecordDate;

            // GSM: 04/09/2013. Not showing ex cupon day
        } else if (NEXTCA_EXDATE.equals(columnId)){
            JDate caNextExDate = null;
            if ((product != null) && (product instanceof Equity)) {
                Equity equity = (Equity)product;
                Dividend dividend = ((EquityBase)equity).getNextDividend(valDate);
                if (dividend==null){
                    return caNextExDate;
                }
                caNextExDate = dividend.getCA().getExDate();
            }
            return caNextExDate;
        } else if (NEXTCATYPE.equals(columnId)){
            String caType = "";
            if ((product != null) && (product instanceof Bond)) {

            }
            if ((product != null) && (product instanceof Equity)) {
                Equity equity = (Equity)product;
                Dividend dividend = ((EquityBase)equity).getNextDividend(valDate);
                if (dividend==null){
                    return caType;
                }
                caType = dividend.getCA().getSwiftEventCode().toString();
            }
            return caType;
        }else if (SANT_EX_DATE_COUPON.equals(columnId)) {

            if ((product != null) && (product instanceof Bond)) {

                Bond bond = (Bond) product;
                // GSM: 04/09/2013. Not showing ex cupon day. We take it from
                // the super getColumn
                JDate nextCouponDate = (JDate) super.getColumnValue(row,
                        BondReportStyle.NEXT_COUPON_DATE, errors);

                if (nextCouponDate == null) {
                    if ((product != null) && (product instanceof Bond)) {
                        nextCouponDate = ((Bond) product).getMaturityDate();
                    }
                }
                // JDate nextCouponDate =
                // bond.getNextCouponDate(getValDate(row));
                if ((nextCouponDate != null)
                        && nextCouponDate.after(JDate.getNow())) {
                    int exDividendDays = (bond.getExdividendDays() <= 0) ? 0
                            : bond.getExdividendDays();
                    boolean exdividendDayBusB = bond.getExdividendDayBusB();
                    if (exdividendDayBusB) {
                        return nextCouponDate.addBusinessDays(
                                -1 * exDividendDays, bond.getHolidays());
                    } else {
                        return nextCouponDate.addDays(-1 * exDividendDays);
                    }
                }
            }
            return "";
            // GSM: 04/09/2013 If "today" is after maturity, it should so blank
            // for this field
        } else if (columnId.equals(BondReportStyle.NEXT_COUPON_DATE)) {

            Object couponDate = super.getColumnValue(row, columnId, errors);
            if (couponDate == null) {
                if ((product != null) && (product instanceof Bond)) {
                    // return ((Bond) product).getMaturityDate();
                    final JDate maturity = ((Bond) product).getMaturityDate();
                    if (maturity.after(JDate.getNow())) {
                        return maturity;
                    }
                    return "";
                }
            } else {
                return couponDate;
            }
            // COUPON_FREQUENCY
        } else if (columnId.equals(COUPON_FREQUENCY)) {
            if ((product != null) && (product instanceof Bond)) {
                return ((Bond) product).getCouponFrequency();
            }
        } else if (columnId.equals(MCC_COUPON_RIGHTS)) {
            return (mcConfig == null) ? null
                    : mcConfig.getAdditionalField(
                    MCC_ADD_FIELD_MCC_COUPON_RIGHTS);
        } else if (columnId.equals(ACCOUNTING_BOOK)) {
            return inventory.getBook().getAccountingBook().getName();
        } else if (columnId.equals(TOTAL_ISSUED)) {
            if (product != null) {
                if (product instanceof Bond) {
                    Bond bond = (Bond) product;
                    return bond.getTotalIssued();
                } else if (product instanceof Equity) {
                    Equity equity = (Equity) product;
                    return equity.getTotalIssued();
                }
            }
        } else if (columnId.equals(BOOK_ACTIVITY)) {
            return inventory.getBook().getActivity();
        } else if (ValDatePrice.equals(columnId)) {
            return getValDateCleanPrice(row, product, quoteSet, holidays, pricingEnv, valDate);
        } else if (columnId.equals(PRICE)) {
            return getDirtyPrice(row, inventory, product, quoteSet, holidays, pricingEnv, errors);

        } else if (columnId.equals(FX_FIXING)) {
            if (quotesLoader == null) {
                initQuotesLoader(row, pricingEnv);
            }
            buildMarketValueEURAndFXFix(row, quotesLoader, inventory, product, quoteSet, holidays, pricingEnv, errors);
            return row.getProperty(BOSecurityPositionReportTemplate.FX_RATE_NAME_PROPERTY);

        } else if (columnId.equals(MARKET_VALUE)) {
            Double mv = getMarketValue(row, inventory, product, quoteSet, holidays, pricingEnv, errors);
            if (mv != null)
                return new Amount(mv, 2);

        } else if (columnId.equals(MARKET_VALUE_EUR)) {
            if (quotesLoader == null) {
                initQuotesLoader(row, pricingEnv);
            }
            buildMarketValueEURAndFXFix(row, quotesLoader, inventory, product, quoteSet, holidays, pricingEnv, errors);
            Double mvEur = (Double) row.getProperty(BOSecurityPositionReportTemplate.MARKET_VALUE_EUR_PROPERTY);
            if (mvEur != null)
                return new Amount(mvEur, 2);

        } else if (columnId.equals(NOMINAL)) {
            Double nom = getNominal(row, inventory);
            if (nom != null)
                return new Amount(nom, 2);

        } else if (columnId.equals(DIRECTION)) {
            Double nom = getNominal(row, inventory);
            if (nom != null)
                return nom < 0 ? "Pay" : "Receive";
            // Is MarginCall trade eligible in its contract
        } else if (ELIGIBLE.equals(columnId)) {
            return getIsPositionEligible(inventory, mcConfig);
        } else if (ROWID.equals(columnId)) {
            return String.valueOf(rowNumber);
        } else if (JISSUER.equals(columnId)) {
            return getJIssuer(product);
        } else if (UNDERLYING_TYPE.equals(columnId)) {
            if (product != null) {
                if (product instanceof Bond) {
                    return "RF";
                } else if (product instanceof Equity) {
                    return "RV";
                }
            }
        } else if (EQUITY_NOMINAL.equals(columnId)) {
            return "1";
        } else if (EQUITY_SISTEMA_CALC.equals(columnId)) {
            return "UNIT";
        } else if (EQUITY_CURRENCY.equals(columnId)) {
            return "";
        }
        else if (EQUITY_NUMTITULOS.equals(columnId)) {
            SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
            columnId = sdfDay.format(valDate.getDate()) + "-" + sdfMonth.format(valDate.getDate()) + "-" + sdfYear.format(valDate.getDate());
        } else if (ROWID.equals(columnId)) {
            return String.valueOf(rowNumber);
        } else if (JISSUER.equals(columnId)) {
            return getJIssuer(product);
        } else if (UNDERLYING_TYPE.equals(columnId)) {
            if (product != null) {
                if (product instanceof Bond) {
                    return "RF";
                } else if (product instanceof Equity) {
                    return "RV";
                }
            }
        } else if (EQUITY_NOMINAL.equals(columnId)) {
            return "1";
        } else if (EQUITY_SISTEMA_CALC.equals(columnId)) {
            return "UNIT";
        } else if (EQUITY_CURRENCY.equals(columnId)) {
            return "";
        } else if (EQUITY_FEED.equals(columnId)) {
            String accountName = (String) super.getColumnValue(row, "Account", errors);
            if(Util.isEmpty(accountName)){
                return "";
            }

            String ccy = (String) super.getColumnValue(row, "Account.Currency", errors);
            if(Util.isEmpty(ccy)){
                return "";
            }

            String poName = (String) super.getColumnValue(row, "Account.Processing Org", errors);
            if(Util.isEmpty(poName)){
                return "";
            }
            LegalEntity le = null;
            try {
                le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(poName);
            } catch (CalypsoServiceException e) {
                Log.error("Error: ", e);
            }
            if(le==null){
                return "";
            }
            int poId = le.getId();

            Account account = null;
            try {
                account = DSConnection.getDefault().getRemoteAccounting().getAccount(accountName, poId, ccy);
            } catch (CalypsoServiceException e) {
                Log.error("Error: ", e);
            }

            if(account!=null) {
                String accAttr = account.getAccountProperty("XferAgentAccount");
                if (!Util.isEmpty(accAttr)) {
                    return accAttr;
                }
                else if (account.getName().contains("@")){
                    String[] accNames = account.getName().split("@");
                    if (accNames.length >=2){
                        return accNames[1];
                    }
                }
                else {
                    return account.getName();
                }
            }

            return "";
        }
        else if (EQUITY_NUMTITULOS.equals(columnId)) {
            SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
            columnId = sdfDay.format(valDate.getDate()) + "-" + sdfMonth.format(valDate.getDate()) + "-" + sdfYear.format(valDate.getDate());
        }
        else if (EQUITY_DESCRIPCION.equals(columnId)) {
            return "";
        }
        else if (EQUITY_MERCADO.equals(columnId)) {
            return "";
        }
        else if (EQUITY_ANOTACION.equals(columnId)) {
            return "";
        }
        else if (EQUITY_PRECIO_UD.equals(columnId)) {
            return "";
        }
        else if (EQUITY_TIPOPRODUCTO.equals(columnId)) {
            return "";
        }
        else {
            // Is MarginCall trade eligible in its contract - End
            String movementType = (String) row
                    .getProperty(BOPositionReportTemplate.MOVE);
            if (!Util.isEmpty(movementType)
                    && (movementType.equals("Balance_HC"))) {

                JDate columnDate = extractDate(columnId);
                if (columnDate != null) {

                    Hashtable positions = (Hashtable) row
                            .getProperty(BOPositionReport.POSITIONS);
                    if (positions == null) {
                        return null;
                    }

                    String s = Util.dateToMString(columnDate);
                    Vector datedPositions = (Vector) positions.get(s);
                    if ((datedPositions == null)
                            || (datedPositions.size() == 0)) {
                        return null;
                    }
                    InventorySecurityPosition invSecPos = (InventorySecurityPosition) datedPositions
                            .get(0);
                    double totalSecurity = invSecPos.getTotalSecurity();
                    Product security = invSecPos.getProduct();
                    Double collateralHaircut = getCollateralHaircut(inventory,
                            row, columnId, errors);
                    Double faceValue = null;
                    if ((security != null) && (security instanceof Bond)) {
                        faceValue = ((Bond) security).getFaceValue();
                    }

                    Double bondClosingPricing = getBodnClosingPrice(row, product, quoteSet, pricingEnv, valDate);

                    // Amount balanceHCAmount = null;
                    if ((faceValue != null) && (collateralHaircut != null)
                            && (bondClosingPricing != null)) {
                        Double balanceHC = totalSecurity * faceValue
                                * (collateralHaircut / 100)
                                * bondClosingPricing;
                        return new Amount(balanceHC);
                    }
                }
            }
        }
        // try BOSecurityPositionReportStyle column as default
        Object valueCol = super.getColumnValue(row, columnId, errors);
        if (valueCol != null)
            return valueCol;

            // no value return and try CollateralConfig column
        else if (getMarginCallConfigReportStyle().isMarginCallConfigColumn(
                MARGIN_CALL_CONFIG_PREFIX, columnId)) {
            // check is collateral Config
            return getMarginCallConfigColumn(row, columnId, errors);
        }

        return valueCol;
    }

    private String getSectorContable(String columnId, CollateralConfig mcConfig, Inventory inventory, Product product, ReportRow row) {
        String rst = "";
        int legalEntityId = 0;
        if (inventory instanceof InventorySecurityPosition) {
            if (columnId.equals(SANT_SECTOR_CPTY)) {
                legalEntityId = mcConfig.getLegalEntity().getLegalEntityId();
            } else {
                int securityId = ((InventorySecurityPosition) inventory)
                        .getSecurityId();
                if (product instanceof Security) {
                    legalEntityId = ((Security) product).getIssuerId();
                }
            }
            LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), legalEntityId, legalEntityId, "ALL", SECTORCONTABLE);
            if (attr != null) {
                rst = attr.getAttributeValue();
            }
        }
        return rst;
    }

    public static Amount getNominalFactored(ReportRow row) {
        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        Map positions = row.getProperty(BOPositionReport.POSITIONS);
        BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");

        Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
        if (Util.isEmpty(datedPositions)) {
            return null;
        }
        Double nominal = InventorySecurityPosition.getTotalSecurity(datedPositions, "Balance");
        final Product product = inventory.getProduct();
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            nominal *= bond.getFaceValue();
        }
        if (nominal != null) {
            return new Amount(nominal, 2);
        }
        return null;
    }

    private String getTipoPosicion(ReportRow row, CollateralConfig mcConfig, Inventory inventory) {
        if (mcConfig.isTriParty()||
                ("CSD".equalsIgnoreCase(mcConfig.getContractType()) && !mcConfig.isTriParty())) {
            Double saldoNominal = getNominal(row, inventory);
            if ("CSD".equalsIgnoreCase(mcConfig.getContractType())
                    && Double.compare(saldoNominal, 0d) < 0) {
                return "IM_PROPIO";
            }
            if ("CSD".equalsIgnoreCase(mcConfig.getContractType())
                    && Double.compare(saldoNominal, 0d) > 0) {
                return "IM_CPTY";
            }
            if ("GMRA".equalsIgnoreCase(mcConfig.getAdditionalField("MASTER_AGREEMENT_TYPE"))) {
                return "REPO_TRI";
            }
            return "TRIPARTY";
        }
        return "";
    }

    @SuppressWarnings("unused")
    private JDate getValDate(ReportRow row) {
        JDatetime valDatetime = (JDatetime) row
                .getProperty(ReportRow.VALUATION_DATETIME);
        if (valDatetime == null) {
            valDatetime = new JDatetime();
        }
        JDate valDate = null;
        PricingEnv env = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
        if (env != null) {
            valDate = valDatetime.getJDate(env.getTimeZone());
        } else {
            valDate = valDatetime.getJDate(TimeZone.getDefault());
        }
        return valDate;
    }

    protected Double getCollateralHaircut(Inventory inventory, ReportRow row,
                                          String columnId, @SuppressWarnings("rawtypes") Vector errors) {
        Double mccHairCutStr = 100.0;
        try {
            MarginCallConfig marginCallConfig = BOCache
                    .getMarginCallConfig(DSConnection.getDefault(), inventory.getMarginCallConfigId());
            Product product = inventory.getProduct();
            if ((marginCallConfig != null) && (product != null)) {
                String haircutRuleName = marginCallConfig.getHaircutName();
                if (haircutRuleName != null) {
                    double haircutValue = HaircutUtil.getHaircutValue(haircutRuleName, product, (String) null, null, ((JDate) row.getProperty(END_DATE_PROPERTY)), false, true) * 100;
                    mccHairCutStr -= Math.abs(haircutValue);
                }
                return mccHairCutStr;
            }
        } catch (Exception e) {
            Log.error("Error: ", e);
        }
        return mccHairCutStr;
    }

    private int getHigherRemainingmaturity(String remainingMaturityStr) {
        int higherMaturity = 0;
        if (remainingMaturityStr.indexOf("-") != -1) {
            String higherMaturityStr = remainingMaturityStr
                    .substring(remainingMaturityStr.indexOf("-") + 1);
            if (higherMaturityStr.indexOf("Y") != -1) {
                String temp = higherMaturityStr.substring(0,
                        higherMaturityStr.indexOf("Y"));
                higherMaturity = Integer.parseInt(temp);
            }
        }
        return higherMaturity;
    }

    @Override
    public boolean containsPricingEnvDependentColumns(ReportTemplate template) {
        return true;
    }

    protected SantProductCustomDataReportStyle bondCustomReportStyle = null;

    @Override
    public TreeList getTreeList() {
        if (this._treeList != null) {
            return this._treeList;
        }
        final TreeList treeList = super.getTreeList();
        if (this.bondCustomReportStyle == null) {
            this.bondCustomReportStyle = getProductCustomDataReportStyle();
        }
        if (this.bondCustomReportStyle != null) {
            treeList.add(this.bondCustomReportStyle.getNonInheritedTreeList());
        }

        if (collateralConfigReportStyle == null) {
            collateralConfigReportStyle = getMarginCallConfigReportStyle();
        }

        // add CollateralConfig tree
        if (collateralConfigReportStyle != null) {
            addSubTreeList(treeList, new Vector<String>(),
                    MARGIN_CALL_CONFIG_PREFIX,
                    collateralConfigReportStyle.getTreeList());
        }

        // new columns
        treeList.add(PRICE);
        treeList.add(FX_FIXING);
        treeList.add(MARKET_VALUE);
        treeList.add(MARKET_VALUE_EUR);
        treeList.add(CONCILIATION_PRICE_D);
        treeList.add(CONCILIATION_PRICE);
        treeList.add(CONCILIATION_PRICE_2);
        treeList.add(ROWID);
        treeList.add(JISSUER);

        return treeList;
    }

    protected SantProductCustomDataReportStyle getProductCustomDataReportStyle() {
        try {
            if (this.bondCustomReportStyle == null) {
                String className = "calypsox.tk.report.SantProductCustomDataReportStyle";
                this.bondCustomReportStyle = (SantProductCustomDataReportStyle) InstantiateUtil
                        .getInstance(className, true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.bondCustomReportStyle;
    }

    /**
     * @return custom CollateralConfigReportStyle
     */
    private CollateralConfigReportStyle getMarginCallConfigReportStyle() {
        try {
            if (this.collateralConfigReportStyle == null) {
                String className = "calypsox.tk.report.CollateralConfigReportStyle";

                this.collateralConfigReportStyle = (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil
                        .getInstance(className, true, true);

            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.collateralConfigReportStyle;
    }

    /**
     * @param row
     * @param columnName
     * @param errors
     * @return value of Collateral Config if is a MarginCAllConfig Column
     */
    private Object getMarginCallConfigColumn(ReportRow row, String columnName,
                                             @SuppressWarnings("rawtypes") Vector errors) {

        // Somehow super method isMarginCallConfigColumn returns null.
        // Implemented logic here
        String name = getMarginCallConfigReportStyle()
                .getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);
        return getMarginCallConfigReportStyle().getColumnValue(row, name,
                errors);
    }

    // Is MarginCall trade eligible in its contract
    private boolean isPositionEligible(Inventory inventory,
                                       CollateralConfig contract) {
        boolean isEligible = false;

        if (inventory instanceof InventorySecurityPosition) {
            InventorySecurityPosition inventoryPosition = (InventorySecurityPosition) inventory;
            int securityId = inventoryPosition.getSecurityId();
            Product security = BOCache.getExchangedTradedProduct(
                    DSConnection.getDefault(), securityId);

            isEligible = SantConcentrationLimitsUtil
                    .isSecurityEligible(security, contract);
        }

        return isEligible;
    }


    public double getPriceByDate(ReportRow row, Product product, int business) {

        JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
        if (valDatetime == null) {
            valDatetime = new JDatetime();
        }
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        PricingEnv env = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
        Vector holidays = row.getProperty(HOLIDAYS);

        if (product != null && valDate != null && env != null && holidays != null) {
            return CollateralUtilities.getDirtyPrice(product, valDate.addBusinessDays(business, holidays), env, holidays);
        }

        return 0.0;
    }

    private String getIsPositionEligible(Inventory inventory,
                                         CollateralConfig contract) {
        String value = POSITION_ELIGIBLE_FALSE;

        if (isPositionEligible(inventory, contract)) {
            value = POSITION_ELIGIBLE_TRUE;
        }

        return value;
    }
    // Is MarginCall trade eligible in its contract - End

    private Double getValDateCleanPrice(ReportRow row, Product product, QuoteSet quoteSet, Vector holidays, PricingEnv pricingEnv, JDate valDate) {
        //Added because in the schedule task have a -1 day offset
        if (row.getProperty("ValDateCleanPrice") != null) {
            return row.getProperty("ValDateCleanPrice");
        }
        if (row.getProperty("ValDateCleanPriceINFO") == null) {
            QuoteValue valDateProductQuote = quoteSet.getProductQuote(product, valDate.addBusinessDays(1, holidays), pricingEnv.getName());
            if ((valDateProductQuote != null) && (!Double.isNaN(valDateProductQuote.getClose()))) {
                Double valDateClosePrice = valDateProductQuote.getClose();
                /*si es Equity (ojo el paquete del que sacar Equity) se mult por 100 y sino no*/
                if (!(product instanceof com.calypso.tk.product.Equity)) {
                    valDateClosePrice *= 100;
                }
                row.setProperty("ValDateCleanPrice", valDateClosePrice);
                return valDateClosePrice;
            }
            row.setProperty("ValDateCleanPriceINFO", "ValDateCleanPrice_notFound");
            return null;
        }
        return null;
    }

    private Double getDirtyPrice(ReportRow row, final Inventory inventory, final Product product,
                                 final QuoteSet quoteSet, Vector holidays, PricingEnv pricingEnv, Vector<String> errors) {

        if (row.getProperty(DIRTY_PRICE_PROPERTY) != null) {
            return row.getProperty(DIRTY_PRICE_PROPERTY);
        }

        if (row.getProperty("DIRTY_PRICE_PROPERTY") == null) {
            if (pricingEnv != null && inventory.getProduct() != null) {
                //ojo, mirar fecha valueDate
                JDate quoteDate = row.getProperty(END_DATE_MINUS1_PROPERTY);
                QuoteValue productQuote = quoteSet.getProductQuote(product, quoteDate, pricingEnv.getName());

                if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                    row.setProperty(DIRTY_PRICE_PROPERTY, productQuote.getClose());
                    return productQuote.getClose();
                }
                final String error = "Quote not available for Product ISIN: " + inventory.getProduct().getSecCode("ISIN");
                errors.add(error);
                com.calypso.tk.core.Log.error(this, error);
                row.setProperty("DIRTY_PRICE_INFO", "DIRTYPRICE_notFound");
                return null;
            }
            com.calypso.tk.core.Log.error(this, "error Pricing or product null");
            row.setProperty("DIRTY_PRICE_INFO", "DIRTYPRICE_notFound");
            return null;
        }
        return null;
    }

    private Double getNominal(final ReportRow row, Inventory inventory) {

        if (row.getProperty(NOMINAL_PROPERTY) != null) {
            return row.getProperty(NOMINAL_PROPERTY);
        }

        if (row.getProperty("NOMINAL_PROPERTY_INFO") == null) {
            Map positions = row.getProperty(BOPositionReport.POSITIONS);
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            if (inventory == null || context == null) {
                com.calypso.tk.core.Log.error(this, "Inventory/context not available for row " + row.toString());
                row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
                return null;
            }
            if (inventory instanceof InventorySecurityPosition) {
                Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
                if (Util.isEmpty(datedPositions)) {
                    row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
                    return null;
                }
                row.setProperty(NOMINAL_PROPERTY, InventorySecurityPosition.getTotalSecurity(datedPositions, BOSecurityPositionReport.BALANCE));
                return row.getProperty(NOMINAL_PROPERTY);
            }
            row.setProperty("NOMINAL_PROPERTY_INFO", "NOMINAL_notFound");
            return null;
        }
        return null;
    }

    public Double getMarketValue(final ReportRow row, Inventory inventory, Product product, QuoteSet quoteSet,
                                 Vector holidays, PricingEnv pricingEnv, Vector<String> errors) {

        if (row.getProperty(MARKET_VALUE_PROPERTY) != null) {
            return row.getProperty(MARKET_VALUE_PROPERTY);
        }

        if (row.getProperty("MARKET_VALUE_INFO") == null) {
            Double marketValue = null;

            if (inventory == null) {
                com.calypso.tk.core.Log.error(this, "Inventory/context not available for row " + row.toString());
                row.setProperty("MARKET_VALUE_INFO", "MARKET_VALUE_notFound");
                return null;
            }

            if (product == null) {
                com.calypso.tk.core.Log.info(this, "Product does NOT exist for position " + row.toString());
                row.setProperty("MARKET_VALUE_INFO", "MARKET_VALUE_notFound");
                return null;
            }

            final Double priceQuote = getDirtyPrice(row, inventory, product, quoteSet, holidays, pricingEnv, errors);
            final Double nominal = getNominal(row, inventory);

            if (priceQuote == null || nominal == null) {
                row.setProperty("MARKET_VALUE_INFO", "MARKET_VALUE_notFound");
                return null;
            }

            if (!Double.isNaN(nominal)) {
                marketValue = nominal * priceQuote;
                if (product instanceof Bond) {
                    Bond bond = (Bond) product;
                    marketValue *= bond.getFaceValue();
                }
                row.setProperty(MARKET_VALUE_PROPERTY, marketValue);
                return marketValue;
            }
            row.setProperty("MARKET_VALUE_INFO", "MARKET_VALUE_notFound");
            return null;
        }
        return null;
    }

    private Double getCleanPrice(ReportRow row, Product product, QuoteSet quoteSet, PricingEnv pricingEnv, JDate valDate) {

        if (row.getProperty("CleanPrice") != null) {
            return row.getProperty("CleanPrice");
        }

        if (row.getProperty("CleanPriceINFO") == null) {
            QuoteValue productQuote = quoteSet.getProductQuote(product, valDate, pricingEnv.getName());
            if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                Double closePrice = productQuote.getClose();
                closePrice *= 100;
                row.setProperty("CleanPrice", closePrice);
                return closePrice;
            }
            row.setProperty("CleanPriceINFO", "CleanPrice_notFound");
            return null;
        }
        return null;
    }

    private Double getCleanPriceValue(ReportRow row, Product product, QuoteSet quoteSet, PricingEnv pricingEnv, JDate valDate) {
        if (row.getProperty("CleanPrice_Value") != null) {
            return row.getProperty("CleanPrice_Value");
        }


        if (row.getProperty("CleanPriceValueINFO") == null) {
            Double cleanPrice = getCleanPrice(row, product, quoteSet, pricingEnv, valDate);
            if (cleanPrice != null) {
                Double closePrice = (cleanPrice / 100);
                Map positions = row.getProperty(BOPositionReport.POSITIONS);
                Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(valDate);
                if (!Util.isEmpty(datedPositions)) {
                    Double total = InventorySecurityPosition.getTotalSecurity(datedPositions, "Balance");
                    if ((closePrice != null) && !Double.isNaN(total) && (product instanceof Bond)) {
                        Bond bond = (Bond) product;
                        Double cleanPriceValue = total * closePrice * bond.getFaceValue();
                        row.setProperty("CleanPrice_Value", cleanPriceValue);
                        return cleanPriceValue;
                    }
                    row.setProperty("CleanPriceValueINFO", "CleanPriceValue_notFound");
                    return null;
                }
                row.setProperty("CleanPriceValueINFO", "CleanPriceValue_notFound");
                return null;
            }
            row.setProperty("CleanPriceValueINFO", "CleanPriceValue_notFound");
            return null;
        }
        return null;
    }

    private Double getBodnClosingPrice(ReportRow row, Product product, QuoteSet quoteSet, PricingEnv pricingEnv, JDate valDate) {
        if (row.getProperty("Bodn_Closing_Price") != null) {
            return row.getProperty("Bodn_Closing_Price");
        }
        if (row.getProperty("BodnClosingPriceINFO") == null) {
            Double cleanPrice = getCleanPrice(row, product, quoteSet, pricingEnv, valDate);
            if (cleanPrice != null) {
                Double closePrice = (cleanPrice / 100);
                if (product instanceof Bond) {
                    row.setProperty("Bodn_Closing_Price", closePrice);
                    return closePrice;
                }
                row.setProperty("BodnClosingPriceINFO", "BodnClosingPrice_notFound");
                return null;
            }
            row.setProperty("BodnClosingPriceINFO", "BodnClosingPrice_notFound");
            return null;
        }
        return null;
    }

    private void buildMarketValueEURAndFXFix(ReportRow row, SantGenericQuotesLoader quotesLoader, Inventory inventory,
                                             final Product product, QuoteSet quoteSet, Vector holidays, PricingEnv pricingEnv, Vector errors) {

        if (row.getProperty("FX_RATE_NAME_INFO") == null && row.getProperty("MARKET_VALUE_EUR_INFO") == null) {
            Double marketValue = getMarketValue(row, inventory, product, quoteSet, holidays, pricingEnv, errors);
            if (marketValue == null || inventory == null || product == null) {
                row.setProperty("FX_RATE_NAME_INFO", "FxRateName_notFound");
                row.setProperty("MARKET_VALUE_EUR_INFO", "MarketValueEUR_notFound");
                return;
            }

            //market value fixing for products which ccy is different to EUR
            if (product.getCurrency().equals(BOSecurityPositionReport.EUR_NAME)) {
                row.setProperty(FX_RATE_NAME_PROPERTY, BOSecurityPositionReport.EUR_NAME);
                row.setProperty(MARKET_VALUE_EUR_PROPERTY, marketValue);
            } else {
                //marketValue in EUR, we require the daily (D-1) fixing
                final QuoteValue qvFXfix = quotesLoader.fetchFXQuoteValue(BOSecurityPositionReport.EUR_NAME, product.getCurrency());
                if (QuoteValue.isNull(qvFXfix) || QuoteValue.isNull(qvFXfix.getClose())) {
                    com.calypso.tk.core.Log.error(this, "Not FX Fixing found for FX.EUR." + product.getCurrency());
                    row.setProperty("FX_RATE_NAME_INFO", "FxRateName_notFound");
                    row.setProperty("MARKET_VALUE_EUR_INFO", "MarketValueEUR_notFound");
                    return;
                }

                final String fxFixing = buildFXName(product, qvFXfix);
                row.setProperty(FX_RATE_NAME_PROPERTY, fxFixing);

                //precio*nominal* D-1 FX.EUR fixing
                Double priceValueEur = marketValue * qvFXfix.getClose();
                row.setProperty(MARKET_VALUE_EUR_PROPERTY, priceValueEur);
            }
        }
    }


    private String buildFXName(final Product product, final QuoteValue qvFix) {

        return "FX.EUR." + product.getCurrency() + "=" + qvFix.getClose();
    }

    private void initQuotesLoader(ReportRow row, PricingEnv pricingEnv) {
        quotesLoader = new SantGenericQuotesLoader(true, buildFXWhereClause(row, pricingEnv));
        // start threads
        quotesLoader.load();
        // join: wait all threads till last one finishes
        try {
            quotesLoader.join();
        } catch (InterruptedException e) {
            com.calypso.tk.core.Log.error(this, e);
        }
    }

    private List<String> buildFXWhereClause(ReportRow row, PricingEnv pricingEnv) {

        final String priceEnvironment = pricingEnv.getName();
        final JDate quoteDate = row.getProperty(END_DATE_MINUS1_PROPERTY);
        //two calls might seem weird: in case FX are in other env than OFFICIAL we recover it from here. Otherwise from official
        //in case FX in in both env it will prevails the configured one.
        final String body = " and quote_name like 'FX.%' and length(quote_name) = 10 and TRUNC(quote_date) = ";
        final StringBuilder specificPE = new StringBuilder(" quote_set_name= '").append(priceEnvironment).append("'").append(body).append(
                Util.date2SQLString(quoteDate));

        final StringBuilder officialPE = new StringBuilder(" quote_set_name= 'OFFICIAL'").append(body).append(
                Util.date2SQLString(quoteDate));
        return Arrays.asList(new String[]{specificPE.toString(), officialPE.toString()});
    }

    /**
     * Method search the external reference issuer of the product
     *
     * @param product
     * @return
     */
    private static String getJIssuer(Product product) {

        Equity equity = (Equity) product;
        return null!=product ? equity.getIssuer().getExternalRef() : "";
    }

    /**
     * Calculate Nominal factorado para los Informes Internos
     * @param row
     * @return
     */
    public static Double getInformesInternosNominal(ReportRow row) {
        Double nominal = null;
        if (row.getProperty("INFORMES_INT_NOMINAL_PROPERTY_INFO") == null && row.getProperty(INFORMES_INT_NOMINAL_PROPERTY) == null) {
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            Map positions = row.getProperty(BOPositionReport.POSITIONS);
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(context.endDate);
            if (Util.isEmpty(datedPositions)) {
                row.setProperty("INFORMES_INT_NOMINAL_PROPERTY_INFO", "InformesInternosNominal_Not_Calculated");
            }
            nominal = InventorySecurityPosition.getTotalSecurity(datedPositions, "Balance");
            final Product product = inventory.getProduct();
            if (product instanceof Bond) {
                Bond bond = (Bond) product;
                nominal *= bond.getFaceValue();
                if (bond.isSinkingBond()){
                    double notionalAmount = getNotionalAmount(bond,context.endDate);
                    if (notionalAmount != 0){
                        nominal *= notionalAmount/100;
                    }
                }
            }
            if (nominal != null) {
                row.setProperty(INFORMES_INT_NOMINAL_PROPERTY, nominal);
                row.setProperty("INFORMES_INT_NOMINAL_PROPERTY_INFO", "InformesInternosNominal_OK");
            }
        }
        if (nominal == null) {
            row.setProperty("INFORMES_INT_NOMINAL_PROPERTY_INFO", "InformesInternosNominal_Not_Calculated");
        }

        return row.getProperty(INFORMES_INT_NOMINAL_PROPERTY);
    }

    public static double getNotionalAmount(Bond bond, JDate endDate) {
        if ("Notional Percent".equalsIgnoreCase(bond.getNotionalType())) {
            Vector notionalDates = bond.getAmortSchedule();
            Collections.sort(notionalDates, (Comparator<NotionalDate>) (o1, o2) -> {
                if (o1.getStartDate() == null || o2.getStartDate() == null)
                    return 0;
                return o2.getStartDate().compareTo(o1.getStartDate());
            });
            for (Object notionalDate : notionalDates) {
                if (((NotionalDate) notionalDate).getStartDate().before(endDate) || (((NotionalDate) notionalDate).getStartDate().equals(endDate))) {
                    return ((NotionalDate) notionalDate).getNotionalAmt();
                }
            }
        }
        return 0;
    }


    /**
     * Calculate Nominal factorado para los Informes Internos
     * @param row
     * @return
     */
    public static Double getInformesInternosMarketValue(ReportRow row) {
        // INFORMES_INT_MVALUE_PROPERTY = "InformesInternosMarketValue";
        if (row.getProperty("INFORMES_INT_MVALUE_PROPERTY_INFO") == null && row.getProperty(INFORMES_INT_MVALUE_PROPERTY) == null) {
            Double mValueDateValue = null;
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            Double nominalInformes = getInformesInternosNominal(row);
            QuoteSet quoteSet = row.getProperty(QUOTE_SET_PROPERTY);
            JDate quoteDate = row.getProperty(END_DATE_PROPERTY);
            PricingEnv pEnv = row.getProperty(PRICING_ENV_PROPERTY);
            if (quoteDate != null) {
                QuoteValue productQuote = quoteSet.getProductQuote(inventory.getProduct(), quoteDate, pEnv.getName());
                if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                    double price = productQuote.getClose();
                    mValueDateValue = (nominalInformes * price);
                    row.setProperty(INFORMES_INT_MVALUE_PROPERTY, mValueDateValue);
                    row.setProperty("INFORMES_INT_MVALUE_PROPERTY_INFO", "InformesInternosMarketValue_OK");
                }
            }
            if (mValueDateValue == null) {
                row.setProperty("INFORMES_INT_MVALUE_PROPERTY_INFO", "InformesInternosMarketValue_Not_Calculated");
            }
        }
        return row.getProperty(INFORMES_INT_MVALUE_PROPERTY);
    }

    public static Double getInformesInternosMarketValueEURNoFixing(ReportRow row) {
        if (row.getProperty("INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY_INFO") == null && row.getProperty(INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY) == null) {
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            DisplayValue fxRate = getInformesInternosFXRate(row, inventory);
            Double mValueDateValue = getInformesInternosMarketValue(row);
            if (mValueDateValue != null && fxRate != null) {
                Double mValueDateValueEUR = mValueDateValue * fxRate.get();
                row.setProperty(INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY, mValueDateValueEUR);
                row.setProperty("INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY_INFO", "InformesInternosMValueEUR_OK");
            } else {
                row.setProperty("INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY_INFO", "InformesInternosMValueEUR_Not_Calculated");
            }
        }
        return row.getProperty(INFORMES_INT_MVALUE_EUR_NOFIXING_PROPERTY);
    }

    public static DisplayValue getInformesInternosFXRate(ReportRow row, Inventory inventory) {

        if (row.getProperty("INFORMES_INT_FXRATE_PROPERTY_INFO") == null && row.getProperty(INFORMES_INT_FXRATE_PROPERTY) == null) {
            SpecificInventoryPositionValues.SpecificInventoryPositionValueContext posContext = (SpecificInventoryPositionValues.SpecificInventoryPositionValueContext) row.getProperty("SpecificInventoryPosition");
            SpecificInventorySecurityPositionValues specificInventorySecurityPositionValues = (SpecificInventorySecurityPositionValues) row.getProperty("SpecificInventoryPositionValue");
            Double v = specificInventorySecurityPositionValues.getPositionValue(inventory, "FXRate", posContext);
            DisplayValue fxRate =  new Amount(v, inventory.getProduct().getNominalDecimals(inventory.getProduct().getCurrency()));
            if (fxRate!= null) {
                row.setProperty(INFORMES_INT_FXRATE_PROPERTY, fxRate);
                row.setProperty("INFORMES_INT_FXRATE_PROPERTY_INFO", "InformesInternosFXRate_OK");
            } else {
                row.setProperty("INFORMES_INT_FXRATE_PROPERTY_INFO", "InformesInternosFXRate_Not_Calculated");
            }
        }
        return row.getProperty(INFORMES_INT_FXRATE_PROPERTY);
    }

    public static Double getInformesInternosPrecio(ReportRow row) {
        if (row.getProperty("INFORMES_INT_PRECIO_PROPERTY_INFO") == null && row.getProperty(INFORMES_INT_PRECIO_PROPERTY) == null) {
            Double showPrice = null;
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            JDate endDate = row.getProperty(END_DATE_PROPERTY);
            PricingEnv pEnv = row.getProperty(PRICING_ENV_PROPERTY);
            if (endDate != null) {
                // add 1 day to get price in D
                JDate priceDate = endDate.addBusinessDays(1, row.getProperty(BOSecurityPositionReportStyle.HOLIDAYS));
                showPrice = CollateralUtilities.getDirtyPrice(inventory.getProduct(), priceDate, pEnv, row.getProperty(BOSecurityPositionReportStyle.HOLIDAYS));
                row.setProperty(INFORMES_INT_PRECIO_PROPERTY, showPrice);
                row.setProperty("INFORMES_INT_PRECIO_PROPERTY_INFO", "InformesInternosPrecio_OK");
            }
            if (showPrice == null) {
                row.setProperty("INFORMES_INT_PRECIO_PROPERTY_INFO", "InformesInternosPrecio_Not_Calculated");
            }
        }
        return row.getProperty(INFORMES_INT_PRECIO_PROPERTY);
    }


}

