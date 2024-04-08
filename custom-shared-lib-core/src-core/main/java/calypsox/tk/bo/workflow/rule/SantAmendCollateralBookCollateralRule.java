/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import calypsox.tk.bo.KondorPlusMarginCallMessageFormatter;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 * 
 */
public class SantAmendCollateralBookCollateralRule extends BaseCollateralWorkflowRule {

	@SuppressWarnings("unused")
	private final Map<String, Double> currentCashPosition = new HashMap<String, Double>();
	@SuppressWarnings("unused")
	private final Map<String, List<MarginCallAllocation>> pendingCashPosition = new HashMap<String, List<MarginCallAllocation>>();

	@Override
	public String getDescription() {
		return "This rule amends the book of an allocation before creating MarginCall Trade using Book mapping Logic.";
	}

	@Override
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantAmendCollateralBookCollateralRule.class, "SantAmendCollateralBookCollateralRule - Start");
		WorkflowResult wfr = new WorkflowResult();
		List<MarginCallAllocation> pendingAllocations = entry.getPendingMarginAllocations();

		for (MarginCallAllocation pendingAllocation : pendingAllocations) {
			String newBookName = null;
			Book currentBook = pendingAllocation.getBook();
			if (currentBook != null) {
				if (pendingAllocation instanceof CashAllocation) {
					newBookName = CollateralUtilities.getBookAliasMapped(currentBook.getName(),
							KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_KONDOR);

				} else if (pendingAllocation instanceof SecurityAllocation) {
					CollateralConfig marginCallConfig = entry.getCollateralConfig();
					String aliasForSearch = null;
					Product p = ((SecurityAllocation) pendingAllocation).getProduct();
					if (p != null) {
						// Bond
						if (p instanceof Bond) {
							if (marginCallConfig.isRehypothecable()) {
								aliasForSearch = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_K_REHYP;
							} else {
								aliasForSearch = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_K_NO_REHYP;
							}
						}
						// Equity
						else if (p instanceof Equity) {
							aliasForSearch = KondorPlusMarginCallMessageFormatter.ALIAS_BOOK_EQUITY;
						}
					}

					// We look for the book in the alias included for K+.
					newBookName = CollateralUtilities.getBookAliasMapped(currentBook.getName(), aliasForSearch);
				}

				if (!Util.isEmpty(newBookName) && !newBookName.startsWith("BOOK_WARNING")) {
					Book newBook = BOCache.getBook(dsCon, newBookName);
					if (newBook != null) {
						Log.info(SantAmendCollateralBookCollateralRule.class, "OldBook=" + currentBook.getName()
								+ "; New Book=" + newBookName);
						pendingAllocation.setBook(newBook);
					} else {
						Log.info(SantAmendCollateralBookCollateralRule.class, "Not Amended the book");
					}
				}
			}

		}
		Log.info(SantAmendCollateralBookCollateralRule.class, "SantAmendCollateralBookCollateralRule - End");
		wfr.success();
		return wfr;
	}

	@Override
	protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
			EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
			DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
			List<PSEvent> paramList2) {
		return true;
	}

}
