package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.GenericCommentLight;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.FdnUtilProvider;

public class CustomFallidasReportStyle extends TransferReportStyle {

	private static final String UNMATCHED = "UNMATCHED";
	private static final String SHORT = "SHORT";
	private static final String DK = "DK";
	private static final String CPTY = "CPTY_";
	private static final String SAN = "SAN_";
	private static final String PAY = "PAY";
	private static final String REASON_STATUS_MAP_FAILED = "ReasonStatusMapFailed";
	private static final String MATCHING_REASON = "Matching_Reason";
	private static final String SETTLEMENT_REASON = "Settlement_Reason";
	private static final String SETTLEMENT_REFERENCE_INSTRUCTED = "SettlementReferenceInstructed";
	public static final String SIMULATED = "_Simulated";
	private static final String RECEIVE = "RECEIVE";
	private static final String BYIY = "BYIY";
	private static final String SI = "Si";
	private static final String NO = "No";
	private static final String CSDR_DEFERRAL_DATE = "CSDRDeferralDate";
	private static final String CSDR_BUY_IN_DATE = "CSDRBuyInDate";
	private static final String CSDR_POTENCIAL_PENALTY_DEF_PERIOD = "CSDRPotencialPenaltyDefPeriod";
	private static final String CSDR_POTENCIAL_PENALTY_EXT_PERIOD = "CSDRPotencialPenaltyExtPeriod";
	private static final String CSDR_POTENCIAL_PENALTY_DAILY = "CSDRPotencialPenaltyDaily";
	private static final String CSDR_POTENCIAL_PENALTY_BUY_IN_PERIOD = "CSDRPotencialPenaltyBuyInPeriod";
	private static final String MT537 = "MT537";
	private static final String PSET = ":PSET//";
	private static final String CLEA = ":CLEA//";
	public static final String MT54 = "MT54";
	private static final String SETR = ":SETR//";
	private static final String MT548 = "MT548";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String MESSAGES_MAP = "MESSAGESMAP";
	// Columnas reporte
	public static final String EXTENSION_COLUMN1 = "Comentario";
	public static final String EXTENSION_COLUMN2 = "Producto";
	public static final String EXTENSION_COLUMN3 = "Instrumento";
	public static final String EXTENSION_COLUMN4 = "Folder";
	public static final String EXTENSION_COLUMN5 = "Trader";
	public static final String EXTENSION_COLUMN6 = "Referencia FO";
	public static final String EXTENSION_COLUMN7 = "Referencia BO";
	public static final String EXTENSION_COLUMN8 = "Operacion";
	// public static final String EXTENSION_COLUMN9 = "Matching_Reason"; Eliminado
	// respecto al reporte actual
	// public static final String EXTENSION_COLUMN10 = "Matching_Status"; Eliminado
	// respecto al reporte actual
	public static final String EXTENSION_COLUMN11 = "F. Contra.";
	public static final String EXTENSION_COLUMN12 = "F. Valor";
	public static final String EXTENSION_COLUMN13 = "ISIN";
	public static final String EXTENSION_COLUMN14 = "Descripcion";
	public static final String EXTENSION_COLUMN15 = "Contrapartida";
	public static final String EXTENSION_COLUMN16 = "CounterParty Short Name";
	public static final String EXTENSION_COLUMN17 = "Tipo";
	public static final String EXTENSION_COLUMN18 = "Nominal";
	public static final String EXTENSION_COLUMN19 = "Divisa";
	public static final String EXTENSION_COLUMN20 = "Efectivo";
	public static final String EXTENSION_COLUMN21 = "Trade Comment";
	public static final String EXTENSION_COLUMN22 = "GLS";
	public static final String EXTENSION_COLUMN23 = "Custodio";
	public static final String EXTENSION_COLUMN24 = "Estado actual";
	public static final String EXTENSION_COLUMN25 = "Clase";
	public static final String EXTENSION_COLUMN26 = "Causa";
	public static final String EXTENSION_COLUMN27 = "SETR";
	public static final String EXTENSION_COLUMN28 = "TRAD";
	public static final String EXTENSION_COLUMN29 = "CLEA";
	public static final String EXTENSION_COLUMN30 = "Place of Settlement";
	public static final String EXTENSION_COLUMN31 = "Marca de Recompra";
	public static final String EXTENSION_COLUMN32 = "Referencia Contratacion";
	public static final String EXTENSION_COLUMN33 = "Camara Liquidacion";
	public static final String EXTENSION_COLUMN34 = "Posicion Disponible";
	// public static final String EXTENSION_COLUMN35 = "Deadline"; No se envia, se
	// calcula en la API
	public static final String EXTENSION_COLUMN36 = "DaysToISD";
	public static final String EXTENSION_COLUMN37 = "PotPty ISD";
	public static final String EXTENSION_COLUMN38 = "PotPtySize";
	public static final String EXTENSION_COLUMN39 = "Pty Aggregate";
	public static final String EXTENSION_COLUMN40 = "Pot Pty Buy-in";
	public static final String EXTENSION_COLUMN41 = "PotPtyExt Period";
	public static final String EXTENSION_COLUMN42 = "Pot PtyDef Period";
	public static final String EXTENSION_COLUMN43 = "Status de Fallida";
	// public static final String EXTENSION_COLUMN44 = "NominalRiesgo"; No se envia,
	// se calcula en la API
	// public static final String EXTENSION_COLUMN45 = "CPYCritica"; No se envia, se
	// calcula en la API
	public static final String EXTENSION_COLUMN46 = "Team";
	public static final String EXTENSION_COLUMN47 = "Sub-team";
	// public static final String EXTENSION_COLUMN48 = "Portfolio";Eliminado
	// respecto al reporte actual
	// public static final String EXTENSION_COLUMN49 = "Trader Id"; Eliminado
	// respecto al reporte actual
	public static final String EXTENSION_COLUMN50 = "Pata de Ida/Vuelta";
	public static final String EXTENSION_COLUMN51 = "Buy-in date";
	public static final String EXTENSION_COLUMN52 = "days to buy-in";
	public static final String EXTENSION_COLUMN53 = "days to deferral period";
	public static final String EXTENSION_COLUMN54 = "razomoti";
	public static final String EXTENSION_COLUMN55 = "lastComment";

