package calypsox.tk.event;


import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.refdata.DomainValues;
import java.util.List;


public class PaymentsHubSenderEventFilter implements EventFilter {


    private static final String DN_PH_REJECTED_TEMPLATE_NAMES = "PaymentsHub.rejectedTemplateNames";
    private static final String PAYMENTHUB_PAYMENTMSG_TYPE = "PAYMENTHUB_PAYMENTMSG";
    private static final String PAYMENTHUB_RECEIPTMSG_TYPE = "PAYMENTHUB_RECEIPTMSG";
    private static final String TO_BE_SENT_STATUS = "TO_BE_SENT";
    private static final String MESSAGE_PH_FICTCOV = "PH-FICTCOV";


    @Override
    public boolean accept(PSEvent paramPSEvent) {
        boolean accept = false;
        if (paramPSEvent instanceof PSEventMessage) {
            final PSEventMessage event = (PSEventMessage) paramPSEvent;
            // Get the message type
            final String msgType = event.getMessageType();
            // Get the message status
            final String msgStatus = event.getMessageStatus().getStatus();
            // Check if the template name is accepted.
            final boolean isAcceptedTemplateName = isAcceptedTemplateName(event.getBoMessage().getTemplateName());
            // Check if MessageType is PAYMENTHUB_PAYMENTMSG or PAYMENTHUB_RECEIPTMSG
            if (!Util.isEmpty(msgType) && !Util.isEmpty(msgStatus) && TO_BE_SENT_STATUS.equals(msgStatus) && isAcceptedTemplateName &&
                    (PAYMENTHUB_PAYMENTMSG_TYPE.equals(msgType) || PAYMENTHUB_RECEIPTMSG_TYPE.equals(msgType))) {
                accept = true;
            }
        }
        return accept;
    }


    /**
     * Checks if the Template Name is accepted by the EventFilter.
     *
     * @param templateName
     * @return
     */
    private boolean isAcceptedTemplateName(final String templateName) {
        boolean isAccepted = false;
        // Get Rejected Template Names from DomainValues
        final List<String> rejectedTemplateNames = DomainValues.values(DN_PH_REJECTED_TEMPLATE_NAMES);
        if (!Util.isEmpty(rejectedTemplateNames)) {
            isAccepted = !(rejectedTemplateNames.contains(templateName));
        } else {
            // By Default - Accepted all templateNames except PH-FICTCOV
            isAccepted = !(MESSAGE_PH_FICTCOV.equalsIgnoreCase(templateName));
        }
        return isAccepted;
    }


}
