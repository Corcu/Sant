/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.refdata;

import java.awt.Frame;
import java.util.List;
import java.util.Vector;

import com.calypso.apps.refdata.AccountValidator;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountStatementConfig;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class CustomAccountValidator implements AccountValidator {

	public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
	public static final String PAY_INTEREST_ONLY = "PayInterestOnly";

	public static final String SANT_ACCSTATEMENT_MSGCONFIGID = "SANTAccStatement_MsgConfigID";
	public static final String SANT_ACCSTATEMENT_DATERULE = "SANTAccStatement_DateRule";
	public static final String ACTIVE_STATUS = "Active";
	public static final String PENDING_STATUS = "Pending";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean isValidInput(Account account, Frame frame, Vector messages) {

		if (!account.getCallAccountB()) {
			return true;
		}

		// Hard coded value - always false
		account.setAccountProperty(PAY_INTEREST_ONLY, "False");

		// set default Statement config to the Call Account
		setCustomStatementConfig(account, messages);

		return isValidContractID(account, frame, messages);

	}

	public boolean isValidContractID(Account account, Frame frame, Vector<String> messages) {

		String mccIdStr = account.getAccountProperty(MARGIN_CALL_CONTRACT);

		if (Util.isEmpty(mccIdStr)) {
			messages.add("Contract id must be specified");
			return false;
		}

		CollateralConfig marginCallConfig = null;
		try {
			int mccId = Integer.parseInt(account.getAccountProperty(MARGIN_CALL_CONTRACT));
			marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
			if (marginCallConfig == null) {
				messages.add("Invalid contract id specified");
				return false;
			}
		} catch (Exception exc) {
			Log.info(this, exc); //sonar
			messages.add("Invalid contract id specified");
			return false;
		}

		// Check if PO & LE of Call Account matches to contracts
		int accLeId = account.getLegalEntityId();
		// START OA 27/11/2013 NPE fix
		if (marginCallConfig.getLeId() != accLeId) {
			if (marginCallConfig.getAdditionalLEIds() != null) {
				if ((!marginCallConfig.getAdditionalLEIds().contains(accLeId))) {
					messages.add("The account LE should match with the contract LE or one of the additional LE");
					return false;
				}
			} else {
				messages.add("The account LE should match with the contract LE or one of the additional LE");
				return false;
			}
		}

		int accPoId = account.getProcessingOrgId();
		if (marginCallConfig.getPoId() != accPoId) {
			if (marginCallConfig.getAdditionalPOIds() != null) {
				if (!marginCallConfig.getAdditionalPOIds().contains(accPoId)) {
					messages.add("The account PO should match with contract PO or one of the additional PO");
					return false;
				}
			} else {
				messages.add("The account PO should match with contract PO or one of the additional PO");
				return false;
			}
		}
		// END OA 27/11/2013 NPE fix

		// Check other account with same margin call contract and currency.
		if (isActiveStatus(account)) {
			List<Account> accList = BOCache.getAccountByAttribute(DSConnection.getDefault(), MARGIN_CALL_CONTRACT, mccIdStr);
			if (!Util.isEmpty(accList)) {
				for (Account accCheck : accList) {
					if (accCheck.getId() != account.getId()) {
						// only interested in live accounts
						if (isActiveStatus(accCheck)) {
							if  (accCheck.getCurrency().equals(account.getCurrency())) {
								messages.add("There is already an account with same  MARGIN_CALL_CONTRACT / Currency.");
								return false;
							}
						}
					}
				}
			}
		}

		return true;
	}

	private boolean isActiveStatus(Account account) {
		return ACTIVE_STATUS.equals(account.getAccountStatus())
				|| PENDING_STATUS.equals(account.getAccountStatus());
	}

	/**
	 *
	 * For Call Accounts it is mandatory to set Statement config. But we don't use statement config and is laborious to
	 * set it for all call accounts from the gui.
	 *
	 * This method sets the default dummy Statement config as configured in the domain if there is no statement config
	 * already set.
	 *
	 * @param account
	 * @throws Exception
	 */
	private void setCustomStatementConfig(Account account, Vector<String> messages) {

		if (account.getCallAccountB() && Util.isEmpty(account.getStatementConfigs())) {
			try {
				@SuppressWarnings("rawtypes")
				Vector dateRuleVect = DSConnection.getDefault().getRemoteReferenceData()
						.getDomainValues(SANT_ACCSTATEMENT_DATERULE);
				@SuppressWarnings("rawtypes")
				Vector mesageConfigIdVect = DSConnection.getDefault().getRemoteReferenceData()
						.getDomainValues(SANT_ACCSTATEMENT_MSGCONFIGID);

				// if both domain values are empty then we dont set anything
				if (Util.isEmpty(dateRuleVect) && Util.isEmpty(mesageConfigIdVect)) {
					return;
				} else if (Util.isEmpty(dateRuleVect)) {
					messages.add("Please set a dateRule for CallAccounts StatementConfig under domain "
							+ SANT_ACCSTATEMENT_DATERULE);
					return;
				} else if (Util.isEmpty(mesageConfigIdVect)) {
					messages.add("Please set a dateRule for CallAccounts StatementConfig under domain "
							+ SANT_ACCSTATEMENT_MSGCONFIGID);
					return;
				}

				// check if date rule is valid
				DateRule dateRule = DSConnection.getDefault().getRemoteReferenceData()
						.getDateRule((String) dateRuleVect.get(0));
				if (dateRule == null) {
					messages.add("Date rule configured under domain " + SANT_ACCSTATEMENT_DATERULE + " doesn't exist.");
					return;
				}

				// check if message config id is valid
				AdviceConfig adviceConfig = DSConnection.getDefault().getRemoteReferenceData()
						.getAdviceConfig(Integer.parseInt((String) mesageConfigIdVect.get(0)));

				if (adviceConfig == null) {
					messages.add("Message config configured under domain " + SANT_ACCSTATEMENT_MSGCONFIGID
							+ " is not valid.");
					return;
				}

				Vector<AccountStatementConfig> statementConfigVect = new Vector<AccountStatementConfig>();
				AccountStatementConfig statementConfig = new AccountStatementConfig();
				statementConfigVect.add(statementConfig);

				statementConfig.setAccountId(account.getId());
				statementConfig.setPositionCashSecurityFlag("Cash");
				statementConfig.setPositionClass("Client");
				statementConfig.setPositionType("Actual");
				statementConfig.setPositionDateType("Settle");
				statementConfig.setPositionValue("Quantity");
				statementConfig.setIsPaymentB(true);
				statementConfig.setNoMovementB(true);
				statementConfig.setZeroBalanceB(true);
				statementConfig.setType("Default");
				statementConfig.setAdviceConfigId(adviceConfig.getId());

				statementConfig.setFrequencyDateRule(dateRule);

				account.setStatementConfigs(statementConfigVect);
				account.setStatementB(true);
			} catch (Exception exc) {
				Log.error(CustomAccountValidator.class, exc);
				messages.add("Error setting statement config to the call account.");
			}
		}
	}

}
