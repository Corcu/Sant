package calypsox.tk.event;

import calypsox.tk.util.optimizer.FileReadyBean;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.event.PSEvent;

public class PSEventFileReady extends PSEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7764185682327940425L;
	
	private FileReadyBean fileReady = null;

	public PSEventFileReady(String fileName, JDatetime timestamp, int nbRecords) {
		super();
		fileReady = new FileReadyBean(fileName, timestamp, nbRecords);
	}

	public FileReadyBean getFileReady() {
		return fileReady;
	}

	public void setFileReady(FileReadyBean fileReady) {
		this.fileReady = fileReady;
	}
}
