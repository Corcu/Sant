package calypsox.tk.swift.formatter;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.MT527CollateralSWIFTFormatter;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * Contains the code used to customize several fields for Triparty messages.
 */
public class CustomMT527CollateralSWIFTFormatter extends MT527CollateralSWIFTFormatter {

    private static final String EXPOSURE_TYPE_VALUE = ":COLA//OTCD";

    private static final String ATTRIBUTE_TRIPARTY_AGREED_AMOUNT = "Triparty Agreed Amount";
    private static final String PRINCIPAL_AMOUNT_FIELD_TAG = ":TRAA//";
    private static final String CONTRACT_ADF = "TRIPARTY_MT527_EXPOSURE_TYPE_INDICATOR";
    private static final String EVENT_TYPE = "TRIPARTY_CANCELNEW_COLLATERAL";
    private static final String CANC_EVENT_TYPE = "TRIPARTY_AGREED_CANC_COLLATERAL";
    private static final String ENTRY_ATTRIBUTE = "CANCELNEW";

    // we get the contract
    public static CollateralConfig getMCContract(BOMessage message){
        if (null != message) {
            String attribute = message.getAttribute("marginCallConfigId");
            if (null != attribute) {
                return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        Integer.parseInt(attribute));
            }
        }
        return null;
    }

    // check value on contract
    public static String getExpTypeValue(BOMessage message) {
        CollateralConfig contract = getMCContract(message);
        if (null != contract) {
            if (!Util.isEmpty(contract.getAdditionalField(CONTRACT_ADF))) {
                        return contract.getAdditionalField(CONTRACT_ADF);
                    }
                }
        Log.error(CustomMT527CollateralSWIFTFormatter.class, "Could not load contract: " + message.getAttribute("marginCallConfigId"));
        return "";
    }

    public static synchronized CustomMT527CollateralSWIFTFormatter getInstance() {
        return new CustomMT527CollateralSWIFTFormatter();
    }

    public String parseEXPOSURE_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        // check contract first
        String exposureTypeValue = getExpTypeValue(message);
        if (!Util.isEmpty(exposureTypeValue)) {
            return exposureTypeValue;
        }

        return EXPOSURE_TYPE_VALUE;
    }

    public String parsePRINCIPAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        String returnedValue = "";

        MarginCallEntryDTO dto = getMarginCallEntryDTO(message, con);
        if (dto == null) {
            Log.error(this, String.format("Could not find Margin Call Entry for message id %d", message.getLongId()));
        } else {
            Object rawAttribute = dto.getAttribute(ATTRIBUTE_TRIPARTY_AGREED_AMOUNT);
            Double tripartyAgreedAmount = 0.0;
            if (rawAttribute instanceof Double) {
                tripartyAgreedAmount = (Double) rawAttribute;
            } else if (rawAttribute instanceof String) {
                String tripartyAgreedAmountString = (String) rawAttribute;
                try {
                    tripartyAgreedAmount = Double.valueOf(tripartyAgreedAmountString);
                } catch (NumberFormatException e) {
                    Log.error(this, String.format(
                            "Cannot convert attribute \"%s\" with value \"%s\" to a Double. Message id: %d, MarginCallEntry id: %d",
                            ATTRIBUTE_TRIPARTY_AGREED_AMOUNT, tripartyAgreedAmountString, message.getLongId(), dto.getId()),
                            e);
                }
            }

            if (tripartyAgreedAmount == null || tripartyAgreedAmount == 0.0) {
                returnedValue = "";
            } else {
                StringBuilder returnedValueBuilder = new StringBuilder();
                returnedValueBuilder.append(PRINCIPAL_AMOUNT_FIELD_TAG);
                returnedValueBuilder
                        .append(SwiftUtil.getCurrencySwiftAmount(tripartyAgreedAmount, dto.getContractCurrency()));

                returnedValue = returnedValueBuilder.toString();
            }
        }

        return returnedValue;
    }

    public String parseMESSAGE_SUBACTION(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (message != null && message.getEventType().equals(EVENT_TYPE)) {
            return "CANC";
        }
        return null;
    }

    @Override
    public String parseINSTRUCTION_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        if (message != null && message.getEventType().equals(CANC_EVENT_TYPE)) {
            int cancelMessageID = getLastCancelMessageID(message);
            return getSwfitFieldType(cancelMessageID, ":22H:", "CINT//", dsCon);
        }

        return null;
    }

    public String parseCLIENT_TRADE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                       Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (message != null && message.getEventType().equals(EVENT_TYPE)) {
            int cancelMessageID = getLastCancelMessageID(message);
            return getSwfitFieldType(cancelMessageID, ":20C:", "SCTR//", dsCon);
        }

        return null;
    }

    public String parseSEND_TRADE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (message != null && message.getEventType().equals(EVENT_TYPE)) {
            int cancelMessageID = getLastCancelMessageID(message);
            return getSwfitFieldType(cancelMessageID, ":20C:", "SCTR//", dsCon);
        }

        return null;
    }

    /**
     * @param message
     * @return Integer
     */
    private Integer getLastCancelMessageID(BOMessage message) {
        int entryid = getEntryId(message);
        if (entryid != 0) {
            try {
                Log.info(this, "Loading entry: " + entryid + " from message: " + message.getLongId());
                MarginCallEntryDTO entry = ServiceRegistry.getDefault().getCollateralServer().loadEntry(entryid);
                /*
                 * load CANCELNEW attribute form entry to get the message Instruction of the
                 * CANCELED message.
                 */
                if (entry != null && entry.getAttribute(ENTRY_ATTRIBUTE) != null) {

                    return (Integer) entry.getAttribute(ENTRY_ATTRIBUTE);
                }

            } catch (RemoteException | NumberFormatException e) {
                Log.error(this, "Error extracting the id of the last CANCELED message: " + e);
            }
        }
        return 0;
    }

    /**
     * @param messageID
     * @param tag
     * @param val
     * @param dsCon
     * @return String - Instruction type from swift message
     */
    private String getSwfitFieldType(Integer messageID, String tag, String val, DSConnection dsCon) {
        try {
            BOMessage message = dsCon.getRemoteBO().getMessage(messageID);
            if (message != null) {
                final Vector advicesDocuments = dsCon.getRemoteBackOffice()
                        .getAdviceDocuments("advice_document.advice_id=" + message.getLongId(), null, null);

                if (!Util.isEmpty(advicesDocuments) && advicesDocuments.get(0) instanceof AdviceDocument) {
                    AdviceDocument swiftDocument = (AdviceDocument) advicesDocuments.get(0);
                    StringBuffer buff = swiftDocument.getDocument();
                    SwiftMessage oldSwift = new SwiftMessage(message);

                    if (oldSwift.parseSwiftText(buff.toString(), message.getGateway())) {
                        Vector v = oldSwift.getFields();

                        for (int i = 0; i < v.size(); i++) {
                            SwiftFieldMessage fi = (SwiftFieldMessage) v.get(i);
                            if (fi.getTAG().equals(tag) && fi.getValue().contains(val)) {
                                return fi.getValue();
                            }
                        }
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot load message with ID: " + messageID);
        }

        return "";
    }

    private int getEntryId(BOMessage message) {
        Integer entryid = 0;
        if (!Util.isEmpty(message.getAttribute("marginCallEntryId"))) {
            try {
                entryid = Integer.parseInt(message.getAttribute("marginCallEntryId"));
            } catch (NumberFormatException e) {
                Log.error(this, "Cannot parse marginCallEntryId from message: " + message.getLongId(), e);
            }
        }
        return entryid;
    }

}
