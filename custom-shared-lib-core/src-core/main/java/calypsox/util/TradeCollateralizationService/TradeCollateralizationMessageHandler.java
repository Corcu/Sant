/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.TradeCollateralizationService;

import java.util.Arrays;

/**
 * Handles the tradecollateralization input and output messages. It gather the
 * input message and generates de corresponding bean. On the other side, from an
 * output bean, it generates the corresponding ouput message.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 04/03/2013
 * 
 */
public class TradeCollateralizationMessageHandler extends TradeCollateralizationConstants {
	public static String SEPARATOR = "|";

	/**
	 * 
	 * @param messageStr
	 *            String line to parser in the form
	 *            1116002|BSTE|QTE1|CBL_BOOK|FORWARD_RATE_AGREEMENT|07/11/2012|
	 *            07/05/2013|30/01/2013|EUR|09/01/2013|
	 * @return the inputBean expected
	 * @throws Exception
	 *             if the line does not have as many elements as expected to
	 *             build the bean
	 */
	public TradeCollateralizationInputBean parseMessage(final String messageStr)
			throws TradeCollateralizationException {
		TradeCollateralizationInputBean bean = new TradeCollateralizationInputBean();

		if (!messageStr.contains(SEPARATOR)) {
			throw new TradeCollateralizationException(
					"Not possible to parse message. The message DON'T INCLUDE the separator: "
							+ SEPARATOR + " The expected input field must be: "
							+ DFA_INPUT_SIMULATED_FIELDS.toStringSortedAndDescription() + " The message received is: "
							+ messageStr);
		}

		//String[] parts = splitString(messageStr, DFA_INPUT_SIMULATED_FIELDS.values().length, SEPARATOR);
		String[] parts = messageStr.split("\\|",-1); 
		 
		int dfaSimulatedFieldsNum = 11;
		int dfaNotSimulatedFieldsNum = 3;
		int numberOfPhoenixFields = 5;
		
		bean.setPhoenix(false);
		if ((parts.length == dfaSimulatedFieldsNum + numberOfPhoenixFields || parts.length == dfaNotSimulatedFieldsNum + numberOfPhoenixFields) && parts[0].equals("Phoenix")) {
			bean.setPhoenix(true); 

			bean.setFOExternalReference(parts[1].trim());
			bean.setFOSourceSystem(parts[2].trim());
			bean.setInstrument(parts[3].trim());
			bean.setTipology(parts[4].trim());
			String[] newParts = new String[dfaSimulatedFieldsNum]; // Always simulation mode
			// skip three first fields
			for (int i = numberOfPhoenixFields; i < parts.length; i++) {
				newParts[i-numberOfPhoenixFields] = parts[i];
			} 
			parts = newParts;
		}

		if (parts.length == dfaSimulatedFieldsNum) {
			bean.setBOExternalReference(parts[DFA_INPUT_SIMULATED_FIELDS.BO_EXTERNAL_REFERENCE.getPosition()].trim());
			bean.setBOSourceSystem(parts[DFA_INPUT_SIMULATED_FIELDS.BO_SOURCE_SYSTEM.getPosition()].trim());
			bean.setProcessingOrg(parts[DFA_INPUT_SIMULATED_FIELDS.PROCESSING_ORG.getPosition()].trim());
			bean.setCounterParty(parts[DFA_INPUT_SIMULATED_FIELDS.COUNTERPARTY.getPosition()].trim());
			bean.setProductType(parts[DFA_INPUT_SIMULATED_FIELDS.PRODUCT_TYPE.getPosition()].trim());
			bean.setStartDate(parts[DFA_INPUT_SIMULATED_FIELDS.START_DATE.getPosition()].trim());
			bean.setEndDate(parts[DFA_INPUT_SIMULATED_FIELDS.END_DATE.getPosition()].trim());
			bean.setValueDate(parts[DFA_INPUT_SIMULATED_FIELDS.VALUE_DATE.getPosition()].trim());
			bean.setCurrency(parts[DFA_INPUT_SIMULATED_FIELDS.CURRENCY.getPosition()].trim());
			bean.setProcessingDate(parts[DFA_INPUT_SIMULATED_FIELDS.MCC_PROCESSING_DATE.getPosition()].trim());
			bean.setValuationDate(parts[DFA_INPUT_SIMULATED_FIELDS.MCC_VALUATION_DATE.getPosition()].trim());
			bean.setSimulated(true);
			
			return bean;
		} else if (parts.length == dfaNotSimulatedFieldsNum) {
			bean.setBOExternalReference(parts[DFA_INPUT_TRADE_FIELDS.EXTERNAL_REFERENCE.getPosition()].trim());
			bean.setBOSourceSystem(parts[DFA_INPUT_TRADE_FIELDS.SOURCE_SYSTEM.getPosition()].trim());
			bean.setValueDate(parts[DFA_INPUT_TRADE_FIELDS.VALUE_DATE.getPosition()].trim());
			bean.setSimulated(false);

			return bean;
		} else {
			throw new TradeCollateralizationException("Not possible to parse message. We receive " + parts.length
					+ " and we EXPECT " + DFA_INPUT_SIMULATED_FIELDS.values().length + " or "
					+ DFA_INPUT_TRADE_FIELDS.values().length + " The expected input field must be: "
					+ DFA_INPUT_SIMULATED_FIELDS.toStringSortedAndDescription() + " or either: "
					+ DFA_INPUT_TRADE_FIELDS.toStringSortedAndDescription() + " The message recieved is: "
					+ messageStr);
		}
	}

