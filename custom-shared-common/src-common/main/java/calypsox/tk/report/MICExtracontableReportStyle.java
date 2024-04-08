package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableBean;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class MICExtracontableReportStyle extends TradeReportStyle {

    public static final String COLUMNA1 = "Centro";
    public static final String COLUMNA2 = "Cod ISIN";
    public static final String COLUMNA3 = "Empresa Contrato";
    public static final String COLUMNA4 = "Cod Sector";
    public static final String COLUMNA5 = "Ind Tipo Operacion";
    public static final String COLUMNA6 = "Ind Pertenencia grupo";
    public static final String COLUMNA7 = "Moneda Contravalor";
    public static final String COLUMNA8 = "Numero Contrato";
    public static final String COLUMNA9 = "Numero Operac DGO";
    public static final String COLUMNA10 = "Ind Cobertura Contable";
    public static final String COLUMNA11 = "Cod Pais Emisor";
    public static final String COLUMNA12 = "Cod Producto";
    public static final String COLUMNA13 = "Ind Subtip Catalogo Cto";
    public static final String COLUMNA14 = "Moneda Contrato";
    public static final String COLUMNA15 = "Cod Ref Intra-Grupo";
    public static final String COLUMNA16 = "Cod GLS Contrapar";
    public static final String COLUMNA17 = "Cod GLS Emisor";
    public static final String COLUMNA18 = "Cod GLS Entidad";
    public static final String COLUMNA19 = "Cod Pais Contrapar";
    public static final String COLUMNA20 = "Cod Contrapar";
    public static final String COLUMNA21 = "Cod Emisor";
    public static final String COLUMNA22 = "Desc Cod Contrapar";
    public static final String COLUMNA23 = "Cod CIF Emisor";
    public static final String COLUMNA24 = "Fecha Contratacion";
    public static final String COLUMNA25 = "Fecha Vencimiento";
    public static final String COLUMNA26 = "Sector Banco Espana Contrapar";
    public static final String COLUMNA27 = "Sector Banco Espana Emisor";
    public static final String COLUMNA28 = "Tipo Interes/Tir";
    public static final String COLUMNA29 = "Cod Portfolio";
    public static final String COLUMNA30 = "Cod Tipo Operacion-3";
    public static final String COLUMNA31 = "Cod Estrategia Operacion";
    public static final String COLUMNA32 = "Cod Tipo Cobertura";
    public static final String COLUMNA33 = "Cod Sentido";
    public static final String COLUMNA34 = "Imp Nominal";
    public static final String COLUMNA35 = "Cod J Contrapar";
    public static final String COLUMNA36 = "Cod Num Operac Sist Front";
    public static final String COLUMNA37 = "Cod Num Operac Sist Back";
    public static final String COLUMNA38 = "Cod Num Evento Back";
    public static final String COLUMNA39 = "Fecha Valor.1";
    public static final String COLUMNA40 = "Imp Intereses";
    public static final String COLUMNA41 = "Imp Principal";
    public static final String COLUMNA42 = "Fecha Inicio Fijacion";
    public static final String COLUMNA43 = "Fecha Vencimiento Fijacion";
    public static final String CLASECONTABLE="Clase Contable";

    public static final String CDTIPOPC="Tipo de Opcion";
    public static final String CDSUREFI="Subyancente Renta Fija";
    public static final String INDSUBORD="Indicador de Subordinacion";
    public static final String IDANOCTA="Indicador de Anotacion Cuenta";
    public static final String IDDERIMP="Indicador de Derivado Implicito";
    public static final String IDSEGREG="Indicador de Segregacion";

    public static final String TCREFINT="Referencia Interna";

    public static final String CDREOPIN="Referencia Operacion Interna";
    public static final String EMPTY_93="Empty93";


    private static final long serialVersionUID = 6216200284059440314L;

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
        Object columnValue;
        MICExtracontableBean bean = row.getProperty(MICExtracontableReport.ROW_PROP_NAME);
        if (COLUMNA1.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdCent();
        } else if (COLUMNA2.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodIsin();
        } else if (COLUMNA3.equalsIgnoreCase(columnId)) {
            columnValue = bean.getEmprContrato();
        } else if (COLUMNA4.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodSector();
        } else if (COLUMNA5.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndTipOper();
        } else if (COLUMNA6.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndPertGrupo();
        } else if (COLUMNA7.equalsIgnoreCase(columnId)) {
            columnValue = bean.getMonedaContravalor();
        } else if (COLUMNA8.equalsIgnoreCase(columnId)) {
            columnValue = bean.getNumContrato();
        } else if (COLUMNA9.equalsIgnoreCase(columnId)) {
            columnValue = bean.getNumOperacDGO();
        } else if (COLUMNA10.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndCoberCont();
        } else if (COLUMNA11.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPaisEmisor();
        } else if (COLUMNA12.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodProducto();
        } else if (COLUMNA13.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndSubCa();
        } else if (COLUMNA14.equalsIgnoreCase(columnId)) {
            columnValue = bean.getMonContr();
        } else if (COLUMNA15.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodRefInGr();
        } else if (COLUMNA16.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSContrapar();
        } else if (COLUMNA17.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSEmisor();
        } else if (COLUMNA18.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodGLSEntidad();
        } else if (COLUMNA19.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPaisContrapar();
        } else if (COLUMNA20.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodContrapar();
        } else if (COLUMNA21.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodEmisor();
        } else if (COLUMNA22.equalsIgnoreCase(columnId)) {
            columnValue = bean.getDescCodContrapar();
        } else if (COLUMNA23.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodCifEmi();
        } else if (COLUMNA24.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFContrata();
        } else if (COLUMNA25.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFVenci();
        } else if (COLUMNA26.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSecBancoEspContrapar();
        } else if (COLUMNA27.equalsIgnoreCase(columnId)) {
            columnValue = bean.getSecBancoEspEmisor();
        } else if (COLUMNA28.equalsIgnoreCase(columnId)) {
            columnValue = bean.getTipoInteres();
        } else if (COLUMNA29.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodPortf();
        } else if (COLUMNA30.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodTipoOpe3();
        } else if (COLUMNA31.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodEstrOpe();
        } else if (COLUMNA32.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodTipoCobertura();
        } else if (COLUMNA33.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodSentido();
        } else if (COLUMNA34.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpNominal();
        } else if (COLUMNA35.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodJContrapar();
        } else if (COLUMNA36.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumOpeFront();
        } else if (COLUMNA37.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumOpeBack();
        } else if (COLUMNA38.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodNumEventoBack();
        } else if (COLUMNA39.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFvalor();
        } else if (COLUMNA40.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpIntereses();
        } else if (COLUMNA41.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImpPrincipal();
        } else if (COLUMNA42.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFIniFij();
        } else if (COLUMNA43.equalsIgnoreCase(columnId)) {
            columnValue = bean.getFVenciFij();
        } else if(CLASECONTABLE.equals(columnId)){
            columnValue= bean.getClaseContable();
        }else if(TCREFINT.equals(columnId)){
            columnValue= bean.getTcRefInt();
        } else if(CDTIPOPC.equals(columnId)){
            columnValue= bean.getTipOpcion();
        } else if(CDSUREFI.equals(columnId)){
            columnValue= bean.getSubyRF();
        } else if(INDSUBORD.equals(columnId)){
            columnValue= bean.getIndSubordi();
        } else if(IDANOCTA.equals(columnId)){
            columnValue= bean.getIndAnotCuenta();
        } else if(IDDERIMP.equals(columnId)){
            columnValue= bean.getIndDerivativeImp();
        } else if(IDSEGREG.equals(columnId)){
            columnValue= bean.getIndSegregation();
        }else if(CDREOPIN.equals(columnId)){
            columnValue= bean.getCdreopin();
        }else if(EMPTY_93.equals(columnId)){
            columnValue= bean.getEmpty();
        } else{
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
        treeList.add(COLUMNA41);
        treeList.add(COLUMNA42);
        treeList.add(COLUMNA43);
        treeList.add(CLASECONTABLE);
        treeList.add(EMPTY_93);
        return treeList;
    }

}
