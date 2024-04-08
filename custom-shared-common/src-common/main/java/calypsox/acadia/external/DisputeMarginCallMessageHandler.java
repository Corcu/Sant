package calypsox.acadia.external;

import com.calypso.acadia.model.ACADIAMarginCall;
import com.calypso.tk.bo.ACADIAResponseMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

public class DisputeMarginCallMessageHandler extends com.calypso.acadia.external.DisputeMarginCallMessageHandler {

    public boolean handleExternalMessage(ExternalMessage externalMessage, PricingEnv env, PSEvent event, String engineName, DSConnection ds, Object dbCon, boolean publishedTask, boolean reprocessMessage) throws MessageParseException {
        ACADIAMarginCall acadiaMarginCall = (ACADIAMarginCall)((ACADIAMarginCall)((ACADIAResponseMessage)externalMessage).getUnmarshalObject());// 47 48

        try {
            MarginCallEntryDTO marginCallEntryDTO = this.getMarginCallEntryDTO(acadiaMarginCall);
            if (marginCallEntryDTO == null) {
                return false;
            } else {
                CollateralConfig config = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralDataServer().getMarginCallConfig(marginCallEntryDTO.getCollateralConfigId());
                int sign = marginCallEntryDTO.getGlobalRequiredMargin() < 0.0D ? -1 : 1;
                marginCallEntryDTO.setDispute(true);
                marginCallEntryDTO.setDisputeAmount(acadiaMarginCall.getDisputedAmount());
                marginCallEntryDTO.setDisputeStatus("Fully disputed");// 66
                double cptyMTM = acadiaMarginCall.getMarkToMarket() + (null != acadiaMarginCall.getInitialExposure() ? acadiaMarginCall.getInitialExposure() : 0.0D);
                marginCallEntryDTO.setCptyMarkToMarket(-1.0D * cptyMTM);
                marginCallEntryDTO.setDisputeMTMAmount(marginCallEntryDTO.getTradeMargin() - marginCallEntryDTO.getCptyMarkToMarket());
                marginCallEntryDTO.setCptyDeliverAmount((double)sign * acadiaMarginCall.getDeliverAmount());
                marginCallEntryDTO.setCptyReturnAmount((double)sign * acadiaMarginCall.getReturnAmount());
                this.updateAcadiaMCEntry(acadiaMarginCall, marginCallEntryDTO, config);
                if (this.checkVersionForMessaging(acadiaMarginCall)) {
                    this.saveMCEntry(marginCallEntryDTO, this.getAction(marginCallEntryDTO), config.getValuationTimeZone(), acadiaMarginCall);
                }

                return true;
            }
        } catch (CollateralServiceException var15) {
            Log.error(this.getClass(), var15);
            if (acadiaMarginCall != null) {
                acadiaMarginCall.addError(var15.getMessage());
            }

            return false;// 93
        } catch (NumberFormatException var16) {
            Log.error(this, var16);
            if (acadiaMarginCall != null) {
                acadiaMarginCall.addError(var16.getMessage());
            }

            return false;
        }
    }
    private String getAction(MarginCallEntryDTO marginCallEntryDTO) {
        return "fully disputed".equals(marginCallEntryDTO.getProcessingType()) ? "FULLY_DISPUTE" : "DISPUTE";
    }
}
