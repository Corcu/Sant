/**
 *
 */
package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.util.Vector;

/**
 * @author aela
 *
 */
public class SantApplyMCContactMessageRule implements WfMessageRule {

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfMessageRule#check(com.calypso.tk.bo. TaskWorkflowConfig,
     * com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade,
     * com.calypso.tk.bo.BOTransfer, java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector,
     * com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
     */
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfMessageRule#getDescription()
     */
    @Override
    public String getDescription() {
        return "This rule will set the message contact to the contact configured for the margin call contract";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfMessageRule#update(com.calypso.tk.bo. TaskWorkflowConfig,
     * com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade,
     * com.calypso.tk.bo.BOTransfer, java.util.Vector, com.calypso.tk.service.DSConnection, java.util.Vector,
     * com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
     */
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {
        LEContact contactToUse = null;
        // get the message receiver contacts list
        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(dsCon, message.getStatementId());

        if (mcc == null) {
            messages.add("No margin call contract found for message " + message.getLongId());
            return false;
        }

        // get the receiver contact from the message
        try {
            Vector receiverContacts = dsCon.getRemoteReferenceData().getLEContacts(message.getReceiverId());
            if (receiverContacts != null && receiverContacts.size() > 0) {
                for (int i = 0; i < receiverContacts.size(); i++) {
                    LEContact contact = (LEContact) receiverContacts.get(i);
                    String SDFname = contact.getStaticDataFilter();
                    StaticDataFilter contactSDF = BOCache.getStaticDataFilter(dsCon, SDFname);

                    if (contactSDF != null) {
                        StaticDataFilterElement contactElem = StaticDataFilter.find(contactSDF,
                                StaticDataFilterElement.TRADE_MARGIN_CALL_CONTRACT, StaticDataFilterElement.IN);
                        if (contactElem != null) {
                            if (contactElem.getValues().contains(mcc.getId())) {
                                contactToUse = contact;
                                break;
                            }
                        }
                    }

                    // if
                    // (!message.getReceiverContactType().equals(contact.getContactType()))
                    // continue;
                    //
                    // if
                    // (!message.getReceiverRole().equals(contact.getLegalEntityRole())
                    // && !LEContact.ALL.equals(contact.getLegalEntityRole()))
                    // continue;
                    //
                    // Vector contactProductList = contact.getProductTypeList();
                    //
                    // if (contactProductList != null &&
                    // !contactProductList.contains(message.getProductType())
                    // && !contactProductList.contains(LEContact.ALL))
                    // continue;
                    //
                    // if (contact.getEffectiveFrom() != null
                    // &&
                    // contact.getEffectiveFrom().after(message.getCreationDate().getJDate(TimeZone.getDefault())))
                    // continue;
                    //
                    // if (contact.getEffectiveTo() != null
                    // &&
                    // contact.getEffectiveTo().before(message.getCreationDate().getJDate(TimeZone.getDefault())))
                    // continue;
                    //
                    // int contactMCC = 0;
                    // try {
                    // contactMCC =
                    // Integer.parseInt(contact.getAddressCode("MarginCallContract"));
                    // } catch (Exception e) {
                    // Log.error(this, e);
                    // contactMCC = 0;
                    // }
                    //
                    // if (contactMCC == mcc.getId()) {
                    // contactToUse = contact;
                    // break;
                    // }
                }
            } else {
                messages.add("No contact defined for the legal entity " + message.getReceiverId());
                return false;
            }
            // ("MarginCallContract", "" + mcc.getId());
        } catch (RemoteException e) {
            Log.error(this, e);
            messages.add("Unable to get contact for message " + message.getLongId());
            return false;
        }

        if (contactToUse != null) {
            message.setReceiverContactId(contactToUse.getId());
            message.setReceiverAddressCode(contactToUse.getAddressCode(message.getAddressMethod()));
        }
        return true;
    }
}