	/**
	 * @param output
	 *            Bean
	 * @return the string build with the output format 1||CSA - BAD!
	 *         BBIL||294723.21||INTEREST_RATE_SWAP||BILLATERAL||01/01/1997||05/
	 *         12/2015
	 */
	public String generateMessageResponse(final TradeCollateralizationOutputBean output) {
		StringBuffer sb = new StringBuffer();
		String separator = SEPARATOR;

		sb.append(output.getIsCollateralizedDeal()).append(separator); // 1
		sb.append(output.getBOExternalReference()).append(separator); // 2
		sb.append(output.getBOSourceSystem()).append(separator); // 3
		sb.append(output.getValueDate()).append(separator); // 4
		sb.append(output.getCollateralName()).append(separator); // 5
		sb.append(output.getCollateralType()).append(separator); // 6
		sb.append(output.getProductType()).append(separator); // 7
		sb.append(output.getContractDirection()).append(separator); // 8
		sb.append(output.getCollateralStartDate()).append(separator); // 9
		sb.append(output.getCollateralEndDate()).append(separator); // 10

		return sb.toString();
	}
	
	/**
	 * @param output
	 *            Bean
	 * @return the string build with the output format 1||CSA - BAD!
	 *         BBIL||294723.21||INTEREST_RATE_SWAP||BILLATERAL||01/01/1997||05/
	 *         12/2015
	 */
	public String generateMessageResponsePhoenix(final TradeCollateralizationOutputBean output) {
		StringBuffer sb = new StringBuffer();
		String separator = SEPARATOR;

		sb.append("Phoenix").append(separator);
		sb.append(output.getFOExternalReference()).append(separator); 
		sb.append(output.getFOSourceSystem()).append(separator); 
		sb.append(output.getBOExternalReference()).append(separator); 
		sb.append(output.getBOSourceSystem()).append(separator); 
		sb.append(output.getIsCollateralizedDeal()).append(separator); 
		sb.append(output.getCollateralPortfolioCode()).append(separator); 
		sb.append(output.getIsTriparty()).append(separator); 
		//sb.append(output.getContractName()).append(separator); 
		sb.append(output.getTripartyAgent()).append(separator); 

		return sb.toString();
	}

	/**
	 * Splits the line into tokens, of either 11 or 3 items, and also
	 * controlling that the last token might be empty (as defined).
	 * 
	 * @param lineFile
	 *            to parser
	 * 
	 * @param numFields
	 *            of the line.
	 * 
	 * @param separator
	 *            used
	 * 
	 * @return an array with all the elements
	 */
	private static String[] splitString(String lineFile, final int numFields, final String separator) {
		final String[] line = new String[numFields];

		// clean special chars: any char not in set [a-zA-Z_0-9| -/.:]
		lineFile = removeSpecialCharacters(lineFile);

		// in case is forgotten the las |, it is added
		if (!lineFile.endsWith(separator)) {
			lineFile += separator;
		}

		for (int i = 0; i < line.length; i++) {
			if ((lineFile.indexOf(separator) < 0) && (i != (line.length - 1))) {
				// not finishes long array
				if ((DFA_INPUT_TRADE_FIELDS.values().length - 1) != i) {
					return Arrays.copyOf(line, i);
				} else {
					return copyShortArray(line, i);
				}
			}

			if ((i != (line.length - 1)) && (lineFile.indexOf(separator) >= 0)) {
				line[i] = lineFile.substring(0, lineFile.indexOf(separator));
				lineFile = lineFile.substring(lineFile.indexOf(separator));
				lineFile = lineFile.substring(1);
			} else {
				line[i] = lineFile;

				if (line[i].contains(separator)) {
					line[i] = line[i].replace(separator, EMPTY);
				}
			}
		}
		return line;

	}

	/*
	 * fix for when the short trade gathered from BO ref and system has no date
	 */
	private static String[] copyShortArray(String[] line, final int realSize) {

		final String[] shortArray = new String[DFA_INPUT_TRADE_FIELDS.values().length];
		for (int i = 0; i < shortArray.length; i++) {
			shortArray[i] = EMPTY;
		}

		line = Arrays.copyOf(line, realSize);

		for (int i = 0; i < line.length; i++) {
			shortArray[i] = line[i];
		}

		return shortArray;
	}

	/**
	 * remove characters not in [a-zA-Z_0-9| -/.:], special chars
	 * 
	 * @param s
	 *            to clean
	 * @return s withouth any char not included in the previous set
	 */
	private static String removeSpecialCharacters(final String s) {
		// replacement, special are "not normal" chars, so:
		final String notNormal = "[^A-Za-z0-9" + SEPARATOR + " -/.:_" + "]";
		return s.replaceAll(notNormal, EMPTY);
	}
}

// values = record.split(FIELD_SEPARATOR + spliter, -1);
// @SuppressWarnings("unused")
// public static void main(String args[]) {
//
// String t1 = "12341234|MDR - New York|BSNY|MSIL|INTEREST_RATE_SWAP|24/02/2011||24/02/2013|CLP|24/02/2013||";
// String t2 = "24/02/2013||";
// String t3 = "12341234|MDR - New York|";
// String t4 = "12341234|MDR - New York|24/02/2011";
//
// // final String[] parts = splitString(t2,
// DFA_INPUT_SIMULATED_FIELDS.values().length,
// // TradeCollateralizationServiceInterface.SEPARATOR);
//
// final String[] parts2 = splitString(t4,
// DFA_INPUT_TRADE_FIELDS.values().length,
// TradeCollateralizationServiceInterface.SEPARATOR);
//
// }
