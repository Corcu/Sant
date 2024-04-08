package calypsox.tk.util.bean;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class FeedFileInfoBean implements Serializable {

	private static final long serialVersionUID = 7007351893006930178L;
	// variables
	private String process;
	private Integer processingOrg;
	private Timestamp startTime;
	private Timestamp endTime;
	private Date processDate;
	private String fileImported;
	private String inout;
	private String result;
	private Integer numberOk;
	private Integer numberWarning;
	private Integer numberError;
	private String originalFile;
	private String comments;

	// constructor for retrieve data
	public FeedFileInfoBean() {

	}

	// constructor for insert data
	public FeedFileInfoBean(String process, Integer processingOrg, Timestamp startTime, Timestamp endTime,
			Date processDate, String fileImported, String inout, String result, Integer numberOk,
			Integer numberWarning, Integer numberError, String originalFile, String comments) {

		setProcess(process);
		setProcessingOrg(processingOrg);
		setStartTime(startTime);
		setEndTime(endTime);
		setProcessDate(processDate);
		setFileImported(fileImported);
		setInout(inout);
		setResult(result);
		setNumberOk(numberOk);
		setNumberWarning(numberWarning);
		setNumberError(numberError);
		setOriginalFile(originalFile);
		setComments(comments);

	}

	// getters and setters
	public String getProcess() {
		return this.process;
	}

	public void setProcess(String process) {
		this.process = process;
	}

	public Integer getProcessingOrg() {
		return this.processingOrg;
	}

	public void setProcessingOrg(Integer processingOrg) {
		this.processingOrg = processingOrg;
	}

	public Timestamp getStartTime() {
		return this.startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public Timestamp getEndTime() {
		return this.endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public Date getProcessDate() {
		return this.processDate;
	}

	public void setProcessDate(Date processDate) {
		this.processDate = processDate;
	}

	public String getFileImported() {
		return this.fileImported;
	}

	public void setFileImported(String fileImported) {
		this.fileImported = fileImported;
	}

	public String getInout() {
		return this.inout;
	}

	public void setInout(String inout) {
		this.inout = inout;
	}

	public String getResult() {
		return this.result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Integer getNumberOk() {
		return this.numberOk;
	}

	public void setNumberOk(Integer numberOk) {
		this.numberOk = numberOk;
	}

	public Integer getNumberWarning() {
		return this.numberWarning;
	}

	public void setNumberWarning(Integer numberWarning) {
		this.numberWarning = numberWarning;
	}

	public Integer getNumberError() {
		return this.numberError;
	}

	public void setNumberError(Integer numberError) {
		this.numberError = numberError;
	}

	public String getOriginalFile() {
		return this.originalFile;
	}

	public void setOriginalFile(String originalFile) {
		this.originalFile = originalFile;
	}

	public String getComments() {
		return this.comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

}
