package calypsox.tk.report;

import calypsox.tk.util.bean.BODisponiblePartenonBean;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.inventory.PositionTypeHelper;
import com.calypso.tk.bo.inventory.SpecificInventoryPositionValues;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.apache.commons.lang3.StringUtils;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Optional;
import java.util.Vector;
/**
 * @author acd
 */
public class BODisponibleSecurityPositionReportStyle extends BOSecurityPositionReportStyle {
    public static final String POSITION_QUANTITY = "StartDate Quantity";
    public static final String POSITION_NOMINAL= "StartDate Nominal";
    public static final String POSITION_PRINCIPAL = "StartDate Principal";

    public static final String POSITION_VALUE = "Position Value Type";
    public static final String REAL_POSITION = "StartDate Real Position";
    public static final String ACTUAL_POSITION = "StartDate Actual Position";
    public static final String ACTUAL_ROW = "ActualInvRow";
    public static final String ISSUE_TYPE = "ISSUE_TYPE";

    public static final String PRODUCT = "PRODUCT";
    public static final String PRODUCT_SUBTYPE = "PRODUCT_SUBTYPE";
    public static final String PARTENON_MATCHING_KEY = "PARTENON_KEY";
    public static final String BOND_SISTEMA_CALC = "BOND_SISTEMA_CALC";
    public static final String BOND_TIPOPRODUCTO = "BOND_TIPOPRODUCTO";
    public static final String NIF_GARANTIA = "NIF GARANTIA";
    public static final String NIF_BENEFICIARIO = "NIF BENEFICIARIO";
    public static final String DESCRIPCION_BENEFICIARIO = "DESCRIPCION BENEFICIARIO";

    public static final String DIRECTION = "DIRECTION";
    public static final String MIC_PARTENON_CONTRACT = "PARTENON_CONTRACT";
    public static final String MIC_EMPRESA = "MIC_EMPRESA";
    public static final String MIC_CENTRO = "MIC_CENTRO";
    public static final String MIC_NUMERO_CONTRATO = "MIC_NUMERO_CONTRATO";
    public static final String MIC_TIPO_PRODUCTO = "MIC_TIPO_PRODUCTO";
    public static final String MIC_SUB_TIPO_PRODUCTO = "MIC_SUB_TIPO_PRODUCTO";

    public static final String BOND_DIRTY_PRICE = "Bond Dirty Price";
    public static final String CENTRO_DISPONIBLE = "Centro Disponible";
    public static final String CENTRO_BY_ACCOUNT_TYPE = "Centro by Account Type";

    public static final String BOOK_NAME =  "Book Name";



    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        String reportTemplateName = Optional.ofNullable(row.getProperty("ReportTemplate")).map(ReportTemplate.class::cast).map(ReportTemplate::getTemplateName).orElse("");
        JDate valDate = getStartDate(row);
        String positionValueOrig = Optional.ofNullable(row.getProperty("ReportTemplate")).map(ReportTemplate.class::cast).map(tp -> tp.get("POSITION_VALUE")).map(String::valueOf).orElse("");

