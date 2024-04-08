package calypsox.tk.bo.swift;

import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.RemoteBackOffice;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.util.Optional;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Customize the contents of SWIFT text messages.
 */
public class SwiftTextCustomizer extends com.calypso.tk.bo.swift.SwiftTextCustomizer {

    /**
     * Beginning of MUR code.
     */
    private static final String MUR_TAG = "GT";
    /**
     * Regular expression to find the start of the MUR code in a SWIFT message.
     */
    private static final String MUR_START_REGEX = "\\{3:.*\\{108:";
    /**
     * Regular expression to find the end of the MUR code in a SWIFT message.
     */
    private static final String MUR_END_REGEX = "\\}";

    private static final int INT_TO_CHAR_BASE = 26;
    private static final int MUR_LENGTH = 14;
    private static final char MUR_PADDING_CHARACTER = '0';

    //AII NUEVOS MIEMBROS DE CLASE
    private static final String EMPTY_FOOTER = "{5:}";
    private static final String TOKEN_113_NNES = "{113:NNES}";
    private static final String TOKEN_113_NNNN = "{113:NNNN}";
    private static final String EUR = "EUR";
    private static final String TOKEN_103_TGT = "{103:TGT}";
    private static final String TOKEN_119_STP = "{119:STP}";
    private static final String template103 = "MT103";
    private static final String template202 = "MT202";
    private static final String template202COV = "MT202COV";
    private static final String TGT2Method = "TARGET2";
    // TOKEN 121 - UETR INFO
    private static final String TOKEN_121_UETR_INI = "{121:";
    private static final String TOKEN_121_UETR_FIN = "}";

    //FIN AII

    private static final String TOTTA_GTW_NAME = "GestorSTPTotta";


