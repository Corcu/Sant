/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.core.Log;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;

/**
 * @author aela
 * 
 */
public abstract class AbstractExternalAllocationReader implements
		ExternalAllocationReader {

	
	protected InputStream is;
	protected ExternalAllocationImportContext context;
	//callback;


	/**
	 * @param is
	 * @param context
	 */
	public AbstractExternalAllocationReader(InputStream is, ExternalAllocationImportContext context) {
		this.is = is;
		this.context = context;
	}
	
	/**
	 * 
	 */
	public AbstractExternalAllocationReader() {
	}
	

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.reader.ExternalAllocationReader#readAllocations(java.util.List)
	 */
	@Override
	public List<ExternalAllocationBean> readAllocations(
			List<AllocImportErrorBean> errors) throws Exception {

		List<ExternalAllocationBean> listExtAllocs = new ArrayList<ExternalAllocationBean>();
		
		if(is != null) {
			String message = "";
			ExternalAllocationBean externalAllocationBean = null;
		    try {
		        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		        if (is!=null) {                         
		            while ((message = reader.readLine()) != null) { 
		            	externalAllocationBean = readAllocation(message,errors);
		            	if(externalAllocationBean !=null) {
		            		listExtAllocs.add(externalAllocationBean);
		            	} 
		            }               
		        }
		    } finally {
		        try { is.close(); } catch (IOException ignore) {
		        	Log.error(this, ignore); //sonar
		        }
		    }
		}
		
		return listExtAllocs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see calypsox.tk.collateral.allocation.reader.ExternalAllocationReader#
	 * readAllocation(java.util.List)
	 */
	@Override
	public abstract ExternalAllocationBean readAllocation(String message, List<AllocImportErrorBean> errors)
			throws Exception;
}
