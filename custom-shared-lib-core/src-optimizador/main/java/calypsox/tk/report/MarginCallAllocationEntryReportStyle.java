/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.Vector;

public class MarginCallAllocationEntryReportStyle extends com.calypso.tk.report.MarginCallAllocationEntryReportStyle {

    private static final long serialVersionUID = 77333494870315733L;

    public static final String MARGIN_CALL_CONTRACT = "Margin Call Contract";

    public static final String OWNER_CODE = "Owner code";

    public static final String TRADE_STATUS = "Trade status";
    
    public static final String SIGNED_ALL_IN_VALUE = "Signed " + ALL_IN_VALUE;
    
    public static final String SIGNED_CONTRACT_VALUE = "Signed " + CONTRACT_VALUE;


    private static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if (MARGIN_CALL_CONTRACT.equals(columnName)) {

            MarginCallAllocationDTO allocation = (MarginCallAllocationDTO) row.getProperty("Default");
            try {
                CollateralConfig mcc = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfig(allocation.getCollateralConfigId());
                if (mcc != null) {
                    return mcc.getName();
                } else {
                    return "";
                }
            } catch (RemoteException re) {
                Log.error(this, re.getMessage() + "\n" + re); //sonar
            }
            return "";
        }

        if (OWNER_CODE.equals(columnName)) {
            MarginCallAllocationDTO allocation = (MarginCallAllocationDTO) row.getProperty("Default");
            try {
                CollateralConfig mcc = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfig(allocation.getCollateralConfigId());
                if (mcc != null) {
                    return mcc.getProcessingOrg().getCode();
                } else {
                    return "";
                }
            } catch (RemoteException re) {
                Log.error(this, re.getMessage() + "\n" + re); //sonar
            }
            return "";
        }

        if (TRADE_STATUS.equals(columnName)) {

            MarginCallAllocationDTO allocation = (MarginCallAllocationDTO) row
                    .getProperty("Default");
            long tradeId = allocation.getTradeId();
            if (tradeId == 0) {
                return "";
            } else {
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade()
                            .getTrade(allocation.getTradeId());
                    if (trade != null) {
                        return trade.getStatus().getStatus();
                    } else {
                        return "";
                    }
                } catch (RemoteException re) {
                    Log.info(this, re.getMessage() + "\n" + re); //sonar
                    return "";
                }
            }

        }

        Object valueColumn = super.getColumnValue(row, columnName, errors);

        //GSM: 09/02/2016 Fix v14. Somehow super method isMarginCallConfigColumn returns null. Implemented logic here
        if (valueColumn == null) {

            String name = getMarginCallConfigColumn(MARGIN_CALL_CONFIG_PREFIX, columnName);

            if (!Util.isEmpty(name)) {
                MarginCallAllocationDTO allocation = (MarginCallAllocationDTO) row.getProperty("Default");
                if (allocation != null) {
                    row.setProperty("MarginCallConfig", getMarginCallConfig(allocation));
                    valueColumn = getMarginCallConfigReportStyle().getColumnValue(row, name, errors);
                }
            }
        }

        return valueColumn;

    }

    private String getMarginCallConfigColumn(String mc_prefix, String columnName) {

        CollateralConfigReportStyle configReportStyle = getMarginCallConfigReportStyle();
        String n = configReportStyle.getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);

        if (Util.isEmpty(n)) {
            return null;
        }
        return n;

    }

    @SuppressWarnings("rawtypes")
    public Object getBaseColumnValue(MarginCallAllocationFacade allocation, String columnName, Vector errors) {

    	if (SIGNED_ALL_IN_VALUE.equals(columnName)) 
    		return new Amount(allocation.getAllInValue(), allocation.getCurrency());
    	
		if (SIGNED_CONTRACT_VALUE.equals(columnName)) {
			double value = CacheCollateralClient.roundAmount(allocation.getContractValue(),
					allocation.getContractCurrency());
			return new Amount(value, allocation.getContractCurrency());
		}
    	return super.getBaseColumnValue(allocation, columnName, errors);
    }
    
    private CollateralConfig getMarginCallConfig(MarginCallAllocationDTO allocation) {
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), allocation.getCollateralConfigId());
    }
    



}
