/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
package calypsox.engine.im.export.input;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.LocalCache;

import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;
import calypsox.engine.im.errorcodes.SantInitialMarginQefErrorCodeEnum;

/**
 * Class to parse message received into object
 * 
 * @author xIS15793
 *
 */
public class SantInitialMarginExportInput {
	/** number of fields expected */
	private static final int NUM_FIELDS_EXPECTED = 4;

	/** columns separator */
	public final static String SEPARATOR = "\\|";
	
	/** error code */
	private JDate processDate;

	/** error description */
	private JDate valueDate;
	
	/** contract id */
	private String contractName;

	/** error code */
	private int errorCode;

	/** error description */
	private String errorMessage;

	/**
	 * Constructor
	 * 
	 * @param msgReceived
	 */
	public SantInitialMarginExportInput() {
		this.processDate = JDate.getNow();
		this.valueDate = JDate.getNow().addBusinessDays(-1, LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
		this.contractName = "";
		this.errorCode = 0;
		this.errorMessage = "";
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


	/**
	 * getter for contract id value
	 * 
	 * @return contract id
	 */
	public String getContractName() {
		return this.contractName;
	}

	/**
	 * Setter for contract id value
	 * 
	 * @param contractName
	 *            contract Name
	 */
	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	/**
	 * Getter for Error code value
	 * 
	 * @return error code value
	 */
	public int getErrorCode() {
		return this.errorCode;
	}

	/**
	 * Setter for Error code value
	 * 
	 * @param error
	 *            code value
	 */
	public void getErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * Getter for Error Description value
	 * 
	 * @return error description
	 */
	public String getErrorDescription() {
		return this.errorMessage;
	}

	/**
	 * Setter for Error Description value
	 * 
	 * @param errorDescription
	 *            description
	 */
	public void setErrorDescription(String errorDescription) {
		this.errorMessage = errorDescription;
	}

	/**
	 * Parse info received into this object
	 * 
	 * @param msgReceived
	 *            msg received through the queue
	 * @return SantInitialMarginErrorCodeEnum
	 */
	public SantInitialMarginCalypsoErrorCodeEnum parseInfo(ExternalMessage msgReceived) {
		SantInitialMarginCalypsoErrorCodeEnum code = SantInitialMarginCalypsoErrorCodeEnum.NoError;

		String msg = msgReceived.getText();
		String[] msgSplit = msg.split(SEPARATOR);

		try {
			this.contractName = msgSplit[2].toString();
		} catch (NumberFormatException ex) {
			code = SantInitialMarginCalypsoErrorCodeEnum.ContractNotValid;
		} catch (Exception ex){
			code = SantInitialMarginCalypsoErrorCodeEnum.ContractNotValid;
		}

		
		if (msgSplit.length >= NUM_FIELDS_EXPECTED) {
			
			if (msgSplit.length == NUM_FIELDS_EXPECTED) {
				
				if (!Util.isNumber(msgSplit[3].toString())){
					code = SantInitialMarginCalypsoErrorCodeEnum.MessageNotValid;
					return code;
				}
				if (Integer.parseInt(msgSplit[3]) != 0){
					code = SantInitialMarginCalypsoErrorCodeEnum.MessageNotValid;
					return code;
				}
				
			}
		} else{
			code = SantInitialMarginCalypsoErrorCodeEnum.MessageNotValid;
			return code;
		}
		
		// no errors yet
		if (code.getCode() == 0) {
			try {
				this.processDate = JDate.valueOf(msgSplit[0]);
				this.valueDate = JDate.valueOf(msgSplit[1]);
			} catch (Exception ex) {
				code = SantInitialMarginCalypsoErrorCodeEnum.FormatDateNotValid;
			}
		}

		// no errors yet
		if (code.getCode() == 0) {
			try {
				this.errorCode = Integer.parseInt(msgSplit[3]);
				if (SantInitialMarginQefErrorCodeEnum.isValid(this.errorCode) == null) {
					code = SantInitialMarginCalypsoErrorCodeEnum.CodeNotValid;
				}
			} catch (NumberFormatException ex) {
				code = SantInitialMarginCalypsoErrorCodeEnum.CodeNotValid;
			}

		}

		// no errors yet
		if (msgSplit.length > NUM_FIELDS_EXPECTED) {
			if (code.getCode() == 0) {
				if (msgSplit[4] != null){
					this.errorMessage = msgSplit[4];
				}
			}
		}

		return code;
	}
}
