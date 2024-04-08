/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import calypsox.tk.core.AuditUtil;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.core.WorkflowUtil;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Cash;
import com.calypso.tk.service.DSConnection;

public class SantCheckNoChangeTradeRule implements WfTradeRule {
    /**
     * Audit domain name for Trade CheckNoChange functionality
     */
    private static final String LBP_CHECK_NO_CHANGE_TRADE = "SantCheckNoChangeTradeRule";

    /**
     * Workflow filter, update is executed if condition is verified
     * 
     * @param wc
     *            workflow transition configuration
     * @param trade
     *            involved Trade
     * @param oldTrade
     *            involved old Trade
     * @param messages
     *            error messages to publish
     * @param ds
     *            Data Server connection
     * @param excps
     *            exceptions to publish
     * @param taskCNC
     *            generated if exists
     * @param db
     *            database connection
     * @param events
     *            PSEvent list
     * @return true if changed Trade attributes are in white list
     * @see AuditUtil
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages, final DSConnection ds,
	    final Vector excps, final Task task, final Object db,
	    final Vector events) {
	Log.info(this, "SantCheckNoChangeTradeRule start");
	boolean validChanges = true;
	// product's authorized changes
	final Collection<String> pIgnore = getIgnoredChanges(
		trade.getProduct(), ds);
	// trades's authorized changes
	final Collection<String> tIgnore = getIgnoredChanges(trade, ds);
	final List<String> errs = new ArrayList<String>();
	if (!AuditUtil.getInstance().ignoreChanges(errs, false, trade,
		oldTrade, tIgnore, pIgnore, null /* fIgnore */, ds)) {
	    final String errorMessage = "Trade Modifications not Allowed for BO_AMEND Action.\nCheck TradeMask Data";
	    messages.add(errorMessage);
	    validChanges = false;
	}
	if (trade.getProduct() instanceof Cash) {
	    final String boAmendDate = trade
		    .getKeywordValue("SantanderUtil.BO_AMEND_EFFECTIVE_DATE");
	    if ((boAmendDate != null) && !"".equals(boAmendDate)) {
		try {
		    WorkflowUtil.getInstance().getJDateFromString(boAmendDate);
		} catch (final ParseException e) {
		    final String errorMessage = "Format of TradeKeyword '"
			    + SantanderUtil.BO_AMEND_EFFECTIVE_DATE
			    + "' is not allowed.\nExpected format is: '"
			    + WorkflowUtil.WF_DATE_FORMAT
			    + "'. Current Value: '" + boAmendDate + "'";
		    messages.add(errorMessage);
		    validChanges = false;
		}
	    }
	}
	Log.info(this, "SantCheckNoChangeTradeRule ends with: " + validChanges);
	return validChanges;
    }

    /**
     * Extended list of Product authorized changes
     * 
     * @param product
     *            involved Product
     * @param ds
     *            Data Server connection
     * @return list of Product authorized changes as String array
     * @see AuditUtil
     */
    public Collection<String> getIgnoredChanges(final Product product,
	    final DSConnection ds) {
	return AuditUtil.getInstance().getIgnoredChanges(product, null /*
								        * default
								        * is
								        * BackOffice
								        */, ds,
		LBP_CHECK_NO_CHANGE_TRADE);
    }

    /**
     * Extended list of Trade authorized changes
     * 
     * @param trade
     *            involved Trade
     * @param ds
     *            Data Server connection
     * @return list of Trade authorized changes as String array
     * @see AuditUtil
     */
    public Collection<String> getIgnoredChanges(final Trade trade,
	    final DSConnection ds) {
	return AuditUtil.getInstance().getIgnoredChanges(trade, null /*
								      * default
								      * is
								      * BackOffice
								      */, ds,
		LBP_CHECK_NO_CHANGE_TRADE);
    }

    /**
     * @return Workflow rule description
     */
    @Override
    public String getDescription() {
	return "-true when no amendment on Trade is made. No update on Trade is authorized by that Check Rule";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages,
	    final DSConnection dsCon, final Vector excps, final Task task,
	    final Object dbCon, final Vector events) {
	return true;
    }
}
