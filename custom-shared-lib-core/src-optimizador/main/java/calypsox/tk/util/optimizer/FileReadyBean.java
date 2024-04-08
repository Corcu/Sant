package calypsox.tk.util.optimizer;

import java.io.Serializable;
import java.util.TimeZone;

import calypsox.tk.interfaces.optimizer.fileready.FileReady;

import com.calypso.tk.core.JDatetime;

public class FileReadyBean implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6417499002238307906L;

	public FileReadyBean(String fileName, JDatetime timestamp, int nbRecords) {
		super();
		this.fileName = fileName;
		this.timeStamp = timestamp;
		this.nbRecords = nbRecords;
	}

	public FileReadyBean(FileReady fileReady) {
		super();
		this.fileName = fileReady.getFileName();
		this.timeStamp = fileReady.getTimeStamp() != null ? new JDatetime(
				FileReadyUtil.toDate(fileReady.getTimeStamp())) : null;
		this.nbRecords = fileReady.getNbRecords();
	}

	public FileReady toFileReady() {
		FileReady fr = new FileReady();
		fr.setFileName(this.fileName);
		if (this.timeStamp != null) {
			fr.setTimeStamp(FileReadyUtil.toXMLGregorianCalendar(this.timeStamp
					.getJDate(TimeZone.getDefault())));
		}
		fr.setNbRecords(this.nbRecords);
		return fr;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public JDatetime getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(JDatetime timestamp) {
		this.timeStamp = timestamp;
	}

	public int getNbRecords() {
		return nbRecords;
	}

	public void setNbRecords(int nbRecords) {
		this.nbRecords = nbRecords;
	}

	private String fileName = null;
	private JDatetime timeStamp = null;
	private int nbRecords = 0;

	/**
	 * Clones this object.
	 * 
	 * @return a copy of this object
	 */
	public FileReadyBean clone() throws CloneNotSupportedException {
		FileReadyBean fileReady = (FileReadyBean) super.clone();
		return fileReady;
	}
}
