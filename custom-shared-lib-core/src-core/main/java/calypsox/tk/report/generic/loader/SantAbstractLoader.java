/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import com.calypso.tk.core.JDate;
import com.calypso.tk.report.ReportTemplate;

public abstract class SantAbstractLoader {

	protected String getDealOwner(final ReportTemplate template) {
		return SantSQLQueryUtil.getDealOwner(template);
	}

	protected String getEconomicSector(final ReportTemplate template) {
		return SantSQLQueryUtil.getEconomicSector(template);
	}

	protected String getCounterparty(final ReportTemplate template) {
		return SantSQLQueryUtil.getCounterparty(template);
	}

	protected boolean getFund(final ReportTemplate template) {
		return SantSQLQueryUtil.getFund(template);
	}

	protected String getTradeStatus(final ReportTemplate template) {
		return SantSQLQueryUtil.getTradeStatus(template);
	}

	protected JDate getDate(final ReportTemplate template, final JDate valDate, final String name,
			final String direction, final String tenor) {
		return SantSQLQueryUtil.getDate(template, valDate, name, direction, tenor);
	}

	protected void buildOptionalCptySQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		SantSQLQueryUtil.buildOptionalCptySQLQuery(sqlFrom, sqlWhere, template);
	}

	protected void buildOptionalFundSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		SantSQLQueryUtil.buildOptionalFundSQLQuery(sqlFrom, sqlWhere, template);
	}

	protected void buildOptionalDealOwnerSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		SantSQLQueryUtil.buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, template);
	}

	protected void buildOptionalTradeStatusSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		SantSQLQueryUtil.buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, template);
	}

	protected void buildOptionalEconomicSectorSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		SantSQLQueryUtil.buildOptionalEconomicSectorSQLQuery(sqlFrom, sqlWhere, template);
	}
}
