/**
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashPositionFacade;
import com.calypso.tk.collateral.MarginCallPositionFacade;
import com.calypso.tk.collateral.SecurityPositionFacade;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.util.Vector;

import static calypsox.tk.report.SACCRCMPositionReportTemplate.*;

/**
 * Style SA CCR BALANCES. Extends all columns for positions (cash & securities), Collateral Configs and MarginCall Entries
 *
 * @author Guillermo Solano
 *
 * @version 1.01
 * @Date 02/01/2017
 */
public class SACCRCMPositionReportStyle extends MarginCallPositionReportStyle {

    /**
     * Seriel UID
     */
    private static final long serialVersionUID = -7786686999420220098L;


    /**
     * Collateral Config Style
     */
    private CollateralConfigReportStyle collateralConfigReportStyle = null;

    /**
     * Margin Call entry Style
     */

    private final MarginCallEntryReportStyle mcEntryStyle = new MarginCallEntryReportStyle();

    /**
     * Product Style, info for bond & Equities
     */
    private final ProductReportStyle productStyle = new ProductReportStyle();

    /**
     * Prefixes to identify a core styles
     */
    private static String CALLATERAL_CONFIG_PREFIX = "MarginCallConfig.";

    private static String MC_ENTRY_PREFIX = "MarginCallEntry.";

    private static String PRODUCT_PREFIX = "Product.";

    /**
     * decimals
     */
    private static Integer DECIMALS = SACCRCMPositionReport.decimalsPositions4Number();

