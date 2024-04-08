/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.loader;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class IssuerLoader extends SantLoader<Integer, String> {

    @SuppressWarnings("unchecked")
    public Map<Integer, String> load() {

        Vector<LegalEntity> issuerVector = new Vector<LegalEntity>();

        // from clause
        StringBuilder fromClause = new StringBuilder();
        fromClause.append("legal_entity_role");

        // where clause
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("legal_entity.legal_entity_id = legal_entity_role.legal_entity_id");
        whereClause.append(" AND legal_entity_role.role_name = ");
        whereClause.append(Util.string2SQLString(LegalEntity.ISSUER));

        // get le from db
        try {
            issuerVector = DSConnection.getDefault().getRemoteReferenceData()
                    .getAllLE(fromClause.toString(), whereClause.toString(), false, null);
        } catch (RemoteException e) {
            Log.error(this, "Error getting issuer legal entities.", e);
            // return null;
        }

        // create map id-code
        if (!Util.isEmpty(issuerVector)) {
            // this.map.put(-1, "");
            for (final LegalEntity issuer : issuerVector) {
                this.map.put(issuer.getId(), issuer.getCode());
            }
        }

        // sort map
        final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(new ValueComparator(this.map));
        sortedMap.putAll(this.map);

        return sortedMap;

    }
}
