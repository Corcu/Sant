/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory;


/**
 * Class containing the different constants to be used in the Sant Position engine (online positions service)
 * 
 * @author Guillermo Solano
 * @version 1.1
 * 
 */
public class SantPositionConstants {

	// ///////////////////////
	// //// CONSTANTS //////
	// ////////////////////
	/**
	 * Parser: Row separator
	 */
	final static public String FIELDS_SEPARATOR = "\\|";
	/**
	 * Response: separator between columns
	 */
	final static public String RESPONSE_SEPARATOR = "|";
	/**
	 * Parser: Lines separator
	 */
	final static public String LINE_SEPARATOR = "\\n";
	/**
	 * Response: separator between lines
	 */
	final static public String RESPONSE_LINE_SEPARATOR = "\n";
	/**
	 * Response: ok String
	 */
	final static public String RESPONSE_OK = "OK";
	/**
	 * Response: ok String
	 */
	final static public String RESPONSE_KO = "KO";
	/**
	 * Trade id string
	 */
	final static public String TRADE_ID = "-Trade Id = ";
	/**
	 * A new pos line with errors, will be separate using this constant
	 */
	final static public String PARSER_ERRORS_SEPARATOR = "-";
	/**
	 * A new pos line status, will be separate using this constant
	 */
	final static public String STATUS_SEPARATOR = " -> ";
	/**
	 * Equal separarto
	 */
	final static public String EQUAL_SEPARATOR = "=";
	/**
	 * Bean: Bloqueo type
	 */
	public static final String BLOQUEO = "BLOQUEO";
	/**
	 * Bean: Bloqueo mapping for GD field
	 */
	public static final String BLOQUEO_MAPPING = BLOQUEO;
	/**
	 * Bean: Actual type
	 */
	public static final String ACTUAL = "ACTUAL";
	/**
	 * Bean: Actual mapping for GD field
	 */
	public static final String ACTUAL_MAPPING = "ACT";
	/**
	 * Bean: theorical type
	 */
	public static final String THEORETICAL = "THEORETICAL";

	/**
	 * Bean: theorical mapping for GD field
	 */
	public static final String THEORETICAL_MAPPING = "THEOR";
	/**
	 * Bean: normal mode
	 */
	public static final String NORMAL_MODE = "0";
	/**
	 * Bean: EOD mode (last message with static errors to D day)
	 */
	public static final String EOD_MODE = "1";
	/**
	 * Empty String
	 */
	public final static String EMPTY = "";
	/**
	 * Number of fields of the incoming message. Defined 10 fields
	 */
	final static public Integer FIELDS_NUMBER = 10;

	// 1 C?digo ISIN X(12) VARCHAR2(12)
	// 2 C?digo Divisa X(03) VARCHAR2(3)
	// 3 C?digo Portfolio X(15) VARCHAR2(32)
	// 4 Nemot?cnico Custodio X(06) VARCHAR2(32)
	// 5 C?digo Cuenta Custodio X(35) VARCHAR2(35)
	// 6 Estado X(07) VARCHAR2(32)
	// 7 N? de T?tulos S9(15) Amount (32)
	// 8 Fecha valor posici?n X(10) X(10)
	// 9 Timestamp envio X(26) X(26)
	// 10. Modo Env?o X(01) X(01)

	// ///////////////////////
	// /////// ENUMS ////////
	// ////////////////////
	/*
	 * Enum that defines the types of responses for each position new line. It contains all the possible responses sent
	 * to the SourceSystem
	 */
	// Enum defined as: "message" and value response
	public enum RESPONSES_CODES {

		/* MESSAGE RESPONSES */
		ACK_OK(" Position updated, no resend", "00"), // input fields errors
		ACK_ERROR(" Position updated, no resend", "01"), // error is anything DIFFERENT to Zero 00
		/* ERRORS RESPONSES */
		ERR_BOOK("Missing /wrong Book", "01"),
		ERR_ISIN("Missing/wrong ISIN", "02"),
		ERR_CUSTODIO_AGENT("Missing/wrong Custodio-Agent", "03"),
		ERR_ACCOUNT("Missing/wrong Account", "04"),
		ERR_STATE("Missing/wrong State", "05"),
		ERR_POSITION_DATE("Missing/wrong position date", "06"),
		ERR_SEND_MODE("Missing/wrong send mode", "07"),
		ERR_UNKONWN("File is unreadible / unknown exception", "08"),
		ERR_DB("Engine is down / Severe DB error", "40"),
		ERR_INTERNAL("Message not send/internal error", "10"),
		ERR_CCY("Missing/wrong ccy", "11"),
		ERR_POSITION_TYPE("Missing/wrong position type", "12"),
		ERR_QTY("Missing/wrong quantity", "13"),
		ERR_INCORRECT_NUMBER_FIELDS("Incorrect number of fields", "20"),
		WAR_SDI_NOT_CONFIGURED("SDI instruction not found or not properly configured", "30"),
		ERR_PAST_POSITION("Past position received, discard it", "31");

		private final String description;
		private final String descriptionValue;

		private RESPONSES_CODES(String description, String v) {
			this.description = description;
			this.descriptionValue = v;
		}

		public String getResponseText() {
			return this.description;
		}

		public String getResponseValue() {
			return this.descriptionValue;
		}
	}

}
