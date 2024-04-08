package calypsox.tk.report;

import calypsox.tk.anacredit.items.AnacreditImportesOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditInventoryImportesReportStyle extends AnacreditInventoryOperReportStyle {

    public static final String FECHA_DATOS = "FECHA_DATOS";
    public static final String ID_CONTRATO = "ID_CONTRATO";
    public static final String TIPO_IMPORTE = "TIPO_IMPORTE";
    public static final String APLICACION_ORIGEN = "APLICACION_ORIGEN";
    public static final String ID_ENTIDAD = "ID_ENTIDAD";
    public static final String ID_CENTRO_CONTABLE = "ID_CENTRO_CONTABLE";
    public static final String PROVINCIA_NEGOCIO = "PROVINCIA_NEGOCIO";
    public static final String PAIS_NEGOCIO = "PAIS_NEGOCIO";
    public static final String TIPO_CARTERA = "TIPO_CARTERA";
    public static final String PRODUCTO = "PRODUCTO";
    public static final String IMPORTE_EUROS = "IMPORTE_EUROS";
    public static final String IMPORTE_DIVISA = "IMPORTE_DIVISA";
    public static final String RC_FACTOR_CONVERSION_ORDEN = "RC_FACTOR_CONVERSION_ORDEN";
    public static final String ID_PLAN_CONTABLE = "ID_PLAN_CONTABLE";
    public static final String TIPO_CARTERA_IFRS9 = "TIPO_CARTERA_IFRS9";

    protected String getLine(AnacreditOperacionesItem item) {
        if(item instanceof AnacreditImportesOperacionesItem){
            return ((AnacreditImportesOperacionesItem)item).toString();
        }else{
            return super.getLine(item);
        }
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if ((row == null) || (row.getProperty(AnacreditImportesOperacionReportTemplate.ROW_DATA) == null)) {
            return null;
        }

        Object property = row.getProperty(AnacreditImportesOperacionReportTemplate.ROW_DATA);
        String copy = row.getProperty("COPY");

        AnacreditImportesOperacionesItem item = null;
        if(property instanceof AnacreditImportesOperacionesItem){
            item = (AnacreditImportesOperacionesItem) property;
        } else {
            return null;
        }

        if(ID_ENTIDAD.equalsIgnoreCase(columnName)){
            item.getId_entidad();
            return "";
        }else if(TIPO_IMPORTE.equalsIgnoreCase(columnName)){
            return item.getTipo_importe();
        }else if(FECHA_DATOS.equalsIgnoreCase(columnName)){
            return "";
        }else if(APLICACION_ORIGEN.equalsIgnoreCase(columnName)){
            return item.getAplicacion_origen();
        }else if(ID_CONTRATO.equalsIgnoreCase(columnName)){
            return item.getId_contrato();
        }else if(ID_CENTRO_CONTABLE.equalsIgnoreCase(columnName)){
            return item.getId_centro_contable();
        }else if(PROVINCIA_NEGOCIO.equalsIgnoreCase(columnName)){
            return item.getProvincia_negocio();
        }else if(PAIS_NEGOCIO.equalsIgnoreCase(columnName)){
            return item.getPais_negocio();
        }else if(TIPO_CARTERA.equalsIgnoreCase(columnName)){
            return item.getTipo_cartera();
        }else if(PRODUCTO.equalsIgnoreCase(columnName)){
            return item.getProducto_ac();
        }else if(IMPORTE_EUROS.equalsIgnoreCase(columnName)){
            return item.getImporte_euros();
        }else if(IMPORTE_DIVISA.equalsIgnoreCase(columnName)){
            return item.getImporte_divisa();
        }else if(RC_FACTOR_CONVERSION_ORDEN.equalsIgnoreCase(columnName)){
            return item.getRc_factor_conversion_orden();
        }else if(ID_PLAN_CONTABLE.equalsIgnoreCase(columnName)){
            return item.getId_plan_contable();
        }else if(TIPO_CARTERA_IFRS9.equalsIgnoreCase(columnName)){
            return item.getTipo_cartera_ifrs9();
        } else {
            return super.getColumnValue(row,columnName,errors);
        }
    }
}
