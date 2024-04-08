package calypsox.tk.bo;


import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.AdviceDocumentBuilder;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.factory.LegalEntityRoleEnum;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

import org.drools.compiler.lang.dsl.DSLMapParser.mapping_file_return;


public class CAMessageFormatter extends MessageFormatter {


    public static final String NOTIF_EMAIL_TEMPLATE = "CorporateEventNotice.htm";
    public static final String NOTICE_EMAIL_TEMPLATE = "CorporateEventDeliveryNotice.html";
    public static final String CORPORATE_EVENTS_FROM_EMAIL = "CORPORATE_EVENTS_FROM_EMAIL";
    private static final String CA_SOURCE_PRODUCT_TYPE = "CASourceProductType";
    private static final String CA_SOURCE = "CASource";
    private static final String PO_ATTR_LOGO = "LOGO";
    private static final String DEFAULT_LOGO_NAME = "BSTE";
    private static final String LOGO_FILE_EXT = ".png";
    private static DefaultReportOutput reportOutput = null;


    public String parseTRADE_QUANTITY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                   BOTransfer transfer, DSConnection dsCon) {
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            if (ca.getUnderlyingProduct() instanceof Equity) {
                return super.parseTRADE_QUANTITY(message, trade, sender, rec, transferRules, transfer, dsCon);
            }
            else if (ca.getUnderlyingProduct() instanceof Bond || ca.getUnderlyingProduct() instanceof BondMMDiscount || ca.getUnderlyingProduct() instanceof BondMMInterest) {
                String cur = ca.getCurrency();
                if (cur == null) {
                    cur = trade.getTradeCurrency();
                }
                Product underlying = ca.getUnderlying();
                if (underlying != null) {
                    double amt = underlying.computeNominal(trade);
                    int digits = 2;
                    if (cur != null) {
                        int roundingMethod = CurrencyUtil.getRoundingMethod(cur);
                        amt = RoundingMethod.round(amt, digits, roundingMethod);
                    }
                    return new SignedAmount(Math.abs(amt), digits).toString();
                }
            }
        }
        return "";
    }


    public String parseRECORD_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                   BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return parseJDate(message, ca.getRecordDate());
        }
        return "";
    }

    public String parseEX_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                               BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return parseJDate(message, ca.getExDate());
        }
        return "";
    }

    public String parseTAX_RATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return String.valueOf(ca.getPaymentFxRate());
        }
        return null;
    }

    public String parseCA_NOMINAL(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                  BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return numberToString(ca.computeNominal(trade));
        }
        return "";
    }

    public String parsePAYMENT_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                    BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return formatJDate(ca.getCashPayValueDate(), "ddMMyyyy", null);
        }
        return "";
    }

    public String parseCA_VALUE_DATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                     BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return formatJDate(ca.getValueDate(), "dd/MM/yyyy", null);
        }
        return "";
    }

    public String parseCA_ISIN(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                               BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            return ca.getSecurity().getSecCode("ISIN");
        }
        return "";
    }

    public String parseCA_ISIN_DESC(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                               BOTransfer transfer, DSConnection dsCon) {
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            Product underlying = ca.getUnderlyingProduct();
            if(underlying instanceof Equity){
                Equity equity = (Equity) underlying;
                return equity.getCorporateName();
            }
            else if(underlying instanceof Bond){
                return "";
            }
        }
        return "";
    }


    public String parseCA_DAYCOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                   BOTransfer transfer, DSConnection dsCon) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            if (ca.getSecurity() instanceof Bond) {
                Bond bond = (Bond) ca.getSecurity();
                return bond.getDaycount().toString();
            } else if (ca.getSecurity() instanceof Equity) {
                Equity equity = (Equity) ca.getSecurity();
                return ca.getSecurity().getProductDayCount(JDate.getNow(), trade).toString();
            }
        }
        return "";
    }

    public String parseCA_NET_PRICE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                     BOTransfer transfer, DSConnection dsCon) {
        if(trade != null) {
            int whtcId = (int)trade.getKeywordAsLongId("WithholdingTaxConfigId");
            WithholdingTaxConfig whtc = BOCache.getWithholdingTaxConfig(DSConnection.getDefault(), whtcId);
            if (whtc != null){
                return numberToString(trade.getTradePrice() * (1-whtc.getWHTRate()));
            }
            return numberToString(trade.getTradePrice());
        }
        return "";
    }


    public String parseCA_GROSS_PRICE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                     BOTransfer transfer, DSConnection dsCon) {
        if(trade != null) {
            return numberToString(trade.getTradePrice());
        }
        return "";
    }


    public String parseCA_GROSS_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                      BOTransfer transfer, DSConnection dsCon) {
        if(trade != null) {
            Double grossAmount = trade.getQuantity() * trade.getTradePrice();
            String currency = parseTRADE_CURRENCY(message, trade, sender, rec, transferRules, transfer, dsCon);
            return numberToString(CurrencyUtil.roundAmount(Math.abs(grossAmount), currency));
        }
        return "";
    }


    public String parseCA_NET_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                       BOTransfer transfer, DSConnection dsCon) {
        if(trade != null) {
            Double netAmount = trade.getProduct().calcSettlementAmount(trade);
            String currency = parseTRADE_CURRENCY(message, trade, sender, rec, transferRules, transfer, dsCon);
            return numberToString(CurrencyUtil.roundAmount(Math.abs(netAmount), currency));
        }
        return "";
    }


    public String parseCA_SENDER_SIGN(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                      BOTransfer transfer, DSConnection dsCon) {

        String sign = sender.getComment();
        return sign.replaceAll("\n", "<br/>");
    }


    public String parseNOTIF_FROM_EMAIL(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (sender == null) {
            return "";
        }
        return sender.getAddressCode(CORPORATE_EVENTS_FROM_EMAIL);
    }

    public String parseCA_SOURCE_PRODUCT_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                              Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String value = trade.getKeywordValue(CA_SOURCE_PRODUCT_TYPE);
            if (!Util.isEmpty(value) && "MarginCall".equalsIgnoreCase(value)) {
                return "Collateral";
            }
            return trade.getKeywordValue(CA_SOURCE_PRODUCT_TYPE);
        }
        return "";
    }

    public String parseCA_SUBTYPE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                  BOTransfer transfer, DSConnection dsConn) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String subType = ca.getSubType();
            if ("INTEREST".equalsIgnoreCase(subType)) {
                subType = "Coupon";
            }
            subType = subType.toUpperCase().charAt(0) + subType.substring(1, subType.length()).toLowerCase();

            return subType;
        }
        return "";

    }

    public String parseCA_PAY_REC(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                  BOTransfer transfer, DSConnection dsConn) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            if (ca.getBuySell(trade) > 0) {
                return "Receivable";
            } else {
                return "Payment";
            }
        }
        return "";

    }


    public String parseCA_PAY_RECEIVE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                  BOTransfer transfer, DSConnection dsConn) {
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            if (ca.getBuySell(trade) > 0) {
                return "Receive";
            } else {
                return "Pay";
            }
        }
        return "";

    }


    public String parseCA_PRODUCT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                  BOTransfer transfer, DSConnection dsConn) {

        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String type = ca.getCAType();
            String subType = ca.getSubType();
            type = type.toUpperCase().charAt(0) + type.substring(1, type.length()).toLowerCase();
            subType = subType.toUpperCase().charAt(0) + subType.substring(1, subType.length()).toLowerCase();

            return type + "/" + subType;
        }
        return "";

    }

    public String parseTITLE_DETAILS(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {

        String subType = parseCA_SUBTYPE(message, trade, po, cp, paramVector, transfer, dsConn);
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String type = ca.getCAType();
            type = type.toUpperCase().charAt(0) + type.substring(1, type.length()).toLowerCase();
            return type + "/" + subType + " Movement Details";
        }
        return "";

    }

    public String parseTITLE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                             BOTransfer transfer, DSConnection dsConn) {
        if (message.getTemplateName().equalsIgnoreCase("CorporateEventGroupNotice.html")) {
            String template = message.getAttribute("ReportTemplate");
            String rango = "";
            if (!Util.isEmpty(template)){
                if (template.equalsIgnoreCase("CA_Claims_Unpaid_ALL")) {
                    rango = " (All Claims)";
                }
                if (template.equalsIgnoreCase("CA_Claims_Unpaid_15D")) {
                    rango = " (15 Days)";
                }
                if (template.equalsIgnoreCase("CA_Claims_Unpaid_1M")) {
                    rango = " (1 Month)";
                }
                if (template.equalsIgnoreCase("CA_Claims_Unpaid_7D_200000EUR_REC")) {
                    rango = " (Receive 7 days 200.000 EUR)";
                }
            }
            return "Grouped Claims " + cp.getContactName() + rango;
        }
        if (trade.getProduct() instanceof CA) {
            String subType = parseCA_SUBTYPE(message, trade, po, cp, paramVector, transfer, dsConn);
            String canceledStatus = "CANCELED".equalsIgnoreCase(trade.getStatus().getStatus()) ? "Cancel " : "";
            CA ca = (CA) trade.getProduct();
            String type = ca.getCAType();
            type = type.toUpperCase().charAt(0) + type.substring(1, type.length()).toLowerCase();
            String attr = transfer.getAttribute("EstadodeGestion");
            String urgent = !Util.isEmpty(attr) && "Escalated".equalsIgnoreCase(attr) ? "Urgent " : "";

            if (message.getTemplateName().equalsIgnoreCase("CorporateEventNotice.htm")) {
                return canceledStatus + urgent + subType + " Claim";
            } else if (message.getTemplateName().equalsIgnoreCase("CorporateEventDeliveryNotice.html")) {
                Product underlying = ca.getUnderlyingProduct();
                if(underlying != null && underlying instanceof Bond){
                    if (ca.getBuySell(trade) > 0.0) {
                        return type + "/" + subType + " Closed Funds Received";
                    } else {
                        return type + "/" + subType + " Payment Advise";
                    }
                }
                else{
                    return type + "/" + subType + " Movement Notice";
                }
            }
        }
        return "";
    }

    public String parsePARTY(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                             BOTransfer transfer, DSConnection dsConn) {
        String payRec;
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String poName = trade.getBook().getLegalEntity().getName();
            if (ca.getBuySell(trade) > 0.0) {
                payRec = "RECEIVES";
            } else {
                payRec = "DELIVERS";
            }
            return poName + " " + payRec;
        }
        return "";

    }

    public String parseLOGO(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                            BOTransfer transfer, DSConnection dsConn) {

        String poShortName = BOCache.getLegalEntityCode(dsConn, po.getLegalEntityId());
        LegalEntityAttribute poLogoType = BOCache.getLegalEntityAttribute(dsConn, po.getId(), po.getLegalEntityId(),
                po.getLegalEntityRole(), PO_ATTR_LOGO);

        if (Util.isEmpty(poShortName)) {
            // use the default logo
            poShortName = DEFAULT_LOGO_NAME;
        }
        // Logo using the attribute for Legal Entity
        if ((!Util.isEmpty(poShortName)) && (poLogoType != null)) {
            poShortName = poLogoType.getAttributeValue();
            // Check the existence of this logo
            InputStream isfor = this.getClass()
                    .getResourceAsStream("/calypsox/templates/" + poShortName + LOGO_FILE_EXT);
            try {
                if ((isfor == null) || (isfor.available() == 0)) {
                    poShortName = DEFAULT_LOGO_NAME;
                }
            } catch (IOException e) {
                poShortName = DEFAULT_LOGO_NAME;
                Log.error(this, e);//Sonar
            }
        } else { // check the existence of this logo

            InputStream is = this.getClass().getResourceAsStream("/calypsox/templates/" + poShortName + LOGO_FILE_EXT);
            try {
                if ((is == null) || (is.available() == 0)) {
                    poShortName = DEFAULT_LOGO_NAME;

                }
            } catch (IOException e) {
                poShortName = DEFAULT_LOGO_NAME;
                Log.error(this, e);//Sonar
            }
        }
        return "<img src=\"" + poShortName + LOGO_FILE_EXT + "\">";
    }

    public String parseCONTRACT_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return "";
    }

    public String parseSETTLEMENT_INSTRUCTIONS_CODE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                    Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = "";
        try {
            Vector notifSDI;
            LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE");
            // get the PO settlement instructions for notification
            notifSDI = getTemplateSDIList(po.getLegalEntityId(), trade, dsCon);
            if (!Util.isEmpty(notifSDI)) {
                List<CAMessageFormatter.SdiHtmlRowData> cashSdisToPrint = new ArrayList<>();
                List<CAMessageFormatter.SdiHtmlRowData> secSdisToPrint = new ArrayList<>();
                for (int i = 0; i < notifSDI.size(); i++) {
                    SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) notifSDI.get(i);
                    // if cash
                    if (SettleDeliveryInstruction.SETTLEMENT == sdi.getType()) {
                        Vector listCurrencies = sdi.getCurrencyList();
                        if (!Util.isEmpty(listCurrencies)) {
                            for (int j = 0; j < listCurrencies.size(); j++) {
                                CAMessageFormatter.SdiHtmlRowData row = new CAMessageFormatter.SdiHtmlRowData();
                                row.setRowType("CASH");
                                row.setSdiCurrency((String) listCurrencies.get(j));
                                setSDIHtmlAgentDetails(sdi, row, trade);
                                setSDIHtmlIntermediaryDetails_1(sdi, row, trade);
                                setSDIHtmlIntermediaryDetails_2(sdi, row, trade);
                                cashSdisToPrint.add(row);
                            }
                        } else {
                            CAMessageFormatter.SdiHtmlRowData row = new CAMessageFormatter.SdiHtmlRowData();
                            row.setRowType("CASH");
                            row.setSdiCurrency("ANY");
                            setSDIHtmlAgentDetails(sdi, row, trade);
                            setSDIHtmlIntermediaryDetails_1(sdi, row, trade);
                            setSDIHtmlIntermediaryDetails_2(sdi, row, trade);
                            cashSdisToPrint.add(row);
                        }
                    } else {
                        CAMessageFormatter.SdiHtmlRowData row = new CAMessageFormatter.SdiHtmlRowData();
                        row.setRowType("SECURITIES");
                        row.setSdiCurrency(sdi.getAttribute("Type of bonds"));
                        setSDIHtmlAgentDetails(sdi, row, trade);
                        setSDIHtmlIntermediaryDetails_1(sdi, row, trade);
                        setSDIHtmlIntermediaryDetails_2(sdi, row, trade);
                        secSdisToPrint.add(row);
                    }
                }

                result = sdisABACodeToHtml(cashSdisToPrint, secSdisToPrint);

            }
        } catch (RemoteException e) {
            Log.error(this, e);

        }

        return result;
    }

    protected Vector getTemplateSDIList(int leId, Trade trade, DSConnection dsCon) throws RemoteException {
        //gets the contract type
        String notificationType;

        //Constructs the query for extracting the SDIs
        String where = new StringBuilder("le_settle_delivery.method='NOTIFICATIONS'")
                .append(" and le_settle_delivery.bene_le=").append(leId)
                .append(" and le_settle_delivery.le_role='").append(LegalEntityRoleEnum.ProcessingOrg.getName()).append("'")
                .append(" and (le_settle_delivery.product_list='CA'")
                .append(" or le_settle_delivery.product_list='ANY')")
                .append(" and (le_settle_delivery.effective_from is null or le_settle_delivery.effective_from <= ")
                .append(ioSQL.date2String(trade.getTradeDate())).append(")")
                .append(" and (le_settle_delivery.effective_to is null or le_settle_delivery.effective_to >= ")
                .append(ioSQL.date2String(trade.getTradeDate())).append(")").toString();

        Vector vector3 = dsCon.getRemoteReferenceData().getSettleDeliveryInstructions(null, where, null);
        Vector vectorR = new Vector();
        for (final Object obj : vector3) {
            if (obj instanceof SettleDeliveryInstruction) {
                SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) obj;
                vectorR.add(obj);
            }

        }
        return vectorR;
    }

    private String sdisABACodeToHtml(List<CAMessageFormatter.SdiHtmlRowData> cashSdis, List<CAMessageFormatter.SdiHtmlRowData> secSdis) {
        StringBuffer html = new StringBuffer();
        String tableHeader = "<table width=\"100%\" class=\"sdiList\">\n";
        String tableFooter = "</table>";
        StringBuffer labelsBuffer = new StringBuffer("");
        String separator = "<tr><td colspan=\"5\" style=\"border-left: none; border-right: none;border-top: none;border-bottom: none;\"></td></tr>";

        html.append(tableHeader);
        html.append(labelsBuffer.append("<tr>").append("<td align=\"center\"><b>CASH/SECURITIES</b></td>")
                .append("<td align=\"center\"><b>TYPE</b></td>")
                // .append("<td align=\"center\"><b>AGENT</b></td>")
                .append("<td align=\"center\"><b>BIC AGENT</b></td>")
                .append("<td align=\"center\"><b>ACCOUNT</b></td>")
                //.append("<td align=\"center\"><b>INTERMEDIARY 1</b></td>")
                .append("<td align=\"center\"><b>BIC INTERM 1</b></td>")
                .append("<td align=\"center\"><b>ACCOUNT</b></td>")
                //  .append("<td align=\"center\"><b>INTERMEDIARY 2</b></td>")
                .append("<td align=\"center\"><b>BIC INTERM 2</b></td>")
                .append("<td align=\"center\"><b>ACCOUNT</b></td></tr>"));
        if (!Util.isEmpty(cashSdis)) {
            html.append(addSDIRowDetails(cashSdis));
            html.append(separator);
        }
        html.append(addSDIRowDetails(secSdis));
        html.append(tableFooter);

        return html.toString();

    }

    private void setSDIHtmlAgentDetails(SettleDeliveryInstruction sdi, CAMessageFormatter.SdiHtmlRowData row, Trade trade) {
        PartySDI agent = sdi.getAgent();
        if (agent != null) {
            LEContact contact = new LEContact();
            LegalEntity leAgent = BOCache.getLegalEntity(DSConnection.getDefault(), agent.getPartyId());
            if (leAgent != null) {
                row.setAgentName(leAgent.getName());
                try {
                    contact = BOCache.getContact(DSConnection.getDefault(), "Agent", leAgent, "CorporateEvents", "CA", trade.getBook().getProcessingOrgBasedId());
                } catch (Exception ex) {
                    Log.error(this, ex.getMessage());
                    Log.error(this, ex);//Sonar
                }
            }
            row.setAgentAcc(sdi.getAgentAccount());
            // ABA Code
            row.setAgentBic(contact != null ? contact.getSwift() : "");
        }
    }

    private void setSDIHtmlIntermediaryDetails_1(SettleDeliveryInstruction sdi, CAMessageFormatter.SdiHtmlRowData row, Trade trade) {
        PartySDI intermediary = sdi.getIntermediary();
        if (intermediary != null) {
            LegalEntity leIntermediary = BOCache.getLegalEntity(DSConnection.getDefault(), intermediary.getPartyId());
            LEContact contact = new LEContact();
            if (leIntermediary != null) {
                row.setIntermediaryName_1(leIntermediary.getName());
                try {
                    contact = BOCache.getContact(DSConnection.getDefault(), "Agent", leIntermediary, "CorporateEvents", "CA", trade.getBook().getProcessingOrgBasedId());
                } catch (Exception ex) {
                    Log.system(CAMessageFormatter.class.getName(), ex.toString());
                    Log.error(this, ex);//Sonar
                }
            }
            row.setIntermediaryAcc_1(sdi.getAgentAccount());
            // ABA Code
            row.setIntermediaryBic_1(contact != null ? contact.getSwift() : "");
        }
    }

    private void setSDIHtmlIntermediaryDetails_2(SettleDeliveryInstruction sdi, CAMessageFormatter.SdiHtmlRowData row, Trade trade) {
        PartySDI intermediary = sdi.getIntermediary2();
        if (intermediary != null) {
            LegalEntity leIntermediary2 = BOCache.getLegalEntity(DSConnection.getDefault(), intermediary.getPartyId());
            LEContact contact = new LEContact();
            if (leIntermediary2 != null) {
                row.setIntermediaryName_2(leIntermediary2.getName());
                try {
                    contact = BOCache.getContact(DSConnection.getDefault(), "Agent", leIntermediary2, "CorporateEvents", "CA", trade.getBook().getProcessingOrgBasedId());
                } catch (Exception ex) {
                    Log.system(CAMessageFormatter.class.getName(), ex.toString());
                    Log.error(this, ex);//Sonar
                }
            }
            row.setIntermediaryAcc_2(sdi.getAgentAccount());
            // ABA Code
            row.setIntermediaryBic_2(contact != null ? contact.getSwift() : "");
        }
    }

    private String addSDIRowDetails(List<CAMessageFormatter.SdiHtmlRowData> sdis) {
        StringBuffer sdiRowDetails = new StringBuffer();
        for (CAMessageFormatter.SdiHtmlRowData sdi : sdis) {
            if (sdiRowDetails.length() == 0) {
                sdiRowDetails.append("<tr>");
                sdiRowDetails.append("<td rowspan=\"" + sdis.size() + "\">");
                sdiRowDetails.append("<b>" + sdi.getRowType() + "<b></td>");
            } else {
                sdiRowDetails.append("<tr>");
            }
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getSdiCurrency())));

            // sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentName())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentBic())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentAcc())));

            //   sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryName_1())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryBic_1())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryAcc_1())));

            //  sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryName_2())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryBic_2())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getIntermediaryAcc_2())));
            sdiRowDetails.append("</tr>");
        }
        return sdiRowDetails.toString();
    }

    private String addRowValue(String value) {
        return "<td style=\"text-align: left;\"><span class=\"small\">" + value + "</span></td>";
    }

    private String emptyIdNull(String string) {
        return string == null ? "" : string;
    }


    public String parseCA_REFERENCE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        String caReference = "";
        if(trade!=null){
            String attr = trade.getKeywordValue("CARefConci");
            if(!Util.isEmpty(attr)){
                caReference = attr;
            }
        }
        return caReference;
    }


    public String parseSIGN_CHANGED(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        String signChanged = "";
        if(transfer!=null){
            String attr = transfer.getAttribute("SignChanged");
            if(!Util.isEmpty(attr)){
                signChanged = attr;
            }
        }
        return signChanged;
    }


    public String parseSIGNATURES(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        StringBuffer html = new StringBuffer();
        if (transfer!=null){
            String sign = transfer.getAttribute("SignChanged");
            if(!Util.isEmpty(sign) && "true".equalsIgnoreCase(sign)){
                html.append("<img src=\"SIGN_3.png\" WIDTH=272 HEIGHT=150> <span>&nbsp;&nbsp;&nbsp;</span> <img src=\"SIGN_4.png\" WIDTH=272 HEIGHT=150>");
            }
            else{
                html.append("<img src=\"SIGN_1.png\" WIDTH=272 HEIGHT=150> <span>&nbsp;&nbsp;&nbsp;</span> <img src=\"SIGN_2.png\" WIDTH=272 HEIGHT=150>");
            }
        }
        else{
            html.append("<img src=\"SIGN_1.png\" WIDTH=272 HEIGHT=150> <span>&nbsp;&nbsp;&nbsp;</span> <img src=\"SIGN_2.png\" WIDTH=272 HEIGHT=150>");
        }
        return html.toString();
    }


    public String parseREFERENCED_TRADE_ID(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                           BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null) {
                    return Long.toString(referencedTrade.getLongId());
                }
            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }


    public String parseREFERENCED_INCOMING_SWIFT_ID(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                           BOTransfer transfer, DSConnection dsConn) {
        long messageId = 0;
        String messageRef = "";
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null) {
                    String where = "trade_id = " + referencedTrade.getLongId() + " AND message_type='INCOMING'";
                    MessageArray messageList = DSConnection.getDefault().getRemoteBO().getMessages(null, where, null, null);
                    if(messageList!=null && messageList.size()>0){
                        for (int i = 0; i < messageList.size(); i++) {
                            if (messageList.get(i).getLongId()>messageId) {
                                messageRef = !Util.isEmpty(messageList.get(i).getAttribute("AgentRef")) ? messageList.get(i).getAttribute("AgentRef") : messageRef;
                            }
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        if(Util.isEmpty(messageRef)){
            return "";
        }
        else {
            return messageRef;
        }
    }


    public String parseREFERENCED_TRADE_ISIN(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                             BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null){
                    if(referencedTrade.getProduct() instanceof Equity) {
                        Equity equity = (Equity) referencedTrade.getProduct();
                        equity.getSecCode(SecCode.ISIN);
                    }
                    else if(referencedTrade.getProduct() instanceof Bond) {
                        Bond bond = (Bond) referencedTrade.getProduct();
                        bond.getSecCode(SecCode.ISIN);
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }


    public String parseREFERENCED_TRADE_ISIN_DESC(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                           BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null){
                    if(referencedTrade.getProduct() instanceof Equity) {
                        Equity equity = (Equity) referencedTrade.getProduct();
                        return equity.getCorporateName();
                    }
                    else if(referencedTrade.getProduct() instanceof Bond) {
                        Bond bond = (Bond) referencedTrade.getProduct();
                        return bond.getName();
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }


    public String parseREFERENCED_TRADE_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                  BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null) {
                    return parseJDate(message, JDate.valueOf(referencedTrade.getTradeDate(), referencedTrade.getBook().getLocation()));
                }
                } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }


    public String parseREFERENCED_SETTLE_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                             BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if(referencedTrade != null) {
                    return parseJDate(message, referencedTrade.getSettleDate());
                }
            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }

    public String parseREFERENCED_TRADE_QUANTITY(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                              BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue(CA_SOURCE);
        if(!Util.isEmpty(attr)){
            try {
                Trade referencedTrade = dsConn.getRemoteTrade().getTrade(Long.valueOf(attr));
                if (referencedTrade != null){
                    double quantity = Math.abs(referencedTrade.getQuantity());
                    return this.numberToString(quantity);
                }

            } catch (CalypsoServiceException e) {
                Log.info(this.getClass(), "Could not get trade " + attr);
            }
        }
        return "";
    }

	public String parseCA_SDI_INFO_NOTIFICATION(BOMessage message, Trade trade, LEContact po, LEContact cp,
			Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
		String payRec = parseCA_PAY_REC(message, trade, po, cp, paramVector, transfer, dsConn);
		String attrHideSdi = transfer.getAttribute("HideSDI");
		StringBuilder htmlText = new StringBuilder();

		if (Util.isEmpty(payRec)) {
			return "";
		}

		if ("CorporateEventNotice.htm".equalsIgnoreCase(message.getTemplateName())) {
			htmlText.append("<span lang=EN-GB style='font-size:9.0pt;mso-ansi-language:EN-GB'>");
			if ("Receivable".equalsIgnoreCase(payRec) && (Util.isEmpty(attrHideSdi)
					|| (!Util.isEmpty(attrHideSdi) && !"true".equalsIgnoreCase(attrHideSdi)))) {
				htmlText.append("Please review our receiver instructions. ");
				htmlText.append(
						"Due to the EU regulation (FAFT16) Banco Santander now require the beneficiary name and account of the final beneficiary.<br/>");
				htmlText.append(
						"If you have any queries or are unable to agree any claims please contact Coupon claim  Fi.claims@gruposantander.com");
			} else if ("Payment".equalsIgnoreCase(payRec)) {
				htmlText.append("<br>Please review the following SDIs and advise us in case of discrepancy.");
			}
			htmlText.append("</span>");
		} else if ("CorporateEventDeliveryNotice.html".equalsIgnoreCase(message.getTemplateName())) {
			htmlText.append("<span lang=EN-GB style='font-size:9.0pt;mso-ansi-language:EN-GB'><b>");
			if (!Util.isEmpty(payRec) && "Receivable".equalsIgnoreCase(payRec)) {
				htmlText.append(parseSENDER_FULL_NAME(message, trade, po, cp, paramVector, transfer, dsConn)
						+ " Settlement Instructions");
			} else if (!Util.isEmpty(payRec) && "Payment".equalsIgnoreCase(payRec)) {
				htmlText.append(parseRECEIVER_FULL_NAME(message, trade, po, cp, paramVector, transfer, dsConn)
						+ " Settlement Instructions");
			}
			htmlText.append("</b></span>");
		}

		if ("Payment".equalsIgnoreCase(payRec) || ("Receivable".equalsIgnoreCase(payRec) && (Util.isEmpty(attrHideSdi)
				|| (!Util.isEmpty(attrHideSdi) && !"true".equalsIgnoreCase(attrHideSdi))))) {

			String processingOrg = BOCache.getLegalEntityCode(dsConn, po.getLegalEntityId());
			String sdiInfo = LocalCache.getDomainValueComment(dsConn, "CA_NOTIF_" + processingOrg,
					transfer.getSettlementCurrency());

			if (sdiInfo != null) {
				List<String> fields = Arrays.asList(sdiInfo.split("-"));
				Map<String, String> fieldMap = fields.stream().map(s -> s.split(":"))
						.collect(Collectors.toMap(f -> f[0], f -> f.length > 1 ? f[1] : ""));
				String td = "<td style='font-size:6.5pt;font-family: SansSerif;mso-tab-count:1;border: 0.5px solid black;'>";
				htmlText.append("<br>");
				htmlText.append("<br>");
				htmlText.append(
						"<table width='100%' style='font-size:7.0pt;border: 1px solid black;border-collapse: collapse;' CELLSPACING='1'> ");
				htmlText.append("<tr>");
				htmlText.append(
						"<td style='font-family: SansSerif;mso-tab-count:1;text-align:center;border: 1.5px solid black;' colspan='4'><b>BENEFICIARY DETAILS (58)</b></td>");
				htmlText.append(
						"<td style='font-family: SansSerif;mso-tab-count:1;text-align:center;border: 1.5px solid black' colspan='2'><b>PAYING/ INTERMEDIARY BANK (57)</b></td>");
				htmlText.append(
						"<td style='font-family: SansSerif;mso-tab-count:1;text-align:center;border: 1.5px solid black' colspan='2'><b>AGENT BANK (56A)</b></td>");
				htmlText.append("</tr>");
				htmlText.append("<tr>");
				htmlText.append(td + "<b>CURRENCY</b></td>");
				htmlText.append(td + "<b>BENEFICIARY NAME</b></td>");
				htmlText.append(td + "<b>SWIFT CODE</b></td>");
				htmlText.append(td + "<b>BENEFICIARY A/C</b></td>");
				htmlText.append(td + "<b>BANK NAME</b></td>");
				htmlText.append(td + "<b>SWIFT CODE</b></td>");
				htmlText.append(td + "<b>BANK NAME</b></td>");
				htmlText.append(td + "<b>SWIFT CODE</b></td>");
				htmlText.append("</tr>");
				htmlText.append("<tr>");
				htmlText.append(td + transfer.getSettlementCurrency() + "</td>");
				htmlText.append(td + fieldMap.get("BENEFICIARY NAME") + "</td>");
				htmlText.append(td + fieldMap.get("SWIFT CODE 58") + "</td>");
				htmlText.append(td + fieldMap.get("BENEFICIARY A/C") + "</td>");
				htmlText.append(td + fieldMap.get("BANK NAME 57") + "</td>");
				htmlText.append(td + fieldMap.get("SWIFT CODE 57") + "</td>");
				htmlText.append(td + fieldMap.get("BANK NAME 64A") + "</td>");
				htmlText.append(td + fieldMap.get("SWIFT CODE 64A") + "</td>");
				htmlText.append("</tr>");
				htmlText.append("</table>");

			}

		}
		return htmlText.toString();
	}
        


    public String parseCA_SDI_INFO_NOTICE_MOVEMENT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                                 BOTransfer transfer, DSConnection dsConn) {
        String payRec = parseCA_PAY_REC(message, trade, po, cp, paramVector, transfer, dsConn);
        StringBuilder htmlText = new StringBuilder();
        String entity = "";
        if ("Payment".equalsIgnoreCase(payRec)) {
            entity = parseRECEIVER_FULL_NAME(message, trade, po, cp, paramVector, transfer, dsConn);
        } else {
            entity = parseSENDER_FULL_NAME(message, trade, po, cp, paramVector, transfer, dsConn);
        }
        htmlText.append("<span class=\"smaller\"><b>");
        htmlText.append(entity + " Settlement Instructions");
        htmlText.append("</b></span>");
        return htmlText.toString();
    }


    public String parseSDI_BENEFICIARY(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                      BOTransfer transfer, DSConnection dsConn) {
        String payRec = parseCA_PAY_REC(message, trade, po, cp, paramVector, transfer, dsConn);
        int externalSdiId = "Payment".equalsIgnoreCase(payRec) ? transfer.getExternalSettleDeliveryId() : transfer.getInternalSettleDeliveryId();
        SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), externalSdiId);
        return sdi.getBeneficiaryName();
    }


    public String parseSDI_SWIFT_CODE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                       BOTransfer transfer, DSConnection dsConn) {
        String payRec = parseCA_PAY_REC(message, trade, po, cp, paramVector, transfer, dsConn);
        return "Payment".equalsIgnoreCase(payRec) ? cp.getSwift() : po.getSwift();
    }


    public String parseAMOUNT_PAY_REC_TEXT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                      BOTransfer transfer, DSConnection dsConn) {
        String payRec = parseCA_PAY_REC(message, trade, po, cp, paramVector, transfer, dsConn);
        if("Payment".equalsIgnoreCase(payRec)) {
            return "Amount delivered by Santander in " + trade.getTradeCurrency();
        }
        else{
            return "Amount received to Santander in " + trade.getTradeCurrency();
        }
    }


    public String parseUNPAID_CLAIM_LIST(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                           BOTransfer transfer, DSConnection dsConn) {
        String reportType = message.getAttribute("ReportType");
        String reportTemplate = message.getAttribute("ReportTemplate");
        String pricingEnv = message.getAttribute("PricingEnv");
        String cptyId = message.getAttribute("CptyId");
        Vector<String> holidays = new Vector<String>(Arrays.asList(message.getAttribute("Holidays").replace("[","").replace("]","").replace(" ","").split(",")));
        JDatetime datetime = JDatetime.valueOf(message.getAttribute("Datetime"));

        try {
            // Obtiene todas las filas del reporte y plantilla configurados como parametros en la ST
            reportOutput = (DefaultReportOutput) generateReportOutput(dsConn, pricingEnv, holidays, reportType, reportTemplate, datetime, cptyId);
        } catch (RemoteException e) {
            Log.error(LOG_CATEGORY, e);
        }

        if (reportOutput == null) {
            return "";
        }

        ReportRow[] rows = reportOutput.getRows();
        if (rows==null || (rows!=null && rows.length == 0)) {
            return "";
        }

        List<String> ccyList = getCurrencies(rows);
        if(ccyList==null || (ccyList!=null && ccyList.size()==0)){
            return "";
        }

        StringBuilder html = new StringBuilder();

        for (int z=0; z<ccyList.size(); z++) {
            String ccy = ccyList.get(z);

            // Fill the header of the table
            html.append("<table border='1' width=\'100%\' style=\'font-size:6.0pt;font-family:SansSerif;mso-tab-count:1;border-collapse:collapse\'>");
            html.append("<tr style=\'color:white;\'>");
            for(int i = 0; i < reportOutput.getReport().getReportTemplate().getColumns().length; i++){
                html.append("<th bgcolor=\'#FF0000\' align=\'center\'>" + reportOutput.getReport().getReportTemplate().getColumnNames()[i] + "</th>");
            }
            html.append("</tr>");

            // Fill the info of the table
            Double settleAmount = 0.0;
            for (int i=0; i<rows.length; i++) {
                BOTransfer xfer = (BOTransfer) rows[i].getProperty("BOTransfer");
                if(ccy.equalsIgnoreCase(xfer.getSettlementCurrency())) {
                    html.append("<tr>");
                    for (int y = 0; y < reportOutput.getReport().getReportTemplate().getColumns().length; y++) {
                        html.append("<td>");
                        String columnValue = reportOutput.getValueAt(i, y) != null ? reportOutput.getValueAt(i, y).toString() : "";
                        if (columnValue.startsWith("(") && columnValue.endsWith(")")) {
                            columnValue = "-" + columnValue.substring(1, columnValue.length() - 1);
                        }
                        html.append(columnValue);
                        html.append("</td>");
                    }
                    html.append("</tr>");

                    if("RECEIVE".equalsIgnoreCase(xfer.getPayReceive())) {
                        settleAmount = settleAmount + xfer.getSettlementAmount();
                    }
                    else if ("PAY".equalsIgnoreCase(xfer.getPayReceive())) {
                        settleAmount = settleAmount - xfer.getSettlementAmount();
                    }
                }
            }



            // Fill the footer of the table
            html.append("<tr style=\'font-size:7.0pt\'>");
            html.append("<td bgcolor=\'#D6EAF8\' colspan='" + reportOutput.getReport().getReportTemplate().getColumns().length + "'  align='center'><b>");
            // TO_CHANGE
            if(settleAmount > 0.0){
                html.append("TOTAL AMOUNT: Santander receives " + ccy + " " + new Amount(Math.abs(settleAmount),2) +" | Please arrange a bulk payment");
            }
            else if(settleAmount < 0.0){
                html.append("TOTAL AMOUNT: Santander pays " + ccy + " " + new Amount(Math.abs(settleAmount),2) + " | Please confirm SSIs");
            }
            else{
                html.append("TOTAL AMOUNT: 0 " + ccy + " | Nothing to pay/receive");
            }
            html.append("</b></td>");
            html.append("</tr>");
            html.append("</table>");
            if(z != ccyList.size()-1){
                html.append("<br><br>");
            }
        }
        return html.toString();
    }


    protected ReportOutput generateReportOutput(DSConnection dsCon, String pricingEnv, Vector<String> holidays, String reportType, String reportTemplate, JDatetime valDatetime, String cptyId) throws RemoteException {
        PricingEnv env = dsCon.getRemoteMarketData().getPricingEnv(pricingEnv, valDatetime);
        Report reportToFormat = createReport(reportType, reportTemplate, null, env, valDatetime, cptyId);
        if (reportToFormat == null) {
            Log.info(this, "Invalid report type: " + reportType + " or no info to process.");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this, "Invalid report template: " + reportType);
            return null;
        } else {
            reportToFormat.getReportTemplate().setHolidays(holidays);
            if (TimeZone.getDefault() != null) {
                reportToFormat.getReportTemplate().setTimeZone(TimeZone.getDefault());
            }
            Vector<String> errorMsgs = new Vector<String>();
            return reportToFormat.load(errorMsgs);
        }
    }


    protected Report createReport(String type, String templateName, StringBuffer sb, PricingEnv env, JDatetime valDatetime, String cptyId) throws RemoteException {
        Report report;
        try {
            String className = "tk.report." + type + "Report";
            report = (Report) InstantiateUtil.getInstance(className, true);
            report.setPricingEnv(env);
            report.setValuationDatetime(valDatetime);
        } catch (Exception e) {
            Log.error(this, e);
            report = null;
        }
        if ((report != null) && !Util.isEmpty(templateName)) {
            final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                    .getReportTemplate(ReportTemplate.getReportName(type), templateName);
            if (template == null) {
                sb.append("Template " + templateName + " Not Found for " + type + " Report");
                Log.error(this, ("Template " + templateName + " Not Found for " + type + " Report"));
            } else {
                report.setReportTemplate(template);
                template.setValDate(valDatetime.getJDate(TimeZone.getDefault()));
                template.getAttributes().getAttributes().put("CptyName", cptyId);
                template.callBeforeLoad();
            }
        }
        return report;
    }


    public String parseCA_PERCENTAGE_RATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        String attr = trade.getKeywordValue("ContractDivRate");
        if(!Util.isEmpty(attr)){
            Double rate = Double.parseDouble(attr)*100;
            String rateValue = String.valueOf(rate);
            if(rateValue.endsWith(".0")){
                rateValue = rateValue.replace(".0","");
            }
            return rateValue+"%";
        }
        return "100%";
    }


    public String parseCA_GROUP_NOTICE_SDI_INFO(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                            BOTransfer transfer, DSConnection dsConn){
        StringBuilder sdiInfo = new StringBuilder();
        sdiInfo.append("<span lang=EN-GB style='font-size:9.0pt;mso-ansi-language:EN-GB'><b>");
        sdiInfo.append("Settlement Delivery Instructions (Cash)");
        sdiInfo.append("</b></span>");

        if (reportOutput == null) {
            sdiInfo.append("<br><br>");
            sdiInfo.append("There is not SDIs.");
            sdiInfo.append("<br><br>");
        }
        else{
            ReportRow[] rows = reportOutput.getRows();
            if (rows == null && rows.length == 0){
                sdiInfo.append("<br><br>");
                sdiInfo.append("There is not SDIs.");
                sdiInfo.append("<br><br>");
            }
            else{
                List<String> sdiList = new ArrayList<>();
                for (int i=0; i<rows.length; i++) {
                    BOTransfer xfer = (BOTransfer) rows[i].getProperty("BOTransfer");
                    int sdiId = 0;
                    if("RECEIVE".equalsIgnoreCase(xfer.getPayReceive())) {
                        sdiId = xfer.getInternalSettleDeliveryId();
                    }
                    String sdiLine = xfer.getSettlementCurrency() + "-" + sdiId;
                    if(!sdiList.contains(sdiLine) && sdiId!=0){
                        sdiList.add(sdiLine);
                        System.out.println(sdiLine);
                    }
                }

                if(sdiList!=null && sdiList.size()>0){

                    sdiInfo.append("<br>");
                    sdiInfo.append("<table border='1'  style='font-size:6.0pt;font-family:SansSerif;mso-tab-count:1;border-collapse:collapse'>");
                    sdiInfo.append("</tr>");
                    sdiInfo.append("<tr style='color:white;'>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'></th>");
                    sdiInfo.append("<th bgcolor='#FF0000' colspan='3' align='center'>BENEFICIARY DETAILS (58)</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' colspan='2' align='center'>PAYING/ INTERMEDIARY BANK (57)</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' colspan='2' align='center'>AGENT BANK (56A)</th>");
                    sdiInfo.append("</tr>");
                    sdiInfo.append("<tr style='color:white;'>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>CURRENCY</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>BENEFICIARY NAME</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>SWIFT CODE</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>BENEFICIARY A/C</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>BANK NAME</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>SWIFT CODE</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>BANK NAME</th>");
                    sdiInfo.append("<th bgcolor='#FF0000' align='center'>SWIFT CODE</th>");

                    for(int y=0; y<sdiList.size(); y++){
                        String[] sdiInfoLine = sdiList.get(y).split("-");
                        String currency = sdiInfoLine[0];
                        int sdiId = Integer.parseInt(sdiInfoLine[1]);
                        SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
                        String beneficiaryName = sdi.getBeneficiaryName();
                        String beneficiarySwiftCode = po.getSwift();
                        String beneficiaryAccount = sdi.getAgentAccount();
                        String agentName = "";
                        String agentSwiftCode = "";
                        String intermediaryName = "";
                        String intermediarySwiftCode = "";

                        LegalEntity agent = BOCache.getLegalEntity(DSConnection.getDefault(), sdi.getAgentId());
                        if (agent!=null){
                            agentName = agent.getName();
                            LEContact agentContact = BOCache.getContact(dsConn, sdi.getAgent().getPartyRole(), agent, sdi.getAgentContactType(),"CA", po.getLegalEntityId());
                            if(agentContact!=null){
                                agentSwiftCode = agentContact.getSwift();
                            }
                        }

                        LegalEntity intermediary = BOCache.getLegalEntity(DSConnection.getDefault(), sdi.getIntermediaryId());
                        if(intermediary!=null){
                            intermediaryName =  intermediary.getName();
                            LEContact intermediaryContact = BOCache.getContact(dsConn, sdi.getIntermediary().getPartyRole(), intermediary, sdi.getIntermediaryContactType(),"CA", po.getLegalEntityId());
                            if(intermediaryContact!=null){
                                intermediarySwiftCode = intermediaryContact.getSwift();
                            }
                        }

                        sdiInfo.append("<tr>");
                        sdiInfo.append("<td>" + currency + "</td>");
                        sdiInfo.append("<td>" + beneficiaryName + "</td>");
                        sdiInfo.append("<td>" + beneficiarySwiftCode + "</td>");
                        sdiInfo.append("<td>" + beneficiaryAccount + "</td>");
                        sdiInfo.append("<td>" + agentName + "</td>");
                        sdiInfo.append("<td>" + agentSwiftCode + "</td>");
                        sdiInfo.append("<td>" + intermediaryName + "</td>");
                        sdiInfo.append("<td>" + intermediarySwiftCode + "</td>");
                        sdiInfo.append("</tr>");
                    }
                    sdiInfo.append("</table>");
                }
            }
        }
        return sdiInfo.toString();
    }


    public String parseCA_SWIFT_MESAGE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer, DSConnection dsConn) throws CalypsoServiceException {
        CA ca = (CA) trade.getProduct();
        Product underlying = ca.getUnderlyingProduct();
        if(underlying != null && underlying instanceof Bond){
            String swiftInfo = "";
            StringBuilder where = new StringBuilder();
            BOMessage paymentMessage = new BOMessage();
            where.append("bo_message.transfer_id = " + message.getTransferLongId() + " AND ");
            where.append("bo_message.message_type IN ('PAYMENTHUB_PAYMENTMSG') AND ");
            where.append("bo_message.template_name IN ('PH-FICT', 'PH-FICCT') AND ");
            where.append("bo_message.message_status NOT IN ('CANCELED')");
            try {
                MessageArray messages = DSConnection.getDefault().getRemoteBackOffice().getMessages(null, where.toString(), null, null);
                if (null != messages && !Util.isEmpty(messages.getMessages())) {
                    paymentMessage = messages.get(0);
                }
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error loading paymments messages for transfer with id: " + message.getTransferLongId());
            }
            if (paymentMessage != null) {
                AdviceDocument adviceDoc = null;
                try {
                    adviceDoc = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(paymentMessage.getLongId(), new JDatetime());
                } catch (CalypsoServiceException exc) {
                    Log.error(this, exc.getCause());
                }
                if (adviceDoc != null) {
                    swiftInfo = adviceDoc.getDocument() != null ? adviceDoc.getDocument().toString() : "";
                }
            }

            if (swiftInfo != null) {
                ObjectMapper mapper = new ObjectMapper();
                String swift = "";
                try {
                    swiftInfo = swiftInfo.replace("\n", "");
                    swift = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(swiftInfo);
                    String aaa = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(swiftInfo);
                } catch (JsonProcessingException e) {
                    Log.error(this, e);
                }
                return swift;
            } else {
                return "";
            }
        }
        else {
            return null;
        }
    }


    public String parseTRANSFER_SETTLE_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        return formatJDate(transfer.getSettleDate(), "dd/MM/yyyy", null);
    }

	public String parseSHOW_TEXT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
			BOTransfer transfer, DSConnection dsConn) {
		String textShown = "";
		boolean showText = false;
		if ("CA_NOTIF".equals(message.getMessageType())) {
			showText = countMessagesByType(dsConn, trade.getLongId(), "CA_NOTIF") > 1;
		} else if ("CA_CLAIM".equals(message.getMessageType())) {
			showText = countMessagesByType(dsConn, trade.getLongId(), "CA_CLAIM") > 1;
		}
		if (showText) {
			StringBuilder text = new StringBuilder();
			text.append("<br>");
			text.append("<p class=MsoNormal><span lang=EN-GB style='font-size:9.0pt;mso-ansi-language:EN-GB'><b>");
			text.append("Please note this notification related to this ID claim superseeds any previous communication");
			text.append("</b></span></p>");
			textShown = text.toString();
		}
		return textShown;
	}
	
	/**
	 * 
	 * @param dsConn
	 * @param tradeId
	 * @param type
	 * @return
	 */
	private long countMessagesByType(DSConnection dsConn, long tradeId, String type) {
		long nMessages = 0;
		try {
			StringBuilder where = new StringBuilder();
			where.append("bo_message.trade_id = " + tradeId);
			where.append(" AND bo_message.message_type ='" + type + "'");
			nMessages = dsConn.getRemoteBO().countMessagesFromWhere(null, where.toString(), false, null);
		} catch (CalypsoServiceException e) {
			Log.error(this, e);
		}
		return nMessages;
	}

    private List<String> getCurrencies(ReportRow[] rows) {
        List<String> ccyList = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            BOTransfer xfer = (BOTransfer) row.getProperty("BOTransfer");
            String ccy = xfer.getSettlementCurrency();
            if (!ccyList.contains(ccy)){
                ccyList.add(ccy);
            }
        }
        return ccyList;
    }


    /**
     * Internal bean to export sdi information in html format
     *
     * @author
     */
    class SdiHtmlRowData {

        private String rowType;
        private String currency;

        private String agentName;
        private String agentBic;
        private String agentAcc;

        private String intermediaryName_1;
        private String intermediaryBic_1;
        private String intermediaryAcc_1;

        private String intermediaryName_2;
        private String intermediaryBic_2;
        private String intermediaryAcc_2;

        public String getRowType() {
            return this.rowType;
        }

        public void setRowType(String rowType) {
            this.rowType = rowType;
        }

        public String getSdiCurrency() {
            return this.currency;
        }

        public void setSdiCurrency(String currency) {
            this.currency = currency;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public void setAgentAcc(String agentAcc) {
            this.agentAcc = agentAcc;
        }

        public String getAgentBic() {
            return this.agentBic;
        }

        public void setAgentBic(String agentBic) {
            this.agentBic = agentBic;
        }

        public void setIntermediaryName_1(String intermediaryName_1) {
            this.intermediaryName_1 = intermediaryName_1;
        }

        public String getIntermediaryBic_1() {
            return intermediaryBic_1;
        }

        public void setIntermediaryBic_1(String intermediaryBic_1) {
            this.intermediaryBic_1 = intermediaryBic_1;
        }

        public void setIntermediaryAcc_1(String intermediaryAcc_1) {
            this.intermediaryAcc_1 = intermediaryAcc_1;
        }

        public void setIntermediaryName_2(String intermediaryName_2) {
            this.intermediaryName_2 = intermediaryName_2;
        }

        public String getIntermediaryBic_2() {
            return intermediaryBic_2;
        }

        public void setIntermediaryBic_2(String intermediaryBic_2) {
            this.intermediaryBic_2 = intermediaryBic_2;
        }

        public void setIntermediaryAcc_2(String intermediaryAcc_2) {
            this.intermediaryAcc_2 = intermediaryAcc_2;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getAgentName() {
            return agentName;
        }

        public String getAgentAcc() {
            return agentAcc;
        }

        public String getIntermediaryName_1() {
            return intermediaryName_1;
        }

        public String getIntermediaryAcc_1() {
            return intermediaryAcc_1;
        }

        public String getIntermediaryName_2() {
            return intermediaryName_2;
        }

        public String getIntermediaryAcc_2() {
            return intermediaryAcc_2;
        }
    }

}
