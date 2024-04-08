/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.core;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.service.DSConnection;

import java.io.Serializable;

public class MarginCallConfigLight implements Serializable {

	private static final long serialVersionUID = -2987446483799879443L;

	private int id;

	private String description;

	private String contractType;

	private String poName;

	public int getId() {
		return this.id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getContractType() {
		return this.contractType;
	}

	public void setContractType(final String contractType) {
		this.contractType = contractType;
	}

	public String getPoName() {
		return this.poName;
	}

	public void setPoId(int poId) {
		this.poName = BOCache.getLegalEntity(DSConnection.getDefault(), poId).getCode();

	}
}
