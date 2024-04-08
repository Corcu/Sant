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
import com.calypso.tk.report.MarginCallAllocationBaseReportStyle;
import com.calypso.tk.report.ReportStyle;

public class SantMarginCallAllocationReportStyleHelper extends SantReportStyleHelper {

	private static final String MRG_CALL_ALLOCATION_DTO_PREFIX = "MarginCallAllocationBase.";

	@Override
	protected ReportStyle getReportStyle() {
		return new MarginCallAllocationBaseReportStyle();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void loadTreeList() {
		this.treeList = new TreeList("SantMarginCallAllocation");
		try {
			final Class cl = Class.forName("com.calypso.tk.report.MarginCallAllocationBaseReportStyle");
			final Field[] fields = cl.getDeclaredFields();

			for (int i = 0; i < fields.length; i++) {
				final Field f = fields[i];
				if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
					final String simpleName = (String) f.get(null);
					final String columnName = MRG_CALL_ALLOCATION_DTO_PREFIX + simpleName;
					this.treeList.add(columnName);
					this.columnNames.put(columnName, simpleName);
				}
			}
		} catch (final Exception e) {
			Log.error(this, e);
		}

	}

}
