package calypsox.tk.util;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.event.PSEventSantGDPosition;
import calypsox.tk.util.gdisponible.GDisponibleUtil;
import calypsox.tk.util.gdisponible.SantGDInvSecPosKey;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.SantPositionConstants;

/**
 * Clean GD Inventory
 * 
 * @author Juan Angel Torija
 * @version 1.0
 * @date 10/06/2015
 */
public class ScheduledTaskCleanGD extends AbstractProcessFeedScheduledTask {

	private static final long serialVersionUID = 123L;

	private static final String TASK_INFORMATION = "Clean GD Positions";
	private final boolean processOK = true;

	private static SimpleDateFormat SDF_SHORT = new SimpleDateFormat("dd/MM/yyyy");
	private static SimpleDateFormat SDF_LONG = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.713610");

	// log control
	protected final LogGeneric logGen = new LogGeneric();

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}
	
	//v14 Migration
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		
		return super.buildAttributeDefinition();	
	}


	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		//v14
		JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());

		HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> posDays = GDisponibleUtil
				.buildSecurityPositionsNbDays(39709, null, Arrays.asList("Bond", "BondAssetBack"),
						Arrays.asList("ACTUAL", "THEORETICAL"), valDate.getJDatetime(TimeZone.getDefault()), 1);

		List<SantGDInvSecPosKey> alreadySent = new ArrayList<SantGDInvSecPosKey>();

		Vector<PSEvent> psEvents = new Vector<PSEvent>();
		int events = 0;

		for (JDate date : posDays.keySet()) {
			HashMap<SantGDInvSecPosKey, InventorySecurityPosition> posDay = posDays.get(date);

			for (SantGDInvSecPosKey key : posDay.keySet()) {

				InventorySecurityPosition invSecPos = posDay.get(key);
				if (!alreadySent.contains(key)
						&& ((Math.abs(invSecPos.getTotalSecurity()) > 0.0000001) || (Math.abs(invSecPos
								.getTotalPledgedOut()) > 0.0000001)) && (invSecPos.getAgentId() != 1)
						&& (invSecPos.getAgentId() != 44196) && (invSecPos.getAccountId() != 1502)) {

					events++;

					PSEventSantGDPosition event = new PSEventSantGDPosition(buildGDExternalMessage(invSecPos, valDate));

					psEvents.add(event);

					alreadySent.add(key);

					if (events == 1000) {
						saveAndPublishEvents(psEvents);
						events = 0;
						psEvents.clear();
					}
				}

			}
		}
		if (events > 0) {
			saveAndPublishEvents(psEvents);
		}

		return this.processOK;
	}

	private void saveAndPublishEvents(Vector<PSEvent> psEvents) {
		try {
			DSConnection.getDefault().getRemoteTrade().saveAndPublish(psEvents);
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
		}
	}

	private String buildGDExternalMessage(InventorySecurityPosition invSecPos, JDate valDate) {
		StringBuffer externalMsg = new StringBuffer();
		try {
			// BLOQUEO
			addPositionZero(externalMsg, invSecPos, SantPositionConstants.BLOQUEO_MAPPING, valDate, 0);
			// ACTUAL
			addPositionZero(externalMsg, invSecPos, SantPositionConstants.ACTUAL_MAPPING, valDate, 0);
			// THEORETICAL
			addPositionZero(externalMsg, invSecPos, SantPositionConstants.THEORETICAL_MAPPING, valDate, 0);
		} catch (RemoteException e) {
			Log.error(ScheduledTaskCleanGD.class.getName(), e);
		}
		return externalMsg.toString();
	}

	private void addPositionZero(StringBuffer externalMsg, InventorySecurityPosition invSecPos, String positionType,
			JDate valDate, double totalSecurity) throws RemoteException {
		// 1 C?digo ISIN X(12) VARCHAR2(12)
		// 2 C?digo Divisa X(03) VARCHAR2(3)
		// 3 C?digo Portfolio X(15) VARCHAR2(32)
		// 4 Nemot?cnico Custodio X(06) VARCHAR2(32)
		// 5 C?digo Cuenta Custodio X(35) VARCHAR2(35)
		// 6 Estado X(07) VARCHAR2(32)
		// 7 N? de T?tulos S9(15) Amount (32)
		// 8 Fecha valor posici?n X(10) X(10)
		// 9 Timestamp envio X(26) X(26)
		// 10. Modo Env?o X(01) X(01)
		Product product = DSConnection.getDefault().getRemoteProduct().getProduct(invSecPos.getSecurityId());

		if (product != null) {
			// ISIN
			externalMsg.append(product.getSecCode("ISIN"));
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Currency
			externalMsg.append(product.getCurrency());
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Book
			externalMsg.append(BOCache.getBook(DSConnection.getDefault(), invSecPos.getBookId()));
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Agent
			externalMsg.append(BOCache.getLegalEntityCode(DSConnection.getDefault(), invSecPos.getAgentId()));
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Account
			externalMsg.append(BOCache.getAccount(DSConnection.getDefault(), invSecPos.getAccountId()).getName());
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Position Type
			externalMsg.append(positionType);
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Position value
			externalMsg.append(totalSecurity);
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Position date
			externalMsg.append(SDF_SHORT.format(valDate.getDate(TimeZone.getDefault())));
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Timestamp
			externalMsg.append(SDF_LONG.format(valDate.getDate(TimeZone.getDefault())));
			externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
			// Sending mode
			externalMsg.append("0");
			// End line
			externalMsg.append("\n");
		}
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

}