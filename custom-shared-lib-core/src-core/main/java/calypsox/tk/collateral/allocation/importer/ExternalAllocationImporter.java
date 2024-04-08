/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.importer;

import java.util.ArrayList;
import java.util.List;

import calypsox.tk.collateral.allocation.importer.mapper.ExcelExternalAllocationMapper;
import calypsox.tk.collateral.allocation.importer.reader.ExcelExternalAllocationReader;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.JDate;

/**
 * @author aela
 * 
 */
public class ExternalAllocationImporter {
	protected ExcelExternalAllocationReader reader;
	protected ExcelExternalAllocationMapper mapper;
	protected MarginCallEntry entry;

	public ExternalAllocationImporter() {
		// TODO Auto-generated constructor stub
	}

	public ExternalAllocationImporter(String filePath, MarginCallEntry entry, JDate processingDate) {
		this.reader = new ExcelExternalAllocationReader(filePath);
		this.mapper = new ExcelExternalAllocationMapper(entry, entry.getProcessDate());
		this.entry = entry;
	}

	public void importAllocations(List<String> messages) throws Exception {
		List<ExternalAllocationBean> marginCallAllocationsBeans = this.reader.readAllocations(messages);
		List<ExternalAllocationBean> validAllocsBeans = new ArrayList<ExternalAllocationBean>();

		if (!Util.isEmpty(marginCallAllocationsBeans)) {
			validAllocsBeans = this.mapper.getValidListAllocation(marginCallAllocationsBeans, messages);
			List<MarginCallAllocation> marginCallAllocations = this.mapper
					.mapListAllocation(validAllocsBeans, messages);
			if (!Util.isEmpty(marginCallAllocations)) {
				for (MarginCallAllocation alloc : marginCallAllocations) {
					if (alloc != null) {
						this.entry.addAllocation(alloc);
					}
				}
			}
		}
	}

	/**
	 * @return the reader
	 */
	public ExcelExternalAllocationReader getReader() {
		return this.reader;
	}

	/**
	 * @return the mapper
	 */
	public ExcelExternalAllocationMapper getMapper() {
		return this.mapper;
	}

}
