package calypsox.tk.report;

import calypsox.tk.anacredit.formatter.AnacreditFormatterCash;
import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.items.AnacreditImportesOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.loader.AnacreditLoader;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AnacreditImportesOperacionReport extends AnacreditAbstractReport {

    @Override
    protected List<ReportRow> extendReportRows(List<ReportRow> allRows, Vector<String> errors) {
        ArrayList<ReportRow> importesRows = new ArrayList<>();

        for (ReportRow row: allRows) {
            AnacreditOperacionesItem item = row.getProperty(AnacreditOperacionesReportTemplate.ROW_DATA);

            if (null == item) {
                continue;
            }
            String direction = "-";
            if(item.getSaldo_deudor_no_ven()!=0.0){

                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("02",3));
                Double value = item.getSaldo_deudor_no_ven();
                bean.setImporte_euros(item.getSaldo_deudor_no_vencido());
                if(value==0.0){
                    direction = "+";
                }
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }

            if(item.getValor_nominal_d()!=0.0){
                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("40",3));
                Double value = item.getValor_nominal_d();
                bean.setImporte_euros(item.getValor_nominal());
                if(value==0.0){
                    direction = "+";
                }
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }

            //TODO valor nominal para contratos
            if(item.getIntereses_devengos()!=0.0){
                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("15",3));
                Double value = item.getIntereses_devengos();
                bean.setImporte_euros(item.getIntereses_devengados());
                if((value==0.0)
                     || item.getIntereses_devengados().contains("+")) {
                    direction = "+";
                }
                bean.setImporte_euros(item.getIntereses_devengados());
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }


            // Saldo Contingente - Renta Variable
            if(item.getSaldo_contingente_d() !=0.0){
                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("27",3));
                Double value = item.getSaldo_contingente_d();
                if(value==0.0){
                    direction = "+";
                }
                bean.setImporte_euros(direction+ AnacreditFormatter.formatUnsignedNumber(item.getSaldo_contingente_eur(),15,2,""));
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }

            //v2.19 - v2.20
            if(item.getReduc_principal_avales_ejecutado() !=0.0){
                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("688",3));
                Double value = item.getReduc_principal_avales_ejecutado();
                if(value==0.0){
                    direction = "+";
                }
                bean.setImporte_euros(direction+ AnacreditFormatter.formatUnsignedNumber(item.getSaldo_contingente_eur(),15,2,""));
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }

            //v2.19 - v2.20
            if(item.getReduc_principal_importe_avalista() !=0.0){
                AnacreditImportesOperacionesItem bean = new AnacreditImportesOperacionesItem();
                setDefaultImp(bean,item); //Init default columns
                bean.setTipo_importe(AnacreditFormatter.formatStringWithBlankOnRight("687",3));
                Double value = item.getReduc_principal_importe_avalista();
                if(value==0.0){
                    direction = "+";
                }
                bean.setImporte_euros(direction+ AnacreditFormatter.formatUnsignedNumber(item.getSaldo_contingente_eur(),15,2,""));
                bean.setImporte_divisa(direction+ AnacreditFormatter.formatUnsignedNumber(value,15,2,""));
                ReportRow clone = (ReportRow) row.clone();
                AnacreditLoader.addRowData(importesRows, clone, bean);
            }


        }

        return importesRows;
    }

    private void setDefaultImp(AnacreditImportesOperacionesItem bean,AnacreditOperacionesItem item){
        bean.setFecha_extraccion(item.getFecha_extraccion());
        bean.setId_contrato(item.getId_contrato());
        bean.setId_entidad("0049");
        bean.setId_centro_contable(AnacreditFormatter.formatUnsignedNumber(1999,6,0,""));
        bean.setAplicacion_origen("A003");
        bean.setProvincia_negocio(item.getProvincia_negocio());
        bean.setPais_negocio(item.getPais_negocio());
        bean.setTipo_cartera(item.getTipo_cartera());
        bean.setMoneda(item.getMoneda());
        bean.setProducto_ac(item.getProducto_ac());
        bean.setRc_factor_conversion_orden(item.getRc_factor_conversion_orden());
        bean.setId_plan_contable(AnacreditFormatterCash.formatStringWithBlankOnRight("",50));
        bean.setTipo_cartera_ifrs9(item.getTipo_cartera_ifrs9());
        bean.setEntidad_grupo_origen(AnacreditFormatterCash.formatStringWithBlankOnRight("",15));
        bean.setCuenta_grupo(AnacreditFormatterCash.formatStringWithBlankOnRight("",20));
        bean.setSubcuenta(AnacreditFormatterCash.formatStringWithBlankOnRight("",10));
        bean.setId_centro_contable_a(AnacreditFormatterCash.formatStringWithBlankOnRight("",10));
    }

}
