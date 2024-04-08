package calypsox.tk.bo.accounting.accountingcurrencyposition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOPostingUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.BalancePosition;
import com.calypso.tk.bo.accounting.accountingcurrencyposition.AccountingConversionUtil.IsGainForFxProcessVisitorI;
import com.calypso.tk.bo.accounting.keyword.KeywordUtil;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.ProductDesc;
import com.calypso.tk.core.SortShell;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountTranslation;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PostingArray;

/**
 * adaptation of core class AccountingConversionUtil
 * @author CedricAllain
 *
 */
public class SantAccountingConversionUtil {

	public static String LOG_CATEGORY = "SantAccountingConversionUtil";

	HashMap<String, Account> accountCache = new HashMap<String, Account>();
	
	public static final String ATTRIBUTE_FX_TRANSLATION_ONLY = "FxTranslationOnly"; 

	@SuppressWarnings({ "deprecation", "unchecked" })
	public BOPosting createPostingForFXProcess(Account account, JDate valDate, BalancePosition bal, String functionCcy,
			boolean fxGain, int poId, String eventType, double postAmount, double otherAmount, String accountPlusDebit,
			String accountPlusCredit, String accountMinusDebit, String accountMinusCredit,
			IsGainForFxProcessVisitorI isGainForFxProcessVisitor, String stType,
			List<String> propagationExcludedAttributeList) {

		// if the account attribute FXtranslation base is set to true force currency to EUR for account
		boolean fxTranslationBase = Util.isTrue(account.getAccountProperty("FxTranslationBase"));
		boolean fxTranslation = Util.isTrue(account.getAccountProperty("FxTranslation"));

		BOPosting posting = new BOPosting();
		posting.setAmount(postAmount);

		posting.setOtherAmount(otherAmount);
		posting.setEffectiveDate(valDate);
		posting.setEventType(eventType);
		
		// add trade id in description and valDate instead of balance date
		posting.setDescription(bal.getTradeLongId() + "/" + account.getId() + "/" + bal.getCurrency() + "/" + valDate);

		if (account.getOriginalAccountId() != 0) {
			Account original = BOCache.getAccount(DSConnection.getDefault(), account.getOriginalAccountId());
			String str = getAutoAttributeValue("Book", original, account);

			if (!Util.isEmptyString(str)) {
				Book book = BOCache.getBook(DSConnection.getDefault(), str);
				if (book != null) {
					posting.setBookId(book.getId());
				} else {
					posting.setBookId(0);
				}
			}
			str = getAutoAttributeValue("ProductId", original, account);

			if (!Util.isEmptyString(str)) {
				int id = Integer.parseInt(str);
				posting.setProductId(id);
			} else {
				str = getAutoAttributeValue("ProductDesc", original, account);

				List<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
				if (!Util.isEmptyString(str)) {
					Vector<ProductDesc> productDesc = null;
					try {
						bindVariables.add(new CalypsoBindVariable(12, str));
						String where = "description = ? ";

						productDesc = DSConnection.getDefault().getRemoteProduct().getAllProductDesc(where,
								bindVariables);
						if (productDesc != null && productDesc.size() > 1) {
							Log.error(LOG_CATEGORY, "There are multiple product descriptions for " + str);

							return null;
						}
					} catch (Exception e) {
						Log.error(LOG_CATEGORY, e);
					}
					if (productDesc != null && (ProductDesc) productDesc.elementAt(0) != null) {
						ProductDesc desc = (ProductDesc) productDesc.elementAt(0);
						int id = desc.getId();
						posting.setProductId(id);
					}
				}
			}
		}
		posting.setPostingType("FX_CONVERSION");
		posting.addAttribute("ORIGINAL_CCY", bal.getCurrency());
		posting.setOriginalEventType(stType);
		posting.setAccountingRuleId(0);
		posting.setCreationDate(new JDatetime());
		posting.setBookingDate(valDate);
		posting.setSentDate(null);
		posting.setStatus("NEW");
		posting.setId(0L);
		// add trade id in the posting
		posting.setTradeLongId(bal.getTradeLongId());
		posting.setLinkedId(0L);
		posting.setTransferLongId(0L);
		posting.setSubId(0L);
		String str = null;
		Account acc = null;

		boolean gain = isGainForFxProcessVisitor.isGainForFxProcess(account, valDate, bal, functionCcy, fxGain, poId,
				eventType);

		if (gain) {
			str = account.getAccountProperty(accountPlusDebit);
			acc = getAccount(str, poId, functionCcy);
			if (acc == null) {
				if (str.equals(account.getName())) {
					acc = account;
				} else {

					Log.error(LOG_CATEGORY,
							"Cannot find Account " + str + " for PO ID " + poId + " and currency " + functionCcy);

					return null;
				}
			}
			if (acc.getAutomaticB()) {
				Account auto = getAutoAccount(account, acc, propagationExcludedAttributeList,
						fxTranslationBase ? functionCcy : null);
				if (auto == null) {
					Log.error(LOG_CATEGORY, "Could not find automatic account for " + acc.getName());
					return null;
				}
				acc = auto;
			}

			posting.setDebitAccountId(acc.getId());
			if (Log.isCategoryLogged("ACCOUNTING_CONVERSION")) {
				Log.debug("ACCOUNTING_CONVERSION", accountPlusDebit + ": " + acc.toString());
			}

			str = account.getAccountProperty(accountPlusCredit);
			acc = getAccount(str, poId, functionCcy);
			if (acc == null) {
				if (str.equals(account.getName())) {
					acc = account;
				} else {

					Log.error(LOG_CATEGORY,
							"Cannot find Account " + str + " for PO ID " + poId + " and currency " + functionCcy);

					return null;
				}
			}
			if (acc.getAutomaticB()) {
				Account auto = getAutoAccount(account, acc, propagationExcludedAttributeList,
						fxTranslationBase ? functionCcy : null);
				if (auto == null) {
					Log.error(LOG_CATEGORY, "Could not find automatic account for " + acc.getName());
					return null;
				}
				acc = auto;
			}

			posting.setCreditAccountId(acc.getId());
			if (Log.isCategoryLogged("ACCOUNTING_CONVERSION")) {
				Log.debug("ACCOUNTING_CONVERSION", accountPlusCredit + ": " + acc.toString());
			}
		} else {

			str = account.getAccountProperty(accountMinusDebit);
			acc = getAccount(str, poId, functionCcy);
			if (acc == null) {
				if (str.equals(account.getName())) {
					acc = account;
				} else {

					Log.error(LOG_CATEGORY,
							"Cannot find Account " + str + " for PO ID " + poId + " and currency " + functionCcy);

					return null;
				}
			}
			if (acc.getAutomaticB()) {
				Account auto = getAutoAccount(account, acc, propagationExcludedAttributeList,
						fxTranslationBase ? functionCcy : null);
				if (auto == null) {
					Log.error(LOG_CATEGORY, "Could not find automatic account for " + acc.getName());
					return null;
				}
				acc = auto;
			}

			posting.setDebitAccountId(acc.getId());
			if (Log.isCategoryLogged("ACCOUNTING_CONVERSION")) {
				Log.debug("ACCOUNTING_CONVERSION", accountMinusDebit + ": " + acc.toString());
			}

			str = account.getAccountProperty(accountMinusCredit);
			acc = getAccount(str, poId, functionCcy);
			if (acc == null) {
				if (str.equals(account.getName())) {
					acc = account;
				} else {

					Log.error(LOG_CATEGORY,
							"Cannot find Account " + str + " for PO ID " + poId + " and currency " + functionCcy);

					return null;
				}
			}
			if (acc.getAutomaticB()) {
				Account auto = getAutoAccount(account, acc, propagationExcludedAttributeList,
						fxTranslationBase ? functionCcy : null);
				if (auto == null) {
					Log.error(LOG_CATEGORY, "Could not find automatic account for " + acc.getName());
					return null;
				}
				acc = auto;
			}

			posting.setCreditAccountId(acc.getId());
			if (Log.isCategoryLogged("ACCOUNTING_CONVERSION")) {
				Log.debug("ACCOUNTING_CONVERSION", accountMinusCredit + ": " + acc.toString());
			}
		}
		posting.setCurrency(functionCcy);
		posting.setMatchingProcess(true);
		
		if(fxTranslation && ! fxTranslationBase)
			posting.addAttribute(ATTRIBUTE_FX_TRANSLATION_ONLY, "true");
		
		return posting;
	}

