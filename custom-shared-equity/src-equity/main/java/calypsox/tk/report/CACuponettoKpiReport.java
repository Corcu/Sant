package calypsox.tk.report;


import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CA;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;


public class CACuponettoKpiReport {


    private static final String COLOR_TITLE = "#F1C40F";
    private static final String COLOR_SUBTITLE = "#F9E79F";
    private static final String COLOR_RANGE_GREEN = "#27AE60";
    private static final String COLOR_RANGE_YELLOW = "#F4D03F";
    private static final String COLOR_RANGE_RED = "#E74C3C";
    private static final String COLOR_POSITIVE = "#D4EFDF";
    private static final String COLOR_NEGATIVE = "#FADBD8";


    public static String getContent(DSConnection dsConn, JDatetime valDatetime, PricingEnv pEnv, Vector<String> holidays){

        CACuponettoKpiReport report = new CACuponettoKpiReport();

        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append(report.getHeaderCaCuponettoKpisDiarios(valDatetime));
        htmlInfo.append(report.getTrableCaCuponettoKpisDiarios(dsConn, valDatetime));
        htmlInfo.append(report.getTableCaCuponettoKpiDesgloseCobros(dsConn, valDatetime, pEnv));
        htmlInfo.append(report.getTableCaCuponettoKpiDesglosePagos(dsConn, valDatetime, pEnv));

        return htmlInfo.toString();
    }



    /************************
     ************************
     *****   CABECERA   *****
     ************************
     ************************/


    public String getHeaderCaCuponettoKpisDiarios(JDatetime datetime) {
        StringBuilder htmlInfo = new StringBuilder();
        final JDate date = datetime.getJDate(TimeZone.getDefault());
        htmlInfo.append("<div align='center' style=\"color: red;\"><b>");
        htmlInfo.append("INFORME KPI&acute;s");
        htmlInfo.append("<br><br>");
        htmlInfo.append("FECHA DATOS DIARIOS: " + date.getDayOfMonth() + "/" + date.getMonth() + "/" + date.getYear());
        htmlInfo.append("</div><b><br></span>");
        return htmlInfo.toString();
    }


    /***********************
     ***********************
     *****   TABLA 1   *****
     ***********************
     ***********************/


