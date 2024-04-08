/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ExecuteSQLQueriesMap {

	private static final HashMap<String, String> sqlMap = new HashMap<String, String>();

	static {
		sqlMap.put(
				"Trade Counts",
				"select to_char(sysdate,'DD/MM/YYYY-HH24:MI') , b.product_type , a.trade_status, c.keyword_value, count(*) "
						+ "\nfrom trade a, product_Desc b, trade_keyword c "
						+ "\nwhere a.product_id = b.product_id AND b.product_type in ('CollateralExposure', 'SecLending', 'Repo') "
						+ "and a.trade_id=c.trade_id(+) and c.keyword_name(+) = 'BO_SYSTEM' "
						+ "\ngroup by a.trade_status, b.product_type, c.keyword_value");

		sqlMap.put("Static Data Objects loaded - Counts", "select 'PRODUCT_BOND', sysdate, count(*) from product_bond "
				+ "\nunion all select 'PRODUCT_EQUITY',sysdate, count(*) from product_equity "
				+ "\nunion all select 'MRGCALL_CONFIG', sysdate, count(*) from mrgcall_config "
				+ "\nunion all select 'BOOK', sysdate, count(*) from book "
				+ "\nunion all select 'TRADE', sysdate,count(*) from trade "
				+ "\nunion all select 'PL_MARK', sysdate, count(*) from pl_mark "
				+ "\nunion all select 'PL_MARK_VALUE', sysdate, count(*) from pl_mark_value "
				+ "\nunion all select 'CALL_ACCOUNT', sysdate, count(*) from acc_account where call_account_b=1");
	}

	public static List<String> getSQLKeys() {

		ArrayList<String> list = new ArrayList<String>();
		list.add("");

		Iterator<String> iterator = sqlMap.keySet().iterator();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	public static String getSqlQuery(String key) {
		return sqlMap.get(key);
	}

}
