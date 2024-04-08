package calypsox.tk.util.korea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.accounting.keyword.KeywordUtil;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.lock.MultiLock;
import com.calypso.tk.lock.ReentrantLock;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.PartySDI;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import calypsox.tk.swift.formatter.MT535KoreaSWIFTFormatter;
import calypsox.util.binding.CustomBindVariablesUtil;

public class KoreaMT535Helper {

	private static final String POS_RECON = "POS_RECON";
	private static final String SWIFT = "SWIFT";
	private static final String PROCESSING_ORG = "ProcessingOrg";
	private static final String QUANTITY = DomainValues.comment("KOREA.MT535fields", "Quantity");
	private static final String SECURITY = DomainValues.comment("KOREA.MT535fields", "Security");
	private static final String XFER_ATTRIBUTE = "XferAgentAccount";

	/**
	 * Method that receives the excel data.
	 * 
	 * @param fileValues
	 * @return true if the all the positions were processed successfully.
	 */
	public Boolean processExternalPositions(Map<String, LinkedList<HashMap<String, String>>> fileValues) {

		boolean result = true;
		for (Map.Entry<String, LinkedList<HashMap<String, String>>> entry : fileValues.entrySet()) {

			String key = entry.getKey();
			String[] inf = key.split("-");
			if (inf.length == 6) {
				result &= generateMT535Message(inf[0], inf[1], inf[2], inf[3], inf[4], inf[5], entry.getValue());
			}
		}
		return result;
	}

	/**
	 * Create the MT535 message al process the external position
	 * 
	 * @param strDate         position date
	 * @param pledgordAccount pledgord account
	 * @param pledgordName    pledgord name
	 * @param pledgeeAccount  pledgee account
	 * @param pledgeeName     pledgee name
	 * @param contractId      contract id
	 * @param data            Content of the excel file
	 * @return true if the external positions were processed successfully
	 */
	public Boolean generateMT535Message(String strDate, String pledgordAccount, String pledgordName,
			String pledgeeAccount, String pledgeeName, String citiContractId, LinkedList<HashMap<String, String>> data) {

		DSConnection dsCon = DSConnection.getDefault();
		Boolean result = false;
		BOMessage message = new BOMessage();
		SwiftMessage swiftMessage = null;
		PricingEnv env = null;
		JDate date = null;
		date = getDate(strDate);
		int contractId = 0;

		SettleDeliveryInstruction sdiReceiver = getSDI(pledgeeAccount, pledgeeName, PROCESSING_ORG, date);
		SettleDeliveryInstruction sdiSender = getSDI(pledgordAccount, pledgordName, "CounterParty", date);

		if (sdiReceiver == null || sdiSender == null) {
			return result;
		}
		contractId = getContractId(sdiReceiver.getBeneficiaryId(), sdiSender.getBeneficiaryId(), citiContractId,pledgordName,pledgeeName);
		if(contractId == 0) {
			return result;
		}
		// Message fields to generate el external position
		message.setMessageType(POS_RECON);
		message.setTransferLongId(0);
		message.setTradeLongId(0);
		message.setAction(Action.NEW);
		message.setSubAction(Action.NONE);
		message.setProductType("ALL");
		message.setGateway(SWIFT);
		message.setAddressMethod(SWIFT);

		message.setSenderRole("Agent");
		message.setReceiverRole(PROCESSING_ORG);

		message.setReceiverId(sdiReceiver.getProcessingOrgBasedId());
		message.setSenderId(sdiSender.getAgentId());
		message.setReceiverContactType(sdiReceiver.getBeneficiaryContactType());
		message.setSenderContactType(sdiSender.getAgentContactType());
		message.setReceiverAddressCode(getReceiverAddresCode(sdiReceiver, dsCon));
		message.setSenderAddressCode(getSenderAddresCode(sdiSender, dsCon));
		message.setTemplateName("MT535Korea");
		message.setCreationDate(JDatetime.currentTimeValueOf(date, TimeZone.getDefault()));

		// Datos necesarios para generar MT353
		message.setSettleDate(date);
		message.setAttribute("SequenceNumber", String.valueOf(data.size()));
		message.setAttribute("SequenceInfo", "ONLY");
		message.setAttribute("Account", getAccount(sdiReceiver, sdiSender, Integer.toString(contractId), dsCon));
		message.setAttribute("ExternalPosition", getExternalPosition(data));

		

		try {
			MT535KoreaSWIFTFormatter sf = new MT535KoreaSWIFTFormatter();
			env = getPricingEvn(date);
			swiftMessage = sf.generateSwift(env, message, dsCon);
			swiftMessage.setIncoming(true); // Needed to specify that the receiver is the PO

			result = processExternalMT535Message(swiftMessage, env, dsCon);

		} catch (MessageFormatException e) {
			Log.error(this, "Error generating MT535 swift message");
		}

		return result;

	}

