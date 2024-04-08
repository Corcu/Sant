package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxReportLogic;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.TimeZone;
import java.util.Vector;

public class EmirFieldBuilderEXECUTIONVENUE implements EmirFieldBuilder {

  @Override
  public String getValue(Trade trade) {

    return EmirFieldBuilderUtil.getInstance().getLogicEXECUTIONVENUE(trade);
  }


}