	protected static Account getAutoAccount(Account balanceAcc, Account account,
			List<String> propagationExcludedAttributeList) {
		return getAutoAccount(balanceAcc, account, propagationExcludedAttributeList);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	protected static Account getAutoAccount(Account balanceAcc, Account account,
			List<String> propagationExcludedAttributeList, String forcedAccountNameCcy) {
		try {
			account = (Account) account.clone();
			// if the currency is force change the balanceAcc currency (for auto acct creation with forced ccy)
			if (forcedAccountNameCcy != null) {
				balanceAcc = (Account) balanceAcc.clone();
				balanceAcc.setCurrency(forcedAccountNameCcy);
			}
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
		if (balanceAcc.getOriginalAccountId() == 0) {
			Log.error(LOG_CATEGORY,
					"Account " + balanceAcc.getName() + " has to be an "
							+ "automatically created account to post FX Translation on " + "auto account " + account

									.getName());
			return null;
		}

		if (forcedAccountNameCcy == null && account.getId() == balanceAcc.getOriginalAccountId()) {
			return balanceAcc;
		}

		Vector<AccountTranslation> attributes = account.getAttributes();

		Account original = BOCache.getAccount(DSConnection.getDefault(), balanceAcc.getOriginalAccountId());

		if (original == null) {
			Log.error(LOG_CATEGORY, "Cannot find original account of " + balanceAcc.getName());
			return null;
		}

		try {
			original = (Account) original.clone();
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}

		if (Util.isTrue(original.getAccountProperty("Propagate"), false)) {

			for (int i = 0; i < attributes.size(); i++) {
				AccountTranslation accTrans = (AccountTranslation) attributes.elementAt(i);
				String attributeName = accTrans.getAttribute();
				if (propagationExcludedAttributeList == null
						|| !propagationExcludedAttributeList.contains(attributeName)) {

					if (!attributeName.equals("Constant")) {

						String ss = balanceAcc.getAccountProperty(attributeName);
						if (!Util.isEmpty(ss)) {
							account.setAccountProperty(attributeName, ss);
						}
					}
				}
			}
		}

		Vector<AccountTranslation> origAttributes = original.getAttributes();

		boolean allFound = true;
		for (int i = 0; i < attributes.size(); i++) {
			AccountTranslation at = (AccountTranslation) attributes.elementAt(i);
			if (!at.getAttribute().equals("Constant")) {

				boolean found = false;
				for (int k = 0; k < origAttributes.size(); k++) {
					AccountTranslation at2 = (AccountTranslation) origAttributes.elementAt(k);
					if (at2.getAttribute().equals(at.getAttribute())) {
						found = true;
					}
				}
				if (!found) {
					allFound = false;
					break;
				}
			}
		}
		if (!allFound) {
			Log.error(LOG_CATEGORY,
					"Auto Account Attributes of " + account.getName() + " are not defined in " + original.getName());
			return null;
		}

		String accountName = null;

		BOTransfer tempXfer = new BOTransfer();
		String str = getAutoAttributeValue("Book", original, balanceAcc);

		if (!Util.isEmptyString(str)) {
			Book book = BOCache.getBook(DSConnection.getDefault(), str);
			if (book != null) {
				tempXfer.setBookId(book.getId());
			}
		}
		if (balanceAcc.getProcessingOrgId() > 0) {
			tempXfer.setInternalLegalEntityId(balanceAcc.getProcessingOrgId());
		} else {
			str = getAutoAttributeValue("ProcessingOrg", original, balanceAcc);

			if (!Util.isEmptyString(str)) {
				LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), str);
				if (po != null) {
					tempXfer.setInternalLegalEntityId(po.getId());
				}
			}
		}
		str = getAutoAttributeValue("ProductId", original, balanceAcc);

		if (!Util.isEmptyString(str)) {
			int id = Integer.parseInt(str);
			tempXfer.setProductId(id);
		} else {
			str = getAutoAttributeValue("ProductDesc", original, balanceAcc);

			List<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
			if (!Util.isEmptyString(str)) {
				Vector<ProductDesc> productDesc = null;
				try {
					bindVariables.add(new CalypsoBindVariable(12, str));
					String where = "description = ? ";

					productDesc = DSConnection.getDefault().getRemoteProduct().getAllProductDesc(where, bindVariables);
					if (productDesc != null && productDesc.size() > 1) {
						Log.error(LOG_CATEGORY, "There are multiple product descriptions for " + str);

						return null;
					}
				} catch (Exception e) {
					Log.error(LOG_CATEGORY, e);
				}
				if (productDesc != null && (ProductDesc) productDesc.elementAt(0) != null) {
					ProductDesc desc = (ProductDesc) productDesc.elementAt(0);
					int id = desc.getId();
					tempXfer.setProductId(id);
				}
			}
		}

		tempXfer.setSettlementCurrency(balanceAcc.getCurrency());

		Vector<String> attributeValues = new Vector<String>();

		Comparator<AccountTranslation> c = new Comparator<AccountTranslation>() {
			public int compare(AccountTranslation a1, AccountTranslation a2) {
				return a1.getOrderId() - a2.getOrderId();
			}
		};
		attributes = SortShell.sort(attributes, c);
		account.setAttributes(attributes);

		accountName = KeywordUtil.fillAccount(null, account, null, tempXfer, null, DSConnection.getDefault(),
				new Vector<String>(), attributeValues);

		Account autoAccount = BOPostingUtil.getAccount(accountName, balanceAcc.getProcessingOrgId(),
				balanceAcc.getCurrency());
		if (autoAccount != null) {
			return autoAccount;
		}

		autoAccount = account.generateAccount(null, null, tempXfer, DSConnection.getDefault(), new Vector<String>());

		if (autoAccount == null) {
			return null;
		}
		autoAccount.setLegalEntityId(account.getLegalEntityId());
		autoAccount.setLegalEntityRole(account.getLegalEntityRole());

		try {
			int id = DSConnection.getDefault().getRemoteAccounting().save(autoAccount);

			autoAccount = DSConnection.getDefault().getRemoteAccounting().getAccount(id);
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
		return autoAccount;
	}

	@SuppressWarnings({ "deprecation" })
	protected static String getAutoAttributeValue(String attrName, Account account, Account balanceAcc) {
		if (!Util.isEmptyString(balanceAcc.getAccountProperty(attrName)))
			return balanceAcc.getAccountProperty(attrName);
		String accName = balanceAcc.getName();
		Vector<AccountTranslation> attributes = account.getAttributes();
		if (attributes == null || attributes.size() <= 0)
			return null;
		Vector<Integer> orderIds = new Vector<Integer>();
		Hashtable<Integer, AccountTranslation> values = new Hashtable<Integer, AccountTranslation>();
		for (int i = 0; i < attributes.size(); i++) {

			AccountTranslation accTrans = (AccountTranslation) attributes.elementAt(i);
			int order = accTrans.getOrderId();
			orderIds.addElement(Integer.valueOf(order));
			values.put(Integer.valueOf(order), accTrans);
		}
		orderIds = Util.sort(orderIds);
		int lastIdx = 0;
		Hashtable<String, String> attrNames = new Hashtable<String, String>();
		for (int ii = 0; ii < orderIds.size(); ii++) {
			Integer id = (Integer) orderIds.elementAt(ii);

			AccountTranslation accTrans = (AccountTranslation) values.get(id);
			String name = accTrans.getAttribute();
			if (name.equals("Constant"))
				continue;
			int idx = 0;
			int gap = 0;
			if (ii < orderIds.size() - 1) {
				Integer next = (Integer) orderIds.elementAt(ii + 1);

				AccountTranslation constant = (AccountTranslation) values.get(next);
				String value = constant.getValue();
				if (Util.isEmpty(value)) {
					continue;
				}

				gap = value.length();
				idx = accName.indexOf(value, lastIdx);
			} else {

				idx = accName.length();
			}
			Log.debug("ACCOUNTING_CONVERSION", "name=" + name + lastIdx + "/" + idx);

			if (idx >= 0) {

				Log.debug("ACCOUNTING_CONVERSION", "name=" + accName.substring(lastIdx, idx).trim());
				attrNames.put(name, accName.substring(lastIdx, idx).trim());
				lastIdx = idx + gap;
			}
			continue;
		}
		return (String) attrNames.get(attrName);
	}

	/**
	 * cancel previous posting. Add trade id criteria compared to core
	 * @param posting
	 * @param valDate
	 * @param postingsToSave
	 * @param isByBookingDate
	 * @param eventType
	 * @return
	 * @throws Exception
	 */
	public static boolean cancelPreviousPostings(BOPosting posting, JDate valDate, PostingArray postingsToSave,
			boolean isByBookingDate, String eventType) throws Exception {

		PostingArray oldPostings = null;

		try {
			List<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
			String where = " trade_id = ? " + " AND  book_id = ?" + " AND  product_id = ?" + " AND  posting_type = "
					+ Util.string2SQLString("FX_CONVERSION") + " AND bo_posting_type = ?" + " AND description = ? "
					+ " AND effective_date = ?" + " AND currency_code = ?" + " AND matching = "
					+ ioSQL.toSQLBoolean(true);
			if (!isByBookingDate) {

				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.LONG, posting.getTradeLongId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, posting.getBookId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, posting.getProductId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, eventType));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, posting.getDescription()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, posting.getEffectiveDate()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, posting.getCurrency()));

			} else {
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.LONG, posting.getTradeLongId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, posting.getBookId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, posting.getProductId()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, eventType));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, posting.getDescription()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, posting.getBookingDate()));
				bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, posting.getCurrency()));
			}

			oldPostings = DSConnection.getDefault().getRemoteBO().getBOPostings(where, true, bindVariables);
		} catch (Exception e) {
			Log.error("ACCOUNTING_CONVERSION", e);
			throw new Exception(e.getMessage());
		}

		if (oldPostings == null || oldPostings.size() <= 0) {
			return false;
		}
		if (oldPostings.size() > 1) {
			String error = "More than 1 existing matching the " + eventType + " Posting " + posting.getDescription();
			Log.error("ACCOUNTING_CONVERSION", error);
			throw new Exception(error);
		}

		boolean existing = false;
		for (int i = 0; i < oldPostings.size(); i++) {
			BOPosting post = (BOPosting) oldPostings.get(i);
			if (post.getAmount() != posting.getAmount() || post.getDebitAccountId() != posting.getDebitAccountId()
					|| post.getCreditAccountId() != posting.getCreditAccountId()) {

				BOPostingUtil.reversePosting(post, postingsToSave, isByBookingDate);
			} else {

				existing = true;
			}
		}

		return existing;
	}

	/**
	 * get account using cache
	 * @param name
	 * @param poId
	 * @param functionCcy
	 * @return
	 */
	public Account getAccount(String name, int poId, String functionCcy) {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(name);
		strBuilder.append("#");
		strBuilder.append(poId);
		strBuilder.append("#");
		strBuilder.append(functionCcy);
		String key = strBuilder.toString();
		Account account = accountCache.get(key);
		if (account == null) {
			account = BOPostingUtil.getAccount(name, poId, functionCcy);
			accountCache.put(key, account);
		}
		return account;
	}

}
