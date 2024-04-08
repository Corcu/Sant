package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FX;
import com.calypso.tk.report.PLMarkReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

public class FXPLMarkReportStyle extends PLMarkReportStyle {

    static final public String NPVFXLINE = "NPVFXLINE";


    private static final String MX_ID = "Mx Global ID";
    private static final int NPV_LENGTH = 17;

    private static final int PARTENON_TRADE_ID_LENGTH = 21;

    @Override
    public Object getColumnValue(final ReportRow row, final String columnId,
                                 final Vector errors) {

        if (row == null) {
            return null;
        }
        Trade trade = (Trade) row.getProperty("Trade");
        double npv = (double) row.getProperty("NPV");
        String ccy = (String) row.getProperty("CCY");
        String partenonId = getPartenonTradeId(trade);
        StringBuilder builder = new StringBuilder();
        if (columnId.equals(NPVFXLINE)) {
            builder.append(getValuationDateFromRow(row));
            builder.append(getIDEMPR(partenonId));
            builder.append(getIDCENT(partenonId));
            builder.append(getIDCONTR(partenonId));
            builder.append(getInstrumento());
            builder.append(getTradeId(trade));
            builder.append(getNPV(npv));
            builder.append(ccy);
            builder.append(getIndicadorPata(npv));
            builder.append(getIDPROD(partenonId));
            return builder.toString();
        }
        return super.getColumnValue(row, columnId, errors);
    }

    private String getInstrumento() {
        return BOCreUtils.getInstance().formatStringWithBlankOnRight("FX IMPLICITO DUAL CURRENCY", 30);
    }

    private String getTradeId(Trade trade) {
        return getOtherMultiCcyTrade(trade) != null ? BOCreUtils.getInstance().formatStringWithBlankOnRight(getOtherMultiCcyTrade(trade).getExternalReference(), 25) : "";
    }

    private String getIDEMPR(String partenonId) {
        return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(0, 4) : "";
    }

    private String getIDCENT(String partenonId) {
        return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(4, 8) : "";
    }

    private String getIDCONTR(String partenonId) {
        return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(11, 18) : "";
    }

    private String getIDPROD(String partenonId) {
        return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(8, 11) : "";
    }

    private String getPartenonTradeId(final Trade trade) {
        String partenonTradeId = trade.getKeywordValue("PartenonAccountingID");
        return SantanderUtil.getInstance().formatStringWithBlankOnRight(
                partenonTradeId, PARTENON_TRADE_ID_LENGTH);
    }

    public static Trade getOtherMultiCcyTrade(Trade trade) {
        String MxId = "";
        try {
            if (trade.getKeywordValue("AllocatedFrom") != null) {
                Trade parentTrade =
                        DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(trade.getKeywordValue("AllocatedFrom")));
                MxId = customMxGlobalID(parentTrade);
            } else {
                MxId = customMxGlobalID(trade);
            }
            TradeArray trades = DSConnection.getDefault().getRemoteTrade()
                    .getTradesByKeywordNameAndValue(MX_ID, MxId);
            for (int i = 0; i < trades.size(); i++) {
                if (trades.get(i).getProductFamily() != trade.getProductFamily()) {
                    return trades.get(i);
                }
            }
        } catch (CalypsoServiceException e) {
            com.calypso.tk.core.Log.error(FXPLMarkReportStyle.class, e);
        }
        return null;
    }

    private String getValuationDateFromRow(ReportRow row) {
        JDatetime dateTime = (JDatetime) row.getProperty("ValuationDatetime");
        JDate date = dateTime.getJDate(TimeZone.getDefault());
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());

        final String str = sdf.format(new JDatetime(date, 0, 0, 0, TimeZone
                .getDefault()));
        return str;
    }

    private String getIndicadorPata(double npv) {
        return npv > 0 ? "C" : "V";
    }

    private String getNPV(double npv) {
        return BOCreUtils.getInstance().formatUnsignedNumber(npv,
                NPV_LENGTH, 2);
    }

    public static String customMxGlobalID(Trade trade) {
        String mxGlobalIdFormat = trade.getKeywordValue("Mx Global ID");
        String mxGlobalId = mxGlobalIdFormat != null ? mxGlobalIdFormat : "";
        if (StringUtils.startsWith(mxGlobalId, "TOMS") && trade.getProduct() instanceof FX) {
            mxGlobalIdFormat = StringUtils.substring(mxGlobalId, 0, mxGlobalId.length() - 4);
        } else if (StringUtils.startsWith(mxGlobalId, "TOMS") && trade.getProduct() instanceof Bond) {
            mxGlobalIdFormat = mxGlobalId + "SPOT";
        }
        return mxGlobalIdFormat;
    }
}
