package com.calypso.tk.report;

/**
 * @author aalonsop
 */
public class DatReportViewer extends AbstractReportViewer {

    @Override
    void formatCell(int type, Object value) {
        this.buffer.append(value.toString());
    }
}