    public String getTrableCaCuponettoKpisDiarios(DSConnection dsCon, JDatetime datetime){

        int numerador1 = 0;
        int numerador2 = 0;
        int numerador3 = 0;
        int numerador4 = 0;
        int numerador5 = 0;
        int denominador1 = 0;
        int denominador2 = 0;
        int denominador3 = 0;
        int denominador4 = 0;
        int denominador5 = 0;

        // Coupon claims to be paid/received by Santander pending of management before EOD
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = datetime.getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault());
            StringBuilder fromClause = new StringBuilder("product_desc");
            StringBuilder whereClause = new StringBuilder("trade.product_id=product_desc.product_id AND product_desc.product_type='CA'");
            whereClause.append(" AND trade.trade_status<>'CANCELED'");
            whereClause.append(" AND trade.le_role='CounterParty'");
            whereClause.append(" AND TO_CHAR(trade.entered_date,'YYYY-MM-DD') = '" + sdf.format(date) + "'");
            final TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
            if(!Util.isEmpty(tradeArray)){
                Iterator<Trade> ite = tradeArray.iterator();
                while (ite.hasNext()) {
                    final Trade trade = ite.next();
                    if (!(trade.getProduct() instanceof CA)) {
                        break;
                    }
                    CA ca = (CA) trade.getProduct();
                    // PAY
                    if (ca.getBuySell(trade) < 0) {
                        denominador1++;
                        numerador1 = !hasXferSettled(dsCon, trade) ? ++numerador1 : numerador1;
                    }
                    // RECEIVE
                    else {
                        denominador2++;
                        numerador2 = !hasXferSettled(dsCon, trade) ? ++numerador2 : numerador2;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Remote Trades in Case 1.");
        }

        // Coupon claims not paid/received by Santander before EOD
        try {
            JDatetime startJDatetime = getTimeRangeJDatetime(datetime.add(-365, 0, 0, 0, 0), "00:00:00");
            JDatetime endJDatetime = getTimeRangeJDatetime(datetime, "23:59:59");
            StringBuilder fromClause = new StringBuilder("product_desc");
            StringBuilder whereClause = new StringBuilder("trade.product_id=product_desc.product_id AND product_desc.product_type='CA'");
            whereClause.append(" AND trade.trade_status<>'CANCELED'");
            whereClause.append(" AND trade.le_role='CounterParty'");
            whereClause.append(" AND trade.entered_date>=");
            whereClause.append(Util.datetime2SQLString(startJDatetime));
            whereClause.append(" AND trade.entered_date<=");
            whereClause.append(Util.datetime2SQLString(endJDatetime));
            final TradeArray tradeArray = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
            if(!Util.isEmpty(tradeArray)){
                Iterator<Trade> ite = tradeArray.iterator();
                while (ite.hasNext()) {
                    final Trade trade = ite.next();
                    if (!(trade.getProduct() instanceof CA)) {
                        break;
                    }
                    CA ca = (CA) trade.getProduct();
                    // PAY
                    if (ca.getBuySell(trade) < 0) {
                        denominador3++;
                        numerador3 = !hasXferSettled(dsCon, trade) ? ++numerador3 : numerador3;
                    }
                    // RECEIVE
                    else {
                        denominador4++;
                        numerador4 = !hasXferSettled(dsCon, trade) ? ++numerador4 : numerador4;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Remote Trades in Case 2.");
        }

        // Number of coupon claims pending receipt of payment for more than 60 days
        try {
            JDatetime startJDatetime = getTimeRangeJDatetime(datetime.add(-60, 0, 0, 0, 0), "00:00:00");
            JDatetime endJDatetime = getTimeRangeJDatetime(datetime, "23:59:59");
            StringBuilder fromClause = new StringBuilder("product_desc");
            StringBuilder whereClause = new StringBuilder("trade.product_id= product_desc.product_id AND product_desc.product_type='CA'");
            whereClause.append(" AND trade.trade_status<>'CANCELED'");
            whereClause.append(" AND trade.le_role='CounterParty'");
            whereClause.append(" AND trade.quantity>0");
            whereClause.append(" AND trade.entered_date<=");
            whereClause.append(Util.datetime2SQLString(startJDatetime));
            final TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
            if(!Util.isEmpty(tradeArray)){
                Iterator<Trade> ite = tradeArray.iterator();
                while (ite.hasNext()) {
                    final Trade trade = ite.next();
                    if (!(trade.getProduct() instanceof CA)) {
                        break;
                    }
                    CA ca = (CA) trade.getProduct();
                    // RECEIVE
                    if (ca.getBuySell(trade) > 0) {
                        denominador5++;
                        numerador5 = !hasXferSettled(dsCon, trade) ? ++numerador5 : numerador5;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Remote Trades in Case 3.");
        }

        long numValues[] = new long[5];
        long denValues[] = new long[5];
        numValues[0] = numerador1;
        numValues[1] = numerador2;
        numValues[2] = numerador3;
        numValues[3] = numerador4;
        numValues[4] = numerador5;
        denValues[0] = denominador1;
        denValues[1] = denominador2;
        denValues[2] = denominador3;
        denValues[3] = denominador4;
        denValues[4] = denominador5;
        return getHtmlCaCuponettoKpisDiarios(numValues, denValues);
    }


    public String getHtmlCaCuponettoKpisDiarios(long[] numValues, long[] denValues){

        DecimalFormat df = new DecimalFormat("#.##");
        double porcentaje1 = numValues[0]==0 ? 0.0 : Double.valueOf(df.format((double)numValues[0]*100/denValues[0]).replace(",","."));
        double porcentaje2 = numValues[1]==0 ? 0.0 : Double.valueOf(df.format((double)numValues[1]*100/denValues[1]).replace(",","."));
        double porcentaje3 = numValues[2]==0 ? 0.0 : Double.valueOf(df.format((double)numValues[2]*100/denValues[2]).replace(",","."));
        double porcentaje4 = numValues[3]==0 ? 0.0 : Double.valueOf(df.format((double)numValues[3]*100/denValues[3]).replace(",","."));
        double porcentaje5 = numValues[4]==0 ? 0.0 : Double.valueOf(df.format((double)numValues[4]*100/denValues[4]).replace(",","."));

        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append("<br><br>");
        htmlInfo.append("<table border='1' width='580' align='center' style='font-size:6.0pt;font-family:SansSerif'>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_TITLE + "' colspan='4' align='center'><b>KPI&acute;S DIARIOS</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_SUBTITLE + "' width='340' align='center'><b>CONTROL</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_SUBTITLE + "' width='80' align='center'><b>NUMERADOR</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_SUBTITLE + "' width='80' align='center'><b>DENOMINADOR</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_SUBTITLE + "' width='80' align='center'><b>%</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='left'><b>Claims to be paid by Santander pending of management before EOD</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[0] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + denValues[0] + "</b></td>");
        htmlInfo.append("<td bgcolor='" + getColorByRange(porcentaje1) + "' align='center'><b>" + porcentaje1 + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='left'><b>Claims to be received by Santander pending of management before EOD</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[1] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + denValues[1] + "</b></td>");
        htmlInfo.append("<td bgcolor='" + getColorByRange(porcentaje2) + "' align='center'><b>" + porcentaje2 + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='left'><b>Claims not paid by Santander before EOD (365 days)</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[2] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + denValues[2] + "</b></td>");
        htmlInfo.append("<td bgcolor='" + getColorByRange(porcentaje3) + "' align='center'><b>" + porcentaje3 + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='left'><b>Claims not received by Santander before EOD (365 days)</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[3] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + denValues[3] + "</b></td>");
        htmlInfo.append("<td bgcolor='" + getColorByRange(porcentaje4) + "' align='center'><b>" + porcentaje4 + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='left'><b>Number of claims pending receipt of payment for more than 60 days</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[4] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + denValues[4] + "</b></td>");
        htmlInfo.append("<td bgcolor='" + getColorByRange(porcentaje5) + "' align='center'><b>" + porcentaje5 + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("</table>");

        return htmlInfo.toString();
    }


    /***********************
     ***********************
     *****   TABLA 2   *****
     ***********************
     ***********************/


    public String getTableCaCuponettoKpiDesgloseCobros(DSConnection dsConn, JDatetime datetime, PricingEnv pEnv){
        int numCobros1 = 0;
        int numCobros2 = 0;
        int numCobros3 = 0;
        int numCobros4 = 0;
        int numCobros5 = 0;
        int numCobros6 = 0;
        double sumaCobros1 = 0;
        double sumaCobros2 = 0;
        double sumaCobros3 = 0;
        double sumaCobros4 = 0;
        double sumaCobros5 = 0;
        double sumaCobros6 = 0;

        int numCobrosOther = 0;
        double sumaCobrosOther = 0;

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            JDatetime startJDatetime = getTimeRangeJDatetime(datetime.add(-365, 0, 0, 0, 0), "00:00:00");
            JDatetime endJDatetime = getTimeRangeJDatetime(datetime, "23:59:59");
            StringBuilder fromClause = new StringBuilder("product_desc, trade");
            StringBuilder whereClause = new StringBuilder("trade.product_id=product_desc.product_id AND product_desc.product_type='CA'");
            whereClause.append(" AND bo_transfer.transfer_status NOT IN ('CANCELED', 'SETTLED', 'SPLIT')");
            whereClause.append(" AND bo_transfer.trade_id=trade.trade_id");
            whereClause.append(" AND trade.le_role='CounterParty'");
            whereClause.append(" AND trade.quantity>0");
            whereClause.append(" AND trade.entered_date>=");
            whereClause.append(Util.datetime2SQLString(startJDatetime));
            whereClause.append(" AND trade.entered_date<=");
            whereClause.append(Util.datetime2SQLString(endJDatetime));
            final TransferArray transferArray = DSConnection.getDefault().getRemoteBO().getBOTransfers(fromClause.toString(), whereClause.toString(), null, 0, null);
            if(!Util.isEmpty(transferArray)){
                Iterator<BOTransfer> ite = transferArray.iterator();
                while (ite.hasNext()) {
                    final BOTransfer xfer = ite.next();
                    if(Status.S_SETTLED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus()) || Status.S_CANCELED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus())){
                        continue;
                    }
                    String estadoDeGestion = xfer.getAttribute("EstadodeGestion");
                    if(Util.isEmpty(estadoDeGestion)){
                        numCobrosOther++;
                        sumaCobrosOther = sumaCobrosOther + xfer.getSettlementAmount();
                        Log.system(this.getClass().toString(), "Estado de Gestion: '" + estadoDeGestion + "' no contemplado para cobro de xfer " + xfer.getLongId());
                        continue;
                    }
                    switch(estadoDeGestion){
                        case "Agreed":
                            numCobros1++;
                            sumaCobros1 = sumaCobros1 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Escalated":
                            numCobros2++;
                            sumaCobros2 = sumaCobros2 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Chaised":
                            numCobros3++;
                            sumaCobros3 = sumaCobros3 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Claimed":
                            numCobros4++;
                            sumaCobros4 = sumaCobros4 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Discrepancy":
                            numCobros5++;
                            sumaCobros5 = sumaCobros5 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Pending":
                            numCobros6++;
                            sumaCobros6 = sumaCobros6 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        default:
                            numCobrosOther++;
                            sumaCobrosOther = sumaCobrosOther + xfer.getSettlementAmount();
                            Log.system(this.getClass().toString(), "Estado de Gestion: '" + estadoDeGestion + "' no contemplado para cobro de xfer " + xfer.getLongId());
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Remote Trades in Table 2.");
        }

        Log.system(this.getClass().toString(), "Xfers de Cobro sin 'Estado de Gestion' contemplados: " + numCobrosOther + " - " + sumaCobrosOther);

        int numValues[] = new int[6];
        double sumValues[] = new double[6];
        numValues[0] = numCobros1;
        numValues[1] = numCobros2;
        numValues[2] = numCobros3;
        numValues[3] = numCobros4;
        numValues[4] = numCobros5;
        numValues[5] = numCobros6;
        sumValues[0] = sumaCobros1;
        sumValues[1] = sumaCobros2;
        sumValues[2] = sumaCobros3;
        sumValues[3] = sumaCobros4;
        sumValues[4] = sumaCobros5;
        sumValues[5] = sumaCobros6;

        return getHtmlCaCuponettoKpiDesgloseCobros(numValues, sumValues);
    }


    public String getHtmlCaCuponettoKpiDesgloseCobros(int[] numValues, double[] sumaValues) {

        int numCobros = numValues[0] + numValues[1] + numValues[2] + numValues[3] + numValues[4] + numValues[5];
        Double sumaCobros = sumaValues[0] + sumaValues[1] + sumaValues[2] + sumaValues[3] + sumaValues[4] + sumaValues[5];

        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append("<br><br>");
        htmlInfo.append("<table border='1' width='450' align='center' style='font-size:6.0pt;font-family:SansSerif'>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_TITLE + "' colspan='5' align='center'><b>KPI&acute;S DESGLOSE - COBROS</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SITUACI&Oacute;N</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SENTIDO</b></td>");
        htmlInfo.append("<td width='130' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>ESTADO</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>N&deg</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SUMA</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Agreed</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[0] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[0]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Escalated</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[1] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[1]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Chased</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[2] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[2]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Claimed</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[3] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[3]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Discrepancy</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[4] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[4]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Cobro</b></td>");
        htmlInfo.append("<td align='center'><b>Pending</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[5] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[5]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_TITLE + "' colspan='3' align='center'><b>TOTAL</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_POSITIVE + "' align='center'><b>" + numCobros + "</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_POSITIVE + "' align='center'><b>" + formatAmount(sumaCobros) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("</table>");
        return htmlInfo.toString();
    }


    /***********************
     ***********************
     *****   TABLA 3   *****
     ***********************
     ***********************/


    public String getTableCaCuponettoKpiDesglosePagos(DSConnection dsConn, JDatetime datetime, PricingEnv pEnv){
        int numPagos1 = 0;
        int numPagos2 = 0;
        int numPagos3 = 0;
        int numPagos4 = 0;
        int numPagos5 = 0;
        int numPagos6 = 0;
        double sumaPagos1 = 0;
        double sumaPagos2 = 0;
        double sumaPagos3 = 0;
        double sumaPagos4 = 0;
        double sumaPagos5 = 0;
        double sumaPagos6 = 0;

        int numPagosOther = 0;
        double sumaPagosOther = 0;

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            JDatetime startJDatetime = getTimeRangeJDatetime(datetime.add(-365, 0, 0, 0, 0), "00:00:00");
            JDatetime endJDatetime = getTimeRangeJDatetime(datetime, "23:59:59");
            StringBuilder fromClause = new StringBuilder("product_desc, trade");
            StringBuilder whereClause = new StringBuilder("trade.product_id=product_desc.product_id AND product_desc.product_type='CA'");
            whereClause.append(" AND bo_transfer.transfer_status NOT IN ('CANCELED', 'SETTLED', 'SPLIT  ')");
            whereClause.append(" AND bo_transfer.trade_id=trade.trade_id");
            whereClause.append(" AND trade.le_role='CounterParty'");
            whereClause.append(" AND trade.quantity<0");
            whereClause.append(" AND trade.entered_date>=");
            whereClause.append(Util.datetime2SQLString(startJDatetime));
            whereClause.append(" AND trade.entered_date<=");
            whereClause.append(Util.datetime2SQLString(endJDatetime));
            final TransferArray transferArray = DSConnection.getDefault().getRemoteBO().getBOTransfers(fromClause.toString(), whereClause.toString(), null, 0, null);
            if(!Util.isEmpty(transferArray)){
                Iterator<BOTransfer> ite = transferArray.iterator();
                while (ite.hasNext()) {
                    final BOTransfer xfer = ite.next();
                    if(Status.S_SETTLED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus()) || Status.S_CANCELED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus())){
                        continue;
                    }
                    String estadoDeGestion = xfer.getAttribute("EstadodeGestion");
                    if(Util.isEmpty(estadoDeGestion)){
                        numPagosOther++;
                        sumaPagosOther = sumaPagosOther + xfer.getSettlementAmount();
                        Log.system(this.getClass().toString(), "Estado de Gestion: '" + estadoDeGestion + "' no contemplado para pago de xfer " + xfer.getLongId());
                        continue;
                    }
                    switch(estadoDeGestion){
                        case "Agreed":
                            numPagos1++;
                            sumaPagos1 = sumaPagos1 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Escalated":
                            numPagos2++;
                            sumaPagos2 = sumaPagos2 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Chaised":
                            numPagos3++;
                            sumaPagos3 = sumaPagos3 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Claimed":
                            numPagos4++;
                            sumaPagos4 = sumaPagos4 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Discrepancy":
                            numPagos5++;
                            sumaPagos5 = sumaPagos5 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        case "Pending":
                            numPagos6++;
                            sumaPagos6 = sumaPagos6 + getAmountInEur(xfer, pEnv, datetime);
                            break;
                        default:
                            numPagosOther++;
                            sumaPagosOther = sumaPagosOther + xfer.getSettlementAmount();
                            Log.system(this.getClass().toString(), "Estado de Gestion: '" + estadoDeGestion + "' no contemplado para pago de xfer " + xfer.getLongId());
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading Remote Trades in Table 2.");
        }

        Log.system(this.getClass().toString(), "Xfers de Pago sin 'Estado de Gestion' contemplados: " + numPagosOther + " - " + sumaPagosOther);

        int numValues[] = new int[6];
        double sumaValues[] = new double[6];
        numValues[0] = numPagos1;
        numValues[1] = numPagos2;
        numValues[2] = numPagos3;
        numValues[3] = numPagos4;
        numValues[4] = numPagos5;
        numValues[5] = numPagos6;
        sumaValues[0] = sumaPagos1;
        sumaValues[1] = sumaPagos2;
        sumaValues[2] = sumaPagos3;
        sumaValues[3] = sumaPagos4;
        sumaValues[4] = sumaPagos5;
        sumaValues[5] = sumaPagos6;
        return getHtmlCaCuponettoKpiDesglosePagos(numValues, sumaValues);
    }


    public String getHtmlCaCuponettoKpiDesglosePagos(int[] numValues, double[] sumaValues){

        int numPagosTotal = numValues[0] + numValues[1] + numValues[2] + numValues[3] + numValues[4] + numValues[5];
        double sumaPagos = sumaValues[0] + sumaValues[1] + sumaValues[2] + sumaValues[3] + sumaValues[4] + sumaValues[5];

        StringBuilder htmlInfo = new StringBuilder();
        htmlInfo.append("<br><br>");
        htmlInfo.append("<table border='1' width='450' align='center' style='font-size:6.0pt;font-family:SansSerif'>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_TITLE + "' colspan='5' align='center'><b>KPI&acute;S DESGLOSE - PAGOS</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SITUACI&Oacute;N</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SENTIDO</b></td>");
        htmlInfo.append("<td width='130' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>ESTADO</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>N&deg</b></td>");
        htmlInfo.append("<td width='80' bgcolor='" + COLOR_SUBTITLE + "' align='center'><b>SUMA</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Agreed</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[0] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[0]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Escalated</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[1] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[1]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Chased</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[2] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[2]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Claimed</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[3] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[3]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Discrepancy</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[4] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[4]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td align='center'><b>VIVO</b></td>");
        htmlInfo.append("<td align='center'><b>Pago</b></td>");
        htmlInfo.append("<td align='center'><b>Pending</b></td>");
        htmlInfo.append("<td align='center'><b>" + numValues[5] + "</b></td>");
        htmlInfo.append("<td align='center'><b>" + formatAmount(sumaValues[5]) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("<tr style='height:20px'>");
        htmlInfo.append("<td bgcolor='" + COLOR_TITLE + "' colspan='3' align='center'><b>TOTAL</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_NEGATIVE  + "' align='center'><b>" + numPagosTotal + "</b></td>");
        htmlInfo.append("<td bgcolor='" + COLOR_NEGATIVE + "' align='center'><b>" + formatAmount(sumaPagos) + "</b></td>");
        htmlInfo.append("</tr>");
        htmlInfo.append("</table>");
        return htmlInfo.toString();
    }


    /***********************
     ***********************/

    /**
    private boolean hasNotificationSent(DSConnection dsCon, Trade trade){
        try {
            final MessageArray messageArray = dsCon.getRemoteBO().getMessages("trade_id=" + trade.getLongId(), null);
            if(messageArray!=null && messageArray.size()>0){
                Iterator<BOMessage> ite = messageArray.iterator();
                while (ite.hasNext()) {
                    final BOMessage msg = ite.next();
                    if("CA_NOTIF".equalsIgnoreCase(msg.getMessageType()) && Status.S_SENT.getStatus().equalsIgnoreCase(msg.getStatus().getStatus())){
                        return true;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading BOMessages of trade " + trade.getLongId());
        }
        return false;
    }
    */

    private boolean hasXferSettled(DSConnection dsCon, Trade trade){
        try {
            final TransferArray transferArray = dsCon.getRemoteBO().getBOTransfers(trade.getLongId());
            if(transferArray!=null && transferArray.size()>0){
                Iterator<BOTransfer> ite = transferArray.iterator();
                while (ite.hasNext()) {
                    final BOTransfer xfer = ite.next();
                    if(Status.S_SETTLED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus())){
                        return true;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error loading BOMessages of trade " + trade.getLongId());
        }
        return false;
    }


    private String getColorByRange(Double num){
        String colour = "#FFFFFF";
        if(num<=5.0){
            colour = COLOR_RANGE_GREEN;
        }
        else if(num>5.0 && num<=10){
            colour = COLOR_RANGE_YELLOW;
        }
        else if(num>10){
            colour = COLOR_RANGE_RED;
        }
        return colour;
    }


    protected JDatetime getTimeRangeJDatetime(final JDatetime jValDatetime, final String time) {
        final String valDateFormatted = formatDate(jValDatetime.getJDate(TimeZone.getDefault()), "dd/MM/yyyy");
        final String range = valDateFormatted + " " + time;
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        Date d;
        JDatetime jDatetime = null;
        try {
            d = sdf.parse(range);
            jDatetime = new JDatetime(d);
        } catch (final ParseException e) {
            Log.error(this, String.format("Could not parse the date \"%s\"", range), e);
        }
        return jDatetime;
    }


    private String formatDate(final JDate jDate, final String format) {
        final Date date = jDate.getDate(TimeZone.getDefault());
        final SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }


    private String formatAmount(final double amount) {
        BigDecimal bd = new BigDecimal(amount);
        return String.format("%.2f", bd);
    }


    private Double getAmountInEur(BOTransfer xfer, PricingEnv pEnv, JDatetime datetime){
        if(!"EUR".equalsIgnoreCase(xfer.getSettlementCurrency())) {
            try {
                return CollateralUtilities.convertCurrency(xfer.getSettlementCurrency(), xfer.getSettlementAmount(),"EUR", datetime.getJDate(TimeZone.getDefault()), pEnv);
            }catch (MarketDataException e){
                Log.warn(this.getClass().getName(), "Can not convert to EUR currency: " + e);
            }
        }
        return xfer.getSettlementAmount();
    }


}