	// Keywords Calypso
	// Keywords reporte /** The Constant FAILING_DAYS . */
	public static final String TRADEKEYWORD_FAILINGDAYS = "FailingDays";
	// Keywords reporte /** The Constant CLIENTREF . */
	public static final String TRADEKEYWORD_CLIENTEREF = "ClientRef";
	// Keywords reporte /** The Constant MXELECTPLATF . */
	public static final String TRADEKEYWORD_MXELECTPLATF = "Mx Electplatf";
	// Keywords reporte /** The Constant MXELECTPLATID . */
	public static final String TRADEKEYWORD_MXELECTPLATID = "Mx Electplatid";
	// Message Attribute reporte /** The Constant FAILING_DAYS . */
	public static final String MESSAGEATTRIBUTE_FAILINGDAYS = "FailingDays";
	// Message Attribute reporte /** The Constant CLIENTREF . */
	public static final String MESSAGEATTRIBUTE_CLIENTEREF = "ClientRef";
	// Message Attribute reporte /** The Constant Penalty_Amount . */
	public static final String MESSAGEATTRIBUTE_PENALTYAMOUNT = "Penalty_Amount";
	// XferAttribute reporte /** The Constant CSDRFailedTransferMark . */
	public static final String XFERRATTRIBUTE_CSDRFAILEDTRANSFERMARK = "CSDRFailedTransferMark";
	// XferAttribute reporte /** The Constant PenaltiesEstimatedAmount . */
	public static final String XFERRATTRIBUTE_PENALTIESESTIMATEDAMOUNT = "Penalties_EstimatedAmount";

	// CSDR Period attributes names.
	public static final String XFERRATTRIBUTE_CSDRBUYINDATE = CSDR_BUY_IN_DATE;
	public static final String XFERRATTRIBUTE_CSDRDEFERRALDATE = CSDR_DEFERRAL_DATE;
	public static final String XFERRATTRIBUTE_CSDRCOMPENSATIONDATE = "CSDRCompensationDate";
	// constante reporte /** Longitud del bic. */
	public static final int MAX_SIZE_BIC = 8;

	// Constant reporte /** The Constant LIMITE NOMINAL 200M. */
	public static final int NOMINAL_LIMIT = 200000000;
	// Constant Domain Name Counterparty critica*/
	public static final String DOMAIN_NAME_CPYS = "Critic Counterparty";

