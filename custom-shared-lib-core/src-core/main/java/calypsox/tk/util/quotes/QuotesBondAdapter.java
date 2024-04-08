/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.quotes;

import java.util.HashMap;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.apps.common.adapter.AdapterException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

public class QuotesBondAdapter {

    public boolean adaptMessage(final String message,
            final HashMap<String, QuoteBondBean> importedObjects)
            throws AdapterException {

        boolean rst = false;

        try {
            final QuoteBondBean bean = new QuoteBondBean();
            final String[] fields = message.split("\t");
            final String date = fields[1];

            bean.setSecuritynum(fields[0]);
            bean.setCloseDate(JDate.valueOf(date));
            // bean.setCollGroup(fields[2]);
            bean.setCollGroupdesc(fields[2]);
            // bean.setCollType(fields[4]);
            bean.setCollDescription(fields[3]);
            // bean.setColldisprice(fields[6]);
            bean.setCollprice(fields[4]);
            bean.setInterestRate(fields[5]);
            bean.setEndyear(fields[6]);

            importedObjects.put(bean.getSecuritynum(), bean);
            rst = true;
        } catch (final Exception e) {
            Log.error(this, buildLogError(message, e));

            final String desc = ErrorCodeEnum.InputFileCanNotBeMoved
                    .getFullTextMesssage(new String[] {
                            "ScheduledTaskSANT_IMPORT_BOND_QUOTES",
                            "Error parsing datas" });
            ControlMErrorLogger.addError(ErrorCodeEnum.InvalidData, desc);
        }

        return rst;

    }

    private String buildLogError(final String message, final Exception ex) {

        final StringBuilder str = new StringBuilder();

        str.append("Error while reading Collateral Prices : ");
        str.append(message);
        str.append(". Error was :");
        str.append(ex.toString());

        return str.toString();

    }
}
