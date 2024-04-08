package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.report.TransferReportStyle;

public class PairOffReportStyle extends com.calypso.tk.report.PairOffReportStyle {


    public TreeList getTreeList() {
        TreeList treeList1 = super.getTreeList();

        TransferReportStyle style = new TransferReportStyle();
        TreeList treeList = style.getTreeList();
        treeList1.add(treeList);

        TradeReportStyle tradeStyle = new TradeReportStyle();
        treeList1.add(tradeStyle.getTreeList());

        return treeList1;
    }

}
