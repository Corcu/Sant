/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.style;

import java.lang.reflect.Field;

import javassist.Modifier;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.MarginCallEntryBaseReportStyle;
import com.calypso.tk.report.ReportStyle;

public class SantMarginCallEntryReportStyleHelper extends SantReportStyleHelper {

	private static final String MRG_CALL_ENTRY_DTO_PREFIX = "MarginCallEntryBase.";

	@SuppressWarnings("rawtypes")
	@Override
	public void loadTreeList() {
		this.treeList = new TreeList("SantMarginCallEntry");
		try {
			final Class cl = Class.forName("com.calypso.tk.report.MarginCallEntryBaseReportStyle");
			final Field[] fields = cl.getDeclaredFields();

			for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
					final String simpleName = (String) f.get(null);
					final String columnName = MRG_CALL_ENTRY_DTO_PREFIX + simpleName;
					this.treeList.add(columnName);
					this.columnNames.put(columnName, simpleName);
				}
			}
		} catch (final Exception e) {
			Log.error(this, e);
		}
	}

	@Override
	protected ReportStyle getReportStyle() {
		return new MarginCallEntryBaseReportStyle();
	}

}
