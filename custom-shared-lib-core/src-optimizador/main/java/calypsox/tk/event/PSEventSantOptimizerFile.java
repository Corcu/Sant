package calypsox.tk.event;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.calypso.tk.core.JDate;
import com.calypso.tk.event.PSEvent;

public class PSEventSantOptimizerFile extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3160443912252635920L;

	public static SimpleDateFormat SDF = new SimpleDateFormat(
			"yyyy-MM-dd-HH.mm.ss");

	public PSEventSantOptimizerFile(String name, int nbRecords) {
		super();
		this.name = name;
		this.nbRecords = nbRecords;
		this.timestamp = SDF.format(JDate.getNow().getDate(
				TimeZone.getDefault()));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNbRecords() {
		return nbRecords;
	}

	public void setNbRecords(int nbRecords) {
		this.nbRecords = nbRecords;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	protected String name = null;
	protected int nbRecords = 0;
	protected String timestamp = null;
}
