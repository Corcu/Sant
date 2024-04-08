package calypsox.acadia.external;

import com.calypso.acadia.model.ACADIAMarginCall;
import com.calypso.acadia.util.AMPCodeUtil;
import com.calypso.infra.util.Util;
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

public class PartialDisputedMarginCallMessageHandler extends com.calypso.acadia.external.PartialDisputedMarginCallMessageHandler {


    public boolean handleExternalMessage(ExternalMessage externalMessage, PricingEnv env, PSEvent event, String engineName, DSConnection ds, Object dbCon, boolean publishedTask, boolean reprocessMessage) throws MessageParseException {
        ACADIAMarginCall acadiaMarginCall = (ACADIAMarginCall)((ACADIAMarginCall)((ACADIAResponseMessage)externalMessage).getUnmarshalObject());

        try {
            MarginCallEntryDTO marginCallEntryDTO = this.getMarginCallEntryDTO(acadiaMarginCall);
            if (marginCallEntryDTO == null) {
                return false;
            } else {
                String msg = AMPCodeUtil.getReasonCode(Integer.valueOf(acadiaMarginCall.getDisputeReasonCode()));
                CollateralConfig config = ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralDataServer().getMarginCallConfig(marginCallEntryDTO.getCollateralConfigId());
                if (!Util.isEmpty(msg)) {
                    marginCallEntryDTO.setDisputeReason(msg);
                } else {
                    marginCallEntryDTO.setDisputeReason(acadiaMarginCall.getDisputeReasonCode());
                }

                double cptyMTM = acadiaMarginCall.getMarkToMarket() + (null != acadiaMarginCall.getInitialExposure() ? acadiaMarginCall.getInitialExposure() : 0.0D);
                marginCallEntryDTO.setCptyMarkToMarket(-1.0D * cptyMTM);
                marginCallEntryDTO.setDisputeMTMAmount(marginCallEntryDTO.getTradeMargin() - marginCallEntryDTO.getCptyMarkToMarket());
                int sign = marginCallEntryDTO.getGlobalRequiredMargin() < 0.0D ? -1 : 1;
                if (acadiaMarginCall.getDirection().equals("Incoming")) {
                    marginCallEntryDTO.setCptyAmount(this.getExpectedAgreedAmount(marginCallEntryDTO, acadiaMarginCall));
                    marginCallEntryDTO.setAgreedDisputeAmount(Math.abs(acadiaMarginCall.getAgreedAmount()));
                    marginCallEntryDTO.setCptyDeliverAmount((double)sign * acadiaMarginCall.getDeliverAmount());
                    marginCallEntryDTO.setCptyReturnAmount((double)sign * acadiaMarginCall.getReturnAmount());
                } else {
                    marginCallEntryDTO.setCptyAmount(this.getAgreedAmount(marginCallEntryDTO, acadiaMarginCall));
                    marginCallEntryDTO.setAgreedDisputeAmount(this.getAgreedAmount(marginCallEntryDTO, acadiaMarginCall));
                    marginCallEntryDTO.setCptyDeliverAmount((double)sign * acadiaMarginCall.getAgreedNewMargin());
                    marginCallEntryDTO.setCptyReturnAmount((double)sign * acadiaMarginCall.getAgreedReturnMargin());
                }

                marginCallEntryDTO.setDisputeAmount(acadiaMarginCall.getDisputedAmount());
                marginCallEntryDTO.setDispute(true);
                marginCallEntryDTO.setDisputeStatus("Partially agreed");
                marginCallEntryDTO.setCptyTotalPrevMrg(-1.0D * acadiaMarginCall.getCptyTotalPrevMrg());
                boolean acceptUndisputedAmt = config.acceptUndisputedAmount();
                if (acceptUndisputedAmt) {
                    marginCallEntryDTO.setGlobalRequiredMargin(this.getAgreedAmount(marginCallEntryDTO, acadiaMarginCall));
                }

                if (config.isAcadiaTriparty() && acceptUndisputedAmt) {
                    marginCallEntryDTO.setRqv(marginCallEntryDTO.getPreviousRQV() + marginCallEntryDTO.getGlobalRequiredMargin());
                }

                this.updateAcadiaMCEntry(acadiaMarginCall, marginCallEntryDTO, config);
                if (this.checkVersionForMessaging(acadiaMarginCall)) {
                    this.saveMCEntry(marginCallEntryDTO, "UPDATE", config.getValuationTimeZone(), acadiaMarginCall);
                }

                return true;
            }
        } catch (NumberFormatException | CollateralServiceException var17) {
            Log.error(this, var17);
            acadiaMarginCall.addError(var17.getMessage());
            return false;
        }
    }

}
