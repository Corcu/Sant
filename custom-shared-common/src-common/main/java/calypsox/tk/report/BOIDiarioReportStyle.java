package calypsox.tk.report;

import calypsox.tk.bo.boi.BOIDiarioBean;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class BOIDiarioReportStyle extends TradeReportStyle {

    public static final String COLUMNA1 = "Fec Informacion";
    public static final String COLUMNA2 = "RIG";
    public static final String COLUMNA3 = "Fuente Informacion";
    public static final String COLUMNA4 = "Cod Producto";
    public static final String COLUMNA5 = "Cod Instrumento";
    public static final String COLUMNA6 = "Cod Portfolio";
    public static final String COLUMNA7 = "Cod Operacion";
    public static final String COLUMNA8 = "Cod Estructura";
    public static final String COLUMNA9 = "Tipo Estructura";
    public static final String COLUMNA10 = "Cod Entidad GLCS";
    public static final String COLUMNA11 = "Cod Operacion Negocio";
    public static final String COLUMNA12 = "Cod Divisa ";
    public static final String COLUMNA13 = "Cod Direccion";
    public static final String COLUMNA14 = "Cod Estrategia";
    public static final String COLUMNA15 = "Fec Captura";
    public static final String COLUMNA16 = "Fec Contratacion";
    public static final String COLUMNA17 = "Fec Valor";
    public static final String COLUMNA18 = "Fec Vencimiento";
    public static final String COLUMNA19 = "Principal Operacion ";
    public static final String COLUMNA20 = "Principal Vigor";
    public static final String COLUMNA21 = "Referencia";
    public static final String COLUMNA22 = "Spread ";
    public static final String COLUMNA23 = "Tasa  Interes";
    public static final String COLUMNA24 = "Fec Inicio";
    public static final String COLUMNA25 = "Fec Fin";
    public static final String COLUMNA26 = "Base Calculo";
    public static final String COLUMNA27 = "Indicador CallPut";
    public static final String COLUMNA28 = "Tipo Opcion 1";
    public static final String COLUMNA29 = "Tipo Opcion 2";
    public static final String COLUMNA30 = "Prima";
    public static final String COLUMNA31 = "Divisa Prima";
    public static final String COLUMNA32 = "Strike";
    public static final String COLUMNA33 = "P/L Cajas Oper";
    public static final String COLUMNA34 = "P/L Devengos Oper";
    public static final String COLUMNA35 = "P/L Valor de Mercado Oper";
    public static final String COLUMNA36 = "P/L Contable Oper";
    public static final String COLUMNA37 = "P/L Cajas Local";
    public static final String COLUMNA38 = "P/L Devengos Local";
    public static final String COLUMNA39 = "P/L Valor de Mercado Local";
    public static final String COLUMNA40 = "P/L Contable Local";

    private static final long serialVersionUID = 6216200284059440314L;

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
        Object columnValue = "";
        Optional<BOIDiarioBean> beanOpt = Optional.ofNullable(row.getProperty("BOIDiarioBean"));
        BOIDiarioBean bean = beanOpt.get();
        if (COLUMNA1.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFecProceso();
        }  else if (COLUMNA2.equalsIgnoreCase(columnId)) {
            columnValue = bean.getRefIntragrupo();
        } else if (COLUMNA3.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSistemaOrigen();
        } else if (COLUMNA4.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodProducto();
        } else if (COLUMNA5.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodInstrumento();
        } else if (COLUMNA6.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPortfolio();
        } else if (COLUMNA7.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodOperacion();
        } else if (COLUMNA8.equalsIgnoreCase(columnId)) {
            columnValue = bean.getcodEstructura();
        } else if (COLUMNA9.equalsIgnoreCase(columnId)) {
            columnValue = bean.getTipoEstructura();
        } else if (COLUMNA10.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLCS();
        } else if (COLUMNA11.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodOperacionNego();
        } else if (COLUMNA12.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodDivisa();
        } else if (COLUMNA13.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodDireccion();
        } else if (COLUMNA14.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodEstrategia();
        } else if (COLUMNA15.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfecCaptura();
        } else if (COLUMNA16.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfecContratacion();
        } else if (COLUMNA17.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfecValor();
        } else if (COLUMNA18.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfecVencimiento();
        } else if (COLUMNA19.equalsIgnoreCase(columnId)) {
            columnValue = bean.getPrincipalOpe();
        } else if (COLUMNA20.equalsIgnoreCase(columnId)) {
            columnValue = bean.getPrincipalVigor();
        } else if (COLUMNA21.equalsIgnoreCase(columnId)) {
            columnValue = bean.getReferencia();
        } else if (COLUMNA22.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSpread();
        } else if (COLUMNA23.equalsIgnoreCase(columnId)) {
            columnValue = bean.getTasaInteres();
        } else if (COLUMNA24.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFecInicio();
        } else if (COLUMNA25.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFecFin();
        } else if (COLUMNA26.equalsIgnoreCase(columnId)) {
            columnValue = bean.getBaseCalculo();
        } else if (COLUMNA27.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndCallPut();
        } else if (COLUMNA28.equalsIgnoreCase(columnId)) {
            columnValue = bean.gettipOpcion1();
        } else if (COLUMNA29.equalsIgnoreCase(columnId)) {
            columnValue = bean.gettipOpcion2();
        } else if (COLUMNA30.equalsIgnoreCase(columnId)) {
            columnValue = bean.getPrima();
        } else if (COLUMNA31.equalsIgnoreCase(columnId)) {
            columnValue = bean.getDivisaPrima();
        } else if (COLUMNA32.equalsIgnoreCase(columnId)) {
            columnValue = bean.getStrike();
        } else if (COLUMNA33.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoCajasOpe();
        } else if (COLUMNA34.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoDevengosOpe();
        } else if (COLUMNA35.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoMercadoOpe();
        } else if (COLUMNA36.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoContableOpe();
        } else if (COLUMNA37.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoCajasLocal();
        } else if (COLUMNA38.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoDevengosLocal();
        } else if (COLUMNA39.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoMercadoLocal();
        } else if (COLUMNA40.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSaldoContableLocal();
        } else {
            columnValue = super.getColumnValue(row, columnId, errors);
        }
        return columnValue;
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(COLUMNA1);
        treeList.add(COLUMNA2);
        treeList.add(COLUMNA3);
        treeList.add(COLUMNA4);
        treeList.add(COLUMNA5);
        treeList.add(COLUMNA6);
        treeList.add(COLUMNA7);
        treeList.add(COLUMNA8);
        treeList.add(COLUMNA9);
        treeList.add(COLUMNA10);
        treeList.add(COLUMNA11);
        treeList.add(COLUMNA12);
        treeList.add(COLUMNA13);
        treeList.add(COLUMNA14);
        treeList.add(COLUMNA15);
        treeList.add(COLUMNA16);
        treeList.add(COLUMNA17);
        treeList.add(COLUMNA18);
        treeList.add(COLUMNA19);
        treeList.add(COLUMNA20);
        treeList.add(COLUMNA21);
        treeList.add(COLUMNA22);
        treeList.add(COLUMNA23);
        treeList.add(COLUMNA24);
        treeList.add(COLUMNA25);
        treeList.add(COLUMNA26);
        treeList.add(COLUMNA27);
        treeList.add(COLUMNA28);
        treeList.add(COLUMNA29);
        treeList.add(COLUMNA30);
        treeList.add(COLUMNA31);
        treeList.add(COLUMNA32);
        treeList.add(COLUMNA33);
        treeList.add(COLUMNA34);
        treeList.add(COLUMNA35);
        treeList.add(COLUMNA36);
        treeList.add(COLUMNA37);
        treeList.add(COLUMNA38);
        treeList.add(COLUMNA39);
        treeList.add(COLUMNA40);
        return treeList;
    }

}
