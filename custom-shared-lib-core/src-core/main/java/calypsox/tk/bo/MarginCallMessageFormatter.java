package calypsox.tk.bo;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.tk.report.*;
import calypsox.util.MarginCallConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.CashAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.factory.LegalEntityRoleEnum;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportViewer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.jidesoft.margin.Margin;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Formatter class for margin call notification template
 *
 * @author VARIOUS
 * @version 2.0
 * @see com.calypso.tk.bo.MC_NOTIFICATIONMarginCallMessageFormatter
 */
@SuppressWarnings("rawtypes")
public class MarginCallMessageFormatter extends com.calypso.tk.bo.MC_NOTIFICATIONMarginCallMessageFormatter {
    private static final String IM_TERMINATION_CCY = "IM_TERMINATION_CCY";
    protected MarginCallEntryDTO entryDTO = null;//Sonar
    protected MarginCallEntry entry = null;//Sonar
    protected CollateralConfig marginConfig = null;//Sonar
    private static final int NUMBER_OF_DEC = 2;
    private static Map<String, String> templateNameTitle = new HashMap<>();
    private static final String DEFAULT_LOGO_NAME = "BSTE";
    private static final String LOGO_FILE_EXT = ".png";
    public static final String EMAIL_SEPARATOR = ";";
    public static final String NOTIF_EMAIL_TEMPLATE = "notifEmailCore.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_EMIR_BILATERAL = "PortfolioReconciliationBilateral.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_EMIR_UNILATERAL = "PortfolioReconciliationUnilateral.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_BALANCE = "MarginCallBalance.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_NOTICE = "MarginCallNotice.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_PORTFOLIO = "MarginCallPortfolioReconciliation.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_PORTFOLIOMSFA = "MarginCallPortfolioReconciliationMSFTA.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_COLLATERALDEAL = "BodyCollateralDeal.htm";
    private static final String GROUP_IM = "IM";

    public static final String NOTIF_EMAIL_TEMPLATE_DELIVERY_NOTICE = "MarginCallDeliveryNotice.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_PARTIAL_DELIVERY_NOTICE = "MarginCallPartialDeliveryNotice.htm";
    public static final String NOTIF_EMAIL_TEMPLATE_INTEREST_STATEMENT_NOTICE = "MarginCallInterestStatement.htm";

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    protected final DateFormat spanishDateFormat = new SimpleDateFormat("d 'de' MMMM 'de' yyyy",
            new Locale("es", "ES"));
    public static final String DEFAULT_FROM_EMAIL = "collateral-backoffice@santander.com";

    protected MC_NOTIFICATIONMarginCallMessageFormatter messageFormatterDelegate;

    private static final String PO_SOVEREIGN = "SBWO";

    protected static final String CONTRACT_TYPE_CSD = "CSD";
    private static final String CONTRACT_TYPE_CSA = "CSA";
    public static final String CONTRACT_TYPE_CGAR = "CGAR";
    private static final String CSD_TITLE = "Initial Margin Requirement Notice";
    private static final String CSA_TITLE = "Variation Margin Notice";
    private static final String CGAR_TITLE = "NOTIFICACION DE MONTO DE GARANTIA Y/O CANTIDAD DE DEVOLUCION";
    private static final String PO_ATTR_LOGO = "LOGO";
    private static final String CGAR_DELIVERY_TITLE = "NOTIFICACION DE ENTREGA";

    private static final String PO_MTM = "PO MtM";
    private static final String ADDITIONAL_EMAIL2 = "ADDITIONAL_EMAIL2";
    private static final String ADDITIONAL_EMAIL3 = "ADDITIONAL_EMAIL3";