        if(POSITION_VALUE.equalsIgnoreCase(columnId)){
            return null!=row.getProperty(POSITION_VALUE) ? row.getProperty(POSITION_VALUE) : getPositionValue(row);
        }else if(POSITION_QUANTITY.equalsIgnoreCase(columnId)){
            return getPositionValueForaDate(getReportStartDate(row),columnId,row,"Quantity");
        }else if(POSITION_NOMINAL.equalsIgnoreCase(columnId)){
            return getPositionValueForaDate(getReportStartDate(row),columnId,row,"Nominal (Unfactored)");
        }else if(POSITION_PRINCIPAL.equalsIgnoreCase(columnId)){
            return getPositionValueForaDate(getReportStartDate(row),columnId,row,"Nominal");
        }else if(REAL_POSITION.equalsIgnoreCase(columnId)){
            return calculateRealPosition(row,columnId);
        }else if(ACTUAL_POSITION.equalsIgnoreCase(columnId)){
            return calculateRealPosition(row,columnId);
        }else if (JISSUER.equals(columnId)) {
            return row.getProperty(JISSUER);
        }else if (ISSUE_TYPE.equals(columnId)) {
            return row.getProperty(ISSUE_TYPE);
        }else if (BOND_SISTEMA_CALC.equals(columnId)) {
            return "FAMT";
        }else if (BOND_TIPOPRODUCTO.equals(columnId)) {
            return "RENTA FIJA";
        }else if (DIRECTION.equals(columnId)) {
            BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
            if(null!=context){
                Amount quantity = (Amount)super.getColumnValue(row, Util.dateToMString(context.endDate), errors);
                if(quantity!=null) {
                    if(quantity.get()>=0)
                        return "LARGA";
                    else
                        return "CORTA";
                }
            }
            return "";
        }else if (NIF_BENEFICIARIO.equals(columnId)) {
            return "";
        }else if (MIC_PARTENON_CONTRACT.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getPartenonContract()).orElse("");
        }else if (MIC_EMPRESA.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getEmpresa()).orElse("");
        }else if (MIC_CENTRO.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getCentro()).orElse("");
        }else if (MIC_NUMERO_CONTRATO.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getNumeroDeContrato()).orElse("");
        }else if (MIC_TIPO_PRODUCTO.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getTipoProducto()).orElse("");
        }else if (MIC_SUB_TIPO_PRODUCTO.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(MIC_PARTENON_CONTRACT)).map(p -> ((BODisponiblePartenonBean)p).getSubTipoProducto()).orElse("");
        }else if (PARTENON_MATCHING_KEY.equals(columnId)) {
            return Optional.ofNullable(row.getProperty(PARTENON_MATCHING_KEY)).orElse("");
        }else if (DESCRIPCION_BENEFICIARIO.equals(columnId)) {
            return "";
        }else if (BOND_DIRTY_PRICE.equals(columnId)) {
            ConstantDisplayValue constantDisplayValue = Optional.ofNullable(super.getColumnValue(row, "Market Quote.DirtyPrice", errors))
                    .filter(ConstantDisplayValue.class::isInstance)
                    .map(ConstantDisplayValue.class::cast).orElse(null);
            return null != constantDisplayValue && !Double.isNaN(constantDisplayValue.get()) ? constantDisplayValue :0.0;
        }else if (CENTRO_DISPONIBLE.equals(columnId)) {
            return getCentroContableDisponible(row);
        }else if (CENTRO_BY_ACCOUNT_TYPE.equals(columnId)) {
            return row.getProperty(CENTRO_BY_ACCOUNT_TYPE);
        }else if (BOOK_NAME.equals(columnId)) {
            return getBookName(row);
        }else if(null!=row.getProperty(POSITION_VALUE) || reportTemplateName.contains("RECON")) {//crappy patch
            if(EQUITY_NUMTITULOS.equals(columnId) && (null!=valDate)){
                    SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
                    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
                    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
                    columnId = sdfDay.format(valDate.getDate()) + "-" + sdfMonth.format(valDate.getDate()) + "-" + sdfYear.format(valDate.getDate());

            }
            JDate columnDate = extractDate(columnId, this.__locale);
            if(null!=columnDate){
                String positionValue = row.getProperty(POSITION_VALUE);
                if(null==positionValue){
                    positionValue = positionValueOrig;
                }
                Amount positionValueForaDate = getPositionValueForaDate(columnDate, columnId, row, positionValue);
                if(reportTemplateName.contains("RECON")){//crappy patch
                    return formatAmount(positionValueForaDate);
                }
                return positionValueForaDate;
            }
        }

        return super.getColumnValue(row, columnId, errors);
    }

    /**
     *
     * ACTUAL_POSITION = Actual(Nominal Unfactored)
     * REAL_POSITION = Actual(Nominal Unfactored) - sum of not settled transfers
     *
     * @param row
     * @param columnId
     * @return
     */
    private Amount calculateRealPosition(ReportRow row, String columnId){
        ReportRow actualRow = row.getProperty(ACTUAL_ROW);
        if(null!=actualRow){
            Amount actualNominal = getPositionValueForaDate(getReportStartDate(row), columnId, actualRow, "Nominal (Unfactored)");
            if(null!=row.getProperty(REAL_POSITION) && null!=actualNominal){
                Amount xferNominal = (Amount) row.getProperty(REAL_POSITION);
                return new Amount(actualNominal.get()-(xferNominal.get()*-1));
            }else if(ACTUAL_POSITION.equalsIgnoreCase(columnId)){
                return actualNominal;
            }
        }
        return null;
    }

    protected Amount getPositionValueForaDate(JDate columnDate, String columnId, ReportRow row, String positionValueType){
        try {
            if(null!=columnDate){
                SpecificInventoryPositionValues.SpecificInventoryPositionValueContext posContext = row.getProperty("SpecificInventoryPosition");
                double amount = 0.0;
                BOPositionReport.ReportRowKey uniqueKey = (BOPositionReport.ReportRowKey)row.getUniqueKey();
                String moveType = uniqueKey.getMoveType();
                HashMap<JDate, Vector<Inventory>> positions = row.getProperty("POSITIONS");
                BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
                JDate startDate = context.startDate;
                boolean showClosing = context.showClosingBalance;
                boolean showMovements = false;
                if (!Util.isEmpty(moveType) && (moveType.equals("true") || PositionTypeHelper.security().isMovementsType(moveType, context.allBalanceTypeNames, context.allMovementTypeNames))) {
                    showMovements = true;
                }

                Vector datedPositions = positions.get(columnDate);
                if (datedPositions == null || datedPositions.size() == 0) {
                    return null;
                }

                if (showClosing && columnDate.equals(startDate)) {
                    if (columnId.contains("Opening Balance for ")) {
                        datedPositions = positions.get(startDate.addDays(-1));
                        if (datedPositions == null || datedPositions.size() == 0) {
                            return null;
                        }

                        amount = this.getTotal(datedPositions, moveType, posContext);
                    } else if (columnId.contains("Closing Balance for ")) {
                        amount = this.getTotal(datedPositions, moveType, posContext);
                    } else {
                        amount = this.getDailySecurity(datedPositions, moveType, posContext);
                    }
                } else if (!showMovements) {
                    amount = this.getTotal(datedPositions, moveType, posContext);
                } else {
                    amount = this.getDailySecurity(datedPositions, moveType, posContext);
                }

                //TODO Check only when qunaity is selected or other custom option
                String positionValue = InventorySecurityPosition.getCompositeTypePositionValue(moveType, context.allBalanceTypeNames, context.allMovementTypeNames);
                if (Util.isEmpty(positionValue) || "Template Position Value".equals(positionValue)) {
                    positionValue = context.positionValue;
                }

                positionValue = positionValueType;

                return applyPositionValueAndBalanceSign(amount, row, columnDate, positionValue);
            }
        }catch (Exception e){
            Log.error(this.getClass().getSimpleName(),"Error: " + e.getMessage());
        }
        return null;
    }

    protected JDate getReportStartDate(ReportRow row){
        BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext context = row.getProperty("ReportContext");
        return context.startDate;
    }


    public static Amount applyPositionValueAndBalanceSign(double amount, ReportRow row, JDate columnDate, String positionValue) {
        int digs = 2;
        InventorySecurityPosition position = row.getProperty("Inventory");
        double principal;
        if (!"Percentage".equals(positionValue)) {
            Product prod;
            if (!positionValue.equals("Quantity")) {
                prod = getProduct(position.getSecurityId());
                principal = 0.0;
                if (prod != null) {
                    if (positionValue.equals("Nominal")) {
                        principal = getPrincipal(prod, row, columnDate);
                    } else {
                        principal = prod.getPrincipal();
                    }

                    digs = prod.getNominalDecimals(prod.getCurrency());
                }

                amount *= principal;
            } else {
                prod = getProduct(position.getSecurityId());
                if (prod != null) {
                    digs = prod.getQuantityDecimals(prod.getCurrency());
                }
            }
        }

        Integer signInt = row.getProperty("Balance Sign");
        principal = 1.0;
        if (signInt != null) {
            principal = (double)signInt;
        }

        amount *= principal;
        if (positionValue.equals("Nominal")) {
            amount = RoundingMethod.roundNearest(amount, digs);
        }

        Amount result = new Amount(amount, digs);
        return result;
    }

    private static double getPrincipal(Product prod, ReportRow row, JDate columnDate) {
        if (prod instanceof Bond) {
            Bond bond = (Bond)prod;
            if (bond.getPIKRate() != 0.0 && ReportRow.getPricingEnv(row) != null) {
                Boolean calcB = row.getProperty("CALCULATED");
                if (calcB == null || !calcB) {
                    try {
                        row.setProperty("CALCULATED", Boolean.TRUE);
                        bond.calculate(bond.getFlows(columnDate), ReportRow.getPricingEnv(row), columnDate);
                    } catch (FlowGenerationException var6) {
                    }
                }
            }
        }

        return prod.getPrincipal(columnDate);
    }

    /**
     * @param row
     * @return Position Value from report context
     */
    private String getPositionValue(ReportRow row){
        return Optional.ofNullable((BOSecurityPositionReportTemplate.BOSecurityPositionReportTemplateContext)row.getProperty("ReportContext")).map(v -> v.positionValue).orElse("");
    }


    public String formatAmount(Amount amount) {
        if(null!=amount){
            try {
                String format = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "DisponibleTLMDecimalFormat");
                String decimalFormat = "###0.00";
                if(!Util.isEmpty(format)){
                    decimalFormat = format;
                }
                final DecimalFormat myFormatter = new DecimalFormat(decimalFormat);
                final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
                tmp.setDecimalSeparator(',');
                myFormatter.setDecimalFormatSymbols(tmp);
                return myFormatter.format(amount.get());
            } catch (Exception e) {
                if (amount != null) {
                    return String.valueOf(amount.get());
                } else {
                    return "0,00";
                }
            }
        }
        return "0,00";
    }

    /**
     * Get las 4 chars from book attribute: "Centro OPContable GER"
     * @param row ReportRow with Inventory object
     * @return return last 4 chars from book attribute: "Centro OPContable GER"
     */
    private String getCentroContableDisponible(ReportRow row){
        String center = Optional.ofNullable(row).map(r -> r.getProperty(ReportRow.INVENTORY))
                .map(Inventory.class::cast)
                .map(Inventory::getBook)
                .map(book -> book.getAttribute("Centro OPContable GER")).orElse("");
        return StringUtils.right(center, 4);
    }

    private String getBookName(ReportRow row){
       return Optional.ofNullable(row).map(r -> r.getProperty(ReportRow.INVENTORY))
                .map(Inventory.class::cast)
                .map(Inventory::getBook)
                .map(Book::getName).orElse("");
    }

    private JDate getStartDate(ReportRow row){//crappy patch
        ReportTemplate reportTemplate = Optional.ofNullable(row).map(r -> r.getProperty("ReportTemplate")).map(ReportTemplate.class::cast).orElse(new ReportTemplate());
        Object startDate = reportTemplate.get("StartDate");
        Object endDate = reportTemplate.get("EndDate");
        String date = "";
        if(null!=startDate){
            date = startDate.toString();
        }else if(endDate!=null){
            date = endDate.toString();
        }else if(reportTemplate.getValDate()!=null){
            date = reportTemplate.getValDate().toString();
        }
        return JDate.valueOf(date);
    }

}
