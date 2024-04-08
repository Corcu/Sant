package calypsox.tk.report;

import calypsox.tk.bo.fiflow.model.CalypsoToTCyCCBean;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.util.Vector;

/**
 * @author dmenendd
 */
public class BondTradeFIFlowReportStyle extends TradeReportStyle {

    public static final String COLUMNA1 = "idEmpr";
    public static final String COLUMNA2 = "idCent";
    public static final String COLUMNA3 = "cdstrOpe";
    public static final String COLUMNA4 = "cdPortfo";
    public static final String COLUMNA5 = "ccreFer ";
    public static final String COLUMNA6 = "codProd ";
    public static final String COLUMNA7 = "codsProd";
    public static final String COLUMNA8 = "fLiqOpe ";
    public static final String COLUMNA9 = "cdoPerbo";
    public static final String COLUMNA10 = "indCober";
    public static final String COLUMNA11 = "cdCobert";
    public static final String COLUMNA12 = "clCobert";
    public static final String COLUMNA13 = "idCobert";
    public static final String COLUMNA14 = "coDivisa";
    public static final String COLUMNA15 = "codDivlq";
    public static final String COLUMNA16 = "imprLimp";
    public static final String COLUMNA17 = "imprSuci";
    public static final String COLUMNA18 = "idSentOp";
    public static final String COLUMNA19 = "nTtituloo";
    public static final String COLUMNA20 = "iPrinOpe";
    public static final String COLUMNA21 = "imcpCorr";
    public static final String COLUMNA22 = "imEfeOpe";
    public static final String COLUMNA23 = "inOpinex";
    public static final String COLUMNA24 = "fConOper";
    public static final String COLUMNA25 = "hConOper";
    public static final String COLUMNA26 = "cestoPbo";
    public static final String COLUMNA27 = "tiPerson";
    public static final String COLUMNA28 = "cdPerson";
    public static final String COLUMNA29 = "cdProdux";
    public static final String COLUMNA30 = "marToMar";
    public static final String COLUMNA31 = "cdIroi";
    public static final String COLUMNA32 = "cdRig";
    public static final String COLUMNA33 = "idStrip";

    private static final long serialVersionUID = 6216200284059440314L;

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
        Object columnValue;
        CalypsoToTCyCCBean bean = row.getProperty(BondTradeFIFlowReport.ROW_PROP_NAME);
        if (COLUMNA1.equalsIgnoreCase(columnId)) {
        columnValue = bean.getIdEmpr();
        } else if (COLUMNA2.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdCent();
        } else if (COLUMNA3.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdstrOpe();
        } else if (COLUMNA4.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdPortfo();
        } else if (COLUMNA5.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCcreFer ();
        } else if (COLUMNA6.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodProd ();
        } else if (COLUMNA7.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodsProd();
        } else if (COLUMNA8.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfLiqOpe ();
        } else if (COLUMNA9.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdoPerbo();
        } else if (COLUMNA10.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIndCober();
        } else if (COLUMNA11.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdCobert();
        } else if (COLUMNA12.equalsIgnoreCase(columnId)) {
            columnValue = bean.getClCobert();
        } else if (COLUMNA13.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdCobert();
        } else if (COLUMNA14.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCoDivisa();
        } else if (COLUMNA15.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodDivlq();
        } else if (COLUMNA16.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImprLimp();
        } else if (COLUMNA17.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImprSuci();
        } else if (COLUMNA18.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdSentOp();
        } else if (COLUMNA19.equalsIgnoreCase(columnId)) {
            columnValue = bean.getnTtituloo();
        } else if (COLUMNA20.equalsIgnoreCase(columnId)) {
            columnValue = bean.getiPrinOpe();
        } else if (COLUMNA21.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImcpCorr();
        } else if (COLUMNA22.equalsIgnoreCase(columnId)) {
            columnValue = bean.getImEfeOpe();
        } else if (COLUMNA23.equalsIgnoreCase(columnId)) {
            columnValue = bean.getInOpinex();
        } else if (COLUMNA24.equalsIgnoreCase(columnId)) {
            columnValue = bean.getfConOper();
        } else if (COLUMNA25.equalsIgnoreCase(columnId)) {
            columnValue = bean.gethConOper();
        } else if (COLUMNA26.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCestoPbo();
        } else if (COLUMNA27.equalsIgnoreCase(columnId)) {
            columnValue = bean.getTiPerson();
        } else if (COLUMNA28.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdPerson();
        } else if (COLUMNA29.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdProdux();
        } else if (COLUMNA30.equalsIgnoreCase(columnId)) {
            columnValue = bean.getMarToMar();
        } else if (COLUMNA31.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdIroi();
        } else if (COLUMNA32.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdRig();
        } else if (COLUMNA33.equalsIgnoreCase(columnId)) {
            columnValue = bean.getIdStrip();
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
        return treeList;
    }

}
