/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.service;

import calypsox.tk.util.riskparameters.SantRiskParameter;

import java.rmi.RemoteException;
import java.util.List;

public interface RemoteSantRiskParameterService {

	public void save(List<SantRiskParameter> rpList) throws RemoteException;

	public List<SantRiskParameter> get(String sqlQuery) throws RemoteException;
}
