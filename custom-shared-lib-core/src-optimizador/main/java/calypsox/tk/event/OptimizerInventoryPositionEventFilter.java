package calypsox.tk.event;

import java.util.HashMap;
import java.util.Vector;

import calypsox.tk.util.gdisponible.GDisponibleUtil;
import calypsox.tk.util.gdisponible.SantGDBookInvSecPosKey;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class OptimizerInventoryPositionEventFilter implements EventFilter {

	private static final String INV_DUMMY_BOOK = "INV_DUMMY_BOOK";
	
	private static final Vector<String> TIME_SLEEP = LocalCache.getDomainValues(DSConnection.getDefault(), "OptimizerInventoryPositionEventFilterTimeSleep");

	public static HashMap<SantGDBookInvSecPosKey, SantGDBookInvSecPosKey> EVENTS_TO_SEND = new HashMap<SantGDBookInvSecPosKey, SantGDBookInvSecPosKey>();

	public boolean waitB = Boolean.FALSE;

	public OptimizerInventoryPositionEventFilter() {
	}

	public boolean accept(PSEvent psevent) {
		if (psevent instanceof PSEventInventorySecPosition) {
			PSEventInventorySecPosition psInvSecPos = (PSEventInventorySecPosition) psevent;
			InventorySecurityPosition invSecPos = psInvSecPos.getPosition();
			if (invSecPos == null) {
				return false;
			}
			Log.debug(OptimizerInventoryPositionEventFilter.class.getName(), psevent.toString());
			Vector<String> dummyBooks = LocalCache.getDomainValues(
					DSConnection.getDefault(), INV_DUMMY_BOOK);
			if (!Util.isEmpty(dummyBooks) && (invSecPos.getBook() != null)
					&& dummyBooks.contains(invSecPos.getBook().getName())) {
				Log.debug(OptimizerInventoryPositionEventFilter.class.getName(), "Skipping "+INV_DUMMY_BOOK);
				return false;
			}
			if (invSecPos.getPositionDate().before(JDate.getNow())
					|| invSecPos
							.getPositionDate()
							.after(JDate
									.getNow()
									.addBusinessDays(
											10,
											Util.string2Vector(GDisponibleUtil.SYSTEM_CAL)))) {
				Log.debug(OptimizerInventoryPositionEventFilter.class.getName(), "Skipping positionDate");
				return false;
			}
			if (!(InventorySecurityPosition.THEORETICAL_TYPE.equals(invSecPos
					.getPositionType()) && InventorySecurityPosition.SETTLE_DATE
					.equals(invSecPos.getDateType()))) {
				Log.debug(OptimizerInventoryPositionEventFilter.class.getName(), "Skipping PositionType or DateType");
				return false;
			}
			boolean returnB = Boolean.FALSE;
			// key by securityId, bookId
			SantGDBookInvSecPosKey key = new SantGDBookInvSecPosKey(
					invSecPos.getSecurityId(), "ALL", invSecPos.getBookId());
			synchronized (OptimizerInventoryPositionEventFilter.class.getName()) {
				if (EVENTS_TO_SEND.get(key) == null) {
					waitB = true;
					synchronized (OptimizerInventoryPositionEventFilter.class
							.getName()) {
						EVENTS_TO_SEND.put(key, key);
					}
				} else {
					returnB = true;
				}
			}
			if (returnB) {
				Log.system(
						OptimizerInventoryPositionEventFilter.class.getName(),
						"SYNCHRO RETURN FALSE: "+psevent.toString());
				return false;
			}
			if (waitB) {
				Sleeper sleeper = new Sleeper(key);
				sleeper.start();
			}
		}
		Log.system(OptimizerInventoryPositionEventFilter.class.getName(),
				"SYNCHRO RETURN TRUE: "+psevent.toString());
		return true;
	}

	public class Sleeper extends Thread {
		protected SantGDBookInvSecPosKey key = null;

		public Sleeper(SantGDBookInvSecPosKey key) {
			super();
			this.key = key;
		}

		@SuppressWarnings("static-access")
		public void run() {
			try {
				Log.system(
						OptimizerInventoryPositionEventFilter.class.getName(),
						"SLEEPING secs");
				if (!Util.isEmpty(TIME_SLEEP) && !Util.isEmpty(TIME_SLEEP.get(0))) {
					Thread.currentThread().sleep(Long.valueOf(TIME_SLEEP.get(0)));
				} else {
					Thread.currentThread().sleep(5000L);
				}

				synchronized (OptimizerInventoryPositionEventFilter.class
						.getName()) {
					EVENTS_TO_SEND.remove(key);
				}
				waitB = Boolean.FALSE;

			} catch (Exception e) {
				EVENTS_TO_SEND.remove(key);
				Log.error(
						OptimizerInventoryPositionEventFilter.class.getName(),
						e);
			}
		}
	}
}
