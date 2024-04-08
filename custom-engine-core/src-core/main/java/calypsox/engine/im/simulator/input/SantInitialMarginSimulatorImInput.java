/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
package calypsox.engine.im.simulator.input;

import com.calypso.tk.bo.ExternalMessage;

import calypsox.engine.im.errorcodes.SantInitialMarginCalypsoErrorCodeEnum;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationInputBean;

public class SantInitialMarginSimulatorImInput extends TradeCollateralizationInputBean {
	private static final int NUM_FIELDS_EXPECTED = 9;

	private static final int CCY_LENGTH = 3;

	public static final String SEPARATOR = "\\|";

	public String book = "";

	public String getBook() {
		return this.book;
	}

	public void setBook(String book) {
		this.book = book;
	}

	public SantInitialMarginCalypsoErrorCodeEnum parseInfo(ExternalMessage msgReceived) {
		SantInitialMarginCalypsoErrorCodeEnum code = SantInitialMarginCalypsoErrorCodeEnum.NoError;

		String msg = msgReceived.getText();
		String[] msgSplit = msg.split(SEPARATOR);

		if (msgSplit.length != NUM_FIELDS_EXPECTED) {
			code = SantInitialMarginCalypsoErrorCodeEnum.MessageNotValid;

			return code;
		}

		setBOExternalReference(msgSplit[0]);
		setProcessingOrg(msgSplit[1]);
		setCounterParty(msgSplit[2]);
		this.book = msgSplit[3];
		setProductType(msgSplit[4]);
		setStartDate(msgSplit[5]);
		setEndDate(msgSplit[6]);
		setValueDate(msgSplit[7]);

		// no errors yet
		if (code.getCode() == 0) {
			setCurrency(msgSplit[8]);
			if (msgSplit[8].length() != CCY_LENGTH) {
				code = SantInitialMarginCalypsoErrorCodeEnum.CurrencyNotValid;

				return code;
			}
		}

		return code;
	}
}
