/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import calypsox.tk.report.SantCashFlowsReportTemplate;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.InventoryPositionArray;

public class SantBOPositionLoader extends SantAbstractLoader {

    public InventoryPositionArray<?> load(final ReportTemplate template,
	    final JDate valDate) throws Exception {

	final InventoryPositionArray<?> positions = loadPositions(template,
		valDate);

	return positions;
    }

    private InventoryPositionArray<?> loadPositions(final ReportTemplate template,
	    final JDate valDate) throws Exception {

	final List<String> from = new ArrayList<String>();
	final StringBuilder sqlWhere = new StringBuilder();

	buildMarginCallEntriesSQLQuery(template, valDate, from, sqlWhere);

	return null;

    }

    protected void buildMarginCallEntriesSQLQuery(
	    final ReportTemplate template, final JDate valDate,
	    final List<String> from, final StringBuilder sqlWhere) {

	// Call Account
	final String callAccountStr = (String) template
		.get(SantCashFlowsReportTemplate.CALL_ACCOUNT);
	Vector<String> callAccountIds = null;

	if (!Util.isEmpty(callAccountStr)) {
	    from.add("acc_account");
	    callAccountIds = Util.string2Vector(callAccountStr);
	    sqlWhere.append(" AND mrgcall_config.account_id = acc_account.acc_account_id ");
	    sqlWhere.append(" AND acc_account.acc_account_name in ( ");
	    sqlWhere.append(Util.collectionToSQLString(callAccountIds)).append(
		    ")");
	}
    }
}
