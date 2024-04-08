package calypsox.tk.report;

import calypsox.tk.anacredit.items.AnacreditImportesOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditInventoryPersonaReportStyle extends AnacreditInventoryOperReportStyle {
    public static final String ID_ENTIDAD = "ID_ENTIDAD";
    public static final String FECHA_ALTA_RELACION = "FECHA_ALTA_RELACION";
    public static final String FECHA_DATOS = "FECHA_DATOS";
    public static final String FECHA_BAJA_RELACION = "FECHA_BAJA_RELACION";
    public static final String ID_CONTRATO = "ID_CONTRATO";
    public static final String ID_PERSONA = "ID_PERSONA";
    public static final String NATURALEZA_INTERVENCION = "NATURALEZA_INTERVENCION";
    public static final String GRUPO_TITULARES_MANCOMUNADOS = "GRUPO_TITULARES_MANCOMUNADOS";
    public static final String PORCENTAJE_PRINCIPAL_SUBVENCIONADO = "PORCENTAJE_PRINCIPAL_SUBVENCIONADO";
    public static final String PORCENTAJE_INTERESES_SUBVENCIONADOS = "PORCENTAJE_INTERESES_SUBVENCIONADOS";
    public static final String PORCENTAJE_PARTICIPACION = "PORCENTAJE_PARTICIPACION";
    public static final String RELACION_NO_DECLARABLE_CIRBE = "RELACION_NO_DECLARABLE_CIRBE";
    public static final String IMPORTE_MAXIMO_RESPONSABILIDAD_CONJUNTA = "IMPORTE_MAXIMO_RESPONSABILIDAD_CONJUNTA";
    public static final String IMPORTE_RESPONSABILIDAD_CONJUNTA = "IMPORTE_RESPONSABILIDAD_CONJUNTA";
    public static final String GRADO_RELEVANCIA_GARANTE = "GRADO_RELEVANCIA_GARANTE";
    public static final String TITULAR_NO_CONVENIO_ACREEDORES = "TITULAR_NO_CONVENIO_ACREEDORES";
    public static final String TRATAMIENTO_ESPECIAL_CIRBE = "TRATAMIENTO_ESPECIAL_CIRBE";

    @Override
    protected String getLine(AnacreditOperacionesItem item) {
        if(item instanceof AnacreditPersonaOperacionesItem){
            return ((AnacreditPersonaOperacionesItem)item).toString();
        }else if (item instanceof AnacreditImportesOperacionesItem){
            return ((AnacreditImportesOperacionesItem)item).toString();
        }else{
            return super.getLine(item);
        }
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

         if ((row == null) || (row.getProperty(AnacreditInventoryPersonaReportTemplate.ROW_DATA) == null)) {
            return null;
        }

        Object property = row.getProperty(AnacreditInventoryPersonaReportTemplate.ROW_DATA);
        String copy = row.getProperty("COPY");

        AnacreditPersonaOperacionesItem item = null;
        if(property instanceof AnacreditPersonaOperacionesItem){
            item = (AnacreditPersonaOperacionesItem) property;
        } else {
            return null;
        }


        if(ID_ENTIDAD.equalsIgnoreCase(columnName)){
            item.getId_entidad();
            return "";
        }else if(FECHA_ALTA_RELACION.equalsIgnoreCase(columnName)){
            return item.getFecha_alta_relacion();
        }else if(FECHA_DATOS.equalsIgnoreCase(columnName)){
            return "";
        }else if(FECHA_BAJA_RELACION.equalsIgnoreCase(columnName)){
            return item.getFecha_baja_relacion();
        }else if(ID_CONTRATO.equalsIgnoreCase(columnName)){
            return item.getId_contrato();
        }else if(ID_PERSONA.equalsIgnoreCase(columnName)){
            return item.getId_persona();
        }else if(NATURALEZA_INTERVENCION.equalsIgnoreCase(columnName)){
            return item.getNaturaleza_intervencion();
        }else if(GRUPO_TITULARES_MANCOMUNADOS.equalsIgnoreCase(columnName)){
            return item.getGrupo_titulares_mancomunados();
        }else if(PORCENTAJE_PRINCIPAL_SUBVENCIONADO.equalsIgnoreCase(columnName)){
            return item.getPorcentaje_principal_subvencionado();
        }else if(PORCENTAJE_INTERESES_SUBVENCIONADOS.equalsIgnoreCase(columnName)){
            return item.getPorcentaje_intereses_subvencionados();
        }else if(PORCENTAJE_PARTICIPACION.equalsIgnoreCase(columnName)){
            return item.getPorcentaje_participacion();
        }else if(RELACION_NO_DECLARABLE_CIRBE.equalsIgnoreCase(columnName)){
            return item.getRelacion_no_declarable_cirbe();
        }else if(IMPORTE_MAXIMO_RESPONSABILIDAD_CONJUNTA.equalsIgnoreCase(columnName)){
            return item.getImporte_maximo_responsabilidad_conjunta();
        }else if(IMPORTE_RESPONSABILIDAD_CONJUNTA.equalsIgnoreCase(columnName)){
            return item.getImporte_maximo_responsabilidad_conjunta();
        }else if(GRADO_RELEVANCIA_GARANTE.equalsIgnoreCase(columnName)){
            return item.getGrado_relevancia_garante();
        }else if(TITULAR_NO_CONVENIO_ACREEDORES.equalsIgnoreCase(columnName)){
            return item.getTitular_no_convenio_acreedores();
        }else if(TRATAMIENTO_ESPECIAL_CIRBE.equalsIgnoreCase(columnName)){
            return item.getTratamiento_especial_cirbe();
        } else {
            return super.getColumnValue(row,columnName,errors);
        }
    }
}
