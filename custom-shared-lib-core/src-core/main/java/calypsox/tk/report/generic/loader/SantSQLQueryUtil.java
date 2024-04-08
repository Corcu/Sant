/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.util.List;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;

public abstract class SantSQLQueryUtil {

	public static String getDealOwner(final ReportTemplate template) {
		return (String) template.get(SantGenericTradeReportTemplate.OWNER_DEALS);
	}

	public static String getEconomicSector(final ReportTemplate template) {
		return (String) template.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);
	}

	public static String getCounterparty(final ReportTemplate template) {
		return (String) template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
	}

	public static boolean getFund(final ReportTemplate template) {
		final Boolean fund = (Boolean) template.get(SantGenericTradeReportTemplate.FUND_ONLY);
		if (fund != null) {
			return fund;
		}
		return false;
	}

	public static String getTradeStatus(final ReportTemplate template) {
		return (String) template.get(SantGenericTradeReportTemplate.TRADE_STATUS);
	}

	public static JDate getDate(final ReportTemplate template, final JDate valDate, final String name,
			final String direction, final String tenor) {
		return Report.getDate(template, valDate, name, direction, tenor);
	}

	@SuppressWarnings("unchecked")
	public static void buildOptionalCptySQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {

		final String s = getCounterparty(template);
		if (Util.isEmpty(s)) {
			return;
		}

		final Vector<String> ids = Util.string2Vector(s);
		if (ids.size() > 0) {
			if (sqlWhere.length() > 0) {
				sqlWhere.append(" AND ");
			}
			if (ids.size() < ioSQL.MAX_ITEMS_IN_LIST) {
				sqlWhere.append(" trade.cpty_id IN (").append(Util.collectionToString(ids)).append(")");
			} else {
				final List<String> idsStrList = ioSQL.returnStringsOfStrings(ids);
				sqlWhere.append("(trade.cpty_id IN (").append(idsStrList.get(0)).append(")");
				for (int i = 1; i < idsStrList.size(); i++) {
					sqlWhere.append(" OR trade.cpty_id IN (").append(idsStrList.get(i)).append(")");
				}
				sqlWhere.append(")");
			}

		}

	}

	public static void buildOptionalFundSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		final boolean fund = getFund(template);
		if (!fund) {
			return;
		}
		sqlFrom.append(", legal_entity le_for_role, legal_entity_role role");

		sqlWhere.append(" AND trade.cpty_id = le_for_role.legal_entity_id");
		sqlWhere.append(" AND le_for_role.legal_entity_id = role.legal_entity_id");
		sqlWhere.append(" AND role.role_name = 'Fund'");

	}

	public static void buildOptionalDealOwnerSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		final String s = getDealOwner(template);
		if (Util.isEmpty(s)) {
			return;
		}

		sqlFrom.append(", book, legal_entity le_deal_owner");

		final Vector<String> ids = Util.string2Vector(s);
		sqlWhere.append(" AND trade.book_id = book.book_id ");
		sqlWhere.append(" AND book.legal_entity_id = le_deal_owner.legal_entity_id");
		sqlWhere.append(" AND le_deal_owner.legal_entity_id IN (");
		sqlWhere.append(Util.collectionToString(ids));
		sqlWhere.append(")");
	}

	public static void buildOptionalTradeStatusSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		final String s = getTradeStatus(template);
		if (Util.isEmpty(s)) {
			return;
		}
		final Vector<String> tradeStatusVect = Util.string2Vector(s, ",");
		if ((tradeStatusVect != null) && (tradeStatusVect.size() > 0)) {
			sqlWhere.append(" AND trade.trade_status in ").append(Util.collectionToSQLString(tradeStatusVect));
		}

	}

	public static void buildOptionalEconomicSectorSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
			final ReportTemplate template) {
		final String s = getEconomicSector(template);
		if (Util.isEmpty(s)) {
			return;
		}
		sqlFrom.append(", trade_keyword kw_eco_sector)");

		sqlWhere.append("AND  trade.trade_id=kw_eco_sector.trade_id ");
		sqlWhere.append("AND kw_eco_sector.keyword_name='");
		sqlWhere.append(SantGenericTradeReportTemplate.ECONOMIC_SECTOR).append("' ");
		sqlWhere.append("AND kw_eco_sector.keyword_value in '").append(s).append("' ");

	}
}
