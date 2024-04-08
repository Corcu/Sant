/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
package calypsox.engine.im.importim.output;

import com.calypso.tk.core.JDate;

public class SantInitialMarginImportImOutput {
	public final static String SEPARATOR = "|";

	private JDate processDate;
	private JDate valueDate;
	private String contractName;
	private int errorCode;
	private String errorDescription;

	
	public SantInitialMarginImportImOutput() {
	}

	public JDate getProcessDate() {
		return processDate;
	}

	public void setProcessDate(JDate processDate) {
		this.processDate = processDate;
	}

	public JDate getValueDate() {
		return valueDate;
	}

	public void setValueDate(JDate valueDate) {
		this.valueDate = valueDate;
	}

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public int getErrorCode() {
		return this.errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDescription() {
		return this.errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public String generateOutput() {
		StringBuilder infoFormatted = new StringBuilder();

		infoFormatted.append(this.processDate.toString());
		infoFormatted.append(SEPARATOR);

		infoFormatted.append(this.valueDate.toString());
		infoFormatted.append(SEPARATOR);
		
		infoFormatted.append(this.contractName);
		infoFormatted.append(SEPARATOR);

		infoFormatted.append(String.valueOf(this.errorCode));
		infoFormatted.append(SEPARATOR);

		infoFormatted.append(this.errorDescription);
		infoFormatted.append(SEPARATOR);

		return infoFormatted.toString();
	}
}
