package calypsox.tk.bo;


import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;


public class CO2_NOTIFEquityMessageFormatter extends MessageFormatter {


    private static final String TRADE_KEYWORD_MUREX_ROOT_CONTRACT = "MurexRootContract";
    private static final String PRODUCT_EQUITY_SEC_CODE_CO2_FACTURA_DESCRIPTION = "CO2_FACTURA_DESCRIPTION";


    public String parseDATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        JDate date = JDate.getNow();
        return formatDate(date,"dd/MM/yyyy","Fecha");
    }


    public String parseTRADE_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        JDate date = JDate.valueOf(trade.getTradeDate(), trade.getBook().getLocation());
        return formatDate(date,"dd/MM/yyyy","Fecha");
    }


    public String parseSETTLE_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        JDate date = trade.getSettleDate();
        return formatDate(date,"dd/MM/yyyy","Fecha");
    }

    public String parseTRADE_SETTLEMENT_CURRENCY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        return trade.getSettleCurrency();
    }


    public String parseMUREX_ROOT_CONTRACT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {
        return trade.getKeywordValue(TRADE_KEYWORD_MUREX_ROOT_CONTRACT);
    }


    public String parseCPTY_CODE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                            BOTransfer transfer, DSConnection dsConn) {
        return trade.getCounterParty().getCode();
    }


    public String parseCPTY_NAME(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {
        return trade.getCounterParty().getName();
    }


    public String parseISIN_DESC(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {
        String isinDesc = "";
        if (trade.getProduct() instanceof Equity) {
            Equity equity = (Equity) trade.getProduct();
            isinDesc = equity.getSecCode(PRODUCT_EQUITY_SEC_CODE_CO2_FACTURA_DESCRIPTION);
        }
        return isinDesc;
    }


    public String parseCANCELLATION_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {
        try {
            String where = "ENTITY_ID=" + trade.getLongId() + " AND ENTITY_CLASS_NAME='Trade' AND NEW_VALUE='CANCELED'";
            Vector<?> auditValues= DSConnection.getDefault().getRemoteTrade().getAudit(where,null, null);
            if(!Util.isEmpty(auditValues) && auditValues.size()>0){
                JDatetime date = ((AuditValue) auditValues.get(0)).getModifDate();
                if(date != null) {
                    return formatDate(date.getJDate(TimeZone.getDefault()),"dd/MM/yyyy", "Cancellation Date");
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Could not get the audit for the trade " + trade.getLongId());
        }

        return "";
    }


    private String formatDate(JDate jDate, String format, String field){
        String messageCreationDate = null;
        try {
            JDatetime dateTime = jDate.getJDatetime(TimeZone.getDefault());
            messageCreationDate = (new SimpleDateFormat(format)).format(dateTime);
        } catch (IllegalArgumentException e) {
            Log.error("AbstractFormatter", "Format " + format + " for " + field + " is not correct.");
        }
        return messageCreationDate;
    }


    public String parseTABLE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {

        String date = parseDATE(message, trade, po, cp, paramVector, transfer, dsConn);
        String murexRootContract = parseMUREX_ROOT_CONTRACT(message, trade, po, cp, paramVector, transfer, dsConn);
        String cptyCode = parseCPTY_CODE(message, trade, po, cp, paramVector, transfer, dsConn);
        String cptyName = parseCPTY_NAME(message, trade, po, cp, paramVector, transfer, dsConn);
        String tradeSettlementAmount = parseTRADE_SETTLEMENT_AMOUNT(message, trade, po, cp, paramVector, transfer, dsConn);
        String tradeSettlementCurrency = parseTRADE_SETTLEMENT_CURRENCY(message, trade, po, cp, paramVector, transfer, dsConn);
        String tradeDate = parseTRADE_DATE(message, trade, po, cp, paramVector, transfer, dsConn);
        String settleDate = parseSETTLE_DATE(message, trade, po, cp, paramVector, transfer, dsConn);
        String nominal = parseTRADE_NOMINAL(message, trade, po, cp, paramVector, transfer, dsConn);
        String isinDesc = parseISIN_DESC(message, trade, po, cp, paramVector, transfer, dsConn);

        StringBuilder html = new StringBuilder();
        html.append("<table width='600' style='margin: 20px;font-size:9.0pt;' CELLSPACING='2'>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Date:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + date + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Reference:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>"+ murexRootContract + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>GLCS:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + cptyCode + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Name:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + cptyName + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Amount:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + tradeSettlementAmount + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Currency:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + tradeSettlementCurrency + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Trade Date:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + tradeDate + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Value Date:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + settleDate + "</td>");
        html.append("</tr>");
        if(Action.S_CANCEL.toString().equalsIgnoreCase(message.getSubAction().toString())) {
            String cancellationDate = parseCANCELLATION_DATE(message, trade, po, cp, paramVector, transfer, dsConn);
            html.append("<tr>");
            html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Cancellation Date:</b></td>");
            html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + cancellationDate + "</td>");
            html.append("</tr>");
        }
        html.append("<tr>");
        html.append(" <td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Buy/Sell:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>Sell</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Nominal:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + nominal + "</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td width='100' style='font-family: SansSerif;mso-tab-count:1'><b>Description:</b></td>");
        html.append("<td width='500' style='font-family: SansSerif;mso-tab-count:3'>" + isinDesc + "</td>");
        html.append("</tr>");
        html.append("</table>");

        return html.toString();
    }


    public String parseTITLE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                             BOTransfer transfer, DSConnection dsConn) {
        String canceled = Action.S_CANCEL.toString().equalsIgnoreCase(message.getSubAction().toString()) ? " (CANCELED)" : "";
        return "Venta derechos CO2 - " + parseMUREX_ROOT_CONTRACT(message, trade, po, cp, paramVector, transfer, dsConn) + canceled;
    }


}
