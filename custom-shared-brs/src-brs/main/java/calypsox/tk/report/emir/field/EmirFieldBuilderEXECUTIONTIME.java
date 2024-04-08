package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxReportLogic;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.TimeZone;
import java.util.Vector;

public class EmirFieldBuilderEXECUTIONTIME implements EmirFieldBuilder {
  private static final String EMPTY_SPACE = "";

  @Override
  public String getValue(Trade trade) {
    final StringBuilder rst = new StringBuilder(EMPTY_SPACE);

    String sModifDate = "";
    if (EmirFieldBuilder.emirFieldMap.get(EmirSnapshotReduxReportLogic.VALUATION_DATE) != null) {
      final JDate valDate = (JDate) EmirFieldBuilder.emirFieldMap.get(EmirSnapshotReduxReportLogic.VALUATION_DATE);
      final String fromDate = Util.date2SQLString(valDate);
      final String toDate = Util.date2SQLString(valDate.addDays(1));
      sModifDate = "modif_date >= " + fromDate + " AND modif_date < " + toDate + " AND ";
    }

    // check if the last audit was because uti change
    final String whereClauseConf = sModifDate + " entity_class_name = 'Trade' " + " AND VERSION_NUM >=  "
        + trade.getVersion() + " AND entity_id = " + trade.getLongId() + " AND (audit_action LIKE '%NEW%' "
        + " OR audit_action  LIKE '%CANCEL%'" + " OR audit_action LIKE '%TERMINATE%'"
        + " OR audit_action LIKE'%BO_UTI_AMEND%'" + " OR audit_action LIKE '%BO_CANCEL%'"
        + " OR entity_field_name LIKE '%CancelReissueFrom%'" + " OR entity_field_name LIKE '%ConfirmationDateTime%') ";

    final String orderBy = " VERSION_NUM ASC";

    try {
      @SuppressWarnings("unchecked")
      final Vector<AuditValue> auditConf = DSConnection.getDefault().getRemoteTrade().getAudit(whereClauseConf, orderBy,null);

      if (!Util.isEmpty(auditConf)) {

        final AuditValue auditValue = auditConf.get(0);
        final JDatetime jModifDatetime = new JDatetime(auditValue.getModifDate().getTime());

        final String executionDateTime = Util.datetimeToString(jModifDatetime,
            EmirSnapshotReduxConstants.UTC_DATE_FORMAT, TimeZone.getTimeZone(EmirSnapshotReduxConstants.TIMEZONE_UTC));

        rst.append(executionDateTime);
      }

    } catch (final RemoteException e) {
      final String message = "Could not conecting DB";
      Log.error(this, message, e);
    }

    return rst.toString();
  }
}
