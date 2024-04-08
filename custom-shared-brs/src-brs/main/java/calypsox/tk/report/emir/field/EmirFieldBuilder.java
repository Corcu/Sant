package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

import java.util.HashMap;

public interface EmirFieldBuilder {
  HashMap<String, Object> emirFieldMap = new HashMap<String, Object>();

  public String getValue(Trade trade);
}
