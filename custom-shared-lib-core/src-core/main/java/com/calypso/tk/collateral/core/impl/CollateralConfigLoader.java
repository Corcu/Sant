package com.calypso.tk.collateral.core.impl;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CollateralDateRuleUtil;
import com.calypso.tk.collateral.core.MarginCallConfigLoader;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfigInterface;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class CollateralConfigLoader extends MarginCallConfigLoader {
	public List<MarginCallConfigInterface> getAllContracts(String poName, String leName, List<String> contractTypes,
			JDatetime valDatetime, JDatetime processDatetime, List<String> roles, boolean lazy) {
		LegalEntity po = null;
		LegalEntity le = null;
		DSConnection ds = DSConnection.getDefault();
		if ((!(Util.isEmpty(poName))) && (!("ALL".equals(poName)))) {
			po = BOCache.getLegalEntity(ds, poName);
		}
		if ((!(Util.isEmpty(leName))) && (!("ALL".equals(leName)))) {
			le = BOCache.getLegalEntity(ds, leName);
		}
		int poId = 0;
		int leId = 0;
		if (po != null)
			poId = po.getId();
		if (le != null)
			leId = le.getId();
		List allContracts;
		try {
			if (lazy) {
				allContracts = DSConnection.getDefault().getRemoteReferenceData()
						.getAllMarginCallConfigDescriptions(poId, leId);
			} else {
				allContracts = DSConnection.getDefault().getRemoteReferenceData().getAllMarginCallConfig(poId, leId);
			}
		} catch (RemoteException e) {
			Log.error(this, e);
			allContracts = new ArrayList();
		}
		if (!(Util.isEmpty(allContracts))) {
			List filteredContracts = new ArrayList(allContracts);

			JDate valDate = null;
			if (processDatetime != null) {
				valDate = JDate.valueOf(processDatetime);
			}
			for (int i = 0; i < allContracts.size(); ++i) {
				MarginCallConfigInterface mcc = (MarginCallConfigInterface) allContracts.get(i);

				if ((!(Util.isEmpty(contractTypes))) && (!(contractTypes.contains(mcc.getContractType())))) {
					filteredContracts.remove(mcc);
				}

				if (valDatetime != null) {
					if (mcc.getStartingDate().after(valDatetime)) {
						filteredContracts.remove(mcc);
					}

					if ((mcc.getAgreementStatus().equals("CLOSED")) && (mcc.getClosingDate() != null)
							&& (mcc.getClosingDate().before(valDatetime))) {
						filteredContracts.remove(mcc);
					}

				}

				if ((valDate != null) && (!(CollateralDateRuleUtil.contains(mcc.getDateRule(), valDate)))) {
					filteredContracts.remove(mcc);
				}
				if ((Util.isEmpty(roles)) || (roles.contains(mcc.getLeRole())))
					continue;
				filteredContracts.remove(mcc);
			}

			return filteredContracts;
		}
		return allContracts;
	}

	public List<MarginCallConfigInterface> loadContracts(List<String> ids) {
		List result = new ArrayList();
		if (!(Util.isEmpty(ids))) {
			for (int i = 0; i < ids.size(); ++i) {
				String id = (String) ids.get(i);
				if (!(Util.isEmpty(id))) {
					MarginCallConfigInterface config = CollateralCacheUtil
							.getMarginCallConfig(DSConnection.getDefault(), Integer.valueOf(id).intValue());

					if (accept(config)) {
						result.add(config);
					}
				}
			}
		}
		return result;
	}

	private boolean accept(MarginCallConfigInterface config) {
		boolean result = true;
		if ((config == null) || ((config.getParentId() > 0) && (!(config.isNonNettingIndependentAmount())))) {
			result = false;
		}
		return result;
	}

	public MarginCallConfigInterface getMarginCallConfig(Trade trade, JDatetime valDatetime) {
		MarginCallConfigInterface result = null;
		String keywordTransient = trade.getTransientKeywordValue("MARGIN_CALL_CONFIG_ID");

		String keyword = trade.getKeywordValue("MARGIN_CALL_CONFIG_ID");

		String marginCallConfig = null;
		if (!(Util.isEmpty(keywordTransient)))
			marginCallConfig = keywordTransient;
		else if (!(Util.isEmpty(keyword))) {
			marginCallConfig = keyword;
		}

		int configId = 0;
		if (!(Util.isEmpty(marginCallConfig))) {
			configId = Util.stringToInteger(marginCallConfig);
			if (configId > 0) {
				result = CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(), configId);
			}

		}

		if (result == null) {
			TimeZone location = null;
			int poId = 0;
			if (trade.getBook() != null) {
				poId = trade.getBook().getLegalEntity().getId();
				location = trade.getBook().getLocation();
			}
			int leId = 0;
			if (trade.getCounterParty() != null) {
				leId = trade.getCounterParty().getId();
			}

			List configs = loadMarginCallConfig(poId, leId, trade.getRole(), "ALL", "OPEN");

			if (!(Util.isEmpty(configs))) {
				List validConfigs = new ArrayList();
				for (int i = 0; i < configs.size(); ++i) {
					MarginCallConfigInterface config = (MarginCallConfigInterface) configs.get(i);
					if ((config == null) || (!(((CollateralConfig) config).acceptPerimeterType(trade, valDatetime,
							valDatetime.getJDate(location), DSConnection.getDefault())))) {
						continue;
					}

					validConfigs.add(config);
				}

				if ((validConfigs != null) && (validConfigs.size() == 1)) {
					result = (MarginCallConfigInterface) validConfigs.get(0);
				}
			}
		}
		return result;
	}

	public List<MarginCallConfigInterface> loadMarginCallConfig(int poId, int leId, String role, String contract,
			String status) {
		List v = null;
		try {
			v = ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig(poId, leId);
		} catch (Exception e) {
			Log.error(this, e);
		}
		List v1 = new ArrayList();
		if (!(Util.isEmpty(v))) {
			for (int i = 0; i < v.size(); ++i) {
				MarginCallConfigInterface mcc = (MarginCallConfigInterface) v.get(i);
				int pId = mcc.getPoId();
				LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), pId);

				boolean access = AccessUtil.isAuthorizedProcOrg(po, false);
				if (access) {
					v1.add(mcc);
				}
			}
		}
		for (int i = v1.size() - 1; i >= 0; --i) {
			MarginCallConfigInterface mcc = (MarginCallConfigInterface) v1.get(i);
			if ((!(role.equals("ALL"))) && (!(mcc.getLeRole().equals(role)))) {
				v1.remove(i);
			} else if ((!(contract.equals("ALL"))) && (!(mcc.getContractType().equals(contract)))) {
				v1.remove(i);
			} else {
				if ((status.equals("ALL")) || (mcc.getAgreementStatus().equals(status)))
					continue;
				v1.remove(i);
			}

		}

		return v1;
	}

	public List<MarginCallConfigInterface> getAllContracts(String poName, String leName, List<String> contractTypes) {
		return getAllContracts(poName, leName, contractTypes, null, null, null, false);
	}
}