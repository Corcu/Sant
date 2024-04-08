package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import com.calypso.tk.core.JDate;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Vector;

public interface IOperacionesFormatter {
    /**
     * COPY 3
     * @param valDate
     */

    public List<AnacreditOperacionesItem> format(CollateralConfig contract, ReportRow row, JDate valDate, PricingEnv pricingEnv, Vector<String> errors);
  }
