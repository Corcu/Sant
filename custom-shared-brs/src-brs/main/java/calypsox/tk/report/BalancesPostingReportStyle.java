package calypsox.tk.report;

import calypsox.tk.bo.acyg.BalancesPostingBean;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.BalanceReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class BalancesPostingReportStyle extends BalanceReportStyle {

    public static final String BMYCSPOS_IDEMPR = "BMYCSPOS_IDEMPR";
    public static final String BMYCSPOS_IDCENT = "BMYCSPOS_IDCENT";
    public static final String BMYCSPOS_IDPROD = "BMYCSPOS_IDPROD";
    public static final String BMYCSPOS_IDCONTR = "BMYCSPOS_IDCONTR";
    public static final String BMYCSPOS_SUBCONTR = "BMYCSPOS_SUBCONTR";
    public static final String BMYCSPOS_IDSPROD = "BMYCSPOS_IDSPROD";
    public static final String BMYCSPOS_ACTVPORT = "BMYCSPOS_ACTVPORT";
    public static final String BMYCSPOS_CODMONSW = "BMYCSPOS_CODMONSW";
    public static final String BMYCSPOS_CODMONREL = "BMYCSPOS_CODMONREL";
    public static final String BMYCSPOS_CTOSALDO = "BMYCSPOS_CTOSALDO";
    public static final String BMYCSPOS_SITUACION = "BMYCSPOS_SITUACION";
    public static final String BMYCSPOS_MOROSIDAD = "BMYCSPOS_MOROSIDAD";
    public static final String BMYCSPOS_IMPSLD = "BMYCSPOS_IMPSLD";
    public static final String BMYCSPOS_IMPOCVL = "BMYCSPOS_IMPOCVL";
    public static final String BMYCSPOS_MONCVL = "BMYCSPOS_MONCVL";
    public static final String BMYCSPOS_IDEMPR2 = "BMYCSPOS_IDEMPR2";
    public static final String BMYCSPOS_IDCENT2 = "BMYCSPOS_IDCENT2";
    public static final String FILLER = "FILLER";


    public static final String[] ADDITIONAL_COLUMNS = {BMYCSPOS_IDEMPR, BMYCSPOS_IDCENT, BMYCSPOS_IDPROD, BMYCSPOS_IDCONTR, BMYCSPOS_SUBCONTR,
            BMYCSPOS_IDSPROD, BMYCSPOS_ACTVPORT, BMYCSPOS_CODMONSW, BMYCSPOS_CODMONREL, BMYCSPOS_CTOSALDO, BMYCSPOS_SITUACION, BMYCSPOS_MOROSIDAD, BMYCSPOS_IMPSLD,
            BMYCSPOS_IMPOCVL, BMYCSPOS_MONCVL, BMYCSPOS_IDEMPR, BMYCSPOS_IDCENT, BMYCSPOS_IDEMPR2, BMYCSPOS_IDCENT2, FILLER};


    public Object getColumnValue(ReportRow row, String columnId, Vector error) {
        Object columnValue = "";
        BalancesPostingBean item = row.getProperty("BalancesPostingBean");

        if (null != item) {
            if (columnId.equalsIgnoreCase(BMYCSPOS_IDEMPR)) {
                columnValue = item.getEmpresaContrato();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDCENT)) {
                columnValue = item.getCentroContrato();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDPROD)) {
                columnValue = item.getCodigoProductoCatalogo1();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDCONTR)) {
                columnValue = item.getPartenonContract();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_SUBCONTR)) {
                columnValue = item.getSubContract();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDSPROD)) {
                columnValue = item.getCodigoProductoCatalogo2();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_ACTVPORT)) {
                columnValue = item.getBook();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_CODMONSW)) {
                columnValue = item.getMonedaContrato();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_CODMONREL)) {
                columnValue = item.getMonedaRelacionada();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_CTOSALDO)) {
                columnValue = item.getPosicion();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_SITUACION)) {
                columnValue = item.getSituacion();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_MOROSIDAD)) {
                columnValue = item.getMorosidad();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IMPSLD)) {
                columnValue = item.getImporteSaldo();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IMPOCVL)) {
                columnValue = item.getImporteSaldoContValor();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_MONCVL)) {
                columnValue = item.getMonedaContravalor();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDEMPR2)) {
                columnValue = item.getEmpresaContable();
            } else if (columnId.equalsIgnoreCase(BMYCSPOS_IDCENT2)) {
                columnValue = item.getCentroContable();
            } else if (columnId.equalsIgnoreCase(FILLER)) {
                columnValue = item.getFiller();
            } else {
                row.setProperty("Trade", item.getTrade());
                row.setProperty("Account", item.getAccount());
                row.setProperty("Default", item.getBalance());
                row.setProperty("Book", item.getBook());
                row.setProperty("PricingEnv", item.getPricingEnv());
                row.setProperty("ValuationDatetime", item.getProcessDate());
                columnValue = super.getColumnValue(row, columnId, error);
            }
        }
        return columnValue;
    }

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();// 68
        CalypsoTreeNode dummyNode = new CalypsoTreeNode("BalancesPosting");// 69
        for (String column : ADDITIONAL_COLUMNS) {
            treeList.add(dummyNode, column);// 70
        }
        return treeList;// 74
    }

}