	// Constantes mensajeria SWIFT
	// MT548 TAG: 24B. Reason Code
	public static final String MT548_TAG_24B = ":24B:";
	// MT548 TAG: 24B. Matching Code
	public static final String MT548_TAG_25D = ":25D:";
	// MT54|1/2/3 TAG: 20C. Reference
	public static final String MT54X_TAG_20C = ":20C:";
	// MT540 TAG: 22F. Linkage Type Indicator
	public static final String MT540_TAG_22F = ":22F:";
	// MT54X TAG: 22F. Linkage Type Indicator
	public static final String MT54X_TAG_22F = ":22F:";
	// MT548 TAG: 22F. Linkage Type Indicator
	public static final String MT548_TAG_22F = ":22F:";
	// MT54|1/2/3 TAG: 94B. Place of trade
	public static final String MT54X_TAG_94B = ":94B:";
	// MT54|1/2/3 TAG: 94A. Place
	public static final String MT54X_TAG_94A = ":94A:";
	// MT54|1/2/3 TAG: 94A. CLEA
	public static final String MT54X_TAG_94H = ":94H:";
	// MT548 TAG: 95. Place
	public static final String MT54X_TAG_95A = ":95A:";
	// MT548 TAG: 95. Place of Settlement
	public static final String MT54X_TAG_95P = ":95P:";
	// MT54|1/2/3 QUALIFIER: CLEA. Place of Clearing
	public static final String MT54X_QUALIFIER_CLEA = "CLEA//";
	// MT54|1/2/3 QUALIFIER: PSET. Place of Clearing
	public static final String MT54X_QUALIFIER_PSET = "PSET//";
	// MT54|1/2/3 QUALIFIER: SETR. Type of settlement transaction
	public static final String MT54X_QUALIFIER_SETR = "SETR//";
	// MT540 QUALIFIER: SETR. Type of settlement transaction
	public static final String MT540_QUALIFIER_SETR = "SETR//";
	// MT54|1/2/3 QUALIFIER: TRAD. Type of settlement transaction
	public static final String MT54X_QUALIFIER_TRAD = "TRAD//";
	// MT54|1/2/3 QUALIFIER: SEME. Sender's Message Reference
	public static final String MT54X_QUALIFIER_SEME = "SEME//";
	// MT548 QUALIFIER: NMAT. Unmatched Reason
	public static final String MT548_QUALIFIER_NMAT = "NMAT//";
	// MT548 QUALIFIER: MTACH. Matching status
	public static final String MT548_QUALIFIER_MATCH = "MTCH//MACH";
	// MT548 CODE: LACK. Lack of Securities
	public static final String MT548_CODE_LACK = "LACK";
	// MT548 CODE: CLAC. Counterparty Insufficient Securities
	public static final String MT548_CODE_CLAC = "CLAC";
	// Posibles valores de Status de fallida
	public static final String STATUS_FAILED_EXTENSION_PERIOD = "failed - extension period";
	public static final String STATUS_FAILED_DEFERRAL_PERIOD = "failed - deferral period";
	public static final String STATUS_FAILED_BUYIN_PERIOD = "failed - buy in period";

