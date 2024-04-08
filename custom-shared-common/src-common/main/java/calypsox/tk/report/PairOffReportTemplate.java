package calypsox.tk.report;

import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.report.TransferReportStyle;
import org.xhtmlrenderer.util.Util;

import java.util.ArrayList;
import java.util.List;

public class PairOffReportTemplate extends com.calypso.tk.report.PairOffReportTemplate {


    @Override
    public void setDefaults() {
        super.setDefaults();
        List<String> finalSortColumns = new ArrayList<>();

        TransferReportStyle style = new TransferReportStyle();
        style.getDefaultColumns();

        TradeReportStyle tradeStyle = new TradeReportStyle();
        tradeStyle.getDefaultColumns();

        String[] sortColumnNames = this.getSortColumnNames();
        finalSortColumns.addAll( Util.toList(style.getDefaultColumns()));
        finalSortColumns.addAll( Util.toList(tradeStyle.getDefaultColumns()));
        finalSortColumns.addAll( Util.toList(sortColumnNames));
        finalSortColumns.add("Xfer_SecCode.ISIN");
        finalSortColumns.add("PRODUCT_CODE.ISIN");
        this.setSortColumns((String[])((String[])finalSortColumns.toArray(new String[finalSortColumns.size()])));

    }

}
