package calypsox.tk.report;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.report.ReportRow;

import java.util.Iterator;
import java.util.Map;

public class MessageSwiftReportRow extends com.calypso.tk.report.ReportRow {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The constant */
  static final String SWIFT_CONTRACT_ID = "SWIFT_CONTRACT_ID";

  /** Instantiates a new custom report row. */
  public MessageSwiftReportRow(
      final BOMessage message, final ReportRow row, final String swiftContractID) {
    super(message);
    Map<String, Object> properties = row.getProperties();
    Iterator<String> it = properties.keySet().iterator();
    while (it.hasNext()) {
      String key = it.next();
      if (!key.equals("BOMessage")) {
        setProperty(key, properties.get(key));
      }
    }
    setPropertys(swiftContractID);
    setUniqueKey(message.getKey());
  }

  /**
   * get the Values on String
   *
   * @return String[]
   */
  private void setPropertys(final String swiftcontractID) {
    setProperty(SWIFT_CONTRACT_ID, swiftcontractID);
  }

  /**
   * Get SwiftContractID
   *
   * @return Long
   */
  public Long getSwiftContractID() {
    return getProperty(SWIFT_CONTRACT_ID);
  }
}
