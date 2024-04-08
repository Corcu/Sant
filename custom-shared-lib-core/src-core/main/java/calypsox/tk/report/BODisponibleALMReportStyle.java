package calypsox.tk.report;

import calypsox.tk.bo.fiflow.model.jaxb.FIFlowField;
import calypsox.tk.util.bean.BODisponiblePartenonBean;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import org.apache.commons.lang.StringUtils;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class BODisponibleALMReportStyle extends BODisponibleBlockingPositionReportStyle {

    /*
RFYDIBS-LAYOUT   PIC 9(03)  -> ‘20’
RFYDIBS-FEXTRAC  PIC X(10)  -> FECHA ODATE -> Report Date in SUPER
RFYDIBS-FEPOSTRX PIC X(10)  -> FECHA ODATE -> Report Date in SUPER
RFYDIBS-APPFTE   PIC 9(03)  -> ‘23’
RFYDIBS-CARTERA  PIC X(30)  -> Portfolio -> Exists in SUPER
RFYDIBS-CUSTODIO PIC X(30)  -> Descripción Custodio => Agent Name super -> Exists in SUPER
RFYDIBS-REFEREN  PIC X(50) ->  Descripción emisión => Bond Name descrription -> Prd Description in SUPER
RFYDIBS-DIVISA   PIC X(03)  -> Divisa => divisa posición -> Exists in SUPER
RFYDIBS-ISIN     PIC X(30)  -> ISIN => ISIN posición -> Exists in SUPER
RFYDIBS-FECVAL   PIC X(10)  ->  Fecha Valor -> Report Date?
RFYDIBS-FECVENCI PIC X(10)  -> Fecha Vencimiento para bloqueos sino fecha vto emisión. => maturity Bono -> FECHA VENCIMIENTO in SUPER
RFYDIBS-VALORNOM PIC S9(15)V9(2) -> Nominal => ISIN posición -> Valor posicion
RFYDIBS-ACTIVO   PIC X(50)   -> “A”+Entidad+Centro+producto+Contrato+1 blanco+Portfolio   para bloqueos => “A”+0049+Centro contable+producto contable+Contrato Partenon +” “+Book (esto por confirmar con el ejemplo de fichero que nos va a enviar)
RFYDIBS-FECONTRA PIC X(10)  ->  Fecha contratación para Bloqueos o fecha valor para saldo pignorado. => fecha valor, es una posición para nostros -> Report Date?
RFYDIBS-ENDTOEND PIC X(16) ->  Referencia Iberclear para Bloqueos => Adrí?
RFYDIBS-CERTIFIC PIC X(30)  -> Certificado Iberclear para Bloqueos. => Adrí?
RFYDIBS-NIFGAR   PIC X(09) -> NIF de la entidad. => está en un atributo de la LE  pero ya lo sé cual, busca en el código existente porfa -> Que LE? NIF GARANTIA?
RFYDIBS-NIFBENF  PIC X(09)  -> NIF del beneficiario => está en un atributo de la LE pero ya lo sé cual, busca en el código existente porfa -> NIF BENEFICIARIO in SUPER
RFYDIBS-TIPOPER  PIC X(02) -> “BA”
RFYDIBS-ESTADOIB PIC X(09)   -> RFYDIBS-ENDTOEND NOT = SPACES AND  RFYDIBS-CERTIFIC NOT = SPACES AND  Custodio = Iberclear  se le asigna “ACEPTADA” sino blancos
RFYDIBS-ESTADO   PIC X(40)   -> Descripción del estado del bloqueo/saldo pignorado => aquí falta detalle, vemos con el fichero de ejemplo
RFYDIBS-BENEFIC  PIC X(30)   -> Nombre del Beneficiario => no sé bien que quiere decir con beneficiario tampoco, vemos con el fichero de ejemplo -> DESCRIPCION BENEFICIARIO in SUPER
     */

    public static final String LAYOUT = "LAYOUT";
    public static final String APPFTE = "APPFTE";
    public static final String TIPOPER = "TIPOPER";
    public static final String FEXTRAC = "FEXTRAC";
    public static final String FEPOSTRX = "FEPOSTRX";
    public static final String ISIN = "ISIN";
    public static final String FECVAL = "FECVAL";
    public static final String FECCONTRA = "FECCONTRA";
    public static final String ENDTOEND = "ENDTOEND";
    public static final String CERTIFIC = "CERTIFIC";
    public static final String ESTADOIB = "ESTADOIB";
    public static final String ESTADO = "ESTADO";

    private final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("ddMMyyyy");
    private final DecimalFormat decimalFormat = initDecimalFormat();


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        Object columnValue = "";

        String accountAttrDescripcionBeneficiario = ACCOUNT_PREFIX + "Acc_Attr.DescripcionBeneficiario";
        if (LAYOUT.equalsIgnoreCase(columnId)) {
            columnValue = getLayout();
        } else if (APPFTE.equalsIgnoreCase(columnId)) {
            columnValue = getAppFte();
        } else if (BOOK.equalsIgnoreCase(columnId)) {
            columnValue = getCartera(row, errors);
        } else if (TIPOPER.equalsIgnoreCase(columnId)) {
            columnValue = getTipoOper();
        } else if (ACTIVO.equalsIgnoreCase(columnId)) {
            columnValue = buildActivoColumn(row);
        } else if (FEXTRAC.equalsIgnoreCase(columnId)) {
            columnValue = getReportDate(row, errors);
        } else if (FEPOSTRX.equalsIgnoreCase(columnId)) {
            columnValue = getReportDate(row, errors);
        } else if (AGENT_NAME.equalsIgnoreCase(columnId)) {
            columnValue = getCustodio(row, errors);
        } else if (PRODUCT_DESCRIPTION.equalsIgnoreCase(columnId)) {
            columnValue = getPrdDescription(row, errors);
        } else if (CURRENCY.equalsIgnoreCase(columnId)) {
            columnValue = getCurrency(row, errors);
        } else if (ISIN.equalsIgnoreCase(columnId)) {
            columnValue = getIsin(row, errors);
        } else if (FECVAL.equalsIgnoreCase(columnId)) {
            columnValue = getReportDate(row, errors);
        } else if (MATURITY_DATE.equalsIgnoreCase(columnId)) {
            columnValue = getMaturityDate(row, errors);
        } else if (FECCONTRA.equalsIgnoreCase(columnId)) {
            columnValue = getReportDate(row, errors);
        } else if (ENDTOEND.equalsIgnoreCase(columnId)) {
            columnValue = getRefIbrc();
        } else if (CERTIFIC.equalsIgnoreCase(columnId)) {
            columnValue = getCertIbrc();
        } else if (NIF_GARANTIA.equalsIgnoreCase(columnId)) {
            columnValue = getNifGarantia(row, errors);
        } else if (NIF_BENEFICIARIO.equalsIgnoreCase(columnId)) {
            columnValue = getNifBeneficiario(row, errors);
        } else if (ESTADOIB.equalsIgnoreCase(columnId)) {
            columnValue = getEstadoib(row);
        } else if (ESTADO.equalsIgnoreCase(columnId)) {
            columnValue = getEstado();
        } else if (accountAttrDescripcionBeneficiario.equalsIgnoreCase(columnId)) {
            columnValue = getBeneficiario();
        } else {
            columnValue = extractPositionDateOrSuperColumn(row, columnId, errors);
        }
        return columnValue;

    }

    private String getLayout() {
        FIFlowField<Integer> formattedField = new FIFlowField<>(3);
        formattedField.setContent(20);
        return formattedField.toString();
    }

    private String getAppFte() {
        FIFlowField<Integer> formattedField = new FIFlowField<>(3);
        formattedField.setContent(23);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return CARTERA columnName
     */
    private String getCartera(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(30);
        String agentName = Optional.ofNullable(super.getColumnValue(row, BOOK, errors)).map(Object::toString).orElse("");
        formattedField.setContent(agentName);
        return formattedField.toString();
    }

    private String getReportDate(ReportRow row, Vector errors) {
        Object reportDate = super.getColumnValue(row, REPORT_DATE, errors);
        return formatJDateContent(reportDate);
    }

    private String getTipoOper() {
        FIFlowField<String> formattedField = new FIFlowField<>(2);
        formattedField.setContent("BA");
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return CUSTODIO columnName
     */
    private String getCustodio(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(30);
        String agentName = Optional.ofNullable(super.getColumnValue(row, AGENT_NAME, errors)).map(Object::toString).orElse("");
        formattedField.setContent(agentName);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return REFEREN columnName
     */
    private String getPrdDescription(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(50);
        String prdDesc = Optional.ofNullable(super.getColumnValue(row, PRODUCT_DESCRIPTION, errors)).map(Object::toString).orElse("");
        formattedField.setContent(prdDesc);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return DIVISA columnName
     */
    private String getCurrency(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(3);
        String currency = Optional.ofNullable(super.getColumnValue(row, CURRENCY, errors)).map(Object::toString).orElse("");
        formattedField.setContent(currency);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return NIFBENEFICIARIO columnName
     */
    private String getNifBeneficiario(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(9);
        String taxId = Optional.ofNullable(getAgent(row))
                .map(LegalEntity::getLegalEntityAttributes)
                .filter(attrs -> attrs instanceof Vector)
                .map(attrs -> getTaxIdLEAttribute((Vector<LegalEntityAttribute>) attrs))
                .orElse("");
        formattedField.setContent(taxId);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return BENEFICIARIO columnName
     */
    private String getBeneficiario() {
        FIFlowField<String> formattedField = new FIFlowField<>(30);
        /*String nif = Optional.ofNullable(super.getColumnValue(row, accountAttrDescripcionBeneficiario, errors))
                .map(Object::toString).orElse("");*/
        //blank by definition
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return NIFGARANTIA columnName
     */
    private String getNifGarantia(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(9);
        String nif = Optional.ofNullable(super.getColumnValue(row, NIF_GARANTIA, errors)).map(Object::toString).orElse("");
        formattedField.setContent(nif);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return FECVENCI columnName
     */
    private String getMaturityDate(ReportRow row, Vector errors) {
        Object reportDate = super.getColumnValue(row, MATURITY_DATE, errors);
        return formatJDateContent(reportDate);
    }

    /**
     * @param row
     * @param errors
     * @return VALORNOM columnName
     */
    private String getPositionValue(ReportRow row, String columnId, Vector errors) {
        return Optional.ofNullable(super.getColumnValue(row, columnId, errors))
                .filter(value -> value instanceof Amount)
                .map(amt -> ((Amount)amt).get())
                .map(this.decimalFormat::format)
                .map(nmberStr-> StringUtils.leftPad(nmberStr,17," "))
                .orElseGet(()->new FIFlowField<String>(17).toString());
    }


    /**
     * @param row
     * @param errors
     * @return ISIN columnName
     */
    private String getIsin(ReportRow row, Vector errors) {
        FIFlowField<String> formattedField = new FIFlowField<>(30);
        String isin = Optional.ofNullable(super.getColumnValue(row, PRODUCT_CODE_PREFIX + ISIN, errors)).map(Object::toString).orElse("");
        formattedField.setContent(isin);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return ENDTOEND columnName
     */
    private String getRefIbrc() {
        FIFlowField<String> formattedField = new FIFlowField<>(16);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return CERTIFIC columnName
     */
    private String getCertIbrc() {
        FIFlowField<String> formattedField = new FIFlowField<>(30);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return ESTADO columnName
     */
    private String getEstado() {
        FIFlowField<String> formattedField = new FIFlowField<>(40);
        return formattedField.toString();
    }

    /**
     * @param row
     * @param errors
     * @return ESTADOIB columnName
     */
    private String getEstadoib(ReportRow row) {
        String iberclearStatus = Optional.ofNullable(getAgent(row))
                .filter(this::isIberclear)
                .map(le -> "ACEPTADA")
                .orElse("");
        FIFlowField<String> formattedField = new FIFlowField<>(9);
        formattedField.setContent(iberclearStatus);
        return formattedField.toString();
    }

    private String buildActivoColumn(ReportRow row) {
        FIFlowField<String> formattedField = new FIFlowField<>(50);
        String partenon = getPartenon(row);
        String book = Optional.ofNullable(getBook(row))
                .map(Book::getName)
                .orElse("");
        formattedField.setContent("A" + partenon + " " + book);
        return formattedField.toString();
    }

    private boolean isIberclear(LegalEntity entity) {
        return "5GSR".equals(entity.getCode());
    }

    /**
     * @param row
     * @return ¿BOPosition has a partenon code?
     */
    private String getPartenon(ReportRow row) {
        StringBuilder stringBuilder = new StringBuilder();
        BODisponiblePartenonBean boDisponiblePartenonBean = (BODisponiblePartenonBean) row.getProperty(MIC_PARTENON_CONTRACT);
        if(null!=boDisponiblePartenonBean){
            stringBuilder.append(boDisponiblePartenonBean.getEmpresa());
            stringBuilder.append(boDisponiblePartenonBean.getCentro());
            stringBuilder.append(boDisponiblePartenonBean.getTipoProducto());
            stringBuilder.append(boDisponiblePartenonBean.getPartenonContract());
        }
        return stringBuilder.toString();
    }

    private LegalEntity getAgent(ReportRow row) {
        Inventory inventory = row.getProperty(ReportRow.INVENTORY);
        return inventory.getAgent();
    }

    private String formatJDateContent(Object rawContent) {
        String formattedDate;
        if (rawContent instanceof JDate) {
            LocalDate date = Optional.of((JDate) rawContent)
                    .map(ld -> LocalDate.of(ld.getYear(), ld.getMonth(), ld.getDayOfMonth()))
                    .orElseGet(() -> LocalDate.of(1, 1, 1901));
            formattedDate = datePattern.format(date);
        } else {
            FIFlowField<String> field = new FIFlowField<>(8);
            formattedDate = field.toString();
        }
        return formattedDate;
    }

    private Object extractPositionDateOrSuperColumn(ReportRow row, String columnId,Vector errors){
        Object columnValue;
        JDate columnDate = extractDate(columnId, this.__locale);
        if (null != columnDate) {
            columnValue = getPositionValue(row, columnId, errors);
        }else{
            columnValue = super.getColumnValue(row, columnId, errors);
        }
        return columnValue;
    }

    private DecimalFormat initDecimalFormat(){
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator(',');
        return new DecimalFormat("#.00", otherSymbols);
    }
}
