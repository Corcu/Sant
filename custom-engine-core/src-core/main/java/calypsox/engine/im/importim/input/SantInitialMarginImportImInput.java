/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
package calypsox.engine.im.importim.input;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.LocalCache;

import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;

public class SantInitialMarginImportImInput {
	private static final int NUM_FIELDS_EXPECTED = 8;

	private static final int CCY_LENGTH = 3;

	private static final String DEFAULT_VALUE_NONE = "NONE";

	public final static String SEPARATOR = "\\|";

	private JDate processDate;
	private JDate valueDate;
	
	private String contractName;
	private Double imPo;
	private Double imCpty;
	private String contractCcyPo;
	
	/** error code */
	private String contractCcyCpty;
	
	/** error code */
	private int errorCode;

	/** error description */
	private String errorMessage;
	
	
	public SantInitialMarginImportImInput() {
		this.processDate = JDate.getNow();
		this.valueDate = JDate.getNow().addBusinessDays(-1, LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
		this.contractName = "";
		this.imPo = new Double(0.0);
		this.imCpty = new Double(0.0);
		this.contractCcyPo = DEFAULT_VALUE_NONE;
		this.contractCcyCpty = DEFAULT_VALUE_NONE;
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

	public Double getImPo() {
		return this.imPo;
	}

	public void setImPo(Double imPo) {
		this.imPo = imPo;
	}

	public Double getImCpty() {
		return this.imCpty;
	}

	public void setImCpty(Double imCpty) {
		this.imCpty = imCpty;
	}

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public String getContractCcyPo() {
		return contractCcyPo;
	}

	public void setContractCcyPo(String contractCcyPo) {
		this.contractCcyPo = contractCcyPo;
	}

	public String getContractCcyCpty() {
		return contractCcyCpty;
	}

	public void setContractCcyCpty(String contractCcyCpty) {
		this.contractCcyCpty = contractCcyCpty;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

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
			
				if (!Util.isNumber(msgSplit[7].toString()) || Integer.parseInt(msgSplit[7]) != 0){
					code = SantInitialMarginCalypsoErrorCodeEnum.MessageNotValid;
					if (msgSplit.length > NUM_FIELDS_EXPECTED && msgSplit[8] != null){
						this.errorMessage = msgSplit[8];
					}
					return code;
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
				this.imPo = Util.stringToNumber(msgSplit[3]);
				this.imCpty = Util.stringToNumber(msgSplit[4]);
			} catch (NumberFormatException ex) {
				code = SantInitialMarginCalypsoErrorCodeEnum.InitialMarginNotValid;
			}
		}

		// no errors yet
		if (code.getCode() == 0) {
			this.contractCcyPo = msgSplit[5];
			if (this.contractCcyPo.length() != CCY_LENGTH) {
				code = SantInitialMarginCalypsoErrorCodeEnum.CurrencyPONotValid;

				return code;
			}
		}
		
		// no errors yet
		if (code.getCode() == 0) {
			this.contractCcyCpty = msgSplit[6];
			if (this.contractCcyCpty.length() != CCY_LENGTH) {
				code = SantInitialMarginCalypsoErrorCodeEnum.CurrencyCPTYNotValid;

				return code;
			}
		}
		
		// no errors yet
		if (code.getCode() == 0) {
			if (msgSplit[7] == null) {
				code = SantInitialMarginCalypsoErrorCodeEnum.CodeNotValid;
				return code;
			}
			this.errorCode = Integer.parseInt(msgSplit[7]);
			
		}

		return code;
	}
}