    @Override
    public String getSwiftText(final SwiftMessage swiftMessage, final PricingEnv pricingEnv, final BOMessage boMessage,
                               final DSConnection dsCon) {
        StringBuilder sb = null;
        sb = new StringBuilder();

        if (boMessage.getTemplateName().contains("CREST")) {
            sb.append(getCRESTBasicHeaderBlock(dsCon));
            sb.append(getMessageBlock(swiftMessage));
            sb.append(getCRESTFinalBlock());
        } else {
            sb.append(getBasicHeaderBlock(swiftMessage));
            sb.append(getApplicationHeaderBlock(swiftMessage, boMessage, dsCon));
            sb.append(getUserHeaderBlock(swiftMessage, boMessage, dsCon));
            sb.append(getMessageBlock(swiftMessage));
            sb.append(getFinalBlock(swiftMessage));
        }

        if (Log.isDebug()) {
            final byte[] bytes = sb.toString().getBytes();
            final StringBuilder hexRepresentation = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                hexRepresentation.append(Integer.toString(
                        (bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            Log.debug(this, "SwiftTextCustomizer::binary representation:"
                    + hexRepresentation);
        }
        return sb.toString();
    }


    private String getBasicHeaderBlock(final SwiftMessage swiftMessage) {
        String basicHeaderBlock = swiftMessage.getBasicHeaderBlock();

        if (swiftMessage.getGateway() != null && swiftMessage.getGateway().equals(TOTTA_GTW_NAME)) {
        	BOMessage msg = swiftMessage.getMessage();
        	if (msg != null && msg.getLongId() > 0) {
        		String toBeReplaced = basicHeaderBlock.substring(18, 28);
        		String toReplace = StringUtils.leftPad(String.valueOf(msg.getLongId()), 10, '0');
        		basicHeaderBlock = basicHeaderBlock.replaceAll(toBeReplaced, toReplace);
        	}
        }

        return basicHeaderBlock;
    }

    private String getCRESTBasicHeaderBlock (final DSConnection dsCon) {
        Vector<String> domainCache = LocalCache.getDomainValues(dsCon, "CREST.IUSE");
        String iuse = "";
        iuse = domainCache.get(0);

        String senderReference = ":senderReference:" + iuse + "\n";
        String text = ":text:\n";
        return senderReference + text;
    }

    private String getApplicationHeaderBlock(final SwiftMessage swiftMessage,
                                             final BOMessage boMessage, final DSConnection dsCon) {
        String applicationHeaderBlock = swiftMessage.getApplicationHeaderBlock();

        // add modifications here

        //Este if usa una domain para ver si esta activa para eliminar la prioridad
        // if (SantanderSwiftUtil.eraseDeliveryNotification(dsCon)) {
        if (!isIncomingMsg(boMessage)) {
            applicationHeaderBlock = applicationHeaderBlock.replace("2020", "");
        }
        // }

        return applicationHeaderBlock;
    }


    /**
     * Get the User Header block (block 3) and do the required modifications to it.
     *
     * @param SwiftMessage The message to retrieve the User Header block
     * @param BOMessage    Required BOMessage for internal use.
     * @return A String which contains the Customized User header Block
     * @param    DsConnection A connection to DataServer
     */
    private String getUserHeaderBlock(final SwiftMessage swiftMessage,
                                      final BOMessage boMessage, final DSConnection dsCon) {


        String FormattedSwiftText = swiftMessage.getUserHeaderBlock();
        StringBuilder swiftTextBuilder = new StringBuilder(FormattedSwiftText);
        //AII userblok modification. Updated MUR
        substituteMUR(swiftTextBuilder, boMessage, dsCon);

        //AII  tag 103 TGT
        FormattedSwiftText = add103ToHeader(swiftTextBuilder.toString(), boMessage, dsCon);
        swiftTextBuilder = new StringBuilder(FormattedSwiftText);
        //
        //AII tag 113 bank priority
        FormattedSwiftText = add113ToHeader(swiftTextBuilder.toString(), boMessage, dsCon);
        swiftTextBuilder = new StringBuilder(FormattedSwiftText);

        //AII  tag 121 UETR.
        FormattedSwiftText = checkAndFixField121(swiftTextBuilder.toString(), boMessage, dsCon);
        swiftTextBuilder = new StringBuilder(FormattedSwiftText);


        //AII deleting of field 119  {119:STP}
        //check119field(swiftTextBuilder, boMessage, dsCon);

        return swiftTextBuilder.toString();
    }


    /**
     * Get the Message block (block 4) and do the required modifications to it.
     *
     * @param SwiftMessage The message to retrieve the trailer block
     * @return A String which contains the Customized Message Block.
     */
    private String getMessageBlock(final SwiftMessage swiftMessage) {
        String message = "";
        if (swiftMessage.getBOMessage() != null && swiftMessage.getBOMessage().getTemplateName().contains("CREST")) {
            message = swiftMessage.getSwiftBody();
        } else {
            message = "{4:" + SwiftMessage.START_OF_TEXT
                    + swiftMessage.getSwiftBody() + SwiftMessage.END_OF_TEXT + "}";
        }
        // add modifications here

        return message;
    }


    /**
     * Get the final block (block 5 or trailer block) and do the required modifications to it.
     *
     * @param SwiftMessage The message to retrieve the trailer block
     * @return An empty String if the finalBlock is equals to EMPTY_FOOTER else
     * the finalBlock
     */
    private String getFinalBlock(final SwiftMessage swiftMessage) {
        String finalBlock = swiftMessage.getFinalBlock();
        // add modifications here

        // ends of custom modifications
        finalBlock = refactorFinalBlockWhenIsEmpty(finalBlock);
        return finalBlock;
    }

    private String getCRESTFinalBlock() {
        String endOfText = ":EndOfText:\n";
        return endOfText;
    }

    /**
     * Delete the finalBlock of the SwiftMessage when is empty
     *
     * @param footer previous footer
     * @return An empty String if the finalBlock is equals to EMPTY_FOOTER else
     * the finalBlock
     */
    private String refactorFinalBlockWhenIsEmpty(final String finalBlock) {
        if (SwiftTextCustomizer.EMPTY_FOOTER.compareTo(finalBlock) == 0) {
            return "";
        }
        return finalBlock;
    }

    //AII fin codigo nuevo

    /**
     * AII MODIFICADO. ES EL ORIOGINAL DE CALyPSO Overridden method to customize the contents of SWIFT messages.
     */
    public String getSwiftText_AII(SwiftMessage message, PricingEnv pricingEnv, BOMessage boMessage, DSConnection dsCon) {
        String defaultSwiftText = super.getSwiftText(message, pricingEnv, boMessage, dsCon);
        StringBuilder swiftTextBuilder = new StringBuilder(defaultSwiftText);

        substituteMUR(swiftTextBuilder, boMessage, dsCon);

        String swiftTextBuilderToString = swiftTextBuilder.toString();

        //Delete block 5 to send from calypso to GestorSTP
        //If there are differents Swift messagess you have to insert this part
        //of code in a if block to check the type of Swift
        if (swiftTextBuilder != null && swiftTextBuilderToString != null) {
            String swiftTextReplaced = swiftTextBuilderToString.replace("{5:}", "");

            swiftTextReplaced = modHeaderBlock(swiftTextReplaced);

            if (swiftTextReplaced != null) {
                return swiftTextReplaced;
            }
        }

        return swiftTextBuilderToString;
    }


    public String modHeaderBlock(String swiftTextReplaced) {
        try {
            if (!Util.isEmpty(swiftTextReplaced)) {
                Pattern pattern = Pattern.compile("\\{2(.*?)\\}");
                Matcher matcher = pattern.matcher(swiftTextReplaced);

                if (matcher.find() && matcher.group(0).contains("2020")) {
                    String oldValue = matcher.group(0);
                    String newValue = matcher.group(0).replace("2020", "");
                    return swiftTextReplaced.replace(oldValue, newValue);
                }
            }
        } catch (Exception e) {
            Log.error(this, "Cannot mod Heador block of message: " + swiftTextReplaced);
        }

        return swiftTextReplaced;
    }

    private void check119ield(StringBuilder swiftTextBuilder, BOMessage boMessage, DSConnection dsCon) {
        // Remove field 119 STP
        String str_aux = swiftTextBuilder.toString();
        int index = str_aux.indexOf(SwiftTextCustomizer.TOKEN_119_STP);
        if (index >= 0)
            swiftTextBuilder = swiftTextBuilder.replace(index, index + SwiftTextCustomizer.TOKEN_119_STP.length(), "");

    }

    private String add103ToHeader(final String userHeaderBlock,
                                  final BOMessage boMessage, DSConnection dsCon) {
        Log.debug(this, "SwiftTextCustomizer.add103ToHeader: Start.");


        //First: check if msg templates are MT103 or MT202 only for TGT2 Payments.
        String templateName = boMessage.getTemplateName();
        if (templateName == null) {
            Log.debug(this,
                    "SwiftTextCustomizer.check103field: The template name of the message:"
                            + boMessage.getLongId()
                            + " is null. Nothing added to user header.");
            return userHeaderBlock;
        }

        if (templateName.equals(SwiftTextCustomizer.template103) || templateName.equals(SwiftTextCustomizer.template202)) {
            //Second: retrieve CptySDI to check if transfer Method is Target2. If so, we add the 103:TGT field.
            //final String addressType=boMessage.getAddressMethod();

            try {
                BOTransfer transferencia = DSConnection.getDefault().getRemoteBO().getBOTransfer(boMessage.getTransferLongId());

                TradeTransferRule rule = transferencia.toTradeTransferRule();
                SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(dsCon, rule.getCounterPartySDId());


                final String metodo = si.getSettlementMethod();

                if (metodo != null && metodo.equals(SwiftTextCustomizer.TGT2Method)) {
                    //at this point we add the 103 field
                    final String block = removeTags(userHeaderBlock, "103");
                    return addTextToHeaderOnFirstPosition(block,
                            SwiftTextCustomizer.TOKEN_103_TGT);

                }
            } catch (CalypsoServiceException e) {
                Log.debug(SwiftTextCustomizer.class, "SwiftTextCustomizer.check103field: Something was wrong ckecking the need to add 103 tag for TGT2: "
                        + e);
                return userHeaderBlock;

            }
        } else Log.debug(this,
                "SwiftTextCustomizer.check103field: No validations passed. Nothing to add.");

        return userHeaderBlock;
    }


    /**
     * Looks for the place where the MUR code should be written in the given
     * text and substitutes the existing MUR with a customized code.
     */
    private void substituteMUR(StringBuilder swiftTextBuilder, BOMessage boMessage, DSConnection dsCon) {
        Pattern murStartPattern = Pattern.compile(MUR_START_REGEX);
        Matcher murStartMatcher = murStartPattern.matcher(swiftTextBuilder.toString());
        if (murStartMatcher.find()) {
            int murStartIndex = murStartMatcher.end();

            Pattern murEndPattern = Pattern.compile(MUR_END_REGEX);
            Matcher murEndMatcher = murEndPattern.matcher(swiftTextBuilder);
            if (murEndMatcher.find(murStartIndex)) {
                int murEndIndex = murEndMatcher.start();
                swiftTextBuilder.delete(murStartIndex, murEndIndex);
                swiftTextBuilder.insert(murStartIndex, getMsgVersionCharToMUR(dsCon, boMessage));
            } else {
                Log.error(this, String.format("Could not find end of MUR for message %d", boMessage.getLongId()));
            }
        } else {
            Log.error(this,
                    String.format("Could not find field 108 in message %d to write MUR code", boMessage.getLongId()));
        }
    }

    /**
     * Returns the MUR code corresponding to the given message.
     */
    private String getMsgVersionCharToMUR(final DSConnection dsCon, final BOMessage boMessage) {
        final int documentVersion = SwiftUtilPublic.getDocumentVersion(dsCon, boMessage);
        final String version = SantanderUtil.getInstance().mapIntToChar(documentVersion, INT_TO_CHAR_BASE);
        final String strId = Util.lpad(String.valueOf(boMessage.getLongId()), MUR_LENGTH - version.length(),
                MUR_PADDING_CHARACTER);

        StringBuilder mur = new StringBuilder();
        mur.append(MUR_TAG);
        mur.append(version);
        mur.append(strId);

        return mur.toString();
    }

    /**
     * Add to the applicationHeaderBlock the token "{113:NNES}" if the
     * settlement currency is EUR and the ISOCode of the counterParty is "ES"
     * else add "{113:NNES}". If the header has the token "{103:TGT}" the new
     * token is added after this, else is added at the start of the header
     *
     * @param userHeaderBlock
     * @param boMessage
     * @return the new header.
     */
    private String add113ToHeader(final String userHeaderBlock,
                                  final BOMessage boMessage, final DSConnection dsCon) {
        final String templateName = boMessage.getTemplateName();
        String newUserHeaderBlock = userHeaderBlock;
        if (templateName == null) {
            Log.debug(this,
                    "SwiftTextCustomizer.add113ToHeader: The template name of the message:"
                            + boMessage.getLongId()
                            + " is null.");
            return newUserHeaderBlock;
        }
        if ("MT103".equals(templateName) || "MT202".equals(templateName) || "MT541BILAT".equals(templateName) || "MT543BILAT".equals(templateName)) {
            Log.debug(this, "SwiftTextCustomizer.add113ToHeader: Start");
            // getting RemoteBackOffice
            final RemoteBackOffice rbo = dsCon.getRemoteBO();
            if (rbo == null) {
                Log.info(
                        this,
                        "SwiftTextCustomizer.add113ToHeader: Error getting the RemoteBackOffice. RemoteBackOffice is null");
                return newUserHeaderBlock;
            }
            // getting BOTransfer
            BOTransfer transfer = null;
            try {
                transfer = rbo.getBOTransfer(boMessage.getTransferLongId());
            } catch (final RemoteException re) {
                Log.error(
                        this,
                        "SwiftTextCustomizer.add113ToHeader: RemoteException while getting the transfer:"
                                + boMessage.getTransferLongId() + ".", re);
            }
            if (transfer == null) {
                Log.info(this,
                        "SwiftTextCustomizer.add113ToHeader: The transfer:"
                                + boMessage.getTransferLongId() + " is null.");
                return newUserHeaderBlock;
            }
            // getting LegalEntity
            final LegalEntity contrapartyLe = BOCache.getLegalEntity(dsCon,
                    transfer.getExternalLegalEntityId());
            if (contrapartyLe == null) {
                Log.info(this,
                        "SwiftTextCustomizer.add113ToHeader: The legal entity:"
                                + transfer.getExternalLegalEntityId()
                                + " is null");
                return newUserHeaderBlock;
            }
            // getting Country
            final String countryS = contrapartyLe.getCountry();
            if (countryS == null) {
                Log.info(this,
                        "SwiftTextCustomizer.add113ToHeader: The country of the legal entity: "
                                + contrapartyLe.getId() + " is null.");
                return null;
            }
            final Country country = BOCache.getCountry(
                    DSConnection.getDefault(), countryS);
            if (country == null) {
                Log.info(this,
                        "SwiftTextCustomizer.add113ToHeader: The country of the legal entity: "
                                + contrapartyLe.getId() + " is null.");
                return null;
            }
            // getting ISOCode
            final String isoCode = country.getISOCode();
            if (isoCode == null) {
                Log.info(this,
                        "SwiftTextCustomizer.add113ToHeader: The ISOCode of the country: "
                                + country.getId() + " is null.");
                return null;
            }
            /*
             * start logic:if the settlement currency of the transfer is EUR and
             * the ISOCode of the counterParty is ES then add NNES.
             * If country is not ES, but ccy=EUR then add NNNN
             */
            if (transfer.getSettlementCurrency().compareTo(
                    SwiftTextCustomizer.EUR) == 0) {
                String valueField113;
                if ("ES".equals(isoCode)) {
                    Log.debug(this, "Header with "
                            + SwiftTextCustomizer.TOKEN_113_NNES);
                    valueField113 = SwiftTextCustomizer.TOKEN_113_NNES;
                } else {
                    Log.debug(this, "Header with "
                            + SwiftTextCustomizer.TOKEN_113_NNNN);
                    valueField113 = SwiftTextCustomizer.TOKEN_113_NNNN;
                }

                newUserHeaderBlock = removeTags(newUserHeaderBlock, "113");
                final String aux = addTextToHeader(newUserHeaderBlock,
                        valueField113, SwiftTextCustomizer.TOKEN_103_TGT);
                if (aux == null) {
                    newUserHeaderBlock = addTextToHeaderOnFirstPosition(
                            newUserHeaderBlock, valueField113);
                } else {
                    newUserHeaderBlock = aux;
                }
            }
        }
        return newUserHeaderBlock;
    }

    /**
     * Removes the tags.
     *
     * @param header  the header
     * @param tagname the tagname
     * @return the string
     */
    protected String removeTags(final String header, final String tagname) {
        final String startTag = "{" + tagname + ":";
        final int i = header.indexOf(startTag);
        if (i < 0) {
            // the tag is not present
            return header;
        } else {
            final String str1 = header.substring(0, i);
            final int j = header.indexOf('}', i);
            final String str2 = header.substring(j + 1);
            return removeTags(str1 + str2, tagname);
        }
    }

    /**
     * Add the textToAdd to the header behind to the first occurrence of
     * textToFind;
     *
     * @param header     HeaderBlock of message swift.
     * @param textToAdd  Text to add for header.
     * @param textToFind The textToAdd will be appended to textToFind in the header
     *                   String.
     * @return The header changed if the textToFind is in the header String,
     * else returns null.
     */
    private String addTextToHeader(final String header, final String textToAdd,
                                   final String textToFind) {
        final int index = header.indexOf(textToFind);
        if (index != -1) {
            final int addIndex = index + textToFind.length();
            final StringBuilder sb = new StringBuilder();
            // header+textToAdd+header
            sb.append(header.substring(0, addIndex));
            sb.append(textToAdd);
            sb.append(header.substring(addIndex, header.length()));
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * Add the textToAdd to the end of header
     *
     * @param header    Header of message swift.
     * @param textToAdd Text to add for header.
     * @return Return the new header with the textToAdd added.
     */
    private String addTextToHeaderOnFirstPosition(final String header,
                                                  final String textToAdd) {
        final StringBuilder sb = new StringBuilder();
        // {x:+textToAdd+header
        sb.append(header.substring(0, 3));
        sb.append(textToAdd);
        sb.append(header.substring(3, header.length()));
        return sb.toString();
    }

    /**
     * Add the textToAdd to the end of header
     *
     * @param header    Header of message swift.
     * @param textToAdd Text to add for header.
     * @return Return the new header with the textToAdd added.
     */
    private String addTextToHeaderOnEndPosition(final String header,
                                                final String textToAdd) {
        // header+textToAdd+}
        final StringBuilder sb = new StringBuilder();
        sb.append(header.substring(0, header.length() - 1));
        sb.append(textToAdd);
        sb.append(header.substring(header.length() - 1, header.length()));
        return sb.toString();
    }

    /**
     * Replace tag.
     *
     * @param header  the header
     * @param tagname the tagname
     * @param newTag  the new tag
     * @return the string
     */
    protected String replaceTag(final String header, final String tagname,
                                final String newTag) {
        final String startTag = "{" + tagname + ":";
        final int i = header.indexOf(startTag);
        if (i < 0) {
            // the tag is not present
            return header;
        } else {
            final String str1 = header.substring(0, i);
            final int j = header.indexOf('}', i);
            final String str2 = header.substring(j + 1);
            return str1 + newTag + str2;
        }
    }

    /**
     * Checks if this message is actually a valid MT103+. If it is not the
     * remove the {121:'uetr_info'} field.
     *
     * @param headerBlock  Contents of the {3:} header
     * @param swiftMessage The SWIFT message that contains the full text of the message
     * @param message      The BOMessage object
     * @return A String with the contents of the {3:} header fixed.
     */
    private String checkAndFixField121(final String headerBlock,
                                       final BOMessage message, final DSConnection dsCon) {
        String updatedHeader = headerBlock;
        String tagUetr121 = "";
        final String template = message.getTemplateName();

        if (!headerBlock.contains(TOKEN_121_UETR_INI)
                && (template103.equalsIgnoreCase(template)
                || template202.equalsIgnoreCase(template)
                || template202COV.equalsIgnoreCase(template))) {
            try {
                final BOTransfer transfer = dsCon.getRemoteBO().getBOTransfer(
                        message.getTransferLongId());
                if ((transfer != null) && (transfer.getLongId() != 0)) {
                    final String uetrAttr = transfer.getAttribute(KeywordConstantsUtil.TRANSFER_ATTRIBUTE_UETR);
                    if (!Util.isEmpty(uetrAttr)) {
                        tagUetr121 = TOKEN_121_UETR_INI + uetrAttr
                                + TOKEN_121_UETR_FIN;
                        updatedHeader = addTextToHeaderOnEndPosition(
                                headerBlock, tagUetr121);
                    }
                }
            } catch (final RemoteException e) {
                Log.error(
                        this,
                        "SwiftTextCustomizer.checkAndFixField121: RemoteException while getting the transfer:"
                                + message.getTransferLongId() + ".", e);
            }
        }

        return updatedHeader;
    }

    private boolean isTripartyMsg(BOMessage boMessage){

        if (Optional.ofNullable(boMessage).isPresent()){
            return boMessage.getTemplateName().equals("MT569") && boMessage.getMessageType().equals("INC_ALLOCATION");
        }
    return false;
    }

    private boolean isIncomingMsg(BOMessage boMessage){
        boolean res;
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","DEACTIVATE_BLOCK2_SWIFT");
        if(!Boolean.parseBoolean(activationFlag)){
            res=isIncRecon(boMessage)||isIncoming(boMessage)||isTripartyMsg(boMessage);
        }else{
            res=isTripartyMsg(boMessage);
        }
        return res;
    }


    private boolean isIncRecon(BOMessage boMessage){
        return Optional.ofNullable(boMessage)
                .map(BOMessage::getMessageType).map(t->t.equals("INC_RECON")).orElse(false);
    }

    private boolean isIncoming(BOMessage boMessage){
        return Optional.ofNullable(boMessage)
                .map(BOMessage::getMessageType).map(t->t.equals("INCOMING")).orElse(false);
    }

}
