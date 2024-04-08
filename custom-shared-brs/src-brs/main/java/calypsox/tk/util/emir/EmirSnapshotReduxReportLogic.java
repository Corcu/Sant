package calypsox.tk.util.emir;

import calypsox.tk.report.SantEmirSnapshotReportItem;
import calypsox.tk.report.emir.field.EmirFieldBuilder;
import calypsox.tk.report.emir.field.EmirFieldBuilderFactory;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

import java.util.List;

//import calypsox.tk.report.emir_field.EmirFieldBuilder;
//import calypsox.tk.report.emir_field.EmirFieldBuilderFactory;

public class EmirSnapshotReduxReportLogic {

  private static final String SNAPSHOT_TYPE = "SNP";
  public static final String VALUATION_DATE = "VALUATION_DATE";

  private EmirSnapshotReportType reportType = null;
  private Trade trade = null;

  public EmirSnapshotReduxReportLogic(EmirSnapshotReportType reportType,
                                      Trade trade, JDate valuationDate) {
    this.reportType = reportType;
    this.trade = trade;
    EmirFieldBuilder.emirFieldMap.put(VALUATION_DATE, valuationDate);
  }

  public SantEmirSnapshotReportItem fillItem() {
    final SantEmirSnapshotReportItem item = new SantEmirSnapshotReportItem();

    final List<EmirSnapshotColumn> columns = getColumns();

    for (final EmirSnapshotColumn column : columns) {
      String value = "NOT IMPLEMENTED!";

      final EmirFieldBuilder fieldBuilder = EmirFieldBuilderFactory
          .getInstance().getFieldBuilder(column, reportType);
      if (fieldBuilder != null) {
        value = fieldBuilder.getValue(trade);
      }

      final String columnName = column.name();
      item.setColumnValue(columnName, value);
      item.setReportTypeValue(columnName, SNAPSHOT_TYPE);
    }

    return item;
  }

  private List<EmirSnapshotColumn> getColumns() {
    List<EmirSnapshotColumn> columns = null;

    if (reportType == EmirSnapshotReportType.INDEPENDENT) {
      columns = EmirSnapshotColumn.getIndependentColumns();
    } else if (reportType == EmirSnapshotReportType.DELEGATE) {
      columns = EmirSnapshotColumn.getDelegateColumns();
    } else if (reportType == EmirSnapshotReportType.BOTH) {
      columns = EmirSnapshotColumn.getBothColumns();
    }

    return columns;
  }
}