    public static final String NOTIFICATION_TYPE = "NotificationType";
    public static final String IM = "IM";
    public static final String VM = "VM";

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |TITLE|
     */
    public String parseTITLE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                             BOTransfer transfer, DSConnection dsConn) {
        this.entryDTO = getMarginCallEntryDTO(message, dsConn);

        prepareMarginCallEntryDTO(message, dsConn);

        String contractType = parseCONTRACT_TYPE(message, trade, po, cp, paramVector, transfer, dsConn);
        String templateTitle;
        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CSD)) {
            templateTitle = CSD_TITLE;
        } else if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CGAR)) {
            templateTitle = getCGARTemplateTitle(message.getTemplateName());
        } else {
            templateTitle = getTitleFromTemplateName(message, dsConn);
            message.setAttribute(MarginCallConstants.TRANS_MESSAGE_ATTR_TEMP_TITLE, templateTitle);
        }
        return templateTitle;
    }

    /**
     * @param templateName template name
     * @return value for title
     */
    private String getCGARTemplateTitle(String templateName) {
        if (NOTIF_EMAIL_TEMPLATE_DELIVERY_NOTICE.equals(templateName)
                || NOTIF_EMAIL_TEMPLATE_PARTIAL_DELIVERY_NOTICE.equals(templateName)) {
            return CGAR_DELIVERY_TITLE;
        }
        return CGAR_TITLE;
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |NET_BALANCE|
     */
    public String parseNET_BALANCE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                   BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        Double poNetBalance = null;
        try {
            if (this.entryDTO.getAttribute(PO_MTM) != null) {
                poNetBalance = (Double) this.entryDTO.getAttribute(PO_MTM);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        double netBalance = poNetBalance == null ? this.entryDTO.getNetBalance() : poNetBalance;

        return numberToString(netBalance, NUMBER_OF_DEC);

    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |NET_EXPOSURE|
     */
    public String parseNET_EXPOSURE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        // net exposure should be all the time the PO MtM, so in case of a
        // dispute, return the PO MtM instead of the
        // netExposure
        Double poMtM = null;
        try {
            if (this.entryDTO.getAttribute(PO_MTM) != null) {
                poMtM = (Double) this.entryDTO.getAttribute(PO_MTM);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        double netExposure = (poMtM == null ? this.entryDTO.getNetExposure() : poMtM);
        return numberToString(netExposure, NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATION_DELIVERER_NAME|
     */
    public String parseALLOCATION_DELIVERER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                 Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntryDTO(message, dsCon);
        if (SantMarginCallUtil.isPayMarginCall(this.entryDTO)) {
            return parseSENDER_FULL_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
        } else {
            return parseRECEIVER_FULL_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATION_RECEIVER_NAME|
     */
    public String parseALLOCATION_RECEIVER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntryDTO(message, dsCon);
        if (SantMarginCallUtil.isPayMarginCall(this.entryDTO)) {
            return parseRECEIVER_FULL_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
        } else {
            return parseSENDER_FULL_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
        }
    }

    @Override
    public String parseRECEIVER_EMAIL(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntryDTO(message, dsCon);
        String receiverEmailIds = super.parseRECEIVER_EMAIL(message, trade, sender, rec, transferRules, transfer,
                dsCon);

        String additionalEmails = "";
        if (rec != null) {
            additionalEmails = rec.getAddressCode("ADDITIONAL_EMAIL");

            if (!Util.isEmpty(rec.getAddressCode(ADDITIONAL_EMAIL2))) {
                if (!Util.isEmpty(additionalEmails)) {
                    additionalEmails = additionalEmails + ";" + rec.getAddressCode(ADDITIONAL_EMAIL2);
                } else {
                    additionalEmails = rec.getAddressCode(ADDITIONAL_EMAIL2);
                }
            }
            if (!Util.isEmpty(rec.getAddressCode(ADDITIONAL_EMAIL3))) {
                if (!Util.isEmpty(additionalEmails)) {
                    additionalEmails = additionalEmails + ";" + rec.getAddressCode(ADDITIONAL_EMAIL3);
                } else {
                    additionalEmails = rec.getAddressCode(ADDITIONAL_EMAIL3);
                }
            }
        }

        receiverEmailIds = buildAdditionEmails(receiverEmailIds, additionalEmails);

        return receiverEmailIds;
    }

    private String buildAdditionEmails(String finalList, String additionalEmails) {
        if (!Util.isEmpty(additionalEmails)) {
            if (!Util.isEmpty(finalList)) {
                finalList += " " + additionalEmails;
            } else {
                finalList = additionalEmails;
            }

            if (!Util.isEmpty(finalList) && (finalList.indexOf(EMAIL_SEPARATOR) > 0)) {
                List<String> emails = Arrays.asList(finalList.split(EMAIL_SEPARATOR));
                StringBuffer formattedEmailList = new StringBuffer("");
                for (String email : emails) {
                    if ((formattedEmailList.length() == 0) && !Util.isEmpty(email)) {
                        formattedEmailList.append(email);
                    } else {
                        formattedEmailList.append(EMAIL_SEPARATOR);
                        formattedEmailList.append(" ");
                        formattedEmailList.append(email);
                    }
                }
                finalList = formattedEmailList.toString();
            }
        }
        return finalList;
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |GLOBAL_CALC_REQUIRED_MARGIN|
     */
    public String parseGLOBAL_CALC_REQUIRED_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                   Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getGlobalRequiredMarginCalc(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |INDP_AMOUNT|
     */
    public String parseINDP_AMOUNT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                   BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getIndependentAmount(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |MC_DATE|
     */
    public String parseMC_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                               BOTransfer transfer, DSConnection dsConn) {

        prepareMarginCallEntryDTO(message, dsConn);

        // fix GSM 28/06/2016 - spanish date
        final JDate mcJdate = this.entryDTO.getValueDatetime().getJDate(TimeZone.getDefault());

        String contractType = parseCONTRACT_TYPE(message, trade, po, cp, paramVector, transfer, dsConn);

        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CGAR)) {

            Date date = mcJdate.getDate(TimeZone.getDefault());
            return spanishDateFormat.format(date);
        }

        return parseJDate(message, this.entryDTO.getValueDatetime().getJDate(TimeZone.getDefault()));
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |CPT_MTM|
     */
    public String parseCPT_MTM(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                               BOTransfer transfer, DSConnection dsConn) {
        String emptyCptyAmount = "-";
        prepareMarginCallEntryDTO(message, dsConn);
        Double cptyDoubleAmount = null;
        try {
            cptyDoubleAmount = (Double) this.entryDTO.getAttribute("Cpty MtM");
        } catch (Exception e) {
            cptyDoubleAmount = null;
            Log.error(this, e);//Sonar
        }
        if (cptyDoubleAmount != null) {
            return numberToString(cptyDoubleAmount, NUMBER_OF_DEC);
        }

        return emptyCptyAmount;
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |CONSTITUTED_MARGIN|
     */
    public String parseCONSTITUTED_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                          Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getConstitutedMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |MARGIN_REQUIRED|
     */
    public String parseMARGIN_REQUIRED(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                       BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getMarginRequired(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |PREVIOUS_CASH_MARGIN|
     */
    public String parsePREVIOUS_CASH_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                            Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getPreviousActualCashMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |PREVIOUS_SECURITY_MARGIN|
     */
    public String parsePREVIOUS_SECURITY_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getPreviousActualSecurityMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |PREVIOUS_PEND_CASH_MARGIN|
     */
    public String parsePREVIOUS_PEND_CASH_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                 Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntry(message, dsConn);
        return numberToString(this.entry.getPreviousNotSettledCashMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |PREVIOUS_PEND_SECURITY_MARGIN|
     */
    public String parsePREVIOUS_PEND_SECURITY_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                     Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntry(message, dsConn);
        return numberToString(this.entry.getPreviousNotSettledSecurityMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |GLOBAL_REQUIRED_MARGIN|
     */
    public String parseGLOBAL_REQUIRED_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                              Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getGlobalRequiredMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |CONSTITUED_MARGIN|
     */
    public String parseCONSTITUED_MARGIN(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                         BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        return numberToString(this.entryDTO.getConstitutedMargin(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ACCRUED_INTEREST|
     */
    public String parseACCRUED_INTEREST(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                        BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        double sumAccruedInterest = 0;
        // the the additional field MC_INCLUDE_ACCRUED_INTERESTis set to true on
        // the contract, so return the sum of the accrued interests

        // Defaulted to 0 at this stage

        // if (Boolean.valueOf(get(message.getStatementId(), dsConn)
        // .getAdditionalField(
        // MarginCallConstants.MC_INCLUDE_ACCRUED_INTEREST))) {
        // sumAccruedInterest = entry.getInterest().getAmount();
        // }
        return numberToString(sumAccruedInterest, NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |OUTSTANDING_SUBSTIT_TRANSACT|
     */
    public String parseOUTSTANDING_SUBSTIT_TRANSACT(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                    Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntry(message, dsConn);
        // return numberToString(entry.getPreviousNotSettledTotalMargin(),
        // NUMBER_OF_DEC);
        return String.valueOf("-");
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |COLLATERAL_VALUE|
     */
    public String parseCOLLATERAL_VALUE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                        BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        List<CashAllocationDTO> cashAllocs = this.entryDTO.getCashAllocations();
        List<SecurityAllocationDTO> secAllocs = this.entryDTO.getSecurityAllocations();

        double sumCashAllocation = 0;
        double sumSecurityAllocation = 0;
        // sum cash allocations
        for (CashAllocationDTO cash : cashAllocs) {
            sumCashAllocation += cash.getContractValue();
        }
        // sum cash allocations
        for (SecurityAllocationDTO sec : secAllocs) {
            sumSecurityAllocation += sec.getContractValue();

        }
        return numberToString(sumCashAllocation + sumSecurityAllocation, NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |DAY_BEFORE_DATE|
     */
    public String parseDAY_BEFORE_DATE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                       BOTransfer transfer, DSConnection dsConn) {
        prepareMarginCallEntryDTO(message, dsConn);
        JDate yesterday = JDate.getNow().addBusinessDays(-1, this.marginConfig.getBook().getHolidays());
        return parseJDate(message, yesterday);
    }

    /**
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return
     */
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
        return "<img src=\"" + poShortName + LOGO_FILE_EXT + "\" \\>";
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |DAY_BEFORE_DATE|
     */
    @Override
    public String parseCONTRACT_OPENDATE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        prepareMarginCallEntryDTO(message, dsCon);

        if (this.marginConfig == null) {
            return "";
        }

        // fix GSM 28/06/2016
        final String contractType = parseCONTRACT_TYPE(message, trade, sender, rec, transferRules, transfer, dsCon);

        // get the information from the mccAdditionalField MA_SIGN_DATE
        String signDate = this.marginConfig.getAdditionalField("MA_SIGN_DATE");
        if (!Util.isEmpty(signDate)) {

            try {
                JDate mcDate = JDate.valueOf(this.dateFormat.parse(signDate));

                if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CGAR)) {

                    Date date = mcDate.getDate(TimeZone.getDefault());
                    return spanishDateFormat.format(date);
                }

                return parseJDate(message, mcDate);

            } catch (ParseException e) {
                // do nothing
                Log.error(this, e);
            }

        }

        if (this.marginConfig.getStartingDate() == null) {
            return "";
        }
        // fix GSM 28/06/2016

        final JDate mcOpenJdate = this.entryDTO.getValueDatetime().getJDate(TimeZone.getDefault());

        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CGAR)) {

            Date date = mcOpenJdate.getDate(TimeZone.getDefault());
            return spanishDateFormat.format(date);
        }

        return parseJDate(message, this.marginConfig.getStartingDate().getJDate(TimeZone.getDefault()));
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |NOTIF_FROM_EMAIL|
     */
    public String parseNOTIF_FROM_EMAIL(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntryDTO(message, dsCon);
        if (this.marginConfig == null) {
            return "";
        }
        return getFromAddress(message, this.marginConfig, dsCon);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |THRESHOLD_AMOUNT|
     */
    public String parseTHRESHOLD_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        prepareMarginCallEntryDTO(message, dsCon);
        return numberToString(this.entryDTO.getThresholdAmount(), NUMBER_OF_DEC);
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |MTA_AMOUNT|
     */
    public String parseMTA_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                                  BOTransfer transfer, DSConnection dsCon) {

        prepareMarginCallEntryDTO(message, dsCon);
        return numberToString(this.entryDTO.getMTAAmount(), NUMBER_OF_DEC);

    }

    /**
     * extract the margin call entry dto for this message
     *
     * @param message
     * @param dsConn
     */
    private void prepareMarginCallEntryDTO(BOMessage message, DSConnection dsConn) {
        if (this.entryDTO == null) {
            this.entryDTO = getMarginCallEntryDTO(message, dsConn);
        }

        if (this.marginConfig == null) {
            this.marginConfig = getMcc(message.getStatementId(), dsConn);

        }
    }

    /**
     * extract the margin call entry for this message
     *
     * @param message
     * @param dsConn
     */
    private void prepareMarginCallEntry(BOMessage message, DSConnection dsConn) {
        if (this.entry == null) {
            prepareMarginCallEntryDTO(message, dsConn);
            this.entry = SantMarginCallUtil.getMarginCallEntry(this.entryDTO, this.marginConfig, false);
        }
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATION|
     */
    @Override
    public String parseALLOCATIONS(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                   Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String result = null;

        MarginCallEntryDTO entry = getMarginCallEntryDTO(message, dsCon);

        if (entry != null) {
            SantMCAllocationReportTemplate template = (SantMCAllocationReportTemplate) ReportTemplate
                    .getReportTemplate("SantMCAllocation");
            template.setTemplateName("SantMCAllocation");

            CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                    entry.getCollateralConfigId());
            if ((mcc != null) && (mcc.getProcessingOrg() != null)) {
                String contractType = parseCONTRACT_TYPE(message, trade, sender, rec, transferRules, transfer, dsCon);
                List<String> columns;
                if (contractType.equals(CONTRACT_TYPE_CGAR)) {
                    columns = Arrays.asList(SantMCAllocationReportStyle.POSITION_ACTION_MEX,
                            SantMCAllocationReportStyle.VALUE_DATE_MEX, SantMCAllocationReportStyle.ASSET_MEX,
                            SantMCAllocationReportStyle.CURRENCY_MEX, SantMCAllocationReportStyle.NOMINAL_MEX,
                            SantMCAllocationReportStyle.UNIT_PRICE_MEX, SantMCAllocationReportStyle.MARKET_VALUE_MEX,
                            SantMCAllocationReportStyle.VALUATION_PERCENTAGE_MEX,
                            SantMCAllocationReportStyle.ADJUSTED_VALUE_MEX);
                } else {
                    columns = Arrays.asList(SantMCAllocationReportStyle.POSITION_ACTION,
                            SantMCAllocationReportStyle.VALUE_DATE, SantMCAllocationReportStyle.ASSET,
                            SantMCAllocationReportStyle.CURRENCY, SantMCAllocationReportStyle.NOMINAL,
                            SantMCAllocationReportStyle.UNIT_PRICE, SantMCAllocationReportStyle.MARKET_VALUE,
                            SantMCAllocationReportStyle.VALUATION_PERCENTAGE,
                            SantMCAllocationReportStyle.ADJUSTED_VALUE);
                }

                template.setColumns(Util.collection2StringArray(columns));
            }

            // list of all entry allocations (Securities and Cash)
            List<MarginCallAllocationDTO> allocations = new ArrayList<>();
            // get Cash allocations
            List<CashAllocationDTO> cashAllocations = entry.getCashAllocations();
            // get Securities allocations
            List<SecurityAllocationDTO> securityAllocations = entry.getSecurityAllocations();

            // merge the cash and securities allocations
            allocations.addAll(cashAllocations);
            allocations.addAll(securityAllocations);

            if (!Util.isEmpty(allocations)) {
                // exclude canceled allocations
                List<MarginCallAllocationDTO> nonCanceledAllocs = new ArrayList<>();
                for (MarginCallAllocationDTO alloc : allocations) {
                    if ((alloc != null) && !MarginCallAllocation.STATUS_CANCELLED.equals(alloc.getStatus())) {

                        // Check to see if the Trade is cancelled.
                        if (alloc.getTradeId() != 0) {
                            try {
                                Trade allocTrade = dsCon.getRemoteTrade().getTrade(alloc.getTradeId());
                                if ((allocTrade != null)
                                        && allocTrade.getStatus().getStatus().equals(Status.CANCELED)) {
                                    continue;
                                }
                            } catch (RemoteException e) {
                                Log.error(this, e);
                            }
                        }

                        nonCanceledAllocs.add(alloc);
                    }
                }
                template.setAllocations(nonCanceledAllocs);

                Report report = Report.getReport(SantMCAllocationReport.TYPE);
                report.setReportTemplate(template);

                ReportOutput output = report.load(new Vector());

                ReportViewer viewer = new SantHTMLCollateralReportViewer();
                output.format(viewer);

                result = viewer.toString();
            }
        }

        return result;

    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATION|
     */
    public String parseSUBSTITUTION_ALLOCATIONS(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String result = null;

        MarginCallEntryDTO entry = getMarginCallEntryDTO(message, dsCon);

        if (entry != null) {
            SantMCSubstitAllocationReportTemplate template = (SantMCSubstitAllocationReportTemplate) ReportTemplate
                    .getReportTemplate("SantMCSubstitAllocation");
            template.setTemplateName("SantMCSubstitAllocation");
            // list of all entry allocations (Securities and Cash)
            List<MarginCallAllocationDTO> allocations = new ArrayList<>();
            // get Cash allocations
            List<CashAllocationDTO> cashAllocations = entry.getCashAllocations();
            // get Securities allocations
            List<SecurityAllocationDTO> securityAllocations = entry.getSecurityAllocations();

            // merge the cash and securities allocations
            if (cashAllocations != null) {
                for (CashAllocationDTO cashAlloc : cashAllocations) {
                    allocations.add(cashAlloc);
                }
            }
            if (securityAllocations != null) {
                for (SecurityAllocationDTO secAlloc : securityAllocations) {
                    allocations.add(secAlloc);
                }
            }

            if (!Util.isEmpty(allocations)) {
                // exclude canceled allocations
                List<MarginCallAllocationDTO> nonCanceledAllocs = new ArrayList<>();
                for (MarginCallAllocationDTO alloc : allocations) {
                    if ((alloc != null) && !MarginCallAllocation.STATUS_CANCELLED.equals(alloc.getStatus())) {
                        nonCanceledAllocs.add(alloc);
                    }
                }

                template.setAllocations(nonCanceledAllocs);

                Report report = Report.getReport(SantMCSubstitAllocationReport.TYPE);
                report.setReportTemplate(template);

                ReportOutput output = report.load(new Vector());

                ReportViewer viewer = new SantHTMLCollateralReportViewer();
                output.format(viewer);

                result = viewer.toString();
            }
        }

        return result;

    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATION|
     */
    public String parseREQUEST_SUBSTITUTION_ALLOCATIONS(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String result = null;

        MarginCallEntryDTO entry = getMarginCallEntryDTO(message, dsCon);

        if (entry != null) {
            SantMCRequestSubstitAllocationReportTemplate template = (SantMCRequestSubstitAllocationReportTemplate) ReportTemplate
                    .getReportTemplate("SantMCRequestSubstitAllocation");
            template.setTemplateName("SantMCRequestSubstitAllocation");
            // list of all entry allocations (Securities and Cash)
            List<MarginCallAllocationDTO> allocations = new ArrayList<>();
            // get Cash allocations
            List<CashAllocationDTO> cashAllocations = entry.getCashAllocations();
            // get Securities allocations
            List<SecurityAllocationDTO> securityAllocations = entry.getSecurityAllocations();

            // add one fake allocation
            CashAllocationDTO adviseAllocation = new CashAllocationDTO();
            adviseAllocation.setType(MarginCallConstants.ALLOCATION_TYPE_ADVISE_SUBSTITUTION);
            adviseAllocation.addAttribute("AllocationMode", "Substitution");

            if (cashAllocations != null) {
                for (CashAllocationDTO cashAlloc : cashAllocations) {
                    // if
                    // (MarginCallAllocation.ALLOCATION_SUBSTITUTION.equals(cashAlloc.getType()))
                    // {
                    adviseAllocation.setCollateralConfigId(cashAlloc.getCollateralConfigId());
                    allocations.add(cashAlloc);
                    adviseAllocation.setContractValue(cashAlloc.getContractValue());
                    // }
                }
            }
            if (securityAllocations != null) {
                for (SecurityAllocationDTO secAlloc : securityAllocations) {
                    // if
                    // (MarginCallAllocation.ALLOCATION_SUBSTITUTION.equals(secAlloc.getType()))
                    // {
                    adviseAllocation.setCollateralConfigId(secAlloc.getCollateralConfigId());
                    allocations.add(secAlloc);
                    adviseAllocation.setContractValue(secAlloc.getContractValue());
                    // }
                }
            }
            if (allocations.size() == 1) {
                allocations.add(adviseAllocation);
            }

            if (!Util.isEmpty(allocations)) {
                // exclude canceled allocations
                List<MarginCallAllocationDTO> nonCanceledAllocs = new ArrayList<>();
                for (MarginCallAllocationDTO alloc : allocations) {
                    if ((alloc != null) && !MarginCallAllocation.STATUS_CANCELLED.equals(alloc.getStatus())) {
                        nonCanceledAllocs.add(alloc);
                    }
                }

                template.setAllocations(nonCanceledAllocs);

                Report report = Report.getReport(SantMCRequestSubstitAllocationReport.TYPE);
                report.setReportTemplate(template);

                ReportOutput output = report.load(new Vector());

                ReportViewer viewer = new SantHTMLCollateralReportViewer();
                output.format(viewer);

                result = viewer.toString();
            }
        }

        return result;

    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |SETTLEMENT_INSTRUCTIONS|
     */
    @Override
    public String parseSETTLEMENT_INSTRUCTIONS(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                               Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = "";
        prepareMarginCallEntry(message, dsCon);
        // get the PO settlement instructions for notification
        try {
            Vector notifSDI = getTemplateSDIList(getLeIdForSDI(message, trade, dsCon), dsCon, message);
            if (!Util.isEmpty(notifSDI)) {
                List<SdiHtmlRowData> cashSdisToPrint = new ArrayList<>();
                List<SdiHtmlRowData> secSdisToPrint = new ArrayList<>();
                for (int i = 0; i < notifSDI.size(); i++) {
                    SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) notifSDI.get(i);
                    // if cash
                    if (SettleDeliveryInstruction.SETTLEMENT == sdi.getType()) {
                        Vector listCurrencies = sdi.getCurrencyList();
                        if (!Util.isEmpty(listCurrencies)) {
                            for (int j = 0; j < listCurrencies.size(); j++) {
                                SdiHtmlRowData row = new SdiHtmlRowData();
                                row.setRowType("CASH");
                                row.setCurMarkPlace((String) listCurrencies.get(j));
                                PartySDI agent = sdi.getAgent();
                                if (agent != null) {
                                    LegalEntity leAgent = BOCache.getLegalEntity(dsCon, agent.getPartyId());
                                    if (leAgent != null) {
                                        row.setAgentIntermName(leAgent.getName());
                                    }
                                    row.setAgentIntermAcc(sdi.getAgentAccount());
                                    row.setAgentIntermBic(agent.getCodeValue());
                                }
                                cashSdisToPrint.add(row);
                            }
                        }
                    } else {
                        SdiHtmlRowData row = new SdiHtmlRowData();
                        row.setRowType("SECURITIES");
                        PartySDI agent = sdi.getAgent();
                        if (agent != null) {
                            LegalEntity leAgent = BOCache.getLegalEntity(dsCon, agent.getPartyId());
                            if (leAgent != null) {
                                row.setAgentIntermName(leAgent.getName());
                            }
                            row.setCurMarkPlace(sdi.getAttribute("Type of bonds"));
                            row.setAgentIntermAcc(sdi.getAgentAccount());
                            row.setAgentIntermBic(agent.getCodeValue());
                        }
                        secSdisToPrint.add(row);
                    }
                }

                result = sdisToHtml(cashSdisToPrint, secSdisToPrint);

            }
        } catch (RemoteException e) {
            Log.error(this, e);

        }

        return result;

    }

    /**
     * @param message
     * @param dsCon
     * @return the list of SDI that should be printed for this template
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected Vector getTemplateSDIList(int leId, DSConnection dsCon, BOMessage message) throws RemoteException {
        if (this.entry == null) {
            return new Vector();
        }
        //gets the contract type
        String notificationType;
        String contractType = getContractType(message, dsCon);
        //if it is CSD then the notification type is IM (Initial Margin)
        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CSD)) notificationType = IM;
        else notificationType = VM;

        //Constructs the query for extracting the SDIs

        String where = new StringBuilder("le_settle_delivery.method='NOTIFICATIONS'")
                .append(" and le_settle_delivery.bene_le=").append(leId)
                .append(" and le_settle_delivery.le_role='").append(LegalEntityRoleEnum.ProcessingOrg.getName()).append("'")
                .append(" and le_settle_delivery.preferred_b=1")
                .append(" and (le_settle_delivery.effective_from is null or le_settle_delivery.effective_from <= ")
                .append(ioSQL.date2String(this.entry.getProcessDate())).append(")")
                .append(" and (le_settle_delivery.effective_to is null or le_settle_delivery.effective_to >= ")
                .append(ioSQL.date2String(this.entry.getProcessDate())).append(")").toString();


        //Depending in the notification type, extracts SDIs with an attribute or other attribute
        if (notificationType.equals(IM)) {
            //Additional part for extracting the SDIs attribute
            String whereType = new StringBuilder(" and sdi_attribute.sdi_id=le_settle_delivery.sdi_id")
                    .append(" and sdi_attribute.attribute_name='").append(NOTIFICATION_TYPE).append("'")
                    .append(" and sdi_attribute.attribute_value='").append(notificationType).append("'").toString();
            return dsCon.getRemoteReferenceData().getSettleDeliveryInstructions("sdi_attribute", new StringBuilder(where).append(whereType).toString(), null);
        } else {
            Vector vector3 = dsCon.getRemoteReferenceData().getSettleDeliveryInstructions(null, where, null);
            Vector vectorR = new Vector();
            for (final Object obj : vector3) {
                if (obj instanceof SettleDeliveryInstruction) {
                    SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) obj;
                    if (sdi.getAttribute("NotificationType") != null) {
                        if (sdi.getAttribute("NotificationType").equals("VM")) {
                            vectorR.add(obj);
                        }
                    } else {
                        vectorR.add(obj);
                    }
                }
            }
            return vectorR;
        }
    }

    /**
     * @param message
     * @param dsCon
     * @return the legal entity id to get SDI that will be printed in this
     * template
     */
    protected int getLeIdForSDI(BOMessage message, Trade trade, DSConnection dsCon) {
        prepareMarginCallEntryDTO(message, dsCon);
        return this.marginConfig != null ? this.marginConfig.getPoId()
                : (trade != null) ? trade.getBook().getProcessingOrgBasedId() : 0;
    }

    /**
     * @param cashSdis
     * @param secSdis
     * @return an html table with the given sdi information
     */
    private String sdisToHtml(List<SdiHtmlRowData> cashSdis, List<SdiHtmlRowData> secSdis) {
        String tableHeader = "<table class=\"sdiList\">\n";
        StringBuffer cashRows = new StringBuffer("");
        StringBuffer secRows = new StringBuffer("");
        String tableFooter = "</table>";
        String separator = "<tr> <td colspan=\"5\" style=\"border-left: none; border-right: none;border-top: none;border-bottom: none;\"></td></tr>";

        for (SdiHtmlRowData sdi : cashSdis) {
            if (cashRows.length() == 0) {
                cashRows.append("<tr>");
                cashRows.append("<td rowspan=\"" + cashSdis.size() + "\">");
                cashRows.append("<b>" + sdi.getRowType() + "<b></td>");
            } else {
                cashRows.append("<tr>");
            }
            cashRows.append("<td>" + emptyIdNull(sdi.getCurMarkPlace()) + "</td>");
            cashRows.append("<td>" + emptyIdNull(sdi.getAgentIntermName()) + "</td>");
            cashRows.append("<td>" + emptyIdNull(sdi.getAgentIntermBic()) + "</td>");
            cashRows.append("<td>" + emptyIdNull(sdi.getAgentIntermAcc()) + "</td>");
            cashRows.append("</tr>");
        }

        for (SdiHtmlRowData sdi : secSdis) {
            if (secRows.length() == 0) {
                secRows.append("<tr>");
                secRows.append("<td rowspan=\"" + secSdis.size() + "\">");
                secRows.append("<b>" + sdi.getRowType() + "<b></td>");
            } else {
                secRows.append("<tr>");
            }
            secRows.append("<td>" + emptyIdNull(sdi.getCurMarkPlace()) + "</td>");
            secRows.append("<td>" + emptyIdNull(sdi.getAgentIntermName()) + "</td>");
            secRows.append("<td>" + emptyIdNull(sdi.getAgentIntermBic()) + "</td>");
            secRows.append("<td>" + emptyIdNull(sdi.getAgentIntermAcc()) + "</td>");
            secRows.append("</tr>");
        }

        return tableHeader + cashRows.toString() + separator + secRows.toString() + tableFooter;
    }

    private String emptyIdNull(String string) {
        return string == null ? "" : string;
    }

    /**
     * Internal bean to export sdi information in html format
     *
     * @author aela
     */
    class SdiHtmlRowData {

        private String rowType;
        private String curMarkPlace;
        private String agentIntermName;
        private String agentIntermBic;
        private String agentIntermAcc;

        public String getRowType() {
            return this.rowType;
        }

        public void setRowType(String rowType) {
            this.rowType = rowType;
        }

        public String getCurMarkPlace() {
            return this.curMarkPlace;
        }

        public void setCurMarkPlace(String curMarkPlace) {
            this.curMarkPlace = curMarkPlace;
        }

        public String getAgentIntermName() {
            return this.agentIntermName;
        }

        public void setAgentIntermName(String agentIntermName) {
            this.agentIntermName = agentIntermName;
        }

        public String getAgentIntermAcc() {
            return this.agentIntermAcc;
        }

        public void setAgentIntermAcc(String agentIntermAcc) {
            this.agentIntermAcc = agentIntermAcc;
        }

        public String getAgentIntermBic() {
            return this.agentIntermBic;
        }

        public void setAgentIntermBic(String agentIntermBic) {
            this.agentIntermBic = agentIntermBic;
        }

    }

    /**
     * @param message
     * @param dsConn
     * @return the template tile from the template name using the domainValues
     * configuration
     */
    public static String getTitleFromTemplateName(BOMessage message, DSConnection dsConn) {
        initTemplateNameTitleList(message, dsConn);
        String templateName = message.getTemplateName();
        // ORIG_TEMPLATE
        if (NOTIF_EMAIL_TEMPLATE.equals(templateName)) {
            templateName = message.getAttribute("ORIG_TEMPLATE");
        }
        return templateNameTitle.get(templateName);

    }

    public static String getFromAddress(BOMessage message, CollateralConfig mcc, DSConnection dsCon) {
        if (message == null) {
            return null;
        }

        // get the sender contact id
        final LEContact senderContact = BOCache.getLegalEntityContact(dsCon, message.getSenderContactId());
        String fromEmail = DEFAULT_FROM_EMAIL;
        String templateName = message.getTemplateName();
        // ORIG_TEMPLATE
        if (NOTIF_EMAIL_TEMPLATE.equals(templateName)) {
            templateName = message.getAttribute("ORIG_TEMPLATE");
        }

        if (senderContact != null) {
            // TODO to redesign
            String additionalAddressCode = mcc.getContractType() + "_FROM_EMAIL";
            if ("MarginCallValuationStatement.htm".equals(templateName)) {
                additionalAddressCode = additionalAddressCode + "_VAL_STAT";
            } else if (NOTIF_EMAIL_TEMPLATE_PORTFOLIO.equals(templateName) || NOTIF_EMAIL_TEMPLATE_PORTFOLIOMSFA.equals(templateName)) {
                additionalAddressCode = additionalAddressCode + "_PRTF_RECO";
            }
            String senderAddress = senderContact.getAddressCode(additionalAddressCode);
            if (!Util.isEmpty(senderAddress)) {
                fromEmail = senderAddress;
            }
        }

        return fromEmail;
    }

    /**
     * Init the list of tempalteName<-->TemplateTitle by retrieving the
     * configuration from domainValues
     *
     * @param message
     * @param dsConn
     */
    public static void initTemplateNameTitleList(BOMessage message, DSConnection dsConn) {
        templateNameTitle = (templateNameTitle != null ? templateNameTitle : new HashMap<String, String>());
        if (templateNameTitle.size() == 0) {
            Vector domainValueTemplates = LocalCache.getDomainValues(dsConn, "PDF.Templates");
            if (!Util.isEmpty(domainValueTemplates)) {
                for (int i = 0; i < domainValueTemplates.size(); i++) {
                    String domainValue = (String) domainValueTemplates.get(i);
                    if (domainValue != null) {
                        String domainComment = LocalCache.getDomainValueComment(dsConn, "PDF.Templates", domainValue);
                        templateNameTitle.put(domainValue, domainComment);
                    }
                }
            }
        }

    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |ALLOCATIONS_PROPOSAL|
     */
    public String parseALLOCATIONS_PROPOSAL(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                            Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        StringBuffer allocRows = new StringBuffer("");

        MarginCallEntryDTO entry = getMarginCallEntryDTO(message, dsCon);

        if (entry != null) {
            // list of all entry allocations (Securities and Cash)
            List<MarginCallAllocationDTO> allocations = new ArrayList<>();
            // get Cash allocations
            List<CashAllocationDTO> cashAllocations = entry.getCashAllocations();
            // get Securities allocations
            List<SecurityAllocationDTO> securityAllocations = entry.getSecurityAllocations();

            // merge the cash and securities allocations
            allocations.addAll(cashAllocations);
            allocations.addAll(securityAllocations);

            List<MarginCallAllocationDTO> nonCanceledAllocs = new ArrayList<>();
            if (!Util.isEmpty(allocations)) {
                // exclude canceled allocations
                for (MarginCallAllocationDTO alloc : allocations) {
                    if ((alloc != null) && !MarginCallAllocation.STATUS_CANCELLED.equals(alloc.getStatus())) {
                        nonCanceledAllocs.add(alloc);
                    }
                }
            }

            String tableHeader = "<table class=\"small\" style=\"width: 100%\">";
            String tableFooter = "</table>";

            allocRows.append(tableHeader);

            allocRows.append("<tr>");
            allocRows.append(addRowValue("<b>Action</b>"));
            allocRows.append(addRowValue("<b>Value Date</b>"));
            allocRows.append(addRowValue("<b>Collateral</b>"));
            allocRows.append(addRowValue("<b>Face/Par</b>"));
            allocRows.append(addRowValue("<b>Market Value</b>"));
            allocRows.append(addRowValue("<b>Adjusted Value</b>"));
            allocRows.append("</tr>");

            for (MarginCallAllocationDTO alloc : nonCanceledAllocs) {

                boolean isCash = "Cash".equals(alloc.getUnderlyingType());

                allocRows.append("<tr>");
                allocRows.append(addRowValue(emptyIdNull(alloc.getDirection())));
                allocRows.append(addRowValue(Util.dateToString(alloc.getSettlementDate(), Locale.getDefault())));
                allocRows.append(addRowValue(emptyIdNull(isCash ? alloc.getUnderlyingType() + " " + alloc.getCurrency()
                        : alloc.getUnderlyingType() + " " + alloc.getDescription().substring(0, 40))));
                allocRows.append(addRowValue(Util.numberToString(isCash ? ((CashAllocationDTO) alloc).getPrincipal()
                        : ((SecurityAllocationDTO) alloc).getNominal(), Locale.getDefault())));
                allocRows.append(addRowValue(Util.numberToString(alloc.getValue(), Locale.getDefault())));
                allocRows.append(addRowValue(Util.numberToString(alloc.getContractValue(), Locale.getDefault())));
                allocRows.append("</tr>");
            }
            allocRows.append(tableFooter);
        }

        return allocRows.toString();
    }

    private String addRowValue(String value) {
        return "<td style=\"text-align: left;\"><span class=\"small\">" + value + "</span></td>";
    }

    /**
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return
     */
    public String parsePOLITE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                              BOTransfer transfer, DSConnection dsConn) {
        String poShortName = BOCache.getLegalEntityCode(dsConn, po.getLegalEntityId());
        if (PO_SOVEREIGN.equals(poShortName)) {
            return "Dear Sir or Madam,";
        }
        return "Dear Sirs,";
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |SETTLEMENT_INSTRUCTIONS_ABA_CODE|
     */
    public String parseSETTLEMENT_INSTRUCTIONS_ABA_CODE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String result = "";
        prepareMarginCallEntry(message, dsCon);
        // get the PO settlement instructions for notification
        try {
            Vector notifSDI;
            if (this.entry != null) {
                notifSDI = getTemplateSDIList(getLeIdForSDI(message, trade, dsCon), dsCon, message);
            } else {
                notifSDI = getTemplateSDIList(getLeIdForSDI(message, trade, dsCon), trade, dsCon, message);
            }
            if (!Util.isEmpty(notifSDI)) {
                List<SdiHtmlRowData> cashSdisToPrint = new ArrayList<>();
                List<SdiHtmlRowData> secSdisToPrint = new ArrayList<>();
                for (int i = 0; i < notifSDI.size(); i++) {
                    SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) notifSDI.get(i);
                    // if cash
                    if (SettleDeliveryInstruction.SETTLEMENT == sdi.getType()) {
                        Vector listCurrencies = sdi.getCurrencyList();
                        if (!Util.isEmpty(listCurrencies)) {
                            for (int j = 0; j < listCurrencies.size(); j++) {
                                SdiHtmlRowData row = new SdiHtmlRowData();
                                row.setRowType("CASH");
                                row.setCurMarkPlace((String) listCurrencies.get(j));
                                setSDIHtmlAgentDetails(sdi, row, (String) listCurrencies.get(j));
                                cashSdisToPrint.add(row);
                            }
                        }
                    } else {
                        SdiHtmlRowData row = new SdiHtmlRowData();
                        row.setRowType("SECURITIES");
                        row.setCurMarkPlace(sdi.getAttribute("Type of bonds"));
                        setSDIHtmlAgentDetails(sdi, row);
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

    private void setSDIHtmlAgentDetails(SettleDeliveryInstruction sdi, SdiHtmlRowData row, String currency) {
        PartySDI agent = sdi.getAgent();
        if (agent != null) {
            Account account = new Account();
            LegalEntity leAgent = BOCache.getLegalEntity(DSConnection.getDefault(), agent.getPartyId());
            if (leAgent != null) {
                row.setAgentIntermName(leAgent.getName());
                try {
                    account = BOCache.getAccount(DSConnection.getDefault(), sdi.getAgentAccount(),
                            leAgent.getLegalEntityId(), currency);
                } catch (Exception ex) {
                    Log.error(this, ex.getMessage());
                    Log.error(this, ex);//Sonar
                }
            }
            row.setAgentIntermAcc(sdi.getAgentAccount());
            // ABA Code
            row.setAgentIntermBic(account != null ? account.getAccountProperty("ABA") : "");
        }
    }

    private void setSDIHtmlAgentDetails(SettleDeliveryInstruction sdi, SdiHtmlRowData row) {
        PartySDI agent = sdi.getAgent();
        if (agent != null) {
            LegalEntity leAgent = BOCache.getLegalEntity(DSConnection.getDefault(), agent.getPartyId());
            Account account = new Account();
            if (leAgent != null) {
                row.setAgentIntermName(leAgent.getName());

                try {
                    if (!Util.isEmpty(sdi.getCurrencyList())) {
                        account = BOCache.getAccount(DSConnection.getDefault(), sdi.getAgentAccount(),
                                leAgent.getLegalEntityId(), (String) sdi.getCurrencyList().get(0));
                    } else {
                        account = BOCache.getAccount(DSConnection.getDefault(), sdi.getAgentAccount(),
                                leAgent.getLegalEntityId(), "ANY");
                    }
                } catch (Exception ex) {
                    Log.system(MarginCallMessageFormatter.class.getName(), ex.toString());
                    Log.error(this, ex);//Sonar
                }
            }
            row.setAgentIntermAcc(sdi.getAgentAccount());
            // ABA Code
            row.setAgentIntermBic(account != null ? account.getAccountProperty("ABA") : "");
        }
    }

    /**
     * @param cashSdis
     * @param secSdis
     * @return an html table with the given sdi information
     */
    private String sdisABACodeToHtml(List<SdiHtmlRowData> cashSdis, List<SdiHtmlRowData> secSdis) {
        StringBuffer html = new StringBuffer();
        String tableHeader = "<table class=\"sdiList\">\n";
        String tableFooter = "</table>";
        StringBuffer labelsBuffer = new StringBuffer("");
        String separator = "<tr><td colspan=\"5\" style=\"border-left: none; border-right: none;border-top: none;border-bottom: none;\"></td></tr>";

        html.append(tableHeader);
        html.append(labelsBuffer.append("<tr>").append("<td align=\"center\"><b>CASH/SECURITIES</b></td>")
                .append("<td align=\"center\"><b>TYPE</b></td>").append("<td align=\"center\"><b>AGENT</b></td>")
                .append("<td align=\"center\"><b>ABA</b></td>")
                .append("<td align=\"center\"><b>ACCOUNT NUMBER</b></td></tr>"));
        if (!Util.isEmpty(cashSdis)) {
            html.append(addSDIRowDetails(cashSdis));
            html.append(separator);
        }
        html.append(addSDIRowDetails(secSdis));
        html.append(tableFooter);

        return html.toString();

    }

    private String addSDIRowDetails(List<SdiHtmlRowData> sdis) {
        StringBuffer sdiRowDetails = new StringBuffer();
        for (SdiHtmlRowData sdi : sdis) {
            if (sdiRowDetails.length() == 0) {
                sdiRowDetails.append("<tr>");
                sdiRowDetails.append("<td rowspan=\"" + sdis.size() + "\">");
                sdiRowDetails.append("<b>" + sdi.getRowType() + "<b></td>");
            } else {
                sdiRowDetails.append("<tr>");
            }
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getCurMarkPlace())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentIntermName())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentIntermBic())));
            sdiRowDetails.append(addRowValue(emptyIdNull(sdi.getAgentIntermAcc())));

            sdiRowDetails.append("</tr>");
        }
        return sdiRowDetails.toString();
    }

    public String parseMC_PROCESSING_ORG(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (this.marginConfig == null) {
            if (trade != null) {
                LegalEntity legalentity = BOCache.getProcessingOrg(dsCon, trade.getBook());
                return legalentity.getCode();
            } else {
                return "";
            }
        }
        return this.marginConfig.getProcessingOrg() != null ? this.marginConfig.getProcessingOrg().getCode() : "";
    }

    /**
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return
     */
    public String parsePO_SOVEREIGN(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        return PO_SOVEREIGN;
    }

    /**
     * @param message
     * @param dsCon
     * @return the list of SDI that should be printed for this template
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected Vector getTemplateSDIList(int leId, Trade trade, DSConnection dsCon, BOMessage message) throws RemoteException {
        //gets the contract type
        String notificationType;
        String contractType = getContractType(message, dsCon);
        //if it is CSD then the notification type is IM (Initial Margin)
        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CSD)) notificationType = IM;
        else notificationType = VM;

        //Constructs the query for extracting the SDIs

        String where = new StringBuilder("le_settle_delivery.method='NOTIFICATIONS'")
                .append(" and le_settle_delivery.bene_le=").append(leId)
                .append(" and le_settle_delivery.le_role='").append(LegalEntityRoleEnum.ProcessingOrg.getName()).append("'")
                .append(" and le_settle_delivery.preferred_b=1")
                .append(" and (le_settle_delivery.effective_from is null or le_settle_delivery.effective_from <= ")
                .append(ioSQL.date2String(trade.getTradeDate())).append(")")
                .append(" and (le_settle_delivery.effective_to is null or le_settle_delivery.effective_to >= ")
                .append(ioSQL.date2String(trade.getTradeDate())).append(")").toString();

        //Additional part for extracting the SDIs attribute


        //Depending in the notification type, extracts SDIs with an attribute or other attribute
        if (notificationType.equals(IM)) {
            String whereType = new StringBuilder(" and sdi_attribute.sdi_id=le_settle_delivery.sdi_id")
                    .append(" and sdi_attribute.attribute_name='").append(NOTIFICATION_TYPE).append("'")
                    .append(" and sdi_attribute.attribute_value='").append(notificationType).append("'").toString();
            return dsCon.getRemoteReferenceData().getSettleDeliveryInstructions("sdi_attribute", new StringBuilder(where).append(whereType).toString(), null);
        } else {
            Vector vector3 = dsCon.getRemoteReferenceData().getSettleDeliveryInstructions(null, where, null);
            Vector vectorR = new Vector();
            for (final Object obj : vector3) {
                if (obj instanceof SettleDeliveryInstruction) {
                    SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) obj;
                    if (sdi.getAttribute("NotificationType") != null) {
                        if (sdi.getAttribute("NotificationType").equals("VM")) {
                            vectorR.add(obj);
                        }
                    } else {
                        vectorR.add(obj);
                    }
                }
            }
            return vectorR;
        }
    }

    private String getContractType(BOMessage message, DSConnection dsCon) {
        //gets the Collateral Config to get its contract type
        CollateralConfig mcc = getMcc(message.getStatementId(), dsCon);
        if ((mcc == null) || (Util.isEmpty(mcc.getContractType()))) return "";
        return mcc.getContractType();
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |IM_TERM_CURRENCY|
     */
    public String parseIM_TERM_CURRENCY(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                        BOTransfer transfer, DSConnection dsConn) {
        if (!Util.isEmpty(this.marginConfig.getAdditionalField(IM_TERMINATION_CCY))) {
            return this.marginConfig.getAdditionalField(IM_TERMINATION_CCY);
        }
        return "";
    }

    /**
     * @param message       message being formatted
     * @param trade         trade related to the message
     * @param sender        Processing organization for the trade
     * @param rec           counter party for the trade
     * @param transferRules error messages vector
     * @param transfer      the transfer related to the message if relevant
     * @param con           the Data Server connection
     * @return value for keyword |DATE|
     */
    @Override
    public String parseDATE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules,
                            BOTransfer transfer, DSConnection con) {

        String contractType = parseCONTRACT_TYPE(message, trade, sender, rec, transferRules, transfer, con);

        if (contractType.equalsIgnoreCase(CONTRACT_TYPE_CGAR)) {

            JDate jdate = JDate.getNow();
            Date date = jdate.getDate(TimeZone.getDefault());
            return spanishDateFormat.format(date);
        }

        return super.parseDATE(message, trade, sender, rec, transferRules, transfer, con);

    }

    public String parseISIN(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                               Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        MarginCall mc = (MarginCall) trade.getProduct();
        if (null != mc.getSecurity()) {
            String isin = mc.getSecurity().getSecCode("ISIN");
            return null != isin ? isin : "";
        }
        return "";
    }

    public String parseDESCRIPTION(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                            Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        MarginCall mc = (MarginCall) trade.getProduct();
        if (null != mc.getSecurity()) {
            String description = mc.getSecurity().getName();
            return null != description ? description : "";
        }
        return "";
    }

    public String parseCSD_CUSTODIAN_PLEDGE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                   Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String ret = trade.getKeywordValue("CSDCustodianPledge");
        return null != ret ? ret : "";
    }

    public String parseCONTRACT_GROUP(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        CollateralConfig mcc = getMcc(message.getStatementId(), dsCon);
        if (mcc == null || Util.isEmpty(mcc.getContractGroup())) {
            return "";
        } else {
            return mcc.getContractGroup();
        }
    }

    public String parsePREVIOUS_RQV(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntry(message, dsCon);
        return numberToString(this.entry.getPreviousRQV(), NUMBER_OF_DEC);
    }


    public String parseREQUIRED_VALUE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntry(message, dsCon);

        return numberToString(this.entry.getRqv(), NUMBER_OF_DEC);
    }

    public String parseTOTAL_PREV_MRG(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntry(message, dsCon);

        return numberToString(this.entry.getPreviousTotalMargin(), NUMBER_OF_DEC);
    }

    public String parseAGREED_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntry(message, dsCon);

        return numberToString(this.entry.getAgreedDisputeAmount(), NUMBER_OF_DEC);
    }

    public String parseMCC_TRIPARTY_AGENT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        prepareMarginCallEntry(message, dsCon);

        return this.marginConfig.getTripartyAgent();
    }

    /**
     * @param message     message being formatted
     * @param trade       trade related to the message
     * @param po          Processing organization for the trade
     * @param cp          counter party for the trade
     * @param paramVector error messages vector
     * @param transfer    the transfer related to the message if relevant
     * @param dsConn      the Data Server connection
     * @return value for keyword |IM_TERM_CURRENCY|
     */
    public String parseACADIA_AMPID(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                        BOTransfer transfer, DSConnection dsConn) {
        if (!Util.isEmpty(this.marginConfig.getAdditionalField("ACADIA_AMPID"))) {
            return this.marginConfig.getAdditionalField("ACADIA_AMPID");
        }
        return "";
    }

    public String parseDELIVERER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        return SantMessageFormatterUtil.getInstance().parseDELIVERER_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
    }

    public String parseRECEIVER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        return SantMessageFormatterUtil.getInstance().parseRECEIVER_NAME(message, trade, sender, rec, transferRules, transfer, dsCon);
    }

    public String parseUNDERLYING_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String underlyingType = "";
        MarginCall mc = (MarginCall) trade.getProduct();
        if (null != mc.getSecurity()) {
            underlyingType = mc.getSecurity().getUnderlyingProduct().getType();
        }
        return null != underlyingType ? underlyingType : "";
    }


}
