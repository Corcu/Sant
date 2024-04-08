package calypsox.tk.anacredit.formatter;

import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import com.calypso.tk.core.JDate;
import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Vector;

public interface IPersonaFormatter {
    /**
     * COPY 4
     * @param valDate
     */
    public List<AnacreditPersonaOperacionesItem> formatPersonaItem(AnacreditOperacionesItem item, ReportRow row, JDate valDate, Vector<String> errors);

  }
