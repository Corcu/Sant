package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportStyle;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.tk.report.FTTXferReportTemplate.*;

public class FTTXferReportStyle extends TransferReportStyle {

    private static final String EMPTY_SPACE = "";
    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMdd");

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {


        if(REPORT_DATE.equalsIgnoreCase(columnName)){
            final JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
            final JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            return valDate;
        }  else  if(REPORT_ROW_SEQ.equalsIgnoreCase(columnName)) {
            return row.getProperty(FTTXferReport.ROW_COUNTER);
        }  else  if(FTT_REFERENCE_ID.equalsIgnoreCase(columnName)) {
            BOTransfer xfer = row.getProperty(ReportRow.TRANSFER);
            if("SecLending".equalsIgnoreCase(xfer.getProductType()))  {
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(xfer.getTradeLongId());
                    if  (trade != null) {
                        return trade.getExternalReference();
                    }
                } catch (CalypsoServiceException e) {
                    e.printStackTrace();
                }
            }
            return String.valueOf(xfer.getTradeLongId());
        }  else  if(FTT_STANDARD_S.equalsIgnoreCase(columnName)) {
            return "S";
        }  else  if(FTT_STANDARD_N.equalsIgnoreCase(columnName)) {
            return "N";
        }  else  if(FTT_TRADE_DATE.equalsIgnoreCase(columnName)) {
            Trade trade = row.getProperty(ReportRow.TRADE);
            if (trade!= null) {
                try {
                    return _sdf.format(trade.getTradeDate());
                } catch (Exception ex) {
                    Log.error("PdvTransferReportStyle", ex);
                }
            }
            return null;
        }  else  if(FTT_SETTLE_DATE.equalsIgnoreCase(columnName)) {
            BOTransfer xfer = row.getProperty(ReportRow.TRANSFER);
            if (xfer!= null) {
                try {
                    return _sdf.format(xfer.getSettleDate().getDate());
                } catch (Exception ex) {
                    Log.error("PdvTransferReportStyle", ex);
                }
                return null;
            }
        }  else  if(FTT_QUANTITY.equalsIgnoreCase(columnName)) {
            Object value =  super.getColumnValue(row, "Quantity", errors);
            return getFormatQuantity(value);
        }  else  if(FTT_UNIT.equalsIgnoreCase(columnName)) {
            return "UNT";
        }  else  if(FTT_CASH_AMOUNT.equalsIgnoreCase(columnName)) {
            Trade trade = row.getProperty(ReportRow.TRADE);
            if (trade.getProduct() instanceof SecLending) {
                SecLending secLending = (SecLending) trade.getProduct();
                if ("DFP".equals(secLending.getDeliveryType()))  {
                    Object obj =  super.getColumnValue(row, TRANSFER_AMOUNT, errors);
                    if (obj != null && obj instanceof DisplayValue) {
                        return formatNumberOutput((DisplayValue)obj);
                    }
                } else if ("DAP".equals(secLending.getDeliveryType()))  {
                    Object obj =  super.getColumnValue(row, OTHER_AMOUNT, errors);
                    if (obj != null && obj instanceof DisplayValue) {
                        return formatNumberOutput((DisplayValue)obj);
                    }
                }
            }
            return 0;
        }  else  if(FTT_TAXABLE_FLAG.equalsIgnoreCase(columnName)) {
            return "N";
        }  else  if(FTT_EXONERATION_CODE.equalsIgnoreCase(columnName)) {
            return "6";
        }  else  if(FTT_TAX_AMOUNT.equalsIgnoreCase(columnName)) {
            return "0";
        }  else  if(FTT_PLACE_OF_TRADE.equalsIgnoreCase(columnName)) {
            return "XPAR";
        }  else  if(FTT_NARRATIVE.equalsIgnoreCase(columnName)) {
            return EMPTY_SPACE;
        }

        return super.getColumnValue(row, columnName, errors);
    }

    public Object formatNumberOutput(DisplayValue obj) {
        obj = new Amount(obj.get(),2);

        String valorString = obj.toString();
        valorString = valorString.replace("-", "");
        valorString = valorString.replace("(", "");
        valorString = valorString.replace(")", "");
        valorString = valorString.replace(",", "");
        valorString = valorString.replace(".", ",");
        return valorString;
    }

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(REPORT_DATE);
        treeList.add(REPORT_ROW_SEQ);
        treeList.add(FTT_REFERENCE_ID);
        treeList.add(FTT_STANDARD_S);
        treeList.add(FTT_STANDARD_N);
        treeList.add(FTT_TRADE_DATE);
        treeList.add(FTT_SETTLE_DATE);
        treeList.add(FTT_QUANTITY);
        treeList.add(FTT_UNIT);
        treeList.add(FTT_CASH_AMOUNT);
        treeList.add(FTT_TAXABLE_FLAG);
        treeList.add(FTT_EXONERATION_CODE);
        treeList.add(FTT_TAX_AMOUNT);
        treeList.add(FTT_PLACE_OF_TRADE);
        treeList.add(FTT_NARRATIVE);
        return treeList;
    }

    public String getFormatQuantity(final Object value) {

        if (value instanceof  DisplayValue) {
            final DecimalFormat myFormatter = new DecimalFormat("###0");
            final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
            myFormatter.setDecimalFormatSymbols(tmp);
            if (value != null) {
                return myFormatter.format(Math.abs(((DisplayValue)value).get()));
            }
        }
        return "";
    }

}
