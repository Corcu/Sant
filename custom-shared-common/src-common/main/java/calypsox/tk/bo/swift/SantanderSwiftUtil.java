package calypsox.tk.bo.swift;

import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftTextCustomizer;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDI;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import java.rmi.RemoteException;
import java.util.Vector;

public final class SantanderSwiftUtil {

	/**
	 * The Constant NETTED_PREFIX.
	 */
	public static final String NETTED_PREFIX = "NET";

	/**
	 * The Constant MM_PREFIX.
	 */
	public static final String MC_PREFIX = "COL";

	public static final String IM_prefix = "IM";
	public static final String CSA_prefix = "CSA";
	public static final String OSLA_perfix = "OSLA";
	public static final String ISMA_prefix = "ISMA";
	public static final String CustomerTransfer_prefix = "INT";
	public static final String NettingTransfer_prefix = "N";
	public static final String CorporateEvent_prefix = "CA";
	public static final String Bond_prefix = "BOND";
	public static final String PerformanceSwap_prefix = "DER";
	public static final int TRNlength = 16;

	public static final String CRLF = "\r\n";
	public static final String COV202_NONREF = "NONREF";

	public static final String ADDRESS_METHOD_SWIFT = "SWIFT";

	private static final String EOL_PATTERN = "(\r(?!\n)|(?<!\r)\n)|(\r\n|\r(?!\n))|(\r\n|(?<!\r)\n)";

	/**
	 * Return the MessageAttribute TRN.
	 *
	 * @param message the message
	 * @return the string
	 */
    public static String parseSANT_MSG_TRN(final BOMessage message,
                                           final LEContact sender, final LEContact rec,
			final BOTransfer transfer, final DSConnection dsCon) {
		String rst = "";
		try {
			final String trn = getTRN(message, transfer);
			rst = trn;

		} catch (final RemoteException e) {
            Log.error(SantanderSwiftUtil.class,
                    "Could not generate TRN Number for transfer: " + transfer.getLongId(), e);
			rst = "0000000000000000";
		}
		return rst;
	}


	/**
     * Generate the TRN based on the message id and the Product. For Margin call products
     * the the TRN prefix is CAL. For a netted transfer the prefix is CAN
	 *
	 * @param msg the msg
	 * @return the trn
	 * @throws RemoteException the remote exception
	 */
	public static String getTRN(final BOMessage msg, final BOTransfer transfer) throws RemoteException {
        final Trade trade = DSConnection.getDefault().getRemoteTrade()
                .getTrade(msg.getTradeLongId());
		return getTRN(msg, trade, transfer);
	}

	/**
     * Gets the TRN. (Transaction Reference Number)
     * TIPO DE ACUERDO+GLCS CONTRAPARTIDA+ID MOVIMIENTO
     * It must reach a maximun of 16x characters.
	 *
	 * @param msg   the msg
	 * @param trade the trade
	 * @return the trn
	 */

	public static String getTRN(final BOMessage msg, final Trade trade, final BOTransfer transfer) {

		String result = "0000000000000000";

		try {
			// Si es un neto generamos un TRN propio
			if (transfer.getTradeLongId() < 1) {
				if (transfer.getProductType().equalsIgnoreCase(Bond.class.getSimpleName())) {
					result = getTRN_CB(msg, trade, transfer, true);
				} else
					result = getTRN_Netting(msg);
			} else {
				// enviamos CA, Bond, Customer Transfer y Margin Calls
				if (trade.getProduct() instanceof CustomerTransfer) {
					result = getTRN_CT(msg, trade);
				}

				if (trade.getProduct() instanceof MarginCall) {
					result = getTRN_CM(msg, trade);
				}

				if (trade.getProduct() instanceof CA) {
					result = getTRN_CA(msg, trade);
				}

				if (trade.getProduct() instanceof Bond) {
					result = getTRN_CB(msg, trade, transfer, false);
				}

				if (trade.getProduct() instanceof PerformanceSwap) {
					result = getTRN_BRS(msg, trade, transfer);
				}
			}

		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN:: Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and transfer " + transfer.getLongId(), e);
		}

		return result;
	}