	/**
	 *
	 */
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors) {

		if (row == null) {
			return null;
		}

		// Override these columns
		BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
		Trade trade = row.getProperty(ReportRow.TRADE);
		Double posicionDisponible = row.getProperty(CustomFallidasReport.PROPERTY_POSITION);
		PricingEnv env = row.<PricingEnv>getProperty("PricingEnv");
		HashMap<?, ?> message = null;
		if (row.getProperty(MESSAGES_MAP) instanceof HashMap<?, ?>) {
			message = row.getProperty(MESSAGES_MAP);
		}

		SettleDeliveryInstruction sdi = row.getProperty(ReportRow.LE_SDI);
		LEContact leContact = row.getProperty(ReportRow.LE_CONTACT);
		JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);
		if (valDateTime == null)
			valDateTime = new JDatetime();
		JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
				: valDateTime.getJDate(TimeZone.getDefault());
		Log.debug(CustomFallidasReportStyle.class,
				"Entrada metodo getColumnValue Custom Report Style " + transfer.getLongId());

		Object rst = null;

		if (columnName.equals(EXTENSION_COLUMN1)) {
			// Se envia vacio
		} else if (columnName.equals(EXTENSION_COLUMN2)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN3)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN4)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN5)) {
			if (trade != null && trade.getKeywordValue(TRADEKEYWORD_MXELECTPLATF) != null) {
				rst = trade.getKeywordValue(TRADEKEYWORD_MXELECTPLATF);
			}
		} else if (columnName.equals(EXTENSION_COLUMN6)) {
			if (trade != null && trade.getExternalReference() != null) {
				rst = trade.getExternalReference();
			}
		} else if (columnName.equals(EXTENSION_COLUMN7)) {
			if (trade != null && trade.getLongId() != 0) {
				rst = Long.toString(trade.getLongId());
			}
		} else if (columnName.equals(EXTENSION_COLUMN8)) {
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);			
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_20C,
						MT54X_QUALIFIER_SEME, null);
				if (swiftField != null) {
					String tag20Seme = swiftField.getValue();
					if (tag20Seme.contains(":SEME//")) {
						tag20Seme = tag20Seme.replace(":SEME//", "");
					}
					rst = tag20Seme;
				}
			}else if(transfer!=null){
				rst=transfer.getAttribute(SETTLEMENT_REFERENCE_INSTRUCTED);
			}
		} else if (columnName.equals(EXTENSION_COLUMN11)) {
			if (trade != null && trade.getBook() != null && trade.getTradeDate() != null) {
				JDate tradeDate = trade.getBook().getJDate(trade.getTradeDate());
				rst = tradeDate;
			}
		} else if (columnName.equals(EXTENSION_COLUMN12)) {
			if (transfer != null && transfer.getValueDate() != null) {
				rst = transfer.getValueDate();
			}
		} else if (columnName.equals(EXTENSION_COLUMN13)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN14)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN15)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN16)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN17)) {
			rst = super.getColumnValue(row, DELIVERY_TYPE, errors);
			if (rst instanceof String && transfer.getPayReceive().equals(RECEIVE)) {
				rst = ((String) rst).replace("D", "R");
			}
		} else if (columnName.equals(EXTENSION_COLUMN19)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN20)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN21)) {
			// En el reporte actual
		} else if (columnName.equals(EXTENSION_COLUMN22)) {
			if (trade != null) {
				// Campo Ctpty. Code
				rst = trade.getCounterParty().getCode();
			}
		} else if (columnName.equals(EXTENSION_COLUMN23)) {
			// BIC del AGENT con 8 en vez de 11 caracteres
			rst = getBic(leContact);
		} else if (columnName.equals(EXTENSION_COLUMN24)) {
			rst = "Failed/Pending to Settlement";
		} else if (columnName.equals(EXTENSION_COLUMN25)) {
			rst = getClass(transfer, message);
		} else if (columnName.equals(EXTENSION_COLUMN26)) {
			rst = getCause(transfer, message);
		} else if (columnName.equals(EXTENSION_COLUMN27)) {
			// :22F::SETR - Type of settlement transaction del MT54|0/1/2/3
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);
			if(swiftMessage==null) {
				swiftMessage = getSimulatedSwift(message,transfer,trade,env,errors);
			}
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_22F,
						MT54X_QUALIFIER_SETR, null);
				if (swiftField != null) {
					String tag22FSetr = swiftField.getValue();

					if (tag22FSetr != null) {
						if (tag22FSetr.contains(SETR)) {
							tag22FSetr = tag22FSetr.replace(SETR, "");
						}

						rst = tag22FSetr;
					}
				}
			}

		} else if (columnName.equals(EXTENSION_COLUMN28)) {
			// :94B::TRAD - Place of trade del MT54|0/1/2/3
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);
			if(swiftMessage==null) {
				swiftMessage = getSimulatedSwift(message,transfer,trade,env,errors);
			}
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_94B,
						MT54X_QUALIFIER_TRAD, null);
				if (swiftField != null) {
					String tag94BTRAD = swiftField.getValue();
					if (tag94BTRAD != null) {
						if (tag94BTRAD.contains(":TRAD//")) {
							tag94BTRAD = tag94BTRAD.replace(":TRAD//", "");
						}

						rst = tag94BTRAD;
					}
				}

			}
		} else if (columnName.equals(EXTENSION_COLUMN29)) {
			// :94a::CLEA - Place of clearing del MT54|1/2/3
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);
			if(swiftMessage==null) {
				swiftMessage = getSimulatedSwift(message,transfer,trade,env,errors);
			}
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_94H,
						MT54X_QUALIFIER_CLEA, null);
				if (swiftField != null) {
					String tag94Clea = swiftField.getValue();

					if (tag94Clea != null) {
						if (tag94Clea.contains(CLEA)) {
							tag94Clea = tag94Clea.replace(CLEA, "");
						}
						rst = tag94Clea;
					}
				}
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField2 = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_94A,
						MT54X_QUALIFIER_CLEA, null);
				if (swiftField2 != null) {
					String tag94Clea = swiftField2.getValue();

					if (tag94Clea != null) {
						if (tag94Clea.contains(CLEA)) {
							tag94Clea = tag94Clea.replace(CLEA, "");
						}
						rst = tag94Clea;
					}
				}
			}
		} else if (columnName.equals(EXTENSION_COLUMN30)) {
			// :95a::PSET - Place of settlement
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);
			if(swiftMessage==null) {
				swiftMessage = getSimulatedSwift(message,transfer,trade,env,errors);
			}
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_95P,
						MT54X_QUALIFIER_PSET, null);
				if (swiftField != null) {
					String tag95pPset = swiftField.getValue();

					if (tag95pPset != null) {
						if (tag95pPset.contains(PSET)) {
							tag95pPset = tag95pPset.replace(PSET, "");
						}
						rst = tag95pPset;
					}
				}
				SwiftFieldMessage swiftField2 = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_95A,
						MT54X_QUALIFIER_PSET, null);
				if (swiftField2 != null) {
					String tag95aPset = swiftField2.getValue();

					if (tag95aPset != null) {
						if (tag95aPset.contains(PSET)) {
							tag95aPset = tag95aPset.replace(PSET, "");
						}
						rst = tag95aPset;
					}
				}
			}

		} else if (columnName.equals(EXTENSION_COLUMN31)) {
			rst = NO;
			// Si tag :22F::SETR - Type of settlement transaction es BYIY entonces Si, else
			// No.
			SwiftMessage swiftMessage = getInstructedSwift(message,transfer,trade,env,errors);
			if(swiftMessage==null) {
				swiftMessage = getSimulatedSwift(message,transfer,trade,env,errors);
			}
			if (swiftMessage != null) {
				// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
				SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT54X_TAG_22F,
						MT54X_QUALIFIER_SETR, null);
				if (swiftField != null) {
					String tag22FSetr = swiftField.getValue();
					if (tag22FSetr != null && tag22FSetr.contains(SETR)) {
						tag22FSetr = tag22FSetr.replace(SETR, "").trim();
						if (tag22FSetr.equalsIgnoreCase(BYIY)) {
							rst = SI;
						}
					}

				}
			}

		} else if (columnName.equals(EXTENSION_COLUMN32)) {
			// Message/Trade Atributes ClienteRef
			BOMessage boMessage = null;
			if (message != null && message.get(MT537) != null) {
				if (message.get(MT537) instanceof HashMap<?, ?>) {
					HashMap<?, ?> hashMess = (HashMap<?, ?>) message.get(MT537);
					if (hashMess.get(BOMessage.class.getSimpleName()) instanceof BOMessage) {
						boMessage = (BOMessage) hashMess.get(BOMessage.class.getSimpleName());
					}
				}
				if (boMessage != null && boMessage.getAttribute(MESSAGEATTRIBUTE_CLIENTEREF) != null) {
					rst = boMessage.getAttribute(MESSAGEATTRIBUTE_CLIENTEREF);
				} else if (trade != null && trade.getKeywordValue(TRADEKEYWORD_CLIENTEREF) != null) {
					rst = trade.getKeywordValue(TRADEKEYWORD_CLIENTEREF);
				}
			} else {
				for (int i = 0; i < 4 && boMessage == null; i++) {
					if (message != null && message.get(MT54 + i) != null
							&& message.get(MT54 + i) instanceof HashMap<?, ?>) {
						HashMap<?, ?> hashMess = (HashMap<?, ?>) message.get(MT54 + i);
						if (hashMess.get(BOMessage.class.getSimpleName()) instanceof BOMessage) {
							boMessage = (BOMessage) hashMess.get(BOMessage.class.getSimpleName());
							rst = boMessage.getLongId();
						}
					}

				}
			}

		} else if (columnName.equals(EXTENSION_COLUMN33)) {
			if (sdi != null && sdi.getIntermediaryId() > 0) {
				rst = sdi.getIntermediaryName();
			} else if (sdi != null && sdi.getAgentId() > 0) {
				rst = sdi.getAgentName();
			}
		} else if (columnName.equals(EXTENSION_COLUMN34)) {
			if (posicionDisponible != null) {
				rst = new Amount(posicionDisponible);
			}
		} else if (columnName.equals(EXTENSION_COLUMN36)) {
			rst = getDaysToISD(trade, transfer, valDate);
		} else if (columnName.equals(EXTENSION_COLUMN37)) {
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY) != null) {
				rst = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY);
			}
		} else if (columnName.equals(EXTENSION_COLUMN38)) {
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY) != null) {
				String rstAmt = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY);

				double amt = Math.abs(Double.parseDouble(rstAmt));

				try {
					amt = CurrencyUtil.convertAmount(env, amt, transfer.getSettlementCurrency(), "EUR", valDate,
							env.getQuoteSet());
				} catch (MarketDataException e) {
					Log.error("Cannot convert penalty amount in " + transfer.getSettlementCurrency()
							+ " to EUR. Using rate 1.", e);
				}
				if (amt >= 0.0 && amt <= 9.0) {
					rst = "XS";
				} else if (amt > 9.0 && amt <= 49.0) {
					rst = "S";
				} else if (amt > 49.0 && amt <= 499.0) {
					rst = "M";
				} else if (amt > 499.0 && amt <= 999.0) {
					rst = "L";
				} else if (amt > 999.0 && amt <= 4999.0) {
					rst = "XL";
				} else if (amt > 4999.0) {
					rst = "XXL";
				}
			}
		} else if (columnName.equals(EXTENSION_COLUMN39)) {
			// Message/Transfer Atributes Penalty_Amount
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY) != null) {
				String rstAmt = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DAILY);

				double amt = Double.parseDouble(rstAmt);
				int days = getDaysToISD(trade, transfer, valDate);
				if (days > 0) {
					rst = new Amount(amt * days);
				} else {
					rst = new Amount(0.0);
				}
			}
		} else if (columnName.equals(EXTENSION_COLUMN40)) {
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_BUY_IN_PERIOD) != null) {
				String rstAmt = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_BUY_IN_PERIOD);
				rst = new Amount(Double.parseDouble(rstAmt));
			}
		} else if (columnName.equals(EXTENSION_COLUMN41)) {
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_EXT_PERIOD) != null) {
				String rstAmt = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_EXT_PERIOD);
				rst = new Amount(Double.parseDouble(rstAmt));
			}
		} else if (columnName.equals(EXTENSION_COLUMN42)) {
			if (transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DEF_PERIOD) != null) {
				String rstAmt = transfer.getAttribute(CSDR_POTENCIAL_PENALTY_DEF_PERIOD);
				rst = new Amount(Double.parseDouble(rstAmt));
			}
		} else if (columnName.equals(EXTENSION_COLUMN43)) {
			// Xfer Atributes CSDRFailedTransferMark
			rst = transfer.getAttribute(XFERRATTRIBUTE_CSDRFAILEDTRANSFERMARK);
		} else if (columnName.equals(EXTENSION_COLUMN46)) {
			// No viajara desde Calypso
		} else if (columnName.equals(EXTENSION_COLUMN47)) {
			// No viajara desde Calypso
		} else if (columnName.equals(EXTENSION_COLUMN50)) {
			// Se descarta en esta fase
		} else if (columnName.equals(EXTENSION_COLUMN51)) {
			rst = getBuyInDate(transfer);
		} else if (columnName.equals(EXTENSION_COLUMN52)) {
			String statusFall = transfer.getAttribute(XFERRATTRIBUTE_CSDRFAILEDTRANSFERMARK);
			if (statusFall != null && statusFall.equalsIgnoreCase(STATUS_FAILED_EXTENSION_PERIOD)) {
				rst = diffBusinessDays(valDate, getBuyInDate(transfer), trade);
			}
		} else if (columnName.equals(EXTENSION_COLUMN53)) {
			String statusFall = transfer.getAttribute(XFERRATTRIBUTE_CSDRFAILEDTRANSFERMARK);
			if (statusFall != null && statusFall.equalsIgnoreCase(STATUS_FAILED_BUYIN_PERIOD)) {
				rst = diffBusinessDays(valDate, getDeferralDate(transfer), trade);
			}
		} else if (columnName.equals(EXTENSION_COLUMN54)) {
			 rst = getRazomoti(transfer,message);
		}
		
		else if(columnName.equals(EXTENSION_COLUMN55)){
			long idTransfer = transfer.getLongId();
			List<String> comentType = new ArrayList<String>();
			comentType.add("Fallidas");
			try {
				 List<GenericCommentLight> listaComment = DSConnection.getDefault().getRemoteBO().getLatestGenericComments(idTransfer, "Transfer", comentType);
				 if(listaComment != null && !listaComment.isEmpty()) {
					 rst = listaComment.get(0).getComment();
				 }	 
			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting GenericComment", e);
			}
		}
		else {
			return super.getColumnValue(row, columnName, errors);
		}

		return rst;

	}

	public String getClass(BOTransfer transfer, HashMap<?, ?> message) {
		String rst = DK;
		SwiftMessage swiftMessage = null;
		String status = getStatus(transfer,message);

		if (!Util.isEmpty(status)) {
			status = status.replaceAll(":", "");
			String rstTxt = LocalCache.getDomainValueComment(DSConnection.getDefault(), REASON_STATUS_MAP_FAILED,
					status);
			if (!Util.isEmpty(rstTxt)) {
				rst = rstTxt;
			}
		}
		return rst;
	}

	public String getCause(BOTransfer transfer, HashMap<?, ?> message) {
		String rst = UNMATCHED;
		SwiftMessage swiftMessage = null;
		String status = getStatus(transfer,message);

		if (!Util.isEmpty(status)) {
			status = status.replaceAll(":", "");
			String rstTxt = LocalCache.getDomainValueComment(DSConnection.getDefault(), REASON_STATUS_MAP_FAILED,
					status);
			if (!Util.isEmpty(rstTxt)) {
				if (status.contains(MT548_CODE_LACK)) {
					rst = SAN + rstTxt;
				} else if (status.contains(MT548_CODE_CLAC)) {
					rst = CPTY + rstTxt;
				} else if (transfer.getPayReceive() != null && transfer.getPayReceive().equals(PAY)
						&& rstTxt.equalsIgnoreCase(SHORT)) {
					rst = SAN + rstTxt;
				} else if ((transfer.getPayReceive() != null && !transfer.getPayReceive().equals(PAY))
						&& rstTxt.equalsIgnoreCase(SHORT)) {
					rst = CPTY + rstTxt;
				} else if (rstTxt.equalsIgnoreCase(DK)) {
					rst = UNMATCHED;
				} else {
					rst = rstTxt;
				}
			}
		}
		return rst;
	}
	
	public String getStatus(BOTransfer transfer, HashMap<?, ?> message) {
		SwiftMessage swiftMessage = null;
		String status = null;

		if (message != null && message.get(MT548) != null && message.get(MT548) instanceof HashMap<?, ?>) {
			HashMap<?, ?> hashMess = (HashMap<?, ?>) message.get(MT548);
			if (hashMess.get(SwiftMessage.class.getSimpleName()) instanceof SwiftMessage) {
				swiftMessage = (SwiftMessage) hashMess.get(SwiftMessage.class.getSimpleName());
			}
		}
		if (Util.isEmpty(status) && swiftMessage != null) {
			// Al metodo getSwiftField se les pasa (campos, tag, qualifier, code)
			SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT548_TAG_24B, null,
					null);
			if (swiftField != null) {
				status = swiftField.getValue();
			}
		}
		if (Util.isEmpty(status)) {
			status = transfer.getAttribute(SETTLEMENT_REASON);
		}

		if (Util.isEmpty(status) && swiftMessage != null) {
			SwiftFieldMessage swiftField = swiftMessage.getSwiftField(swiftMessage.getFields(), MT548_TAG_25D, null,
					null);
			if (swiftField != null) {
				status = swiftField.getValue();
			}
		}
		if (Util.isEmpty(status)) {
			status = transfer.getAttribute(MATCHING_REASON);
		}
		return status;
	}

	public String getRazomoti(BOTransfer transfer, HashMap<?, ?> message) {
		
		String tag24b = getStatus(transfer,message);
		if (!tag24b.isEmpty() && tag24b != null) {
			tag24b = tag24b.replace(":", "");
			
			}
		 String rst = tag24b;
		 
		 return rst;
	}
	
	
	@SuppressWarnings({ "rawtypes" })
	public SwiftMessage getInstructedSwift(Map<?, ?> message,BOTransfer transfer, Trade trade, PricingEnv env,Vector errors) {
		SwiftMessage swiftMessage = null;
		for (int i = 0; i < 4 && swiftMessage == null; i++) {
			if (message != null && message.get(MT54 + i) != null && message.get(MT54 + i) instanceof HashMap<?, ?>) {
				Map<?, ?> hashMess = (Map<?, ?>) message.get(MT54 + i);
				if (hashMess.get(SwiftMessage.class.getSimpleName()) instanceof SwiftMessage) {
					swiftMessage = (SwiftMessage) hashMess.get(SwiftMessage.class.getSimpleName());
				}

			}
		}
		return swiftMessage;
	}
	@SuppressWarnings({ "rawtypes" })
	public SwiftMessage getSimulatedSwift(Map<?, ?> message,BOTransfer transfer, Trade trade, PricingEnv env,Vector errors) {
		SwiftMessage swiftMessage = null;
		for (int i = 0; i < 4 && swiftMessage == null; i++) {
			if (message != null && message.get(MT54 + i) != null && message.get(MT54 + i) instanceof HashMap<?, ?>) {
				Map<?, ?> hashMess = (Map<?, ?>) message.get(MT54 + i);
				if (hashMess.get(SwiftMessage.class.getSimpleName()+SIMULATED) instanceof SwiftMessage) {
					swiftMessage = (SwiftMessage) hashMess.get(SwiftMessage.class.getSimpleName()+SIMULATED);
				}

			}
		}
		return swiftMessage;
	}

	public static int diffBusinessDays(JDate start, JDate end, Trade t) {
		ArrayList<String> hol = new ArrayList<>();
		if (t != null) {
			hol.addAll(t.getBook().getLegalEntity().getHolidays());
		} else {
			hol.add("TARGET");
		}
		return FdnUtilProvider.getDateUtil().numberOfBusinessDays(start, end, hol);
	}

	public static int getDaysToISD(Trade trade, BOTransfer transfer, JDate valDate) {
		int rst = 0;
		JDate dateISD = transfer.getValueDate();
		if (valDate != null && dateISD != null) {
			if (dateISD.before(valDate)) {
				rst = diffBusinessDays(dateISD, valDate, trade);
			} else {
				rst = -diffBusinessDays(valDate, dateISD, trade);
			}
		}
		return rst;
	}

	public static JDate getBuyInDate(BOTransfer transfer) {
		JDate rst = null;
		if (transfer != null && transfer.getAttribute(CSDR_BUY_IN_DATE) != null) {
			rst = Util.istringToJDate(transfer.getAttribute(CSDR_BUY_IN_DATE));
		}
		if (transfer != null && rst == null) {
			JDate valueDate = transfer.getValueDate();
			JDate buyInDate = null;
			buyInDate = valueDate.addDays(7);

			rst = buyInDate;
		}
		return rst;
	}

	public static JDate getDeferralDate(BOTransfer transfer) {
		JDate rst = null;
		if (transfer != null && transfer.getAttribute(CSDR_DEFERRAL_DATE) != null) {

			rst = Util.istringToJDate(transfer.getAttribute(CSDR_DEFERRAL_DATE));
		}
		if (transfer != null && rst == null) {
			JDate valueDate = transfer.getValueDate();
			JDate buyInDate = null;
			buyInDate = valueDate.addDays(14);

			rst = buyInDate;
		}
		return rst;
	}

	/**
	 * @param contact
	 * @return
	 * @throws CalypsoServiceException
	 */
	public static String getBic(LEContact contact) {
		String bic = "";
		if (contact != null && contact.getSwift() != null) {
			bic = contact.getSwift();
		}

		if (bic != null && bic.length() > MAX_SIZE_BIC) {
			bic = bic.substring(0, MAX_SIZE_BIC);
		}
		return bic;
	}
	
}