	/**
	 * Obtain the preferred SDI having count the parameters provided.
	 * 
	 * @param acc             Agent account field of the SDI
	 * @param beneficiaryName Beneficiary name of the SDI
	 * @param role            Role of the SDI
	 * @param date            Date where the SDI have to be active
	 * @return The preferred SDI.
	 */
	@SuppressWarnings("unchecked")
	private SettleDeliveryInstruction getSDI(String acc, String beneficiaryName, String role, JDate date) {

		StringBuilder sbWhere = new StringBuilder();
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(role);
		SettleDeliveryInstruction sdi = null;

		sbWhere.append(" LE_ROLE = ? ");
		sbWhere.append("AND TRIM(BENE_NAME) = ? AND TRIM(AGENT_ACCOUNT) = ? ");
		sbWhere.append("AND(EFFECTIVE_FROM IS NULL OR EFFECTIVE_FROM <= ?) ");
		sbWhere.append("AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO > ? ) ");
		sbWhere.append("AND PRODUCT_LIST = ? ");

		CustomBindVariablesUtil.addNewBindVariableToList(beneficiaryName.trim(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(acc.trim(), bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(date, bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(date, bindVariables);
		CustomBindVariablesUtil.addNewBindVariableToList(Product.MARGINCALL, bindVariables);
		try {

			Vector<SettleDeliveryInstruction> sDIs = DSConnection.getDefault().getRemoteReferenceData()
					.getSettleDeliveryInstructions("", sbWhere.toString(), bindVariables);

			if (sDIs != null && !sDIs.isEmpty()) {
				// Order the sdis by preferred and priority
				sDIs.sort(Comparator.comparing(SettleDeliveryInstruction::getPreferredB)
						.thenComparing(SettleDeliveryInstruction::getPriority));
				sdi = sDIs.get(0); // Gets the sdi with more priority

			} else {
				List<CalypsoBindVariable> bindVariables2 = CustomBindVariablesUtil.createNewBindVariable(role);
				// Search by Product Type: ALL
				sbWhere = new StringBuilder();
				sbWhere.append(" LE_ROLE = ? ");
				sbWhere.append("AND BENE_NAME = ? AND AGENT_ACCOUNT = ? ");
				sbWhere.append(" AND (EFFECTIVE_FROM IS NULL OR EFFECTIVE_FROM <= ?) ");
				sbWhere.append("AND (EFFECTIVE_TO IS NULL OR EFFECTIVE_TO > ? ) ");
				sbWhere.append("AND PRODUCT_LIST IS NULL ");

				CustomBindVariablesUtil.addNewBindVariableToList(beneficiaryName, bindVariables2);
				CustomBindVariablesUtil.addNewBindVariableToList(acc, bindVariables2);
				CustomBindVariablesUtil.addNewBindVariableToList(date, bindVariables2);
				CustomBindVariablesUtil.addNewBindVariableToList(date, bindVariables2);

				sDIs = DSConnection.getDefault().getRemoteReferenceData()
						.getSettleDeliveryInstructions(sbWhere.toString(), bindVariables2);

				if (sDIs != null && !sDIs.isEmpty()) {
					// Order the sdis by preferred and priority
					sDIs.sort(Comparator.comparing(SettleDeliveryInstruction::getPreferredB)
							.thenComparing(SettleDeliveryInstruction::getPriority));
					sdi = sDIs.get(0); // Gets the sdi with more priority

				} else {
					Log.error(this, "Cannot obtain SDI");
				}

			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting SDIs", e);
		}

		return sdi;
	}

	/**
	 * Obtains the account name of the account where the external position should be
	 * stored. Cheks if the account exists and creates it if necessary.
	 * 
	 * @param sdi        Processing org SDI
	 * @param sdiSender  Agent SDI
	 * @param contractId Collateral contract number
	 * @param dsCon      ds connection
	 * @return Account name
	 */
	private String getAccount(SettleDeliveryInstruction sdi, SettleDeliveryInstruction sdiSender, String contractId,
			DSConnection dsCon) {
		int accountId = sdi.getGeneralLedgerAccount();
		String accountName = "";
		Account account = null;
		if (accountId != 0) {
			account = BOCache.getAccount(dsCon, accountId);
			if (account != null) {
				accountName = getAccountName(account, contractId, sdiSender, sdi, dsCon);
				checkAccount(account, accountName, account.getProcessingOrgBasedId(), account.getCurrency(), dsCon);

			}

		}
		return accountName;
	}

	/**
	 * Obtain the automatic account name where the position should be added. Uses
	 * the account attributes of the parent account.
	 * 
	 * @param account     parent account
	 * @param contractId  collateral contract id
	 * @param sdiSender   Agent SDI
	 * @param sdiReceiver ProcesingOrg SDI
	 * @param dsCon       ds connection
	 * @return the account name
	 */
	private String getAccountName(Account account, String contractId, SettleDeliveryInstruction sdiSender,
			SettleDeliveryInstruction sdiReceiver, DSConnection dsCon) {
		BOTransfer transfer = new BOTransfer();
		Trade trade = new Trade();
		trade.addKeyword("MC_CONTRACT_NUMBER", contractId);
		transfer.setExternalLegalEntityId(sdiSender.getBeneficiaryId());
		transfer.setInternalLegalEntityId(sdiReceiver.getProcessingOrgBasedId());
		return KeywordUtil.fillAccount(null, account, trade, transfer, null, dsCon, null, new Vector<String>());
	}

	/**
	 * Get the SWIFT BIC code of the ProcessingOrg.
	 * 
	 * @param sdi   ProcessingOrg SDI
	 * @param dsCon ds connection
	 * @return SWIFT BIC code
	 */
	private String getReceiverAddresCode(SettleDeliveryInstruction sdi, DSConnection dsCon) {

		String receiverAddr = "";
		LegalEntity lePO = BOCache.getLegalEntity(DSConnection.getDefault(), sdi.getProcessingOrgBasedId());
		if (lePO != null) {
			LEContact contact = BOCache.getContact(dsCon, PROCESSING_ORG, lePO, sdi.getBeneficiaryContactType(),
					sdi.getProductList().toString(), sdi.getProcessingOrgBasedId());
			if (contact != null) {
				receiverAddr = contact.getSwift();
			} else {
				// Search a contact by processing org ALL
				contact = BOCache.getContact(dsCon, PROCESSING_ORG, lePO, sdi.getBeneficiaryContactType(),
						sdi.getProductList().toString(), 0);
				if (contact != null) {
					receiverAddr = contact.getSwift();
				}
			}
		}

		return receiverAddr;
	}

	/**
	 * Get the SWIFT BIC code of the Agent.
	 * 
	 * @param sdi   agent SDI
	 * @param dsCon ds connection
	 * @return
	 */
	private String getSenderAddresCode(SettleDeliveryInstruction sdi, DSConnection dsCon) {

		String senderAddr = "";
		PartySDI agent = sdi.getAgent();
		if (agent != null) {
			LEContact contact = null;
			LegalEntity leAgent = BOCache.getLegalEntity(DSConnection.getDefault(), agent.getPartyId());
			if (leAgent != null) {
				try {
					contact = BOCache.getContact(DSConnection.getDefault(), "Agent", leAgent, sdi.getAgentContactType(),
							sdi.getProductList().toString(), sdi.getProcessingOrgBasedId());
				} catch (Exception ex) {
					Log.error(this, "Can not get the sender contact", ex);
				}
			}
			if (contact != null) {
				senderAddr = contact.getSwift();
			} else {
				// Search a contact by processing org ALL
				contact = BOCache.getContact(dsCon, PROCESSING_ORG, leAgent, sdi.getBeneficiaryContactType(),
						sdi.getProductList().toString(), 0);
				if (contact != null) {
					senderAddr = contact.getSwift();
				}
			}

		}
		return senderAddr;
	}

	/**
	 * Obtain the Pricing Enviroment
	 * 
	 * @param date date of the pricnign enviroment
	 * @return Pricing enviroment
	 */
	private PricingEnv getPricingEvn(JDate date) {
		try {
			return DSConnection.getDefault().getRemoteMarketData().getPricingEnv(
					DSConnection.getDefault().getDefaultPricingEnv(), new JDatetime(date, TimeZone.getDefault()));
		} catch (CalypsoServiceException e) {
			Log.error(this, "Cannot get the Princing Enviromet", e);
			return null;
		}
	}

	/**
	 * Converts the String format date in JDate format.
	 * 
	 * @param strDate date in String format YYYYMMDD
	 * @return Jdate
	 */
	private JDate getDate(String strDate) {
		int year = Integer.parseInt(strDate.substring(0, 4));
		int month = Integer.parseInt(strDate.substring(4, 6));
		int day = Integer.parseInt(strDate.substring(6, 8));
		return JDate.valueOf(year, month, day);
	}

	/**
	 * This method gets the SECURITY-QUANTITY pair and creates a formatted string to
	 * pass to the Iterator class. In this way iterator can be fed to save all the
	 * positions of the same contract in a single MT535.
	 * 
	 * @param data excel content for the same position.
	 * @return formated string to pass to the Iterator class.
	 */
	private String getExternalPosition(LinkedList<HashMap<String, String>> data) {

		ArrayList<String> position = new ArrayList<>();
		for (HashMap<String, String> row : data) {
			position.add(row.get(SECURITY) + "-" + row.get(QUANTITY));
		}
		return String.join(";", position);
	}

	/**
	 * Check if the account has the Property XferAgentAccount properly setted. If
	 * not, set the value property an save the account. If the account does not exist yet, create it.
	 * 
	 * @param referenceAcc automatic parent account
	 * @param acc          name of the automatic child account
	 * @param poId         processing org id
	 * @param currency     currency of the account
	 * @param dsCon
	 */
	private void checkAccount(Account referenceAcc, String acc, int poId, String currency, DSConnection dsCon) {

		Account account = BOCache.getAccount(dsCon, acc, poId, currency);
		if (account != null) {
			String xferAgent = account.getAccountProperty(XFER_ATTRIBUTE);
			if (xferAgent == null || xferAgent.isEmpty() || !xferAgent.equals(acc)) {
				Account cloneAcc = (Account) account.clone();
				cloneAcc.setAccountProperty(XFER_ATTRIBUTE, acc);
				try {
					dsCon.getRemoteAccounting().save(cloneAcc);
				} catch (CalypsoServiceException e) {
					e.printStackTrace();
				}

			}
		} else {
			createAtomaticAccount(referenceAcc, acc, dsCon);
		}

	}

	/**
	 * Creates the automatic child account.
	 * 
	 * @param account     automatic parent account
	 * @param accountName automatic child account name
	 * @param dsCon       ds connection
	 */
	private void createAtomaticAccount(Account account, String accountName, DSConnection dsCon) {

		if (!account.getAutomaticB() || (account.getAutomaticB() && account.getAccEngineOnlyB())) {
			Log.error(this, "Can not create the automatic account");
			return;
		}

		Account newAccount = null;
		int poId = 0;
		poId = account.getProcessingOrgId();
		poId = account.getAutoPO(poId);
		String currency = account.getCurrency();

		newAccount = BOCache.getAccount(dsCon, accountName, poId, currency);
		if (newAccount == null) {

			Account acc = KeywordUtil.createFromOriginalAccount(account, accountName, currency, poId, null, dsCon);

			acc.setAccountProperty(XFER_ATTRIBUTE, accountName);

			ReentrantLock accountLock = null;

			try {
				accountLock = MultiLock.get(account);
				accountLock.acquire();

				Account accountCheck = BOCache.getAccount(dsCon, accountName, poId, currency);

				if (accountCheck == null) {

					dsCon.getRemoteAccounting().save(acc, false);

				}
			} catch (Exception e) {
				Log.error(this, "Can not create the new account", e);

			} finally {
				MultiLock.release(account);
			}
		}
	}

	/**
	 * Method that process the formed MT535 message and save the external position.
	 * @param swiftMessage MT535 message
	 * @param env pricing environment
	 * @param dsCon ds con object
	 * @return true if the position has been correctly imported.
	 */
	private boolean processExternalMT535Message(SwiftMessage swiftMessage, PricingEnv env, DSConnection dsCon) {
		
		try {

			ExternalMessageHandler handler = SwiftParserUtil.getHandler(SWIFT, swiftMessage.getType(), true);
			if (handler != null) {
				return handler.handleExternalMessage(swiftMessage, env, null, null, dsCon, null);
			} else {
				return SwiftParserUtil.processExternalMessage(swiftMessage, env, (PSEvent) null, (String) null,
						dsCon, (Object) null);
			}
	
		} catch (MessageParseException e) {
			Log.error(this, "Can not proccess the MT535 message", e);
			return false;
		}
	}
	
	/**
	 * Obtain the Collateral contract Id by the ProcessingOrg Id, the LE id and the
	 * cibibank contract id if necessary.
	 * @param poId           processing org id
	 * @param leId           legal entity id
	 * @param citiContractId CITIBANK contract id
	 * @return the collateral contract id
	 */
	private int getContractId(int poId, int leId, String citiContractId,String pledgordName, String pledgeeName) {
		int contractId = 0;

		try {
			List<CollateralConfig> mcList = ServiceRegistry.getDefault(DSConnection.getDefault())
					.getCollateralDataServer().getAllMarginCallConfig(poId, leId);
			if (mcList == null || mcList.isEmpty()) {
				Log.error(this, "There is not any Collateral contract for the PO:" + pledgeeName
						+ " and the Counterparty: " + pledgordName);
				return 0;
			}
			mcList = mcList.stream().filter(mc -> mc.getAdditionalField("MT599_Management") != null
					&& mc.getAdditionalField("MT599_Management").equals("true")).collect(Collectors.toList());
			if (mcList.size() == 1) {
				contractId = mcList.get(0).getId();
			} else if (mcList.size() > 1) {
				mcList = mcList.stream()
						.filter(mc -> mc.getAdditionalField("CITIBANK_ID") != null
								&& mc.getAdditionalField("CITIBANK_ID").equals(citiContractId))
						.collect(Collectors.toList());
				if (mcList.size() == 1) {
					contractId = mcList.get(0).getId();
				} else {
					Log.error(this,
							"Can not obtain the Collateral contract for the PO:" + pledgeeName
									+ " and the Counterparty: " + pledgordName
									+ " and the CITIBANK contract Id: " + citiContractId);
				}

			} else {
				Log.error(this,
						"There is not any collateral contract with the MT599_Management addtional field set to true "
								+ "for the PO:" + pledgeeName + " and the Counterparty: "
								+ pledgordName);
			}

		} catch (Exception e) {
			Log.error(this, "Error getting the Margin Call Config ID", e);
		}

		return contractId;
	}
}
