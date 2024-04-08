/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferReportTemplate extends com.calypso.tk.report.TransferReportTemplate {

	private static final long serialVersionUID = 1L;
	// columns
	public static final String IM_VM = "IM/VM";
	public static final String TOMADO_PRESTADO = "Tomado/Prestado";
	public static final String SIGNO_COMISION = "Signo Comision";

	public static final String CUSTOM_NOMINAL = "Custom Nominal";


	@Override
	public void setDefaults() {
		super.setDefaults();
		
		String[] columns = super.getColumns();
		if(columns!=null && columns.length!=0){
			List<String> columnsToAdd = Arrays.asList(columns);
			List<String> newColumns = new ArrayList<String>();
			newColumns.addAll(columnsToAdd);
			newColumns.add("IM/VM");
			newColumns.add(TOMADO_PRESTADO);
			newColumns.add(SIGNO_COMISION);
			newColumns.add(CUSTOM_NOMINAL);
			super.setColumns(newColumns.toArray(new String[newColumns.size()]));
		}
	}
}
