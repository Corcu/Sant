package calypsox.tk.report;


import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


public class BondContingenciaALMReport extends TradeReport {

    public static final String SLB = "1111";
    public static final String MADRID = "1999";
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput load(Vector errorMsgs) {

        final DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();
        if(output==null) {
            return null;
        }
        final ReportRow[] rows = output.getRows();

        for(int i=0; i<rows.length; i++) {
            final ReportRow row = rows[i];
            if (row != null){
                final Trade trade = row.getProperty(ReportRow.TRADE);
                if (checkInternalCntrContable(trade)) {
                    final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
                    final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
                    final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
                    PLMark plMarkValueNPVD = getPLMarkValue(PricingEnv.loadPE("OFFICIAL_ACCOUNTING", valDate.getJDatetime()), trade, valDate);
                    Double mtmFullLago = getPLMark(plMarkValueNPVD, "MTM_NET_MUREX");
                    row.setProperty("MTM_NET_MUREX", mtmFullLago);
                    finalRows.add(row);
                }
            }
        }
        final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
        output.setRows(finalReportRows);
        return output;
    }

    /**
     * Validate is internal contable
     *
     * @param trade
     * @return
     */
    private boolean checkInternalCntrContable(Trade trade) {
        try {
            Trade tradeMirrorBook = DSConnection.getDefault().getRemoteTrade().getTrade(trade.getMirrorTradeId());

            String partenonTrade = null != trade ? trade.getKeywordValue("PartenonAccountingID") : "";
            String partenonMirrorTrade = null != tradeMirrorBook ? tradeMirrorBook.getKeywordValue("PartenonAccountingID") : "";

            String codCentroTrade = getCodigoCentro(partenonTrade);
            String codCentroTradeMirror = getCodigoCentro(partenonMirrorTrade);

            if (trade.getMirrorTradeId() > 0){
                return (validateIstreasury(codCentroTrade) || validateIstreasury(codCentroTradeMirror)) ?
                        (validateIstreasury(codCentroTrade) && validateIstreasury(codCentroTradeMirror)) ? false : true: false;
            }
            return true;
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error: " + e);
        }
        return true;
    }

    /**
     * Get Cod Centro
     *
     * @param keyword
     * @return
     */
    private String getCodigoCentro(String keyword) {
        return checkString(keyword, 4, 8);
    }

    /**
     * Check String
     *
     * @param value
     * @param init
     * @param fin
     * @return
     */
    private String checkString(String value, int init, int fin) {
        if (value != null && value.length() >= fin) {
            return Optional.ofNullable(value.substring(init, fin)).orElse("");
        } else {
            return "";
        }
    }

    /**
     * Validate is treasury
     *
     * @param value
     * @return
     */
    private boolean validateIstreasury(String value) {
        return (SLB.equalsIgnoreCase(value) || MADRID.equalsIgnoreCase(value)) ? true : false;
    }

    private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark = null;
        try {
            plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(), date);
            return plMark;
        } catch (RemoteException e) {
            Log.error(this, e);
            return null;

        }
    }


    private Double getPLMark(PLMark plMark, String type){
        return null!=plMark && null!=plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
    }


}