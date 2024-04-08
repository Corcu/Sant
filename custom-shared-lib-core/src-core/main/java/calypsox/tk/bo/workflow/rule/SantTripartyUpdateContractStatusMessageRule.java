package calypsox.tk.bo.workflow.rule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import calypsox.util.collateral.CollateralManagerUtil;

/**
 * @author ACD
 * 
 */
public class SantTripartyUpdateContractStatusMessageRule implements WfMessageRule {
	private static final String TRIPARTY_TRANSACTION_GROUP_REFERENCE = "TripartyTransactionGroupReference";
	private static final String CONTRACT_PREV_STATUS = "TRIPARTY_AGREED";
	private static final String CONTRACT_PREV_STATUS2 = "TRIPARTY_AGREED_CANC";
	private static final String CONTRACT_ACTION = "TRIPARTY_EXECUTED";

	@Override
	public boolean check(TaskWorkflowConfig arg0, BOMessage arg1, BOMessage arg2, Trade arg3, BOTransfer arg4,
			Vector arg5, DSConnection arg6, Vector arg7, Task arg8, Object arg9, Vector arg10) {

		return true;
	}

	@Override
	public String getDescription() {

		return "Update Contract Status to TRIPARTY EXECUTED";
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection ds, Vector excps, Task task, Object dbCon,
			Vector events) {

		if (message != null && !Util.isEmpty(message.getAttribute(TRIPARTY_TRANSACTION_GROUP_REFERENCE))
				&& message.getAttribute(TRIPARTY_TRANSACTION_GROUP_REFERENCE).contains("--")) {
			List<Integer> contract = new ArrayList<>();
			// msg 0 Contract id
			// msg 1 Message id
			String[] msg = message.getAttribute(TRIPARTY_TRANSACTION_GROUP_REFERENCE).split("--");
			try {
				contract.add(decodeInteger(msg[0]));
				JDate proccesDate = getSwiftDate(message.getAttribute("CustomPD"));
				// get Entry from Contractid + ProccesDate
				
				List<MarginCallEntryDTO> entry = CollateralManagerUtil.loadMarginCallEntriesDTO(contract, proccesDate, true);

				if (!Util.isEmpty(entry) && null != entry.get(0)
						&& (CONTRACT_PREV_STATUS.equals(entry.get(0).getStatus())
								|| CONTRACT_PREV_STATUS2.equals(entry.get(0).getStatus())
								|| CONTRACT_ACTION.equals(entry.get(0).getStatus()))) {
					MarginCallEntryDTO mccEntryDTO = ServiceRegistry.getDefault(ds).getCollateralServer().saveWithReturn(entry.get(0), CONTRACT_ACTION,
							TimeZone.getDefault(),true);
					messages.add("Update entry: " + mccEntryDTO.getId() + " to " + CONTRACT_ACTION + " for contract " + contract.get(0)
							+ " on " + proccesDate);
					return true;
				}

			} catch (Exception e) {
				Log.error(this, "Cannot update entry to TRIPARTY EXECUTED Error: " + e);
				messages.add("Cannot update entry to TRIPARTY EXECUTED");
				return false;
			}
		}
		return true;
	}

	public static int decodeInteger(String integer) {
		if (!Util.isEmpty(integer)) {
			return Integer.parseInt(integer, 36);
		}
		return 0;
	}

	public JDate getSwiftDate(String date) {
		JDate jdate = null;
		if (date != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			try {
				jdate = JDate.valueOf(formatter.parse(date));
			} catch (ParseException e) {
				Log.error(this, "Cannot format SwfitDate");
			}
		}
		if (jdate == null) {
			jdate = JDate.getNow();
		}
		return jdate;
	}

	private JDate getDate(StringBuffer buf) throws ParseException {
		// :98C::PREP//2017011905112447
		JDate date = null;
		if (buf != null) {
			String[] lines = buf.toString().split("\\n");
			for (String line : lines) {
				if (line.contains(":98E::PREP//")) {
					String[] values = line.split("//");
					if (values[1].length() >= 8) {
						String tagdate = values[1].substring(0, 8);
						SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
						date = JDate.valueOf(formatter.parse(tagdate));
					}
				}
			}
		}
		return date;
	}

}
