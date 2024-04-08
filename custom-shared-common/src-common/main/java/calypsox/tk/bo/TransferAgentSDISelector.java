package calypsox.tk.bo;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Comparator;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

public class TransferAgentSDISelector extends com.calypso.tk.bo.TransferAgentSDISelector {

    @Override
    public Vector getValidSDIList(Trade trade, TradeTransferRule transfer, JDate settleDate, String legalEntity, String legalEntityRole, Vector exceptions, boolean includeNotPreferred, DSConnection dsCon) {
        Vector available = super.getValidSDIList(trade, transfer, settleDate, legalEntity, legalEntityRole, exceptions, includeNotPreferred, dsCon);
        if (Util.isEmpty(available)) {
            return available;
        } else {
            //returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second
            /*

The process for associating SDIs with a trade is automated based on the SDIs specified in the system and according to the search algorithm described below.

SDIs are selected for each type of cashflow associated with the trade. The system represents each type of cashflow with a trade transfer rule and assigns an SDI to each trade transfer rule.

Prior to assigning SDIs, the system generates all the cashflows and aggregates them by type, by direction (pay/receive), and by beneficiary in order to build the trade transfer rules. Then the SDIs are selected as described below.

•	Select the preferred SDIs
•	Select SDIs that are active on the settlement date (or trade date)
•	Select SDIs that satisfy the static data filter if any
•	Order the selected SDIs based on their priority
•	Then match the best SDI for the trade counterparty with the following criteria in the specified order:
–	Beneficiary role and Beneficiary
–	Processing org, or ALL
–	Cash / Security, or BOTH
–	Pay / Receive, or BOTH
–	Product types, or ANY
–	Currencies, or ANY
If no SDI is found for the trade counterparty (and only if no SDI is found for the trade counterparty), the system searches SDIs for its parent if any, or for the beneficiary “ALL” (see “ALL” Beneficiary below for details), using the same algorithm.

•	Then match the best SDI for the processing org with the following criteria in the specified order:
–	Settlement method of the selected counterparty’s SDI
–	Product types, or ANY
–	Currencies, or ANY
If no SDI is found for the same settlement method, the system investigates the possible routes using the processing organization agent, and then the route of the trade counterparty agent.

If the MATCH attribute is set to true, a match can only occur if the processing org agent is the same as the trade counterparty agent.

You can set the SDI attribute MATCH_AGENT to true on the counterparty to force the SDI selection based on the same agent between counterparty and processing org, without checking any Intermediary.



If multiple SDIs are found for the trade counterparty because they have the same keys, the system will indicate that it cannot select the SDIs. You have to modify the priority for example, so that the system can select an SDI, or the SDIs have to be manually assigned.

For the processing org, if multiple SDIs are found because they have the same keys, the system will select the first SDI found by default. If the number of products is different however, the system will indicate that it cannot select the SDIs. To prevent a blocking message when the list of products is different, you need to set the environment property SDI_CHECK_PRODUCT_SIZE = false. In this case, the SDI with the lowest ID is selected (It is true by default). You can also elect to apply the same behavior as for the counterparty by setting the SDI attribute "UseAgentAsKey = false" for ALL SDIs that have the same keys. In that case, the system will indicate that it cannot select SDIs.
*/
            available.sort(
                    Comparator.comparingInt((SettleDeliveryInstruction sdi) -> (sdi.getPreferredB() ? 0 : 1))
                            .thenComparingInt(SettleDeliveryInstruction::getPriority)
                            .thenComparingInt(sdi -> (sdi.getProcessingOrgBasedId() == 0 ? 1 : 0))
                            .thenComparingInt(sdi -> (sdi.getType() == 0 ? 1 : 0))
                            .thenComparingInt(sdi -> (Util.isEmpty(sdi.getProductList()) ? 1 : 0))
                            .thenComparingInt(sdi -> (Util.isEmpty(sdi.getCurrencyList()) ? 1 : 0)));
        }

        return available;
    }
}

