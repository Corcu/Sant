package calypsox.tk.swift.formatter;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class MT527JPMorganCollateralSWIFTFormatter
        extends com.calypso.tk.bo.swift.MT527JPMorganCollateralSWIFTFormatter {

    private static final String EXPOSURE_TYPE_VALUE = "TRIPARTY_MT527_EXPOSURE_TYPE_INDICATOR";
    private static final String EXPOSURE_TYPE_VALUE_JPM = ":COLA/SLEB";

    public String parseEXPOSURE_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String exposureTypeValue = CustomMT527CollateralSWIFTFormatter.getExpTypeValue(message);
        if (!com.calypso.tk.core.Util.isEmpty(exposureTypeValue)) {
            return exposureTypeValue;
        }

        return EXPOSURE_TYPE_VALUE_JPM;
    }

    public String parsePRINCIPAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {

        String value = CustomMT527CollateralSWIFTFormatter.getInstance().parsePRINCIPAL_AMOUNT(message, trade, sender,
                rec, transferRules, transfer, format, con);

        // TODO To change whent calypso HD delivery
        if (Util.isEmpty(value)) {
			value = super.parsePRINCIPAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, format, con);
//           value = customParsePRINCIPAL_AMOUNT(message, trade, sender, rec, transferRules, transfer, format, con);
        }

        value = checkExposureTypeIndicator(message,value);
        return value;
    }

/*    public String customParsePRINCIPAL_AMOUNT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        MarginCallEntryDTO dto = this.getMarginCallEntryDTO(message, con);
        return dto == null ? "" : ":TRAA//" + SwiftUtil.getCurrencySwiftAmount(dto.getGlobalRequiredMargin(), dto.getContractCurrency());
    }

*/

    public String checkExposureTypeIndicator(BOMessage message, String value){
        if (message!=null) {
            if (CustomMT527CollateralSWIFTFormatter.getMCContract(message) != null) {
                String exposureType = CustomMT527CollateralSWIFTFormatter.getMCContract(message).getAdditionalField(EXPOSURE_TYPE_VALUE);
                if (Util.isEmpty(exposureType) || exposureType.contains("SLEB")) {
                    value = value.replaceAll("TRAA", "VASO");

                } else {
                    value = value.replaceAll("VASO", "TRAA");
                }
            }
        }
        return value;
    }
}