	public static String getTRN_CB(final BOMessage msg, final Trade trade, final BOTransfer transfer, boolean netting) {
		String result = "0000000000000000";
		String transferID = "", prefix = "";
		int fillnumber = 0;

		try {
			transferID = String.valueOf(transfer.getLongId());
			prefix = netting ? NettingTransfer_prefix + Bond_prefix : Bond_prefix;

			// check and padding till 16x
			int transferIDLength = transferID.length();
			int prefixLength = prefix.length();
			int totallong = transferIDLength + prefixLength;

			// control posiciones para no sobrepasar los 16 caracteres
			if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
				fillnumber = TRNlength - prefixLength;
				transferID = padStringZero(transferID, fillnumber);
			}
			result = prefix + transferID;

		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_CB::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}

		return result;
	}

	public static String getTRN_CA(final BOMessage msg, final Trade trade) {

		String result = "0000000000000000";

		int fillnumber, caRefLong;
		long IDtransfer;
		String sTransferID = "";

		try {

			IDtransfer = msg.getTransferLongId();
			sTransferID = Long.toString(IDtransfer);
			String caRefConci = trade.getKeywordValue("CARefConci");

			// Modificamos la lógica del campo TRN para producto CA
            //TRN = CARefConci trade keyword (Swift Event Code + Product ID del CA) + ultimos digitos del id de la transfer linkada al pago MT103/202
			// hasta llegar a 16 caracteres

			if (!Util.isEmpty(caRefConci)) {
				caRefLong = caRefConci.length();

				if (TRNlength > caRefLong) {
					int transferIdLength = TRNlength - caRefLong;
					String subTransferId = sTransferID.substring(sTransferID.length() - transferIdLength);
					result = caRefConci + subTransferId;
				}
			} else {
				// si el tradekeyword CARefConci está vacío, lógica antigua
				long IDmsg;
				String sMsgID = "";
				IDmsg = msg.getLongId();
				sMsgID = Long.toString(IDmsg);
				String GLCS = trade.getCounterParty().getCode();
				String prefix = getCorporateEvent_prefix();
				int totallong = prefix.length() + GLCS.length() + sMsgID.length();

				// control posiciones para no sobrepasar los 16 caracteres
				if (TRNlength >= totallong) {
                    //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
					fillnumber = sMsgID.length() + (TRNlength - totallong);
					sMsgID = padStringZero(sMsgID, fillnumber);
					result = prefix + GLCS + sMsgID;
				} else { // debemos recomponer para que no pase de 16x, eliminando el GLS
					GLCS = "";
					// check and padding till 16x
					totallong = prefix.length() + sMsgID.length();
					fillnumber = sMsgID.length() + (TRNlength - totallong);
					sMsgID = padStringZero(sMsgID, fillnumber);
					result = prefix + GLCS + sMsgID;
				}
			}


		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_CA::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}

		return result;
	}


	public static String getTRN_CM(final BOMessage msg, final Trade trade) {
		String result = "0000000000000000";
		String sMsgID = "", Acuerdo = "", prefix = "";
		int fillnumber = 0;

		long IDmsg;

		try {
			MarginCall mc = (MarginCall) trade.getProduct();
			Acuerdo = mc.getMarginCallConfig().getContractType();


			IDmsg = msg.getLongId();

			sMsgID = Long.toString(IDmsg);
			String GLCS = trade.getCounterParty().getCode();
			prefix = getPrefixCM(Acuerdo);

			// check and padding till 16x
			int padLength = prefix.length() + GLCS.length();
			int MsgIDlon = sMsgID.length();
			int totallong = padLength + MsgIDlon;

			// control posiciones para no sobrepasar los 16 caracteres
			if (TRNlength >= totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
				fillnumber = MsgIDlon + (TRNlength - totallong);
				sMsgID = padStringZero(sMsgID, fillnumber);
			} else { // debemos recomponer para que no pase de 16x, eliminando el GLS
				GLCS = "";
				// check and padding till 16x
				padLength = prefix.length() + GLCS.length();
				MsgIDlon = sMsgID.length();
				totallong = padLength + MsgIDlon;
				fillnumber = MsgIDlon + (TRNlength - totallong);
				sMsgID = padStringZero(sMsgID, fillnumber);
			}

			result = new String(prefix + GLCS + sMsgID);


		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_CM::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}

		return result;
	}

	public static String getTRN_CT(final BOMessage msg, final Trade trade) {
		// se compone como el prefijo INT + numero mensaje
		String result = "0000000000000000";
		long IDmsg;
		String sMsgID, prefix = "";
		int fillnumber, totallong;

		try {


			IDmsg = msg.getLongId();
			sMsgID = Long.toString(IDmsg);
			prefix = getPrefixCT();
			totallong = sMsgID.length() + prefix.length();

			if (totallong <= TRNlength) {
				// relleno
				fillnumber = sMsgID.length() + (TRNlength - totallong);
				sMsgID = padStringZero(sMsgID, fillnumber);
				result = prefix + sMsgID;
			}

		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_CT::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}


		return result;
	}


	public static String getTRN_BRS(final BOMessage msg, final Trade trade, final BOTransfer transfer) {
		String result = "0000000000000000";
		String msgId = "";
		String prefix = "";
		int fillnumber = 0;
		try {
			msgId = String.valueOf(msg.getLongId());
			prefix = PerformanceSwap_prefix;
			// check and padding till 16x
			int msgIdLength = msgId.length();
			int prefixLength = prefix.length();
			int totallong = msgIdLength + prefixLength;
			// control posiciones para no sobrepasar los 16 caracteres
			if (TRNlength > totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
				fillnumber = TRNlength - prefixLength;
				msgId = padStringZero(msgId, fillnumber);
			}
			result = prefix + msgId;
		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_BRS::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}
		return result;
	}


	public static String getTRN_Netting(final BOMessage msg) {
		// se compone como el prefijo N + numero mensaje (numero transfer para CA)
		String result = "0000000000000000";
		long IDobject;
		String sID, prefix = "";
		int fillnumber, totallong;

		try {

			// if CA, TRN = NCA + transfer id
			if (msg.getProductType().equalsIgnoreCase("CA")) {
				prefix = "NCA";
				IDobject = msg.getTransferLongId();
				sID = Long.toString(IDobject);
				totallong = sID.length() + prefix.length();

				if (totallong <= TRNlength) {
					// relleno
					fillnumber = sID.length() + (TRNlength - totallong);
					sID = padStringZero(sID, fillnumber);
					result = prefix + sID;
				}

				// if not CA, TRN = prefix + msg id

			} else {
				IDobject = msg.getLongId();
				sID = Long.toString(IDobject);
				prefix = getPrefixNetting();

				totallong = sID.length() + prefix.length();

				if (totallong <= TRNlength) {
					// relleno
					fillnumber = sID.length() + (TRNlength - totallong);
					sID = padStringZero(sID, fillnumber);
					result = prefix + sID;
				}
			}
		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "getTRN_Netting::Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + msg.getLongId(), e);
		}

		return result;
	}


	/**
     * Gets the TRN. (Transaction Reference Number)
     * TIPO DE ACUERDO+GLCS CONTRAPARTIDA+ID MOVIMIENTO
     * It must reach a maximun of 16x characters.
	 *
	 * @param msg   the msg
	 * @param trade the trade
	 * @return the trn
	 */

	public static String getTRN_V0(final BOMessage msg, final Trade trade) {

		String result = "0000000000000000";
		final int TRNlength = 16;
		String sTradeID = "";
		int fillnumber = 0;

		try {
			MarginCall mc = (MarginCall) trade.getProduct();
			String Acuerdo = mc.getMarginCallConfig().getContractType();
			long IDtrade = trade.getLongId();

			sTradeID = Long.toString(IDtrade);
			String GLCS = trade.getCounterParty().getCode();


			final String prefix = getPrefixCM(Acuerdo);

			// check and padding till 16x
			int padLength = prefix.length() + GLCS.length();
			int tradeIDlon = sTradeID.length();
			int totallong = padLength + tradeIDlon;

            //hacer metodo de relleno del tradeid y llamarlo segun necesidad de si se pasa de 16 y quitar el contrato, o meter 0 si no llega a 16
			if (TRNlength >= totallong) {
                //caracteres a anadir : como el format trabaja sobre un minimo de longitud, le debemos sumar a la longitud del trade el numero de rellenos
				fillnumber = tradeIDlon + (TRNlength - totallong);
				sTradeID = padStringZero(sTradeID, fillnumber);
			} else { // debemos recomponer para que no pase de 16x, eliminando el GLS
				GLCS = "";
				// check and padding till 16x
				padLength = prefix.length() + GLCS.length();
				tradeIDlon = sTradeID.length();
				totallong = padLength + tradeIDlon;
				fillnumber = tradeIDlon + (TRNlength - totallong);
				sTradeID = padStringZero(sTradeID, fillnumber);
			}

			result = new String(prefix + GLCS + sTradeID);


		} catch (Exception e) {
            Log.error(SantanderSwiftUtil.class, "Something went wrong composing the TRN for BOMessage " + msg.getLongId() +
                    " and trade " + trade.getLongId(), e);
		}

		return result;
	}


	private static String getPrefixCM(String acuerdo) {

		String output = "";
		switch (acuerdo) {
		case IM_prefix:
			output = "IM";
			break;
		case CSA_prefix:
			output = "C";
			break;
		case OSLA_perfix:
			output = "O";
			break;
		case ISMA_prefix:
			output = "I";
			break;
		default:
			output = "C";
			break;
		}


		return output;
	}

	private static String getPrefixCT() {

		return CustomerTransfer_prefix;
	}

	private static String getPrefixNetting() {

		return NettingTransfer_prefix;
	}

	public static String getCorporateEvent_prefix() {
		return CorporateEvent_prefix;
	}

	public static String padStringZero(String input, int totalLegth) {
		String output = "";
		output = String.format("%" + totalLegth + "s", input).replace(" ", "0");
		return output;
	}


    public static String parseSANT_DETAILS_CHARGES(final BOMessage message, final Trade trade, final LEContact sender, final LEContact rec,
                                                   final Vector transferRules, final BOTransfer transfer, final String format, final DSConnection dsCon) {

		String str_details = null;
		if (transfer != null) {
			TradeTransferRule rule = transfer.toTradeTransferRule();
			SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(dsCon, rule.getCounterPartySDId());

			if (si != null) {
				str_details = si.getAttribute("Details_Of_Charges");
            } else str_details = "OUR";  //by default

        } else str_details = "OUR"; //by default

		// integrity checks (Error code(s): T08 . Only allowed OUR,BEN or SHA
        if (str_details == null) str_details = "OUR";

		if (!str_details.equals("OUR") && !str_details.equals("BEN") && !str_details.equals("SHA"))
			str_details = "OUR";


		return str_details;
	}


	public static String parseSANT_PO_DELIVERY_AGENT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
			Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) {

		String str_53a = "";
		// First: check if msg templates are MT103 or MT202 only for TGT2 Payments.
		String templateName = message.getTemplateName();
		if (templateName == null) {
			Log.debug(SwiftTextCustomizer.class,
					"SwiftTextCustomizer.parseSANT_PO_DELIVERY_AGENT: The template name of the message:"
                            + message.getLongId()
                            + " is null. Nothing added to message.");
			return str_53a;
		}

		if (templateName.equals("MT103") || templateName.equals("MT202") || templateName.contentEquals("MT202COV")) {
            //Second: retrieve CptySDI to check if transfer Method is Target2. If so, we add the 103:TGT field.
			// final String addressType=boMessage.getAddressMethod();

			try {
				BOTransfer transferencia = transfer;
				if (!PaymentsHubUtil.isPHOrigin(message) || !PaymentsHubUtil.hasCustomSDIs(transfer)) {
					transferencia = DSConnection.getDefault().getRemoteBO()
							.getBOTransfer(message.getTransferLongId());
				}

				TradeTransferRule rule = transferencia.toTradeTransferRule();
				SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(dsCon, rule.getProcessingOrgSDId());

				final String metodo = si.getSettlementMethod();

                if (metodo != null && ((metodo.contentEquals("SWIFT") || metodo.contentEquals("TARGET2")) ||
                        (transfer.getProductType().equalsIgnoreCase(Bond.class.getSimpleName()) &&
                            (metodo.contentEquals("CLEARSTREAM") || metodo.contentEquals("CREST") ||
                                    metodo.contentEquals("DTC") || metodo.contentEquals("EUROCLEAR") ||
                                    metodo.contentEquals("FED") || metodo.contentEquals("CREST") ||
                                    metodo.contentEquals("DTC") || metodo.contentEquals("FRANCE_DOMESTIC") ||
                                    metodo.contentEquals("IBERCLEAR_RF") || metodo.contentEquals("ITALY_DOMESTIC"))))) {
                     /*recuepramos BIC agente y cta. OLD FASHION WAY
                     StringBuilder output=new StringBuilder(str_53a);
                     output.append("/");
                     output.append(si.getAgent().getPartyAccountName());
                     output.append(CRLF);
                     output.append(si.getAgent().getCodeValue());
					 */

					// nuevo

					int agentID = si.getAgentId();
					PartySDI agente = si.getAgent();
					final String RolAgente = agente.getPartyRole();

					final String tipoContacto = agente.getPartyContactType();
					// final String contactType= si.getAgentContactType();

					final String productType = transfer.getProductType();
					final int processingOrgId = transfer.getProcessingOrg();

					// aqui necesito crear un LE que sea el agente..
					LegalEntity le = BOCache.getLegalEntity(dsCon, agentID);

                    final LEContact agentContact = BOCache.getContact(dsCon, RolAgente, le, tipoContacto, productType, processingOrgId);
					final String agent_bic = agentContact.getSwift();

                    //TO-DO mejora: sacar primero la cta y el BIC. Lo primero que se pone es la cta, y en la segunda linea el BIC.
                    //en caso de que alguno no exista, solo deberia enviar el que exista y sin la / primera al lado del 53a

					StringBuilder output = new StringBuilder(str_53a);
					output.append("/");
					output.append(si.getAgent().getPartyAccountName());
					// output.append(CRLF);
					output.append(SwiftMessage.END_OF_LINE);
					output.append(agent_bic);

					str_53a = output.toString();

					TagValue tate = new TagValue();

					tate.setTag(SwiftUtil.TAG_53);
					tate.setValue(str_53a);


					str_53a = tate.getValue();
				}

			} catch (CalypsoServiceException e) {
                Log.debug(SwiftTextCustomizer.class, "SwiftTextCustomizer.check103field:  Something went wrong ckecking the need to add 103 tag for TGT2: "
								+ e);
				return str_53a;

			}
		}

		return str_53a;

	}

	/**
	 * @param swiftMsgID
	 * @param dsConn
	 * @return
	 */
	public static SwiftMessage getSwiftMessage(long swiftMsgID, DSConnection dsConn) {
		SwiftMessage swiftMessage = new SwiftMessage();

		if (swiftMsgID != 0) {
			Log.info(SwiftTextCustomizer.class, "Loading advice document from message: " + swiftMsgID);
			Vector adviceDocuments = null;
			try {
                adviceDocuments = dsConn.getRemoteBackOffice().getAdviceDocuments("advice_document.advice_id=" + swiftMsgID, null, null);
			} catch (CalypsoServiceException e) {
				Log.error(SwiftTextCustomizer.class, "Cannot load Advice Documents for Swift message id " + swiftMsgID);
			}

			if (Util.isEmpty(adviceDocuments))
				Log.warn(SwiftTextCustomizer.class, "No Advice Documents found for Swift message id " + swiftMsgID);
			else {
				for (Object docObj : adviceDocuments) {
					AdviceDocument document = (AdviceDocument) docObj;
                    if (document != null
                            && document.getAddressMethod().equalsIgnoreCase(ADDRESS_METHOD_SWIFT)) {
                        Log.info(
                                SwiftTextCustomizer.class, "generate swiftMessage from Advice Document id: " + document.getAdviceId());

						if (!swiftMessage.parseSwiftText(document.getDocument().toString(), false)) {
							Log.error("", "Can't parse raw records from Triparty Margin Detail");
							return null;
						}
						return swiftMessage;
					}
				}
                Log.warn(
                        SwiftTextCustomizer.class,
                        " Advice documents found for Swift message id "
                                + swiftMsgID
						+ ", but none of them are Swift messages");
			}
		}
		return null;
	}

	/**
	 *
	 * @param message
	 * @param trade
	 * @param sender
	 * @param rec
	 * @param transferRules
	 * @param transfer
	 * @param format
	 * @param dsCon
	 * @return
	 */
	public static String parseSANT_ADDITIONAL_INFO(final BOMessage message, final Trade trade, final LEContact sender,
			final LEContact rec, final Vector transferRules, final BOTransfer transfer, final String format,
			final DSConnection dsCon) {

		if (null != trade && trade.getProduct() instanceof CA) {
			String additionalInfo = "/BNF/";
			String genericComment = getMessgaeGenericComment(message, "Pagos Swift Campo 72");
			if (genericComment != null && !genericComment.isEmpty()) {
				additionalInfo += genericComment;
			} else {
				CA ca = (CA) trade.getProduct();
				additionalInfo += " CLAIM "+ ca.getSecurity().getSecCode("ISIN");
			}
			return additionalInfo;
		}
		return "";
	}

	/**
	 *
	 * @param message
	 * @param trade
	 * @param transfer
	 * @param con
	 * @return
	 */
    public static String parseSANT_PO_BENEFICIARY(BOMessage message, Trade trade, BOTransfer transfer, DSConnection con) {
		if (transfer != null) {
			int ordererId = transfer.getInternalLegalEntityId();
			int poId = transfer.getOrdererProcessingOrg();
            String value = SwiftUtil.getSwiftCode(ordererId, "ProcessingOrg", message.getSenderContactType(), message.getProductType(), poId, trade, transfer, message, con);
			if (value != null && !value.equals("UNKNOWN")) {
				return value;
			} else {
                value = SwiftUtil.getSwiftLongAddress(ordererId, (String)null, "ProcessingOrg", message.getSenderContactType(), message.getProductType(), poId, trade, transfer, message, con, (Object)null);
				return value;
			}
		} else {
			TagValue tagValue = SwiftUtil.getPoTagValue("BENEFICIARY", trade, transfer, message, false, con);
			return tagValue.getValue();
		}
	}

	/**
	 * 
	 * @param message
	 * @param commentType
	 * @return Generic Comment of the message for defined commentType
	 */
	public static String getMessgaeGenericComment(BOMessage message, String commentType) {
		try {
			StringBuilder query = new StringBuilder();
			query.append("OBJECT_CLASS = 'Message' ");
			query.append("AND OBJECT_ID='" + message.getLongId() + "'");
			query.append("AND COMMENT_TYPE='" + commentType + "'");
			
			@SuppressWarnings("unchecked")
			Vector<GenericComment> comments = (Vector<GenericComment>) DSConnection.getDefault().getRemoteBackOffice()
					.getGenericComments(null, query.toString(), null, null);
			if (!comments.isEmpty()) {
				return comments.get(0).getComment();
			}
		} catch (CalypsoServiceException e) {
			Log.error(SantanderSwiftUtil.class, e);
		}
		return null;
	}
	public static ExternalMessage toExternalMessage(BOMessage msg ) throws MessageParseException, CalypsoServiceException {
		return toExternalMessage(msg, DSConnection.getDefault());
	}
	public static ExternalMessage toExternalMessage(BOMessage msg, DSConnection ds) throws MessageParseException, CalypsoServiceException {
		AdviceDocument swift = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(msg.getLongId(), null);
		ExternalMessageParser parser = SwiftParserUtil.getParser(msg.getFormatType());
		return parser.readExternal(reformatSwift(swift.getTextDocument().toString()), msg.getGateway());
	}

	private static String reformatSwift(String swiftText) {
		String[] lines = swiftText.split(EOL_PATTERN);
		if (!Util.isEmpty(lines)) {
			StringBuilder sb = new StringBuilder();
			for (String line : lines) {
				sb.append(line).append(SwiftMessage.END_OF_LINE);
			}
			return sb.toString();
		}
		return "";
	}

}
