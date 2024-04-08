package calypsox.tk.report;

import calypsox.apps.reporting.MICBarridoReportTemplatePanel;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;
import java.util.Vector;

public class MICBarridoReportStyle extends TradeReportStyle {

    public static final String TRADE_ID_MIC_FORMAT="Trade Id for MIC";
    public static final String EMPTY_COLUMN="Empty";
    public static final String VAL_DATE="VD";
    public static final String PLMARK="PLMARK";
    public static final String PLMARK_CCY="PLMARK_CCY";

    public static final String PLMARKORG="PLMARKORG";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
    	
        Trade trade = (Trade) Optional.ofNullable(row).map(r->r.getProperty("Trade")).orElse(null);
        final String plmarkType = Optional.ofNullable(row).map(r -> (String) r.getProperty(MICBarridoReportTemplatePanel.PLMARK_TYPE)).orElse("MTM_NET_MUREX");
        PLMark plMark = getPlMark(row);

        if(TRADE_ID_MIC_FORMAT.equalsIgnoreCase(columnId)){
            BOCreUtils creUtils=BOCreUtils.getInstance();
            long tradeId= Optional.ofNullable(trade).map(Trade::getLongId).orElse(0L);
            return creUtils.formatStringWithZeroOnLeft(tradeId,16);
        } else if(VAL_DATE.equalsIgnoreCase(columnId)){
            if(trade!=null){
                JDate valDate = (JDate) row.getProperty("VD");
                JDateFormat format = new JDateFormat("dd/MM/yyyy");
                return format.format(valDate);
            }
        } else if(PLMARK.equalsIgnoreCase(columnId)){
        	PLMarkValue repoPLMarketValue = new PLMarkValue();
        	if (row.getProperty("PLMarkRepo") != null) {
        		repoPLMarketValue.setMarkValue(row.getProperty("PLMarkRepo"));
        	}
            return formatAmount(Optional.ofNullable(plMark).map(pl -> pl.getPLMarkValueByName(plmarkType)).orElse(repoPLMarketValue).getMarkValue());
        }else if(PLMARKORG.equalsIgnoreCase(columnId)){
            return Optional.ofNullable(plMark).map(pl -> pl.getPLMarkValueByName(plmarkType)).orElse(new PLMarkValue()).getMarkValue();
        }else if(PLMARK_CCY.equalsIgnoreCase(columnId)){
            return Optional.ofNullable(plMark).map(pl -> pl.getPLMarkValueByName(plmarkType)).map(PLMarkValue::getCurrency).orElse("");
        }else if(EMPTY_COLUMN.equalsIgnoreCase(columnId)){
            return "";
        }

        return super.getColumnValue(row, columnId, errors);
    }


    private PLMark getPlMark(ReportRow row){
        return (PLMark) Optional.ofNullable(row).map(r -> r.getProperty("PLMark")).orElse(null);
    }

    public String formatAmount(Double value){
        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        final DecimalFormat df = new DecimalFormat("#0.00", decimalSymbol);
        return df.format(value);
    }


}
