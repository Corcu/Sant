package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.tk.report.SACCRInventoryPositionReportTemplate.*;


public class SACCRInventoryPositionReportStyle extends SantCollateralBOPositionReportStyle {


    private com.calypso.tk.report.CollateralConfigReportStyle collateralConfigReportStyle = null;
    private com.calypso.tk.report.BondReportStyle bondReportStyle = null;
    private com.calypso.tk.report.EquityReportStyle equityReportStyle = null;


    private final MarginCallEntryReportStyle mcEntryStyle = new MarginCallEntryReportStyle();
    private final ProductReportStyle productStyle = new ProductReportStyle();
    private final BOSecurityPositionReportStyle secReportStyle = new BOSecurityPositionReportStyle();
    private final BOCashPositionReportStyle cashReportStyle = new BOCashPositionReportStyle();
    private AccountReportStyle accountReportStyle = new AccountReportStyle();


    public static String COLLATERAL_CONFIG_PREFIX = "MarginCallConfig.";
    public static String ACCOUNT_PREFIX = "Account.";
    private static String MC_ENTRY_PREFIX = "MarginCallEntry.";
    private static String PRODUCT_PREFIX = "Product.";
    private static Integer DECIMALS = SACCRCMPositionReport.decimalsPositions4Number();

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        final CalypsoTreeNode saccrTreeNode = new CalypsoTreeNode("SACCRInventoryPosition");
        treeList.add(saccrTreeNode);
        final String[] columns = SACCRInventoryPositionReportTemplate.DEFAULT_COLUMNS;
        for (String column : columns) {
            treeList.add(saccrTreeNode, column);
        }
        return treeList;
    }

    /**
     * Override method to get columns values for the style
     */
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors)
            throws InvalidParameterException {

        Object valueCol = null;
        CollateralConfig config = row.getProperty("MarginCallConfig");
        Inventory position = row.getProperty("Inventory");
        Account account = row.getProperty("Account");
        JDate valDate = ((JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME)).getJDate(TimeZone.getDefault());
        MarginCallEntry entry = (MarginCallEntry) row.getProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY);
        String positionCcy = UtilReport.getCurrencyPair(position.getSettleCurrency());

        //custom columns names
        if (columnName.equals(COLLATERAL_CONFIG_TYPE)) {
            return getCollateralConfigTypeLogic(config);

        } else if (columnName.equals("Currency")) {
            return positionCcy;
        } else if (columnName.equals(SACCRInventoryPositionReportTemplate.DEAL_ID_LAGO)) {
            return (config.getLongId() + config.getCurrency());

        } else if (columnName.equals(COLLATERAL_MOVEMENT_TYPE)) {
            return row.getProperty(COLLATERAL_MOVEMENT_TYPE);

        } else if (columnName.equals(COLLATERAL_PROCESS_DATE)) {
            return row.getProperty(COLLATERAL_PROCESS_DATE);

        } else if (columnName.equals(COLLATERAL_VALUE_DATE)) {
            return row.getProperty(COLLATERAL_VALUE_DATE);

        } else if (columnName.equals(COLLATERAL_MATURITY_DATE)) {
            return row.getProperty(COLLATERAL_MATURITY_DATE);

        } else if (columnName.equals("MarginCallEntry.MarginCallConfig.ADDITIONAL_FIELD.SEGREGATED_COLLATERAL") || columnName.equals("MarginCallEntry.ADDITIONAL_FIELD.SEGREGATED_COLLATERAL")) {
            return getMarginCallConfigColumn(row, "MarginCallConfig.ADDITIONAL_FIELD.SEGREGATED_COLLATERAL", errors);

        } else if (columnName.equals(CCP_PLATFORM)) {
            return Util.isEmpty((String) getMarginCallConfigColumn(row, "MarginCallConfig.ADDITIONAL_FIELD.CCP", errors)) ? "N" : "Y";

        } else if (columnName.equals("Haircut")) {
            Double hc = getCollateralHaircut(position, row, columnName, errors);
            if (!hc.isNaN()) {
                return hc - 100;
            }
            return 0;

        } else if (columnName.equals(MARGIN_TYPE)) {
            return row.getProperty(MARGIN_TYPE);

        } else if (columnName.equals("Description")) {
            return getProdutctDescription(position);

        } else if (columnName.equals("Contract Value")) {
            double value = getContractValueLogic(position, row, valDate, errors);
            return new Amount(value, config.getCurrency());

        } else if (columnName.equals(SEGREGATED_COLLATERAL)) {
            return ""; //TODO

        } else if (columnName.equals("FX Rate")) {
            return row.getProperty("FX RATE");

        } else if (columnName.equals("IM Amount")) {
            return row.getProperty("IM_AMOUNT");

        } else if (columnName.equals("Short (Nominal)")) {
            Amount nominal = getNominal(valDate, row, errors);
            return nominal.get() < 0 ? nominal : "";

        } else if (columnName.equals("Long (Nominal)")) {
            Amount nominal = getNominal(valDate, row, errors);
            return nominal.get() > 0 ? nominal : "";

        } else if (columnName.equals("Short (Quantity)")) {
            Amount quantity = getQuantityValue(position, row, valDate, positionCcy, errors);
            return quantity.get() < 0 ? quantity : "";

        } else if (columnName.equals("Long (Quantity)")) {
            Amount quantity = getQuantityValue(position, row, valDate, positionCcy, errors);
            return quantity.get() > 0 ? quantity : "";

        } else if (columnName.equals("Quantity")) {
            return getQuantityValue(position, row, valDate, positionCcy, errors);

        } else if (columnName.equals("Status")) {
            if (entry != null)
                return entry.getCollateralizationStatus();

        } else if (columnName.equals("Sant Dirty Price") || columnName.equals("Clean Price") || columnName.equals("Quote Price")) {
            if (position instanceof InventoryCashPosition) {
                return "";
            } else if (position instanceof InventorySecurityPosition) {
                double price = getPrice(position.getProduct(), valDate, row);
                return new Amount(price, 8);
            }

        }

        //Collateral Config columns names
        else if (getMarginCallConfigReportStyle().isMarginCallConfigColumn(COLLATERAL_CONFIG_PREFIX, columnName)) {
            if (columnName.equals(SOURCE_SYSTEM)) {
                //control to ensure is included due to it's importance - mandatory
                final String sourceSystem = (String) getMarginCallConfigColumn(row, columnName, errors);
                if (!Util.isEmpty(sourceSystem)) {
                    return sourceSystem;
                } else {
                    errors.add("ERROR: Source system attribute is not configured for PO: " + config.getProcessingOrg().getCode());
                    return "";
                }
            } else if ("MarginCallConfig.Book".equals(columnName)) {
                if (null != position) {
                    return BOCache.getBook(DSConnection.getDefault(), position.getBookId());
                }
            }
            return getMarginCallConfigColumn(row, columnName, errors);
        }

        //Accounts columns
        final String accountColumnName = AccountReportStyle.getColumnName(ACCOUNT_PREFIX, columnName);
        if (!Util.isEmpty(accountColumnName) && null != account) {
            valueCol = accountReportStyle.getColumnValue(row, accountColumnName, errors);
            if (null != valueCol)
                return valueCol;
        }

        //cash positions style
        else if (position instanceof InventoryCashPosition) {
            if (columnName.equals("Type")) {
                return "Cash";
            }

            if (columnName.equals(COLLATERAL_IN_TRANSIT)) {
                if (entry != null) {
                    Double transit = entry.getPreviousNotSettledCashMargin();
                    return new Amount(round(transit, DECIMALS), DECIMALS);
                }
            }

            if (columnName.equals("Nominal") || columnName.equals("Value")) {
                return getNominal(valDate, row, errors);
            }

            valueCol = cashReportStyle.getColumnValue(row, columnName, errors);

            if (valueCol != null) return valueCol;

        }
        //security positions style
        else if (position instanceof InventorySecurityPosition) {
            if (columnName.equals("Type")) {
                return "Security";
            }
            if (columnName.equals(COLLATERAL_IN_TRANSIT)) {
                if (entry != null) {
                    double transit = entry.getPreviousNotSettledSecurityMargin();
                    return new Amount(round(transit, DECIMALS), DECIMALS);
                }
            }
            if (columnName.equals("Value")) {
                double value = getSecurityValueLogic(row, (InventorySecurityPosition) position, valDate, errors);
                return new Amount(value, positionCcy);
            }
            if (columnName.equals("Nominal")) {
                return getNominal(valDate, row, errors);
            }
            //core Securities
            valueCol = secReportStyle.getColumnValue(row, columnName, errors);

            if (valueCol != null)
                return valueCol;

            //finally try Product Style
            final String newProductColumnName = ProductReportStyle.getColumnName(PRODUCT_PREFIX, columnName);
            if (!Util.isEmpty(newProductColumnName) && null != getProductFromRow(row)) {
                valueCol = productStyle.getColumnValue(row, newProductColumnName, errors);
                if (valueCol != null)
                    return valueCol;
            }
            valueCol = getUnderlyingColumnValue(position.getProduct(), row, columnName, errors);
        }
        //finally, try MCEntry Style
        if (valueCol == null) {
            //no value return and try MC entry column
            final String newMCColumnName = ReportStyle.getColumnName(MC_ENTRY_PREFIX, columnName);
            MarginCallEntryDTO entryDTO = row.getProperty("MarginCallEntryDTO");
            if ((!Util.isEmpty(newMCColumnName) || columnName.equals(MARGIN_CALL_ENTRY_DIRECTION)) && null != entryDTO) {
                ReportRow rowClone = row.clone();
                rowClone.setProperty("Default", row.getProperty("MarginCallEntryDTO"));

                if (columnName.equals(MARGIN_CALL_ENTRY_DIRECTION)) {
                    final String directionLogic = (String) mcEntryStyle.getColumnValue(rowClone, "Direction", errors);
                    if (!Util.isEmpty(directionLogic)) {
                        return directionLogic.equals("Pay") ? "1" : "0";
                    }
                } else {
                    return mcEntryStyle.getColumnValue(rowClone, newMCColumnName, errors);
                }
            }
        }
        return valueCol;
    }

    public static Double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * @param row
     * @param columnName
     * @param errors
     * @return value of Collateral Config if is a MarginCAllConfig Column
     */
    private Object getMarginCallConfigColumn(ReportRow row, String columnName, Vector errors) {
        //Somehow super method isMarginCallConfigColumn returns null. Implemented logic here
        String name = getMarginCallConfigReportStyle().getRealColumnName(COLLATERAL_CONFIG_PREFIX, columnName);
        return getMarginCallConfigReportStyle().getColumnValue(row, name, errors);
    }

    /**
     * @return custom CollateralConfigReportStyle. If custom code is not found, it will retrieve the custom version
     */
    private com.calypso.tk.report.CollateralConfigReportStyle getMarginCallConfigReportStyle() {
        try {
            if (this.collateralConfigReportStyle == null) {
                String className = "calypsox.tk.report.CollateralConfigReportStyle";
                this.collateralConfigReportStyle = (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
                        true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.collateralConfigReportStyle;
    }

    /**
     * @return custom AccountReportStyle. If custom code is not found, it will retrieve the custom version
     */
    public AccountReportStyle getAccountReportStyle() {
        try {
            if (this.accountReportStyle == null) {
                String className = "calypsox.tk.report.AccountReportStyle";
                this.accountReportStyle = (calypsox.tk.report.AccountReportStyle) InstantiateUtil.getInstance(className,
                        true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.accountReportStyle;
    }

    private void loadBondReportStyle() {
        try {
            if (this.bondReportStyle == null) {
                String className = "calypsox.tk.report.BondReportStyle";
                this.bondReportStyle = (calypsox.tk.report.BondReportStyle) InstantiateUtil.getInstance(className,
                        true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
    }

    private void loadEquityReportStyle() {
        try {
            if (this.equityReportStyle == null) {
                String className = "calypsox.tk.report.EquityReportStyle";
                this.equityReportStyle = (calypsox.tk.report.EquityReportStyle) InstantiateUtil.getInstance(className,
                        true, true);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
    }

    private Product getProductFromRow(ReportRow row) {
        Product product = row.getProperty("INV_PRODUCT");
        if (null == product) {
            product = row.getProperty("Product");
        }
        return product;
    }

    public void precalculateColumnValues(ReportRow row, String[] columns, Vector errors) {
        Inventory pos = row.getProperty("Inventory");
        if (pos instanceof InventoryCashPosition) {
            return;
        } else {
            super.precalculateColumnValues(row, columns, errors);
        }
    }

    private String getCollateralConfigTypeLogic(CollateralConfig config) {
        final String collateralMarginType = config.getContractType();
        if (!Util.isEmpty(collateralMarginType)) {
            if (collateralMarginType.equals("CSD")) {
                return "IM";
            }
        }
        return collateralMarginType;
    }

    private double getSecurityValueLogic(ReportRow row, InventorySecurityPosition position, JDate valDate, Vector errors) {
        Amount nominal = getNominal(valDate, row, errors);
        if (position.getProduct() instanceof Bond) {
            Bond bond = (Bond) position.getProduct();
            return nominal.get() * (getPrice(bond, valDate, row) / 100);
        }
        if (position.getProduct() instanceof Equity) {
            Equity equity = (Equity) position.getProduct();
            return nominal.get() * (getPrice(equity, valDate, row));
        }
        return 0;
    }

    private double getContractValueLogic(Inventory pos, ReportRow row, JDate valDate, Vector errors) {
        double hc = getCollateralHaircut(pos, row, null, errors);
        Amount fx = row.getProperty("FX RATE");
        if (null == fx) fx = new Amount(1);
        if (pos instanceof InventoryCashPosition) {
            return getNominal(valDate, row, errors).get() * fx.get() * hc / 100;

        } else if (pos instanceof InventorySecurityPosition) {
            double value = getSecurityValueLogic(row, (InventorySecurityPosition) pos, valDate, errors);
            return value * fx.get() * hc / 100;
        }
        return 0;
    }

    private double getPrice(Product product, JDate valDate, ReportRow row) {
        PricingEnv pricingEnv = row.getProperty("PricingEnv");
        if (null == pricingEnv) {
            pricingEnv = PricingEnv.loadPE("DirtyPrice", valDate.getJDatetime());
        }
        //price on valDate
        return CollateralUtilities.getQuotePriceWithParentQuoteSet(product, valDate, pricingEnv);
    }

    private Amount getQuantityValue(Inventory pos, ReportRow row, JDate valDate, String positionCcy, Vector errors) {

        String columnDate = Util.dateToMString(valDate, Locale.getDefault());
        Amount amount = ((Amount) super.getColumnValue(row, columnDate, errors));
        if (pos instanceof InventorySecurityPosition) {
            if (pos.getProduct() instanceof Bond) {
                Bond bond = (Bond) pos.getProduct();
                return new Amount(amount.get() / bond.getFaceValue(), positionCcy);
            }
        }
        return amount;
    }

    private String getProdutctDescription(Inventory pos) {
        if (pos instanceof InventoryCashPosition) {
            return ((InventoryCashPosition) pos).getCurrency();
        }
        if (pos instanceof InventorySecurityPosition) {
            return pos.getProduct().getDescription();
        }
        return null;
    }

    private Object getUnderlyingColumnValue(Product product, ReportRow row, String columnName, Vector errors) {

        if (product instanceof Bond) {
            if (null == this.bondReportStyle) loadBondReportStyle();
            return this.bondReportStyle.getColumnValue(row, columnName, errors);
        }
        if (product instanceof Equity) {
            if (null == this.equityReportStyle) loadEquityReportStyle();
            return this.equityReportStyle.getColumnValue(row, columnName, errors);
        }
        return null;
    }

    private Amount getNominal(JDate valDate, ReportRow row, Vector errors) {
        Inventory pos = row.getProperty("Inventory");
        Double ccyPairFx = UtilReport.getFXCurrencyPair(pos.getSettleCurrency());
        String columnDate = Util.dateToMString(valDate, Locale.getDefault());
        Amount amount = ((Amount) super.getColumnValue(row, columnDate, errors));
        if (null != amount){
            amount.set(amount.get() * ccyPairFx);
            amount.setDigit(2);
        }
        return amount;
    }
}