    /**
     * Override method to get columns values for the style
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
            throws InvalidParameterException {


        Object valueCol = null;

        //custom columns names
        if (columnName.equals(COLLATERAL_CONFIG_TYPE)) {
            return row.getProperty(COLLATERAL_CONFIG_TYPE);

        } else if (columnName.equals(COLLATERAL_MOVEMENT_TYPE)) {
            return row.getProperty(COLLATERAL_MOVEMENT_TYPE);

        } else if (columnName.equals(COLLATERAL_PROCESS_DATE)) {
            return row.getProperty(COLLATERAL_PROCESS_DATE);

        } else if (columnName.equals(COLLATERAL_VALUE_DATE)) {
            return row.getProperty(COLLATERAL_VALUE_DATE);

        } else if (columnName.equals(COLLATERAL_MATURITY_DATE)) {
            return row.getProperty(COLLATERAL_MATURITY_DATE);

        } else if (columnName.equals(CCP_PLATFORM)) {
            return Util.isEmpty((String) getMarginCallConfigColumn(row, "MarginCallConfig.ADDITIONAL_FIELD.CCP", errors)) ? "N" : "Y";

        } else if (columnName.equals(MARGIN_TYPE)) {
            return row.getProperty(MARGIN_TYPE);
        } else if (columnName.equals(SEGREGATED_COLLATERAL)) {
            return ""; //TODO

            //Collateral Config columns names
        } else if (getMarginCallConfigReportStyle().isMarginCallConfigColumn(CALLATERAL_CONFIG_PREFIX, columnName)) {

            if (columnName.equals(SOURCE_SYSTEM)) {
                //control to ensure is included due to it's importance - mandatory
                final String sourceSystem = (String) getMarginCallConfigColumn(row, columnName, errors);
                if (!Util.isEmpty(sourceSystem)) {
                    return sourceSystem;
                } else {
                    CollateralConfig config = row.getProperty("MarginCallConfig");
                    errors.add("ERROR: Source system attribute is not configured for PO: " + config.getProcessingOrg().getCode());
                    return "";
                }
            } else if ("MarginCallConfig.Book".equals(columnName)) {
                MarginCallPositionFacade position = row.getProperty(SACCRCMPositionReportTemplate.MC_POSITION);
                if (position.getBookId() > 0) {
                    return BOCache.getBook(DSConnection.getDefault(), position.getBookId());
                }
            }
            //check is collateral Config
            return getMarginCallConfigColumn(row, columnName, errors);
        }

        MarginCallPositionFacade position = row.getProperty(SACCRCMPositionReportTemplate.MC_POSITION);

        //cash positions style
        if (position instanceof CashPositionFacade) {

            if (columnName.equals(COLLATERAL_IN_TRANSIT)) {
                MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY);
                if (entry != null) {
                    Double transit = entry.getPreviousNotSettledCashMargin();
                    return new Amount(round(transit, DECIMALS), DECIMALS);
                }
            }
            //core call
            valueCol = super.getColumnValue(row, columnName, errors);
            if (valueCol != null)
                return valueCol;

            //security positions style
        } else if (position instanceof SecurityPositionFacade) {

            if (columnName.equals(COLLATERAL_IN_TRANSIT)) {

                MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY);
                if (entry != null) {
                    Double transit = entry.getPreviousNotSettledSecurityMargin();
                    return new Amount(round(transit, DECIMALS), DECIMALS);
                }
            }

            //core Securities
            valueCol = super.getColumnValue(row, columnName, errors);
            if (valueCol != null)
                return valueCol;

            //finally try Product Style
            final String newProductColumnName = ProductReportStyle.getColumnName(PRODUCT_PREFIX, columnName);
            if (!Util.isEmpty(newProductColumnName)) {
                return productStyle.getColumnValue(row, newProductColumnName, errors);
            }
        }
        //finally, try MCEntry Style
        if (valueCol == null) {

            //no value return and try MC entry column
            final String newMCColumnName = ReportStyle.getColumnName(MC_ENTRY_PREFIX, columnName);

            if (!Util.isEmpty(newMCColumnName) || columnName.equals(MARGIN_CALL_ENTRY_DIRECTION)) {
                /*
                 * Has same attribute main MCPosition, so it's re-assigned in this section
                 */
                ReportRow rowClone = row.clone();
                MarginCallEntryDTO entry = (MarginCallEntryDTO) row.getProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY);
                rowClone.setProperty("Default", entry);

                if (columnName.equals(MARGIN_CALL_ENTRY_DIRECTION)) {

                    final String directionLogic = (String) mcEntryStyle.getColumnValue(rowClone, "Direction", errors);
                    if (!Util.isEmpty(directionLogic)) {
                        return directionLogic.equals("Pay") ? "1" : "0";
                    }
                } else
                    return mcEntryStyle.getColumnValue(rowClone, newMCColumnName, errors);

            }

        }

        return null;

    }

    /**
     * Recovers the tree list (columns for the style). Adds new columns and Collateral Config & MC Entry styles + custom columns
     */
    @SuppressWarnings("deprecation")
    @Override
    public TreeList getTreeList() {

        final TreeList treeList = super.getTreeList();

        if (collateralConfigReportStyle == null) {
            collateralConfigReportStyle = getMarginCallConfigReportStyle();
        }

        //add CollateralConfig tree
        if (collateralConfigReportStyle != null) {
            addSubTreeList(treeList, new Vector<String>(), CALLATERAL_CONFIG_PREFIX, collateralConfigReportStyle.getTreeList());
        }


        if (mcEntryStyle != null) {
            addSubTreeList(treeList, new Vector<String>(), MC_ENTRY_PREFIX, mcEntryStyle.getTreeList());
        }

        if (productStyle != null) {
            addSubTreeList(treeList, new Vector<String>(), PRODUCT_PREFIX, productStyle.getTreeList());
        }

        //new columns
        treeList.add(COLLATERAL_CONFIG_TYPE);
        treeList.add(COLLATERAL_MOVEMENT_TYPE);
        treeList.add(COLLATERAL_PROCESS_DATE);
        treeList.add(COLLATERAL_VALUE_DATE);
        treeList.add(COLLATERAL_MATURITY_DATE);
        treeList.add(MARGIN_CALL_ENTRY_DIRECTION);
        treeList.add(COLLATERAL_IN_TRANSIT);
        treeList.add(SEGREGATED_COLLATERAL);
        treeList.add(MARGIN_TYPE);

        return treeList;
    }

    public static Double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     *
     * @param row
     * @param columnName
     * @param errors
     * @return value of Collateral Config if is a MarginCAllConfig Column
     */
    private Object getMarginCallConfigColumn(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors) {

        //Somehow super method isMarginCallConfigColumn returns null. Implemented logic here
        String name = getMarginCallConfigReportStyle().getRealColumnName(CALLATERAL_CONFIG_PREFIX, columnName);
        return getMarginCallConfigReportStyle().getColumnValue(row, name, errors);
    }

    /**
     * @return custom CollateralConfigReportStyle. If custom code is not found, it will retrieve the custom version
     */
    private CollateralConfigReportStyle getMarginCallConfigReportStyle() {
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


}
